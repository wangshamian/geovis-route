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
package org.heigit.geovisroute.routing.graphhopper.extensions.storages;

import com.graphhopper.storage.*;
import com.graphhopper.util.GHUtility;
import org.heigit.geovisroute.routing.graphhopper.extensions.reader.traffic.TrafficEnums;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * 边界限制路由的图存储类
 */
public class TrafficGraphStorage implements GraphExtension {

    public enum Property {ROAD_TYPE}

    // 道路类型
    public enum RoadTypes {
        IGNORE(0),
        MOTORWAY(1),
        MOTORWAY_LINK(2),
        MOTORROAD(3),
        TRUNK(4),
        TRUNK_LINK(5),
        PRIMARY(6),
        PRIMARY_LINK(7),
        SECONDARY(8),
        SECONDARY_LINK(9),
        TERTIARY(10),
        TERTIARY_LINK(11),
        RESIDENTIAL(12),
        UNCLASSIFIED(13);

        public final byte value;

        RoadTypes(int value) {
            this.value = (byte) value;
        }
    }

    /* 道路类型的指针 */
    private static final byte LOCATION_ROAD_TYPE = 0;         // 道路类型的字节位置
    private static final int LOCATION_TRAFFIC_PRIORITY = 0;         // 正向交通 link id 的字节位置
    private static final int LOCATION_TRAFFIC = 1;         // 交通 link id 的字节位置
    private static final int LOCATION_TRAFFIC_MAXSPEED = 15; // 每周最大交通速度的字节位置
    private static final int FORWARD_OFFSET = 0;
    private static final int LOCATION_FORWARD_TRAFFIC_PRIORITY = LOCATION_TRAFFIC_PRIORITY + FORWARD_OFFSET;         // 正向交通 link id 的字节位置
    private static final int LOCATION_FORWARD_TRAFFIC = LOCATION_TRAFFIC + FORWARD_OFFSET;         // 正向交通 link id 的字节位置
    private static final int BACKWARD_OFFSET = 16;
    private static final int LOCATION_BACKWARD_TRAFFIC_PRIORITY = LOCATION_TRAFFIC_PRIORITY + BACKWARD_OFFSET;         // 反向交通 link id 的字节位置
    private static final int LOCATION_BACKWARD_TRAFFIC = LOCATION_TRAFFIC + BACKWARD_OFFSET;         // 反向交通 link id 的字节位置

    public static final int PROPERTY_BYTE_COUNT = 1;
    public static final int LINK_LOOKUP_BYTE_COUNT = 32; // 每天 2 字节。每周 7 天。一周正向、一周反向。+ 每周 1 字节表示优先级 + 正/反向最大速度 = 2 * 7 * 2 + 2 + 2 = 32
    public static final int DAILY_TRAFFIC_PATTERNS_BYTE_COUNT = 96; // 模式值会被转换为 mph 以便用字节存储。1 字节 * 4（每小时 15 分钟）* 24 小时
    public static final int MAX_DAILY_TRAFFIC_SPEED_BYTE_COUNT = 1; // 每日交通模式值的最大值

    private DataAccess orsEdgesProperties; // RAMDataAccess
    private DataAccess orsEdgesTrafficLinkLookup; // RAMDataAccess
    private DataAccess orsSpeedPatternLookup; // RAMDataAccess

    private ZoneId zoneId = ZoneId.of("Europe/Berlin");

    private int edgePropertyEntryBytes;
    private int edgeLinkLookupEntryBytes;
    private int patternEntryBytes;
    private int edgesCount; // 带自定义值的边数
    private int maxEdgeId = 0; // 有交通数据的最大边 id
    private int patternCount; // 交通模式的数量
    private final byte[] propertyValue;
    private final byte[] speedValue;
    private final byte[] priorityValue;

    public TrafficGraphStorage() {
        int edgeEntryIndex = 0;
        edgePropertyEntryBytes = edgeEntryIndex + PROPERTY_BYTE_COUNT;
        edgeLinkLookupEntryBytes = edgeEntryIndex + LINK_LOOKUP_BYTE_COUNT;
        patternEntryBytes = edgeEntryIndex + DAILY_TRAFFIC_PATTERNS_BYTE_COUNT + MAX_DAILY_TRAFFIC_SPEED_BYTE_COUNT;
        propertyValue = new byte[1];
        speedValue = new byte[1];
        priorityValue = new byte[1];
        edgesCount = 0;
    }

