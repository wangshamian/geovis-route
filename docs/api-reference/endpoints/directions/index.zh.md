# Directions 服务

Directions 服务可为两种或多种交通方式计算两地或多地之间的路线与导航指示。

所有请求参数与返回类型的完整、简洁概览可在 [API Playground](https://openrouteservice.org/dev/#/api-docs/directions_service) 中找到。
不过，以下几个主题会在这里作更详细的讲解：

* 不同的 directions（即路径规划）[请求与返回类型](requests-and-return-types.zh.md)
* 如何定义高级的[路径规划选项](routing-options.zh.md)
* 如何使用[自定义模型](custom-models.zh.md)在路线计算过程中影响边的权重
* 如何请求[额外信息](extra-info/index.zh.md)，例如路面、道路类型、OpenStreetMap way ID，或额外的[路线属性](route-attributes.zh.md)
* 如何对 directions 响应中的几何进行[解码](geometry-decoding.zh.md)
* [指示类型](instruction-types.zh.md)在 directions 响应中是如何编码的
