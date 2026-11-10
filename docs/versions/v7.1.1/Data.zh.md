# 数据
openrouteservice 使用公开的开源数据。为了生成最佳路线，系统会使用多种不同的数据集。

## 基础数据
用于路网及相关信息（道路类型、通行限制等）的基础数据来自 [OpenStreetMap](https://openstreetmap.org)。该数据集是一个自由、开放的数据集，任何人都可以编辑。

## 高程
用于高程的数据来自 [SRTM](http://srtm.csi.cgiar.org/) 和 GMTED。

## 人口
在请求等时圈（isochrones）时，你还可以一并请求等时圈区域内的人口数据。所使用的数据是欧盟委员会的[全球人类住区层（Global Human Settlement Layer, GHSL）](https://human-settlement.emergency.copernicus.eu/ghs_pop2023.php)。openrouteservice 使用的是 2020 年、分辨率为 100m 的数据（数据集年份：2023）。

## 边界
与“避开边界”（avoid borders）功能相关的数据取自 OpenStreetMap 中的行政边界要素。有关开放边界的信息来自 [Wikipedia](https://en.wikipedia.org/wiki/Open_border)。

## 绿色与安静
用于识别绿色路线和安静路线的数据来自海德堡大学 GIScience 研究组的研究项目。有关这些内容的更多信息，可以在 GIScience 新闻博客的
[这里](http://k1z.blog.uni-heidelberg.de/2017/07/03/healthy-routing-prefering-green-areas-added-to-openrouteserviceorg/)
和
[这里](http://k1z.blog.uni-heidelberg.de/2017/07/10/reducing-stress-by-avoiding-noise-with-quiet-routing-in-openrouteservice/)
查阅。
