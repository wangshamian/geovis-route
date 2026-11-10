# 通过 `gr-config.json` 进行配置

:::warning
gr-config.json 已弃用！请[迁移到 gr-config.yml](https://github.com/GIScience/gr-config-migration#usage)。
:::

为了便利，这种"旧"配置方式还会继续支持一段时间。
只要 openrouteservice 仍然支持通过 JSON 文件进行配置，下面的说明就会保留，
但我们**不**推荐使用这种配置方式。
请考虑将[你的 JSON 配置迁移](https://github.com/GIScience/gr-config-migration#usage)到新样式。
请注意，目前所提供的 JSON 配置文件中的所有设置都会**覆盖** YAML 文件中的任何设置。

## ors

顶层元素。

| key            | type   | description                       | example value                          |
|----------------|--------|-----------------------------------|----------------------------------------|
| services       | object | 包含各项服务的对象                | [services](#ors-services)              |  
| logging        | object | 日志属性                          | [logging](#ors-logging)                |
| system_message | list   | 系统消息对象列表                  | [system messages](#ors-system-message) |

---

### ors.services

| key                     | type   | description                           | example value                          |
|-------------------------|--------|---------------------------------------|----------------------------------------|
| ors.services.routing    | object | 路由及其配置的设置                    | [routing](#ors-services-routing)       | 
| ors.services.isochrones | object | 等时圈限制的设置                      | [isochrones](#ors-services-isochrones) | 
| ors.services.matrix     | object | 矩阵限制的设置                        | [matrix](#ors-services-matrix)         | 
| ors.services.snap       | object | snap 的设置                           | [snap](#ors-services-snap)             | 

---

#### ors.services.routing

| key                    | type    | description                                                                                                                                                                                                             | example value                                        |
|------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|      
| enabled                | boolean | 启用或禁用（true/false）该端点。默认值为 true。                                                                                                                                                                        | `true`                                               |
| mode                   | string  | 可以是 "normal" 或 "preparation"。如果为 "preparation"，则会构建图，随后服务将被关闭。                                                                                                                                  | `"normal"`                                           |
| description            | string  |                                                                                                                                                                                                                         | `"This is a routing file from openrouteservice"`     |
| routing_name           | string  | 指定 gpx 响应中返回的 gpx `name` 标签                                                                                                                                                                                   | `"openrouteservice"`                                 |
| sources                | list    | 要使用的 osm 文件，支持的格式有 `.osm`、`.osm.gz`、`.osm.zip` 和 `.pbf`                                                                                                                                                | `["heidelberg.osm.gz"]`                              |
| init_threads           | number  | 用于初始化（构建/加载）图的线程数。数值越高需要越多 RAM。                                                                                                                                                              | `2`                                                  |
| attribution            | string  | 添加到响应元数据中的署名信息                                                                                                                                                                                            | `"openrouteservice.org, OpenStreetMap contributors"` |
| elevation_preprocessed | boolean | 启用或禁用读取节点的 ele 标签。默认值为 false。如果启用，将阻止 GH 的高程查找，所有没有 ele 标签的节点将默认为 0。实验性功能，用于 ORS 预处理器                        | `false`                                              |
| profiles               | object  |                                                                                                                                                                                                                         | [profiles](#ors-services-routing-profiles)           |

---

##### **ors.services.routing.profiles**

| key            | type   | description                                                                                                                                                                                                                                                                                                    | example value                                                   |
|----------------|--------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------|      
| active         | list   | 定义激活的路由配置列表。元素名称 XXX 必须对应 "profile-XXX" 的记法，该记法用于后续各节。可以是 `"car"`、`"hgv"`、`"bike-regular"`、`"bike-mountain"`、`"bike-road"`、`"bike-electric"`、`"walking"`、`"hiking"` 或 `"wheelchair"` 中的一个或多个 | `["car", "bike-regular"]`                                       |
| default_params | object | 设置默认应用于每个配置的参数                                                                                                                                                                                                                                                                                   | [default_params](#ors-services-routing-profiles-default-params) |
| profile-XXX    | object | 应用于特定配置的设置。你可以同时运行多个配置。                                                                                                                                                                                                                                                                 | [profile-XXX](#ors-services-routing-profiles-profile-xxx)       |

---

##### **ors.services.routing.profiles.default_params**

| key                                               | type    | description                                                                                                                                                                                                                                                                                                                                                       | example value                                                            |
|---------------------------------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------| 
| encoder_flags_size                                | number  | 用于 FlagEncoders 的字节数                                                                                                                                                                                                                                                                                                                                         | `8`                                                                      |
| graphs_root_path                                  | string  | 用于存储图的目录根路径                                                                                                                                                                                                                                                                                                                                             | `"../graphs"`                                                            |
| elevation_provider                                | string  | 高程提供程序的名称。可能的值为 multi、cgiar 或 srtm                                                                                                                                                                                                                                                                | `"multi"`                                                                |
| elevation_cache_path                              | string  | 用于存储 SRTM 瓦片的目录路径                                                                                                                                                                                                                                                                                                                                       | `"elevation_cache"`                                                      |
| elevation_cache_clear                             | boolean | 高程数据一经下载即保留                                                                                                                                                                                                                                                                                                                                             | `false`                                                                  |
| elevation_smoothing                               | boolean | 平滑高程数据                                                                                                                                                                                                                                                                                                                                                       | `false`                                                                  |
| instructions                                      | boolean | 指定是否在导入期间存储道路名称                                                                                                                                                                                                                                                                                                                                     | `true`                                                                   |
| maximum_distance                                  | number  | 允许的路径总距离最大值                                                                                                                                                                                                                                                                                                                                             | `100000`                                                                 |
| maximum_segment_distance_<br>with_dynamic_weights | number  | 使用动态权重时两个途经点之间允许的距离最大值                                                                                                                                                                                                                                                                                                                       | `50000`                                                                  |
| maximum_waypoints                                 | number  | 一次请求中的途经点数量上限                                                                                                                                                                                                                                                                                                                                         | `50`                                                                     |
| maximum_snapping_radius                           | number  | 在给定坐标周围查找可连接边的最大距离                                                                                                                                                                                                                                                                                                                               | `100`                                                                    |
| maximum_distance_round_trip_routes                | number  | 往返算法允许的路径总距离最大值                                                                                                                                                                                                                                                                                                                                     | `100000`                                                                 |
| maximum_distance_alternative_routes               | number  | 备选路径算法允许的路径总距离最大值                                                                                                                                                                                                                                                                                                                                 | `100000`                                                                 |
| maximum_alternative_routes                        | number  | 一次请求中备选路径的最大数量                                                                                                                                                                                                                                                                                                                                       | `3`                                                                      |
| maximum_avoid_polygon_area                        | number  | 允许的多边形总面积最大值（平方千米），可选                                                                                                                                                                                                                                                         | `200000000`                                                              |
| maximum_avoid_polygon_extent                      | number  | 允许的多边形范围（即包络边长）最大值（千米），可选                                                                                                                                                                                                                                                 | `20000`                                                                  |
| location_index_resolution                         | number  | 位置索引中瓦片的最小分辨率（米）。较小的值会以更高的内存需求为代价换取更快的查询。降低分辨率会缩小查找半径，可通过增加 `location_index_search_iterations` 来补偿。对应 GraphHopper 的 `index.high_resolution` 配置参数。                                                                              | `500` (default)                                                          |
| location_index_search_iterations                  | number  | 坐标查找中执行的最大迭代次数。较大的值会带来更广的搜索范围，但可能降低查询性能。它只影响存储查找而不影响其布局，因此修改该参数不需要重建位置索引。对应 GraphHopper 的 `index.max_region_search` 配置参数。                                                                                          | `4` (default)                                                            |
| maximum_speed_lower_bound                         | number  | 指定查询参数 `maximum_speed` 的阈值。                                                                                                                                                                                                                                                              | `80` (default)                                                           |
| interpolate_bridges_and_tunnels                   | boolean | 对桥梁和隧道的高程进行插值。                                                                                                                                                                                                                                                                                                                                       | `true` (default)                                                         | 
| preparation                                       | object  | ...                                                                                                                                                                                                                                                                                                                                                               | [preparation](#ors-services-routing-profiles-default-params-preparation) |
| execution                                         | object  | ...                                                                                                                                                                                                                                                                                                                                                               | [execution](#ors-services-routing-profiles-default-params-execution)     |

---

##### **ors.services.routing.profiles.default_params.preparation**

| key              | type   | description                                                       | example value                                                                |
|------------------|--------|-------------------------------------------------------------------|------------------------------------------------------------------------------| 
| min_network_size | number | 独立路由子图的最小规模（边数）                                     | `200`                                                                        |
| methods          | object |                                                                   | [methods](#ors-services-routing-profiles-default-params-preparation-methods) |     

##### **ors.services.routing.profiles.default_params.preparation.methods**

| key  | type   | description                                        | example value                                                                  |
|------|--------|----------------------------------------------------|--------------------------------------------------------------------------------| 
| ch   | object | 收缩层次结构预处理的设置                             | [ch](#ors-services-routing-profiles-default-params-preparation-methods-ch)     |
| lm   | object | 地标预处理的设置                                     | [lm](#ors-services-routing-profiles-default-params-preparation-methods-lm)     |
| core | object | 地标预处理的设置                                     | [core](#ors-services-routing-profiles-default-params-preparation-methods-core) |

##### **ors.services.routing.profiles.default_params.preparation.methods.ch**

| key        | type    | description | example value   |
|------------|---------|-------------|-----------------| 
| enabled    | boolean |             | `true`          |
| threads    | number  |             | `1`             |
| weightings | string  |             | `"recommended"` |

##### **ors.services.routing.profiles.default_params.preparation.methods.lm**

| key        | type    | description                                                                                                                                                               | example value            |
|------------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------| 
| enabled    | boolean |                                                                                                                                                                           | `true`                   |
| threads    | number  |                                                                                                                                                                           | `1`                      |
| weightings | string  |                                                                                                                                                                           | `"recommended,shortest"` |
| landmarks  | number  | 预计算地标的总数，查询期间使用的子集在 [`active_landmarks`](#ors-services-routing-profiles-default-params-executionmethods-lm) 中设置                              | `16`                     |

##### **ors.services.routing.profiles.default_params.preparation.methods.core**

| key        | type    | description                                                                                                                                                                  | example value                                                 |
|------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------| 
| enabled    | boolean |                                                                                                                                                                              | `true`                                                        |
| threads    | number  |                                                                                                                                                                              | `1`                                                           |
| weightings | string  |                                                                                                                                                                              | `"recommended,shortest"`                                      |
| landmarks  | number  | 预计算地标的总数，查询期间使用的子集在 [`active_landmarks`](#ors-services-routing-profiles-default-params-execution-methods-core) 中设置                                   | `32`                                                          |
| lmsets     | string  |                                                                                                                                                                              | `"highways,tollways;highways;tollways;country_193;allow_all"` |

---

##### **ors.services.routing.profiles.default_params.execution**

| key     | type   | description | example value                                                              |
|---------|--------|-------------|----------------------------------------------------------------------------| 
| methods | object |             | [methods](#ors-services-routing-profiles-default-params-execution-methods) |

##### **ors.services.routing.profiles.default_params.execution.methods**

| key  | type   | description                                           | example value                                                                |
|------|--------|-------------------------------------------------------|------------------------------------------------------------------------------| 
| ch   | object | 在路由中使用收缩层次结构的设置                          | [ch](#ors-services-routing-profiles-default-params-execution-methods-ch)     |
| lm   | object | 在路由中使用地标的设置                                  | [lm](#ors-services-routing-profiles-default-params-execution-methods-lm)     |
| core | object | 在路由中使用地标的设置                                  | [core](#ors-services-routing-profiles-default-params-execution-methods-core) |

##### **ors.services.routing.profiles.default_params.execution.methods.lm**

| key               | type    | description                                      | example value |
|-------------------|---------|--------------------------------------------------|---------------| 
| active_landmarks  | number  | 用于计算路径的地标数量                           | `8`           |

##### **ors.services.routing.profiles.default_params.execution.methods.core**

| key               | type    | description                                      | example value |
|-------------------|---------|--------------------------------------------------|---------------| 
| active_landmarks  | number  | 用于计算路径的地标数量                           | `6`           |

---

##### **ors.services.routing.profiles.profile-XXX**

| key        | type   | description                                   | example value                                                       |
|------------|--------|-----------------------------------------------|---------------------------------------------------------------------| 
| profiles   | string | 通过 API 调用时的配置名称                       | `driving-car`                                                       |
| parameters | object | 某个配置的特定配置参数                          | [parameters](#ors-services-routing-profiles-profile-xxx-parameters) |

##### **ors.services.routing.profiles.profile-XXX.parameters**

| key                                 | type    | description                                                                                       | example value                                                                           |
|-------------------------------------|---------|---------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------| 
| encoder_options                     | string  |                                                                                                   | `"turn_costs=true\                                                                      |block_fords=false"` [encoder options](#ors-services-routing-profiles-profile-xxx-parametersencoder_options) |
| elevation                           | boolean | 这将在响应中启用高程信息                                                                          | `true`                                                                                  |
| maximum_distance                    | number  | 路径可具有的最大距离（米）                                                                        | `100000`                                                                                |
| maximum_snapping_radius             | number  | 在给定坐标周围查找可连接边的最大距离                                                              | `100`                                                                                   |
| maximum_distance_round_trip_routes  | number  | 往返路径算法允许的路径总距离最大值                                                                | `100000`                                                                                |
| maximum_distance_alternative_routes | number  | 备选路径算法允许的路径总距离最大值                                                                | `100000`                                                                                |
| maximum_alternative_routes          | number  | 一次请求中备选路径的最大数量                                                                      | `3`                                                                                     |
| maximum_avoid_polygon_area          | number  | 允许的多边形总面积最大值（平方千米），可选                                                        | `200000000`                                                                             |
| maximum_avoid_polygon_extent        | number  | 允许的多边形范围（即包络边长）最大值（千米），可选                                                | `20000`                                                                                 |
| ext_storages                        | object  | 控制启用哪些外部存储                                                                              | [external storages](#ors-services-routing-profiles-profile-xxx-parameters-ext-storages) |
| gtfs_file                           | string  | 仅用于 pt 配置：所使用的 gtfs 文件位置。可以是 zip 文件或解压后的文件夹                            | `"src/test/files/vrn_gtfs_cut.zip"`                                                     |

##### **ors.services.routing.profiles.profile-XXX.parameters.encoder_options**

| key                      | type   | description                                                                                                                                                                       | example value                  |
|--------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------| 
| problematic_speed_factor | number | 仅用于 wheelchair 配置！被归类为不适合轮椅使用者通行的边上的行驶速度将乘以该系数，用于在此类道路上设置较慢的行驶速度                                                              | `problematic_speed_factor=0.7` |
| preferred_speed_factor   | number | 仅用于 wheelchair 配置！被归类为适合轮椅使用者通行的边上的行驶速度将乘以该系数，用于在此类道路上设置较快的行驶速度                                                                | `preferred_speed_factor=1.2`   |

##### **ors.services.routing.profiles.profile-XXX.parameters.ext_storages**

| key                    | type   | description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | example value                                                                         |
|------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------| 
| WayCategory            | object | 在路径响应中返回道路类别，兼容任何配置类型                                                                                                                                                                                                                                                                                                                                                                                                                                                    | `{}`                                                                                  |
| WaySurfaceType         | object | 在路径响应中返回道路表面类型，兼容任何配置类型                                                                                                                                                                                                                                                                                                                                                                                                                                                | `{}`                                                                                  |
| HillIndex              | object | 在路径响应中返回爬升/下降信息，兼容任何配置类型                                                                                                                                                                                                                                                                                                                                                                                                                                               | `{}`                                                                                  |
| TrailDifficulty        | object | 在路径响应中返回路径难度，兼容 profile-hiking                                                                                                                                                                                                                                                                                                                                                                                                                                                 | `{}`                                                                                  |
| RoadAccessRestrictions | object | RoadAccessRestrictions 指道路在特定情况下对特定车辆的限制，例如 access=destination。use_for_warnings 参数告诉 ors，此存储可用于在路由响应中生成警告消息。对于 RoadAccessRestrictions，这意味着每当路径经过有某种限制的道路时，响应中都会附带一条警告消息，并自动添加 roadaccessrestrictions 额外信息。                                                                                                                    | `{ use_for_warnings: true }`                                                          |
| Wheelchair             | object | 兼容 wheelchair                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | `{ KerbsOnCrossings: "true" }`                                                        |
| OsmId                  | object | 返回道路的 OsmId，兼容 wheelchair                                                                                                                                                                                                                                                                                                                                                                                                                                                            | `{}`                                                                                  |
| Borders                | object | Borders 允许限制路径不跨越国界，兼容任何配置类型                                                                                                                                                                                                                                                                                                                                                                                                                                               | [Borders](#ors-services-routing-profiles-profile-xxx-parameters-ext-storages-borders) |    

##### **ors.services.routing.profiles.profile-XXX.parameters.ext_storages.Borders**

| key         | type   | description                                                                                         | example value              |
|-------------|--------|-----------------------------------------------------------------------------------------------------|----------------------------| 
| boundaries  | string | 包含代表国界 geojson 数据的文件的路径                                                                | `'borders.geojson.tar.gz'` |
| ids         | string | 指向 csv 文件的路径，该文件包含每个国家的唯一 id、其本地名称及其英文名称                               | `'ids.csv'`                |
| openborders | string | 指向 csv 文件的路径，该文件包含边界开放（即申根边界）的国家对                                          | `'openborders.csv'`        |

---

#### ors.services.isochrones

| key                    | type    | description                                                                                                                                | example value                                                                              |
|------------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------| 
| enabled                | boolean | 启用或禁用（true/false）该端点（默认：true）                                                                                                 | `true`                                                                                     |
| maximum_range_distance | list    | maximum_range_distance 和 maximum_range_time 的可能值为一个整数，或为每个配置分别定义的值列表                                                | `[{profiles: "any", value: 50000}, {profiles: "driving-car, driving-hgv", value: 100000}]` |
| maximum_range_time     | list    |                                                                                                                                            | `[{profiles: "any", value: 18000},{profiles: "driving-car, driving-hgv", value: 3600}]`    |
| maximum_intervals      | number  | 为每个位置计算的最大间隔/等时圈数量                                                                                                          | `10`                                                                                       |
| maximum_locations      | number  | 一次请求中的最大位置数量                                                                                                                     | `2`                                                                                        |
| allow_compute_area     | number  | 指定是否允许进行面积计算                                                                                                                     | `true`                                                                                     |

##### **ors.services.isochrones.fastisochrones**

| key                    | type | description                                                                                                                                | example value                                                                              |
|------------------------|------|--------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------| 
| maximum_range_distance | list | maximum_range_distance 和 maximum_range_time 的可能值为一个整数，或为每个配置分别定义的值列表                                                | `[{profiles: "any", value: 50000}, {profiles: "driving-car, driving-hgv", value: 100000}]` |
| maximum_range_time     | list |                                                                                                                                            | `[{profiles: "any", value: 18000},{profiles: "driving-car, driving-hgv", value: 3600}]`    |

##### **ors.services.isochrones.fastisochrones.profiles.default_params**

| key          | type    | description                                              | example value            |
|--------------|---------|----------------------------------------------------------|--------------------------|
| enabled      | boolean |                                                          | `true`                   |
| threads      | number  |                                                          | `1`                      |
| weightings   | string  |                                                          | `"recommended,shortest"` |
| maxcellnodes | number  | 单个等时圈单元格中允许的最大节点数                       | `5000`                   |

---

#### ors.services.matrix

| key                     | type    | description                                                                                                                 | example value                                        |
|-------------------------|---------|-----------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------| 
| enabled                 | boolean | 启用或禁用（true/false）该端点（默认：true）                                                                                  | `true`                                               |
| maximum_routes          | number  | matrix 应计算的路径数量上限。例如 `2500` 可以对应 50x50 的矩阵或 1x2500 的矩阵                                                | `2500`                                               |
| maximum_routes_flexible | number  | 使用不支持收缩层次结构的自定义配置时的路径数量上限                                                                            | `25`                                                 |
| maximum_search_radius   | number  | 请求坐标与最近道路上的点之间允许的距离最大值。数值以米为单位                                                                    | `5000`                                               |
| maximum_visited_nodes   | number  | 最短路径计算中允许访问的节点数量上限。该阈值仅适用于 Dijkstra 算法                                                              | `100000`                                             |
| allow_resolve_locations | number  | 指定是否可以解析距离位置最近的街道名称。默认值为 true                                                                         | `true`                                               |
| attribution             | string  | 添加到响应元数据中的署名信息                                                                                                  | `"openrouteservice.org, OpenStreetMap contributors"` |

---
#### ors.services.snap

| key               | type    | description                                                    | example value                                        |
|-------------------|---------|----------------------------------------------------------------|------------------------------------------------------| 
| enabled           | boolean | 启用或禁用（true/false）该端点（默认：true）                     | `true`                                               |
| attribution       | string  | 添加到响应元数据中的署名信息                                    | `"openrouteservice.org, OpenStreetMap contributors"` |
| maximum_locations | number  | 一次请求中的最大位置数量                                         | `5000`                                               |

---

#### ors.logging

| key        | type    | description                                                     | example value          |
|------------|---------|-----------------------------------------------------------------|------------------------| 
| enabled    | boolean | 启用或禁用该端点（默认：true）                                   | `true`                 |
| level_file | string  | 可以是 `DEBUG_LOGGING.json` 或 `PRODUCTION_LOGGING.json`         | `"DEBUG_LOGGING.json"` |
| location   | string  | 日志的位置                                                       | `"/var/log/ors"`       |
| stdout     | boolean |                                                                 | `true`                 |

---

#### ors.system_message

消息对象数组，其中每个对象都有

| key       | type    | description                                                      | example value        |
|-----------|---------|------------------------------------------------------------------|----------------------| 
| active    | boolean | 启用或禁用此消息                                                 | `true`               |
| text      | string  | 消息文本                                                         | `"The message text"` |
| condition | object  | 可选；可包含下表中的任意条件                                     |                      |

| condition          | value                                                                                                                | description                                                         |
|--------------------|----------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| time_before        | ISO 8601 datetime string                                                                                             | 如果服务器本地时间早于给定时间点则发送消息                          |
| time_after         | ISO 8601 datetime string                                                                                             | 如果服务器本地时间晚于给定时间点则发送消息                          |
| api_version        | 1 or 2                                                                                                               | 如果请求的 API 版本与值匹配则发送消息                               |
| api_format         | String with output formats ("json", "geojson", "gpx"), comma separated                                               | 如果请求的输出格式与值匹配则发送消息                                |
| request_service    | String with service names ("routing", "matrix", "isochrones"), comma separated                                       | 如果请求的服务与给定名称之一匹配则发送消息                          |
| request_profile    | String with profile names, comma separated                                                                           | 如果请求的配置与给定名称之一匹配则发送消息                          |
| request_preference | String with preference (weightings for routing, metrics for matrix, rangetype for isochrones) names, comma separated | 如果请求的偏好（路由的权重、矩阵的度量、等时圈的范围类型）与给定名称之一匹配则发送消息 |

##### 示例：

```
system_message: [
    {
        active: true,
        text: "This message would be sent with every routing bike fastest request. E.g. 'The fastest weighting for cycling profiles is deprecated, use recommended weighting instead. API will be kept for compatibility until release of version 7.0.0'",
        condition: {
            "request_service": "routing",
            "request_profile": "cycling-regular,cycling-mountain,cycling-road,cycling-electric",
            "request_preference": "fastest"
        }
    },
    {
        active: true,
        text: "This message would be sent with every request for geojson response.",
        condition: {
            "api_format": "geojson"
        }
    },
    {
        active: true,
        text: "This message would be sent with every request on API v1 from January 2020 until June 2050. E.g. 'The V1 API is deprecated. You should switch to using the V2 API.'",
        condition: {
            "api_version": "1",
            "time_after": "2020-01-01T00:00:00Z",
            "time_before": "2050-06-01T00:00:00Z"
        }
    },
    {
        active: true,
        text: "This message would be sent with every request. E.g. 'Scheduled downtime due to version upgrade on March 15th 2020, 12:00 AM CET for approx. 3 hours.'"
    }
]
```
