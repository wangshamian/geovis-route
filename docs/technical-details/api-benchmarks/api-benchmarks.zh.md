# API 基准测试工具

openrouteservice 提供了用于对 API 性能进行基准测试的工具，使你能够在各种负载条件下测试 openrouteservice 实例或外部 API 的响应能力、吞吐量和可扩展性。

## 前置条件

要使用 API 基准测试工具，你需要：

- Java 17+
- Maven
- 一个运行中的 openrouteservice 实例（本地或远程）
- 包含测试坐标的 CSV 文件（可使用[坐标生成工具](/technical-details/api-benchmarks/coordinate-generators.zh.md)生成）

## 概述

基准测试工具是 `ors-benchmark` 模块的一部分，包括：

1. **Isochrones Range Load Test** - 在启用 range 选项的各种负载条件下测试 Isochrones API 的性能。
2. **Routing Algorithm Load Test** - 使用不同路径规划算法和配置测试 Directions API 的性能。

这些工具使用 Gatling 框架模拟用户负载，并提供详细的性能指标，帮助你：

- 评估 openrouteservice 实例的性能
- 找出配置中的瓶颈
- 比较不同的路径规划算法和配置
- 针对特定用例优化部署

## Isochrones Range Load Test

Isochrones Range Load Test 工具通过发送带有可配置参数的多个请求来测试 Isochrones API 的性能，其中包含对地点的批处理以及 Range 类型（时间或距离）的支持。

### Isochrones 工作原理

该工具从 CSV 文件读取坐标，并创建具有不同批大小（每个请求的地点数量）的等时圈请求。它使用 Gatling 框架模拟并发用户，并测量响应时间、吞吐量及其他性能指标。

### Isochrones 选项

| 选项 | 说明 | 默认值 |
|--------|-------------|---------|
| `source_files` | 以逗号分隔的 CSV 文件列表，文件包含坐标对（必填） | （必填） |
| `base_url` | ORS API 基础 URL | <http://localhost:8082/ors> |
| `api_key` | 用于身份验证的 API key | （无） |
| `profile` | 路径规划配置 | driving-car |
| `range` | 以逗号分隔的等时圈 range 列表，单位为米或秒 | 300 |
| `field_lon` | 经度列对应的 CSV 字段名 | longitude |
| `field_lat` | 纬度列对应的 CSV 字段名 | latitude |
| `concurrent_users` | 并发用户数 | 1 |
| `query_sizes` | 以逗号分隔的地点处理批大小列表 | 1 |
| `test_unit` | 要测试的 range 类型 - 'distance' 或 'time' | distance |
| `parallel_execution` | 并行或顺序运行场景 | false |

### Isochrones CSV 文件格式

Isochrones Range Load Test 期望的 CSV 文件格式如下：

```csv
longitude,latitude,profile
8.681495,49.41461,driving-car
8.686507,49.41943,driving-car
8.681495,49.41461,cycling-regular
8.686507,49.41943,cycling-regular
...
```

### Isochrones 示例

#### 为 Isochrones 测试生成点

在德国范围内为多种配置生成 10,000 个点：

```bash
./mvnw clean compile exec:java -Dexec.cleanupDaemonThreads=false -pl 'ors-benchmark' \
  -Dexec.mainClass="org.heigit.ors.coordinates_generator.SnappingGeneratorApp" \
  -Dexec.args="\
  -n 2500 \
  -e 7.5,47.3,13.5,55.0 \
  -p driving-car,cycling-regular,foot-walking,driving-hgv \
  -r 350 \
  -u http://localhost:8080/ors \
  -o germany_car-foot-bike-hgv_10000_points.csv"
```

#### Isochrones 基本用法

使用默认参数测试 Isochrones API：

```bash
./mvnw -pl 'ors-benchmark' gatling:test \
  -Dgatling.simulationClass=org.heigit.ors.benchmark.IsochronesRangeLoadTest \
  -Dsource_files='germany_car-foot-bike-hgv_10000_points.csv' \
  -Dbase_url='http://localhost:8080/ors'
```

#### 使用批处理进行测试

测试使用不同批大小的 Isochrones API，每个请求分别处理 1、2、4 和 8 个地点：

```bash
./mvnw -pl 'ors-benchmark' gatling:test \
  -Dgatling.simulationClass=org.heigit.ors.benchmark.IsochronesRangeLoadTest \
  -Dsource_files='germany_car-foot-bike-hgv_10000_points.csv' \
  -Dbase_url='http://localhost:8080/ors' \
  -Dquery_sizes='1,2,4,8' \
  -Drange='300,600,900' \
  -Dtest_unit='time'
```

#### 使用全部参数的完整测试

生成针对 cycling-regular 配置的点：

```bash
./mvnw -pl 'ors-benchmark' gatling:test \
  -Dgatling.simulationClass=org.heigit.ors.benchmark.IsochronesRangeLoadTest \
  -Dsource_files='germany_car-foot-bike-hgv_10000_points.csv' \
  -Dbase_url='http://localhost:8080/ors' \
  -Dprofile='cycling-regular' \
  -Drange=500 \
  -Dconcurrent_users=1 \
  -Dquery_sizes='1,2,4,6,8,10,12,15,20' \
  -Dtest_unit='distance'
```

## Routing Algorithm Load Test

Routing Algorithm Load Test 工具使你能够使用不同的路径规划算法和配置测试 Directions API 的性能。

