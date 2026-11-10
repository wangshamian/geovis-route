package org.heigit.geovisroute.fastisochrones.storage;

/**
 * 用于存储一组节点 id 及其对应距离的对象。
 *
 * @author Hendrik Leuschner
 */
public class BorderNodeDistanceSet {
    int[] adjBorderNodeIds;
    double[] adjBorderNodeDistances;

    public BorderNodeDistanceSet(int[] adjBorderNodeIds, double[] adjBorderNodeDistances) {
        this.adjBorderNodeIds = adjBorderNodeIds;
        this.adjBorderNodeDistances = adjBorderNodeDistances;
    }

    public int[] getAdjBorderNodeIds() {
        return adjBorderNodeIds;
    }

    public double[] getAdjBorderNodeDistances() {
        return adjBorderNodeDistances;
    }
}
