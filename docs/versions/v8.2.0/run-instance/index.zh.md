# 运行你自己的 openrouteservice 实例

如果你需要自定义 openrouteservice 的行为，或者我们的公共 API 所提供的功能或配额不能满足你的需求，你可以在服务器或个人电脑上运行自己的 openrouteservice 实例。在自有实例中，你可以启用所有端点，包括那些在我们的公共 API 中不可用的端点。

有多种方式可以实现这一点。该服务可以构建为不同的产物类型：**WAR**、**JAR** 或 **Docker 镜像**。在下表的第一列中，你可以找到指向每种产物类型基本信息的链接。

无论你想以何种方式运行 openrouteservice，首先都需要对应的产物。第二列和第三列链接到关于如何自行构建定制版本或从何处下载产物的详细信息。

对于配置、运行和故障排除而言，你运行的是下载版本还是定制版本并无影响（只要你不修改配置等基础内容）。在右侧三列中，你可以找到关于这些主题的信息链接。

但在你开始以自己选择的技术方式运行自己的 openrouteservice 之前，请先阅读有关[系统要求](system-requirements)和[数据](data)的文档！

| 产物                                   | 下载                                                                    | 或自行构建                                               | 运行                                                                | 配置                                                       | 故障排除                                                         |
|----------------------------------------|-------------------------------------------------------------------------|----------------------------------------------------------|---------------------------------------------------------------------|------------------------------------------------------------|------------------------------------------------------------------|
| [JAR](running-jar.md)                  | [下载 JAR](running-jar.md#download)                                     | [构建 JAR](building-from-source.md#build-jar)            | [运行 JAR](running-jar.md#run)                                      | [配置 JAR](running-jar.md#configure)                       | [JAR 故障排除](running-jar.md#troubleshoot)                      |
| [WAR](running-war.md)                  | [下载 WAR](running-war.md#download)                                     | [构建 WAR](building-from-source.md#build-war)            | [运行 WAR](running-war.md#run)                                      | [配置 WAR](running-war.md#configure)                       | [WAR 故障排除](running-war.md#troubleshoot)                      |
| [Docker 镜像](running-with-docker.md)  | [下载 Docker 镜像](running-with-docker.md#running-prebuilt-images)       | [构建 Docker 镜像](running-with-docker.md#build-image)   | [运行 Docker 镜像](running-with-docker.md#running-prebuilt-images)   | [配置 Docker 镜像](running-with-docker.md#configure)        | [Docker 镜像故障排除](running-with-docker.md#troubleshoot)       |
| [源代码](building-from-source.md)      | [下载源代码](building-from-source.md#download-source-code)               |                                                          | [运行源代码](building-from-source.md#run-source-code-directly)       | [配置](building-from-source.md#configure)                  |                                                                  |

[//]: # (TODO add row for rpm package once integrated)

::: tip
为了简单起见，我们建议使用 docker 方式；如果你不想安装 Docker，则使用纯 JAR。
:::

## 检查

默认情况下，可以通过 [health 端点](../api-reference/endpoints/health/index.md)查询服务状态。

```shell 
curl 'http://localhost:8080/ors/v2/health'
# should result in an output like 
# {"status":"ready"}
```

当服务就绪后，你可以请求 [status 端点](../api-reference/endpoints/status/index.md)以获取正在运行的服务的信息。

```shell 
curl 'http://localhost:8080/ors/v2/status'
# should result in an output like 
# {"languages":["cs","cs-cz","de","de-de","en","en-us","eo","eo-eo","es","es-es","fr","fr-fr","gr","gr-gr","he","he-il","hu","hu-hu","id","id-id","it","it-it","ja","ja-jp","ne","ne-np","nl","nl-nl","pl","pl-pl","pt","pt-pt","ro","ro-ro","ru","ru-ru","tr","tr-tr","zh","zh-cn"],"engine":{"build_date":"2024-01-02T16:34:45Z","version":"8.0"},"profiles":{"profile 1":{"storages":{"WayCategory":{"gh_profile":"car_ors_fastest_with_turn_costs"},"HeavyVehicle":{"gh_profile":"car_ors_fastest_with_turn_costs"},"WaySurfaceType":{"gh_profile":"car_ors_fastest_with_turn_costs"},"RoadAccessRestrictions":{"gh_profile":"car_ors_fastest_with_turn_costs","use_for_warnings":"true"}},"profiles":"driving-car","creation_date":"","limits":{"maximum_distance":100000,"maximum_waypoints":50,"maximum_distance_dynamic_weights":100000,"maximum_distance_avoid_areas":100000}}},"services":["routing","isochrones","matrix","snap"]}
```

如果你使用默认数据集，就可以向海德堡的 car 配置发起类似下面这样的路径请求。

```shell 
curl 'http://localhost:8080/ors/v2/directions/driving-car?start=8.681495,49.41461&end=8.687872,49.420318'
```


## 实例基础设施

虽然单个 openrouteservice 实例对于较小的数据集或图数据无需更新的场景运行良好，但在许多真实应用中，仅有一个实例并不是最合适的方案。如果你只有一个实例，那么所有路径的构建和提供都通过该单一实例完成，这意味着在重建图时，你无法向该实例发出例如路径规划这类请求，因为没有可用于生成路径的完整图。如果既要确保能够从新数据更新图，又要确保用户无法发出请求的时间尽可能短，我们建议使用两个实例——一个永久运行用于处理请求，另一个用于重建图。

在这种设置下，图构建完成后，你可以简单地停止处理请求的容器，替换所使用的图（它们映射到主机上的某个文件夹，该文件夹在 `docker-compose` 文件中定义），然后重启“请求”实例。新的图将被重新加载到内存中（所需时间取决于图的大小和硬盘类型），随后即可用于路由。重新加载已构建好的图所造成的停机时间通常远小于构建图所需的时间。不过需要注意一点，你应确保构建服务器和请求服务器上的配置文件以及分配的 RAM 数量（如前所述）相同，否则新构建的图可能无法加载。**另外，请确保请求服务容器使用的 `REBUILD_GRAPHS` 参数被设置为 false，否则它会尝试自行构建图！**
