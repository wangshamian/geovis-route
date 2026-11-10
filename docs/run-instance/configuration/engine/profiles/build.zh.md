# `ors.engine.profiles.<PROFILE-NAME>.build`

`ors.engine.profiles.<PROFILE-NAME>.build` 下的属性用于为指定配置定义构建路由
图的参数。

| key                              | type    | description                                                                                                                                                                                                                                                                                                                                                       | default value                                |
|----------------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|
| source_file                      | string  | 要使用的 OSM 文件，支持的格式有 `.osm`、`.osm.gz`、`.osm.zip` 和 `.pbf`                                                                                                                                                                                                                                                                           | `ors-api/src/test/files/heidelberg.test.pbf` |
| elevation                        | boolean | 指定是否下载并使用高程数据。如果为 true，还必须在 ors.engine.elevation 中设置 `cache_path` 和 `provider`。                                                                                                                                                                                                                           | `false`                                      |
| elevation_smoothing              | boolean | 平滑高程数据                                                                                                                                                                                                                                                                                                                                         | `false`                                      |
| traffic                          | boolean | 在可用时使用交通数据                                                                                                                                                                                                                                                                                                                                     | `false`                                      |
| interpolate_bridges_and_tunnels  | boolean | 切换桥梁和隧道的高程插值                                                                                                                                                                                                                                                                                                  | `true`                                       |
| instructions                     | boolean | 指定导入期间是否存储道路名称                                                                                                                                                                                                                                                                                               | `true`                                       |
| optimize                         | boolean | 在为 CH 收缩节点时优化排序顺序。这相当耗时，但能产生更好的收缩层次。                                                                                                                                                                                                                                       | `false`                                      |
| maximum_speed_lower_bound        | number  | 指定查询参数 `maximum_speed` 的阈值，在为 Core-ALT 算法计算预处理数据时需要                                                                                                                                                                                                                            | `80`                                         |
| encoder_flags_size               | number  | 用于 FlagEncoders 的字节数                                                                                                                                                                                                                                                                                                                         | `8`                                          |
| location_index_resolution        | number  | 位置索引中瓦片的最小分辨率（米）。较低的值查询更快，但会增加内存需求。降低分辨率会缩小查找半径，可通过增加 `location_index_search_iterations` 来补偿。对应 GraphHopper 的 `index.high_resolution` 配置参数。         | `500`                                        |
| location_index_search_iterations | number  | 坐标查找中执行的最大迭代次数。较高的值会扩大搜索范围，但可能降低查询性能。它只影响存储查找而不影响其布局，因此更改此参数不需要重建位置索引。对应 GraphHopper 的 `index.max_region_search` 配置参数。 | `4`                                          |
| gtfs_file                        | string  | 仅用于 `public-transport` 配置：GTFS 数据的位置；可以是 zip 文件或解压后的文件夹                                                                                                                                                                                                                                                       | _NA_                                         |
| encoder_options                  | string  | 详情请参见下文的 [encoder_options](#encoder-options)                                                                                                                                                                                                                                                                                                         |                                              |
| preparation                      | object  | 用于构建路由图的[预处理设置](#preparation)                                                                                                                                                                                                                                                                                              |                                              |
| ext_storages                     | object  | 用于返回额外信息的[外部存储](#ext-storages)                                                                                                                                                                                                                                                                                                |                                              |

## `encoder_options`

`ors.engine.profiles.<PROFILE-NAME>.build.encoder_options` 下的属性：

| key                      | type    | profiles         | description                                                                                                                                                                                                                                                                                                                                                                                                      | example value |
|--------------------------|---------|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| block_fords              | boolean | *                | 不穿越浅滩（ford）进行路由                                                                                                                                                                                                                                                                                                                                                                                       | `true`        |
| consider_elevation       | boolean | bike-*           | 最大可能速度是自行车类型特定的最大下坡速度，高于通常的最大速度                                                                                                                                                                                                                                                                                        | `true`        |
| maximum_grade_level      | number  | car, hgv         | 与[tracktype](https://wiki.openstreetmap.org/wiki/Key:tracktype)中描述的道路质量有关。例如指定 `maximum_grade_level=1` 意味着 `tracktype=grade2` 及以下等级不会参与路由。设置 `maximum_grade_level=0` 会丢弃所有带有效 `tracktype` 标签的道路，而负值如 `maximum_grade_level=-1` 则完全禁用道路上的路由。 | `3`           |
| preferred_speed_factor   | number  | wheelchair       | 对轮椅用户来说被归类为更优的边，其行驶速度将乘以该系数，用于在此类道路上设置较快的行驶速度                                                                                                                                                                                                                                                              | `1.2`         |
| problematic_speed_factor | number  | wheelchair       | 对轮椅用户来说被归类为有问题的边，其行驶速度将乘以该系数，用于在此类道路上设置较慢的行驶速度                                                                                                                                                                                                                                                               | `0.7`         |
| turn_costs               | boolean | car, hgv, bike-* | 是否应遵守转向限制                                                                                                                                                                                                                                                                                                                                                                            | `true`        |
| use_acceleration         | boolean | car, hgv         | 模拟车辆在道路段上如何加速到允许的最大速度。在实践中，它会降低较短道路段（例如城市中邻近交叉口之间的路段）上的速度                                                                                                                                                                                                                | `true`        |
| enable_custom_models     | boolean | *                | 启用该配置是否准备好支持自定义模型。另请参见[服务属性](service.zh.md)中对应的参数 `allow_custom_models`。                                                                                                                                                                                                                                             | `false`       |

## `preparation`

`ors.engine.profiles.<PROFILE-NAME>.build.preparation` 下的属性：

| key              | type   | description                                                       | default value |
|------------------|--------|-------------------------------------------------------------------|---------------|
| min_network_size | number | 独立路由子图的最小规模（边数） | `200`         |
| methods          | object | 见下文                                                         |               |

### `methods.ch`

收缩层次预处理设置

`ors.engine.profiles.<PROFILE-NAME>.build.preparation.methods.ch` 下的属性：

| key        | type    | description                                              | example value          |
|------------|---------|----------------------------------------------------------|------------------------|
| enabled    | boolean | 启用或禁用该路由算法                | `true`                 |
| threads    | number  | 计算预处理的并行线程数 | `1`                    |
| weightings | string  | 以逗号分隔的权重列表                       | `recommended,shortest` |

### `methods.lm`

使用 landmarks 的 A* 预处理设置

`ors.engine.profiles.<PROFILE-NAME>.build.preparation.methods.lm` 下的属性：

| key        | type    | description                                                                                                               | default value          |
|------------|---------|---------------------------------------------------------------------------------------------------------------------------|------------------------|
| enabled    | boolean | 启用或禁用该路由算法                                                                                 | `true`                 |
| threads    | number  | 计算预处理的并行线程数                                                                  | `1`                    |
| weightings | string  | 以逗号分隔的权重列表                                                                                        | `recommended,shortest` |
| landmarks  | number  | 预计算 landmarks 的总数；查询期间使用的子集在 `execution.methods.lm.active_landmarks` 中设置 | `16`                   |

### `methods.core`

使用 landmarks 的 core 路由预处理设置

`ors.engine.profiles.<PROFILE-NAME>.build.preparation.methods.core` 下的属性：

| key        | type    | description                                                                                                                 | example value                                               |
|------------|---------|-----------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| enabled    | boolean | 启用或禁用该路由算法                                                                                   | `true`                                                      |
| threads    | number  | 计算预处理的并行线程数                                                                    | `1`                                                         |
| weightings | string  | 以逗号分隔的权重列表                                                                                          | `recommended,shortest`                                      |
| landmarks  | number  | 预计算 landmarks 的总数，查询期间使用的子集在 `execution.methods.core.active_landmarks` 中设置 | `32`                                                        |
| lmsets     | string  | 为特定避免过滤器启用的定制 landmark 集合                                                                   | `highways,tollways;highways;tollways;country_193;allow_all` |

## `ext_storages`

可以为每个配置定义图中应包含哪些辅助元数据。
这些信息会作为 `extra_info` 在路由响应中提供。

为此，请添加下表中的某个键。
其值留空即可，除非你想指定更多选项（目前仅适用于
[RoadAccessRestrictions](#roadaccessrestrictions)、[Borders](#borders)、[Wheelchair](#wheelchair)
和 [HeavyVehicle](#heavyvehicle)）。

::: warning
除了在查询响应中提供信息外，`WayCategory` 和 `Tollways` 存储中的数据还会被用于通过
[`options.avoid_features`](/api-reference/endpoints/directions/routing-options.md#options-avoid-features)
查询参数过滤掉某些道路，而 `Borders` 对于
[`options.avoid_borders`](/api-reference/endpoints/directions/routing-options.md#options-avoid-borders) 和
[`options.avoid_countries`](/api-reference/endpoints/directions/routing-options.md#options-avoid-countries)
查询参数背后的功能是必需的。来自
[
`options.profile_params.restrictions`](/api-reference/endpoints/directions/routing-options.md#options-profile-params-restrictions)
的选项需要启用 `HeavyVehicle` 或 `Wheelchair` 存储。此外，在
[`options.vehicle_type`](/api-reference/endpoints/directions/routing-options.md#options-vehicle-type)
参数中指定的 hgv 配置特定通行限制
依赖于 `HeavyVehicle` 存储。
:::

`ors.engine.profiles.<PROFILE-NAME>.build.ext_storages` 下的属性：

| key                    | type   | description                                                                                                                                      | example value                                     |
|------------------------|--------|--------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| WayCategory            | object | 在路线响应中返回道路类别，适用于任何配置类型                                                                 |                                                   |
| WaySurfaceType         | object | 在路线响应中返回道路表面，适用于任何配置类型                                                                  |                                                   |
| Tollways               | object | 在路线响应中返回道路通行费，适用于驾车配置                                                                        |                                                   |
| Borders                | object | Borders 允许将路线限制为不跨越国界，适用于任何配置类型                                          | [Borders](#borders)                               |
| RoadAccessRestrictions | object | 关于道路对特定车辆或情形限制的信息，例如 `access=destination`                                              | [RoadAccessRestrictions](#roadaccessrestrictions) |
| HeavyVehicle           | object | 重型车辆专用存储，仅与该配置兼容；包含重量和尺寸限制以及车辆类型通行限制 | [HeavyVehicle](#heavyvehicle)                     |
| HillIndex              | object | 在路线响应中返回上升/下降，适用于任何配置类型                                                               |                                                   |
| TrailDifficulty        | object | 在路线响应中返回小径难度，适用于步行和骑行配置                                                 |                                                   |
| Wheelchair             | object | 轮椅专用属性，仅与该配置兼容                                                                                 | [Wheelchair](#wheelchair)                         |
| OsmId                  | object | 返回道路的 OsmId，适用于任何配置类型                                                                                   |                                                   |

有关 extra info 的可用性，请查看[此表](/api-reference/endpoints/directions/extra-info/index.md#extra-info-availability)。
下表总结了默认情况下哪些存储对哪些配置启用。

|             | WayCategory | WaySurfaceType | Tollways | Borders | RoadAccessRestrictions | HeavyVehicle | HillIndex | TrailDifficulty | Wheelchair | OsmId |
|:------------|:-----------:|:--------------:|:--------:|---------|:----------------------:|:------------:|:---------:|:---------------:|:----------:|:-----:|
| driving-car |      x      |       x        |    x     |         |           x            |              |           |                 |            |       |
| driving-hgv |      x      |       x        |    x     |         |                        |      x       |           |                 |            |       |
| cycling-*   |      x      |       x        |          |         |                        |              |     x     |        x        |            |       |
| foot-*      |      x      |       x        |          |         |                        |              |     x     |        x        |            |       |
| wheelchair  |      x      |       x        |          |         |                        |              |           |                 |     x      |   x   |

`use_for_warnings` 参数告知 ors 该存储可用于在
路线响应中生成警告消息。

| key              | type    | description                                                                                                                                                                                | example value |
|------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| use_for_warnings | boolean | 每当路线经过带有某些限制的道路时，响应中会附带一条警告消息，并自动添加 roadaccessrestrictions 额外信息。 | `true`        |

### `Wheelchair`

| key              | type    | description                              | example value |
|------------------|---------|------------------------------------------|---------------|
| KerbsOnCrossings | boolean | 仅在交叉口解析路缘（kerb）高度。 | `true`        |

### `Borders`

`ors.engine.profiles.<PROFILE-NAME>.build.ext_storages.Borders` 下的属性可用于定义
路线不跨越国界的限制，适用于任何配置类型。

| key          | type    | description                                                                                                                                                                                                                             | example value            |
|--------------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| boundaries   | string  | 包含表示国界多边形的 geojson 文件的路径。如果设置了 `preprocessed = true` 则忽略。                                                                                                                   | `borders.geojson.tar.gz` |
| preprocessed | boolean | 指示源 OSM 文件是否已用国家数据增强。如果设为 `true`，则国家代码从 `country` 节点标签读取，而不是在构建时根据 `boundaries` 中提供的几何形状解析。 | `true`                    |
| ids          | string  | 包含每个国家唯一 id、其本地名称和英文名称的 csv 文件路径                                                                                                                                         | `ids.csv`                |
| openborders  | string  | 包含边界开放（即申根边界）的国家对的 csv 文件路径                                                                                                                                     | `openborders.csv`        |

### `HeavyVehicle`

| key          | type    | description                                                                                                                                                                                                                                                    | example value |
|--------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| restrictions | boolean | 编码某些尺寸和重量限制，例如 `maxheight`、`maxlength`、`maxwidth`、`maxweight` 和 `maxaxleload` OSM 道路标签中包含的限制。还包括 `hazmat` 标签所提供的运输危险品车辆通行限制。 | `true`        |
