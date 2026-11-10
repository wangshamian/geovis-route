# 数据：输入与输出文件夹及文件

## 输入文件

最重要的输入文件是配置文件。
在[配置](configuration/index.md)章节中，你可以找到关于配置选项和配置文件的所有内容。

Openrouteservice 使用公共开源数据。为了生成最佳路径，它使用了若干不同的数据集。

### OSM 数据
用于道路网络及相关信息（道路类型、通行限制等）的基础数据是 [OpenStreetMap](https://openstreetmap.org)（OSM）。该数据集是免费的开放数据集，任何人都可以编辑。  
你可以从 https://planet.openstreetmap.org/ 下载最新的 OSM 数据集，或从 http://download.geofabrik.de/ 下载区域摘录。

要使用的 OSM 文件必须通过属性 [`ors.engine.source_file`](configuration/ors/engine/index.md) 进行配置。

### 高程
用于高程的数据是 [SRTM](http://srtm.csi.cgiar.org/) 和 [GMTED](https://www.usgs.gov/coastal-changes-and-impacts/gmted2010)。  

配置：[`ors.engine.elevation`](configuration/ors/engine/elevation.md)

### 人口
在请求等时圈时，你还可以请求获取等时圈区域的人口数据。用于此目的的数据是欧盟委员会的[全球人类住区层（GHSL）](https://ghsl.jrc.ec.europa.eu/ghs_pop2023.php)。
请注意，虽然该数据集于 2023 年发布，但其中包含的最新数据来自 2020 年。openrouteservice 以 100m 的分辨率使用该数据。

### 边界
与规避边界功能相关的数据源自 OpenStreetMap 中的行政边界要素。有关开放边界的信息来自 [Wikipedia](https://en.wikipedia.org/wiki/Open_border)。

配置参数：[`ors.engine.profiles.*.ext_storages.Borders`](configuration/ors/engine/profiles.md#borders)

### GTFS
公共交通配置集成了 [GTFS](https://developers.google.com/transit/gtfs) 数据，用于公共交通部分。GTFS 数据源可以从例如 https://gtfs.de/（针对德国）或当地公共交通运营商处获取。

配置参数：[`ors.engine.profiles.*.gtfs_file`](configuration/ors/engine/profiles.md)

### 绿色与安静
用于识别绿色和安静路线的数据源自海德堡大学 GIScience 研究组的研究项目。
有关这些内容的更多信息可以在 GIScience 新闻博客上找到：[此处](https://giscienceblog.uni-heidelberg.de/2017/07/03/healthy-routing-prefering-green-areas-added-to-openrouteserviceorg/)和[此处](http://giscienceblog.uni-heidelberg.de/2017/07/10/reducing-stress-by-avoiding-noise-with-quiet-routing-in-openrouteservice/)

配置参数：[`ors.engine.profiles.*.ext_storages`](configuration/ors/engine/profiles.md#ext-storages)


## 输出文件

Openrouteservice 生成三种类型的输出文件，其路径均可配置。这些路径所指向的目录必须*可写*。

### 图

openrouteservice 读取输入数据，并为每个启用的路由配置计算一个图。

图的根目录可以通过配置属性 [`ors.engine.graphs_root_path`](configuration/ors/engine/index.md) 进行配置。

### 高程缓存

如果在配置中启用了高程，openrouteservice 会下载高程数据瓦片并缓存到某个目录中，
该目录可以通过属性 [`ors.engine.elevation.cache_path`](configuration/ors/engine/index.md) 进行配置。

### 日志

日志输出会写入自动轮转的日志文件。
有关配置日志文件位置的详细信息，请参阅[日志](configuration/spring/logging.md)章节。
