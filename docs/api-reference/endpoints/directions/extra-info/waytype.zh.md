# WayType

directions 响应中以下位置取值的取值列表

```jsonpath
$.routes[*].extras.waytype.values
```

该额外信息提供路线对应部分道路类型的信息。

| 值 |     名称     |  对应的 [`highway`](https://wiki.openstreetmap.org/wiki/Key:highway) 标签 |
|:-----:|:------------:|:----------------------------------------------------------------------------------:|
|   0   |    未知   |                                                                                    |
|   1   |  州道  |    `primary`, `primary_link`, `motorway`, `motorway_link`, `trunk`, `trunk_link`   |
|   2   |     道路     | `secondary`, `secondary_link`, `tertiary`, `tertiary_link`, `road`, `unclassified` |
|   3   |    街道    |                      `residential`, `service`, `living_street`                     |
|   4   |     小径     |                                       `path`                                       |
|   5   |     土路    |                                       `track`                                      |
|   6   |   自行车道   |                                     `cycleway`                                     |
|   7   |    人行道    |                         `footway`, `pedestrian`, `crossing`                        |
|   8   |     台阶    |                                       `steps`                                      |
|   9   |     渡轮    |                        `route=shuttle_train`, `route=ferry`                        |
|   10  | 施工道路 |                                   `construction`                                   |

[//]: # (keep in sync with org.heigit.ors.routing.graphhopper.extensions.WayType)
