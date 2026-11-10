# 在 Ubuntu 22.04 上使用 Tomcat 10 部署 ORS v8

本指南将指导你如何使用 Java 17 和 Tomcat 10 部署 openrouteservice v8。

::: info
本教程的[德文版](de_tomcat-10-ubuntu_22_04)。
:::

## 前置条件

- 已启用 Systemd 的 Ubuntu 22.04（Systemd 默认应已启用）
- Tomcat 10
- Java 17+

## 前提假设

- 我们假设你的系统上只运行一个 Tomcat 10 实例。
- 如果你计划运行多个 Tomcat 服务，那么你是高级用户，应相应地调整这些说明。

## 准备 Tomcat 10 环境

以下步骤将指导你完成 Tomcat 10 环境的准备过程。

### 安装 Java 17

Openrouteservice v8 需要 Java 17 或更高版本才能运行。
原因是引入了 Tomcat 10。
如果有更高版本的 Java 可用，你也可以使用。

```shell
# Update the package index
> sudo apt-get update
# Install Java 17 or higher, curl and nano
> sudo apt-get install openjdk-17-jre-headless curl nano
```

- `openjdk-17-jre-headless` 是 Java 17 运行环境的 headless 包。openrouteservice 不需要图形用户界面。
- `curl` 是一个用于从网络下载文件的命令行工具，将用于下载某些资源。
- `nano` 是一个简单的文本编辑器，将用于编辑某些文件。

列出你可用的 Java 版本。

```shell
# List available options and copy the path to the Java 17 installation
sudo update-alternatives --list java
```

输出应类似于：

```shell
[...]
/usr/lib/jvm/java-17-openjdk-amd64/
[...]
```

将默认 Java 版本更新为 Java 17 或你安装的版本。

```shell
# Set the default Java version to Java 17
> sudo update-alternatives --set java /usr/lib/jvm/java-17-openjdk-amd64/bin/java
```

::: info
**注意：** Java 17 安装路径可能因系统而异。 \
**注意：** 从现在起，我们将把 Java 17 的安装路径称为 `JAVA_HOME`。
:::

### 为 Tomcat 10 创建新用户

出于安全原因，Tomcat 10 运行时应以单独的用户身份运行。
该用户不应有任何登录权限，也不应能够执行命令。
我们将此用户称为 `tomcat`。

```shell
# Create a new user for Tomcat 10
> useradd -r -m -U -d /opt/tomcat -s /bin/false tomcat
```

- `-r` 创建系统用户
- `-m` 为该用户创建主目录（`/opt/tomcat`）并应用必要的权限
- `-U` 创建与用户同名的组

### 下载并设置 Tomcat 10

```shell
# Set the Tomcat version
> export TOMCAT_VERSION=10.1.33
# Download the Tomcat 10 tarball
> curl -L https://dlcdn.apache.org/tomcat/tomcat-10/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz > apache-tomcat-$TOMCAT_VERSION.tar.gz
# Extract the downloaded file
> tar -xf apache-tomcat-$TOMCAT_VERSION.tar.gz
# Copy the contents of the extracted directory to the /opt/tomcat directory
> cp -R apache-tomcat-$TOMCAT_VERSION/** /opt/tomcat
# Clean up the extracted directory
> rm -r apache-tomcat-$TOMCAT_VERSION apache-tomcat-$TOMCAT_VERSION.tar.gz
```

### 为 Tomcat 10 用户设置必要的权限

以下命令将为 `tomcat` 用户设置必要的权限。
它们必须以 root 用户身份执行，并且在对 Tomcat 目录进行更改后应始终执行。

```shell
> sudo chown -R tomcat:tomcat /opt/tomcat
> sudo chmod -R 754 /opt/tomcat
```

- `chown -R tomcat:tomcat /opt/tomcat` 将 `/opt/tomcat` 目录的所有者和组设置为 `tomcat` 用户。
- `chmod -R 754 /opt/tomcat` 设置 `/opt/tomcat` 目录的权限。
    - `7` 授予所有者读、写和执行权限。
    - `5` 授予组读和执行权限。
    - `4` 授予其他人读权限。

### 为 Tomcat 10 创建 systemd 服务

我们现在为 Tomcat 10 创建一个 systemd 服务，以管理 Tomcat 10 实例。
这将使你的 Systemd 能够自动以服务方式启动和停止 Tomcat 10。
将内容粘贴到文件中并保存。

```shell
> sudo nano /etc/systemd/system/openrouteservice.service
```

