package org.heigit.geovisroute.routing.util;

import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.PointList;

import java.util.List;

public class SteepnessUtil {

    public static final double ELEVATION_THRESHOLD = 20;

    private SteepnessUtil() {
        throw new IllegalStateException("Utility class should not be instantiated.");
    }

    public static int getCategory(double value) {

        //与文档保持同步：steepness.md

        if (Double.isNaN(value))
            return 0;

        double absValue = Math.abs(value);
        int res = 0;

        // 0%：平坦道路
        if (absValue < 1.0)
            res = 0;
            // 1-3%：略有上坡，但挑战性不大。有点像逆风骑行。
        else if (absValue >= 1 && absValue < 4)
            res = 1;
            // 4-6%：坡度可控，但长时间会累积疲劳。
        else if (absValue >= 4 && absValue < 7)
            res = 2;
            // 7-9%：对经验丰富的骑行者开始变得不适，对新手爬坡者很有挑战。
        else if (absValue >= 7 && absValue < 10)
            res = 3;
            // 10%-15%：痛苦的坡度，尤其是持续一段时间时
        else if (absValue >= 10 && absValue < 16)
            res = 4;
            // 16%+：对所有水平的骑行者都极具挑战。长时间保持这种坡度非常痛苦。
        else if (absValue >= 16)
            res = 5;

        return res * ((value > 0) ? 1 : -1);
    }

    public static void computeRouteSplits(PointList points, boolean reverse, DistanceCalc dc, List<RouteSplit> splits) {
        splits.clear();

        if (points.size() == 0)
            return;

        int nPoints = points.size();
        int i0 = reverse ? nPoints - 1 : 0;
        double maxAltitude = Double.MIN_VALUE;
        double minAltitude = Double.MAX_VALUE;
        double prevMinAltitude;
        double prevMaxAltitude;
        int iStart = i0;
        double splitLength = 0;
        double cumElev = 0.0;
        RouteSplit prevSplit = null;
        int prevGC = 0;
        int iPoints = 0;

        double x0 = points.getLon(i0);
        double y0 = points.getLat(i0);
        double z0 = points.getEle(i0);

        if (z0 > maxAltitude)
            maxAltitude = z0;
        if (z0 < minAltitude)
            minAltitude = z0;

        for (int j = 1; j < nPoints; j++) {

            int jj = reverse ? (nPoints - 1 - j) : j;
            double x1 = points.getLon(jj);
            double y1 = points.getLat(jj);
            double z1 = points.getEle(jj);

            double elevDiff = z1 - z0;
            double length = dc.calcDist(y0, x0, y1, x1);
            cumElev += elevDiff;

            splitLength += length;

            prevMinAltitude = minAltitude;
            prevMaxAltitude = maxAltitude;
            if (z1 > maxAltitude)
                maxAltitude = z1;
            if (z1 < minAltitude)
                minAltitude = z1;

            if (maxAltitude - z1 > ELEVATION_THRESHOLD || z1 - minAltitude > ELEVATION_THRESHOLD) {
                boolean bApply = true;
                int elevSign = cumElev > 0 ? 1 : -1;
                double gradient = elevSign * 100 * (prevMaxAltitude - prevMinAltitude) / splitLength;
                if (Double.isNaN(gradient) || Math.abs(gradient) > 30) // 可能是噪声
                    gradient = 0.0;

                if (prevGC != 0) {
                    double zn = Double.MIN_NORMAL;

                    if (!reverse) {
                        if (jj + 1 < nPoints)
                            zn = points.getEle(jj + 1);
                    } else {
                        if (jj - 1 >= 0)
                            zn = points.getEle(jj - 1);
                    }

                    if (zn != Double.MIN_VALUE) {
                        double elevGap = length / 30;
                        if (
                                (
                                        elevSign > 0 && prevGC > 0 || prevGC < 0
                                )
                                        && Math.abs(zn - z1) < elevGap) {
                            bApply = false;
                        }
                    }
                }

                if (bApply) {
                    int iEnd = reverse ? (nPoints - iPoints - 1) : (iPoints - 1);

                    int gc = getCategory(gradient);
                    RouteSplit split = new RouteSplit();
                    split.value = gc;
                    split.gradient = gradient;
                    split.length = splitLength;
                    split.verticalClimb = prevMaxAltitude - prevMinAltitude;

                    if (reverse) {
                        split.start = iEnd;
                        split.end = iStart;
                        splits.add(0, split);
                    } else {
                        split.start = iStart;
                        split.end = iEnd;
                        splits.add(split);
                    }

                    prevGC = gc;
                    prevSplit = split;

                    iStart = iEnd;
                    minAltitude = Math.min(z0, z1);
                    maxAltitude = Math.max(z0, z1);
                    splitLength = 0.0;

                    cumElev = elevDiff;
                }
            }

            x0 = x1;
            y0 = y1;
            z0 = z1;
            iPoints++;
        }

        if (splitLength > 0) {
            int iEnd = reverse ? (nPoints - iPoints - 1) : (iPoints - 1);
            double elevDiff = maxAltitude - minAltitude;
            if (splits.isEmpty() && splitLength < 50 && elevDiff < ELEVATION_THRESHOLD)
                elevDiff = 0;
            double gradient = (cumElev > 0 ? 1 : -1) * 100 * elevDiff / splitLength;
            if (Math.abs(gradient) > 7 && maxAltitude < 100 && splitLength < 120)
                gradient = 0.0; //  噪声

            int gc = getCategory(gradient);
            if (prevSplit != null && (prevSplit.value == gc || splitLength < 25)) {
                prevSplit.end = iEnd;
            } else {
                RouteSplit lastSplit = new RouteSplit();
                lastSplit.value = gc;
                lastSplit.gradient = gradient;
                lastSplit.length = splitLength;
                lastSplit.verticalClimb = elevDiff;

                if (reverse) {
                    lastSplit.start = iEnd;
                    lastSplit.end = iStart;
                    splits.add(0, lastSplit);
                } else {
                    lastSplit.start = iStart;
                    lastSplit.end = iEnd;
                    splits.add(lastSplit);
                }
            }
        }
    }
}
