# openrouteservice 中要配置什么

配置属性以层次化结构组织，
以下是位于顶层的属性。
由于 openrouteservice 基于 spring，
所有
常见的 [spring 属性](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
都可以设置。
最重要的属性有：

* [服务器属性](server.zh.md)
* [日志属性](logging.zh.md)

其他 openrouteservice 特有的属性组织在 `ors` 对象中：

* [ors.endpoints](endpoints/index.zh.md)：处理 API 请求时在运行时所需的设置。
* [ors.engine](engine/index.zh.md)：启动时构建图阶段所需的设置，以及有关实例上运行的[配置](engine/profiles/index.zh.md)的设置。
* [ors.cors](cors/index.zh.md)：跨源资源共享（CORS）设置。
* [ors.messages](messages/index.zh.md)：可按简单规则随 API 响应一起发送的系统消息。

::: tip
在本文档中，我们使用点号表示法来书写属性。
请注意，相同的属性可以用[不同的表示法](how-to-configure.zh.md#different-notations)定义。
:::

## 最小配置

至少，openrouteservice 的配置需要包含至少一个
已启用的[配置](engine/profiles/index.zh.md)，
以及对 [OSM 数据文件](/run-instance/data.md#osm-data)的引用，才能正常运行。因此，最小有效
配置
例如可以是：

```yaml
ors:
    engine:
        profile_default:
            build:
                source_file: ./osm_file.pbf
        profiles:
            driving-car:
                enabled: true
```

用 properties 表示的相同配置如下所示：

```properties
ors.engine.profile_default.build.source_file=./osm_file.pbf
ors.engine.profiles.driving-car.enabled=true
```

以环境变量表示的配置：

```bash
ORS_ENGINE_PROFILE_DEFAULT_BUILD_SOURCE_FILE=./osm_file.pbf
ORS_ENGINE_PROFILES_DRIVING_CAR_ENABLED=true
```

有关如何使用 `properties` 和环境变量的更深入理解，
请参见[如何配置](how-to-configure.zh.md)