    public static byte getWayTypeFromString(String highway) {
        return switch (highway.toLowerCase()) {
            case "motorway" -> RoadTypes.MOTORWAY.value;
            case "motorway_link" -> RoadTypes.MOTORWAY_LINK.value;
            case "motorroad" -> RoadTypes.MOTORROAD.value;
            case "trunk" -> RoadTypes.TRUNK.value;
            case "trunk_link" -> RoadTypes.TRUNK_LINK.value;
            case "primary" -> RoadTypes.PRIMARY.value;
            case "primary_link" -> RoadTypes.PRIMARY_LINK.value;
            case "secondary" -> RoadTypes.SECONDARY.value;
            case "secondary_link" -> RoadTypes.SECONDARY_LINK.value;
            case "tertiary" -> RoadTypes.TERTIARY.value;
            case "tertiary_link" -> RoadTypes.TERTIARY_LINK.value;
            case "residential" -> RoadTypes.RESIDENTIAL.value;
            case "unclassified" -> RoadTypes.UNCLASSIFIED.value;
            default -> RoadTypes.IGNORE.value;
        };
    }

    /**
     * 根据自定义属性为边设置值<br/><br/>
     * <p>
     * 该方法接收边的内部 ID，并添加所需的值，例如 way type。
     *
     * @param edgeId 图边的内部 ID。
     * @param prop   指示值存储位置的属性。
     * @param value  包含要放到 prop 变量对应索引位置的信息的值。
     **/
    public void setOrsRoadProperties(int edgeId, Property prop, short value) {
        edgesCount++;
        ensureEdgesPropertyIndex(edgeId);
        long edgePointer = (long) edgeId * edgePropertyEntryBytes;
        if (prop == Property.ROAD_TYPE)
            propertyValue[0] = (byte) value;
        orsEdgesProperties.setBytes(edgePointer + LOCATION_ROAD_TYPE, propertyValue, 1);
    }

    /**
     * 存储所有工作日（周一至周日）的 linkID <-> patternId 匹配关系。</-><br/><br/>
     * <p>
     * 该方法接收交通边的 ID，并将工作日对应的模式 Id 添加到查找表中。
     *
     * @param edgeKey   边 key
     * @param patternId 交通模式的 Id。
     * @param weekday   表示该交通模式 Id 所属工作日的枚举值。
     **/
    public void setEdgeIdTrafficPatternLookup(int edgeKey, int patternId, TrafficEnums.WeekDay weekday, int priority) {
        if (patternId <= 0)
            return;

        priority = Math.min(priority, 255);
        patternId = patternId > 65535 ? 0 : patternId;
        int edgeId = GHUtility.getEdgeFromEdgeKey(edgeKey);
        boolean forward = isForward(edgeKey);
        if (edgeId > maxEdgeId)
            maxEdgeId = edgeId;
        ensureEdgesTrafficLinkLookupIndex(edgeId);

        int lastPriority = getEdgeIdTrafficPatternPriority(edgeId, forward);

        if (getEdgeIdTrafficPatternLookup(edgeKey, weekday) > 0 && lastPriority > priority)
            return;

        long edgePointer = (long) edgeId * edgeLinkLookupEntryBytes;

        priorityValue[0] = (byte) priority;

        if (forward) {
            orsEdgesTrafficLinkLookup.setBytes(edgePointer + LOCATION_FORWARD_TRAFFIC_PRIORITY, priorityValue, 1);
            orsEdgesTrafficLinkLookup.setShort(edgePointer + LOCATION_FORWARD_TRAFFIC + weekday.getByteLocation(), (short) patternId);
        } else {
            orsEdgesTrafficLinkLookup.setBytes(edgePointer + LOCATION_BACKWARD_TRAFFIC_PRIORITY, priorityValue, 1);
            orsEdgesTrafficLinkLookup.setShort(edgePointer + LOCATION_BACKWARD_TRAFFIC + weekday.getByteLocation(), (short) patternId);
        }
    }

    private boolean isForward(int edgeKey) {
        // 边状态与存储方向相同 -> edge key 为偶数
        // 边状态与存储方向相反 -> edge key 为奇数
        return edgeKey % 2 == 0;
    }

