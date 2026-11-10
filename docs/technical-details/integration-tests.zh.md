# Openrouteservice 集成测试

::: warning
集成测试相当耗时，是 openrouteservice 中的高级主题。
只有在必要时、并且作为活跃开发工作的一部分，才应运行它们。
:::

集成测试位于 `ors-test-scenarios/` 模块中。
这些测试针对正在运行的 ORS 实例执行一组预定义的场景。
测试使用 Java 编写，并使用 JUnit 与 testcontainers 框架。

## 前置条件

运行这些测试需要以下前置条件：

- Docker
- Maven
- Java 17+

## 概述

你可以像运行其他任何单元测试一样运行这些测试。
这些测试在资源和执行时间上的开销相当大。
为避免意外执行，测试未包含在默认的测试执行中。
在 maven 命令中加入 `-P integrationTests` 即可运行这些测试。

重要内容与说明：

```shell
ors-test-scenarios/
├── ors-test-scenarios/graphs-integrationtests/
│   └── ors-test-scenarios/graphs-integrationtests/sharedGraphMount/ # Contains the shared graph mount for the tests to avoid rebuilding the graph. Can be deleted.
├── ors-test-scenarios/pom.xml # The pom file for the integration tests.
└── ors-test-scenarios/src/
    └── ors-test-scenarios/src/test/
        ├── ors-test-scenarios/src/test/java/
        │   ├── ors-test-scenarios/src/test/java/integrationtests/ # Contains the integration tests.
        │   └── ors-test-scenarios/src/test/java/utils/ # Contains the helper classes for the tests.
        └── ors-test-scenarios/src/test/resources/
            ├── ors-test-scenarios/src/test/resources/Builder.Dockerfile # The Dockerfile to create the builder images.
            ├── ors-test-scenarios/src/test/resources/war.Dockerfile # The Dockerfile to create war test images.
            ├── ors-test-scenarios/src/test/resources/jar.Dockerfile # The Dockerfile to create jar test images.
            ├── ors-test-scenarios/src/test/resources/maven.Dockerfile # The Dockerfile to create maven test images.
            ├── ors-test-scenarios/src/test/resources/junit-platform.properties # The config file for the JUnit platform.
            ├── ors-test-scenarios/src/test/resources/logback.xml # Testcontaieners usese logback
            └── ors-test-scenarios/src/test/resources/maven-entrypoint.sh # The entrypoint for the maven test image.
```

## 最重要的示例：

```shell
# Execute all tests from the repo root with testcontainers.
./mvnw -pl ors-test-scenarios test -P integrationTests  
# Execute only war tests
./mvnw -pl ors-test-scenarios test -P integrationTests -Dcontainer.run.scenario=war
```

## 高级测试运行

测试模块提供了多个属性，用于自定义测试执行：

- `container.run.scenario` - 要针对其运行测试的场景。选项有：
    - `maven`,
    - `jar`,
    - `war`,
    - `all`（默认）。
- `container.builder.use_prebuild` - 使用预构建的构建器镜像。默认值为 `false`。
- `container.run.share_graphs` - 在测试之间共享图。默认值为 `true`。**只有在你清楚自己在做什么时才应更改此项。**

`container.builder.use_prebuild` 属性在你想要使用自定义预构建构建器镜像时很有用。
Testcontainers 使用 Docker 为测试构建镜像。
如有必要，可以向测试中注入自定义构建器镜像。
构建器镜像可以用 `buildx` 构建并推送到本地 registry。
这使用户能够借助缓存进行构建，或总体上更高效、更快速地构建。
这主要用于各种 workflow，或用于重量级的本地测试。

###### 示例：

使用由 `testcontainers` 控制构建器镜像并共享图的配置，针对 war/tomcat 部署运行测试。

```shell
./mvnw -pl ors-test-scenarios test -P integrationTests -Dcontainer.run.scenario=war
```

使用自定义构建器镜像和 maven 部署构建并运行测试

```shell
docker buildx build --target ors-test-scenarios-maven-builder -t ors-test-scenarios-maven-builder:latest -f ors-test-scenarios/src/test/resources/Builder.Dockerfile .
./mvnw -pl ors-test-scenarios test -P integrationTests \
-Dcontainer.run.scenario=maven \
-Dcontainer.builder.use_prebuild=true
```

## 预构建的构建器镜像

用于测试场景实例的 dockerfile（`{war,jar,maven}.Dockerfile`）与构建器 dockerfile
`Bulid.Dockerfile` 是分开的。
有时这也是必要的，因为 testcontainers 自有的构建器镜像缓存策略对 CI 而言并不总是最优的。

面向场景的 dockerfile `war.Dockerfile`、`jar.Dockerfile` 和 `maven.Dockerfile` 只用于最后的
阶段。
为了在不使用 testcontainers 的情况下构建它，必须先执行 `Build.Dockerfile`。

从以下目标中选择一个：

- ors-test-scenarios-maven-builder
- ors-test-scenarios-jar-builder
- ors-test-scenarios-war-builder

###### 示例：

使用自定义构建器镜像执行 jar 测试场景：

```shell
# Choose from: jar, war, maven.
export TEST_SCENARIO=jar
# Execute the jar setup with a custom builder image
docker buildx build --target ors-test-scenarios-${TEST_SCENARIO}-builder -t ors-test-scenarios-${TEST_SCENARIO}-builder:latest -f ors-test-scenarios/src/test/resources/Builder.Dockerfile .
./mvnw -pl ors-test-scenarios test -P integrationTests \
  -Dcontainer.run.scenario=${TEST_SCENARIO} \
  -Dcontainer.builder.use_prebuild=true
```

使用各自的自定义构建器镜像执行所有场景和测试：

