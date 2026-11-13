package com.graphhopper.offroad;

/**
 * 公路信息提供者：混合路由时用于判断网格是否落在公路上，并给出该处公路的通行速度。
 */
public interface RoadAccessProvider {

    /**
     * 查询给定坐标附近的公路。
     *
     * @return 附近没有可通行公路时返回 null
     */
    RoadAccess getRoad(double lat, double lon);

    record RoadAccess(double speedKmh) {
    }
}
