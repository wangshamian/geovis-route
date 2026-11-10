# 运行你自己的 openrouteservice 实例

如果你需要自定义 openrouteservice 的行为，或者我们的公共 API 提供的功能或配额不能满足你的需求，你可以在服务器或本地计算机上运行自己的 openrouteservice 实例。在自己的实例中，你可以激活所有端点，包括那些在我们的公共 API 中不可用的端点。

有多种方式可以实现这一点。该服务可以构建为不同形式的产物：**WAR**、**JAR** 或 **Docker 镜像**。在下表的第一列中，你可以找到每种产物基本信息的链接。

无论你打算以何种方式运行 openrouteservice，首先都需要相应的产物。第二列和第三列提供了指向详细信息的链接，说明如何自行构建定制版本，或者从哪里下载该产物。

对于配置、运行和故障排查而言，无论你运行的是下载的版本还是定制版本，都没有区别（只要你不更改配置等基础内容）。你可以在右侧三列中找到有关这些主题的信息链接。

但在你按照自己选择的技术方式开始运行自己的 openrouteservice 之前，请先阅读有关[系统要求](system-requirements)和[数据](data)的文档！

| 产物                                   | 下载                                                                    | 或自行构建                                               | 运行                                                                | 配置                                                       | 故障排查                                                         |
|----------------------------------------|-------------------------------------------------------------------------|----------------------------------------------------------|---------------------------------------------------------------------|------------------------------------------------------------|------------------------------------------------------------------|
| [JAR](running-jar.zh.md)                  | [下载 JAR](running-jar.zh.md#download)                                     | [构建 JAR](building-from-source.zh.md#build-jar)            | [运行 JAR](running-jar.zh.md#run)                                      | [配置 JAR](running-jar.zh.md#configure)                       | [JAR 故障排查](running-jar.zh.md#troubleshoot)                      |
| [WAR](running-war.zh.md)                  | [下载 WAR](running-war.zh.md#download)                                     | [构建 WAR](building-from-source.zh.md#build-war)            | [运行 WAR](running-war.zh.md#run)                                      | [配置 WAR](running-war.zh.md#configure)                       | [WAR 故障排查](running-war.zh.md#troubleshoot)                      |
| [Docker 镜像](running-with-docker.zh.md)  | [下载 Docker 镜像](running-with-docker.zh.md#running-prebuilt-images)      | [构建 Docker 镜像](running-with-docker.zh.md#build-image)   | [运行 Docker 镜像](running-with-docker.zh.md#running-prebuilt-images)  | [配置 Docker 镜像](running-with-docker.zh.md#configure)       | [Docker 镜像故障排查](running-with-docker.zh.md#troubleshoot)       |
| [源代码](building-from-source.zh.md)      | [下载源代码](building-from-source.zh.md#download-source-code)              |                                                          | [运行源代码](building-from-source.zh.md#run-source-code-directly)      | [配置](building-from-source.zh.md#configure)                  |                                                                  |

[//]: # (TODO add row for rpm package once integrated)

::: tip
为了简单起见，我们推荐使用 Docker 方式；如果你不想安装 Docker，也可以直接使用 JAR。
:::

## 检查

默认情况下，可以通过[健康检查端点](/api-reference/endpoints/health/index.md)查询服务状态。

```shell 
curl 'http://localhost:8080/ors/v2/health'
# should result in an output like 
# {"status":"ready"}
```

当服务就绪后，你可以请求[状态端点](/api-reference/endpoints/status/index.md)，获取正在运行的服务的进一步信息。

```shell 
curl 'http://localhost:8080/ors/v2/status'
# should result in an output like 
# {"languages":["cs","cs-cz","de","de-de","en","en-us","eo","eo-eo","es","es-es","fr","fr-fr","gr","gr-gr","he","he-il","hu","hu-hu","id","id-id","it","it-it","ja","ja-jp","ne","ne-np","nl","nl-nl","pl","pl-pl","pt","pt-pt","ro","ro-ro","ru","ru-ru","tr","tr-tr","zh","zh-cn"],"engine":{"build_date":"2024-01-02T16:34:45Z","version":"8.0"},"profiles":{"profile 1":{"storages":{"WayCategory":{"gh_profile":"car_ors_fastest_with_turn_costs"},"HeavyVehicle":{"gh_profile":"car_ors_fastest_with_turn_costs"},"WaySurfaceType":{"gh_profile":"car_ors_fastest_with_turn_costs"},"RoadAccessRestrictions":{"gh_profile":"car_ors_fastest_with_turn_costs","use_for_warnings":"true"}},"profiles":"driving-car","creation_date":"","limits":{"maximum_distance":100000,"maximum_waypoints":50,"maximum_distance_dynamic_weights":100000,"maximum_distance_avoid_areas":100000}}},"services":["routing","isochrones","matrix","snap"]}
```

如果你使用默认数据集，就可以向海德堡（Heidelberg）的汽车配置发送如下所示的路由请求。

```shell 
curl 'http://localhost:8080/ors/v2/directions/driving-car?start=8.681495,49.41461&end=8.687872,49.420318'
```


## 实例基础设施

虽然对于较小的数据集，或者在图数据无需更新的场景下，运行单个 openrouteservice 实例就能很好地工作，但在许多实际部署中，仅有一个实例并不是最合适的方案。如果你只有一个实例，那么所有路线的构建和服务都通过这一个实例完成，这意味着在重建图时，你无法向该实例发起路径规划等请求，因为此时没有可用于生成路线的完整图。如果在保证用户无法请求的时间尽可能短的同时，图能够基于新数据更新对你来说很重要，我们建议使用两个实例——一个永久处于活动状态用于处理请求，另一个则启动起来用于重建图。

在这种部署方式下，当图构建完成后，你可以简单地停止用于处理请求的容器，替换所使用的图（它们映射到主机上的某个文件夹，该文件夹在 `docker-compose` 文件中定义），然后重启“请求”实例。新的图将被重新加载到内存中（所需时间取决于图的大小和硬盘类型），之后即可用于路径规划。重新加载已构建好的图所造成的停机时间通常远小于构建图所需的时间。但需要注意，你应确保构建服务器和请求服务器上的配置文件以及分配的 RAM 大小（如前所述）保持一致，否则新构建的图可能无法加载。**另外，请确保请求服务容器使用的 `REBUILD_GRAPHS` 参数设置为 false，否则它会尝试自行构建图！**
