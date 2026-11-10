# 配置

你自己的 **openrouteservice** 实例通过名为 `gr-config.yml` 的 YAML 文件进行配置。
**openrouteservice** 会在多个位置查找这类文件。如果某个属性在多个文件中都有设置，
则后出现的设置会覆盖先前的设置。

- `/etc/openrouteservice/gr-config.yml`
- `~/.openrouteservice/gr-config.yml`
- `./gr-config.yml`（位于运行时工作目录中）

### 可用属性

属性以层级结构组织。由于 **openrouteservice** 基于
[spring](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)，
所有通用的 spring 属性都可以在 gr-config.yml 文件中设置。日常使用中最相关的属性如下：

| 键                            | 类型   | 描述                                                                                                                                                                                                  | 默认值                       |
|--------------------------------|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------|
| server.port                    | int    | 服务器端口                                                                                                                                                                                                   | 8082                                |                        
| logging.log4j2.config.override | string | 日志配置文件。**openrouteservice** 自带三种预设（`DEFAULT_LOGGING.json`、`DEBUG_LOGGING.json`、`PRODUCTION_LOGGING.json`），你也可以定义自己的日志配置。 | classpath:logs/DEFAULT_LOGGING.json |                        

**openrouteservice** 特有的其他属性按下列顶层 key 组织。每个配置块的详细说明见下文。

| 键           | 描述                                                            |
|---------------|------------------------------------------------------------------------|
| ors.endpoints | 运行时处理 API 请求所需的设置。                  |
| ors.engine    | 启动时构建图（graph）阶段所需的设置。                  |
| ors.cors      | **openrouteservice** API 的 CORS 设置。                        |
| ors.messages  | 可以按简单规则随 API 响应发送系统消息。 |

### `endpoints` 块中的属性

| 键                                             | 类型   | 描述                                                                            | 默认值                     |
|-------------------------------------------------|--------|----------------------------------------------------------------------------------------|-----------------------------------|
| ors.endpoints.routing.base_url                  | string |                                                                                        | https://openrouteservice.org/     |
| ors.endpoints.routing.swagger_documentation_url | string | 定义 swagger 文档的 url。可以与 `base_url` 不同 | https://api.openrouteservice.org/ |
| ors.endpoints.routing.support_mail              | string |                                                                                        | support@openrouteservice.org      |
| ors.endpoints.routing.author_tag                | string |                                                                                        | openrouteservice                  |
| ors.endpoints.routing.content_licence           | string |                                                                                        | LGPL 3.0                          ||    

### `engine` 块中的属性

| 键          | 类型   | 描述 | 默认值 |
|--------------|--------|-------------|---------------|
| ors.engine.x | string |             |               |

### `cors` 块中的属性

| 键                    | 类型                          | 描述                                                                        | 默认值                                                                                                                |
|------------------------|-------------------------------|------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| cors.allowed_origins   | string / comma separated list | 配置 Access-Control-Allow-Origins CORS 响应头。`*` 表示允许所有来源       | *                                                                                                                            |                        
| cors.allowed_headers   | string / comma separated list | 配置 Access-Control-Allow-Headers CORS 响应头。`*` 表示允许所有请求头       | Content-Type, X-Requested-With, accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization | 
| cors.preflight_max_age | int                           | 持续时间，单位为秒。指定浏览器缓存 OPTIONS 响应的时长 | 600                                                                                                                          |                               

### `messages` 块中的属性

messages 属性期望一个元素列表，每个元素包含以下内容：

| 键       | 类型    | 描述                                                       | 示例值        |
|-----------|---------|-------------------------------------------------------------------|----------------------|
| active    | boolean | 启用或禁用该消息                                  | `true`               |
| text      | string  | 消息文本                                                  | `"The message text"` |
| condition | list    | 可省略；可包含下表任意条件 |                      |

