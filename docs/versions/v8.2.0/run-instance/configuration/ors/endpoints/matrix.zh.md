
# `ors.endpoints.matrix`

matrix 端点的设置。

| key                     | type    | description                                                                                                                      | default value                                      |
|-------------------------|---------|----------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
| enabled                 | boolean | 启用或禁用该端点                                                                                                                  | `true`                                             |
| attribution             | string  | 添加到响应元数据中的署名信息                                                                                                      | `openrouteservice.org, OpenStreetMap contributors` |
| maximum_routes          | number  | matrix 应计算的路径数量上限；例如值 `2500` 可以对应 50x50 的矩阵或 1x2500 的矩阵                                                  | `2500`                                             |
| maximum_routes_flexible | number  | 未启用 CH 路由算法的配置的路径数量上限                                                                                            | `25`                                               |
| maximum_visited_nodes   | number  | 最短路径计算中允许访问的节点数量上限                                                                                              | `100000`                                           |
| maximum_search_radius   | number  | 请求坐标与最近道路上的点之间允许的距离最大值（米）                                                                                | `2000`                                             |
| u_turn_cost             | number  | 执行掉头的惩罚；值 `-1` 会完全禁止掉头                                                                                            | `-1`                                               |
