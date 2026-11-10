# 道路类型

以下为 directions 响应中下列取值对应的值列表

```jsonpath
$.routes[*].extras.waytype.values
```

该额外信息提供路线相应部分的道路类型信息。

| 取值 |     名称     |  对应的 [`highway`](https://wiki.openstreetmap.org/wiki/Key:highway)-标签 |
|:-----:|:------------:|:----------------------------------------------------------------------------------:|
|   0   |     未知     |                                                                                    |
|   1   |   州级道路   |    `primary`, `primary_link`, `motorway`, `motorway_link`, `trunk`, `trunk_link`   |
|   2   |     道路     | `secondary`, `secondary_link`, `tertiary`, `tertiary_link`, `road`, `unclassified` |
|   3   |     街道     |                      `residential`, `service`, `living_street`                     |
|   4   |     小径     |                                       `path`                                       |
|   5   |    农用道    |                                       `track`                                      |
|   6   |   自行车道   |                                     `cycleway`                                     |
|   7   |    人行道    |                         `footway`, `pedestrian`, `crossing`                        |
|   8   |     台阶     |                                       `steps`                                      |
|   9   |     轮渡     |                        `route=shuttle_train`, `route=ferry`                        |
|   10  |   在建道路   |                                   `construction`                                   |

[//]: # (keep in sync with org.heigit.ors.routing.graphhopper.extensions.WayType)