```shell
# Execute all scenarios and tests with a custom builder image
docker buildx build --target ors-test-scenarios-jar-builder -t ors-test-scenarios-jar-builder:latest -f ors-test-scenarios/src/test/resources/Builder.Dockerfile .
docker buildx build --target ors-test-scenarios-war-builder -t ors-test-scenarios-war-builder:latest -f ors-test-scenarios/src/test/resources/Builder.Dockerfile .
docker buildx build --target ors-test-scenarios-maven-builder -t ors-test-scenarios-maven-builder:latest -f ors-test-scenarios/src/test/resources/Builder.Dockerfile .

./mvnw -pl ors-test-scenarios test -P integrationTests \
  -Dcontainer.run.scenario=all \
  -Dcontainer.builder.use_prebuild=true
```

## 搭建本地测试容器

有时需要一个可复现的测试环境容器来试验各种功能。
每个测试场景都可以在容器中运行。可用的容器如下：

- `ors-test-scenarios-maven`：启动一套功能完整的 ORS maven 部署。
- `ors-test-scenarios-jar`：启动一套功能完整的 ORS jar 部署。
- `ors-test-scenarios-war`：启动一套功能完整的 ORS `war/tomcat` 部署。

###### 示例：

```shell
# Fully functional ORS war/tomcat setup
# Build the custom builder image
export TEST_SCENARIO=war
docker buildx build --target ors-test-scenarios-${TEST_SCENARIO}-builder -t ors-test-scenarios-${TEST_SCENARIO}-builder:latest -f ors-test-scenarios/src/test/resources/Builder.Dockerfile .
docker buildx build --target ors-test-scenarios-${TEST_SCENARIO} -t ors-test-scenarios-${TEST_SCENARIO}:latest -f ors-test-scenarios/src/test/resources/${TEST_SCENARIO}.Dockerfile .
docker run -it --rm --name ors-test-scenarios-${TEST_SCENARIO} -p 8080:8080 ors-test-scenarios-${TEST_SCENARIO}
```

使用**自定义 PBF 文件**运行 ORS war 部署，**缓存图**并**启用 hgv 配置**：

```shell
# Basic builder image setup.
export TEST_SCENARIO=war
docker buildx build --target ors-test-scenarios-${TEST_SCENARIO}-builder -t ors-test-scenarios-${TEST_SCENARIO}-builder:latest -f ors-test-scenarios/src/test/resources/Builder.Dockerfile .
docker buildx build --target ors-test-scenarios-${TEST_SCENARIO} -t ors-test-scenarios-${TEST_SCENARIO}:latest -f ors-test-scenarios/src/test/resources/${TEST_SCENARIO}.Dockerfile .
# Run the container with the custom war file and cache the graphs
docker run --rm -it --name ors-test-scenarios-${TEST_SCENARIO} -p 8080:8080 \
  -v ./ors-api/src/test/files/heidelberg.test.pbf:/home/ors/openrouteservice/files/heidelberg.test.pbf \
  -v ./your-local-graph-${TEST_SCENARIO}:/home/ors/openrouteservice/graphs \
  -e "ors.engine.profiles.driving-hgv.enabled=true" \
  ors-test-scenarios-${TEST_SCENARIO}
```

旁加载自定义 war 文件用于测试：

```shell
# Basic builder image setup.
docker buildx build --target ors-test-scenarios-war-builder -t ors-test-scenarios-war-builder:latest -f ors-test-scenarios/src/test/resources/Builder.Dockerfile .
docker buildx build --target ors-test-scenarios-war -t ors-test-scenarios-war:latest -f ors-test-scenarios/src/test/resources/war.Dockerfile .

# Build the custom war file
./mvnw clean package -DskipTests -PbuildWar

# Run the container with the custom war file
docker run --rm -it --name ors-test-scenarios-war -p 8080:8080 \
  -v ./your-local-graph-war:/home/ors/openrouteservice/graphs \
  -v ./ors-api/target/ors.war:/usr/local/tomcat/webapps/ors.war \
  ors-test-scenarios-war
```

旁加载自定义 jar 文件用于测试：

```shell
# Basic builder image setup.
docker buildx build --target ors-test-scenarios-jar-builder -t ors-test-scenarios-jar-builder:latest -f ors-test-scenarios/src/test/resources/Builder.Dockerfile .
docker buildx build --target ors-test-scenarios-jar -t ors-test-scenarios-jar:latest -f ors-test-scenarios/src/test/resources/jar.Dockerfile .

# Build the custom jar file
./mvnw clean package -DskipTests

# Run the container with the custom jar file
docker run --rm -it --name ors-test-scenarios-jar -p 8080:8080 \
  -v ./your-local-graph-jar:/home/ors/openrouteservice/graphs \
  -v ./ors-api/target/ors.jar:/home/ors/openrouteservice/ors.jar \
  ors-test-scenarios-jar
```

旁加载自定义 maven 部署用于测试：

```shell
# Basic builder image setup.
docker buildx build --target ors-test-scenarios-maven-builder -t ors-test-scenarios-maven-builder:latest -f ors-test-scenarios/src/test/resources/Builder.Dockerfile .
docker buildx build --target ors-test-scenarios-maven -t ors-test-scenarios-maven:latest -f ors-test-scenarios/src/test/resources/maven.Dockerfile .

./mvnw clean package -DskipTests

# Run the container with the custom maven setup
docker run --rm -it --name ors-test-scenarios-maven -p 8080:8080 \
  -v ./your-local-graph-maven:/home/ors/openrouteservice/graphs \
  -v ./ors-api:/home/ors/openrouteservice/ors-api \
  -v ./ors-engine:/home/ors/openrouteservice/ors-engine \
  ors-test-scenarios-maven
```