    /**
     * 存储 24 小时内每 15 分钟的交通模式。</-><br/><br/>
     * <p>
     * 该方法接收模式 Id，并将速度值添加到对应小时的对应刻钟位置。
     *
     * @param patternId     交通模式的 Id。
     * @param patternValues 以 mph 或 kph 表示的速度值。
     **/
    public void setTrafficPatterns(int patternId, short[] patternValues) {
        patternCount++;
        ensureSpeedPatternLookupIndex(patternId);
        // 添加条目
        int minuteCounter = 0;
        short maxValue = 0;
        for (int i = 0; i < patternValues.length; i++) {
            if (minuteCounter > 3) {
                minuteCounter = 0;
            }
            short patternValue = patternValues[i];
            if (patternValue > maxValue)
                maxValue = patternValue;
            setTrafficSpeed(patternId, patternValue, i / 4, (minuteCounter * 15));
            minuteCounter++;
        }
        setMaxTrafficSpeed(patternId, maxValue);
    }

    /**
     * 存储 24 小时内每 15 分钟的交通模式。</-><br/><br/>
     * <p>
     * 该方法接收模式 Id，并将速度值添加到对应小时的对应刻钟位置。
     *
     * @param patternId  交通模式的 Id。
     * @param speedValue 以 mph 或 kph 表示的速度值。
     * @param hour       要添加速度值的小时。
     * @param minute     要添加速度值的分钟。该值会被归一到 15 分钟步长！
     **/
    private void setTrafficSpeed(int patternId, short speedValue, int hour, int minute) {
        int minutePointer = generateMinutePointer(minute);
        long patternPointer = (long) patternId * patternEntryBytes;
        ensureSpeedPatternLookupIndex(patternId);
        speedValue = speedValue > 255 ? 255 : speedValue;
        this.speedValue[0] = (byte) speedValue;
        orsSpeedPatternLookup.setBytes(patternPointer + ((hour * 4L) + minutePointer), this.speedValue, 1);
    }

    /**
     * 存储每日交通模式中出现的最大速度值
     *
     * @param patternId     交通模式的 Id。
     * @param maxSpeedValue 以 mph 或 kph 表示的速度值。
     **/
    private void setMaxTrafficSpeed(int patternId, short maxSpeedValue) {
        long patternPointer = (long) patternId * patternEntryBytes;
        ensureSpeedPatternLookupIndex(patternId);
        maxSpeedValue = maxSpeedValue > 255 ? 255 : maxSpeedValue;
        this.speedValue[0] = (byte) maxSpeedValue;
        orsSpeedPatternLookup.setBytes(patternPointer + DAILY_TRAFFIC_PATTERNS_BYTE_COUNT, this.speedValue, 1);
    }

    /**
     * 获取在 setValueEdge 方法中为边指定的自定义值<br/><br/>
     * <p>
     * 该方法接收边的标识符，然后从存储中获取该边所请求的值
     *
     * @param edgeId 要获取值的边的内部 ID
     * @param prop   要获取的边属性（TYPE - 边界类型（0,1,2），START - 边起始所在国家的 ID，
     *               END - 边结束所在国家的 ID。
     * @return 所请求属性的值
     */
    public int getOrsRoadProperties(int edgeId, Property prop) {
        byte[] propertyValue = new byte[1];
        long edgePointer = (long) edgeId * edgePropertyEntryBytes;
        if (prop == Property.ROAD_TYPE) {
            orsEdgesProperties.getBytes(edgePointer + LOCATION_ROAD_TYPE, propertyValue, 1);
        }
        return Byte.toUnsignedInt(propertyValue[0]);
    }

    /**
     * 获取两个方向上的 edgeID <-> linkID 匹配关系。</-><br/><br/>
     * <p>
     * 该方法返回基于内部边 ID 匹配到的 linkID（若存在），包含两个方向。
     *
     * @param edgeKey 图边的内部 ID。
     * @param weekday 要获取模式的工作日枚举。
     **/
    public int getEdgeIdTrafficPatternLookup(int edgeKey, TrafficEnums.WeekDay weekday) {
        int edgeId = GHUtility.getEdgeFromEdgeKey(edgeKey);
        if (invalidEdgeId(edgeId))
            return 0;
        long edgePointer = (long) edgeId * edgeLinkLookupEntryBytes;
        if (isForward(edgeKey))
            return Short.toUnsignedInt(orsEdgesTrafficLinkLookup.getShort(edgePointer + LOCATION_FORWARD_TRAFFIC + weekday.getByteLocation()));
        else
            return Short.toUnsignedInt(orsEdgesTrafficLinkLookup.getShort(edgePointer + LOCATION_BACKWARD_TRAFFIC + weekday.getByteLocation()));
    }

