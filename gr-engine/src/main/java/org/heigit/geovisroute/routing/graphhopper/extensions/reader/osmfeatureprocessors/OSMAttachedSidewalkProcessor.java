package org.heigit.geovisroute.routing.graphhopper.extensions.reader.osmfeatureprocessors;

import com.graphhopper.reader.ReaderWay;

import java.util.HashSet;
import java.util.Set;

public class OSMAttachedSidewalkProcessor {

    public static final String KEY_ORS_SIDEWALK_SIDE = "ors-sidewalk-side";
    public static final String VAL_RIGHT = "right";
    public static final String VAL_LEFT = "left";

    protected enum Side {LEFT, RIGHT, BOTH, NONE}

    public boolean hasSidewalkInfo(ReaderWay way) {
        return identifySidesWhereSidewalkIsPresent(way) != Side.NONE;
    }

    /**
     * 获取表示人行道信息的键
     *
     * @param way
     * @return
     */
    protected Set<String> getSidewalkKeys(ReaderWay way) {
        Set<String> sidewalkInfoKeys = new HashSet<>();

        Set<String> keys = way.getTags().keySet();
        for (String k : keys) {
            if (isSidewalkInfoKey(k)) {
                sidewalkInfoKeys.add(k);
            }
        }

        return sidewalkInfoKeys;
    }

    /**
     * 判断指定的键是否可能包含人行道信息
     *
     * @param osmTagKey
     * @return
     */
    private boolean isSidewalkInfoKey(String osmTagKey) {
        return osmTagKey.startsWith("sidewalk:") || osmTagKey.startsWith("footway:");
    }

    /**
     * 向 way 添加一个 tag，用于标明需要处理道路的哪一侧人行道
     *
     * @param way
     * @param side
     * @return
     */
    public ReaderWay attachSidewalkTag(ReaderWay way, Side side) {
        switch (side) {
            case LEFT:
                way.setTag(KEY_ORS_SIDEWALK_SIDE, VAL_LEFT);
                break;
            case RIGHT:
                way.setTag(KEY_ORS_SIDEWALK_SIDE, VAL_RIGHT);
                break;
            case BOTH:
                if (way.hasTag(KEY_ORS_SIDEWALK_SIDE) && way.getTag(KEY_ORS_SIDEWALK_SIDE).equalsIgnoreCase(VAL_LEFT)) {
                    // 之前已附加左侧，因此现在附加右侧
                    way.setTag(KEY_ORS_SIDEWALK_SIDE, VAL_RIGHT);
                } else {
                    // 从左侧开始
                    way.setTag(KEY_ORS_SIDEWALK_SIDE, VAL_LEFT);
                }
                break;
            case NONE:
                if (way.hasTag(KEY_ORS_SIDEWALK_SIDE)) {
                    way.removeTag(KEY_ORS_SIDEWALK_SIDE);
                }
        }

        return way;
    }

    /**
     * 根据所附加的 tag 判断需要处理哪一侧人行道。
     *
     * @param way
     * @return
     */
    public Side getPreparedSide(ReaderWay way) {
        if (way.hasTag(KEY_ORS_SIDEWALK_SIDE)) {
            String preparedSide = way.getTag(KEY_ORS_SIDEWALK_SIDE);
            if (preparedSide.equalsIgnoreCase(VAL_LEFT)) {
                return Side.LEFT;
            }
            if (preparedSide.equalsIgnoreCase(VAL_RIGHT)) {
                return Side.RIGHT;
            }
        }

        return Side.NONE;
    }

    /**
     * 根据 OSM 中赋予的 tags 判断道路上哪一侧存在人行道
     *
     * @param osmWay
     * @return
     */
    protected Side identifySidesWhereSidewalkIsPresent(ReaderWay osmWay) {
        boolean sidewalkOnLeftSide = false;
        boolean sidewalkOnRightSide = false;
        boolean sidewalkOnBothSides = false;

        if (osmWay.hasTag("sidewalk")) {
            String side = osmWay.getTag("sidewalk");
            switch (side) {
                case VAL_LEFT -> sidewalkOnLeftSide = true;
                case VAL_RIGHT -> sidewalkOnRightSide = true;
                case "both" -> sidewalkOnBothSides = true;
                default -> {
                }
            }
        }

        Set<String> sidewalkProperties = getSidewalkKeys(osmWay);

        for (String key : sidewalkProperties) {
            if (key.startsWith("sidewalk:left") || key.startsWith("footway:left")) sidewalkOnLeftSide = true;
            if (key.startsWith("sidewalk:right") || key.startsWith("footway:right")) sidewalkOnRightSide = true;
            if (key.startsWith("sidewalk:both") || key.startsWith("footway:both")) sidewalkOnBothSides = true;
        }

        if (sidewalkOnLeftSide && sidewalkOnRightSide) {
            sidewalkOnBothSides = true;
        }

        if (sidewalkOnBothSides) {
            return Side.BOTH;
        }

        if (sidewalkOnLeftSide) {
            return Side.LEFT;
        }

        if (sidewalkOnRightSide) {
            return Side.RIGHT;
        }

        return Side.NONE;
    }
}
