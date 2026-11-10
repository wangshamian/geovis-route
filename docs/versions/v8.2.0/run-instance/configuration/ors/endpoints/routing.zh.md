
# `ors.endpoints.routing`

路由端点的设置。

| key                          | type    | description                                                                            | default value                                                                    |
|------------------------------|---------|----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| enabled                      | boolean | 启用或禁用该端点                                                                        | `true`                                                                           |
| attribution                  | string  | 添加到响应元数据中的署名信息                                                            | `openrouteservice.org, OpenStreetMap contributors, tmc - BASt`                   |
| gpx_name                     | string  | 指定 gpx 响应 `metadata` 中返回的 `name` 标签                                            | `ORSRouting`                                                                     |
| gpx_description              | string  | 指定 gpx 响应 `metadata` 中返回的 `desc` 标签                                            | `This is a directions instructions file as GPX, generated from openrouteservice` |
| gpx_base_url                 | string  | 指定 gpx 响应 `metadata/author` 中返回的 `link` 标签                                     | `https://openrouteservice.org/`                                                  |
| gpx_support_mail             | string  | 指定 gpx 响应 `metadata/author` 中返回的 `email` 标签                                    | `support@openrouteservice.org`                                                   |
| gpx_author                   | string  | 指定 gpx 响应 `metadata/author` 中返回的 `name` 标签                                     | `openrouteservice`                                                               |
| gpx_content_licence          | string  | 指定 gpx 响应 `metadata/copyright` 中返回的 `license` 标签                               | `LGPL 3.0`                                                                       |
| maximum_avoid_polygon_area   | number  | 允许的多边形总面积最大值（平方千米），可选                                              | `200000000`                                                                      |
| maximum_avoid_polygon_extent | number  | 允许的多边形范围（即包络边长）最大值（千米），可选                                      | `20000`                                                                          |
| maximum_alternative_routes   | number  | 一次请求中备选路径的最大数量                                                            | `3`                                                                              |
