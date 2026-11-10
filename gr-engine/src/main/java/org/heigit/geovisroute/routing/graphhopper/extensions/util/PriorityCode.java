package org.heigit.geovisroute.routing.graphhopper.extensions.util;

/**
 * 用于在边的 way flags 中存储优先级值。与
 * PriorityWeighting 配合使用
 *
 * @author Peter Karich
 */
public enum PriorityCode {
    WORST(0),
    AVOID_AT_ALL_COSTS(1),
    REACH_DEST(2),
    AVOID_IF_POSSIBLE(3),
    UNCHANGED(4),
    PREFER(5),
    VERY_NICE(6),
    BEST(7);
    private final int value;

    PriorityCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * 该方法返回取值在 0 到 1 之间的 PriorityCode.value，便于直接在 Weighting 中使用。
     */
    public static double getFactor(int val) {
        return (double) val / BEST.getValue();
    }

}
