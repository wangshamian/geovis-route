# 坐标生成工具

openrouteservice 提供了用于生成测试坐标的工具，可用于基准测试、测试和开发用途。这些工具使你能够创建与 openrouteservice API 兼容、并适用于不同路径规划配置的逼真坐标。

## 前置条件

要使用坐标生成工具，你需要：

- Java 17+
- Maven
- 一个运行中的 openrouteservice 实例（本地或远程）

## 概述

坐标生成工具是 `ors-benchmark` 模块的一部分，包括：

1. **Route Generator** - 创建适用于路径规划请求的起点-终点坐标对
2. **Snapping Generator** - 创建吸附到路网最近点的坐标

这些工具特别适用于：

- 创建用于性能基准测试的测试数据集
- 为负载测试生成逼真的坐标集
- 为演示或教程准备示例数据

## Route Generator

Route Generator 根据指定条件（例如距离约束和路径规划配置）创建适用于路径规划请求的坐标对（起点和终点）。

### Route Generator 工作原理

该工具在指定边界框内生成随机坐标，然后使用 openrouteservice API 验证每对坐标之间是否存在有效路线。这样可以确保生成的坐标可用于路线测试，并为指定的路径规划配置提供可用数据。

### Route Generator 选项

| 选项 | 说明 | 默认值 |
|--------|-------------|---------|
| `-n, --num-routes` | 要生成的路线数量。 | （必填） |
| `-e, --extent` | 边界框（minLon,minLat,maxLon,maxLat）。可使用 <https://boundingbox.klokantech.com/> 生成范围。 | （必填） |
| `-p, --profiles` | 以逗号分隔的路径规划配置。 | （必填） |
| `-u, --url` | ORS API 基础 URL。 | <http://localhost:8080/ors> |
| `-o, --output` | 输出 CSV 文件路径。 | route_coordinates.csv |
| `-d, --min-distance` | a 到 b 路径规划对中起点与终点的最小距离，单位为米。对所有配置均有效。 | 1 |
| `-m, --max-distances` | 起点与终点之间的最大距离，单位为米，**按配置**指定。 | （无） |
| `-t, --threads` | 要使用的线程数。 | 可用处理器数 |

### Route Generator 示例

#### 基本路线示例

为 driving-car 配置生成 100 条最小距离为 1000m 的路线：

```bash
./mvnw clean compile exec:java -Dexec.cleanupDaemonThreads=false -pl 'ors-benchmark' \
  -Dexec.mainClass="org.heigit.ors.coordinates_generator.RouteGeneratorApp" \
  -Dexec.args="\
  -n 100 \
  -e 8.6,49.3,8.7,49.4 \
  -p driving-car \
  -d 1000 \
  -u http://localhost:8080/ors \
  -o routes.csv"
```

#### 多种配置与不同距离约束

为 driving-car 和 cycling-regular 两种配置分别生成 50 条具有不同最大距离的路线：

```bash
./mvnw clean compile exec:java -Dexec.cleanupDaemonThreads=false -pl 'ors-benchmark' \
  -Dexec.mainClass="org.heigit.ors.coordinates_generator.RouteGeneratorApp" \
  -Dexec.args="\
  --num-routes 50 \
  --extent 8.681495,49.411721,8.695485,49.419365 \
  --profiles driving-car,cycling-regular \
  --min-distance 2000 \
  --max-distances 5000,3000 \
  --threads 4 \
  --url http://localhost:8080/ors \
  --output routes.csv"
```

在这个示例中，driving-car 路线的最大距离为 5000m，而 cycling-regular 路线的最大距离为 3000m。

## Snapping Generator

Snapping Generator 创建吸附到路网最近点的坐标。当你需要保证位于路网上的坐标时，这很有用。

### Snapping Generator 工作原理

该工具在指定边界框内生成随机坐标，然后使用 openrouteservice API 将这些点吸附到最近的路段。每个点都使用指定的路径规划配置进行吸附，确保这些坐标可用于该配置的路网。

### Snapping Generator 选项

| 选项 | 说明 | 默认值 |
|--------|-------------|---------|
| `-n, --num-points` | 每个配置要生成的点数。 | （必填） |
| `-e, --extent` | 边界框（minLon,minLat,maxLon,maxLat）。 | （必填） |
| `-p, --profiles` | 以逗号分隔的路径规划配置列表。 | （必填） |
| `-r, --radius` | 以米为单位的搜索半径。 | 350 |
| `-u, --url` | ORS API 基础 URL。 | <http://localhost:8080/ors> |
| `-o, --output` | 输出 CSV 文件路径。 | snapped_coordinates.csv |

### Snapping Generator 示例

#### 基本吸附示例

为 driving-car 配置生成 100 个搜索半径为 500m 的吸附点：

```bash
./mvnw clean compile exec:java -Dexec.cleanupDaemonThreads=false -pl 'ors-benchmark' \
  -Dexec.mainClass="org.heigit.ors.coordinates_generator.SnappingGeneratorApp" \
  -Dexec.args="\
  -n 100 \
  -e 8.6,49.3,8.7,49.4 \
  -p driving-car \
  -r 500 \
  -u http://localhost:8080/ors \
  -o snapped.csv"
```

#### 使用多种配置进行吸附

为 driving-car 和 cycling-regular 两种配置生成 50 个吸附点：

```bash
./mvnw clean compile exec:java -Dexec.cleanupDaemonThreads=false -pl 'ors-benchmark' \
  -Dexec.mainClass="org.heigit.ors.coordinates_generator.SnappingGeneratorApp" \
  -Dexec.args="\
  --num-points 50 \
  --extent 8.681495,49.411721,8.695485,49.419365 \
  --profiles driving-car,cycling-regular \
  --radius 250 \
  --url http://localhost:8080/ors \
  --output snapped.csv"
```

## 输出格式

### Route Generator 输出

Route Generator 生成的 CSV 文件包含以下列：

```csv
start_longitude,start_latitude,end_longitude,end_latitude,profile
```

### Snapping Generator 输出

Snapping Generator 生成的 CSV 文件包含以下列：

```csv
longitude,latitude,profile
```

## 性能提示

1. **调整线程数**：增加线程数可加快生成速度，但要注意系统资源。将线程数设为可用核心数的两倍仍能获得不错的性能。
2. **设置有意义的距离约束**：过大/过小的最小/最大距离可能导致有效路线变少。这在很大程度上取决于所选配置。例如，汽车的长距离比步行者耗时少得多。
3. **使用本地 ORS 实例**：生成大数据集时，本地实例可减少网络延迟并节省 API 配额。
