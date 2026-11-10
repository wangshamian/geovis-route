
# `ors.endpoints.snap`

snapping 端点的设置。

| key               | type    | description                                | default value                                      |
|-------------------|---------|--------------------------------------------|----------------------------------------------------|
| enabled           | boolean | 启用或禁用该端点                           | `true`                                             |
| attribution       | string  | 添加到响应元数据中的署名信息               | `openrouteservice.org, OpenStreetMap contributors` |
| maximum_locations | number  | 每次请求的最大位置数量                     | `5000`                                             |
