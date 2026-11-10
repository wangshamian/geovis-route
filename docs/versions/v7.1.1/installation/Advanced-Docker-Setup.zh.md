# 高级 Docker 设置

通过 Docker 和 docker-compose 开箱即用地运行 openrouteservice，是快速了解如何使用 openrouteservice 的好方法。不过对大多数应用而言，你会想自定义一些内容，比如要构建哪些 profile，以及要用哪些数据做路线规划。在其初始 Docker 容器形态下，openrouteservice 使用一份较早的德国海德堡测试数据集作为基础数据，并且只构建 car profile。要做更多的事，你就得稍微动手改一些配置设置。这听起来可能有些吓人，但它的设计思路是：一旦你跨过了对配置和 Docker 的初始理解门槛，你很快就能使用自己的数据集并决定使用哪些 profile。

在本搭建过程中，我们假设你是在从官方 openrouteservice 仓库克隆后，通过 Docker Compose 运行 openrouteservice。当然，你也可以只用 Docker，甚至通过 Maven 和 Tomcat 来做，但这里我们着眼于 Docker Compose 方式，因为这通常是最好的。

那么，开始吧！

## 你是第一次运行还是第二次？

决定如何更改配置的最大因素之一，是你首次构建 openrouteservice Docker 容器，还是重启一个已经构建好的容器。

总的来说，我们建议你先用默认设置构建 openrouteservice，然后在首次构建之后再做修改，因为那时文件夹结构和配置文件都已生成好了。

### 首次构建

如果你刚从 GitHub 仓库下载 openrouteservice（`git clone https://github.com/GIScience/openrouteservice`），并且还没有运行过 `docker-compose up`，那么首先要看的是 `docker` 文件夹中的 `docker-compose.yml` 文件。你在这里告诉 Docker 要使用哪些文件。

最容易更改的是你要使用的数据。在该文件中，你会看到一段被注释掉的代码，如下所示：
```yaml
#    build:
#      context: ../
#      args:
#        ORS_CONFIG: ./ors-api/src/main/resources/gr-config-sample.json
#        OSM_FILE: ./ors-api/src/main/files/heidelberg.osm.gz
```
这段代码告诉 docker-compose：我们不要使用 docker hub 上的预构建镜像（或你已经构建好的镜像），而是要在这里构建镜像。所以首先，把这行块中每行开头的 `#` 去掉，取消注释。`args` 中的两个值用于告诉构建过程使用哪个配置文件（我们很快会讲到）以及使用哪个 osm 数据集。这里容易让人困惑的一点是，文件相对于 docker-compose 文件应放在哪里。你会看到 `context: ../` 这一项，它告诉 docker-compose：Docker 文件所在的“根”文件夹以及由此产生的所有路径，都在 `docker-compose.yml` 文件所在位置的上一级文件夹。这意味着你在两个 `args` 项中定义的任何路径都是相对于该根文件夹的。
所以，假设你想使用德国的 OSM PBF 文件——你可以在 Docker 文件所在的同一文件夹中创建一个名为 `data` 的文件夹，把 PBF 文件放进去。然后在 docker-compose 文件中，把 `OSM_FILE` 项改为 `OSM_FILE: ./data/germany-latest.osm.pbf`。现在当你运行 `docker-compose up` 时，该文件会被复制到容器中，并重命名为 `/ors-core/data` 文件夹中的 `osm_file.pbf`，这就是用于构建图的文件。

另一行 `ORS_CONFIG: ...` 告诉系统要把哪个配置文件复制到容器中，作为创建与 Docker 容器兼容的配置文件的基础。由于使用的所有文件路径都必须相对于容器的内部文件结构，所以在容器首次构建时，所提供的配置文件会被修改，使其中的 osm 文件、高程数据和图的路径都改为指向 `/ors-core/data/...` 文件夹。此外，它还被设置为只构建要构建的 profile 列表中的第一个 profile（见下文）。随后，生成的 gr-config.json 文件会被复制到 `conf` 文件夹，该文件夹现在位于 docker-compose 文件所在的文件夹中。建议你在完成构建后，以此配置文件作为所有修改的基础。

