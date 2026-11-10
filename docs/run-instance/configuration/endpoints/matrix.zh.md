
# `ors.endpoints.matrix`

矩阵端点的设置。

| key                     | type    | description                                                                                                                      | default value                                      |
|-------------------------|---------|----------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
| enabled                 | boolean | 启用或禁用该端点                                                                                                | `true`                                             |
| attribution             | string  | 添加到响应元数据中的署名信息                                                                                       | `openrouteservice.org, OpenStreetMap contributors` |
| maximum_routes          | number  | 矩阵应计算的最大路线数量；例如值 `2500` 可以对应 50x50 矩阵或 1x2500 矩阵 | `2500`                                             |
| maximum_routes_flexible | number  | 对于未启用 CH 路由算法的配置所允许的最大路线数量                                          | `25`                                               |
| maximum_visited_nodes   | number  | 最短路径计算中允许访问的最大节点数                                                             | `100000`                                           |
| maximum_search_radius   | number  | 请求坐标与最近道路上某点之间允许的最大距离（米）                              | `2000`                                             |
| u_turn_costs            | number  | 执行 U 形转弯的惩罚；值 `-1` 会完全禁止 U 形转弯                                                         | `-1`                                               |
