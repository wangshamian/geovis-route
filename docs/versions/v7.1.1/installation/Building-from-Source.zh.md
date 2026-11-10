# 从源码构建

**我们建议使用 Docker 容器运行 openrouteservice（参见[使用 Docker 运行](Running-with-Docker)）**

## 从源码安装

如果你需要在没有 Docker 的情况下安装，在 Ubuntu 20.04 系统上（通常也适用于更新的 Ubuntu 版本）可以按以下步骤操作：

  1. （Fork 并）将 openrouteservice 仓库克隆到你的机器上。
      ```bash
      git clone https://github.com/user/openrouteservice.git
      cd openrouteservice
      ```

  2. 确保已将 Java 17 设为默认的 Java 环境。
  3. 确保已安装 Maven。
  4. 在机器上下载/创建 OpenStreetMap pbf 文件。
  5. 将 `ors-api/src/main/resources/gr-config-sample.json` 文件复制到
     相同位置，并重命名为 `gr-config.json`。
  6. 更新 `gr-config.json` 文件，以反映各项设置、你希望运行的 profile，
     以及各文件的位置，特别是将要使用的 OSM 文件的源位置
     以及扩展存储所需的附加文件。你应确保这些文件夹/文件
     可被服务访问，例如使用 `sudo chmod -R 777
     [path to folder]` 命令。
     有关文件格式和参数的说明可在[此处](Configuration)找到
  7. 在 `openrouteservice` 根目录下运行命令 `mvn package`。这会构建
     openrouteservice，使其可供 tomcat 部署。
  8. 若要同时运行单元测试和 api 测试，请给 `mvn` 添加 `-Papitests` 参数。
     ```
     mvn -Papitests verify
     ```
     请注意，除非删除 `ors-engine/graphs-apitests` 文件夹，
     否则测试用的图不会被重新构建。

在打包好 openrouteservice 之后，有两种运行方式。
一种是运行 `mvn spring-boot:run` 命令，这会启动一个运行在 `8082` 端口上的
spring-boot 原生 Tomcat 实例。这种方式在 Tomcat 设置方面限制更多。另一种是安装并运行 Tomcat 8

## 在 IDE 中运行

要在 IDE 中运行该项目，你需要：

  1. 设置好 IDE 项目，并以 Maven 模型的方式导入 `openrouteservice`
     模块。
     对 IntelliJ Idea，可参阅[这些说明](Opening-Project-in-IntelliJ)。

  2. 将 IDE 配置为以 maven goal 的方式运行 `spring-boot:run`，并设置
     环境变量 `ORS_CONFIG=gr-config-test.json`。

  3. 你可以通过 JUnit 运行所有测试。