### 运行已构建的容器

如果你已经构建过 openrouteservice 容器一次，那么修改 osm 数据和配置会相对容易一些。

在做修改之前，你首先应该停止容器。**请注意，使用 `docker-compose restart` 不会带入 `docker-compose.yml` 文件中所做的更改，因此你应该用 `docker-compose down` 关闭容器，在修改完成后再用 `docker-compose up` 重新启动。**

如果你所做的更改涉及 OSM 数据或会影响图构建方式的设置，则需要删除 `graphs` 中的文件夹，或者确保 `docker-compose` 文件中 `BUILD_GRAPHS` 被设为 true。这样图就会用新的数据/设置重新构建。

要更改所使用的 OSM 数据，你需要取消注释 `docker-compose.yml` 文件中 `volumes` 部分下的以下行：
```yaml
#- ./your_osm.pbf:/ors-core/data/osm_file.pbf
```
然后把 `./your_osm.pbf` 部分改为指向你想使用的 osm 文件。这会告诉 docker 用你提供的文件覆盖容器中当前的 osm 文件。

需要注意的一点是数据大小以及容器内分配给 Java 的 RAM 容量。我们通常[建议](System-Requirements)至少分配文件大小两倍的 RAM，但要让 Java 知道这一点，你需要更新 `docker-compose` 文件 `environment` 部分中的 `JAVA_OPTS`。在该行中，你会看到 `-Xms1g` 和 `-Xmx2g` 两项。它们告诉 Java 初始分配 1GB RAM，使用上限不超过 2GB。如果你的 pbf 文件大小为 1.5GB，那么就应把 `-Xmx` 项改为**至少** `-Xmx3g`。总的来说，如果条件允许，我们建议再多加一点 RAM，以降低在图构建接近尾声时出现内存溢出异常的可能性。

要更改任何配置设置，你只需修改 `conf` 文件夹中的 `gr-config.json` 文件，然后按上文所述重启容器。有许多配置项可以修改，关于每项具体含义的信息可参阅[配置 wiki 页面](Configuration)。举个简单的例子，默认情况下 openrouteservice docker 容器只构建 car profile。如果你想添加另一个 profile，例如 hiking profile，你只需要修改
```json
"active": [
            "car"
          ],
```
列表以包含该 profile：
```json
"active": [
            "car",
            "hiking"
          ],
```
现在当你重启容器时，hiking profile 也会被构建。再举一个例子，假设你不满足于矩阵请求中默认的最大位置数（100），而是想用多得多的位置做分析，例如 10,000，以便完成 100 个起点 x 100 个终点的矩阵计算。为此，你可以把 `matrix` 对象中的 `"maximum_routes": 100` 改为 `"maximum_routes": 10000`，然后重启容器。

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

## 实例基础设施
尽管对于较小的数据集或图数据无需更新时，单个容器运行得很好，但在许多实际部署中，只用一个实例并不是最合适的方案。如果你只有一个容器，那么所有构建和路线服务都通过这一个容器进行，这意味着当你重建图时，就无法向该实例发出诸如路线规划之类的请求，因为没有可用于生成路线的完整图。如果既要能够用新数据更新图，又要确保用户无法发出请求的时间尽可能短，我们建议使用两个实例——一个持续活跃、用于处理请求，另一个启动起来用于重建图。

在这种设置下，图构建完成后，你可以简单地停止处理请求的容器，替换所用的图（它们映射到 `docker-compose` 文件中定义的宿主机文件夹），然后重启容器。新的图会被重新加载到内存中（所需时间取决于图的大小和硬盘类型），随后即可用于路线规划。重新加载已构建好的图所造成的停机时间通常远少于构建图所需的时间。不过要注意，你应确保构建服务器和请求服务器上的配置文件与分配的 RAM 容量（如前面所述）完全相同，否则新构建的图可能无法加载。**另外，请确保处理请求的容器所用 `docker-compose` 文件中的 `BUILD_GRAPHS` 参数设为 false，否则它会尝试自己构建图！**
