/*  This file is part of Openrouteservice.
 *
 *  Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.

 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.

 *  You should have received a copy of the GNU Lesser General Public License along with this library;
 *  if not, see <https://www.gnu.org/licenses/>.
 */
package org.heigit.geovisroute.routing.graphhopper.extensions.storages.builders;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.storage.GraphExtension;
import com.graphhopper.util.EdgeIteratorState;
import org.heigit.geovisroute.config.profile.ExtendedStorageProperties;
import org.heigit.geovisroute.routing.graphhopper.extensions.WheelchairAttributes;
import org.heigit.geovisroute.routing.graphhopper.extensions.WheelchairTypesEncoder;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.WheelchairAttributesGraphStorage;
import org.heigit.geovisroute.util.UnitsConverter;
import org.locationtech.jts.geom.Coordinate;

import java.util.*;

public class WheelchairGraphStorageBuilder extends AbstractGraphStorageBuilder {
    public static final String KEY_SLOPED_CURB = "sloped_curb";
    public static final String KEY_SLOPED_KERB = "sloped_kerb";
    public static final String KEY_KERB_HEIGHT = "kerb:height";
    public static final String KEY_FOOTWAY = "footway";
    public static final String SW_VAL_RIGHT = "right";
    public static final String SW_VAL_LEFT = "left";
    public static final String KEY_BOTH = "both";
    public static final String KEY_SIDEWALK_BOTH = "sidewalk:both:";
    public static final String KEY_FOOTWAY_BOTH = "footway:both:";
    public static final String KEY_CURB_HEIGHT = "curb:height";

    public enum Side {
        LEFT,
        RIGHT,
        NONE
    }

    private WheelchairAttributesGraphStorage storage;
    private final WheelchairAttributes wheelchairAttributes;
    private final WheelchairAttributes wheelchairAttributesLeftSide;
    private final WheelchairAttributes wheelchairAttributesRightSide;

    private Map<Integer, Map<String, String>> nodeTagsOnWay;
    private Map<String, Object> cleanedTags;

    private boolean hasLeftSidewalk = false;
    private boolean hasRightSidewalk = false;
    private boolean kerbHeightOnlyOnCrossing = false;

    public WheelchairGraphStorageBuilder() {
        wheelchairAttributes = new WheelchairAttributes();
        wheelchairAttributesLeftSide = new WheelchairAttributes();
        wheelchairAttributesRightSide = new WheelchairAttributes();
        nodeTagsOnWay = new HashMap<>();
        cleanedTags = new HashMap<>();
    }

    /**
     * 构造函数 - 用于测试
     *
     * @param onlyAttachKerbsToCrossings 是否只在人行横道上附加路缘高度？
     */
    public WheelchairGraphStorageBuilder(boolean onlyAttachKerbsToCrossings) {
        this();
        kerbHeightOnlyOnCrossing = onlyAttachKerbsToCrossings;
    }

    /**
     * 初始化轮椅存储构建器
     *
     * @param graphhopper 要运行的 graphhopper 实例
     * @return 由该构建器创建的存储
     * @throws Exception 当存储已初始化时抛出
     */
    @Override
    public GraphExtension init(GraphHopper graphhopper) throws Exception {
        if (storage != null)
            throw new Exception("GraphStorageBuilder has been already initialized.");

        ExtendedStorageProperties parameters;
        parameters = this.parameters;

        if (parameters.getKerbsOnCrossings() != null) {
            kerbHeightOnlyOnCrossing = parameters.getKerbsOnCrossings();
        }
        storage = new WheelchairAttributesGraphStorage();
        return storage;
    }

    /**
     * 以空坐标和标签调用 processWay 方法
     *
     * @param way 要处理的 way
     */
    @Override
    public void processWay(ReaderWay way) {
        this.processWay(way, new Coordinate[0], new HashMap<>());
    }

