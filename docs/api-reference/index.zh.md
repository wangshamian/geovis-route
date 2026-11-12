# API 参考

获取 openrouteservice API 信息有多种途径。

作为起步，最好先试用我们的 [API Playground](#api-playground)。

接下来的 [Endpoints](endpoints/index.zh.md) 章节包含了一些在 API Playground 中可能说明得不够充分的特定主题的讲解与示例。
这里并未完整覆盖单个端点的全部请求参数等内容。
但另一方面，这里也包含了那些在我们线上 API 中不可用、而仅在你自行运行或托管的实例上可用的端点的信息，
例如 [Export](endpoints/export/index.zh.md)、[Health](endpoints/health/index.zh.md) 和 [Status](endpoints/status/index.zh.md)。
本实例还额外提供基于地形坡度的 [越野路径](endpoints/offroad/index.zh.md) 端点（`POST /v2/offroad`）。

如果你正在开发 openrouteservice 或运行自己的实例，随附的 [Swagger-UI](#swagger-ui) 可能会对你有所帮助。

## API Playground

[API Playground](https://openrouteservice.org/dev/#/api-docs/directions_service) 是对我们线上 API 完整、交互式且简洁的技术文档。
它也可以用于向我们线上的 API 或你本机运行的 openrouteservice 实例发送示例请求。
如果你想向本地实例发送请求，请使用左上角的 “API Server” 表单选择并编辑 “Development Server”：
![开发服务器用法](/playground-select-server.png "开发服务器用法"){ style="display: block; margin: 0 auto"}

示例请求的响应会在右下角的小地图上可视化展示，
也可以以 json、表格形式显示，或作为文件下载：
![示例请求可视化](/playground-map.png "示例请求可视化"){ style="display: block; margin: 0 auto"}

::: warning 提示
如果你更偏好 swagger-ui，也可以使用 [swagger editor](https://editor-next.swagger.io/) 并从
https://openrouteservice.org/wp-json/ors-api/v1/api-doc/source/V2 加载我们 API 的完整 OpenAPI 规范文件。
不过，其中展示的部分端点或功能在我们的线上 API 中无法访问。
:::

## Swagger-UI

openrouteservice 的本地实例同样提供可访问的 swagger-ui。
通过它可以获取当前版本或较旧版本 openrouteservice 的交互式 API 文档：

1. 检出所需版本或标签的源代码，并在[本地运行服务](/run-instance/index.md)。
2. 然后你可以在浏览器中访问本地实例托管的 swagger-ui：[http://localhost:8082/ors/swagger-ui/index.html](http://localhost:8082/ors/swagger-ui/index.html)
（你本地环境中的端口可能不同）。

:::warning 提示
由 HeiGIT 托管的生产环境 openrouteservice 实例上不提供 swagger-ui。
:::

![Swagger-UI](/swagger-ui.png "Swagger UI")
