# 以 JAR 方式运行 openrouteservice

从版本 8 开始，openrouteservice 可以构建为 fat JAR 文件，其中包含所有依赖，可作为独立应用程序运行。

## 前提条件

* 应安装 [java](https://www.java.com/en/) 17（或更高版本），最好作为默认的 Java 环境。

要运行 openrouteservice，你还需要一个 OSM 数据文件，例如来自 [Geofabrik](http://download.geofabrik.de) 的文件。更多详细信息，请参见[数据](data.zh.md)章节。

## 下载

从版本 8 开始，你可以从我们的 GitHub [releases](https://github.com/GIScience/openrouteservice/releases) 页面中所需发布版本的“Assets”部分下载可直接使用的 JAR 文件。

## 构建

如何构建与你想要使用的产物类型无关，相关内容记录在[从源代码构建](building-from-source.zh.md)中。

## 运行

要运行 openrouteservice 应用程序，请使用以下命令：

```shell 
java -jar ors.jar
```

## 配置

以普通方式使用 JAR 文件运行的 openrouteservice 实例，推荐的配置方式是使用 YAML 配置文件。你可以使用以下命令下载示例文件：

```shell 
wget https://raw.githubusercontent.com/GIScience/openrouteservice/main/gr-config.yml
```

有关如何让 openrouteservice 应用配置文件中的设置（有多种方式）的详细信息，请参见[配置](configuration/index.zh.md)章节。

## 故障排查

日志文件的写入位置由配置属性 `logging.file.name` 定义。
在[日志文档](configuration/logging.zh.md)中，你可以找到更多日志选项，例如设置日志级别等。

openrouteservice 的启动日志大致如下：
```shell
ors-app  | ▢ Startup command: java -Djava.awt.headless=true -server -XX:TargetSurvivorRatio=75 -XX:SurvivorRatio=64 -XX:MaxTenuringThreshold=3 -XX:+UseG1GC -XX:+ScavengeBeforeFullGC -XX:ParallelGCThreads=4 -Xms1g -Xmx2g  -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9001 -Dcom.sun.management.jmxremote.rmi.port=9001 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=localhost  -jar /ors.jar
ors-app  | 
ors-app  |   .   ____          _            __ _ _
ors-app  |  /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
ors-app  | ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
ors-app  |  \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
ors-app  |   '  |____| .__|_| |_|_| |_\__, | / / / /
ors-app  |  =========|_|==============|___/=/_/_/_/
ors-app  |  :: Spring Boot ::                (v3.1.6)
ors-app  | 
ors-app  | 2024-03-12 10:54:45 INFO                                                  main [ o.h.o.a.org.heigit.ors.api.Application                      ]   Starting org.heigit.ors.api.Application v8.0-SNAPSHOT using Java 21.0.2 with PID 1 (/ors.jar started by root in /home/ors)
ors-app  | 2024-03-12 10:54:45 INFO                                                  main [ o.h.o.a.org.heigit.ors.api.Application                      ]   The following 1 profile is active: "default"
ors-app  | 2024-03-12 10:54:45 INFO                                                  main [ o.h.o.a.ORSEnvironmentPostProcessor      ]   
ors-app  | 2024-03-12 10:54:45 INFO                                                  main [ o.h.o.a.ORSEnvironmentPostProcessor      ]   Configuration lookup started.
ors-app  | 2024-03-12 10:54:45 INFO                                                  main [ o.h.o.a.ORSEnvironmentPostProcessor      ]   Configuration file set by environment variable.
ors-app  | 2024-03-12 10:54:45 INFO                                                  main [ o.h.o.a.ORSEnvironmentPostProcessor      ]   Loaded file '/home/ors/config/gr-config.yml'
ors-app  | 2024-03-12 10:54:45 INFO                                                  main [ o.h.o.a.ORSEnvironmentPostProcessor      ]   Configuration lookup finished.
ors-app  | 2024-03-12 10:54:45 INFO                                                  main [ o.h.o.a.ORSEnvironmentPostProcessor      ]   
ors-app  | 2024-03-12 10:54:46 INFO                                              ORS-Init [ o.h.o.a.s.l.ORSInitContextListener       ]   Initializing ORS...
ors-app  | 2024-03-12 10:54:46 INFO                                              ORS-Init [ o.h.o.r.RoutingProfileManager            ]   Total - 1024 MB, Free - 965.93 MB, Max: 2 GB, Used - 58.07 MB
ors-app  | 2024-03-12 10:54:46 INFO                                              ORS-Init [ o.h.o.r.RoutingProfileManager            ]   ====> Initializing profiles from '/home/ors/files/example-heidelberg.test.pbf' (1 threads) ...
ors-app  | 2024-03-12 10:54:46 INFO                                              ORS-Init [ o.h.o.r.RoutingProfileManager            ]   2 profile configurations submitted as tasks.
ors-app  | 2024-03-12 10:54:46 INFO                                     ORS-pl-wheelchair [ o.h.o.r.RoutingProfile                   ]   [1] Profiles: 'wheelchair', location: './graphs/wheelchair'.
ors-app  | 2024-03-12 10:54:47 INFO                                     ORS-pl-wheelchair [ o.h.o.r.g.e.ORSGraphHopper               ]   version v4.9.1|2024-01-17T09:08:46Z (7,20,5,4,5,7)
ors-app  | 2024-03-12 10:54:47 INFO                                     ORS-pl-wheelchair [ o.h.o.r.g.e.ORSGraphHopper               ]   graph wheelchair|RAM_STORE|3D|turn_cost|,,,,, details:edges:0(0MB), nodes:0(0MB), name:(0MB), geo:0(0MB), bounds:1.7976931348623157E308,-1.7976931348623157E308,1.7976931348623157E308,-1.7976931348623157E308,1.7976931348623157E308,-1.7976931348623157E308
ors-app  | 2024-03-12 10:54:47 INFO                                     ORS-pl-wheelchair [ o.h.o.r.g.e.ORSGraphHopper               ]   No custom areas are used, custom_areas.directory not given
ors-app  | 2024-03-12 10:54:47 INFO                                     ORS-pl-wheelchair [ o.h.o.r.g.e.ORSGraphHopper               ]   start creating graph from /home/ors/files/example-heidelberg.test.pbf
ors-app  | 2024-03-12 10:54:47 INFO                                     ORS-pl-wheelchair [ o.h.o.r.g.e.ORSGraphHopper               ]   using wheelchair|RAM_STORE|3D|turn_cost|,,,,, memory:totalMB:1024, usedMB:260
ors-app  | 2024-03-12 10:54:47 INFO                                                  main [ o.h.o.a.org.heigit.ors.api.Application                      ]   Started org.heigit.ors.api.Application in 2.442 seconds (process running for 3.066)
ors-app  | 2024-03-12 10:54:47 INFO                                                  main [ o.h.o.a.org.heigit.ors.api.Application                      ]   openrouteservice {"build_date":"2024-03-08T15:01:47Z","version":"8.0"}
```

这里最重要的信息：

* openrouteservice 和 java 的版本：`Starting org.heigit.ors.api.Application v8.0-SNAPSHOT using Java 21.0.2`
* 实际使用的配置文件：`Loaded file '/home/ors/config/gr-config.yml'`
* 内存使用情况：`Total - 1024 MB, Free - 965.93 MB, Max: 2 GB, Used - 58.07 MB`
* 实际使用的 OSM 文件：`====> Initializing profiles from '/home/ors/files/example-heidelberg.test.pbf'`
* 你的设置中可能存在的错误

在启动部分之后，你可以找到有关运行时错误的信息。

::: tip 提示
输出 `The following 1 profile is active: "default"` 来自 spring，
它指的是激活的 **spring profile**，与路由配置无关。
:::
