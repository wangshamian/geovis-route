# 端点

:::warning 提示
并非所有端点都在我们的在线 API 中可用。
但你可以在自己托管的 openrouteservice 实例中使用它们。
:::

## 空间端点

Openrouteservice 提供了一组用于不同空间用途的端点：

* [Directions Service](directions/index.zh.md)：获取不同交通方式的路线指引
* [Isochrones Service](isochrones/index.zh.md)：获取从给定位置出发的可达区域
* [Matrix Service](matrix/index.zh.md)：获取时间与距离的一对多、多对一和多对多矩阵
* [Snapping Service](snapping/index.zh.md)：将坐标吸附到道路网络

## 技术端点

此外还有一些技术端点，它们在我们的在线 API 中 :warning: _不可用_：

* [Export Service](export/index.zh.md)：导出不同交通方式的基础图
* [Health Service](health/index.zh.md)：获取正在运行的 openrouteservice 实例的健康状况信息
* [Status Service](status/index.zh.md)：获取 openrouteservice 实例的状态信息

## 内置服务 

为了便利，我们还托管了一些其他服务，它们 :warning: _仅_ 包含在我们的公共 API 中。
这些是独立服务，:warning: _不包含_ 在本地 openrouteservice 实例中，需要
单独进行本地安装：

* [POI Service](poi/index.zh.md)：由 HeiGIT 提供的独立服务，返回几何要素周边区域内的兴趣点
* [Elevation Service](elevation/index.zh.md)：由 HeiGIT 提供的独立服务，返回点或线几何要素的高程 
* [Geocoding Service](geocoder/index.zh.md)：由 HeiGIT 托管的第三方服务（[Pelias](https://www.pelias.io)），用于在地理坐标与地址之间进行解析转换
* [Optimization Service](optimization/index.zh.md)：由 HeiGIT 托管的第三方服务（[VROOM](https://github.com/VROOM-Project/vroom)），用于为车队优化路线