## 安装并运行 tomcat8

  1. 使用 `sudo apt-get install tomcat8` 安装 Tomcat 8。

     请注意，它在你发行版的最新软件源中可能已不可用。
     这种情况下，请将以下行添加到你的 `/etc/apt/sources.list`：
     ```
     debian:  deb http://ftp.de.debian.org/debian/ stretch main
              deb http://security.debian.org/ stretch/updates main

     ubuntu:  deb http://de.archive.ubuntu.com/ubuntu bionic main universe
              deb http://de.archive.ubuntu.com/ubuntu bionic-security main universe
     ```
     更多细节请访问 [debian wiki](https://wiki.debian.org/SourcesList) 了解 `sources.list` 格式。

  2. 如果你想使用默认值以外的系统设置（即 Java 堆大小），
     则需要把这些设置添加到
     `/usr/share/tomcat8/bin/setenv.sh` 文件。如果该文件不存在，
     你可以创建它。我们服务器上通常使用的设置大致如下：

     ```bash
     JAVA_OPTS="-server -XX:TargetSurvivorRatio=75 -XX:SurvivorRatio=64 -XX:MaxTenuringThreshold=3 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:ParallelGCThreads=4 -Xms105g -Xmx105g -XX:MaxMetaspaceSize=50m"
     CATALINA_OPTS="(here we set settings for JMX monitoring)"
     ```

  3. 如果你把这些新设置添加到 `setenv.sh` 文件，则需使用
     `sudo systemctl restart
     tomcat8.service` 重启 Tomcat 使它们生效。
  4. 要让 openrouteservice 启动并运行，请把 `ors-api/target` 文件夹中的
     `ors.war` 文件复制到 Tomcat 的 webapps 文件夹。例如

     ```bash
     sudo cp ~/openrouteservice/ors-api/target/ors.war /var/lib/tomcat8/webapps/
     ```

  5. 现在 Tomcat 应会自动检测到新的 WAR 文件并部署
     该服务。视 profile 和 OSM 数据的大小而定，
     openrouteservice 构建图并准备好生成路线可能需要
     一些时间。你可以通过访问
     `http://localhost:8080/ors/health` 检查它是否就绪（如果你
     安装 Tomcat 的方式与上述不同，端口和 URL 可能有所差异）。如果看到 `status: ready`
     消息，就说明你可以开始创建路线了。

`gr-config.json` 中有许多设置高度依赖
你的具体情况，但其中许多[已有文档说明](Configuration)。不过作为参考，
你可以查看 `ors-api/src/main/resources` 文件夹中的
`gr-config-sample.json` 文件。如果你遇到内存溢出或类似问题，
则需要相应地调整 java/tomcat 设置。

## 集成 GraphHopper

如果你需要对我们 fork 并修改过的 [GraphHopper
仓库](https://github.com/GIScience/graphhopper)做调整，请按以下步骤操作：

1. 克隆并检出 `ors_4.0`：

   ```bash
   git clone https://github.com/GIScience/graphhopper.git
   cd graphhopper
   git checkout ors_4.0
   ```

2. 构建项目以创建本地快照。

3. 修改 `ors-engine/pom.xml`：

   ```xml
   <!--
   <dependency>
       <groupId>com.github.GIScience.graphhopper</groupId>
       <artifactId>graphhopper-core</artifactId>
       <version>v4.5.2</version>
       <exclusions>
           <exclusion>
               <groupId>com.fasterxml.jackson.dataformat</groupId>
               <artifactId>jackson-dataformat-xml</artifactId>
           </exclusion>
       </exclusions>
   </dependency>

   <dependency>
   <groupId>com.github.GIScience.graphhopper</groupId>
   <artifactId>graphhopper-reader-gtfs</artifactId>
   <version>v4.5.2</version>
   <exclusions>
       <exclusion>
           <groupId>com.fasterxml.jackson.dataformat</groupId>
           <artifactId>jackson-dataformat-xml</artifactId>
       </exclusion>
   </exclusions>
   </dependency>

   <dependency>
   <groupId>com.github.GIScience.graphhopper</groupId>
   <artifactId>graphhopper-web-api</artifactId>
   <version>v4.5.2</version>
   <exclusions>
       <exclusion>
           <groupId>com.fasterxml.jackson.dataformat</groupId>
           <artifactId>jackson-dataformat-xml</artifactId>
       </exclusion>
   </exclusions>
   </dependency>
   -->

   <dependency>
   <groupId>com.graphhopper</groupId>
   <artifactId>graphhopper-core</artifactId>
   <version>4.0-SNAPSHOT</version>
   <exclusions>
       <exclusion>
           <groupId>com.fasterxml.jackson.dataformat</groupId>
           <artifactId>jackson-dataformat-xml</artifactId>
       </exclusion>
   </exclusions>
   </dependency>

   <dependency>
   <groupId>com.graphhopper</groupId>
   <artifactId>graphhopper-web-api</artifactId>
   <version>4.0-SNAPSHOT</version>
   <exclusions>
       <exclusion>
           <groupId>com.fasterxml.jackson.dataformat</groupId>
           <artifactId>jackson-dataformat-xml</artifactId>
       </exclusion>
   </exclusions>
   </dependency>

   <dependency>
   <groupId>com.graphhopper</groupId>
   <artifactId>graphhopper-reader-gtfs</artifactId>
   <version>4.0-SNAPSHOT</version>
   <exclusions>
       <exclusion>
           <groupId>com.fasterxml.jackson.dataformat</groupId>
           <artifactId>jackson-dataformat-xml</artifactId>
       </exclusion>
   </exclusions>
   </dependency>
   ```

4. 将你的功能分支与最新的 `development` 分支变基后，
   测试你的新功能并运行所有测试。如有必要，调整测试

5. 如果一切成功，请分别为
   [openrouteservice](https://github.com/GIScience/openrouteservice/pulls) 和
   [GraphHopper](https://github.com/GIScience/graphhopper/pulls) 向 `master`
   和 `ors_4.0` 分支创建 PR。

**请注意，上例中使用的是 GH 的 4.x 版本——你应根据自己的具体版本做出调整。
要了解该使用哪个版本，请查看
[ors-engine 模块 pom 文件](https://github.com/GIScience/openrouteservice/ors-engine/pom.xml)
，看看 `com.github.GIScience.graphhopper`
依赖使用的是哪个版本**
