# 系统要求

运行 openrouteservice 时，最需要考虑的因素是可用 RAM 的大小。像德国巴登-符腾堡州这样较小的区域可以在中端系统上顺利运行，但更大的区域需要更多 RAM。例如，在 openrouteservice 公共 API 基础设施中，全球范围的每个配置都运行在 128GB 的机器上。低于这个数值，你在构建过程中就会耗尽内存。

影响所需 RAM 大小的主要因素有：
* 加载的数据（OSM 数据）的大小
* 构建的配置（自行车和步行配置比驾车配置需要更多空间）
* 路由优化（启用优化的路由算法需要更多 RAM）
* 高程（使用高程需要将高程数据也加载到 RAM 中，因此地理区域越大，高程所需的 RAM 就越多）
* 额外信息（每个配置都可以计算额外信息，每一项都需要 RAM 用于存储）。

作为参考，你可以查看 OSM 数据摘录的大小，粗略判断需要多少 RAM。[Geofabrik](https://download.geofabrik.de) 提供了许多此类摘录以及 pbf 文件的大小。全球文件（planet）约为 50GB，对于自行车和步行配置，大约需要 100-105GB RAM。德国约为 3GB，通常可以在约 8GB RAM 的普通系统上构建；巴登-符腾堡州为 450MB，可以在约 2-4GB RAM 的机器上构建。不过一般来说，拥有更多 RAM 也会加快构建过程，因为需要执行的垃圾回收操作更少；同时你还应留意机器上是否有其他与 openrouteservice 并行运行并消耗 RAM 的服务。

[//]: # (TODO: @koebi was working on a set of example memory footprint values or something similar? Add here)

## JVM 配置

与 openrouteservice 本身的配置无关，你可能需要调整运行代码的 Java 虚拟机（JVM）的设置。由于 openrouteservice 的内存需求与 OSM 数据（以及由此产生的图）的大小成正比，最常需要调整的设置是堆内存大小。

[//]: # (TODO: elaborate)

```
JAVA_OPTS="-Xms105g -Xmx105g "
```

## 容器化 openrouteservice 实例进行大型构建时的内存映射
如果你正在进行大型构建（例如全球文件），可能需要增加内存映射的数量。你只需在主机上执行此操作，因为该值也会被运行在其上的 Docker 容器使用。为此，请使用 `sudo nano /etc/sysctl.conf` 打开系统配置文件，并在文件末尾添加以下行：

```shell
vm.max_map_count=81920
```

需要做此更改的常见迹象是，你在日志中看到类似以下内容：

```shell
# There is insufficient memory for the Java Runtime Environment to continue.
# Native memory allocation (mmap) failed to map 16384 bytes for committing reserved memory.
# An error report file with more information is saved as:
# /ors-core/hs_err_pid128.log
```
