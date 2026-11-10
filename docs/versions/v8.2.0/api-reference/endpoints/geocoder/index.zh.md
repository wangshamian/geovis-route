# Geocoder 端点

:::warning 注意
该端点不属于 openrouteservice，而属于我们的公共 API。运行自己的 openrouteservice 实例时不可用。 
:::

我们公共 API 的 Geocoder 端点由 [Pelias](https://www.pelias.io) 实例提供服务。
它用于在地理坐标与地址之间进行解析转换。

我们的公共 API 提供以下端点：

* [正向地理编码服务（Forward Geocode Service）](https://openrouteservice.org/dev/#/api-docs/geocode/search/get)：在最简单的搜索中，你只需提供一个参数，即希望在位置详情的任意部分匹配的文本。为此，请构建一个查询，将 text 参数设置为你要查找的内容。
* [地理编码自动补全服务（Geocode Autocomplete Service）](https://openrouteservice.org/dev/#/api-docs/geocode/autocomplete/get)：这种即时提示功能可以帮助用户找到所需内容，而无需完整输入搜索词。通常用户开始输入时会出现一个下拉列表，用户可以从下方列表中选择词条。
* [结构化正向地理编码服务（Structured Forward Geocode Service）](https://openrouteservice.org/dev/#/api-docs/geocode/search/structured/get)：结构化地理编码可以为 `address`、`postalcode` 等特定字段定义搜索词，从而改进搜索中查询条目的解析与解释方式。_（:warning: 该端点处于 beta 阶段。）_
* [反向地理编码服务（Reverse Geocode Service）](https://openrouteservice.org/dev/#/api-docs/geocode/reverse/get)：反向地理编码用于查找某个经纬度对附近的地点或地址——例如点击地图以查看该处有什么。

如需更多了解，请参阅 [Pelias 文档](https://github.com/pelias/documentation)。