| 条件          | 取值                                                                                                                | 描述                                                         |
|--------------------|----------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| time_before        | ISO 8601 datetime string                                                                                             | 当服务器本地时间早于给定时间点时发送该消息     |
| time_after         | ISO 8601 datetime string                                                                                             | 当服务器本地时间晚于给定时间点时发送该消息      |
| api_version        | 1 or 2                                                                                                               | 当请求的 API 版本与值匹配时发送该消息         |
| api_format         | String with output formats ("json", "geojson", "gpx"), comma separated                                               | 当请求的输出格式与值匹配时发送该消息               |
| request_service    | String with service names ("routing", "matrix", "isochrones", "snap"), comma separated                               | 当请求的服务与给定名称之一匹配时发送该消息    |
| request_profile    | String with profile names, comma separated                                                                           | 当请求的 profile 与给定名称之一匹配时发送该消息    |
| request_preference | String with preference (weightings for routing, metrics for matrix, rangetype for isochrones) names, comma separated | 当请求的偏好（preference）与给定名称之一匹配时发送该消息 |

##### 示例：

```
messages:
  - active: true
    text: This message would be sent with every routing bike fastest request
    condition:
      - request_service: routing
      - request_profile: cycling-regular,cycling-mountain,cycling-road,cycling-electric
      - request_preference: fastest
  - active: true
    text: This message would be sent with every request for geojson response
    condition:
      - api_format: geojson
  - active: true
    text: This message would be sent with every request on API v1 from January 2020 until June 2050
    condition:
      - api_version: 1
      - time_after: 2020-01-01T00:00:00Z
      - time_before: 2050-06-01T00:00:00Z
  - active: true
    text: This message would be sent with every request
```

## 通过 `gr-config.json` 进行配置

出于便利考虑，“旧”的配置方式还会继续支持一段时间。下文说明会在
**openrouteservice** 仍支持通过 JSON 文件进行配置期间保留，但我们**不**推荐使用这种配置方式。

### ors

顶层元素。