    private boolean invalidEdgeId(int edgeId) {
        return (edgeId > maxEdgeId);
    }

    /**
     * 获取某个 ors 边 id 及其方向上最后保存的交通信息优先级。</-><br/><br/>
     * <p>
     * 该方法返回最后存储的优先级。这是一个自定义值，用于判断某条 ors 边当前的每周交通模式是否可以被覆盖。
     * 交通边与 ors 边的长度不一定相同。
     * 因此在匹配过程中，同一条 ors 边可能会与不同的交通边匹配多次。
     * 由于并非所有匹配都代表相同长度的 ors 边，可以认为匹配长度越大，结果越接近真实情况。
     * <p>
     * 例如：该函数可用于获取最后一次 ors 边 <-> 交通边 匹配所存储的长度。
     *
     * @param edgeId  图边的内部 ID。
     * @param forward 方向
     **/
    private int getEdgeIdTrafficPatternPriority(int edgeId, boolean forward) {
        long edgePointer = (long) edgeId * edgeLinkLookupEntryBytes;
        byte[] priority = new byte[1];
        if (forward)
            orsEdgesTrafficLinkLookup.getBytes(edgePointer + LOCATION_FORWARD_TRAFFIC_PRIORITY, priority, 1);
        else
            orsEdgesTrafficLinkLookup.getBytes(edgePointer + LOCATION_BACKWARD_TRAFFIC_PRIORITY, priority, 1);
        return Byte.toUnsignedInt(priority[0]);
    }

    /**
     * 获取所需小时的正确交通模式。</-><br/><br/>
     * <p>
     * 该方法以 15 分钟为步长，返回一小时的交通模式到 byte[] 中。
     *
     * @param patternId 图边的内部 ID。
     * @param hour      要获取模式的小时。
     * @param minute    要获取模式的分钟。
     **/
    public int getTrafficSpeed(int patternId, int hour, int minute) {
        byte[] values = new byte[1];
        int minutePointer = generateMinutePointer(minute);
        long patternPointer = (long) patternId * patternEntryBytes;
        orsSpeedPatternLookup.getBytes(patternPointer + ((hour * 4L) + minutePointer), values, 1);
        return Byte.toUnsignedInt(values[0]);
    }

    /**
     * 每日交通模式中出现的最大速度值
     **/
    private int getMaxTrafficSpeed(int patternId) {
        byte[] value = new byte[1];
        long patternPointer = (long) patternId * patternEntryBytes;
        orsSpeedPatternLookup.getBytes(patternPointer + DAILY_TRAFFIC_PATTERNS_BYTE_COUNT, value, 1);
        return Byte.toUnsignedInt(value[0]);
    }

    /**
     * 获取在 setValueEdge 方法中为边指定的自定义值<br/><br/>
     * <p>
     * 该方法接收 edgeId、基础节点与相邻节点以确定其方向，以及 unix 时间，
     * 从而找到合适的交通信息。
     * 由于目前交通信息已被归一化到该分辨率，因此只考虑工作日、小时和分钟。
     * <p>
     * ## 时间解码 ##
     * unix 时间按德国本地时间 GMT+1 解码。
     * 在收集到更多交通数据之前，我们将暂时使用德国 UTC。
     * <p>
     * <p>
     * ## TODO ##
     * - 当收集到更多交通数据时，改进内部时间编码并在整个 ORS 后端中统一。
     *
     * @param edgeKey          内部边 Key
     * @param unixMilliSeconds unix 毫秒时间。
     * @return 以 kph 表示的速度值。若未找到值则返回 -1。
     */
    public int getSpeedValue(int edgeKey, long unixMilliSeconds, int timeZoneOffset) {
        Calendar calendarDate = Calendar.getInstance(TimeZone.getTimeZone("GMT+" + timeZoneOffset));
        calendarDate.setTimeInMillis(unixMilliSeconds);
        int calendarWeekDay = calendarDate.get(Calendar.DAY_OF_WEEK);
        int hour = calendarDate.get(Calendar.HOUR_OF_DAY);
        int minute = calendarDate.get(Calendar.MINUTE);
        int patternId = getEdgeIdTrafficPatternLookup(edgeKey, TrafficEnums.WeekDay.valueOfCanonical(calendarWeekDay));
        if (patternId > 0)
            return getTrafficSpeed(patternId, hour, minute);
        return -1;
    }

