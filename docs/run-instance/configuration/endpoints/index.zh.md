# `ors.endpoints`

`ors.endpoints` 下的设置在运行时处理 API 请求时需要。
有关可用端点的使用详情，请参见[端点参考](/api-reference/endpoints/)。

查询时相关的更多参数，如果特定于所查询的[配置](/run-instance/configuration/engine/profiles/index.md)，
可以在 [ors.engine.profiles.\<profile\>.service](/run-instance/configuration/engine/profiles/service.md) 部分找到。

| key        | type   | description                                                                                                                    |
|------------|--------|--------------------------------------------------------------------------------------------------------------------------------|
| defaults   | object | 与所有端点相关的[参数默认值](defaults.zh.md)                                                                   |
| routing    | object | [路由端点的参数](routing.zh.md)（也称为 [Directions 端点](/api-reference/endpoints/directions/)）    |
| isochrones | object | [等时圈端点的参数](isochrones.zh.md)（另请参见 [Isochrones 端点](/api-reference/endpoints/isochrones/)） |
| matrix     | object | [矩阵端点的参数](matrix.zh.md)（另请参见 [Matrix 端点](/api-reference/endpoints/matrix/)）                 |
| snap       | object | [吸附端点的参数](snap.zh.md)（另请参见 [Snapping 端点](/api-reference/endpoints/snapping/)）             |