```ini
[Unit]
Description=Tomcat - openrouteservice
After=syslog.target network.target

[Service]
Type=forking

User=tomcat
Group=tomcat
RestartSec=10
Restart=always

Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64/"
Environment="JAVA_OPTS=-Djava.security.egd=file:///dev/urandom"
Environment="CATALINA_OPTS=-server -XX:+UseParallelGC"

Environment=CATALINA_PID=/opt/tomcat/temp/tomcat.pid
Environment=CATALINA_HOME=/opt/tomcat
Environment=CATALINA_BASE=/opt/tomcat

ExecStart=/opt/tomcat/bin/startup.sh
ExecStop=/opt/tomcat/bin/shutdown.sh

[Install]
WantedBy=multi-user.target
```

### 测试新的 Tomcat 10 设置

```shell
# Reload the systemd daemon
> sudo systemctl daemon-reload
# Start the Tomcat 10 service
> sudo systemctl enable --now openrouteservice.service
# Check the status of the Tomcat 10 service
> sudo systemctl status openrouteservice.service
```

现在在浏览器中访问 [http://localhost:8080](http://localhost:8080) 以查看 Tomcat 10 欢迎页面。

## 准备 openrouteservice 环境

既然我们已经设置好 Tomcat 10，现在可以使用 Java 17 设置 openrouteservice。

### 下载 openrouteservice WAR 文件

要使用 Tomcat 10 设置 openrouteservice v8，你首先需要相应的 WAR 文件。
请前往 [openrouteservice releases 页面](https://github.com/GIScience/openrouteservice/releases)并下载
最新版本的 WAR 文件。

```shell
# Download the latest openrouteservice WAR-File for v8.2.0.
> curl -L https://github.com/GIScience/openrouteservice/releases/download/v8.2.0/ors.war > ors.war
# Move the WAR-File to the Tomcat webapps directory
> mv ors.war /opt/tomcat/webapps/
# Restart the Tomcat 10 service
> sudo systemctl restart openrouteservice.service
# Check the status of the Tomcat 10 service
> sudo systemctl status openrouteservice.service
```

如果你访问 `http://localhost:8080/ors/v2/health`，你应该会看到 openrouteservice 的健康状态。
输出将如下所示：

```json
{
    "status": "not ready"
}
```

::: info
ORS 现在已正确配置了 Tomcat 10，但它仍需要进行配置。因此，它显示 `not ready`。 \
如果你想查看确切的日志输出，可以查看 `/opt/tomcat/logs` 目录中的 tomcat 日志文件。
:::

### 创建 openrouteservice 文件夹结构

为了让 openrouteservice 正常工作，你需要设置文件夹结构、下载一个测试 osm 文件并
设置配置。

```shell
mkdir -p "/opt/openrouteservice/graphs"
mkdir -p "/opt/openrouteservice/logs"
mkdir -p "/opt/openrouteservice/data"
mkdir -p "/opt/openrouteservice/elevation_cache"
```

::: info
该文件夹结构只是一种建议，可以根据你的需求进行调整。 \
请确保相应地调整配置以及权限。
:::

### 下载一个测试 osm 文件

获取最新 osm 文件的一个好来源是 [Geofabrik 下载服务器](https://download.geofabrik.de/)。
我们将下载一个安道尔的小型测试 osm 文件。

```shell
# Download a test osm file to the /opt/openrouteservice/data directory
> curl -L https://download.geofabrik.de/europe/andorra-latest.osm.pbf > /opt/openrouteservice/data/andorra-latest.osm.pbf
```

### 配置 openrouteservice

配置 Tomcat 和 openrouteservice 最好通过在 Tomcat 的 `bin` 目录中的 `setenv.sh` 文件里设置配置来完成。

```shell
# Create the `setenv.sh` file in the `bin` directory of Tomcat.
> sudo nano /opt/tomcat/bin/setenv.sh
```

将以下内容粘贴到文件中并保存。
请确保将 `-Xmx` 值调整为适合你的系统和图的值。
如果你想构建另一个 OSM 文件，可以调整 `source_file` 的值。

如果你想了解更多关于版本 8 中新增配置选项的信息，请查看
[配置文档](/run-instance/configuration/index.md)。

**openrouteservice v8 的 `setenv.sh` 文件示例**

```shell
export CATALINA_OPTS="$CATALINA_OPTS -server -XX:+UseParallelGC -Xmx15g"
export JAVA_OPTS="$JAVA_OPTS \
-Dors.engine.profiles.car.enabled=true \
-Dors.engine.graphs_data_access=MMAP \
-Dors.engine.profile_default.elevation=false \
-Dors.engine.graphs_root_path=/opt/openrouteservice/graphs \
-Dors.engine.source_file=/opt/openrouteservice/data/andorra-latest.osm.pbf \
-Dlogging.file.name=/opt/openrouteservice/logs/ors.log \
-Dors.engine.elevation.cache_path=/opt/openrouteservice/elevation_cache
"
```

如果你想使用 RAM 存储而不是 MMAP 存储，请将 `ors.engine.graphs_data_access` 设置为 `RAM_STORE`。
请确保将 `-Xmx` 设置为适合你的系统和图的值，并确保你的系统提供足够的内存。

## 运行 openrouteservice

如果你按照上述步骤操作，openrouteservice 现在应该已使用 Tomcat 10 和 Java 17 正确设置好了。
每当你更改配置、文件夹结构或图时，以下步骤都是必须执行的：

```shell
# Reinstate ownerships for tomcat
> sudo chown -R tomcat:tomcat /opt/tomcat
> sudo chown -R tomcat:tomcat /opt/openrouteservice
> sudo chmod -R 754 /opt/tomcat/bin/setenv.sh
> sudo chmod -R 754 /opt/openrouteservice
# Restart the Tomcat 10 service
> sudo systemctl restart openrouteservice.service
# Check the status of the Tomcat 10 service
> sudo systemctl status openrouteservice.service
```

现在 openrouteservice 已使用正确的配置设置完成，请检查
`/opt/openrouteservice/logs/ors.log` 中的日志文件或 `/opt/tomcat/logs` 目录中的 Tomcat 日志文件。

在浏览器中访问 [http://localhost:8080/ors/v2/health](http://localhost:8080/ors/v2/health) 以查看 openrouteservice 的健康状态，或
向 API 发起请求。

```shell
> curl http://localhost:8080/ors/v2/health
```

输出应如下所示：

```json

{
    "status": "ready"
}
```

## 示例请求

让我们通过向 API 发起请求来测试 openrouteservice 是否正常工作。
请根据你下载的 osm 文件调整测试。

```shell
# Request a route from A to B withing Andorra
> curl -X POST \
  'http://localhost:8080/ors/v2/directions/driving-car' \
  -H 'Content-Type: application/json; charset=utf-8' \
  -H 'Accept: application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8' \
  -d '{"coordinates":[[1.5036892890930176, 42.4972256361276],[1.6298711299896242, 42.57831077271361]]}'

# Request an Isochrone from a point in Andorra
> curl -X POST \
  'http://localhost:8080/ors/v2/isochrones/driving-car' \
  -H 'Content-Type: application/json; charset=utf-8' \
  -H 'Accept: application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8' \
  -d '{"locations":[[1.5036892890930176, 42.4972256361276]],"range":[300]}'
```

## 可选：使用预构建图更新

如果你只想用新的预构建图更新现有图，可以按照以下步骤操作：

1. 从相应来源下载新的图。它们很可能以 zip 或 tar.xz 文件形式提供。
2. 解压新的图，并确保它们格式正确。

```shell
# In case of a tar.xz file
> tar -xvf new_graphs.tar.xz
# In case of a zip file you can use the unzip command
> unzip new_graphs.zip
```

解压后的图将具有类似如下的文件夹结构：

```shell
new_graphs/
├── driving-car
└── cycling-regular
```

3. 清空旧的图目录 `/opt/openrouteservice/graphs/`：

```shell
> mv /opt/openrouteservice/graphs /opt/openrouteservice/graphs_old
> mkdir /opt/openrouteservice/graphs
```

4. 将新的图移动到 `/opt/openrouteservice/graphs/` 目录。新的图文件夹结构应类似于：

```shell
/opt/openrouteservice/graphs/
├── driving-car
└── cycling-regular
```

5. 为 Tomcat 目录[应用正确的权限](#set-the-necessary-permissions-for-the-tomcat-10-user)
   并重启 Tomcat 服务。
6. 尝试[示例请求](#example-requests)以查看新的图是否正常工作。

## 可选：将 openrouteservice 更新到新版本

如果你想将 openrouteservice 更新到新版本，可以按照以下步骤操作：

1. 停止 Tomcat 服务。

```shell
> sudo systemctl stop openrouteservice.service
```

2. 清空旧的 webapps 目录 `/opt/tomcat/webapps/`，并将新的 WAR 文件移动到该目录。
3. 下载新版本的[新 WAR 文件](#download-the-openrouteservice-war-file)。
4. 在 `setenv.sh` 文件中调整 [openrouteservice 配置](#configure-openrouteservice)。
5. 为 Tomcat 目录[应用正确的权限](#set-the-necessary-permissions-for-the-tomcat-10-user)
   并重启 Tomcat 服务。
6. 尝试[示例请求](#example-requests)以查看新版本是否正常工作。
