package org.heigit.geovisroute.fastisochrones.partitioning;

/**
 * 快速等时圈算法预处理和查询处理的参数。
 * 部分默认值可通过 gr-config.json 修改
 *
 * @author Hendrik Leuschner
 */
public final class FastIsochroneParameters {
    //名称
    public static final String ACTIVECELLDIJKSTRA = "ActiveCellDijkstra";
    public static final String CORERANGEDIJKSTRA = "CoreRangeDijkstra";
    public static final String RANGEDIJKSTRA = "RangeDijkstra";

    //全局
    private static int maxThreadCount = 12;
    private static boolean log = true;
    //分区
    //基于 Implementierung eines Algorithmus zur schnellen Berechnung metrik-affiner Isochronen in einem Straßennetzwerk by Stefan Panig, 2019
    private static int maxCellNodesNumber = 5000;
    private static int minCellNodesNumber = 1;
    //基于 Aaron Schild & Christian Sommer. On Balanced Seperators in Road Networks, Springer 的系数
    //International Publishing Switzerland, 2015 and
    //Implementierung eines Algorithmus zur schnellen Berechnung metrik-affiner Isochronen in einem Straßennetzwerk by Stefan Panig, 2019
    private static double splitValue = 0.2525;
    //等值线 + 偏心率
    private static boolean enableSuperCells = true;

    private FastIsochroneParameters() {
    }

    public static int getMaxThreadCount() {
        return maxThreadCount;
    }

    public static void setMaxThreadCount(int threads) {
        maxThreadCount = threads;
    }

    public static boolean isLogEnabled() {
        return log;
    }

    public static void setLogEnabled(boolean log) {
        FastIsochroneParameters.log = log;
    }

    public static int getMaxCellNodesNumber() {
        return maxCellNodesNumber;
    }

    public static void setMaxCellNodesNumber(int maxCellNodesNumber) {
        FastIsochroneParameters.maxCellNodesNumber = maxCellNodesNumber;
    }

    public static int getMinCellNodesNumber() {
        return minCellNodesNumber;
    }

    public static void setMinCellNodesNumber(int minCellNodesNumber) {
        FastIsochroneParameters.minCellNodesNumber = minCellNodesNumber;
    }

    public static double getSplitValue() {
        return splitValue;
    }

    public static void setSplitValue(double splitValue) {
        FastIsochroneParameters.splitValue = splitValue;
    }

    public static boolean isSupercellsEnabled() {
        return enableSuperCells;
    }

    public static void setEnableSupercells(boolean enableSupercells) {
        enableSuperCells = enableSupercells;
    }
}
