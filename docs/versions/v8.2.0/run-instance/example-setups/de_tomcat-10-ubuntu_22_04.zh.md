# 在 Ubuntu 22.04 上使用 Tomcat 10 安装 ORS v8

本教程向你展示如何使用 Java 17 和 Tomcat 10 安装 openrouteservice v8。

::: info
本教程的[英文版](en_tomcat-10-ubuntu_22_04)。
:::

## 前置条件

- 已启用 Systemd 的 Ubuntu 22.04（Systemd 默认已启用）
- Tomcat 10
- Java 17 或更高版本

## 前提假设

- 假设你的系统上只运行一个 Tomcat 10 实例。
- 如果你想运行多个 Tomcat 服务，那么你是高级用户，应相应地调整
  这些说明。

## 准备 Tomcat 10 环境

以下步骤可帮助你准备 Tomcat 10 的环境。

### 安装 Java 17

Openrouteservice v8 需要 Java 17 或更高版本。
原因是引入了 Tomcat 10。
如果有更新的 Java 版本可用，你也可以使用。

```shell
# Aktualisiere den Paketindex
> sudo apt-get update
# Installiere Java 17 oder höher, curl und nano
> sudo apt-get install openjdk-17-jre-headless curl nano
```

- `openjdk-17-jre-headless` 是不带图形界面的 Java 17 运行时包。
  openrouteservice 不需要图形用户界面。
- `curl` 是一个用于从网络下载文件的命令行工具，用于下载某些资源。
- `nano` 是一个简单的文本编辑器，用于编辑文件。

列出可用的 Java 版本。

```shell
# Zeige verfügbare Optionen an und kopiere den Pfad zur Java-17-Installation
sudo update-alternatives --list java
```

输出应大致如下：

```shell
[...]
/usr/lib/jvm/java-17-openjdk-amd64/
[...]
```

将默认 Java 版本更新为 Java 17 或已安装的版本。

```shell
# Setze die Standard-Java-Version auf Java 17
> sudo update-alternatives --set java /usr/lib/jvm/java-17-openjdk-amd64/bin/java
```

::: info
**注意：** Java 17 安装路径可能因系统而异。 \
**注意：** 从现在起，Java 17 安装路径将被称为 `JAVA_HOME`。
:::

### 为 Tomcat 10 创建新用户

出于安全原因，Tomcat 10 服务应以单独的用户身份运行。
该用户不应有登录权限，也不应能执行命令。
我们将此用户称为 `tomcat`。

```shell
# Erstelle einen neuen Benutzer für Tomcat 10
> useradd -r -m -U -d /opt/tomcat -s /bin/false tomcat
```

- `-r` 创建系统用户。
- `-m` 为该用户创建主目录 `(/opt/tomcat)` 并设置必要的权限。
- `-U` 创建与用户同名的组。

### 下载并设置 Tomcat 10

```shell
# Setze die Tomcat-Version
> export TOMCAT_VERSION=10.1.33
# Lade das Tomcat-10-Tarball herunter
> curl -L https://dlcdn.apache.org/tomcat/tomcat-10/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz > apache-tomcat-$TOMCAT_VERSION.tar.gz
# Entpacke die heruntergeladene Datei
> tar -xf apache-tomcat-$TOMCAT_VERSION.tar.gz
# Kopiere den Inhalt des entpackten Verzeichnisses in das Verzeichnis /opt/tomcat
> cp -R apache-tomcat-$TOMCAT_VERSION/** /opt/tomcat
# Bereinige das entpackte Verzeichnis
> rm -r apache-tomcat-$TOMCAT_VERSION apache-tomcat-$TOMCAT_VERSION.tar.gz
```

### 为 Tomcat 10 用户设置必要的权限

以下命令为 tomcat 用户设置必要的权限。
它们必须以 root 用户身份执行，并且只要对 Tomcat 目录做了更改，
就应始终执行。

```shell
> sudo chown -R tomcat:tomcat /opt/tomcat
> sudo chmod -R 754 /opt/tomcat
```

- `chown -R tomcat:tomcat /opt/tomcat` 将 `/opt/tomcat` 目录的所有者和组设置为
  tomcat 用户。
