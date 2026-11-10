# Isochrones 端点

Isochrone 服务支持对单个或多个位置进行时间和距离分析。
既可以指定等时圈间隔，也可以提供多个精确的等时圈范围值。
该服务支持与 `/directions` 端点相同的 profile 选项范围，
可帮助你进一步定制请求，以获得更详细的可达区域响应。

结果是一个 GeoJSON，其中等时圈多边形以 `features` 表示：

```jsonpath
$.features
```

关于 isochrones 端点全部特性的概览，请参阅 [API Playground](https://openrouteservice.org/dev/#/api-docs/isochrones_service)。
