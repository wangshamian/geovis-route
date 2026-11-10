# 以 WAR 方式运行 openrouteservice

与旧版本一样，ORS 版本 8 仍然可以构建并作为 Web org.heigit.ors.api.Application Resource（即 Web Archive，WAR）运行。
WAR 文件可以部署到诸如 Tomcat 之类的 Servlet 容器中，以服务方式运行。
要运行 openrouteservice，你还需要一个 OSM 数据文件，例如来自 [Geofabrik](http://download.geofabrik.de) 的文件。更多详细信息请参阅[数据](data.md)章节。

## 前置条件

* 应已安装 [Tomcat](https://tomcat.apache.org/) 10（或更高版本）。例如在 Ubuntu 22.04 上，请按照[这些说明](https://linuxize.com/post/how-to-install-tomcat-10-on-ubuntu-22-04/)操作。

要运行 openrouteservice，你还需要一个 OSM 数据文件，例如来自 [Geofabrik](http://download.geofabrik.de) 的文件。更多详细信息请参阅[数据](data.md)章节。

## 下载

从版本 8 开始，你可以从 GitHub [releases](https://github.com/GIScience/openrouteservice/releases) 页面中所需版本的“Assets”部分下载可直接使用的 WAR。

## 构建

如何构建与你想要使用的产物类型无关，相关文档见[从源码构建](building-from-source.md)。

## 运行

运行 WAR 文件意味着将其部署到 Tomcat 实例中。要让 openrouteservice 启动并运行，只需将 `ors.war` 文件复制到 Tomcat 的 webapps 文件夹中。Tomcat 现在应会自动检测到新的 WAR 文件并部署该服务。

## 配置

在 Tomcat 中部署 openrouteservice 时，有一些重要差异需要注意：
- 向 openrouteservice 传递指令的推荐方式是使用环境变量。你需要将其添加到 tomcat bin 文件夹中的 `setenv.sh` 文件里，该文件夹通常位于类似 `/usr/share/tomcat10/bin/` 的位置。如果该文件不存在，你可以创建它。
- 如果你向 `setenv.sh` 文件添加了新设置，则需要使用类似 `sudo systemctl restart tomcat.service` 的命令重启 Tomcat 才能使其生效。
- 你的配置文件以及该配置引用的所有输入/输出文件和目录，必须对 Tomcat 实例所运行的用户可访问（对于输出文件夹则必须可写）。你可能需要调整上述文件和文件夹的位置，或相应地对其执行 `chmod` / `chown`。

配置 openrouteservice 实例的推荐方式是使用 YAML 配置文件。你可以使用以下命令下载示例文件：

```shell 
wget https://raw.githubusercontent.com/GIScience/openrouteservice/main/gr-config.yml
```

然后你需要在 `setenv.sh` 中添加一个指向该配置文件的环境变量：

```shell
GR_CONFIG_LOCATION=/path/to/gr-config.yml
```

有关如何让 openrouteservice 应用配置文件中的设置（有多种选项）的详细信息，请参阅[配置](configuration/index.md)章节。
