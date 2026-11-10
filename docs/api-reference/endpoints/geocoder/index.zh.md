# Geocoder 端点

:::warning 注意
该端点不属于 openrouteservice，而属于我们的公共 API。运行自己的 openrouteservice 实例时不可用。 
:::

我们公共 API 的 Geocoder 端点由一个 [Pelias](https://www.pelias.io) 实例提供服务。
它可以把地理坐标解析为地址，反之亦然。

我们的公共 API 提供以下端点：

* [Forward Geocode Service](https://openrouteservice.org/dev/#/api-docs/geocode/search/get)：在最简单的搜索中，你只需提供一个参数，即希望在位置详情的任意部分匹配的文本。为此，构建一个查询，把 text 参数设为你要查找的条目即可。
* [Geocode Autocomplete Service](https://openrouteservice.org/dev/#/api-docs/geocode/autocomplete/get)：这种即输即查功能可帮助用户找到他们想要的内容，而无需完整输入搜索词。通常，用户开始输入后会弹出下拉列表，从中可以选择下方列表中的词条。
* [Structured Forward Geocode Service](https://openrouteservice.org/dev/#/api-docs/geocode/search/structured/get)：结构化地理编码可为 `address`、`postalcode` 等特定字段定义搜索词，从而改进查询中各条目在搜索中的解析与解释方式。_（:warning: 该端点处于 beta 阶段。）_
* [Reverse Geocode Service](https://openrouteservice.org/dev/#/api-docs/geocode/reverse/get)：反向地理编码用于查找某个经纬度坐标附近的地点或地址——就像在地图上点击以查看那里有什么。

更多详情请参阅 [Pelias 文档](https://github.com/pelias/documentation)。
