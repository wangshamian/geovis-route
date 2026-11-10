# 使用 Docker 运行

## 用 docker 安装并运行 openrouteservice

> 如需分步指南，请查看[这个 YouTube 视频](https://www.youtube.com/watch?v=VQXlbqKArFk)（非常感谢 SyntaxByte <3）。

使用 Docker 安装 openrouteservice 后端服务相当简单。你只需要一个 OSM 提取文件（例如来自 [Geofabrik](http://download.geofabrik.de)），以及一个可用的 [docker 安装](https://www.digitalocean.com/community/tutorial_collections/how-to-install-and-use-docker)。

使用 [Dockerhub 上托管的 Openrouteservice 镜像](https://hub.docker.com/r/openrouteservice/openrouteservice)，或者构建你自己的镜像
### Docker 使用场景

通过 docker 有多种方式可以快速获得一个运行中的实例。


1. 推荐：使用 `docker compose` 运行特定版本的 ors

```bash
# For example for the latest release
git clone https://github.com/GIScience/openrouteservice.git
cd openrouteservice
# Checkout latest version
export LATEST_ORS_RELEASE=$(git describe --tags --abbrev=0); 
git checkout $LATEST_ORS_RELEASE
# If the docker folder exists cd into it
cd docker || echo "No docker folder found. Continue with next step."
# Now change the version the docker-compose.yml uses
sed -i='' "s/openrouteservice\/openrouteservice:nightly/openrouteservice\/openrouteservice:$LATEST_ORS_RELEASE/g" docker-compose.yml
sed -i='' "s/openrouteservice\/openrouteservice:latest/openrouteservice\/openrouteservice:$LATEST_ORS_RELEASE/g" docker-compose.yml
# Run docker compose with
docker compose up -d
```

2. 使用 `docker compose` 运行 nightly 版本

```bash
# For nightly builds
wget https://raw.githubusercontent.com/GIScience/openrouteservice/master/docker-compose.yml
docker compose up -d
```

3. 对 ors 版本 >= 6.8.2 使用 `docker run`

```bash
# create directories for volumes to mount as local user
mkdir -p docker/conf docker/elevation_cache docker/graphs docker/logs/ors docker/logs/tomcat
docker run -dt -u "${UID}:${GID}" \
  --name ors-app \
  -p 8080:8080 \
  -v $PWD/docker/graphs:/home/ors/ors-core/data/graphs \
  -v $PWD/docker/elevation_cache:/home/ors/ors-core/data/elevation_cache \
  -v $PWD/docker/logs/ors:/home/ors/ors-core/logs/ors \
  -v $PWD/docker/logs/tomcat:/home/ors/tomcat/logs \
  -v $PWD/docker/conf:/home/ors/ors-conf \
  #-v $PWD/your_osm.pbf:/home/ors/ors-core/data/osm_file.pbf \
  #-e "BUILD_GRAPHS=True" \
  -e "JAVA_OPTS=-Djava.awt.headless=true -server -XX:TargetSurvivorRatio=75 -XX:SurvivorRatio=64 -XX:MaxTenuringThreshold=3 -XX:+UseG1GC -XX:+ScavengeBeforeFullGC -XX:ParallelGCThreads=4 -Xms1g -Xmx2g" \
  -e "CATALINA_OPTS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9001 -Dcom.sun.management.jmxremote.rmi.port=9001 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=localhost" \
  openrouteservice/openrouteservice:latest
```

4. 对 ors 版本 <= 6.8.1 使用 `docker run`

```bash
# create directories for volumes to mount as local user
mkdir -p docker/conf docker/elevation_cache docker/graphs docker/logs/ors docker/logs/tomcat
docker run -dt -u "${UID}:${GID}" \
  --name ors-app \
  -p 8080:8080 \
  -v $PWD/docker/graphs:/ors-core/data/graphs \
  -v $PWD/docker/elevation_cache:/ors-core/data/elevation_cache \
  -v $PWD/docker/logs/ors:/home/ors/ors-core/logs/ors \
  -v $PWD/docker/logs/tomcat:/home/ors/tomcat/logs \
  -v $PWD/docker/conf:/ors-conf \
  #-v $PWD/your_osm.pbf:/ors-core/data/osm_file.pbf \
  #-e "BUILD_GRAPHS=True" \
  -e "JAVA_OPTS=-Djava.awt.headless=true -server -XX:TargetSurvivorRatio=75 -XX:SurvivorRatio=64 -XX:MaxTenuringThreshold=3 -XX:+UseG1GC -XX:+ScavengeBeforeFullGC -XX:ParallelGCThreads=4 -Xms1g -Xmx2g" \
  -e "CATALINA_OPTS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9001 -Dcom.sun.management.jmxremote.rmi.port=9001 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=localhost" \
  openrouteservice/openrouteservice:v6.8.1
```

### Docker 配置

**你会得到什么**

上述所有场景都会：

1. 从 dockerhub 拉取 openrouteservice docker 镜像，并启动一个名为 `ors-app` 的容器
2. 在该容器中运行的 tomcat 内，于 `8080` 端口启动 openrouteservice 服务，可通过地址 `http://localhost:8080/ors` 访问。
3. 生成本地 `./docker` 文件夹，其中包含 openrouteservice 使用和生成的文件，方便访问。最重要的是控制 ORS 行为的 `./docker/conf/gr-config.yml`，以及海德堡及其周边地区的测试 OSM 数据文件 `/home/ors/ors-core/data/osm_file.pbf`。

**自定义**

拥有已构建的镜像后，你可以决定用不同的设置启动容器，例如更改活跃的 profile 或其他设置。要用自定义配置运行 ORS，请按需修改 `./docker/conf/gr-config.yml`，然后重启容器。显然，你也可以按需修改 `docker-compose.yml` 中的卷映射。

旧版 JSON 格式的 ORS 配置文件已被弃用。如果你有来自此前安装的自定义 `gr-config.json` 文件，我们强烈建议迁移到新的 YAML 格式。在过渡期内，ORS 允许使用放在 `./docker/conf/gr-config.json` 的旧格式 JSON 文件。若该文件存在，其中的所有设置都会覆盖正规 YAML 格式中的设置。

如果你在容器首次启动后更改了 OSM 文件，别忘了设置环境变量 `BUILD_GRAPHS=True`（参见 dockerfile 中的注释或上面的示例），以强制重建图（或者删除 `./docker/graphs` 文件夹的内容，效果相同）。

***UID***

如果你需要更改 ors 运行所使用的 UID，可以使用以下变量：
```bash
# set it explicitly to 1001
ORS_UID=1001 ORS_GID=1001 docker compose up -d

# or set it to the current user
ORS_UID=${UID} ORS_GID=${GID} docker compose up -d
```

***卷***

有些重要目录可能需要在宿主机上保留，以便在容器重建后数据仍然存在。这些目录应映射为卷。

- `/home/ors/ors-core/data/graphs`：在 ORS 初始化后包含已构建的图。
- `/home/ors/ors-core/data/elevation_cache`：如果指定了高程，则包含 CGIAR 高程瓦片。
- `/home/ors/ors-core/logs/ors`：包含 ORS 日志。
- `/home/ors/tomcat/logs`：包含 Tomcat 日志。
- `/home/ors/ors-conf`：包含用于控制 ORS 的 `gr-config.json`。
- `/home/ors/ors-core/data/osm_file.pbf`：用于生成图的 OSM 文件。

示例请参见 [`docker-compose.yml`](https://github.com/GIScience/openrouteservice/blob/master/docker-compose.yml)。

***环境变量***

- `BUILD_GRAPHS`：设为 `True` 时强制 ORS 重建路线图。当通过 Docker 卷映射把另一个 PBF 文件指定到 `/home/ors/ors-core/data/osm_file.pbf` 时很有用
- `JAVA_OPTS`：自定义 Java 运行时选项，例如 `-Xms` 或 `-Xmx`
- `CATALINA_OPTS`：自定义 Catalina 选项

可在容器启动时指定，也可写在 `docker-compose.yml` 中。

***构建参数***

构建镜像时，以下参数可自定义：

- `ORS_CONFIG`：可修改以指定自定义 `gr-config.json` 文件的位置。默认 `./ors-api/src/main/resources/gr-config.json`。
- `OSM_FILE`：可修改以指向本地的自定义 OSM 文件。默认 `./ors-api/src/test/files/heidelberg.osm.gz`。

**使用不同的 OSM 文件**

你可以在构建镜像时把构建参数 `OSM_FILE` 指向所需的 OSM 文件。

或者在重启容器时更改 PBF 文件：

1. 修改路径 `/home/ors/ors-core/data/osm_file.pbf`，使其指向你的新 PBF
2. 将 `BUILD_GRAPHS` 变量设置为 `True`

例如
`docker run -d -p 8080:8080 -e BUILD_GRAPHS=True -v ./data/andorra-latest.osm.pbf:/home/ors/ors-core/data/osm_file.pbf ors-app`

需要说明的是，如果你的数据集非常大，可能需要调整 `JAVA_OPTS` 环境变量中的 `-Xmx` 参数。一个不错的经验法则是给 Java 分配 PBF 文件大小 2 倍的 **每个 profile** 内存。

请注意，OSM 文件支持 `.osm`、`.osm.gz`、`.osm.zip` 和 `.pbf` 格式。

**检查**

默认情况下，可通过 `http://localhost:8080/ors/v2/health` 端点查询服务状态。服务就绪后，你可以请求 `http://localhost:8080/ors/v2/status` 获取正在运行的服务的更多信息。

如果使用默认数据集，你可以请求 `http://localhost:8080/ors/v2/directions/foot-walking?start=8.676581,49.418204&end=8.692803,49.409465` 进行测试。
