# 系统要求

运行 openrouteservice 时，需要重点考虑的首要因素是可用 RAM 的容量。较小区域（例如德国的巴登-符腾堡州）在中端系统上即可顺利运行，但对于更大的区域，你需要更多 RAM。例如，在 openrouteservice 的基础设施中，面向 planet 的每个 profile 都运行在 128GB 内存的机器上。低于这个容量，构建过程中就会内存耗尽。

影响所需 RAM 容量的主要因素有：
* 所加载数据的大小（OSM 数据）
* 正在构建的 profile（自行车和步行 profile 比驾驶 profile 需要更多空间）
* 路线规划优化（启用优化后的路线规划算法需要更多 RAM）
* 高程（使用高程需要把高程数据也加载到 RAM 中，因此地理区域越大，高程所需的 RAM 就越多）
* extra info（每个 profile 都可以计算 extra info，每一类都需要 RAM 用于存储）。

作为参考，你可以查看 OSM 数据提取文件的大小，粗略判断需要多少 RAM。[Geofabrik](https://download.geofabrik.de) 提供了许多这样的提取文件以及 pbf 文件的大小。planet 文件约为 50GB，对于自行车和步行 profile，这大约需要 100-105GB 的 RAM。德国约为 3GB，通常可在一台约 8GB RAM 的普通系统上构建；巴登-符腾堡州为 450MB，可在约 2-4GB RAM 的机器上构建。不过总的来说，RAM 越多构建过程也越快，因为需要执行的垃圾回收操作更少；同时你还应留意机器上运行在 openrouteservice 之外、占用 RAM 的其他服务。

## 大型构建中的内存映射
如果你正在执行大型构建（例如 planet 文件），可能需要增大内存映射的数量。你只需在宿主机上执行此操作，因为在其上运行的 Docker 容器也会使用该值。为此，可用 `sudo nano /etc/sysctl.conf` 打开系统配置文件，并在文件末尾添加以下行：

```sh
vm.max_map_count=81920
```

需要做此修改的常见信号是：日志中出现类似如下的内容：

```sh
# There is insufficient memory for the Java Runtime Environment to continue.
# Native memory allocation (mmap) failed to map 16384 bytes for committing reserved memory.
# An error report file with more information is saved as:
# /ors-core/hs_err_pid128.log
```