    /**
     * 处理 way
     *
     * @param way      要处理的 way
     * @param coords   该 way 的坐标
     * @param nodeTags 存储在 way 各节点上、处理过程中应使用的标签
     */
    @Override
    public void processWay(ReaderWay way, Coordinate[] coords, Map<Integer, Map<String, String>> nodeTags) {
        // 首先在处理前一个 way 之后重置存储变量
        wheelchairAttributes.reset();
        wheelchairAttributesLeftSide.reset();
        wheelchairAttributesRightSide.reset();
        hasRightSidewalk = false;
        hasLeftSidewalk = false;

        this.nodeTagsOnWay = nodeTags;

        // 恼人的是，标签的各个部分经常不是用 ":" 而是用 "." 分隔，
        // 因此我们需要考虑这种情况
        cleanedTags = cleanTags(way.getTags());

        // 现在需要根据该 way 是独立要素（如 footway）还是附属于道路要素
        //（如带有 sidewalk=left 标签）来分别处理
        processWayCheckForSeparateFeature(way);

        // 即使它是独立的，我们仍然需要处理该 way 本身，以便获取人行道信息
        //（独立的 footway 仍可能带有 sidewalk 标签……）
        processSidewalksAttachedToWay(way);

        // 如果该 way 可归类为独立的 footway，则其适宜性已知
        wheelchairAttributes.setSuitable(isSeparateFootway(way) || way.hasTag("wheelchair_accessible", true));

        // 人行道始终意味着适宜性已知
        wheelchairAttributesLeftSide.setSuitable(true);
        wheelchairAttributesRightSide.setSuitable(true);

        // 处理路缘标签。
        processKerbTags();
    }

    /**
     * 返回道路指定一侧的人行道属性；当传入 NONE 时返回通用属性
     *
     * @param side 需要获取数据的道路一侧
     * @return 包含指定一侧信息的 WheelchairAttributes 对象
     */
    public WheelchairAttributes getStoredAttributes(Side side) {
        return switch (side) {
            case LEFT -> wheelchairAttributesLeftSide;
            case RIGHT -> wheelchairAttributesRightSide;
            case NONE -> wheelchairAttributes;
            default -> null;
        };
    }

