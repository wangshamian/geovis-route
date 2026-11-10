# Elevation 服务

:::warning 注意
该端点不属于 openrouteservice，而属于我们的公共 API。运行自己的 openrouteservice 实例时不可用。
:::

elevation 服务是一个 Flask 应用，可从多种高程数据集中为 Point 或 LineString 的二维几何要素提取高程，
并以多种格式返回三维几何要素。
与 openrouteservice 一样，elevation 服务也可以通过我们的公共 API 访问。

关于如何使用它的详细信息可以在我们的 [API Playground](https://openrouteservice.org/dev/#/api-docs/elevation) 中找到。

如果你需要更深入的了解，或者想运行自己的实例，请访问我们的 GitHub 项目 [openelevationservice](https://github.com/GIScience/openelevationservice)。
