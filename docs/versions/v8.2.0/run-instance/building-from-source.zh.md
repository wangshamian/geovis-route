# 从源码构建

如果你需要比[配置](configuration/index.md)所能提供的更进一步地自定义 openrouteservice 实例，可能需要修改代码。
如果你实现的功能可能对其他人也有用，请考虑[参与贡献](../contributing/index.md)！以下说明有助于你完成设置，开始修改代码。


## 前置条件

以下文档假设你运行的是 Ubuntu 20.04 系统（通常也适用于更新的 Ubuntu 版本）。根据你的环境，可能需要调整某些细节。你还需要确保已安装以下内容：
* [java](https://www.java.com/en/) 17（或更高版本）应可用，最好作为默认的 Java 环境。
* 你的系统上应已安装 [maven](https://maven.apache.org/)。
* 如果你想方便地下载源代码，你的系统上应可用 [git](https://github.com/git-guides/install-git)。

## 下载源代码

如果你已安装 git，下载源代码最简单的方法是使用以下命令克隆仓库：

```shell
git clone https://github.com/GIScience/openrouteservice.git
cd openrouteservice
```

这会创建一个名为 `openrouteservice` 的目录，其中包含下载的源代码。以下所有说明都假设你位于该目录中。

如果你的系统上没有安装 git，你也可以从 GitHub [releases](https://github.com/GIScience/openrouteservice/releases) 页面中所需版本的“Assets”部分下载打包的（`.zip` 和 `.tar.gz`）源代码文件。解压归档文件，并在解压源代码的目录中执行以下说明。

## 直接运行源代码

你应该能够直接使用以下命令运行应用程序

```shell
mvn spring-boot:run
```

或在你的 IDE 中运行（见下文）。这将以项目根目录中的默认配置 `gr-config.yml` 启动 openrouteservice，端口为 `8082`，
并使用来自海德堡的小型 OSM 数据集。

在[配置](configuration/index.md)章节中，你可以找到使用自定义配置的选项。


### 在 IDE 中运行

[//]: # (TODO: review below here)

要在 IDE 中运行该项目，你需要：

1. 打开 IntelliJ，通过 File -> New -> Project from Existing Sources 创建一个项目
2. 从克隆的 "openrouteservice" 文件夹中选择 "pom.xml" 文件并点击 "Open"
3. 点击 "Next" 逐步完成项目设置，直到进入选择项目 SDK 的页面。
4. 选择 "17" 作为项目 SDK，然后点击 "Next"
5. 在最后一个窗口中点击 "Finish" 完成项目导入。
6. 将你的 IDE 配置为以 maven 目标 `spring-boot:run` 运行。现在你就可以直接在 IntelliJ 中运行应用程序了。
7. 若要使用项目目录中 `gr-config.yml` 之外的配置文件，可以在运行配置中设置环境变量 `GR_CONFIG_LOCATION=<config>`。
8. 你可以通过 JUnit 运行所有测试。


### 运行测试

如果你修改了代码，运行测试是必不可少的。请始终确保所有测试都通过。测试失败有时表明代码修改破坏了已有代码。如果应用程序的预期行为发生变化，可能也需要修改现有测试。对于新功能，应添加新测试。

在 IDE 中运行单元测试（全部或仅一个或几个）非常方便。
你也可以在命令行中通过 maven 运行测试。以下是一些示例，更多选项请查阅 maven 文档。

```shell
mvn clean test                   # runs unit tests in all modules
mvn clean test -pl :ors-api      # runs unit tests in ors-api
mvn clean test -pl :ors-api -Dtest="APIEnumsTest" # run tests in a single test class
mvn clean test -pl :ors-api -Dtest="APIEnumsTest#testLanguagesEnumCreation" # or a single test method only
```

api 测试（位于 `ors-api/src/test/java/org/heigit/ors/apitests`）默认被排除。
要包含它们，请添加选项 `-Papitests`，例如：

```shell
mvn clean verify -Papitests
```

如果你想在不运行测试的情况下执行 maven 任务，请添加选项 `-DskipTests`，例如：

```shell
mvn clean package -DskipTests
```


### 集成 GraphHopper

如果你需要对我们分叉并修改过的 [GraphHopper 仓库](https://github.com/GIScience/graphhopper)进行调整，请按照以下步骤操作：

1. 克隆并检出 `ors_4.0` 分支：

   ```shell
   git clone https://github.com/GIScience/graphhopper.git
   cd graphhopper
   git checkout ors_4.0
   ```

2. 构建项目以创建本地快照。

   ```shell
    mvn package
   ```

3. 修改 `ors-engine/pom.xml`：

   ```xml
    <!-- remove the comment to enable debugging // [!code ++]
    <dependency>
        <groupId>com.github.GIScience.graphhopper</groupId>
        <artifactId>graphhopper-core</artifactId>
        <version>v4.9.1</version>
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
        <version>v4.9.1</version>
        <exclusions>
            <exclusion>
                <groupId>com.fasterxml.jackson.dataformat</groupId>
                <artifactId>jackson-dataformat-xml</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <dependency>
        <groupId>com.github.GIScience.graphhopper</groupId>
        <artifactId>graphhopper-map-matching</artifactId>
        <version>v4.9.1</version>
        <exclusions>
            <exclusion>
                <groupId>com.fasterxml.jackson.dataformat</groupId>
                <artifactId>jackson-dataformat-xml</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- remove the comment to enable debugging // [!code --]
    --> // [!code ++]
    <dependency>
        <groupId>com.github.GIScience.graphhopper</groupId>
        <artifactId>graphhopper-core</artifactId>
        <version>4.9-SNAPSHOT</version>
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
        <version>4.9-SNAPSHOT</version>
        <exclusions>
            <exclusion>
                <groupId>com.fasterxml.jackson.dataformat</groupId>
                <artifactId>jackson-dataformat-xml</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <dependency>
        <groupId>com.github.GIScience.graphhopper</groupId>
        <artifactId>graphhopper-map-matching</artifactId>
        <version>4.9-SNAPSHOT</version>
        <exclusions>
            <exclusion>
                <groupId>com.fasterxml.jackson.dataformat</groupId>
                <artifactId>jackson-dataformat-xml</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    --> // [!code --]
   ```

4. 测试你的新功能，并在将功能分支与最新的 `main` 分支变基（rebase）之后运行所有测试。如有必要，调整测试。

5. 如果一切顺利，分别为 [openrouteservice](https://github.com/GIScience/openrouteservice/pulls) 和 [GraphHopper](https://github.com/GIScience/graphhopper/pulls) 向 `master` 和 `ors_4.0` 分支创建 PR。


## 构建可运行产物

### 构建 JAR

源代码设置完成后，你可以生成可运行的 openrouteservice fat JAR：

```shell
mvn clean package -PbuildFatJar
```

由于 JAR 是默认选项，你也可以不带 `-PbuildFatJar` 运行该命令：

```shell
mvn clean package
```

你将在 `ors-api/target/ors.jar` 中找到 fat JAR 文件

[JAR](running-jar.md) 产物章节说明了如何配置和运行该 JAR 文件。


### 构建 WAR

源代码设置完成后，你可以生成可部署的 openrouteservice WAR：

```shell
mvn clean package -PbuildWar
```

你将在 `ors-api/target/ors.war` 中找到 WAR 文件

[WAR](running-war.md) 产物章节说明了如何配置和部署该 WAR 文件。


### 构建 docker 镜像

::: tip
本章仅介绍如何在本地_构建_一个 docker 容器。
在你第一次_运行_自定义 docker 镜像之前，
请阅读[运行预构建镜像](running-with-docker.md#running-prebuilt-images)
以了解前置条件以及容器化 openrouteservice 的运行方式。
:::

要基于本地（已修改的）代码构建本地 openrouteservice docker 镜像，
使用项目目录中的 docker compose 文件会很方便。
当你位于项目目录并第一次运行

```shell
docker compose up 
```

时，你的本地 docker 镜像 `local/openrouteservice:latest` 将被构建。如果该镜像已存在，则会使用现有镜像。

要使用 docker compose 重新构建镜像，你可以执行

```shell
docker compose build 
```

或者当然也可以直接使用 docker 构建镜像：

```shell
docker build . -t local/openrouteservice
```

在本地构建 docker 镜像时，底层会执行 `mvn clean package`，
它会把你的本地源代码编译成一个 fat jar，并放入你的 docker 镜像中。

如果你不想在本地构建，而是想运行 nightly 镜像，
请修改 `docker-compose.yml`：

```yaml
image: local/openrouteservice:latest // [!code --]
image: openrouteservice/openrouteservice:nightly // [!code ++]
```
