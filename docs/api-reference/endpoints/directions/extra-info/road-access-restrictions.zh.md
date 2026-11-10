# 道路通行限制

directions 响应中以下位置取值的取值列表

```jsonpath
$.routes[*].extras.roadaccessrestrictions.values
```

提供关于道路上可能存在的限制的信息。
各取值的解释可在 [OSM Wiki 中的可能取值列表](https://wiki.openstreetmap.org/wiki/Key:access)中找到。

| 值 |             编码             |
|:-----:|:--------------------------------:|
|   0   | 无（没有任何限制） |
|   1   |                No                |
|   2   |            Customers             |
|   4   |           Destination            |
|   8   |             Delivery             |
|  16   |             Private              |
|  32   |            Permissive            |
|  64   |              Permit              |

[//]: # (keep in sync with org.heigit.ors.routing.graphhopper.extensions.AccessRestrictionType.class)