### 路径规划工作原理

该工具从 CSV 文件读取起点-终点坐标对，并向 Directions API 发送路径规划请求。它可以测试不同的路径规划算法和配置，模拟并发用户，并测量响应时间、吞吐量及其他性能指标。

### 路径规划选项

| 选项 | 说明 | 默认值 |
|--------|-------------|---------|
| `source_files` | 以逗号分隔的 CSV 文件列表，文件包含坐标对（必填） | （无） |
| `base_url` | ORS API 基础 URL | <http://localhost:8082/ors> |
| `api_key` | 用于身份验证的 API key | （无） |
| `modes` | 以逗号分隔的路径规划模式：algoch, algocore, algolmastar | all |
| `field_start_lon` | 起点经度列对应的 CSV 字段名 | start_longitude |
| `field_start_lat` | 起点纬度列对应的 CSV 字段名 | start_latitude |
| `field_end_lon` | 终点经度列对应的 CSV 字段名 | end_longitude |
| `field_end_lat` | 终点纬度列对应的 CSV 字段名 | end_latitude |
| `concurrent_users` | 并发用户数 | 1 |
| `parallel_execution` | 并行或顺序运行场景 | false |

### 路径规划 CSV 文件格式

Routing Algorithm Load Test 期望的 CSV 文件格式如下：

```csv
start_longitude,start_latitude,end_longitude,end_latitude,profile
8.681495,49.41461,8.686507,49.41943,driving-car
8.686507,49.41943,8.691528,49.41567,driving-car
...
```

### 路径规划示例

#### 为路径规划测试生成路线

按配置生成 10,000 条具有不同距离约束的路线：

```bash
./mvnw clean compile exec:java -Dexec.cleanupDaemonThreads=false -pl 'ors-benchmark' \
  -Dexec.mainClass="org.heigit.ors.coordinates_generator.RouteGeneratorApp" \
  -Dexec.args="\
  -n 2500 \
  -e 7.5,47.3,13.5,55.0 \
  -p driving-car,cycling-regular,foot-walking,driving-hgv \
  -d 200 \
  -m 600,10,5,600 \
  -t 8 \
  -u http://localhost:8080/ors \
  -o germany_car600-bike10-foot5-hgv600_10000_routes.csv"
```

#### 路径规划基本用法

使用默认参数测试 Directions API：

```bash
./mvnw -pl 'ors-benchmark' gatling:test \
  -Dgatling.simulationClass=org.heigit.ors.benchmark.RoutingAlgorithmLoadTest \
  -Dsource_files='germany_car600-bike10-foot5-hgv600_10000_routes.csv' \
  -Dbase_url='http://localhost:9082/ors'
```

#### 使用自定义字段名进行测试

生成使用自定义列名的路线：

```bash
./mvnw clean compile exec:java -Dexec.cleanupDaemonThreads=false -pl 'ors-benchmark' \
  -Dexec.mainClass="org.heigit.ors.coordinates_generator.RouteGeneratorApp" \
  -Dexec.args="\
  -n 10000 \
  -e 7.5,47.3,13.5,55.0 \
  -p driving-car,cycling-regular,foot-walking,driving-hgv \
  -d 200 \
  -m 600,10,5,600 \
  -t 8 \
  -u http://localhost:8080/ors \
  -o germany_car600-bike10-foot5-hgv600_10000_routes.csv"
```

使用自定义字段名测试 Directions API：

```bash
./mvnw -pl 'ors-benchmark' gatling:test \
  -Dgatling.simulationClass=org.heigit.ors.benchmark.RoutingAlgorithmLoadTest \
  -Dsource_files='germany_car600-bike10-foot5-hgv600_10000_routes.csv' \
  -Dfield_start_lon='from_lon' \
  -Dfield_start_lat='from_lat' \
  -Dfield_end_lon='to_lon' \
  -Dfield_end_lat='to_lat' \
  -Dmodes='algolmastar'
```

#### 测试多种路径规划算法

使用多种路径规划算法测试 Directions API：

```bash
./mvnw -pl 'ors-benchmark' gatling:test \
  -Dgatling.simulationClass=org.heigit.ors.benchmark.RoutingAlgorithmLoadTest \
  -Dsource_files='germany_car600-bike10-foot5-hgv600_10000_routes.csv' \
  -Dparallel_execution=false \
  -Dmodes='algoch,algocore'
```

## 性能测试最佳实践

1. **从基线开始**：在已知且稳定的配置上运行测试，以建立用于比较的基线。
2. **逐步增加负载**：从低并发开始，逐步提高，以找出临界点。
3. **贴近真实场景**：使用符合预期使用情况的真实数据和请求模式。
4. **等待稳态**：在得出结论前，让系统达到稳态。
5. **隔离变量**：每次只更改一个参数，以便理解其影响。
6. **重复测试**：多次运行测试会得到更可靠的结果。
7. **监控资源**：测试期间关注 CPU、内存、磁盘 I/O 和网络使用情况。

## 故障排查

- **内存不足错误**：通过 `-Xmx` 参数增大 JVM 堆大小。
- **连接超时**：检查网络连通性和防火墙设置。
- **速率限制**：如果使用远程 API，请留意其速率限制策略。
- **CSV 文件问题**：确保 CSV 文件格式正确且不含 BOM 标记。
