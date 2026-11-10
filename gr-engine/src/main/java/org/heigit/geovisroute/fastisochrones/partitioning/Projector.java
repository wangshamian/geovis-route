package org.heigit.geovisroute.fastisochrones.partitioning;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntHashSet;
import com.graphhopper.storage.BaseGraph;
import org.heigit.geovisroute.fastisochrones.Contour;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.heigit.geovisroute.fastisochrones.partitioning.FastIsochroneParameters.getSplitValue;
import static org.heigit.geovisroute.fastisochrones.partitioning.Projector.Projection.*;

public class Projector {
    protected static Map<Projection, Projection> correspondingProjMap;
    private BaseGraph graph;
    private static final double TAN_67_5 = 2.414213;
    private static final double TAN_45 = 1;
    private static final double TAN_22_5 = 0.414213;


    public Projector() {
        prepareProjectionMaps();
    }

    private static void prepareProjectionMaps() {
        correspondingProjMap = new EnumMap<>(Projection.class);
        correspondingProjMap.put(LINE_P90, LINE_M00);
        correspondingProjMap.put(LINE_P675, LINE_M225);
        correspondingProjMap.put(LINE_P45, LINE_M45);
        correspondingProjMap.put(LINE_P225, LINE_M675);
        correspondingProjMap.put(LINE_M00, LINE_P90);
        correspondingProjMap.put(LINE_M225, LINE_P675);
        correspondingProjMap.put(LINE_M45, LINE_P45);
        correspondingProjMap.put(LINE_M675, LINE_P225);
    }

    protected Map<Projection, IntArrayList> calculateProjections() {
        //>> 遍历线性组合并对每个节点进行投影
        EnumMap<Projection, IntArrayList> nodeListProjMap = new EnumMap<>(Projection.class);
        Integer[] ids = IntStream.rangeClosed(0, graph.getNodes() - 1).boxed().toArray(Integer[]::new);

        Double[] values = new Double[ids.length];
        Sort sort = new Sort();
        for (Projection proj : Projection.values()) {
            //>> 对投影后的节点排序
            for (int i = 0; i < ids.length; i++) {
                values[i] = proj.sortValue(graph.getNodeAccess().getLat(ids[i]), graph.getNodeAccess().getLon(ids[i]));
            }
            nodeListProjMap.put(proj, sort.sortByValueReturnList(ids, values));
        }
        //检查是否至少有两个不同的投影。如果未设置经纬度数据，则所有投影都相同。
        boolean valid = false;
        for (Projection proj : Projection.values()) {
            if (!nodeListProjMap.get(proj).equals(nodeListProjMap.get(LINE_M00)))
                valid = true;
        }
        if (!valid)
            throw new IllegalStateException("All projections of the graph are the same. Maybe NodeAccess is faulty or not initialized?");
        return nodeListProjMap;
    }

    protected BiPartitionProjection partitionProjections(Map<Projection, IntArrayList> originalProjections, BiPartition biPartition) {
        IntHashSet part0 = biPartition.getPartition(0);
        EnumMap<Projection, IntArrayList> projections0 = new EnumMap<>(Projection.class);
        EnumMap<Projection, IntArrayList> projections1 = new EnumMap<>(Projection.class);
        int origNodeCount = originalProjections.get(Projection.values()[0]).size();
        //添加初始列表
        for (Projection proj : Projection.values()) {
            projections0.put(proj, new IntArrayList(origNodeCount / 3));
            projections1.put(proj, new IntArrayList(origNodeCount / 3));
        }

        //遍历原始投影，将每个投影拆分为对应两个子集的投影，并保持顺序
        for (int i = 0; i < origNodeCount; i++) {
            for (Map.Entry<Projection, IntArrayList> proj : originalProjections.entrySet()) {
                int node = proj.getValue().get(i);
                if (part0.contains(node))
                    projections0.get(proj.getKey()).add(node);
                else
                    projections1.get(proj.getKey()).add(node);
            }
        }

        return new BiPartitionProjection(projections0, projections1);
    }

    protected List<Projection> calculateProjectionOrder(Map<Projection, IntArrayList> projections) {
        List<Projection> order;
        EnumMap<Projection, Double> squareRangeProjMap = new EnumMap<>(Projection.class);
        EnumMap<Projection, Double> orthogonalDiffProjMap = new EnumMap<>(Projection.class);
        //>> 计算投影距离
        for (Map.Entry<Projection, IntArrayList> proj : projections.entrySet()) {
            int idx = (int) (proj.getValue().size() * getSplitValue());
            squareRangeProjMap.put(proj.getKey(), projIndividualValue(projections, proj.getKey(), idx));
        }

        //>> 合并逆投影距离
        for (Projection proj : projections.keySet()) {
            orthogonalDiffProjMap.put(proj, projCombinedValue(squareRangeProjMap, proj));
        }

        //>> 按投影值对投影排序
        Sort sort = new Sort();
        order = sort.sortByValueReturnList(orthogonalDiffProjMap, false);
        return order;
    }

    private double projIndividualValue(Map<Projection, IntArrayList> projMap, Projection proj, int idx) {
        IntArrayList tmpNodeList = projMap.get(proj);
        double toLat = graph.getNodeAccess().getLat(tmpNodeList.get(idx));
        double toLon = graph.getNodeAccess().getLon(tmpNodeList.get(idx));
        double fromLat = graph.getNodeAccess().getLat(tmpNodeList.get(tmpNodeList.size() - idx - 1));
        double fromLon = graph.getNodeAccess().getLon(tmpNodeList.get(tmpNodeList.size() - idx - 1));

        return Contour.distance(fromLat, toLat, fromLon, toLon);
    }

    private double projCombinedValue(Map<Projection, Double> squareRangeProjMap, Projection proj) {
        return squareRangeProjMap.get(proj) * squareRangeProjMap.get(proj) / squareRangeProjMap.get(correspondingProjMap.get(proj));
    }

    public void setGraph(BaseGraph graph) {
        this.graph = graph;
    }

    public enum Projection {
        LINE_P90 {
            public double sortValue(double lat, double lon) {
                return lat;
            }
        },
        LINE_P675 {
            public double sortValue(double lat, double lon) {
                return lat + TAN_67_5 * lon;
            }
        },
        LINE_P45 {
            public double sortValue(double lat, double lon) {
                return lat + TAN_45 * lon;
            }
        },
        LINE_P225 {
            public double sortValue(double lat, double lon) {
                return lat + TAN_22_5 * lon;
            }
        },
        LINE_M00 {
            public double sortValue(double lat, double lon) {
                return lon;
            }
        },
        LINE_M225 {
            public double sortValue(double lat, double lon) {
                return lat - TAN_22_5 * lon;
            }
        },
        LINE_M45 {
            public double sortValue(double lat, double lon) {
                return lat - TAN_45 * lon;
            }
        },
        LINE_M675 {
            public double sortValue(double lat, double lon) {
                return lat - TAN_67_5 * lon;
            }
        };

        abstract double sortValue(double lat, double lon);
    }
}
