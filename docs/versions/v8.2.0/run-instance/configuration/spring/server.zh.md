# `server` 

spring 使用的通用服务器属性

| key                         | type   | description                     | default value |
|-----------------------------|--------|---------------------------------|---------------|
| server.port                 | int    | 服务器 HTTP 端口                | `8082`        |
| server.servlet.context-path | string | 应用程序的上下文路径            | `/ors`        |

::: info
其他 [spring 服务器属性](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#appendix.application-properties.server)
也许可以直接使用，但未经测试。
:::