- `chmod -R 754 /opt/tomcat` 设置 `/opt/tomcat` 目录的权限。
    - 7 授予所有者读、写和执行权限。
    - 5 授予组读和执行权限。
    - 4 授予其他人读权限。

### 为 Tomcat 10 创建 systemd 服务

创建一个 systemd 服务来管理 Tomcat 10。
这样 Systemd 就可以自动启动和停止 Tomcat 10 服务。
将以下内容粘贴到文件中并保存。

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
# Lade den Systemd-Daemon neu
> sudo systemctl daemon-reload
# Starte den Tomcat-10-Dienst
> sudo systemctl enable --now openrouteservice.service
# Überprüfe den Status des Tomcat-10-Dienstes
> sudo systemctl status openrouteservice.service
```

在浏览器中访问 [http://localhost:8080](http://localhost:8080)，以查看 Tomcat 10 欢迎页面。

## 准备 openrouteservice 环境

既然 Tomcat 10 已设置完成，我们也可以使用 Java 17 设置 openrouteservice。

### 下载 openrouteservice WAR 文件

要使用 Tomcat 10 设置 openrouteservice v8，你首先需要相应的 WAR 文件。请访问
[openrouteservice Releases 页面](https://github.com/GIScience/openrouteservice/releases)并下载最新
版本的 WAR 文件。

```shell
# Lade das neueste openrouteservice WAR-File herunter für v8.2.0.
> curl -L https://github.com/GIScience/openrouteservice/releases/download/v8.2.0/ors.war > ors.war
# Verschiebe das WAR-File in das Tomcat-Webapps-Verzeichnis
> mv ors.war /opt/tomcat/webapps/
# Starte den Tomcat-10-Dienst neu
> sudo systemctl restart openrouteservice.service
# Überprüfe den Status des Tomcat-10-Dienstes
> sudo systemctl status openrouteservice.service
```

访问 [http://localhost:8080/ors/v2/health](http://localhost:8080/ors/v2/health)，以查看 openrouteservice
的状态。
输出应如下所示：

```json
{
    "status": "not ready"
}

```

::: info
ORS 现已正确地与 Tomcat 10 一起设置完成，但仍需进行配置。因此它显示 `not ready`。
如果你想查看详细的日志输出，可以检查 `/opt/tomcat/logs` 目录中的 Tomcat 日志文件。
:::

### 创建 openrouteservice 文件夹结构

要让 openrouteservice 正常运行，
你必须设置文件夹结构、
下载一个测试 OSM 文件并进行配置。

```shell
mkdir -p "/opt/openrouteservice/graphs"
mkdir -p "/opt/openrouteservice/logs"
mkdir -p "/opt/openrouteservice/data"
mkdir -p "/opt/openrouteservice/elevation_cache"
```

::: info
该文件夹结构是我们的建议，可以根据你的需求进行调整。
只需确保相应地调整配置并正确设置权限。
:::

### 下载一个测试 OSM 文件

获取最新 OSM 文件的一个好来源是 [Geofabrik 下载服务器](https://download.geofabrik.de/)。
我们将下载一个安道尔的小型测试 OSM 文件。

```shell
# Lade eine Test-OSM-Datei in das Verzeichnis /opt/openrouteservice/data herunter
> curl -L https://download.geofabrik.de/europe/andorra-latest.osm.pbf > /opt/openrouteservice/data/andorra-latest.osm.pbf
```

### 配置 openrouteservice

Tomcat 和 openrouteservice 的配置最好通过在 Tomcat 的 `bin` 目录中的
`setenv.sh` 文件里设置配置来完成。

```shell
# Erstelle die Datei `setenv.sh` im `bin`-Verzeichnis von Tomcat
> sudo nano /opt/tomcat/bin/setenv.sh
```

将以下内容粘贴到文件中并保存。
请确保将 `-Xmx` 的值调整为适合你的系统以及所使用的 `OSM 文件`的大小。
如果你想构建另一个 `OSM 文件`，可以调整 source_file 的值。

如果你想了解更多关于版本 8 中新增配置选项的信息，请阅读
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
请注意 `-Xmx` 要调整为适合你的系统和图的值，并且你的系统有足够的内存。


## 运行 openrouteservice

如果你遵循了上述步骤，openrouteservice 现在应该已使用 Tomcat 10 和 Java 17 正确
设置完成。
每当你更改配置、文件夹结构或图时，以下步骤都是必需的：

```shell
# Setze die Besitzrechte für Tomcat erneut
> sudo chown -R tomcat:tomcat /opt/tomcat
> sudo chown -R tomcat:tomcat /opt/openrouteservice
> sudo chmod -R 754 /opt/tomcat/bin/setenv.sh
> sudo chmod -R 754 /opt/openrouteservice
# Starte den Tomcat-10-Dienst neu
> sudo systemctl restart openrouteservice.service
# Überprüfe den Status des Tomcat-10-Dienstes
> sudo systemctl status openrouteservice.service
```

现在 openrouteservice 已使用正确的配置正确设置完成，
请检查 `/opt/openrouteservice/logs/ors.log` 目录中的日志文件或 `/opt/tomcat/logs` 目录中的 Tomcat 日志文件。
在浏览器中访问 [http://localhost:8080/ors/v2/health](http://localhost:8080/ors/v2/health)，以查看
openrouteservice 的状态，或向 API 发送请求。

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

通过向 API 发送请求来测试 openrouteservice 是否正常工作。
请根据你下载的 OSM 文件调整测试。

```shell
# Teste eine Route von A nach B in Andorra
> curl -X POST \
  'http://localhost:8080/ors/v2/directions/driving-car' \
  -H 'Content-Type: application/json; charset=utf-8' \
  -H 'Accept: application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8' \
  -d '{"coordinates":[[1.5036892890930176, 42.4972256361276],[1.6298711299896242, 42.57831077271361]]}'

