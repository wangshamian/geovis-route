# `ors.endpoints.isochrones`

isochrones 端点的设置。

| key                            | type    | description                                                                                        | default value                                      |
|--------------------------------|---------|----------------------------------------------------------------------------------------------------|----------------------------------------------------|
| enabled                        | boolean | 启用或禁用该端点                                                                                    | `true`                                             |
| attribution                    | string  | 添加到响应元数据中的署名信息                                                                        | `openrouteservice.org, OpenStreetMap contributors` |
| maximum_locations              | number  | 每次请求的最大位置数量                                                                              | `2`                                                |
| allow_compute_area             | boolean | 开启或关闭面积计算                                                                                  | `true`                                             |
| maximum_intervals              | number  | 为每个位置计算的最大间隔/等时圈数量                                                                 | `1`                                                |
| fastisochrones                 | object  | [fastisochrones](#fastisochrones) 的范围限制                                                         |                                                    |
| statistics_providers           | object  | [统计提供程序属性](#statistics-providers)                                                            |                                                    |
| maximum_range_distance_default | number  | 所有配置的默认最大范围（米）；可在 `maximum_range_distance` 中覆盖                                    | `50000`                                            |
| maximum_range_distance         | object  | [最大范围属性](#maximum-range-properties)列表                                                        |                                                    |
| maximum_range_time_default     | number  | 所有配置的默认最大范围（秒）；可在 `maximum_range_time` 中覆盖                                        | `18000`                                            |
| maximum_range_time             | object  | [最大范围属性](#maximum-range-properties)列表                                                        |                                                    |

## fastisochrones

`ors.endpoints.isochrones.fastisochrones` 下仅供 fastisochrone 使用的属性：

| key                            | type   | description                                                                                        | default value |
|--------------------------------|--------|----------------------------------------------------------------------------------------------------|---------------|
| maximum_range_distance_default | number | 所有配置的默认最大范围（米）；可在 `maximum_range_distance` 中覆盖                                    | `50000`       |
| maximum_range_distance         | object | [`maximum range properties`](#maximum-range-properties) 对象                                        |               |
| maximum_range_time_default     | number | 所有配置的默认最大范围（秒）；可在 `maximum_range_time` 中覆盖                                        | `18000`       |
| maximum_range_time             | object | [`maximum range properties`](#maximum-range-properties) 对象                                        |               |

### maximum_range properties
这些属性可以嵌套在以下各项之下
* `ors.endpoints.isochrones.maximum_range_distance`
* `ors.endpoints.isochrones.maximum_range_time`
* `ors.endpoints.isochrones.fastisochrones.maximum_range_distance`
* `ors.endpoints.isochrones.fastisochrones.maximum_range_time`

| key      | type   | description                     | example value              |
|----------|--------|---------------------------------|----------------------------|
| profiles | list   | 配置名称列表                     | `driving-car, driving-hgv` |
| value    | number | 距离或时间限制的值               | `3600`                     |


## statistics_providers
`ors.endpoints.isochrones.statistics_providers` 下的属性：

| key                 | type    | description                                                                                                  | example value                                   | 
|---------------------|---------|--------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| enabled             | boolean | 启用或禁用该提供程序                                                                                          | true                                            |
| attribution         | string  | 提供程序署名信息                                                                                              | OpenStreetMap contributors                      |
| provider_name       | string  | 用于引用此提供程序的内部名称（无实际作用）                                                                    | my_statistics_provider_1                        |
| provider_parameters | object  | 提供程序配置参数列表                                                                                          | 参见 [provider_parameters](#provider-parameters) |
| property_mapping    | object  | 键值对，其名称为 isochrones 请求中 `attributes` 参数里要请求的值的名称                                        | total_pop: total_pop (only supported currently) |

::: warning
要调整 `property_mapping`，需要在 `IsochronesRequestEnums.java`、
`PostgresSQLStatisticsProvider.java`、`RoutingProfile.java` 和 `GeoJSONIsochroneProperties.java` 中进行更多实现。
:::

### provider_parameters

`ors.endpoints.isochrones.statistics_providers.provider_parameters` 下的属性：

| key             | type   | description                                                | example value       |
|-----------------|--------|------------------------------------------------------------|---------------------|
| host            | string | 运行 postgres/postgis 数据库的主机                          | "localhost",        |
| port            | number | 数据库运行的端口                                            | 5432,               |
| user            | string | 数据库用户                                                  | "admin",            |
| password        | string | 用户的数据库密码                                            | "my_safe_password", |
| db_name         | string | 数据库名称                                                  | "postgres",         |
| table_name      | string | SQL 查询中使用的表名                                        | "pop_data",         |
| geometry_column | string | SQL 查询中使用的列名                                        | "rast",             |
| postgis_version | string | 用于 SQL 查询中条件逻辑的 Postgis 版本                      | "3"                 |
