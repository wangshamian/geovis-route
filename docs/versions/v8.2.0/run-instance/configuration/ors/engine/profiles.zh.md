
# `ors.engine.profiles`

profiles 对象包含你使用的每个配置的键-对象对。

我们的标准 gr-config.yml 中有一些默认的配置键，并预定义了 flag encoder 以及推荐的配置特定设置。
这些标准配置是：

| `ors.engine.profiles.*` 中的 `*` | flag encoder `ors.engine.profiles.*.profile` |
|--------------------------------|----------------------------------------------|
| `car`                          | `driving-car`                                | 
| `hgv`                          | `driving-hgv`                                | 
| `bike-regular`                 | `cycling-regular`                            | 
| `bike-mountain`                | `cycling-mountain`                           | 
| `bike-road`                    | `cycling-road`                               | 
| `bike-electric`                | `cycling-electric`                           | 
| `walking`                      | `foot-car`                                   | 
| `hiking`                       | `foot-hgv`                                   | 
| `wheelchair`                   | `wheelchair`                                 | 
| `public-transport`             | `public-transport`                           | 

::: warning
预定义设置会覆盖你在 gr-config.yml 或 gr-config.env 的 `profile_default` 中指定的设置！
如果你想基于你自己的特定 `profile_default` 值指定自己的配置设置，可以通过给配置取不同的名称来变通，例如用 `custom-car` 代替 `car`。
请注意，配置名称可以自由选择，但不能包含你的操作系统上无法用于目录名的特殊字符。
:::

::: warning
在 directions 端点中，配置是通过其 encoder 名称（例如 `driving-car`）来引用的！
:::

每个（已启用的）配置的属性设置在 `ors.engine.profiles.<profile>` 下，例如
- `ors.engine.profiles.car`
- `ors.engine.profiles.hiking`

