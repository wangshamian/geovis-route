# 路面

以下为 directions 响应中下列取值对应的值列表

```jsonpath
$.routes[*].extras.surface.values
```

该额外信息提供路线相应部分的[路面](https://wiki.openstreetmap.org/wiki/Key:surface)信息。
带删除线的取值最近已被移除。

| 取值  |       名称       |   对应的 value* [`surface`](https://wiki.openstreetmap.org/wiki/Key:surface)-标签   |
|:------:|:----------------:|:-----------------------------------------------------------------------------------------------:|
|   0    |       未知       |                                                                                                 |
|   1    |      铺装路面    |                                             `paved`                                             |
|   2    |     未铺装路面   |               `unpaved`, `woodchips`, `rock`, `rocks`, `stone`, `shells`, `salt`                |
|   3    |       沥青       |                            `asphalt`, `chipseal`, `bitmac`, `tarmac`                            |
|   4    |       混凝土     |                                      `concrete`, `cement`                                       |
| ~~5~~  | ~~鹅卵石路面~~   |                                                                                                 |
|   6    |       金属       |                                             `metal`                                             |
|   7    |       木质       |                                             `wood`                                              |
|   8    |     压实砾石     |                                   `compacted`, `pebblestone`                                    |
| ~~9~~  | ~~细砾石路面~~   |                                                                                                 |
|   10   |       砾石       |                                     `gravel`, `fine_gravel`                                     |
|   11   |       泥土       |                                     `dirt`, `earth`, `soil`                                     |
|   12   |       地面       |                                         `ground`, `mud`                                         |
|   13   |       冰         |                                          `ice`, `snow`                                          |
|   14   |      铺路石      | `paving_stones`, `paved_stones`, `sett`, `cobblestone`, `unhewn_cobblestone`, `bricks`, `brick` |
|   15   |       沙地       |                                             `sand`                                              |
| ~~16~~ | ~~木屑路面~~     |                                                                                                 |
|   17   |       草地       |                                             `grass`                                             |
|   18   |      植草砖      |                                          `grass_paver`                                          |

*) 对于用分号 `;` 分隔多个取值的标签，只考虑第一个取值；对于给定的 `value`，
会匹配所有形式为 `value[:*]` 的取值，其中 `[:*]` 部分是可选的。例如，分别标有
`surface=concrete`、`surface=concrete:plates;asphalt` 和 `surface=cement` 的三条道路，
都会被归类为“混凝土”。

[//]: # (keep in sync with org.heigit.ors.routing.graphhopper.extensions.SurfaceType)
