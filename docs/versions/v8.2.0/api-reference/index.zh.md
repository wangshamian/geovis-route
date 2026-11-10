# API 参考

了解 openrouteservice API 的方式有多种。

作为起点，最好先试用我们的 [API Playground](#api-playground)。

接下来的[端点](endpoints/index.zh.md)一章包含对一些精选主题的说明和示例，这些主题
在 API Playground 中可能没有解释得足够充分。
这里并不会完整覆盖单个端点的所有请求参数等内容，
但另一方面，这里也包含一些我们的在线 API 中不提供、
而在你自己运行或托管的实例中可用的端点信息，例如 [Export](endpoints/export/index.zh.md)、[Health](endpoints/health/index.zh.md) 和 [Status](endpoints/status/index.zh.md)。

如果你正在开发 ORS 或运行自己的实例，随附的 [Swagger-UI](#swagger-ui) 可能会对你有帮助。

## API Playground

[API Playground](https://openrouteservice.org/dev/#/api-docs/directions_service) 是关于我们在线 API 的完整、交互式且简洁的技术文档。
它也可以用来向我们的在线 API 或你本机运行的 openrouteservice 实例发送示例请求。
如果你想向本地实例发送请求，请使用左上角的 "API Server" 表单来选择并编辑 "Development Server"：
![开发服务器用法](/playground-select-server.png "开发服务器用法"){ style="display: block; margin: 0 auto"}

示例请求的响应会显示在右下角的小地图上，
也可以以 json、表格形式展示，或作为文件下载：
![示例请求可视化](/playground-map.png "示例请求可视化"){ style="display: block; margin: 0 auto"}

::: warning 提示
如果你更喜欢 swagger-ui，也可以使用 [swagger 编辑器](https://editor-next.swagger.io/)并从 https://openrouteservice.org/wp-json/ors-api/v1/api-doc/source/V2 加载我们 API 的完整 OpenAPI 规范文件。
不过，其中部分展示的端点或功能无法通过我们的在线 API 访问。
:::

## Swagger-UI

openrouteservice 的本地实例同样提供一个可访问的 swagger-ui。
通过它可以获取当前版本或旧版本 openrouteservice 的交互式 API 文档：
1. 检出所需版本或 tag 的源代码，并[在本地运行该服务](../run-instance/index.zh.md)。
2. 然后可以在浏览器中访问由本地实例托管的 swagger-ui：[http://localhost:8082/ors/swagger-ui/index.html](http://localhost:8082/ors/swagger-ui/index.html)
（你的本地环境中端口可能不同）。

:::warning 提示
由 HeiGIT 托管的生产环境 ORS 实例不提供 swagger-ui。
:::

![Swagger-UI](/swagger-ui.png "Swagger UI")
