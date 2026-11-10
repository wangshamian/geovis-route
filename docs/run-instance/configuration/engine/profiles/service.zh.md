# `ors.engine.profiles.<PROFILE-NAME>.service`

`ors.engine.profiles.<PROFILE-NAME>.service` 下的属性表示查询服务时相关的参数，
这些参数需要针对每个配置单独设置。查询时相关的更多参数可以在 [
`ors.endpoints`](/api-reference/endpoints/index.md) 部分找到。

| key                                 | type    | description                                                                                                                                                                         | default value |
|-------------------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| maximum_distance                    | number  | 一条路线允许的最大总距离                                                                                                                                       | `100000`      |
| maximum_distance_dynamic_weights    | number  | 使用动态权重时两个途经点之间允许的最大距离                                                                                                   | `100000`      |
| maximum_distance_avoid_areas        | number  | 提供要避开的区域时两个途经点之间允许的最大距离                                                                                           | `100000`      |
| maximum_distance_alternative_routes | number  | 备选路线算法路线的最大允许总距离                                                                                                  | `100000`      |
| maximum_distance_round_trip_routes  | number  | 往返算法路线的最大允许总距离                                                                                                          | `100000`      |
| maximum_way_points                  | number  | 请求中途经点的最大数量                                                                                                                                       | `50`          |
| maximum_snapping_radius             | number  | 给定坐标周围用于查找可连接边的最大距离                                                                                                                | `400`         |
| maximum_visited_nodes               | number  | 仅用于 `public-transport` 配置：最短路径计算中允许访问的最大节点数                                                                           | `1000000`     |
| force_turn_costs                    | boolean | 是否应遵守转向限制                                                                                                                                                  | `false`       |
| allow_custom_models                 | boolean | 允许在此配置上请求自定义模型。要求在该配置的[构建](build.zh.md#encoder_options)部分中将 `encoder_options.enable_custom_models` 设为 true。 | `true`        |
| execution                           | object  | 查询服务时相关的[执行设置](#execution)                                                                                                                    |               |

## `execution`

`ors.engine.profiles.<PROFILE-NAME>.service.execution` 下的属性表示特定于某些
算法的选项。

### `methods.astar`

A* 路由算法中直线（beeline）近似相关的参数。

`ors.engine.profiles.<PROFILE-NAME>.service.execution.methods.astar` 下的属性：

| key           | type   | description                                                                                                                                 | default value           |
|---------------|--------|---------------------------------------------------------------------------------------------------------------------------------------------|-------------------------|
| approximation | string | 用于距离近似的方法。可以是更快的 `BeelineSimplification`，也可以是更精确但更慢的 `BeelineAccurate` | `BeelineSimplification` |
| epsilon       | number | 用于距离近似的系数                                                                                                    | `1`                     |

### `methods.lm`

在路由中使用 landmarks 的设置。

`ors.engine.profiles.<PROFILE-NAME>.service.execution.methods.lm` 下的属性：

| key              | type   | description                                      | default value |
|------------------|--------|--------------------------------------------------|---------------|
| active_landmarks | number | 用于计算路线的 landmarks 数量 | `8`           |

### `methods.core`

使用 Core-ALT 算法在路由中使用 landmarks 的设置。

`ors.engine.profiles.<PROFILE-NAME>.service.execution.methods.core` 下的属性：

| key              | type   | description                                      | example value |
|------------------|--------|--------------------------------------------------|---------------|
| active_landmarks | number | 用于计算路线的 landmarks 数量 | `6`           |
