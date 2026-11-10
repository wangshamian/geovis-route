# `ors.engine.graph_management`

::: warning
图管理功能目前处于 beta 测试阶段，尚未向更广泛的公众开放。
请访问[图仓库客户端](/technical-details/graph-repo-client/)以更深入地了解此
功能。

如果你有兴趣测试图管理功能，
请在我们的[用户论坛](https://ask.openrouteservice.org/c/ors/5)中联系我们，
或发送电子邮件至 [support@smartmobility.heigit.org](mailto:support@smartmobility.heigit.org)。
:::

`ors.engine.graph_management` 下的属性用于为整个 openrouteservice
实例定义图管理。
更多信息请参见[图仓库客户端](/technical-details/graph-repo-client/)。

| Property              | Meaning                                                                      | Default                 |
|-----------------------|------------------------------------------------------------------------------|-------------------------|
| `enabled`             | 全局启用或禁用所有配置的图管理                 | `false`                 |
| `download_schedule`   | 定义何时检查和下载新图的 cron 表达式 | 表示从不执行的模式 |
| `activation_schedule` | 定义何时激活已下载的新图的 cron 表达式    | 表示从不执行的模式 |
| `max_backups`         | 激活新图版本时保留多少个旧图版本作为备份       | `0`                     |

#### Cron 表达式

`*_schedule` 属性的值是包含 6 个位置的 cron 表达式。
各位置的含义如下（从左到右）：

* 秒
* 分
* 时
* 日（月内）
* 月
* 星期几

更多信息
请参见 [org.springframework.scheduling.annotation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/Scheduled.html#cron())