| key                                 | type    | description                                                                                                                                                                                                                                                                                                                                                       | default value |
|-------------------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| profile                             | string  | 用于 directions 端点的配置名称。也用作构建图时 flag encoder 的指定名称。可能的值仅限于上表第二列中的值！                                                                                                                                                                                            | _NA_          |
| enabled                             | boolean | 在**所有 openrouteservice** 端点中启用或禁用该配置                                                                                                                                                                                                                                                | `true`        |
| elevation                           | boolean | 指定是否使用高程数据                                                                                                                                                                                                                                                                               | `false`       |
| elevation_smoothing                 | boolean | 平滑高程数据                                                                                                                                                                                                                                                                                       | `false`       |
| traffic                             | boolean | 如果可用则使用交通数据                                                                                                                                                                                                                                                                             | `false`       |
| interpolate_bridges_and_tunnels     | boolean | 开启或关闭桥梁和隧道的高程插值                                                                                                                                                                                                                                                                     | `true`        |
| instructions                        | boolean | 指定是否在导入期间存储道路名称                                                                                                                                                                                                                                                                     | `true`        |
| optimize                            | boolean | 在为 CH 收缩节点时优化排序顺序。这相当耗时，但会产生更好的收缩层次结构。                                                                                                                                                                                                                           | `false`       |
| graph_path                          | string  | `ors.engine.graphs_root_path` 下的子目录名称。如果未设置，则使用 `profiles` 列表中的配置条目名称                                                                                                                                                                                                   | _NA_          |
| encoder_options                     | string  | 详情见下文 [encoder_options](#encoder_options)                                                                                                                                                                                                                                                     |               |
| preparation                         | object  | 用于构建路由图的[预处理设置](#preparation)                                                                                                                                                                                                                                                         |               |
| execution                           | object  | 查询服务时相关的[执行设置](#execution)                                                                                                                                                                                                                                                             |               |
| ext_storages                        | object  | 用于返回额外信息的[外部存储](#ext_storages)                                                                                                                                                                                                                                                        |               |
| maximum_distance                    | number  | 允许的路径总距离最大值                                                                                                                                                                                                                                                                             | `100000`      |
| maximum_distance_dynamic_weights    | number  | 使用动态权重时两个途经点之间允许的距离最大值                                                                                                                                                                                                                                                       | `100000`      |
| maximum_distance_avoid_areas        | number  | 提供规避区域时两个途经点之间允许的距离最大值                                                                                                                                                                                                                                                       | `100000`      |
| maximum_distance_alternative_routes | number  | 备选路径算法允许的路径总距离最大值                                                                                                                                                                                                                                                                 | `100000`      |
| maximum_distance_round_trip_routes  | number  | 往返路径算法允许的路径总距离最大值                                                                                                                                                                                                                                                                 | `100000`      |
| maximum_speed_lower_bound           | number  | 指定查询参数 `maximum_speed` 的阈值                                                                                                                                                                                                                                                                | `80`          |
| maximum_way_points                  | number  | 一次请求中的途经点数量上限                                                                                                                                                                                                                                                                         | `50`          |
| maximum_snapping_radius             | number  | 在给定坐标周围查找可连接边的最大距离                                                                                                                                                                                                                                                               | `400`         |
| maximum_visited_nodes               | number  | 最短路径计算中允许访问的节点数量上限                                                                                                                                                                                                                                                               | `1000000`     |
| encoder_flags_size                  | number  | 用于 FlagEncoders 的字节数                                                                                                                                                                                                                                                                         | `8`           |
| location_index_resolution           | number  | 位置索引中瓦片的最小分辨率（米）。较小的值会以更高的内存需求为代价换取更快的查询。降低分辨率会缩小查找半径，可通过增加 `location_index_search_iterations` 来补偿。对应 GraphHopper 的 `index.high_resolution` 配置参数。                                                                              | `500`         |
| location_index_search_iterations    | number  | 坐标查找中执行的最大迭代次数。较大的值会带来更广的搜索范围，但可能降低查询性能。它只影响存储查找而不影响其布局，因此修改该参数不需要重建位置索引。对应 GraphHopper 的 `index.max_region_search` 配置参数。                                                                                          | `4`           |
| force_turn_costs                    | boolean | 是否应遵守转向限制                                                                                                                                                                                                                                                                                 | `false`       |
| gtfs_file                           | string  | 仅用于 `pt` 配置：GTFS 数据的位置；可以是 zip 文件或解压后的文件夹                                                                                                                                                                                                                                  | _NA_          |

## encoder_options

`ors.engine.profiles.*.encoder_options` 下的属性：

| key                      | type    | profiles         | description                                                                                                                                                                                                                                                                                                                                                                                                      | example value |
|--------------------------|---------|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| block_fords              | boolean | *                | 不规划穿越浅滩的路线                                                                                                                                                                                                                                                                                                                                                                                             | `true`        |
| consider_elevation       | boolean | bike-*           | 最高可能速度为自行车类型特定的下坡最高速度，该速度高于通常的最高速度                                                                                                                                                                                                                                                                                                                                              | `true`        |
| maximum_grade_level      | number  | car, hgv         | 与 [tracktype](https://wiki.openstreetmap.org/wiki/Key:tracktype) 中所描述的路径质量相关。例如指定 `maximum_grade_level=1` 意味着 `tracktype=grade2` 及更差的路不会被考虑用于路由。设置 `maximum_grade_level=0` 会丢弃所有带有有效 `tracktype` 标签的路径，而诸如 `maximum_grade_level=-1` 的负值则完全禁用在这些路径上的路由。 | `3`           |
| preferred_speed_factor   | number  | wheelchair       | 被归类为适合轮椅使用者通行的边上的行驶速度将乘以该系数，用于在此类道路上设置更快的行驶速度                                                                                                                                                                                                                                                         | `1.2`         |
| problematic_speed_factor | number  | wheelchair       | 被归类为不适合轮椅使用者通行的边上的行驶速度将乘以该系数，用于在此类道路上设置较慢的行驶速度                                                                                                                                                                                                                                                       | `0.7`         |
| turn_costs               | boolean | car, hgv, bike-* | 是否应遵守转向限制                                                                                                                                                                                                                                                                                                                                                                                               | `true`        |
| use_acceleration         | boolean | car, hgv         | 模拟车辆在路段上如何加速到允许的最高速度。实际上它会降低较短路段（例如城市中邻近路口之间的路段）上的速度                                                                                                                                                                                                                                           | `true`        |

## preparation

`ors.engine.profiles.*.preparation` 下的属性：

| key                      | type   | description                                                       | default value |
|--------------------------|--------|-------------------------------------------------------------------|---------------|
| min_network_size         | number | 独立路由子图的最小规模（边数）                                     | `200`         |
| methods                  | object | 见下文                                                             |               |

### methods.ch

用于预处理收缩层次结构的设置

`ors.engine.profiles.*.preparation.methods.ch` 下的属性：

| key        | type    | description                                              | example value          |
|------------|---------|----------------------------------------------------------|------------------------|
| enabled    | boolean | 启用或禁用该路由算法                                     | `true`                 |
| threads    | number  | 用于计算预处理的并行线程数                               | `1`                    |
| weightings | string  | 以逗号分隔的权重列表                                     | `recommended,shortest` |

### methods.lm
用于使用地标进行 A* 预处理的设置

`ors.engine.profiles.*.preparation.methods.lm` 下的属性：

| key        | type    | description                                                                                                               | default value          |
|------------|---------|---------------------------------------------------------------------------------------------------------------------------|------------------------|
| enabled    | boolean | 启用或禁用该路由算法                                                                                                       | `true`                 |
| threads    | number  | 用于计算预处理的并行线程数                                                                                                 | `1`                    |
| weightings | string  | 以逗号分隔的权重列表                                                                                                       | `recommended,shortest` |
| landmarks  | number  | 预计算地标的总数；查询期间使用的子集在 `execution.methods.lm.active_landmarks` 中设置                                        | `16`                   |

### methods.core
用于使用地标进行核心路由预处理的设置

`ors.engine.profiles.*.preparation.methods.core` 下的属性：

| key        | type    | description                                                                                                                 | example value                                               |
|------------|---------|-----------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| enabled    | boolean | 启用或禁用该路由算法                                                                                                         | `true`                                                      |
| threads    | number  | 用于计算预处理的并行线程数                                                                                                   | `1`                                                         |
| weightings | string  | 以逗号分隔的权重列表                                                                                                         | `recommended,shortest`                                      |
| landmarks  | number  | 预计算地标的总数，查询期间使用的子集在 `execution.methods.core.active_landmarks` 中设置                                       | `32`                                                        |
| lmsets     | string  | 为启用特定规避过滤器而定制的各地标集                                                                                          | `highways,tollways;highways;tollways;country_193;allow_all` |

## execution

`ors.engine.profiles.*.execution` 下的属性在查询服务时相关。

### methods.astar

A* 路由算法中直线近似（beeline approximation）的参数。

`ors.engine.profiles.*.execution.methods.astar` 下的属性：

| key           | type   | description                                                                                                                                 | default value           |
|---------------|--------|---------------------------------------------------------------------------------------------------------------------------------------------|-------------------------| 
| approximation | string | 用于距离近似的方法。可以是更快的 `BeelineSimplification`，或更精确但更慢的 `BeelineAccurate`                                                 | `BeelineSimplification` |
| epsilon       | number | 用于距离近似的系数                                                                                                                          | `1`                     |

### methods.lm

在路由中使用地标的设置。

`ors.engine.profiles.*.execution.methods.lm` 下的属性：

| key               | type    | description                                      | default value |
|-------------------|---------|--------------------------------------------------|---------------| 
| active_landmarks  | number  | 用于计算路径的地标数量                           | `8`           |

### methods.core

在路由中使用地标的设置。

`ors.engine.profiles.*.execution.methods.core` 下的属性：

| key               | type    | description                                      | example value |
|-------------------|---------|--------------------------------------------------|---------------| 
| active_landmarks  | number  | 用于计算路径的地标数量                           | `6`           |

## ext_storages

可以为每个配置定义应在图中包含哪些辅助元数据。
这些信息会在路由响应中作为 `extra_info` 提供。
为此，请添加下面列表中的一个键。
将其值留空，除非你想指定更多选项（目前仅适用于
[RoadAccessRestrictions](#roadaccessrestrictions)、[Borders](#borders)、[Wheelchair](#wheelchair) 和 [HeavyVehicle](#heavyvehicle)）。

::: warning
除了在查询响应中提供信息之外，`WayCategory` 和 `Tollways` 存储中的数据还被用于通过
[`options.avoid_features`](../../../../api-reference/endpoints/directions/routing-options.md#options-avoid-features)
查询参数过滤掉某些道路，而 `Borders` 是
[`options.avoid_borders`](../../../../api-reference/endpoints/directions/routing-options.md#options-avoid-borders) 和
[`options.avoid_countries`](../../../../api-reference/endpoints/directions/routing-options.md#options-avoid-countries)
查询参数背后的功能所必需的。来自
[`options.profile_params.restrictions`](../../../../api-reference/endpoints/directions/routing-options.md#options-profile-params-restrictions)
的选项要求启用 `HeavyVehicle` 或 `Wheelchair` 存储。此外，在
[`options.vehicle_type`](../../../../api-reference/endpoints/directions/routing-options.md#options-vehicle-type) 参数中指定的 hgv 配置特定通行限制
依赖于 `HeavyVehicle` 存储。
:::

`ors.engine.profiles.*.ext_storages` 下的属性：

| key                    | type   | description                                                                                                                                      | example value                                     |
|------------------------|--------|--------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| WayCategory            | object | 在路径响应中返回道路类别，兼容任何配置类型                                                                                                        |                                                   |
| WaySurfaceType         | object | 在路径响应中返回道路表面类型，兼容任何配置类型                                                                                                    |                                                   |
| Tollways               | object | 在路径响应中返回道路收费信息，兼容驾车配置                                                                                                        |                                                   |
| Borders                | object | Borders 允许限制路径不跨越国界，兼容任何配置类型                                                                                                  | [Borders](#borders)                               |
| RoadAccessRestrictions | object | 关于道路对某些车辆或某些情形的限制信息，例如 `access=destination`                                                                                 | [RoadAccessRestrictions](#roadaccessrestrictions) |
| HeavyVehicle           | object | 重型车辆特定存储，仅与该配置兼容；它包含重量和尺寸限制以及车型通行限制                                                                             | [HeavyVehicle](#heavyvehicle)                     |
| HillIndex              | object | 在路径响应中返回爬升/下降信息，兼容任何配置类型                                                                                                   |                                                   |
| TrailDifficulty        | object | 在路径响应中返回路径难度，兼容步行和自行车配置                                                                                                    |                                                   |
| Wheelchair             | object | 轮椅特定属性，仅与该配置兼容                                                                                                                      | [Wheelchair](#wheelchair)                         |
| OsmId                  | object | 返回道路的 OsmId，仅与 wheelchair 配置兼容                                                                                                        |                                                   |

查看[此表](../../../../api-reference/endpoints/directions/extra-info/index.md#extra-info-availability)了解额外信息的可用性。
下表汇总了默认情况下哪些存储为哪个配置启用。

|             | WayCategory | WaySurfaceType | Tollways | Borders | RoadAccessRestrictions | HeavyVehicle | HillIndex | TrailDifficulty | Wheelchair | OsmId |
|:------------|:-----------:|:--------------:|:--------:|---------|:----------------------:|:------------:|:---------:|:---------------:|:----------:|:-----:|
| driving-car |      x      |       x        |    x     |         |           x            |              |           |                 |            |       |
| driving-hgv |      x      |       x        |    x     |         |                        |      x       |           |                 |            |       |
| cycling-*   |      x      |       x        |          |         |                        |              |     x     |        x        |            |       |
| foot-*      |      x      |       x        |          |         |                        |              |     x     |        x        |            |       |
| wheelchair  |      x      |       x        |          |         |                        |              |           |                 |     x      |   x   |


### RoadAccessRestrictions
`use_for_warnings` 参数告诉 ors，此存储可用于在路由响应中生成警告消息。

| key              | type    | description                                                                                                                                                                                | example value |
|------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| use_for_warnings | boolean | 每当路径经过有某种限制的道路时，响应中都会附带一条警告消息，并自动添加 roadaccessrestrictions 额外信息。                                                                                    | `true`        |

### Wheelchair

| key              | type    | description                              | example value |
|------------------|---------|------------------------------------------|---------------|
| KerbsOnCrossings | boolean | 仅在交叉口处解析路缘高度。               | `true`        |

### Borders

`ors.engine.profiles.*.ext_storages.Borders` 下的属性允许定义限制路径不跨越国界，兼容任何配置类型。

| key         | type   | description                                                                                         | example value            |
|-------------|--------|-----------------------------------------------------------------------------------------------------|--------------------------|
| boundaries  | string | 包含代表国界多边形的 geojson 文件的路径                                                             | `borders.geojson.tar.gz` |
| ids         | string | 指向 csv 文件的路径，该文件包含每个国家的唯一 id、其本地名称及其英文名称                             | `ids.csv`                |
| openborders | string | 指向 csv 文件的路径，该文件包含边界开放（即申根边界）的国家对                                        | `openborders.csv`        |

### HeavyVehicle

| key          | type    | description                                                                                                                                                                                                                                                    | example value |
|--------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| restrictions | boolean | 编码某些尺寸和重量限制，例如 `maxheight`、`maxlength`、`maxwidth`、`maxweight` 和 `maxaxleload` 等 OSM 道路标签中包含的限制。还包括由 `hazmat` 标签提供的运输危险材料的车辆的通行限制。                                                                          | `true`        |
