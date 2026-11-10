# `ors.endpoints.isochrones`

等时圈端点的设置。

| key                            | type    | description                                                                                        | default value                                      |
|--------------------------------|---------|----------------------------------------------------------------------------------------------------|----------------------------------------------------|
| enabled                        | boolean | 启用或禁用该端点                                                                  | `true`                                             |
| attribution                    | string  | 添加到响应元数据中的署名信息                                                         | `openrouteservice.org, OpenStreetMap contributors` |
| maximum_locations              | number  | 每个请求的最大位置数量                                                            | `2`                                                |
| allow_compute_area             | boolean | 开启或关闭面积计算                                                                | `true`                                             |
| maximum_intervals              | number  | 每个位置计算的最大间隔/等时圈数量                                  | `1`                                                |
| fastisochrones                 | object  | [fastisochrones](#fastisochrones)的范围限制                                                 |                                                    |
| statistics_providers           | object  | [统计提供者属性](#statistics-providers)                                           |                                                    |
| maximum_range_distance_default | number  | 所有配置的最大默认范围（米）；可在 `maximum_range_distance` 中覆盖 | `50000`                                            |
| maximum_range_distance         | object  | [最大范围属性](#maximum-range-properties)列表                                      |                                                    |
| maximum_range_time_default     | number  | 所有配置的最大默认范围（秒）；可在 `maximum_range_time` 中覆盖    | `18000`                                            |
| maximum_range_time             | object  | [最大范围属性](#maximum-range-properties)列表                                      |                                                    |

## fastisochrones

仅用于 fastisochrone 的 `ors.endpoints.isochrones.fastisochrones` 下的属性：

| key                            | type   | description                                                                                        | default value |
|--------------------------------|--------|----------------------------------------------------------------------------------------------------|---------------|
| maximum_range_distance_default | number | 所有配置的最大默认范围（米）；可在 `maximum_range_distance` 中覆盖 | `50000`       |
| maximum_range_distance         | object | [`maximum range properties`](#maximum-range-properties) 对象                                     |               |
| maximum_range_time_default     | number | 所有配置的最大默认范围（秒）；可在 `maximum_range_time` 中覆盖    | `18000`       |
| maximum_range_time             | object | [`maximum range properties`](#maximum-range-properties) 对象                                     |               |

### maximum_range 属性
这些属性可以嵌套在以下属性之下
* `ors.endpoints.isochrones.maximum_range_distance`
* `ors.endpoints.isochrones.maximum_range_time`
* `ors.endpoints.isochrones.fastisochrones.maximum_range_distance`
* `ors.endpoints.isochrones.fastisochrones.maximum_range_time`

| key      | type   | description                     | example value              |
|----------|--------|---------------------------------|----------------------------|
| profiles | list   | 配置名称列表           | `driving-car, driving-hgv` |
| value    | number | 距离或时间限制的值 | `3600`                     |


## statistics_providers
`ors.endpoints.isochrones.statistics_providers` 下的属性：

| key                 | type    | description                                                                                                  | example value                                   |
|---------------------|---------|--------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| enabled             | boolean | 启用或禁用该提供者                                                                             | true                                            |
| attribution         | string  | 提供者署名                                                                                         | OpenStreetMap contributors                      |
| provider_name       | string  | 用于引用此提供者的内部名称（无实际作用）                                                    | my_statistics_provider_1                        |
| provider_parameters | object  | 提供者配置参数列表                                                                  | 参见 [provider_parameters](#provider-parameters) |
| property_mapping    | object  | 键值对，键为等时圈请求 `attributes` 参数中要请求的值名称 | total_pop: total_pop (only supported currently) |

::: warning
要进一步调整 `property_mapping`，还需要在 `IsochronesRequestEnums.java`、
`PostgresSQLStatisticsProvider.java`、`RoutingProfile.java` 和 `GeoJSONIsochroneProperties.java` 中实现相应改动。
:::

### provider_parameters

`ors.endpoints.isochrones.statistics_providers.provider_parameters` 下的属性：

| key             | type   | description                                                | example value       |
|-----------------|--------|------------------------------------------------------------|---------------------|
| host            | string | 运行 postgres/postgis 数据库的主机           | "localhost",        |
| port            | number | 数据库运行的端口                                  | 5432,               |
| user            | string | 数据库用户                                                    | "admin",            |
| password        | string | 用户的数据库密码                                       | "my_safe_password", |
| db_name         | string | 数据库的名称                                       | "postgres",         |
| table_name      | string | SQL 查询中使用的表名                     | "pop_data",         |
| geometry_column | string | SQL 查询中使用的列名                        | "rast",             |
| postgis_version | string | SQL 查询中用于条件逻辑的 Postgis 版本 | "3"                 |
