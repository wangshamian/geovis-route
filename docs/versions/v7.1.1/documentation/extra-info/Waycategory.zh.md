# WayCategory

这些值的指数式取值方式用于[位字段（bit fields）](http://eddmann.com/posts/using-bit-flags-and-enumsets-in-java/)。一个路线段落可能属于多个类别。因此，值 ``97`` 表示同时属于 ``Paved road``、``Tunnel`` 和 ``Highway``（``64`` + ``32`` + ``1``）。

| 取值 |             名称             |
|:-----:|:----------------------------:|
| 0     | 无类别                  |
| 1     | 高速公路                      |
| 2     | 台阶                        |
| 4     | 未铺装道路                 |
| 8     | 轮渡                        |
| 16    | 小径（Track）                        |
| 32    | 隧道                       |
| 64    | 铺装道路                   |
| 128   | 浅滩                         |
