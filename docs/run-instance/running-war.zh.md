# 以 WAR 方式运行 openrouteservice

与旧版本一样，openrouteservice 版本 8 仍然可以构建并作为 Web org.heigit.ors.api.Application Resource（即 Web Archive，WAR）运行。
WAR 文件可以部署到以服务方式运行的 Servlet 容器（如 Tomcat）中。
要运行 openrouteservice，你还需要一个 OSM 数据文件，例如来自 [Geofabrik](http://download.geofabrik.de) 的文件。更多详细信息，请参见[数据](data.zh.md)章节。

## 前提条件

* 应安装 [Tomcat](https://tomcat.apache.org/) 10（或更高版本）。例如在 Ubuntu 22.04 上，请按照这些[说明](https://linuxize.com/post/how-to-install-tomcat-10-on-ubuntu-22-04/)操作。

要运行 openrouteservice，你还需要一个 OSM 数据文件，例如来自 [Geofabrik](http://download.geofabrik.de) 的文件。更多详细信息，请参见[数据](data.zh.md)章节。

## 下载

从版本 8 开始，你可以从我们的 GitHub [releases](https://github.com/GIScience/openrouteservice/releases) 页面中所需发布版本的“Assets”部分下载可直接使用的 WAR。

## 构建

如何构建与你想要使用的产物类型无关，相关内容记录在[从源代码构建](building-from-source.zh.md)中。

## 运行

运行 WAR 文件意味着将其部署到 Tomcat 实例中。要让 openrouteservice 启动并运行，只需将 `ors.war` 文件复制到 Tomcat 的 webapps 文件夹。Tomcat 现在应会自动检测到新的 WAR 文件并部署该服务。

## 配置

在 Tomcat 中部署 openrouteservice 时，有一些重要差异需要注意：
- 向 openrouteservice 传递配置指令的推荐方式是使用环境变量。你需要将这些变量添加到 tomcat bin 文件夹（通常类似 `/usr/share/tomcat10/bin/`）中的 `setenv.sh` 文件里。如果该文件不存在，你可以创建它。
- 如果你在 `setenv.sh` 文件中添加了新设置，则需要使用类似 `sudo systemctl restart tomcat.service` 的命令重启 Tomcat 才能使其生效。
- 你的配置文件以及该配置所引用的所有输入/输出文件和目录，都必须对 Tomcat 实例运行时所使用的用户可访问（对于输出文件夹，还必须可写）。你可能需要调整这些文件和文件夹的位置，或相应地对其执行 `chmod` / `chown`。

openrouteservice 实例推荐的配置方式是使用 YAML 配置文件。你可以使用以下命令下载示例文件：

```shell 
wget https://raw.githubusercontent.com/GIScience/openrouteservice/main/gr-config.yml
```

然后，你需要在 `setenv.sh` 中添加一个指向该配置文件的环境变量：

```shell
GR_CONFIG_LOCATION=/path/to/gr-config.yml
```

有关如何让 openrouteservice 应用配置文件中的设置（有多种方式）的详细信息，请参见[配置](configuration/index.zh.md)章节。
