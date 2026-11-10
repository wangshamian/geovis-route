# 配置

## 文件位置

有两种（可选）方式向 openrouteservice 提供配置文件的位置：
1. 程序参数
      ```shell 
      java -jar ors.jar /path/to/gr-config.yml
      ```
2. 环境变量 `GR_CONFIG_LOCATION`
      ```shell 
      export GR_CONFIG_LOCATION=/path/to/gr-config.yml
      java -jar ors.jar
      ```
如果两者都指定了，则以程序参数为准。

如果未指定配置位置，openrouteservice 将按以下顺序在下列位置查找配置文件 `gr-config.yml`。
第一个存在的文件将被用作配置。

| 路径                                        | 说明                                         |
|:--------------------------------------------|:--------------------------------------------|
| `./gr-config.yml`                          | 当前工作目录                                 |
| `~/.config/openrouteservice/gr-config.yml` | 用户配置目录                                 |
| `/etc/openrouteservice/gr-config.yml`      | 全局配置目录                                 |

::: tip
程序启动时，openrouteservice 会报告加载了哪个配置文件。
:::

## 文件格式

根据产物类型的不同，配置属性可以以不同格式指定。
在哪种场景下使用哪种格式，记录在各产物类型的配置文档中：
[JAR](../running-jar.md)、
[WAR](../running-war.md) 和
[Docker](../running-with-docker.md)。

* `.yml` 是自版本 8 起的默认配置格式。你可以找到一个包含所有可用配置选项的[示例配置文件](https://github.com/GIScience/openrouteservice/blob/main/gr-config.yml)。其中只有最小的一组属性是启用的，其他所有属性都被注释掉了。
* 用于 Docker 设置的 `.env` 文件。还有一个[示例 env 文件](https://github.com/GIScience/openrouteservice/blob/main/gr-config.env)可供下载和定制。
* `.json` 配置文件：过去 openrouteservice 是[通过 JSON 文件](json.md)配置的。这种配置方法已被**弃用**，最终将被移除。因此，我们强烈建议不要使用它。如果你有旧的 JSON 配置，请考虑[迁移到新配置](https://github.com/GIScience/gr-config-migration#usage)。

上述所有配置文件都可以包含相同的逻辑应用属性。

例如，属性 `ors.engine.profiles.car.enabled` 在

`*.yml` 中如下
```yaml
ors:
  engine:
    profiles: 
      car: 
        enabled: true
```

在 `*.env` 中如下
```shell
ors.engine.profiles.car.enabled=true
```

在 `*.properties` 中如下
```properties
ors.engine.profiles.car.enabled=true
```

在[替代配置](#alternative-configuration)中，你可以找到将该属性定义为环境变量或程序参数的语法。


## 文件内容


属性以层次结构组织，顶层为以下各项。

- [Spring 属性](spring/index.md)，例如
    * [服务器属性](spring/server.md)
    * [日志属性](spring/logging.md)
- openrouteservice 属性，包含以下子项：
    * [ors.endpoints](ors/endpoints/index.md)：处理 API 请求时在运行时所需的设置。
    * [ors.engine](ors/engine/index.md)：启动时构建图所需的设置。
    * [ors.cors](ors/cors/index.md)：跨源资源共享设置。
    * [ors.messages](ors/messages/index.md)：可按简单规则随 API 响应发送的系统消息。

openrouteservice 至少需要配置中包含一个已启用的[配置](ors/engine/profiles.md)以及
对 [OSM 数据文件](../data.md#osm-data)的引用才能正常运行。因此，此类文件的最小有效内容例如：

```yaml
ors:
  engine:
    source_file: ./osm_file.pbf
    profiles: 
      car: 
        enabled: true
```

## 替代配置

所有配置参数都可以被运行时参数或环境变量覆盖。程序启动时，openrouteservice 会报告每一个*可能*影响其行为的环境变量。你可以完全不用配置文件，通过环境变量设置所有必需的属性来运行 openrouteservice。下面列出的示例实现了与上述相同的示例最小配置。

按优先级排序的选项（优先级高的选项优先于优先级低的）如下：
- Spring 运行时参数
  ```shell 
  java -jar ors.jar --ors.engine.source_file=./osm_file.pbf --ors.engine.profiles.car.enabled=true
  ```
- Java VM 运行时参数
  ```shell 
  java -jar -Dors.engine.source_file=./osm_file.pbf -Dors.engine.profiles.car.enabled=true ors.jar 
  ```
- 环境变量
  ```shell 
  export ors.engine.source_file=./osm_file.pbf 
  export ors.engine.profiles.car.enabled=true
  java -jar ors.jar 
  ```
  
当你希望在容器中运行 openrouteservice（例如使用 [docker](../running-with-docker.md)）时，使用环境变量进行配置的选项尤其有用。

每个属性也对应于一个环境变量名，环境变量名使用*大写字母*，并用*下划线*替换*点*，例如
- `ORS_ENGINE_SOURCE_FILE` 对应 `ors.engine.source_file`
- `ORS_ENGINE_PROFILES_CAR_ENABLED` 对应 `ors.engine.profiles.car.enabled`

因此，以下命令与上面的最后一个示例等价：
```shell
  export ORS_ENGINE_SOURCE_FILE=./osm_file.pbf
  export ORS_ENGINE_PROFILES_CAR_ENABLED=true
  java -jar ors.jar
```
