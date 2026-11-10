# 道路类别

以下为 directions 响应中下列取值对应的值列表

```jsonpath
$.routes[*].extras.waycategory.values
```

这些取值的指数式分配用于[位字段](http://eddmann.com/posts/using-bit-flags-and-enumsets-in-java/)。一个路线段可能同时属于不同类别。因此，取值 ``97`` 表示同时属于 ``Paved road``、``Tunnel`` 和 ``Highway``（``64`` + ``32`` + ``1``）。

| 取值 |     名称    |             对应标签            |
|:-----:|:-----------:|:-------------------------------------------:|
|   0   |   无类别     |                                             |
|   1   |   高速公路   | `highway=motorway`, `highway=motorway_link` |
|   2   |   收费道路   |                 `toll*=yes`                 |
|   4   |    台阶      |               `highway=steps`               |
|   8   |    轮渡      |     `route=shuttle_train`, `route=ferry`    |
|   16  |   过水路面   |                  `ford=yes`                 |

[//]: # (keep in sync with org.heigit.ors.routing.graphhopper.extensions.storages.builders.WayCategoryGraphStorageBuilder)