# Teste eine Isochrone von einem Punkt in Andorra
> curl -X POST \
  'http://localhost:8080/ors/v2/isochrones/driving-car' \
  -H 'Content-Type: application/json; charset=utf-8' \
  -H 'Accept: application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8' \
  -d '{"locations":[[1.5036892890930176, 42.4972256361276]],"range":[300]}'
```

## 可选：更新预构建的图

如果你只想用新的预制图替换现有的图，请按照以下步骤操作：

1. 从相应来源下载新的图。
   它们通常以 `zip` 或 `tar.xz` 文件形式提供。
2. 解压新的图并确保它们格式正确。

```shell
# Im Fall einer tar.xz-Datei
> tar -xvf new_graphs.tar.xz
# Im Fall einer ZIP-Datei kannst du den unzip-Befehl verwenden
> unzip new_graphs.zip
```

解压后的图具有类似如下的文件夹结构：

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

4. 将新的图移动到 `/opt/openrouteservice/graphs/` 目录。
   新的图文件夹结构应如下所示：

```shell
/opt/openrouteservice/graphs/
├── driving-car
└── cycling-regular
```

5. 为 Tomcat 目录设置[正确的权限](#setze-die-notwendigen-berechtigungen-fur-den-tomcat-10-benutzer)并重启
   Tomcat 服务。
6. 执行[示例请求](#beispielanfragen)，以检查新的图是否正常工作。

## 可选：更新 openrouteservice

如果你想将 openrouteservice 更新到新版本，可以执行以下步骤：

1. 停止 Tomcat 服务。

```shell
> sudo systemctl stop openrouteservice.service
```

2. 下载所需版本的[新 WAR 文件](#das-openrouteservice-war-file-herunterladen)。
3. 清空旧的 webapps 目录 `/opt/tomcat/webapps/`，并将新的 `WAR 文件`移动到该目录。
4. 在 `setenv.sh` 文件中调整 [openrouteservice 配置](#openrouteservice-konfigurieren)。
5. 为 Tomcat 目录[设置正确的权限](#setze-die-notwendigen-berechtigungen-fur-den-tomcat-10-benutzer)并重启
   Tomcat 服务。
6. 执行[示例请求](#beispielanfragen)，以检查新版本是否正常工作。
