# 道路通行限制

以下为 directions 响应中下列取值对应的值列表

```jsonpath
$.routes[*].extras.roadaccessrestrictions.values
```

提供关于道路上可能存在的通行限制的信息。
各取值的说明可以在 [OSM Wiki 中的可能取值列表](https://wiki.openstreetmap.org/wiki/Key:access)中找到

| 取值 |             编码              |
|:-----:|:---------------------------------:|
|   0   | 无（没有任何限制）                |
|   1   | 禁止进入                          |
|   2   | 仅限客户                          |
|   4   | 仅限目的地                        |
|   8   | 仅限送货                          |
|  16   | 私人                              |
|  32   | 允许通行                          |

[//]: # (keep in sync with org.heigit.ors.routing.graphhopper.extensions.AccessRestrictionType.class)
