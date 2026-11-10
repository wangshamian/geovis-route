# 道路类型速度
道路类型对应 OSM [highway](https://wiki.openstreetmap.org/wiki/Key:highway) 标签值。
此表汇总了[速度值文件][svf]中的值。

_（所有值单位为 km/h）_

  | 道路类型 \ 配置 ->   | driving-car | driving-hgv | cycling-regular |
  |:--------------------:|:-----------:|:-----------:|:---------------:|
  |       motorway       |     100     |     85      |       18        |
  |    motorway_link     |     60      |     50      |       18        |
  |      motorroad       |     90      |     80      |        -        |
  |        trunk         |     85      |     60      |       18        |
  |      trunk_link      |     60      |     50      |       18        |
  |       primary        |     65      |     60      |       18        |
  |     primary_link     |     50      |     50      |       18        |
  |      secondary       |     60      |     60      |       18        |
  |    secondary_link    |     50      |     50      |       18        |
  |       tertiary       |     50      |     50      |       18        |
  |    tertiary_link     |     40      |     40      |       18        |
  |     unclassified     |     30      |     30      |       16        |
  |     residential      |     30      |     30      |       18        |
  |    living_street     |     10      |     10      |        6        |
  |       service        |     20      |     20      |       14        |
  |         road         |     20      |     20      |       12        |
  |        track         |     15      |     15      |       12        |
  |         path         |      -      |      -      |       10        |
  |       cycleway       |      -      |      -      |       18        |
  |       footway        |      -      |      -      |        6        |
  |      pedestrian      |      -      |      -      |        6        |
  |      bridleway       |      -      |      -      |        6        |
  |        steps         |      -      |      -      |        2        |
  |     construction     |      -      |      -      |       10        |

[svf]: https://github.com/GIScience/openrouteservice/tree/main/ors-engine/src/main/resources/resources/services/routing/speed_limits