    /**
     * 遍历标签并尝试移除所有无效的键（例如复合键使用 '.' 而非 ':' 输入时）
     *
     * @param dirtyTags 需要清理的 OSM 标签集合
     * @return 清理后的标签（标签名中的 . 已替换为 :）
     */
    private HashMap<String, Object> cleanTags(Map<String, Object> dirtyTags) {
        HashMap<String, Object> cleanedTagsMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : dirtyTags.entrySet()) {
            String cleanKey = entry.getKey().replace(".", ":");
            cleanedTagsMap.put(cleanKey, entry.getValue());
        }
        return cleanedTagsMap;
    }

    /**
     * 处理通过 sidewalk 标签附加到 OSM way 的 footway。该方法查找轮椅路由所需的重要参数，
     * 如宽度、平滑度和路缘高度，然后将其存入属性对象中，以便在处理边时使用。
     * 它还会检测人行道是为基础 way 的哪一侧创建的，并相应地存储信息。
     *
     * @param way 要处理的 way
     */
    private void processSidewalksAttachedToWay(ReaderWay way) {

        detectAndRecordSidewalkSide(way);

        // 获取表面类型（沥青、沙地等）
        setSidewalkAttribute(WheelchairAttributes.Attribute.SURFACE);

        // 获取平滑度值（good、terrible 等）
        setSidewalkAttribute(WheelchairAttributes.Attribute.SMOOTHNESS);

        // 获取 track 类型（grade1、grade4 等）
        setSidewalkAttribute(WheelchairAttributes.Attribute.TRACK);

        // 获取 way 的宽度（2、0.1 等）
        setSidewalkAttribute(WheelchairAttributes.Attribute.WIDTH);

        // 获取 way 的坡度（10%、6% 等）
        setSidewalkAttribute(WheelchairAttributes.Attribute.INCLINE);

    }

    /**
     * 处理在 OSM 中作为独立要素存储的 footway，例如人行横道、人行小路或步行道。
     * 与附加处理相同，它查找轮椅路由所需的各种属性标签，
     * 并将其存入通用的轮椅存储对象中
     */
    private void processWayCheckForSeparateFeature(ReaderWay way) {
        boolean markSurfaceQualityKnown = isSeparateFootway(way);
        setWayAttribute(WheelchairAttributes.Attribute.SURFACE, markSurfaceQualityKnown);
        setWayAttribute(WheelchairAttributes.Attribute.SMOOTHNESS, markSurfaceQualityKnown);
        setWayAttribute(WheelchairAttributes.Attribute.TRACK, markSurfaceQualityKnown);
        setWayAttribute(WheelchairAttributes.Attribute.WIDTH, markSurfaceQualityKnown);
        setWayAttribute(WheelchairAttributes.Attribute.INCLINE, markSurfaceQualityKnown);
    }

    /**
     * 根据从 way 收集到的信息，在属性存储对象中设置指定属性。该方法只为独立的 way
     * 设置属性存储对象中的属性，不涉及人行道。
     *
     * @param attribute               要处理的属性
     * @param markSurfaceQualityKnown 是否同时在 WheelchairAttributes 对象中设置 surfaceQualityKnown 标志
     */
    private void setWayAttribute(WheelchairAttributes.Attribute attribute, boolean markSurfaceQualityKnown) {
        if (cleanedTags.containsKey(attributeToTagName(attribute))) {
            setWheelchairAttribute((String) cleanedTags.get(attributeToTagName(attribute)), attribute, markSurfaceQualityKnown);
        }
    }

    /**
     * 为左右两侧人行道在属性存储对象中设置指定属性
     *
     * @param attribute 要处理的属性
     */
    private void setSidewalkAttribute(WheelchairAttributes.Attribute attribute) {
        String[] tagValues;

        tagValues = getSidedTagValue(attributeToTagName(attribute));

        if (tagValues[0] != null && !tagValues[0].isEmpty()) {
            setSidewalkAttributeForSide(tagValues[0], attribute, Side.LEFT);
        }
        if (tagValues[1] != null && !tagValues[1].isEmpty()) {
            setSidewalkAttributeForSide(tagValues[1], attribute, Side.RIGHT);
        }
    }

    /**
     * 检测该 way 上是否有人行道，若有则标记为存在
     *
     * @param way 要查找人行道的 way
     */
    private void detectAndRecordSidewalkSide(ReaderWay way) {
        if (way.hasTag("sidewalk")) {
            String sw = way.getTag("sidewalk");
            switch (sw) {
                case SW_VAL_LEFT -> hasLeftSidewalk = true;
                case SW_VAL_RIGHT -> hasRightSidewalk = true;
                case KEY_BOTH -> {
                    hasLeftSidewalk = true;
                    hasRightSidewalk = true;
                }
                default -> {
                }
            }
        }
    }

    /**
     * 将轮椅属性存储中的属性转换为对应的 OSM 标签键
     *
     * @param attribute 需要获取标签键的属性
     * @return 与该属性对应的 OSM 标签键
     */
    private String attributeToTagName(WheelchairAttributes.Attribute attribute) {
        return switch (attribute) {
            case SURFACE -> "surface";
            case SMOOTHNESS -> "smoothness";
            case TRACK -> "tracktype";
            case WIDTH -> "width";
            case INCLINE -> "incline";
            case KERB -> "kerb";
            default -> "";
        };
    }

    /**
     * 将指定人行道的指定属性设置为传入的值
     *
     * @param value     要存储的值
     * @param attribute 要存储值的属性
     * @param side      该属性所属的人行道
     */
    private void setSidewalkAttributeForSide(String value, WheelchairAttributes.Attribute attribute, Side side) {
        switch (side) {
            case LEFT -> {
                hasLeftSidewalk = true;
                wheelchairAttributesLeftSide.setAttribute(attribute, convertTagValueToEncodedValue(attribute, value), true);
            }
            case RIGHT -> {
                hasRightSidewalk = true;
                wheelchairAttributesRightSide.setAttribute(attribute, convertTagValueToEncodedValue(attribute, value), true);
            }
            default -> {
            }
        }
    }

    /**
     * 将独立 way 上的指定属性设置为传入的值
     *
     * @param value                   要存储的值
     * @param attribute               要存储值的属性
     * @param markSurfaceQualityKnown 是否同时在 WheelchairAttributes 对象中设置 surfaceQualityKnown 标志
     */
    private void setWheelchairAttribute(String value, WheelchairAttributes.Attribute attribute, boolean markSurfaceQualityKnown) {
        wheelchairAttributes.setAttribute(attribute, convertTagValueToEncodedValue(attribute, value), markSurfaceQualityKnown);
    }

    /**
     * 按需使用正确的编码器将值转换为编码值。
     *
     * @param attribute 该值所属的属性
     * @param tagValue  标签中存储的字符串值
     * @return 正确编码后的值
     */
    private int convertTagValueToEncodedValue(WheelchairAttributes.Attribute attribute, String tagValue) {
        switch (attribute) {
            case SMOOTHNESS:
            case TRACK:
            case SURFACE:
                try {
                    return WheelchairTypesEncoder.getEncodedType(attribute, tagValue.toLowerCase());
                } catch (Exception notRecognisedEncodedTypeError) {
                    return -1;
                }
            case WIDTH:
                return (int) (UnitsConverter.convertOSMDistanceTagToMeters(tagValue.toLowerCase()) * 100);
            case INCLINE:
                return getInclineFromTagValue(tagValue.toLowerCase());
            case KERB:
                return convertKerbTagValueToCentimetres(tagValue.toLowerCase());
            default:
                return -1;
        }
    }

    /**
     * 从一组标签中获取路缘高度值。该方法考虑了路缘高度的不同拼写和表示方式，
     * 然后将路缘信息添加到轮椅属性对象中
     */
    private void processKerbTags() {
        String[] assumedKerbTags = new String[]{
                "curb",
                "kerb",
                KEY_SLOPED_CURB,
                KEY_SLOPED_KERB
        };
        String[] explicitKerbTags = new String[]{
                KEY_KERB_HEIGHT,
                KEY_CURB_HEIGHT
        };

        int height = calcSingleKerbHeightFromTagList(assumedKerbTags, -1);
        // 显式高度覆盖假定高度
        height = calcSingleKerbHeightFromTagList(explicitKerbTags, height);

        if (height > -1) {
            wheelchairAttributes.setSlopedKerbHeight(height);
        }

        // 接下来处理附加到 way 两侧的值
        int[] heights = calcSingleKerbHeightFromSidedTagList(assumedKerbTags, new int[]{-1, -1});
        heights = calcSingleKerbHeightFromSidedTagList(explicitKerbTags, heights);

        if (heights[0] > -1) {
            hasLeftSidewalk = true;
            wheelchairAttributesLeftSide.setSlopedKerbHeight(heights[0]);
        }

        if (heights[1] > -1) {
            hasLeftSidewalk = true;
            wheelchairAttributesRightSide.setSlopedKerbHeight(heights[1]);
        }
    }

    /**
     * 根据指定的标签键，计算应存储到图中的路缘高度
     *
     * @param kerbTags     应评估的标签键
     * @param initialValue 返回值的初始值。若未找到路缘高度信息，则返回该值
     * @return 由指定标签键导出的路缘高度值
     */
    private int calcSingleKerbHeightFromTagList(String[] kerbTags, int initialValue) {
        int height = initialValue;
        for (String kerbTag : kerbTags) {
            int kerbHeightValue = convertKerbTagValueToCentimetres((String) cleanedTags.get(kerbTag));
            if (kerbHeightValue != -1) {
                height = kerbHeightValue;
            }
        }
        return height;
    }

    /**
     * 根据指定的标签键，计算应存储到图中的路缘高度。
     * 该方法查看指定道路一侧的标签
     *
     * @param kerbTags      应评估的标签键
     * @param initialValues 返回值的初始值。若未找到路缘高度信息，则返回该值
     * @return 由指定标签键导出的路缘高度值。数组的第一项
     * 用于左侧，第二项用于右侧。
     */
    private int[] calcSingleKerbHeightFromSidedTagList(String[] kerbTags, int[] initialValues) {
        int[] heights = initialValues;
        int height = -1;
        for (String kerbTag : kerbTags) {
            String[] tagValues = getSidedKerbTagValuesToApply(kerbTag);
            if (tagValues[0] != null && !tagValues[0].isEmpty()) {
                height = convertKerbTagValueToCentimetres(tagValues[0].toLowerCase());
                if (height > -1) {
                    heights[0] = height;
                }
            }
            if (tagValues[1] != null && !tagValues[1].isEmpty()) {
                height = convertKerbTagValueToCentimetres(tagValues[1].toLowerCase());
                if (height > -1) {
                    heights[1] = height;
                }
            }
        }

        return heights;
    }

    /**
     * 查看 way 并尝试为其找到正确的路缘高度。在某些情况下，路缘直接附加在 way 上，
     * 并被标记为起点和终点，因此我们需要遍历各个标签来尝试找到它们。
     *
     * @param key 我们正在调查的基础键（例如 "kerb"、"sloped_kerb" 等）
     * @return 应作为路缘高度使用的文本标签
     */
    private String[] getSidedKerbTagValuesToApply(String key) {
        // 如果查看的是路缘，有时 way 的起点和终点会用 ...:start 和 ...:end 标签标记不同的
        // 路缘高度。目前我们只想取其中较差（较高）的值
        double leftStart = -1;
        double leftEnd = -1;
        double rightStart = -1;
        double rightEnd = -1;

        String[] endValues = getSidedTagValue(key + ":end");
        // 转换
        if (endValues[0] != null && !endValues[0].isEmpty()) {
            leftEnd = convertKerbTagValueToCentimetres(endValues[0]);
        }
        if (endValues[1] != null && !endValues[1].isEmpty()) {
            rightEnd = convertKerbTagValueToCentimetres(endValues[1]);
        }
        String[] startValues = getSidedTagValue(key + ":start");
        // 转换
        if (startValues[0] != null && !startValues[0].isEmpty()) {
            leftStart = convertKerbTagValueToCentimetres(startValues[0]);
        }
        if (startValues[1] != null && !startValues[1].isEmpty()) {
            rightStart = convertKerbTagValueToCentimetres(startValues[1]);
        }

        // 现在比较以找出最差值
        String[] values = new String[2];
        if (leftEnd > leftStart)
            values[0] = endValues[0];
        else if (leftStart > leftEnd)
            values[0] = startValues[0];

        if (rightEnd > rightStart)
            values[1] = endValues[1];
        else if (rightStart > rightEnd)
            values[1] = startValues[1];

        return values;
    }

    /**
     * 比较要素左右两侧人行道针对给定属性获得的值，并找出较差的那个。当由于某种原因
     * 人行道无法作为独立边从该要素中拆出时，这很有用；此时如果某属性被视为不可通行，
     * 就会避开整条 way。
     *
     * @param attr 要评估的属性（surface、smoothness 等）
     * @return 被视为最差的值
     */
    private int getWorseAttributeValueFromSeparateItems(WheelchairAttributes.Attribute attr) {
        switch (attr) {
            case SURFACE:
                return Math.max(Math.max(wheelchairAttributesLeftSide.getSurfaceType(), wheelchairAttributesRightSide.getSurfaceType()),
                        wheelchairAttributes.getSurfaceType());
            case SMOOTHNESS:
                return Math.max(Math.max(wheelchairAttributesLeftSide.getSmoothnessType(), wheelchairAttributesRightSide.getSmoothnessType()),
                        wheelchairAttributes.getSmoothnessType());
            case KERB:
                return Math.max(Math.max(wheelchairAttributesLeftSide.getSlopedKerbHeight(), wheelchairAttributesRightSide.getSlopedKerbHeight()),
                        wheelchairAttributes.getSlopedKerbHeight());
            case WIDTH:
                // 默认值为 0，但它总会被返回，因此我们需要做一次检查
                int l = wheelchairAttributesLeftSide.getWidth();
                int r = wheelchairAttributesRightSide.getWidth();
                int w = wheelchairAttributes.getWidth();
                if (l <= 0) l = Integer.MAX_VALUE;
                if (r <= 0) r = Integer.MAX_VALUE;
                if (w <= 0) w = Integer.MAX_VALUE;

                int ret = Math.min(Math.min(l, r), w);
                if (ret == Integer.MAX_VALUE) ret = 0;

                return ret;
            case TRACK:
                return Math.max(Math.max(wheelchairAttributesLeftSide.getTrackType(), wheelchairAttributesRightSide.getTrackType()),
                        wheelchairAttributes.getTrackType());
            case INCLINE:
                return Math.max(Math.max(wheelchairAttributesLeftSide.getIncline(), wheelchairAttributesRightSide.getIncline()),
                        wheelchairAttributes.getIncline());

            default:
                return 0;
        }
    }

    /**
     * 处理由该 way 派生出的单条边，然后将其存储到 storage 中。
     *
     * @param way  父 way 要素
     * @param edge 要处理的特定边
     */
    @Override
    public void processEdge(ReaderWay way, EdgeIteratorState edge) {
        // 我们想进行复制，以免覆盖原始值，因为该边只是 way 的一部分
        WheelchairAttributes at = wheelchairAttributes.copy();

        // 获取各条边的路缘高度，因为它可能覆盖原始值
        int kerbHeight = getKerbHeightForEdge(way);
        if (kerbHeight > -1) {
            at.setSlopedKerbHeight(kerbHeight);
        }

        // 检查是否指定了该处理针对哪一侧
        if (way.hasTag("ors-sidewalk-side")) {
            String side = way.getTag("ors-sidewalk-side");
            if (side.equals(SW_VAL_LEFT)) {
                // 只获取左侧的属性
                at = getAttributes(SW_VAL_LEFT);
                at.setSide(WheelchairAttributes.Side.LEFT);
            }
            if (side.equals(SW_VAL_RIGHT)) {
                at = getAttributes(SW_VAL_RIGHT);
                at.setSide(WheelchairAttributes.Side.RIGHT);
            }
        } else {
            // 如果附加了人行道，那么我们还应考虑它们。只有当前处理未检测到
            // 实际存在的人行道时，才会走到这里……
            if (hasRightSidewalk || hasLeftSidewalk) {
                at = combineAttributesOfWayWhenBothSidesPresent(at);
            }
        }

        storage.setEdgeValues(edge.getEdge(), at);

    }

    /**
     * 按需从 way 上的节点（而非 way 自身存储的数据）获取覆盖用的路缘高度。
     * 当我们指定只在人行横道上存储路缘高度时应如此处理，因为这些要素通常
     * 不带路缘高度。
     *
     * @param way 正在调查的 way
     * @return 来自 way 上节点标签的路缘高度；若未找到/不需要路缘高度则返回 -1
     */
    int getKerbHeightForEdge(ReaderWay way) {
        int kerbHeight = -1;

        if (!kerbHeightOnlyOnCrossing || (way.hasTag(KEY_FOOTWAY) && way.getTag(KEY_FOOTWAY).equals("crossing"))) {
            // 查找路缘信息
            kerbHeight = getKerbHeightFromNodeTags();
        }

        return kerbHeight;
    }

    /**
     * 查看 way 各节点上存储的信息，并从这些数据中提取用于整条 way 的路缘高度。
     *
     * @return 由 way 上节点导出的路缘高度（厘米）
     */
    int getKerbHeightFromNodeTags() {
        // 假定路缘高度是从没有显式 :height 属性的标签获得的
        List<Integer> assumedKerbHeights = new ArrayList<>();
        // 显式高度是由 :height 标签提供的值 - 这些应优先
        List<Integer> explicitKerbHeights = new ArrayList<>();

        for (Map.Entry<Integer, Map<String, String>> entry : nodeTagsOnWay.entrySet()) {
            Map<String, String> tags = entry.getValue();
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                switch (tag.getKey()) {
                    case KEY_SLOPED_CURB, "curb", "kerb", KEY_SLOPED_KERB ->
                            assumedKerbHeights.add(convertKerbTagValueToCentimetres(tag.getValue()));
                    case KEY_KERB_HEIGHT -> explicitKerbHeights.add(convertKerbTagValueToCentimetres(tag.getValue()));
                    default -> {
                    }
                }
            }
        }
        if (!explicitKerbHeights.isEmpty()) {
            return Collections.max(explicitKerbHeights);
        } else if (!assumedKerbHeights.isEmpty()) {
            // 若有多个路缘高度，需要将最大的应用到该边，因为这是最差的情况
            return Collections.max(assumedKerbHeights);
        } else {
            return -1;
        }
    }

    /**
     * 当某条 way 两侧都标记了人行道、而我们又不想把它们作为独立项处理时，
     * 需要为每个属性取得“最差”的值并使用它。
     *
     * @param attributes 需要合并的属性存储对象
     * @return 合并后的结果对象
     */
    public WheelchairAttributes combineAttributesOfWayWhenBothSidesPresent(WheelchairAttributes attributes) {
        WheelchairAttributes at = attributes;

        int tr = getWorseAttributeValueFromSeparateItems(WheelchairAttributes.Attribute.TRACK);
        if (tr > 0) at.setTrackType(tr);

        int su = getWorseAttributeValueFromSeparateItems(WheelchairAttributes.Attribute.SURFACE);
        if (su > 0) at.setSurfaceType(su);

        int sm = getWorseAttributeValueFromSeparateItems(WheelchairAttributes.Attribute.SMOOTHNESS);
        if (sm > 0) at.setSmoothnessType(sm);

        int sl = getWorseAttributeValueFromSeparateItems(WheelchairAttributes.Attribute.KERB);
        if (sl > 0) at.setSlopedKerbHeight(sl);

        int wi = getWorseAttributeValueFromSeparateItems(WheelchairAttributes.Attribute.WIDTH);
        if (wi > 0) at.setWidth(wi);

        int in = getWorseAttributeValueFromSeparateItems(WheelchairAttributes.Attribute.INCLINE);
        if (in > 0) at.setIncline(in);

        at.setSurfaceQualityKnown(
                wheelchairAttributesLeftSide.isSurfaceQualityKnown()
                        && wheelchairAttributesRightSide.isSurfaceQualityKnown()
                        && attributes.isSurfaceQualityKnown()
        );

        at.setSuitable(
                wheelchairAttributesLeftSide.isSuitable()
                        && wheelchairAttributesRightSide.isSuitable()
                        && attributes.isSuitable()
        );

        return at;
    }

    /**
     * Get the attributes of a sidewalk on the specified side of the road
     *
     * @param side The side you want the attributes for
     * @return A WheelchairAttributes object for the side requested. If there are no attributes for the specified
     * side, then the overall attributes for the way are returned
     */
    private WheelchairAttributes getAttributes(String side) {
        WheelchairAttributes at = wheelchairAttributes.copy();

        // Now get the specific items
        switch (side) {
            case SW_VAL_LEFT -> at = at.merge(wheelchairAttributesLeftSide);
            case SW_VAL_RIGHT -> at = at.merge(wheelchairAttributesRightSide);
            default -> {
            }
        }
        return at;
    }

    /**
     * Converts a kerb height value to a numerical height (in centimetres). A kerb could be stored as an explicit height or
     * as an indicator as to whether the kerb is lowered or not.
     *
     * @param value The value of the tag
     * @return The presumed height of the kerb in metres
     */
    private int convertKerbTagValueToCentimetres(String value) {
        int centimetreHeight = -1;

        if (value == null) {
            return -1;
        }
        switch (value) {
            case "yes", KEY_BOTH, "low", "lowered", "dropped", "sloped" -> centimetreHeight = 3;
            case "no", "none", "one", "rolled", "regular" -> centimetreHeight = 15;
            case "at_grade", "flush" -> centimetreHeight = 0;
            default -> {
                double metresHeight = UnitsConverter.convertOSMDistanceTagToMeters(value);
                // If no unit was given in the tag, the value might be in meters or centimeters; we can only guess
                // depending on the value
                if (metresHeight < 0.15) {
                    centimetreHeight = (int) (metresHeight * 100);
                } else {
                    centimetreHeight = (int) metresHeight;
                }
            }
        }

        return centimetreHeight;
    }

    /**
     * Get the values obtained from a way for a specific sidewalk property. For example, providing the property
     * "surface" would check the way for the surface tag stored against attached sidewalks using the keys
     * sidewalk:left:surface, sidewalk:right:surface, and sidewalk:both:surface. The obtained values are then returned
     * in an array.
     *
     * @param property The property to be extracted
     * @return A String array containing two values - the first is the property for the left sidewalk and
     * the second is the property value for the right sidewalk.
     */
    private String[] getSidedTagValue(String property) {
        String[] values = new String[2];
        // Left side
        if (cleanedTags.containsKey("sidewalk:left:" + property))
            values[0] = (String) cleanedTags.get("sidewalk:left:" + property);
        else if (cleanedTags.containsKey("footway:left:" + property))
            values[0] = (String) cleanedTags.get("footway:left:" + property);
        // Right side
        if (cleanedTags.containsKey("sidewalk:right:" + property))
            values[1] = (String) cleanedTags.get("sidewalk:right:" + property);
        else if (cleanedTags.containsKey("footway:right:" + property))
            values[1] = (String) cleanedTags.get("footway:right:" + property);

        // Both
        if (cleanedTags.containsKey(KEY_SIDEWALK_BOTH + property)) {
            values[0] = (String) cleanedTags.get(KEY_SIDEWALK_BOTH + property);
            values[1] = (String) cleanedTags.get(KEY_SIDEWALK_BOTH + property);
        } else if (cleanedTags.containsKey(KEY_FOOTWAY_BOTH + property)) {
            values[0] = (String) cleanedTags.get(KEY_FOOTWAY_BOTH + property);
            values[1] = (String) cleanedTags.get(KEY_FOOTWAY_BOTH + property);
        }
        return values;
    }

    private int getInclineFromTagValue(String inclineValue) {
        double decimalIncline = UnitsConverter.convertOSMInclineValueToPercentage(inclineValue, true);
        decimalIncline = Math.min(decimalIncline, 15.0);
        return (int) Math.round(decimalIncline);
    }


    /**
     * Determine if the way is a separate footway object or a road feature.
     *
     * @param way The OSM way object to be assessed
     * @return Whether the way is seen as a separately drawn footway (true) or a road (false)
     */
    private boolean isSeparateFootway(ReaderWay way) {
        String type = way.getTag("highway", "");

        String[] pedestrianWayTypes = {
                "living_street",
                "pedestrian",
                KEY_FOOTWAY,
                "path",
                "crossing",
                "track"
        };

        // Check if it is a footpath or pedestrian
        if (!type.isEmpty()) {
            // We are looking at a separate footpath
            // we are looking at a road feature so any footway would be attached to it as a tag
            return Arrays.asList(pedestrianWayTypes).contains(type);
        }

        return true;
    }

    @Override
    public String getName() {
        return "Wheelchair";
    }

}
