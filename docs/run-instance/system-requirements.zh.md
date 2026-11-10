# 系统要求

运行 openrouteservice 时，需要考虑的最重要因素就是可用 RAM 的大小。像德国巴登-符腾堡州（Baden-Württemberg）这样较小的区域可以在中端系统上流畅运行，但对于更大的区域，则需要更多 RAM。例如，在 openrouteservice 公共 API 基础设施中，全球（planet）数据的每个配置都运行在 128GB 的机器上。低于这个数值，你在构建过程中就会耗尽内存。

影响所需 RAM 大小的主要因素有：
* 加载的数据量（OSM 数据）
* 要构建的配置（自行车和步行配置比驾车配置需要更多空间）
* 路由优化（使用优化的路由算法需要更多 RAM）
* 高程（使用高程需要将高程数据也加载到 RAM 中，因此地理区域越大，高程所需的 RAM 就越多）
* 额外信息（每个配置都可以计算额外信息，每一项都需要 RAM 来存储）。

作为参考，你可以查看 OSM 数据提取文件的大小，以此粗略判断需要多少 RAM。[Geofabrik](https://download.geofabrik.de) 提供了许多这样的提取文件及其 pbf 文件的大小。全球文件（planet file）约为 50GB，对于自行车和步行配置，这需要约 100-105GB 的 RAM。德国数据约为 3GB，通常可以在约 8GB RAM 的普通系统上构建；巴登-符腾堡州为 450MB，可以在约 2-4GB RAM 的机器上构建。不过一般来说，RAM 越多，构建过程也会越快，因为需要执行的垃圾回收操作更少；同时你还应留意同一台机器上并行运行、同样占用 RAM 的其他服务。

[//]: # (TODO: @koebi was working on a set of example memory footprint values or something similar? Add here)

## JVM 配置

除了 openrouteservice 自身的配置之外，你可能还需要调整运行代码的 Java 虚拟机（JVM）的设置。由于 openrouteservice 的内存需求与 OSM 数据大小（以及由此产生的图）成正比，最经常需要调整的设置就是堆内存大小。

[//]: # (TODO: elaborate)

```
JAVA_OPTS="-Xms105g -Xmx105g "
```

## 容器化 openrouteservice 实例进行大型构建时的内存映射

如果你进行大型构建（例如全球文件），可能需要增加内存映射的数量。你只需在主机上执行此操作，因为该值同样会被主机上运行的 Docker 容器使用。为此，请使用 `sudo nano /etc/sysctl.conf` 打开系统配置文件，并在文件末尾添加以下行：

```shell
vm.max_map_count=81920
```

需要做此更改的常见迹象是，你在日志中看到类似下面的内容：

```shell
# There is insufficient memory for the Java Runtime Environment to continue.
# Native memory allocation (mmap) failed to map 16384 bytes for committing reserved memory.
# An error report file with more information is saved as:
# /ors-core/hs_err_pid128.log
```