    /**
     * 整周的最大交通速度值
     **/
    public int getMaxSpeedValue(int edgeKey) {
        int edgeId = GHUtility.getEdgeFromEdgeKey(edgeKey);
        if (invalidEdgeId(edgeId))
            return 0;
        byte[] value = new byte[1];
        long edgePointer = (long) edgeId * edgeLinkLookupEntryBytes;
        int directionOffset = isForward(edgeKey) ? FORWARD_OFFSET : BACKWARD_OFFSET;
        orsEdgesTrafficLinkLookup.getBytes(edgePointer + LOCATION_TRAFFIC_MAXSPEED + directionOffset, value, 1);
        return Byte.toUnsignedInt(value[0]);
    }

    public boolean hasTrafficSpeed(int edgeKey) {
        // 交通模式存储在所有工作日，因此只检查其中一个即可
        int patternId = getEdgeIdTrafficPatternLookup(edgeKey, TrafficEnums.WeekDay.MONDAY);
        // 模式 ID 从 1 开始，因此 0 被认为表示未分配模式
        return patternId > 0;
    }

    private void ensureEdgesPropertyIndex(int edgeId) {
        orsEdgesProperties.ensureCapacity(((long) edgeId + 1) * edgePropertyEntryBytes);
    }

    public void ensureEdgesTrafficLinkLookupIndex(int edgeId) {
        orsEdgesTrafficLinkLookup.ensureCapacity(((long) edgeId + 1) * edgeLinkLookupEntryBytes);
    }

    private void ensureSpeedPatternLookupIndex(int patternId) {
        orsSpeedPatternLookup.ensureCapacity(((long) patternId + 1) * patternEntryBytes);
    }

    public boolean isMatched() {
        return orsEdgesTrafficLinkLookup.getHeader(8) == 1;
    }

    public void setMatched() {
        orsEdgesTrafficLinkLookup.setHeader(8, 1);
    }

    /**
     * @return 当且仅当图的节点存储需要额外字段时为 true
     */
    public boolean isRequireNodeField() {
        return true;
    }

    /**
     * @return 当且仅当图的边存储需要额外字段时为 true
     */
    public boolean isRequireEdgeField() {
        return true;
    }

    /**
     * @return 创建节点时默认设置的字段值
     */
    public int getDefaultNodeFieldValue() {
        return -1;
    }

    /**
     * @return 创建边时默认设置的字段值
     */
    public int getDefaultEdgeFieldValue() {
        return -1;
    }

    /**
     * 通过传入基础图来初始化扩展存储
     *
     * @param graph 提供图对象。
     * @param dir   初始化图所在的目录。
     */
    @Override
    public void init(Graph graph, Directory dir) {
        if (edgesCount > 0)
            throw new AssertionError("The ORS storage must be initialized only once.");

        this.orsEdgesProperties = dir.create("ext_traffic_edge_properties");
        this.orsEdgesTrafficLinkLookup = dir.create("ext_traffic_edges_traffic_lookup");
        this.orsSpeedPatternLookup = dir.create("ext_traffic_pattern_lookup");
    }

    /**
     * 将扩展存储初始化为空 - 这是测试所需，因为 ext_storage 在运行测试时并不会创建
     */
    public void init() {
        if (edgesCount > 0)
            throw new AssertionError("The ORS storage must be initialized only once.");
        Directory d = new RAMDirectory();
        this.orsEdgesProperties = d.create("");
        this.orsEdgesTrafficLinkLookup = d.create("");
        this.orsSpeedPatternLookup = d.create("");
    }

