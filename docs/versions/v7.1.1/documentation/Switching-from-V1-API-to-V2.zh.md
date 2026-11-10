# 从 V1 API 迁移到 V2

不久之后（2020 年 10 月 30 日），V1 openrouteservice API 将被关闭，因此在此之前务必更新你的查询以反映这一变化。相比 V1 API，V2 API 提供了一些很棒的优点，包括：
* **所有请求信息都通过请求体发送，而无需再编码到 URL 中。这让查询的编写和调试变得容易得多！**
* **URL 的结构更便于请求不同的响应数据类型，也便于看出你正在查询哪个 profile**

那么如何从 V1 切换到 V2 API 呢？最大的区别在于，主要的请求信息需要放在请求体中，并且请求要以 POST 方式发出。请求体本身是简洁的 JSON 格式，使用真正的数组，而不再是 V1 GET 方法中用竖线（|）分隔的列表。此外，URL 本身也改成如下格式：
`https://api.openrouteservice.org/v2/{endpoint}/{profile}`
其中 `{endpoint}` 是 `directions`、`isochrones` 或 `matrix` 之一，`{profile}` 是你要为其获取路线的 profile。另外，`directions` 端点还可以在 profile 之后加上响应数据格式，例如 `https://api.openrouteservice.org/v2/directions/driving-car/{response-format}`，其中 `{response-format}` 是 `json`、`geojson` 或 `gpx` 之一。

**请注意，只有 `directions`、`isochrones` 和 `matrix` 端点使用 V2 API。其他所有端点（例如 geocoding）都与此前保持一致！**

以上就是基本的变化，下面举一些如何更改请求的例子。

## Directions

我们先从一个基本的 car profile directions 请求开始：你有一个起点、终点和一个途经点，想避开高速公路，并且想避开法国。在 V1 API 中，你的请求会是：

`https://api.openrouteservice.org/directions?api_key=xxx&coordinates=8.684692,49.40025|7.849731,48.046874|7.36084,49.231947&profile=driving-car&options={"avoid_features":"highways","avoid_countries":"70|193"}&geometry_format=geojson`

或者其编码后的形式

`https://api.openrouteservice.org/directions?api_key=xxx&coordinates=8.684692,49.40025%7C7.849731,48.046874%7C7.36084,49.231947&profile=driving-car&options=%7B%22avoid_features%22:%22highways%22,%22%22avoid_countries%22%3A%2270%7C193%22%7D&geometry_format=geojson`

使用 V2 API，同样的请求如下：

`https://api.openrouteservice.org/v2/directions/driving-car/geojson?api_key=xxx`

POST 请求体为：

```json
{
	"coordinates": [
		[8.684692,49.40025],
		[7.849731,48.046874],
		[7.36084,49.231947]
	],
	"options": {
		"avoid_countries": ["FRA","CHE"],
		"avoid_features": ["highways"]
	}
}
```

如你所见，这样可读性高多了！

## Isochrones

对于 Isochrones 端点，要请求 2 个位置、最长 60 分钟、间隔相等且为 10 分钟的步行等时圈，在 V1 API 中你会发送以下请求：

`https://api.openrouteservice.org/isochrones?api_key=5b3ce3597851110001cf62480fd03f0ff4c14a33a72cc528290f2823&profile=foot-walking&locations=40.38939,%2056.148562&range=3600&range_type=time&interval=600`

在 V2 API 中，做法如下（同样以 POST 方式）：

`https://api.openrouteservice.org/v2/isochrones/foot-walking?api_key=xxx`

请求体如下：

```json
{
	"locations": [
		[8.681495,49.41461],
		[8.686507,49.41943]
	],
	"range": [3600],
	"interval": 360,
	"range_type": "time"
}
```

## Matrix

最后，对于一个使用 4 个位置的 HGV Matrix 请求——列表中最后 2 个是终点，返回距离和时间，距离单位为公里——在 V1 中你会请求：

`https://api.openrouteservice.org/matrix?api_key=xxx&profile=driving-hgv&locations=9.970093,48.477473|9.207916,49.153868|37.573242,55.801281|115.663757,38.106467&destinations=2,3&metrics=distance|duration&units=km`

或者其编码后的形式：

`https://api.openrouteservice.org/matrix?api_key=xxx&profile=driving-hgv&locations=9.970093,48.477473%7C9.207916,49.153868%7C37.573242,55.801281%7C115.663757,38.106467&destinations=2,3&metrics=distance%7Cduration&units=km`

在 V2 中，同样的请求应使用以下 POST 请求：

`https://api.openrouteservice.org/v2/matrix/driving-hgv?api_key=xxx`

```json
{
	"locations": [
		[9.70093,48.477473],
		[9.207916,49.153868],
		[37.573242,55.801281],
		[115.663757,38.106467]
	],
	"destinations": [2,3],
	"metrics": ["distance","duration"],
	"units": "km"
}
```

以上就是把 V1 API 请求转换为 V2 API 请求的基本方法。如你所见，在新版 API 中可读性要好得多，而且还有一些 V1 API 中不具备的额外功能。

你可以在我们的[交互式文档](https://openrouteservice.org/dev/#/api-docs/)中更好地了解各种请求及其构建方式。
