# WayCategory

directions 响应中以下位置取值的取值列表

```jsonpath
$.routes[*].extras.waycategory.values
```

取值的指数式分配用于[位字段](http://eddmann.com/posts/using-bit-flags-and-enumsets-in-java/)。一个路线区段可能属于多个类别。因此，取值 `3` 表示同时属于 `Tollways` 和 `Highway`（`2` + `1`）。

| 值 |     名称    |             对应标签            |
|:-----:|:-----------:|:-------------------------------------------:|
|   0   | 无类别 |                                             |
|   1   |   高速公路   | `highway=motorway`, `highway=motorway_link` |
|   2   |   收费道路  |                 `toll*=yes`                 |
|   4   |    台阶    |               `highway=steps`               |
|   8   |    渡轮    |     `route=shuttle_train`, `route=ferry`    |
|   16  |     浅滩    |                  `ford=yes`                 |

[//]: # (keep in sync with org.heigit.ors.routing.graphhopper.extensions.storages.builders.WayCategoryGraphStorageBuilder)
