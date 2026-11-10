# `logging`

默认情况下，openrouteservice 将状态和错误消息同时记录到控制台和日志文件。请求不会保留访问日志。

可以使用 `logging.` 命名空间下的 spring 属性来配置日志记录，完整列表请参见 [spring 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#appendix.application-properties.core)。

最重要的属性有：

| key                      | type   | description                                         | default value                                                                                              |
|--------------------------|--------|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| logging.file.name        | string | 日志文件的路径/文件名                       | `./logs/ors.log`                                                                                           |
| logging.pattern.console  | string | 输出到控制台的日志消息格式                 | `%d{YYYY-MM-dd HH:mm:ss} %highlight{%-7p} %style{%50t}{Cyan} %style{[ %-40.40c{1.} ]}{Bright Cyan}   %m%n` |
| logging.pattern.file     | string | 写入日志文件的日志消息格式                | `%d{YYYY-MM-dd HH:mm:ss} %-7p [ %-40.40c{1.} ]   %m%n`                                                     |
| logging.level.root       | string | 默认日志级别                                   | `WARN`                                                                                                     |
| logging.level.org.heigit | string | org.heigit 命名空间消息的日志级别 | `INFO`                                                                                                     |
