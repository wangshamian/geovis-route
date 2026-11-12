# 端点

:::warning 提示
并非所有端点都在我们的线上 API 中可用。
但当你托管自己的 openrouteservice 实例时，就可以使用它们。
:::

## 空间端点

Openrouteservice 提供了一组用于不同空间用途的端点：

* [Directions 服务](directions/index.zh.md)：获取不同交通方式的行车路线与导航指示
* [Isochrones 服务](isochrones/index.zh.md)：获取从指定位置出发的可达区域
* [Matrix 服务](matrix/index.zh.md)：获取一对多、多对一以及多对多的时间和距离矩阵
* [Snapping 服务](snapping/index.zh.md)：将坐标吸附到路网上
* [越野路径服务](offroad/index.zh.md)：不依赖路网，依据地形坡度在任意起终点之间搜索可行路径（本实例扩展端点）

## 技术端点

此外还有一些技术端点，它们 :warning: _在我们的线上 API 中不可用_：

* [Export 服务](export/index.zh.md)：导出不同交通方式的基础图
* [Health 服务](health/index.zh.md)：获取正在运行的 openrouteservice 实例的健康状况信息
* [Status 服务](status/index.zh.md)：获取 openrouteservice 实例的状态信息

## 附带服务

我们还为方便使用而托管了一些其他服务，它们 :warning: _仅_包含在我们的公共 API 中。
这些是独立服务，:warning: _不包含_在本地 openrouteservice 实例中，需要单独在本地安装：

* [POI 服务](poi/index.zh.md)：来自 HeiGIT 的独立服务，返回某几何体周边区域内的兴趣点
* [Elevation 服务](elevation/index.zh.md)：来自 HeiGIT 的独立服务，返回点或线几何体的高程
* [Geocoding 服务](geocoder/index.zh.md)：由 HeiGIT 托管的三方服务（[Pelias](https://www.pelias.io)），可将地理坐标解析为地址，反之亦然
* [Optimization 服务](optimization/index.zh.md)：由 HeiGIT 托管的三方服务（[VROOM](https://github.com/VROOM-Project/vroom)），可为车队优化路线
