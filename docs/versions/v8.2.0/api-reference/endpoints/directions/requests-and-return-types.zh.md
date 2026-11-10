# 请求与返回类型

## Directions 请求

共有四种 POST directions 请求，它们都接受相同的请求体，仅在结果类型上有所不同，结果类型由最后一个路径参数指定：

    POST /v2/directions/{profile}            <- returns JSON
    POST /v2/directions/{profile}/json
    POST /v2/directions/{profile}/geojson
    POST /v2/directions/{profile}/gpx

此外，还有一种简单的 GET 请求，它不允许使用高级请求选项。它返回 GeoJSON：

    GET  /v2/directions/{profile}            <- returns GeoJSON


## 返回类型

### JSON 

**JSON** 返回类型最适合用于后续处理。

### GPX

**GPX** 返回类型是 openrouteservice 基于 [GPS Exchange Format](https://www.topografix.com/gpx.asp) 的一种 XML 方言，并配有自有的 [XML Schema](https://raw.githubusercontent.com/GIScience/openrouteservice-schema/main/gpx/v2/ors-gpx.xsd)。
它是一个非常古老的 GPS 数据轻量交换标准，因此被数百个软件程序和 Web 服务所使用。

### GeoJSON

**GeoJSON** 是一种用于编码各种地理数据结构的格式，参见 [geojson.org](https://datatracker.ietf.org/doc/html/rfc7946)。
它被广泛使用，因此可以在许多应用中轻松处理或展示，例如在 [QGIS](https://qgis.org/) 中或 [geojson.io](http://geojson.io/) 上。

关于结果类型的更多信息可以在 [API Playground](https://openrouteservice.org/dev/#/api-docs/directions_service) 中找到。
