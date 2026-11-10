# 路面

directions 响应中以下位置取值的取值列表

```jsonpath
$.routes[*].extras.surface.values
```

该额外信息提供路线对应部分的[路面](https://wiki.openstreetmap.org/wiki/Key:surface)信息。
带删除线的取值是最近被移除的。

| 值  |       名称       |   [`surface`](https://wiki.openstreetmap.org/wiki/Key:surface) 标签的对应取值*   |
|:------:|:----------------:|:-----------------------------------------------------------------------------------------------:|
|   0    |     未知      |                                                                                                 |
|   1    |      铺装       |                                             `paved`                                             |
|   2    |     未铺装      |               `unpaved`, `woodchips`, `rock`, `rocks`, `stone`, `shells`, `salt`                |
|   3    |     沥青      |                            `asphalt`, `chipseal`, `bitmac`, `tarmac`                            |
|   4    |     混凝土     |                                      `concrete`, `cement`                                       |
| ~~5~~  | ~~鹅卵石~~  |                                                                                                 |
|   6    |      金属       |                                             `metal`                                             |
|   7    |       木材       |                                             `wood`                                              |
|   8    | 压实碎石 |                                   `compacted`, `pebblestone`                                    |
| ~~9~~  | ~~细砾石~~  |                                                                                                 |
|   10   |      砾石      |                                     `gravel`, `fine_gravel`                                     |
|   11   |       泥土       |                                     `dirt`, `earth`, `soil`                                     |
|   12   |      地面      |                                         `ground`, `mud`                                         |
|   13   |       冰        |                                          `ice`, `snow`                                          |
|   14   |  铺路石   | `paving_stones`, `paved_stones`, `sett`, `cobblestone`, `unhewn_cobblestone`, `bricks`, `brick` |
|   15   |       沙       |                                             `sand`                                              |
| ~~16~~ |  ~~木屑~~   |                                                                                                 |
|   17   |      草地       |                                             `grass`                                             |
|   18   |   植草砖    |                                          `grass_paver`                                          |

*) 对于用分号 `;` 分隔多个取值的标签，只考虑第一个取值；对于给定的 `value`，所有形如 `value[:*]` 的取值都会匹配，其中 `[:*]` 部分是可选的。例如，分别标记为 `surface=concrete`、`surface=concrete:plates;asphalt` 和 `surface=cement` 的三条道路，都会被归类为 “Concrete”。

[//]: # (keep in sync with org.heigit.ors.routing.graphhopper.extensions.SurfaceType)