| 键            | 类型   | 描述                       | 示例值                         |
|----------------|--------|-----------------------------------|---------------------------------------|
| services       | object | 包含各服务的对象 | [services](#orsservices)              |  
| logging        | object | 日志相关属性            | [logging](#orslogging)                |
| system_message | list   | 系统消息对象列表    | [system messages](#orssystem_message) |

---

### ors.services

| 键                     | 类型   | 描述                           | 示例值                        |
|-------------------------|--------|---------------------------------------|--------------------------------------|
| ors.services.routing    | object | 路线规划及其 profile 的设置 | [routing](#orsservicesrouting)       | 
| ors.services.isochrones | object | 等时圈限制相关设置  | [isochrones](#orsservicesisochrones) | 
| ors.services.matrix     | object | 矩阵限制相关设置      | [matrix](#orsservicesmatrix)         | 
| ors.services.snap       | object | snap 相关设置                     | [matrix](#orsservicesmatrix)         | 

---

#### ors.services.routing

| 键                    | 类型    | 描述                                                                                                                                                                                                             | 示例值                                        |
|------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|      
| enabled                | boolean | 启用或禁用（true/false）该端点。默认值为 true。                                                                                                                                                  | `true`                                               |
| mode                   | string  | 可以是 "normal" 或 "preparation"。若为 "preparation"，则会构建图，随后服务将被关闭。                                                                                       | `"normal"`                                           |
| description            | string  |                                                                                                                                                                                                                         | `"This is a routing file from openrouteservice"`     |
| routing_name           | string  | 指定 gpx 响应中返回的 gpx `name` 标签                                                                                                                                                         | `"openrouteservice"`                                 |
| sources                | list    | 要使用的 osm 文件，支持的格式为 `.osm`、`.osm.gz`、`.osm.zip` 和 `.pbf`                                                                                                                                 | `["heidelberg.osm.gz"]`                              |
| init_threads           | number  | 用于初始化（构建/加载）图的线程数。数值越大需要越多 RAM。                                                                                                                         | `2`                                                  |
| attribution            | string  | 添加到响应元数据中的署名信息                                                                                                                                                                              | `"openrouteservice.org, OpenStreetMap contributors"` |
| elevation_preprocessed | boolean | 启用或禁用以读取节点 ele 标签。默认值为 false。若启用，则不再使用 GH 的高程查询，所有没有 ele 标签的节点将默认为 0。实验性功能，用于 ORS 预处理器 | `false`                                              |
| profiles               | object  |                                                                                                                                                                                                                         | [profiles](#orsservicesroutingprofiles)              |

---

##### **ors.services.routing.profiles**

| 键            | 类型   | 描述                                                                                                                                                                                                                                                                                                    | 示例值                                               |
|----------------|--------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|      
| active         | list   | 定义活跃的路线 profile 列表。元素名 XXX 必须对应 "profile-XXX" 的写法，该写法会在后续各节中使用。可以是 `"car"`、`"hgv"`、`"bike-regular"`、`"bike-mountain"`、`"bike-road"`、`"bike-electric"`、`"walking"`、`"hiking"` 或 `"wheelchair"` 中的一个或多个 | `["car", "bike-regular"]`                                   |
| default_params | object | 设置默认应用于每个 profile 的参数                                                                                                                                                                                                                                                     | [default_params](#orsservicesroutingprofilesdefault_params) |
| profile-XXX    | object | 应用于特定 profile 的设置。你可以同时运行多个 profile。                                                                                                                                                                                                                      | [profile-XXX](#orsservicesroutingprofilesprofile-xxx)       |

---

##### **ors.services.routing.profiles.default_params**

| 键                                               | 类型    | 描述                                                                                                                                                                                                                                                                                                                                                       | 示例值                                                       |
|---------------------------------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------| 
| encoder_flags_size                                | number  | 用于 FlagEncoders 的字节数                                                                                                                                                                                                                                                                                                                         | `8`                                                                 |
| graphs_root_path                                  | string  | 存放图的目录根路径                                                                                                                                                                                                                                                                                                                   | `"../graphs"`                                                       |
| elevation_provider                                | string  | 高程提供方的名称。可能的值为 multi、cgiar 或 srtm                                                                                                                                                                                                                                                                                                                       | `"multi"`                                                           |
| elevation_cache_path                              | string  | 用于存放 SRTM 瓦片的目录路径                                                                                                                                                                                                                                                                                                        | `"elevation_cache"`                                                 |
| elevation_cache_clear                             | boolean | 下载后是否保留高程数据                                                                                                                                                                                                                                                                                                                   | `false`                                                             |
| elevation_smoothing                               | boolean | 对高程数据进行平滑处理                                                                                                                                                                                                                                                                                                                                         | `false`                                                             |
| instructions                                      | boolean | 指定导入期间是否存储道路名称                                                                                                                                                                                                                                                                                               | `true`                                                              |
| maximum_distance                                  | number  | 一条路线允许的最大总距离                                                                                                                                                                                                                                                                                                                     | `100000`                                                            |
| maximum_segment_distance_<br>with_dynamic_weights | number  | 使用动态权重时两个路径点之间允许的最大距离                                                                                                                                                                                                                                                                                 | `50000`                                                             |
| maximum_waypoints                                 | number  | 一次请求中允许的最大路径点数量                                                                                                                                                                                                                                                                                                                     | `50`                                                                |
| maximum_snapping_radius                           | number  | 给定坐标周围可查找可连接边的最大距离                                                                                                                                                                                                                                                                                              | `100`                                                               |
| maximum_distance_round_trip_routes                | number  | 往返（round trip）算法允许的路线最大总距离                                                                                                                                                                                                                                                                                             | `100000`                                                            |
| maximum_distance_alternative_routes               | number  | 备选路线（alternative route）算法允许的路线最大总距离                                                                                                                                                                                                                                                                                      | `100000`                                                            |
| maximum_alternative_routes                        | number  | 一次请求中备选路线的最大数量                                                                                                                                                                                                                                                                                                             | `3`                                                                 |
| maximum_avoid_polygon_area                        | number  | 多边形允许的最大总面积，单位为平方公里，可选                                                                                                                                                                                                                                                                                        | `200000000`                                                         |
| maximum_avoid_polygon_extent                      | number  | 多边形允许的最大范围（即外接框边长），单位为公里，可选                                                                                                                                                                                                                                                                               | `20000`                                                             |
| location_index_resolution                         | number  | 位置索引中瓦片的最小分辨率，单位为米。值越小查询越快，但内存需求更高。降低分辨率会缩小查找半径，可通过增大 `location_index_search_iterations` 来补偿。对应 GraphHopper 的 `index.high_resolution` 配置参数。         | `500` (default)                                                     |
| location_index_search_iterations                  | number  | 坐标查找中执行的最大迭代次数。值越大搜索范围越广，但可能降低查询性能。它只影响存储查找，不影响其布局，因此修改该参数无需重建位置索引。对应 GraphHopper 的 `index.max_region_search` 配置参数。 | `4` (default)                                                       |
| maximum_speed_lower_bound                         | number  | 指定查询参数 `maximum_speed` 的阈值。                                                                                                                                                                                                                                                                                                  | `80` (default)                                                      |
| interpolate_bridges_and_tunnels                   | boolean | 对桥梁和隧道的高程进行插值。                                                                                                                                                                                                                                                                                                                     | `true` (default)                                                    | 
| preparation                                       | object  | ...                                                                                                                                                                                                                                                                                                                                                               | [preparation](#orsservicesroutingprofilesdefault_paramspreparation) |
| execution                                         | object  | ...                                                                                                                                                                                                                                                                                                                                                               | [execution](#orsservicesroutingprofilesdefault_paramsexecution)     |    `100` 

---

##### **ors.services.routing.profiles.default_params.preparation**

| 键                      | 类型   | 描述 | 示例值                                                          |
|--------------------------|--------|-------------|------------------------------------------------------------------------| 
| min_network_size         | number |             | `200`                                                                  |
| min_one_way_network_size | number |             | `200`                                                                  |
| methods                  | object |             | [methods](#orsservicesroutingprofilesdefault_paramspreparationmethods) |     

##### **ors.services.routing.profiles.default_params.preparation.methods**

| 键  | 类型   | 描述                                        | 示例值                                                           |
|------|--------|----------------------------------------------------|-------------------------------------------------------------------------| 
| ch   | object | 收缩层级（contraction hierarchies）预处理设置 | [ch](#orsservicesroutingprofilesdefault_paramspreparationmethodsch)     |
| lm   | object | 地标（landmarks）预处理设置               | [lm](#orsservicesroutingprofilesdefault_paramspreparationmethodslm)     |
| core | object | 地标（landmarks）预处理设置               | [core](#orsservicesroutingprofilesdefault_paramspreparationmethodscore) |

##### **ors.services.routing.profiles.default_params.preparation.methods.ch**

| 键        | 类型    | 描述 | 示例值   |
|------------|---------|-------------|-----------------| 
| enabled    | boolean |             | `true`          |
| threads    | number  |             | `1`             |
| weightings | string  |             | `"recommended"` |

##### **ors.services.routing.profiles.default_params.preparation.methods.lm**

| 键        | 类型    | 描述                                                                                                                                                         | 示例值            |
|------------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------| 
| enabled    | boolean |                                                                                                                                                                     | `true`                   |
| threads    | number  |                                                                                                                                                                     | `1`                      |
| weightings | string  |                                                                                                                                                                     | `"recommended,shortest"` |
| landmarks  | number  | 预计算的地标总数，查询时使用的子集在 [`active_landmarks`](#orsservicesroutingprofilesdefault_paramsexecutionmethodslm) 中设置 | `16`                     |

##### **ors.services.routing.profiles.default_params.preparation.methods.core**

| 键        | 类型    | 描述                                                                                                                                                           | 示例值                                                 |
|------------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------| 
| enabled    | boolean |                                                                                                                                                                       | `true`                                                        |
| threads    | number  |                                                                                                                                                                       | `1`                                                           |
| weightings | string  |                                                                                                                                                                       | `"recommended,shortest"`                                      |
| landmarks  | number  | 预计算的地标总数，查询时使用的子集在 [`active_landmarks`](#orsservicesroutingprofilesdefault_paramsexecutionmethodscore) 中设置 | `32`                                                          |
| lmsets     | string  |                                                                                                                                                                       | `"highways,tollways;highways;tollways;country_193;allow_all"` |

---

##### **ors.services.routing.profiles.default_params.execution**

| 键     | 类型   | 描述 | 示例值                                                        |
|---------|--------|-------------|----------------------------------------------------------------------| 
| methods | object |             | [methods](#orsservicesroutingprofilesdefault_paramsexecutionmethods) |

##### **ors.services.routing.profiles.default_params.execution.methods**

| 键  | 类型   | 描述                                           | 示例值                                                         |
|------|--------|-------------------------------------------------------|-----------------------------------------------------------------------| 
| ch   | object | 在路线规划中使用收缩层级的设置 | [ch](#orsservicesroutingprofilesdefault_paramsexecutionmethodsch)     |
| lm   | object | 在路线规划中使用地标的设置               | [lm](#orsservicesroutingprofilesdefault_paramsexecutionmethodslm)     |
| core | object | 在路线规划中使用地标的设置               | [core](#orsservicesroutingprofilesdefault_paramsexecutionmethodscore) |

##### **ors.services.routing.profiles.default_params.execution.methods.ch**

| 键               | 类型    | 描述 | 示例值 |
|-------------------|---------|-------------|---------------| 
| disabling_allowed | boolean |             | `true`        |

##### **ors.services.routing.profiles.default_params.execution.methods.lm**

| 键               | 类型    | 描述                                      | 示例值 |
|-------------------|---------|--------------------------------------------------|---------------| 
| disabling_allowed | boolean |                                                  | `true`        |
| active_landmarks  | number  | 用于计算路线的地标数量 | `8`           |

##### **ors.services.routing.profiles.default_params.execution.methods.core**

| 键               | 类型    | 描述                                      | 示例值 |
|-------------------|---------|--------------------------------------------------|---------------| 
| disabling_allowed | boolean |                                                  | `true`        |
| active_landmarks  | number  | 用于计算路线的地标数量 | `6`           |

---

##### **ors.services.routing.profiles.profile-XXX**

| 键        | 类型   | 描述                                   | 示例值                                                  |
|------------|--------|-----------------------------------------------|----------------------------------------------------------------| 
| profiles   | string | 通过 API 调用时的 profile 名称   | `driving-car`                                                  |
| parameters | object | 某个 profile 的特定参数 | [parameters](#orsservicesroutingprofilesprofile-xxxparameters) |

##### **ors.services.routing.profiles.profile-XXX.parameters**

| 键                                 | 类型    | 描述                                                                                       | 示例值                                                                     |
|-------------------------------------|---------|---------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------| 
| encoder_options                     | string  |                                                                                                   | `"turn_costs=true\                                                                |block_fords=false"` [encoder options](#orsservicesroutingprofilesprofile-xxxparametersencoder_options) |
| elevation                           | boolean | 这将在响应中启用高程信息                                        | `true`                                                                            |
| maximum_distance                    | number  | 一条路线可拥有的最大距离，单位为米                                                       | `100000`                                                                          |
| maximum_snapping_radius             | number  | 给定坐标周围可查找可连接边的最大距离                              | `100`                                                                             |
| maximum_distance_round_trip_routes  | number  | 往返路线算法允许的路线最大总距离                       | `100000`                                                                          |
| maximum_distance_alternative_routes | number  | 备选路线算法允许的路线最大总距离                      | `100000`                                                                          |
| maximum_alternative_routes          | number  | 一次请求中备选路线的最大数量                                             | `3`                                                                               |
| maximum_avoid_polygon_area          | number  | 多边形允许的最大总面积，单位为平方公里，可选                        | `200000000`                                                                       |
| maximum_avoid_polygon_extent        | number  | 多边形允许的最大范围（即外接框边长），单位为公里，可选               | `20000`                                                                           |
| ext_storages                        | object  | 控制启用哪些外部存储                                                      | [external storages](#orsservicesroutingprofilesprofile-xxxparametersext_storages) |
| gtfs_file                           | string  | 仅用于 pt profile：所使用的 gtfs 文件位置。可以是 zip 文件或解压后的文件夹。 | `"src/test/files/vrn_gtfs_cut.zip"`                                               |

##### **ors.services.routing.profiles.profile-XXX.parameters.encoder_options**

| 键                      | 类型   | 描述                                                                                                                                                                       | 示例值                  |
|--------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------| 
| problematic_speed_factor | number | 仅用于 wheelchair profile！对轮椅用户而言被判定为 problematic 的边，其通行速度会乘以该系数，用于在这类道路上设置较慢的通行速度   | `problematic_speed_factor=0.7` |
| preferred_speed_factor   | number | 仅用于 wheelchair profile！对轮椅用户而言被判定为 preferable 的边，其通行速度会乘以该系数，用于在这类道路上设置较快的通行速度 | `preferred_speed_factor=1.2`   |

##### **ors.services.routing.profiles.profile-XXX.parameters.ext_storages**

| 键                    | 类型   | 描述                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | 示例值                                                                  |
|------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------| 
| WayCategory            | object | 在路线响应中返回道路类别，适用于任何 profile 类型                                                                                                                                                                                                                                                                                                                                                                                                               | `{}`                                                                           |
| WaySurfaceType         | object | 在路线响应中返回路面类型，适用于任何 profile 类型                                                                                                                                                                                                                                                                                                                                                                                                                | `{}`                                                                           |
| HillIndex              | object | 在路线响应中返回上坡/下坡数据，适用于任何 profile 类型                                                                                                                                                                                                                                                                                                                                                                                                             | `{}`                                                                           |
| TrailDifficulty        | object | 在路线响应中返回小径难度，适用于 profile-hiking                                                                                                                                                                                                                                                                                                                                                                                                             | `{}`                                                                           |
| RoadAccessRestrictions | object | RoadAccessRestrictions 表示道路在特定情况下对特定车辆的限制，例如 access=destination。use_for_warnings 参数告知 ors 该存储可用于在路线响应中生成警告消息。就 RoadAccessRestrictions 而言，这意味着当路线经过带有限制条件的道路时，响应中会附带警告消息，并自动添加 roadaccessrestrictions extra info。 | `{ use_for_warnings: true }`                                                   |
| Wheelchair             | object | 适用于 wheelchair                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | `{ KerbsOnCrossings: "true" }`                                                 |
| OsmId                  | object | 返回道路的 OsmId，适用于 wheelchair                                                                                                                                                                                                                                                                                                                                                                                                                                       | `{}`                                                                           |
| Borders                | object | Borders 允许限制路线不跨越国界，适用于任何 profile 类型                                                                                                                                                                                                                                                                                                                                                                                        | [Borders](#orsservicesroutingprofilesprofile-xxxparametersext_storagesborders) |    

##### **ors.services.routing.profiles.profile-XXX.parameters.ext_storages.Borders**

| 键         | 类型   | 描述                                                                                         | 示例值              |
|-------------|--------|-----------------------------------------------------------------------------------------------------|----------------------------| 
| boundaries  | string | 包含表示各国边界的 geojson 数据的文件路径                    | `'borders.geojson.tar.gz'` |
| ids         | string | 指向 csv 文件的路径，其中包含每个国家的唯一 id、本地名称和英文名称     | `'ids.csv'`                |
| openborders | string | 指向 csv 文件的路径，其中包含边界开放的国家对（即申根边界） | `'openborders.csv'`        |

---

#### ors.services.isochrones

| 键                    | 类型    | 描述                                                                                                                                | 示例值                                                                              |
|------------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------| 
| enabled                | boolean | 启用或禁用（true/false）该端点（默认：true）                                                                             | `true`                                                                                     |
| maximum_range_distance | list    | maximum_range_distance 与 maximum_range_time 的取值可以是整数，也可以是针对每个 profile 单独定义的值列表 | `[{profiles: "any", value: 50000}, {profiles: "driving-car, driving-hgv", value: 100000}]` |
| maximum_range_time     | list    |                                                                                                                                            | `[{profiles: "any", value: 18000},{profiles: "driving-car, driving-hgv", value: 3600}]`    |
| maximum_intervals      | number  | 为每个位置计算的最大间隔/等时圈数量                                                                          | `10`                                                                                       |
| maximum_locations      | number  | 一次请求中的最大位置数量                                                                                                 | `2`                                                                                        |
| allow_compute_area     | number  | 指定是否允许进行面积计算                                                                                              | `true`                                                                                     |

##### **ors.services.isochrones.fastisochrones**

| 键                    | 类型 | 描述                                                                                                                                | 示例值                                                                              |
|------------------------|------|--------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------| 
| maximum_range_distance | list | maximum_range_distance 与 maximum_range_time 的取值可以是整数，也可以是针对每个 profile 单独定义的值列表 | `[{profiles: "any", value: 50000}, {profiles: "driving-car, driving-hgv", value: 100000}]` |
| maximum_range_time     | list |                                                                                                                                            | `[{profiles: "any", value: 18000},{profiles: "driving-car, driving-hgv", value: 3600}]`    |

##### **ors.services.isochrones.fastisochrones.profiles.default_params**

| 键          | 类型    | 描述                                              | 示例值            |
|--------------|---------|----------------------------------------------------------|--------------------------|
| enabled      | boolean |                                                          | `true`                   |
| threads      | number  |                                                          | `1`                      |
| weightings   | string  |                                                          | `"recommended,shortest"` |
| maxcellnodes | number  | 单个等时圈单元格中允许的最大节点数 | `5000`                   |

---

#### ors.services.matrix

| 键                     | 类型    | 描述                                                                                                                 | 示例值                                        |
|-------------------------|---------|-----------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------| 
| enabled                 | boolean | 启用或禁用（true/false）该端点（默认：true）                                                              | `true`                                               |
| maximum_routes          | number  | 矩阵应计算的最大路线数量。例如 `2500` 可以是 50x50 的矩阵，也可以是 1x2500 的矩阵                    | `2500`                                               |
| maximum_routes_flexible | number  | 使用不支持收缩层级的自定义 profile 时允许的最大路线数量                              | `25`                                                 |
| maximum_search_radius   | number  | 请求坐标与最近道路上某点之间允许的最大距离。该值以米为单位  | `5000`                                               |
| maximum_visited_nodes   | number  | 最短路径计算中允许访问的最大节点数。该阈值仅适用于 Dijkstra 算法 | `100000`                                             |
| allow_resolve_locations | number  | 指定是否可以解析出距离该位置最近的街道名称。默认值为 true                | `true`                                               |
| attribution             | string  | 添加到响应元数据中的署名信息                                                                                  | `"openrouteservice.org, OpenStreetMap contributors"` |

---
#### ors.services.snap

| 键                     | 类型    | 描述                                                    | 示例值                                        |
|-------------------------|---------|----------------------------------------------------------------|------------------------------------------------------| 
| enabled                 | boolean | 启用或禁用（true/false）该端点（默认：true） | `true`                                               |
| attribution             | string  | 添加到响应元数据中的署名信息                     | `"openrouteservice.org, OpenStreetMap contributors"` |

---

#### ors.logging

| 键        | 类型    | 描述                                                     | 示例值          |
|------------|---------|-----------------------------------------------------------------|------------------------| 
| enabled    | boolean | 启用或禁用该端点（默认：true）               | `true`                 |
| level_file | string  | 可以是 `DEBUG_LOGGING.json` 或 `PRODUCTION_LOGGING.json` | `"DEBUG_LOGGING.json"` |
| location   | string  | 日志存放位置                                            | `"/var/log/ors"`       |
| stdout     | boolean |                                                                 | `true`                 |

---

#### ors.system_message

消息对象数组，每个对象包含

| 键       | 类型    | 描述                                                       | 示例值        |
|-----------|---------|-------------------------------------------------------------------|----------------------| 
| active    | boolean | 启用或禁用该消息                                  | `true`               |
| text      | string  | 消息文本                                                  | `"The message text"` |
| condition | object  | 可省略；可包含下表任意条件 |                      |

| 条件          | 取值                                                                                                                | 描述                                                         |
|--------------------|----------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| time_before        | ISO 8601 datetime string                                                                                             | 当服务器本地时间早于给定时间点时发送该消息     |
| time_after         | ISO 8601 datetime string                                                                                             | 当服务器本地时间晚于给定时间点时发送该消息      |
| api_version        | 1 or 2                                                                                                               | 当请求的 API 版本与值匹配时发送该消息         |
| api_format         | String with output formats ("json", "geojson", "gpx"), comma separated                                               | 当请求的输出格式与值匹配时发送该消息               |
| request_service    | String with service names ("routing", "matrix", "isochrones"), comma separated                                       | 当请求的服务与给定名称之一匹配时发送该消息    |
| request_profile    | String with profile names, comma separated                                                                           | 当请求的 profile 与给定名称之一匹配时发送该消息    |
| request_preference | String with preference (weightings for routing, metrics for matrix, rangetype for isochrones) names, comma separated | 当请求的偏好（preference）与给定名称之一匹配时发送该消息 |

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