    /**
     * @return 若从持久化存储成功加载则返回 true。
     */
    @Override
    public boolean loadExisting() {
        if (!orsEdgesProperties.loadExisting())
            throw new IllegalStateException("Unable to load storage 'ext_traffic'. corrupt file or directory?");
        if (!orsEdgesTrafficLinkLookup.loadExisting())
            throw new IllegalStateException("Unable to load storage 'ext_traffic_edges_traffic_lookup'. corrupt file or directory?");
        if (!orsSpeedPatternLookup.loadExisting())
            throw new IllegalStateException("Unable to load storage 'ext_traffic_pattern_lookup'. corrupt file or directory?");
        edgePropertyEntryBytes = orsEdgesProperties.getHeader(0);
        edgeLinkLookupEntryBytes = orsEdgesTrafficLinkLookup.getHeader(0);
        patternEntryBytes = orsSpeedPatternLookup.getHeader(0);
        edgesCount = orsEdgesProperties.getHeader(4);
        maxEdgeId = orsEdgesTrafficLinkLookup.getHeader(4);
        return true;
    }

    /**
     * 创建底层存储。若无法加载，则作为第一个操作执行。
     *
     * @param initBytes 初始大小（字节）。
     */
    @Override
    public GraphExtension create(long initBytes) {
        orsEdgesProperties.create(initBytes * edgePropertyEntryBytes);
        orsEdgesTrafficLinkLookup.create(initBytes * edgeLinkLookupEntryBytes);
        orsSpeedPatternLookup.create(initBytes * patternEntryBytes);
        return this;
    }

    /**
     * 此方法确保底层数据被写入存储。请注意，磁盘通常有 IO 缓存，
     * 因此 flush() 可能（较小的可能）无法防止断电丢失。
     */
    @Override
    public void flush() {
        orsEdgesProperties.setHeader(0, edgePropertyEntryBytes);
        orsEdgesTrafficLinkLookup.setHeader(0, edgeLinkLookupEntryBytes);
        orsSpeedPatternLookup.setHeader(0, patternEntryBytes);
        orsEdgesProperties.setHeader(4, edgesCount);
        orsEdgesTrafficLinkLookup.setHeader(4, maxEdgeId);
        orsSpeedPatternLookup.setHeader(4, patternCount);
        orsEdgesProperties.flush();
        orsEdgesTrafficLinkLookup.flush();
        orsSpeedPatternLookup.flush();
    }

    /**
     * 此方法确保释放底层使用的资源。警告：关闭时它并<b>不会</b>
     * 执行 flush！
     */
    @Override
    public void close() {
        orsEdgesProperties.close();
        orsEdgesTrafficLinkLookup.close();
        orsSpeedPatternLookup.close();
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    /**
     * @return 已分配的存储大小（字节）
     */
    @Override
    public long getCapacity() {
        return orsEdgesProperties.getCapacity() + orsEdgesTrafficLinkLookup.getCapacity() + orsSpeedPatternLookup.getCapacity();
    }


    private int generateMinutePointer(int minute) {
        if (minute < 15) {
            return 0;
        } else if (minute < 30) {
            return 1;
        } else if (minute < 45) {
            return 2;
        } else {
            return 3;
        }
    }

    /**
     * 此方法为每条边查找并存储整周范围内正向和反向的最大交通速度。
     */
    public void setMaxTrafficSpeeds() {
        int[] directionOffsets = {FORWARD_OFFSET, BACKWARD_OFFSET};

        for (int edgeId = 0; edgeId <= maxEdgeId; edgeId++) {
            long edgePointer = (long) edgeId * edgeLinkLookupEntryBytes;

            for (int directionOffset : directionOffsets) {
                int weeklyMaxSpeed = 0;

                for (TrafficEnums.WeekDay weekDay : TrafficEnums.WeekDay.values()) {
                    int patternId = Short.toUnsignedInt(orsEdgesTrafficLinkLookup.getShort(edgePointer + LOCATION_TRAFFIC + directionOffset + weekDay.getByteLocation()));
                    if (patternId == 0)
                        break;
                    int dailyMaxSpeed = getMaxTrafficSpeed(patternId);
                    if (dailyMaxSpeed > weeklyMaxSpeed)
                        weeklyMaxSpeed = dailyMaxSpeed;
                }

                this.speedValue[0] = (byte) weeklyMaxSpeed;
                orsEdgesTrafficLinkLookup.setBytes(edgePointer + LOCATION_TRAFFIC_MAXSPEED + directionOffset, this.speedValue, 1);
            }
        }
    }

    /**
     * @return 已处理的交通模式数量
     */
    public int getPatternCount() {
        return patternCount;
    }

    public void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    public ZoneId getZoneId() {
        return this.zoneId;
    }
}
