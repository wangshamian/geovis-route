# 使用 Docker 运行

如果你的系统上可以使用 Docker，运行 openrouteservice 最简单的方式是启动一个[预构建镜像](#running-prebuilt-images)。
你无需克隆 openrouteservice 仓库，运行预构建镜像不需要源代码。
此外，也不必安装 java 和 maven。

如果你有特殊需求，也可以[构建自定义 docker 镜像](building-from-source.md#build-docker-image)。

::: warning
docker 文档仅适用于 openrouteservice v8.0.0 及之后的版本！
:::

## 前置条件

- 你的系统上必须安装 [Docker](https://docs.docker.com/get-docker/)。
- 一个 OSM 摘录，例如来自 [Geofabrik](http://download.geofabrik.de)，或者对于小区域，可以直接从
    [osm.org](https://www.openstreetmap.org/export#map=15/49.4197/8.6893) 导出。

除非你想构建自定义的 docker 镜像，否则不需要 java 或 maven :)


## 运行预构建镜像

openrouteservice 团队提供了一个 `docker compose` 文件，只需一条简单命令即可拉取并启动特定版本的预构建 openrouteservice docker 镜像。
从 openrouteservice v8.0.0 开始，你可以（也应该）从 [releases 页面](https://github.com/GIScience/openrouteservice/releases)的 "assets" 部分下载该 compose 文件，
既可以在浏览器中下载，也可以例如使用

```shell
wget https://github.com/GIScience/openrouteservice/releases/download/v8.0.0/docker-compose.yml
```

现在在后台启动 openrouteservice：
```shell
docker compose up -d
```

这将拉取所选版本的 openrouteservice，并使用示例设置以及提供的德国海德堡及周边地区测试 OSM 文件启动它。

要查看容器的日志，请运行
```shell
docker compose logs 
```
或
```shell
docker compose logs -tf  
```
以持续跟踪日志。

要再次停止容器，请执行
```shell
docker compose stop 
```
或
```shell
docker compose down
```
以停止并移除容器。

文件 `docker-compose.yml` 中包含大量带有说明的注释选项。
你可以取消注释这些选项并设置自定义的选项值。

::: details 不使用 docker compose 运行
修改 Docker compose 文件更容易，但为了测试特定设置，你也可以直接在命令行中运行与 docker compose 所执行的类似的命令。
以下是一个基本示例：
```shell
# create directories for volumes to mount as local user
mkdir -p ors-docker/config ors-docker/elevation_cache ors-docker/graphs ors-docker/files ors-docker/logs 
docker run -dt --name ors-app \
  -p 8080:8082 \
  -v $PWD/ors-docker/config:/home/ors/config \
  -v $PWD/ors-docker/elevation_cache:/home/ors/elevation_cache \
  -v $PWD/ors-docker/graphs:/home/ors/graphs \
  -v $PWD/ors-docker/files:/home/ors/files \
  -v $PWD/ors-docker/logs:/home/ors/logs \
  -e "XMS=1g" \
  -e "XMX=2g" \
  local/openrouteservice:latest 
```
添加 `-e "ors.engine.source_file=files/your.osm.pbf" \` 以指向位于挂载的 $PWD/ors-docker/files 文件夹中的你自己的 OSM 文件。
添加 `-e "BUILD_GRAPHS=True" \` 以触发图重建。

:::


## 你将获得什么

一段时间后，你的 openrouteservice 应该已经运行：
```shell
curl http://localhost:8080/ors/v2/health
{"status":"ready"}
```

当你按说明启动容器后，docker 会创建一个包含若干子目录和文件的 `ors-docker` 目录：
```shell
.
├── docker-compose.yml
└── ors-docker
    ├── config
    │   ├── example-gr-config.env      
    │   ├── example-gr-config.yml     
    │   └── gr-config.yml
    ├── elevation_cache
    │   ├── srtm_38_03.gh
    │   └── srtm_38_03.zip
    ├── files
    │   └── example-heidelberg.osm.gz
    ├── graphs
    │   └── car
    │       ├── edges
    │       ├── ...
    │       ├── ...
    │       └── turn_costs
    └── logs
        └── ors.log

8 directories, 35 files
```

* `example-gr-config.env`：env 文件模板。该文件的定制副本可与 `docker-compose.yml` 组合使用，参见[在环境文件中设置属性](#set-openrouteservice-properties-in-an-environment-file)。不要直接编辑此文件，因为它会被 docker 覆盖！
* `example-gr-config.yml`：YAML 文件模板。该文件的定制副本可作为 `gr-config.yml` 使用，参见[使用定制配置文件](#use-customized-config-file)。不要直接编辑此文件，因为它可能会被 docker 覆盖：例如，如果 `docker-compose.yml` 中使用了更新的 docker 镜像标签，而更新版 openrouteservice 中配置属性或其默认值发生了变化，`example-gr-config.yml` 可能会被更新。更新后的文件可以与定制的 `gr-config.yml` 进行比较。
* `gr-config.yml`：容器内 openrouteservice 使用的默认配置。此文件不会被 docker 覆盖。初始时，该文件与 `example-gr-config.yml` 相同。参见[使用定制配置文件](#use-customized-config-file)
* `elevation_cache`：openrouteservice 存储已下载高程数据以避免重复下载的目录。
* `example-heidelberg.osm.gz`：用于以初始设置运行容器的初始示例 OSM 数据。通常，你会删除此文件，或将另一个 OSM 文件添加到同一目录并调整配置以使用你的文件。
* `graphs`：用于存放生成的图的目录。初始时只有 `car`，因为它是默认 `gr-config.yml` 中唯一启用的路由配置
* `logs`：openrouteservice 日志存储于此。:warning: 多次运行的日志会在此累积。相比之下，通过 `docker compose logs` 获得的日志仅为当前运行的日志，但另一方面它们包含来自 docker 或入口脚本的非常有用的日志记录，这些在 `logs` 中的文件里是看不到的。


### 避免文件归属于 root

在默认的 `docker-compose.yml` 下，生成的文件归属于 root。
如果你没有 root 权限（sudo），或者只是不想让容器绑定的挂载中出现 root 拥有的文件，例如无法在 `ors-docker/config` 中编辑或添加文件，容器提供了以 UID 1000 和 GID 1000 运行的可能性。要能够访问这些文件，你需要在 docker 主机上拥有 UID 1000 或属于组 1000。
当然，你也可以借助 `docker-compose.yml`（参见[此处](#set-openrouteservice-properties-in-docker-composeyml)）或引用的环境文件（参见[那里](#set-openrouteservice-properties-in-an-environment-file)）来配置 openrouteservice，
该环境文件不必位于 config 文件夹中，因为 docker 内运行的 openrouteservice 无需“看到”环境文件。

但如果你例如想将新的 pbf 文件保存到 `ors-docker/files`，那么你需要 sudo 权限。

要避免生成的文件夹和文件归属于 root，

1. 取消注释 `docker-compose.yml` 中的 'user' 行：
    ```yaml
        #user: "1000:1000" // [!code --]
        user: "1000:1000" // [!code ++]
    ```
2. 用你的用户创建这些文件夹，这样你的用户就会成为这些文件夹的所有者：
    ```shell
    mkdir -p ors-docker/config ors-docker/elevation_cache ors-docker/files ors-docker/graphs ors-docker/logs
    ```
    如果文件夹已存在（归属于 root）而你又不想删除它们，可以更改其所有权（同样需要 root 权限）。
3. 当你现在运行 `docker compose down && docker compose up -d` 时，文件和文件夹应可被你的用户编辑。

如你在上面的 `docker-compose.yml` 片段中所见，这对用户 ID 为 1000、组 ID 为 1000 的用户有效，这也是 linux 主机上第一个普通用户的默认值。
但如果你的用户 ID 和组 ID 不同，这个公式对你就不适用了。

你可以使用以下命令检查自己的用户 ID（uid）和组 ID（gid）
```shell
id
```

如果你的 uid/gid 不同，那么你仍然可以在 docker compose 文件中指定你的 uid 和 gid 后构建自己的 docker 镜像。
假设你的 uid=1003、gid=1003：

1. 编辑你的 `docker-compose.yml`：
    ```yaml
        #user: "1000:1000" // [!code --]
        user: "1003:1003" // [!code ++]
    ```
2. 按照[构建自定义 docker 镜像](building-from-source.md#build-docker-image)中的说明在本地构建你的 docker 镜像
3. 现在你应该能够执行上述第 2 步和第 3 步。


### 容器内部和主机上的目录

docker 容器是一种精简的 linux 系统，带有特定的 openrouteservice 设置。
内部安装了正确的 java 版本，用于运行 openrouteservice fat jar。

openrouteservice 启动时的**内部**工作目录是 `/home/ors`。
内部 openrouteservice 被配置为将该工作目录的子文件夹作为预期存放特定输入文件或写入生成数据的位置：

* `/home/ors/config` 
* `/home/ors/graphs` 
* `/home/ors/files` 
* `/home/ors/logs` 
* `/home/ors/elevation_cache` 


yml 中的绝对路径，例如
```yaml
     source_file: /home/ors/files/example-heidelberg.osm.gz
```
会被解释为容器**内部**的绝对路径。

相对路径，例如
```yaml
       cache_path: ./elevation_cache
```
会被解释为相对于容器内工作目录 `/home/ors` 的路径。
 

## 使用不同的目录作为绑定挂载

如果你想挂载主机文件系统中现有的文件夹，而这些文件夹不是 openrouteservice docker 工作目录 `ors-docker` 的子文件夹，
你可以修改 `docker-compose.yml` 中的 volumes 部分。

在以下示例中，某位用户有一个包含 OSM 文件的目录 `/data/osm`。该用户希望直接从那里引用这些文件，而不是将大文件复制到其他地方。
而 openrouteservice 生成的图文件也应存储到另一个位置，那里有比主目录更多的磁盘空间，假设位于 `/data/ors/graphs`。

内部 openrouteservice 所需的目录被单独挂载，
主机系统上的路径写在冒号之前，容器内（未改变的）目录写在冒号之后。

```yaml
    volumes:  # Mount relative directories. ONLY for local container runtime. To switch to docker managed volumes see 'Docker Volumes configuration' section below.
      - ./ors-docker:/home/ors // [!code --]
      - /data/ors/graphs:/home/ors/graphs // [!code ++]
      - ./ors-docker/elevation_cache:/home/ors/elevation_cache // [!code ++]
      - ./ors-docker/config:/home/ors/config // [!code ++]
      - ./ors-docker/logs:/home/ors/logs // [!code ++]
      - /data/osm:/home/ors/files // [!code ++]
```

经过这些更改后，`gr-config.yml` 中配置的 `source_file`：

```yaml
     source_file: ./files/andorra-latest.osm.pbf
```

现在引用的是主机文件系统上的文件 `/data/osm/andorra-latest.osm.pbf`。

::: warning 注意
当各个主机目录被挂载到 `/home/ors` 的内部子目录时，
不能将任何目录直接挂载到内部的 `/home/ors` 上！
:::

## 使用卷

除了使用绑定挂载之外，你还可以使用卷。取消注释**并补全** `docker-compose.yml` 中的卷定义（参见 [docker compose 文档](https://docs.docker.com/compose/compose-file/07-volumes/)）：

```yaml
# ----------------- Docker Volumes configuration ------------------- #
# Define the Docker managed volumes for the ORS application. For more info on Docker Volumes see https://docs.docker.com/compose/compose-file/07-volumes/
# Volumes are used to persist the data and configuration of the ORS application.
# If you want to use volumes, uncomment the following lines and remove the ./ prefix from the volume paths in 'services.ors-app.volumes'.
#volumes: // [!code --]
#    graphs: // [!code --]
#    elevation_cache: // [!code --]
#    config: // [!code --]
#    logs: // [!code --]
#    files: // [!code --]
volumes: // [!code ++]
    graphs: // [!code ++]
    elevation_cache: // [!code ++]
    config: // [!code ++]
    logs: // [!code ++]
    files: // [!code ++]
```

在 `docker-compose.yml` 的 yml 部分 services.ors-app.volumes 中，你在冒号左侧设置卷名而不是主机路径：
```yaml
services:
  ors-app:
    volumes:
      - ./ors-docker:/home/ors  # Mount the ORS application directory (for logs, graphs, elevation_cache, etc.) into its own directory // [!code --]
      #- ./graphs:/home/ors/graphs  # Mount graphs directory individually // [!code --]
      #- ./elevation_cache:/home/ors/elevation_cache  # Mount elevation cache directory individually // [!code --]
      #- ./config:/home/ors/config  # Mount configuration directory individually // [!code --]
      #- ./logs:/home/ors/logs  # Mount logs directory individually // [!code --]
      #- ./files:/home/ors/files  # Mount files directory individually // [!code --]
      #- ./ors-docker:/home/ors  # Mount the ORS application directory (for logs, graphs, elevation_cache, etc.) into its own directory // [!code ++]
      - graphs:/home/ors/graphs  # Mount graphs directory individually // [!code ++]
      - elevation_cache:/home/ors/elevation_cache  # Mount elevation cache directory individually // [!code ++]
      - config:/home/ors/config  # Mount configuration directory individually // [!code ++]
      - logs:/home/ors/logs  # Mount logs directory individually // [!code ++]
      - files:/home/ors/files  # Mount files directory individually // [!code ++]
```

## 配置

使用 openrouteservice docker 镜像时，你有几种配置选项：

* 你可以像以 JAR 或 WAR 方式运行 openrouteservice 时那样使用配置文件。如果你是 `ors-docker/config` 中文件的所有者，这很直接（参见[上文](#avoid-files-owned-by-root)）。
* 你可以在 `docker-compose.yml` 中将配置属性设置为环境变量
* 或者你可以在环境文件中设置配置属性。这一般是 docker 容器的良好实践，也是当文件归属于 root 时配置容器化 openrouteservice 的首选方式。

以下各节将更详细地解释这些选项。
有关可进行的配置设置的详细信息，请参阅
[配置](configuration/index.md)章节。

::: tip
如果你想重建图，许多人只是删除 graphs 文件夹并重启 openrouteservice。
如果你不是图文件的所有者（而是 root），在没有 root 权限的情况下你_无法_删除这些文件。
在这种情况下，你可以在 [`docker-compose.yml`](#set-openrouteservice-properties-in-docker-composeyml) 中设置环境变量 `REBUILD_GRAPHS=True`（该选项包含在文件中，只需取消注释）
或在你的 [`gr-config.env`](#set-openrouteservice-properties-in-an-environment-file) 中设置。
openrouteservice 随后将在下次启动时重建所有已激活的图。
:::

### 使用（定制的）配置文件

如[配置 > 文件位置](configuration/index.md#file-location)中所述，
当 openrouteservice 启动时，它会在不同目录中查找名为 `gr-config.yml` 的文件，如果存在则使用该文件。
或者，可以通过设置环境变量 `GR_CONFIG_LOCATION` 来使用自定义命名或位于其他位置的配置文件（YAML 或已弃用的 JSON 配置）。

在 Docker 版本中，有一个在每次容器启动时执行的初始化脚本，
它采用了略有不同的方式：

* 它会检查是否设置了 `GR_CONFIG_LOCATION`，以及能否找到所引用的文件（YAML 或已弃用的 JSON 配置）（该路径是[从容器内部](#directories-inside-the-container-and-on-the-host)解析的）。
  如果找到，则使用此配置。
* 否则，会查找文件 `/home/ors/config/gr-config.yml` 或 `/home/ors/config/gr-config.json`（容器内路径）。如果存在，则会加载它。
  如果不存在，则会作为 `/home/ors/config/example-gr-config.yml` 的副本添加，**并且也会被加载！**

这意味着，在任何情况下都会加载配置文件！
即使你如接下来两节所述设置了环境变量，
你也必须注意被加载的配置文件，在某些情况下你必须覆盖单个属性，
例如禁用路由配置 `car`——它在默认 `gr-config.yml` 中是启用的——如果你不想启用它的话。

你可以通过编辑生成的 `gr-config.yml` 来配置你的 openrouteservice。
只要该文件仍然存在，它就永远不会被 openrouteservice 覆盖。
但你绝不应编辑 `example-gr-config.yml` `example-gr-config.env`——这些文件在某些情况下会被替换。


### 在 `docker-compose.yml` 中设置 openrouteservice 属性

可以将 openrouteservice 配置属性作为环境变量传入 docker 容器。
`docker-compose.yml` 以注释形式包含许多可用的配置选项，并附有简短说明。
以下修改后的 `docker-compose.yml` 示例片段展示了一个禁用 car 配置、启用 wheelchair 配置的设置：

```yaml
    environment:
      # ----------------- Properties configuration ------------------- #
      # Configure your whole container with only property ENVs.
      # These can be set alternatively or additionally to the yml configuration file.
      # Note, that any values set will override the corresponding values from the yml configuration file.
      # See the gr-config.env file for more options.
      # To have a configuration file-less container, uncomment at least the following properties.
      # The values are examples and provide the default configuration.
      ors.engine.source_file: /home/ors/files/andorra-latest.osm.pbf
      ors.engine.profiles.car.enabled: false
      ors.engine.profiles.wheelchair.enabled: true
      # Here you can also set more advanced spring and tomcat properties, such as proxy settings.
      # See the spring documentation for a complete list of options: https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html
      #server.tomcat.remote-ip-header=x-your-remote-ip-header
      #server.tomcat.protocol-header=x-your-protocol-header
```

::: warning 提示
请记住，必须重新创建容器才能使 `docker-compose.yml` 中的更改生效！
`docker compose stop && docker compose start` 或 `docker compose restart` 是不够的，你必须执行
```shell
docker compose down && docker compose up
```
:::

配置属性的语法/名称不是 yaml，而是 java properties 文件的语法。
不过这只是语法不同。属性与[配置 > 文件格式](configuration/index.md#file-formats)中描述的相同。

### 在环境文件中设置 openrouteservice 属性

在 `docker-compose.yml` 中配置属性是设置少量属性的好方法。
如果你想定制大量属性，或者你可能有多种配置在使用，
将属性保存到单独的文件中会更方便。

上一节 `docker-compose.yml` 片段中设置的属性可以改为在单独的文件 `gr-config.env` 中设置。
唯一的区别是使用等号而不是冒号：

```
ors.engine.source_file=/home/ors/files/andorra-latest.osm.pbf
ors.engine.profiles.car.enabled=false
ors.engine.profiles.wheelchair.enabled=true
```

env 文件必须在 `docker-compose.yml` 中被引用：
```yaml
    # ----------------- ENV file configuration ------------------- #
    # Too many variables for your 'environment:' section?
    # Use an env_file with the ENV properties instead and define everything in there:
    # Values will be overwritten if set in the 'environment' section.
#    env_file: gr-config.env // [!code --]
    env_file: gr-config.env // [!code ++]
```

如果你想使用此配置选项并开始创建环境文件，
你可以使用文件 `ors-docker/config/example-gr-config.env` 作为模板。
将其复制到 `docker-compose.yml` 旁边或重命名它。
你绝不应编辑文件 `ors-docker/config/example-gr-config.env`，因为它在某些情况下会被 openrouteservice 替换！


## 故障排除

如果 openrouteservice 无法启动或无法按预期运行，最重要的是检查日志。
有两种查看日志的方式，存在一些差异：`docker compose logs` 和 `ors-docker/logs/ors.log`

| `docker compose logs`                            | `ors-docker/logs/ors.log`                                                                       | 
|--------------------------------------------------|-------------------------------------------------------------------------------------------------|
| 来自 docker 入口点和 openrouteservice 的日志     | 仅来自 openrouteservice 的日志                                                                  |
| 仅来自当前容器的日志                             | 来自之前容器的日志，即使已通过例如 `docker compose down` 移除，仍然可用                          |

本节将侧重于来自 docker 的日志，因为它们包含附加信息。
要查看容器的入口点日志，请切换到包含 `docker-compose.yml` 的目录并运行
```shell
docker compose logs
```

最初记录的几行由容器内的入口脚本写入，
该脚本在内部 openrouteservice 启动_之前_的每次容器启动时执行。
在这里你可以深入了解容器的内部运作：

```shell
ors-app  | 2024-03-12T08:13:19.118290567Z #################
ors-app  | 2024-03-12T08:13:19.118317827Z # Container ENV #
ors-app  | 2024-03-12T08:13:19.118320018Z #################
ors-app  | 2024-03-12T08:13:19.118321664Z ✓ CONTAINER_LOG_LEVEL: INFO. Set CONTAINER_LOG_LEVEL=DEBUG for more details.
ors-app  | 2024-03-12T08:13:19.125163248Z ⓘ Any config file settings can be overwritten by environment variables.
ors-app  | 2024-03-12T08:13:19.125181048Z ⓘ Use 'CONTAINER_LOG_LEVEL=DEBUG' to see the full list of active environment variables for this container.
ors-app  | 2024-03-12T08:13:19.125193428Z ###########################
ors-app  | 2024-03-12T08:13:19.125195433Z # Container sanity checks #
ors-app  | 2024-03-12T08:13:19.125208729Z ###########################
ors-app  | 2024-03-12T08:13:19.127963029Z ⓘ Running container as user root with id 0 and group 0
ors-app  | 2024-03-12T08:13:19.130316750Z ✓ ORS_HOME: /home/ors exists and is writable.
ors-app  | 2024-03-12T08:13:19.134116420Z ✓ The file /home/ors/config/example-gr-config.env is up to date
ors-app  | 2024-03-12T08:13:19.135634341Z ✓ The file /home/ors/config/example-gr-config.yml is up to date
ors-app  | 2024-03-12T08:13:19.135753416Z ✓ Using existing /home/ors/config/gr-config.yml
ors-app  | 2024-03-12T08:13:19.142101283Z ⓘ Default to graphs folder: /home/ors/graphs
ors-app  | 2024-03-12T08:13:19.142118351Z ⓘ Default to example osm source file: "/home/ors/files/example-heidelberg.osm.gz"
ors-app  | 2024-03-12T08:13:19.142119978Z ⓘ Any ENV variables will have precedence over configuration variables from config files.
ors-app  | 2024-03-12T08:13:19.142151919Z ✓ All checks passed. For details set CONTAINER_LOG_LEVEL=DEBUG.
ors-app  | 2024-03-12T08:13:19.142159578Z #####################################
ors-app  | 2024-03-12T08:13:19.142160979Z # Container file system preparation #
ors-app  | 2024-03-12T08:13:19.142162089Z #####################################
ors-app  | 2024-03-12T08:13:19.199741022Z ✓ The file /home/ors/files/example-heidelberg.osm.gz is up to date
ors-app  | 2024-03-12T08:13:19.199762468Z ✓ Container file system preparation complete. For details set CONTAINER_LOG_LEVEL=DEBUG.
ors-app  | 2024-03-12T08:13:19.199764187Z #######################################
ors-app  | 2024-03-12T08:13:19.199765671Z # Prepare CATALINA_OPTS and JAVA_OPTS #
ors-app  | 2024-03-12T08:13:19.199766895Z #######################################
ors-app  | 2024-03-12T08:13:19.200084573Z ✓ CATALINA_OPTS and JAVA_OPTS ready. For details set CONTAINER_LOG_LEVEL=DEBUG.
ors-app  | 2024-03-12T08:13:19.200120626Z #####################
ors-app  | 2024-03-12T08:13:19.200127702Z # ORS startup phase #
ors-app  | 2024-03-12T08:13:19.200129261Z #####################
ors-app  | 2024-03-12T08:13:19.200130392Z ✓ 🙭 Ready to start the ORS application 🙭
```

如日志中所述，通过在 `docker-compose.yml` 中设置 `CONTAINER_LOG_LEVEL=DEBUG`，你可以获得更多信息。
此设置仅与入口点日志记录相关，它不会改变 openrouteservice 的日志级别。
如何做到这一点记录在[日志文档](configuration/spring/logging.md)中。

::: warning 提示
请记住，必须重新创建容器才能使 `docker-compose.yml` 中的更改生效！
`docker compose stop && docker compose start` 或 `docker compose restart` 是不够的，你必须执行
```shell
docker compose down && docker compose up
```
:::

::: details 跟踪日志
如果你想在 openrouteservice 运行时查看日志，你也可以
```shell
docker compose logs -tf  
```
以持续跟踪日志。按 Ctrl+C 停止跟踪日志，容器不会停止。
:::

你会在 docker 日志的入口点部分找到的重要信息：

* 容器内使用的用户，包括 uid 和 gid
* 容器内的所有环境变量（仅在使用 `CONTAINER_LOG_LEVEL=DEBUG` 时）
* 有关容器内解析路径的信息
* 有关示例文件生成或替换的信息
* 与设置相关的错误

在入口点部分之后，是来自 openrouteservice 本身的日志。
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
ors-app  | 2024-03-12 10:54:46 INFO                                              ORS-Init [ o.h.o.r.RoutingProfileManager            ]   ====> Initializing profiles from '/home/ors/files/example-heidelberg.osm.gz' (1 threads) ...
ors-app  | 2024-03-12 10:54:46 INFO                                              ORS-Init [ o.h.o.r.RoutingProfileManager            ]   2 profile configurations submitted as tasks.
ors-app  | 2024-03-12 10:54:46 INFO                                     ORS-pl-wheelchair [ o.h.o.r.RoutingProfile                   ]   [1] Profiles: 'wheelchair', location: './graphs/wheelchair'.
ors-app  | 2024-03-12 10:54:47 INFO                                     ORS-pl-wheelchair [ o.h.o.r.g.e.ORSGraphHopper               ]   version v4.9.1|2024-01-17T09:08:46Z (7,20,5,4,5,7)
ors-app  | 2024-03-12 10:54:47 INFO                                     ORS-pl-wheelchair [ o.h.o.r.g.e.ORSGraphHopper               ]   graph wheelchair|RAM_STORE|3D|turn_cost|,,,,, details:edges:0(0MB), nodes:0(0MB), name:(0MB), geo:0(0MB), bounds:1.7976931348623157E308,-1.7976931348623157E308,1.7976931348623157E308,-1.7976931348623157E308,1.7976931348623157E308,-1.7976931348623157E308
ors-app  | 2024-03-12 10:54:47 INFO                                     ORS-pl-wheelchair [ o.h.o.r.g.e.ORSGraphHopper               ]   No custom areas are used, custom_areas.directory not given
ors-app  | 2024-03-12 10:54:47 INFO                                     ORS-pl-wheelchair [ o.h.o.r.g.e.ORSGraphHopper               ]   start creating graph from /home/ors/files/example-heidelberg.osm.gz
ors-app  | 2024-03-12 10:54:47 INFO                                     ORS-pl-wheelchair [ o.h.o.r.g.e.ORSGraphHopper               ]   using wheelchair|RAM_STORE|3D|turn_cost|,,,,, memory:totalMB:1024, usedMB:260
ors-app  | 2024-03-12 10:54:47 INFO                                                  main [ o.h.o.a.org.heigit.ors.api.Application                      ]   Started org.heigit.ors.api.Application in 2.442 seconds (process running for 3.066)
ors-app  | 2024-03-12 10:54:47 INFO                                                  main [ o.h.o.a.org.heigit.ors.api.Application                      ]   openrouteservice {"build_date":"2024-03-08T15:01:47Z","version":"8.0"}
```

这里最重要的信息：

* openrouteservice 和 java 的版本：`Starting org.heigit.ors.api.Application v8.0-SNAPSHOT using Java 21.0.2`（在 Docker 设置中你不必担心 Java 版本）
* 解析到的配置文件：`Loaded file '/home/ors/config/gr-config.yml'`（这是容器内的路径）
* 内存使用情况：`Total - 1024 MB, Free - 965.93 MB, Max: 2 GB, Used - 58.07 MB`
* 解析到的 OSM 文件：`====> Initializing profiles from '/home/ors/files/example-heidelberg.osm.gz'`（同样为内部路径）
* 你的设置中可能存在的错误

而在启动部分之后，你将找到运行时的错误信息。

::: tip 提示
输出 `The following 1 profile is active: "default"` 来自 spring，
它指的是激活的 **spring profile**，与路由配置无关。
:::
