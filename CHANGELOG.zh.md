# 变更日志
本项目的所有重要变更都将记录在此文件中。

格式基于 [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)

<!--
一条 Changelog 条目应当如下所示：

## [version] - YYYY-MM-DD

### 新增
- 新功能。
### 变更
- 现有功能。
### 弃用
- 即将移除的功能。
### 移除
- 现已移除的功能。
### 修复
- 任意缺陷。
### 安全
- 涉及漏洞的情况。（用于漏洞修复）

发布流程：
发布相关说明记录在 RELEASE.md 中
-->

## [unreleased]

### 新增

### 变更

### 弃用

### 移除

### 修复

### 安全


## [1.0] - 2025-07-28

### 修复
- 修复涉及与街道相连人行道的步行路线中 way type 与 surface extra info 元数据不正确的问题 ([#2075](https://github.com/GIScience/openrouteservice/pull/2075))
- 国界存储初始化 ([#2095](https://github.com/GIScience/openrouteservice/pull/2095))

### 安全
- 由于 [CVE-2025-30749](https://nvd.nist.gov/vuln/detail/CVE-2025-30749)、[CVE-2025-30754](https://nvd.nist.gov/vuln/detail/CVE-2025-30754)、[CVE-2025-50059](https://nvd.nist.gov/vuln/detail/CVE-2025-50059)、[CVE-2025-50106](https://nvd.nist.gov/vuln/detail/CVE-2025-50106)、[CVE-2025-21587](https://nvd.nist.gov/vuln/detail/CVE-2025-21587)、[CVE-2025-30691](https://nvd.nist.gov/vuln/detail/CVE-2025-30691)、[CVE-2025-30698](https://nvd.nist.gov/vuln/detail/CVE-2025-30698)，将 OpenJDK 更新至 21.0.8


## [9.3.0] - 2025-06-06

### 新增
- 为项目添加 maven wrapper ([#1973](https://github.com/GIScience/openrouteservice/issues/1973))
- 在 API 响应中包含 OSM 数据版本（`osm_date`）([#1192](https://github.com/GIScience/openrouteservice/issues/1192))
- 添加等时圈属性的文档 ([#846](https://github.com/GIScience/openrouteservice/issues/846))
- 在 GPX 响应中包含 `graph_date` 和 `osm_date` ([#2055](https://github.com/GIScience/openrouteservice/issues/2055))
- 使用来自经过增强、包含带有国家 ID 标签节点的 OSM 文件的国家信息 ([#1753](https://github.com/GIScience/openrouteservice/pull/1753))

### 变更
- 重构：使用 DataAccess 实现核心节点 ID 映射 ([#2074](https://github.com/GIScience/openrouteservice/pull/2074))

### 修复
- 修复 matrix 生成请求的 http 连接池 ([#2059](https://github.com/GIScience/openrouteservice/pull/2059))
- 将已弃用的 CodeQL 和运行失败的 Grype 扫描 GitHub actions 升级到当前版本 ([#2061](https://github.com/GIScience/openrouteservice/pull/2061))
- 避免在仅限客户通行的道路上进行路径规划 ([#2060](https://github.com/GIScience/openrouteservice/pull/2060))
- 修复 driving-hgv 在 `hazmat` 为 `false` 或其他默认 Vehicle 初始化值时所必需的动态权重 ([#2071](https://github.com/GIScience/openrouteservice/pull/2071))
- 支持在无需指定车辆类型的情况下设置车辆参数 ([#2073](https://github.com/GIScience/openrouteservice/pull/2073))


## [9.2.0] - 2025-05-06

### 新增
- 为 graph 仓库客户端添加 minio 选项 ([#2050](https://github.com/GIScience/openrouteservice/pull/2050))
- 文档中添加使用 yaml 编辑器的提示 ([#1836](https://github.com/GIScience/openrouteservice/issues/1836))
- 将 `access = permit` OSM 标签作为已知的通行限制类型 ([#2013](https://github.com/GIScience/openrouteservice/issues/2013))
- 用于性能基准测试的 matrix 生成器 ([#2038](https://github.com/GIScience/openrouteservice/pull/2038))

### 修复
- 更新过时的 dockerfile 基础镜像，并添加更健壮的 native golang 基础镜像 ([#2027](https://github.com/GIScience/openrouteservice/pull/2027))
- 未找到路线时缺少 graph 构建日期 ([#2020](https://github.com/GIScience/openrouteservice/pull/2020))
- github action 构建默认 builder 镜像失败 ([#2032](https://github.com/GIScience/openrouteservice/issues/2032)) 
- 修复文档中 extra-info 'waycategory' 的错误示例 ([#1797](https://github.com/GIScience/openrouteservice/issues/1797))
- 修复文档中的人口数据链接 ([#1624](https://github.com/GIScience/openrouteservice/issues/1624))
- docker 配置中的 REBUILD_GRAPHS 接受大小写布尔值 ([#2028](https://github.com/GIScience/openrouteservice/pull/2028))
- 修正 custom-model 文档中可用的特性 ([#2033](https://github.com/GIScience/openrouteservice/issues/2033))
- 防止由 maxspeed OSM 标签推导出的渡轮速度超过 encoder 允许的最大值 ([#2030](https://github.com/GIScience/openrouteservice/issues/2030)) 


## [9.1.2] - 2025-04-10

### 修复
- 查询 round_trip 时传递 extra_info ([#1976](https://github.com/GIScience/openrouteservice/issues/1976))
- 通过固定 buildkit 0.15 修复损坏的集成测试 ([#2003](https://github.com/GIScience/openrouteservice/pull/2003))
- 设置为 false 的高程选项未被考虑 ([#1967](https://github.com/GIScience/openrouteservice/issues/1967))
- 使 gpx 响应符合标准，并将版本设置为 1.1 ([#2004](https://github.com/GIScience/openrouteservice/issues/2004))
- 为 'waytype' 返回正确的 extra info 标签 ([#1579](https://github.com/GIScience/openrouteservice/issues/1579))
- 允许通过带有 `access = permit` 标签的障碍物 ([#2002](https://github.com/GIScience/openrouteservice/pull/2002))
- 修复土耳其语翻译中的拼写错误 ([#2016](https://github.com/GIScience/openrouteservice/pull/2016))
- 后端文档部署到 GitHub pages ([#2022](https://github.com/GIScience/openrouteservice/pull/2022))
- 后端文档版本号及 public 文件夹资源链接的警告 ([#2023](https://github.com/GIScience/openrouteservice/pull/2023))

### 安全
- 由于 [tomcat CVE](https://github.com/advisories/GHSA-83qj-6fr2-vhqg)，将 spring-boot-starter-parent 升级至 3.4.4

## [9.1.1] - 2025-03-13

### 变更
- 将 RELEASING 章节从 CHANGELOG.md 移至新的 RELEASE.md ([#1972](https://github.com/GIScience/openrouteservice/pull/1972))
- 重构：调整 CsvGraphStorage 的值范围 ([#1969](https://github.com/GIScience/openrouteservice/pull/1969))

### 修复
- preparation_mode 标记 ([#1966](https://github.com/GIScience/openrouteservice/pull/1966))
- ORS_CONFIG_LOCATION 使用错误路径时不记录日志 ([#1816](https://github.com/GIScience/openrouteservice/issues/1816))


## [9.1.0] - 2025-01-29
### 新增
- TopoJSON graph 导出 ([#1926](https://github.com/GIScience/openrouteservice/pull/1926))

### 变更
- 改进荷兰语翻译 ([#1913](https://github.com/GIScience/openrouteservice/issues/1913))
- 更新依赖：spring、geotools、jackson、springdoc、swagger、jupiter、testcontainers、surefire ([#1939](https://github.com/GIScience/openrouteservice/pull/1939))
- 重构 routing profile 管理
- 将 vite（文档）从 5.4.10 更新到 5.4.14 ([#1953](https://github.com/GIScience/openrouteservice/pull/1953))

### 修复
- 错误处理中的 NPE ([#1925](https://github.com/GIScience/openrouteservice/pull/1925))
- 在 profile 属性文档中补上缺失的 'build' ([#1947](https://github.com/GIScience/openrouteservice/issues//1947))
- 允许车辆在没有显式 access 标签的情况下使用渡轮 ([#1954](https://github.com/GIScience/openrouteservice/pull/1954))

## [9.0.0] - 2024-12-02
### 新增
- 当通过车辆参数中的 `hazmat` 标志限制危险品运输道路时，考虑隧道类别 B、C、D 和 E ([#1879](https://github.com/GIScience/openrouteservice/pull/1879))
- 乌克兰语翻译 ([#1883](https://github.com/GIScience/openrouteservice/pull/1883))
- 新增从远程仓库下载新 routing graph 的功能 ([#1889](https://github.com/GIScience/openrouteservice/pull/1889))
- 为 directions 请求添加 custom model 支持 ([#1950](https://github.com/GIScience/openrouteservice/pull/1950))
### 变更
- 更新文档依赖：VitePress ([#1872](https://github.com/GIScience/openrouteservice/pull/1872))
- 调整 export 端点的文档 ([#1872](https://github.com/GIScience/openrouteservice/pull/1872))
- 将配置类重构为 ors-engine 中的独立结构
  ([#1889](https://github.com/GIScience/openrouteservice/pull/1889))，这意味着配置属性发生了如下变化：
    - profile 属性 `ors.engine.profile.<profile>.*` 被移入新节点
      `ors.engine.profile.<profile>.build` 和 `ors.engine.profile.<profile>.service`（相应地
      还有 `ors.engine.profile_default.build`……）
    - 将 `ors.engine.source_file` 移至 `ors.engine.profiles.<profile>.build.source_file` /
      `ors.engine.profile_default.build.source_file`
    - 现在可以使用任意字符串作为 profile 名称。该名称会用作 API 请求中的 profile 名称，
      以及该 profile 的 graph 目录名称
    - 现在可以使用多个共用同一个 flag encoder 的 profile
    - 现在可以为每个 profile 指定各自的源文件（osm 数据）
- 用基于 junit 和 TestContainers 的测试替换旧的基于 bash 的集成测试
  ([#1889](https://github.com/GIScience/openrouteservice/pull/1889))
### 修复
- 除非权重计算需要，否则不强制使用时间相关的路径规划算法 ([#1865](https://github.com/GIScience/openrouteservice/pull/1865))
- 修复将 departure/arrival 参数与 avoid polygons 组合使用时查询失败的问题 ([#1871](https://github.com/GIScience/openrouteservice/pull/1871))
- 修复在 sources 或 destinations 中显式指定 'all' 值时 matrix 限额被忽略的问题
  ([#1875](https://github.com/GIScience/openrouteservice/pull/1875))
### 安全
- 更新 graphhopper 依赖，包含针对
  CVE-2024-7254 的修复 ([#1918](https://github.com/GIScience/openrouteservice/pull/1918))

## [8.2.0] - 2024-10-09
### 新增
- 越南语翻译 ([#1859](https://github.com/GIScience/openrouteservice/pull/1859))
- 芬兰语翻译 ([#1862](https://github.com/GIScience/openrouteservice/pull/1862))
### 变更
- 重构：清理 routing profile 管理 ([#1850](https://github.com/GIScience/openrouteservice/pull/1850))
- 改进关于外部存储配置的文档 ([#1811](https://github.com/GIScience/openrouteservice/pull/1811))
- 重构：从 hgv 边过滤器中移除未使用的 access destination 逻辑 ([#1854](https://github.com/GIScience/openrouteservice/pull/1854))
### 修复
- 修复禁止运输危险品道路的通行判断 ([#1853](https://github.com/GIScience/openrouteservice/pull/1853))

## [8.1.3] - 2024-09-13
### 变更
- 更新依赖：GraphHopper、Spring ([#1844](https://github.com/GIScience/openrouteservice/pull/1844))
- 升级 docker 容器版本 ([#1839](https://github.com/GIScience/openrouteservice/pull/1839))

## [8.1.2] - 2024-08-13
### 变更
- 更新依赖：guava、commons、rest-assured 等 ([#1831](https://github.com/GIScience/openrouteservice/pull/1831))
- 更新依赖：spring、swagger、geotools 等 ([#1827](https://github.com/GIScience/openrouteservice/pull/1827))
### 修复
- 允许任意数量的 csv 列 ([#1436](https://github.com/GIScience/openrouteservice/issues/1436))

## [8.1.1] - 2024-07-17
### 新增
- 添加丹麦语翻译 ([#1809](https://github.com/GIScience/openrouteservice/pull/1809))
### 修复
- 若显式设置了 access，则允许自行车通行于在建高速公路 ([#1805](https://github.com/GIScience/openrouteservice/pull/1805))

## [8.1.0] - 2024-06-05
### 新增
- 记录 OSM 标签 `surface` 的哪些取值会被计入 way surface 类别 ([#1794](https://github.com/GIScience/openrouteservice/pull/1794))
- 为 snapping 端点添加配置参数 `maximum_locations` ([#1796](https://github.com/GIScience/openrouteservice/pull/1796))
### 变更
- 仅根据 OSM 标签 `surface` 的取值判断 way surface；如果该标签不存在，则 surface 报告为 "Unknown"，不再根据 way type 推断 ([#1794](https://github.com/GIScience/openrouteservice/pull/1794))
- 改进源点数量多于目标点数量时 RPHAST matrix 查询的性能 ([#1795](https://github.com/GIScience/openrouteservice/pull/1795))
- 修订 snap 端点的错误码 ([#1796](https://github.com/GIScience/openrouteservice/pull/1796))
- 重构：清理 routing profile 管理 ([#1790](https://github.com/GIScience/openrouteservice/pull/1790))
### 移除
- 将 way surface 类别 "Fine gravel"、"Cobblestone" 和 "Woodchips" 分别合并到现有类别 "Gravel"、"Paving stones" 和 "Unpaved" ([#1794](https://github.com/GIScience/openrouteservice/pull/1794))
### 修复
- 可靠地编码 way type 和 surface ([#1794](https://github.com/GIScience/openrouteservice/pull/1794))

## [8.0.1] - 2024-05-14
### 新增
- A* 配置参数的文档 ([#1759](https://github.com/GIScience/openrouteservice/pull/1759))
- `maximum_grade_level` encoder 配置参数非正值取值的文档 ([#1775](https://github.com/GIScience/openrouteservice/pull/1775))
- spring 内置 tomcat 的 keep-alive-timeout ([#1780](https://github.com/GIScience/openrouteservice/pull/1780))

### 修复
- 修复 preparation 模式失败时仍以退出码 0 退出的问题 ([#1772](https://github.com/GIScience/openrouteservice/pull/1772))
- 更多属性现在可以在用户的 gr-config.yml/env 的 ors.engine.profile_default 中定义而不会产生副作用（Issue [#1762](https://github.com/GIScience/openrouteservice/issues/1762)）
- 修复文档 markdown 源文件中的相对链接 ([#1791](https://github.com/GIScience/openrouteservice/pull/1791))

### 安全
- 修复 CVE GHSA-hgjh-9rj2-g67j、GHSA-ccgv-vj62-xf9h、GHSA-7w75-32cg-r6g2、GHSA-v682-8vv8-vpwr ([#1788](https://github.com/GIScience/openrouteservice/pull/1788))

## [8.0.0] - 2024-03-21
### 新增
- 用于返回图上最近点的 snapping 服务端点 ([#1519](https://github.com/GIScience/openrouteservice/issues/1519))
- RPM 打包工作流 ([#1490](https://github.com/GIScience/openrouteservice/pull/1490))
- 使用 GitHub environments 进行 graph 构建的工作流 ([#1468](https://github.com/GIScience/openrouteservice/pull/1468))
- 使用 docker 构建 graph 时用于调整文件夹和路径的环境变量：([#1468](https://github.com/GIScience/openrouteservice/pull/1468))
  - `ELEVATION_CACHE_FOLDER`：用于覆盖 `ors.services.routing.profiles.default_params.elevation_cache_path` 的值
  - `GRAPHS_FOLDER`：用于覆盖 `ors.services.routing.profiles.default_params.graphs_root_path` 的值
  - `LOGS_FOLDER`：用于覆盖 `ors.logging.location` 的值
  - `PBF_FILE_PATH`：用于覆盖 `ors.services.routing.sources` 的值
- 添加 .editorconfig 以统一 IDE 代码风格 ([#1493](https://github.com/GIScience/openrouteservice/pull/1493))
- 参数说明中有关时长格式的信息 ([#1504](https://github.com/GIScience/openrouteservice/pull/1504))
- maven profile buildFatJar，用于构建 fatJar（内嵌 tomcat 的应用，可通过 java -jar ors.jar 启动）
  而非 war 文件
- YML 配置 ([#1506](https://github.com/GIScience/openrouteservice/pull/1506))
- 使用 VitePress 的新后端文档 ([#1617](https://github.com/GIScience/openrouteservice/pull/1617))
- 支持挪威语 ([#1645](https://github.com/GIScience/openrouteservice/pull/1645))
- 支持 mmap dataaccess 模式 ([#1649](https://github.com/GIScience/openrouteservice/pull/1649))
- 为新发布提供 JAR 和 WAR 产物 ([#1669](https://github.com/GIScience/openrouteservice/pull/1669))

### 变更
- 默认分支从 `master` 改为 `main`
- 仅在 PT 响应中包含 transfers 和 fare 属性 ([#1586](https://github.com/GIScience/openrouteservice/pull/1586))
- 在 routing JSON 响应中抑制空的 legs 属性 ([#1584](https://github.com/GIScience/openrouteservice/pull/1584))
- url_check.sh 支持自定义 sleep 和报告间隔 ([#1468](https://github.com/GIScience/openrouteservice/pull/1468))
- 自动生成的 API 规范从 Swagger 2.0 迁移到 OpenAPI 3.0 ([#1487](https://github.com/GIScience/openrouteservice/pull/1487))
- 类/变量命名从 Swagger 改为 OpenAPI ([#1504](https://github.com/GIScience/openrouteservice/pull/1504))
- 迁移到 Jakarta EE9 ([#1504](https://github.com/GIScience/openrouteservice/pull/1504))
- 清理 StatusCodeCaptureWrapper 类 ([#1504](https://github.com/GIScience/openrouteservice/pull/1504))
- spring-boot-starter-parent 升级至 v3.1.1 ([#1504](https://github.com/GIScience/openrouteservice/pull/1504))
- 从 springdoc-openapi-ui 包改为 springdoc-openapi-starter-webmvc-ui ([#1504](https://github.com/GIScience/openrouteservice/pull/1504))
- 重构 RoutingProfile（属于 [#1520](https://github.com/GIScience/openrouteservice/issues/1520)）
- 更新依赖的 maven 仓库 ([#1536](https://github.com/GIScience/openrouteservice/pull/1536))
- spring-boot-starter-parent 升级至 v3.1.6 ([#1630](https://github.com/GIScience/openrouteservice/issues/1630))
- 静默 traffic map-matching 的内部错误 ([#1635](https://github.com/GIScience/openrouteservice/pulls/1635))
- 修复 IN1-JAVA-ORGMOZILLA-1314295 ([#1627](https://github.com/GIScience/openrouteservice/issues/1627))
- 记录 traffic mapmatching 的汇总统计信息，并仅在调试模式下使用进度条 ([#1647](https://github.com/GIScience/openrouteservice/pull/1647))
- 将 APIEnums 移入 API 模块 ([#1634](https://github.com/GIScience/openrouteservice/issues/1634))
- 提升等时圈计算的性能 ([#1607](https://github.com/GIScience/openrouteservice/pull/1607)) 和 ([#1658](https://github.com/GIScience/openrouteservice/pull/1658))
- 计算等时圈时，不对标记为 "access=destination" 的道路施加速度惩罚 ([#1682](https://github.com/GIScience/openrouteservice/pull/1682))
- 后端文档全面改版 ([#1651](https://github.com/GIScience/openrouteservice/pull/1651))
- 将 docker 镜像构建与测试工作流分离。若有改动 main，则按计划每晚构建 ([#1689](https://github.com/GIScience/openrouteservice/pull/1689))
- 重构等时圈构建类 ([#1699](https://github.com/GIScience/openrouteservice/pull/1699))
- 统一各等时圈构建器中的边分割方式，并基于坐标而非实际米制距离来分割边 ([#1708](https://github.com/GIScience/openrouteservice/pull/1708))
- 在文档中补上缺失的 encoder_options [#1752](https://github.com/GIScience/openrouteservice/pull/1752)
- 为 json 配置添加配置测试 [#1749](https://github.com/GIScience/openrouteservice/pull/1749)
- 根据新的 docker 配置调整文档 ([#1746](https://github.com/GIScience/openrouteservice/pull/1746))

### 弃用
- JSON 配置及相关类 ([#1506](https://github.com/GIScience/openrouteservice/pull/1506))

### 移除
- 对 apache-curator 的依赖 ([#1496](https://github.com/GIScience/openrouteservice/issues/1496))
- ORSKafkaConsumer 及相关类 ([#1482](https://github.com/GIScience/openrouteservice/pull/1482))
- RoutingProfileUpdater 及相关类
- 旧的 map matching 代码
- Centrality API 及其实现（请改用 export API 和外部中心性工具）
- springfox-swagger 包与 GroupedOpenApi 构建器的临时解决方案 ([#1504](https://github.com/GIScience/openrouteservice/pull/1504))
- RoutingCHGraph 中错误的 instanceof 检查/强制转换 ([#1504](https://github.com/GIScience/openrouteservice/pull/1504))
- 修复 ElevationSmoother ([#1511](https://github.com/GIScience/openrouteservice/pull/1511))
- 废弃的配置选项 'min_one_way_network_size' 和 'disabling_allowed' ([#1683](https://github.com/GIScience/openrouteservice/pull/1683))

### 修复
- 修复 GH 中使用旧版 jts-core 引起的 GTFS 问题 ([#1501](https://github.com/GIScience/openrouteservice/pull/1501))
- 将 kafka_2.13 从 3.4.0 升级到 3.5.0 ([#1472](https://github.com/GIScience/openrouteservice/issues/1472))
- 移除 maven-shared-utils 依赖 ([#1473](https://github.com/GIScience/openrouteservice/issues/1473))
- 将 spring-boot 从 2.7.10 更新到 2.7.12 ([#1474](https://github.com/GIScience/openrouteservice/issues/1474))
- 将 org.geotools.gt-epsg-hsql 升级到 29.1 版本。([#1479](https://github.com/GIScience/openrouteservice/issues/1479))
- 修复各种代码风格与底层代码问题 ([#1489](https://github.com/GIScience/openrouteservice/pull/1489))
- Java17 代码风格问题 ([#1504](https://github.com/GIScience/openrouteservice/pull/1504))
- matrix 'metrics' 参数的 defaultValue ([#1504](https://github.com/GIScience/openrouteservice/pull/1504))
- Duration 类型参数的 schema 类型 ([#1504](https://github.com/GIScience/openrouteservice/pull/1504))
- 修复 fast-isochrones 最大访问节点数缺陷 ([#1538](https://github.com/GIScience/openrouteservice/pull/1538))
- 调整 heat stress 路径规划的权重，以避免过大的绕行
- 修复等时圈的吸附 ([#1566](https://github.com/GIScience/openrouteservice/pull/1566))
- 修复 fast-isochrones 的吸附 ([#1572](https://github.com/GIScience/openrouteservice/pull/1572))
- 端点属性 'isochrone' 改为 'isochrones' ([#1683](https://github.com/GIScience/openrouteservice/pull/1683))
- 正确解析带有 `hgv=delivery` 标签的 way 的通行权限 ([#1748](https://github.com/GIScience/openrouteservice/pull/1748))

## [7.1.1] - 2023-11-13
### 变更
- 提高生成等时圈时的边分割阈值 ([#1508](https://github.com/GIScience/openrouteservice/pull/1508))

## [7.1.0] - 2023-06-13
### 新增
- 针对不同类别的 issue 模板与使用 issue forms 的配置 ([#1456](https://github.com/GIScience/openrouteservice/issues/1456))
- 从 Junit4 迁移到 Junit5  ([#1320](https://github.com/GIScience/openrouteservice/pull/1320))
- 支持 GTFS 路径规划  ([#1316](https://github.com/GIScience/openrouteservice/pull/1316))
- 升级到 spring-boot 2.7.9，并为 api-tests 对齐依赖 [#1310](https://github.com/GIScience/openrouteservice/pull/1310)
- 将所有 api 测试移入 `openrouteservice` 模块 [#1352](https://github.com/GIScience/openrouteservice/pull/1352)
- java 从 11 迁移到 17 [#1353](https://github.com/GIScience/openrouteservice/pull/1353)
- 时间相关的核心路径规划算法 ([#1388](https://github.com/GIScience/openrouteservice/pull/1388))
- 针对交通数据的测试 [#1449](https://github.com/GIScience/openrouteservice/pull/1449)
### 变更
- 在不同 matrix 算法中一致地使用最大访问节点数限制 ([#1246](https://github.com/GIScience/openrouteservice/issues/1246))
- 更新查询人口数据库的代码，使其可使用更新的 PostGIS 版本 ([#834](https://github.com/GIScience/openrouteservice/issues/834)) 
### 修复
- 修复某些节点引用无效的 OSM way 在图构建期间导致崩溃的情况 [#1351](https://github.com/GIScience/openrouteservice/issues/1351)
- 更新 docker 配置说明 ([#1326](https://github.com/GIScience/openrouteservice/pull/1326))
- 将 org.apache.logging.log4j:log4j-1.2-api 从 2.19.0 升级到 2.20.0 ([#1324](https://github.com/GIScience/openrouteservice/pull/1324))
- 使用 Dijkstra 的时间相关路径规划 ([#1322](https://github.com/GIScience/openrouteservice/pull/1322))
- 将编译模式从 Java 8 改为 Java 11，并移除旧的 Java 8 API 用法 ([#1321](https://github.com/GIScience/openrouteservice/pull/1321))
- 支持 API 时间参数  ([#1315](https://github.com/GIScience/openrouteservice/pull/1315))
- 将 spring-boot 从 2.7.9 升级到 2.7.10  ([#1372](https://github.com/GIScience/openrouteservice/pull/1372))
- 将 graphhopper 版本升级到 v4.4，以正确刷写 graph 存储 [#1378](https://github.com/GIScience/openrouteservice/pull/1378)
- 处理有关传递依赖的警告 [#1383](https://github.com/GIScience/openrouteservice/issues/1383)
- 将 spring-boot 升级到 2.7.11 [#1416](https://github.com/GIScience/openrouteservice/pull/1416)
- 修复 maven-shared-utils CVE [#1414](https://github.com/GIScience/openrouteservice/pull/1414)
- 排除 snakeyaml [#1418](https://github.com/GIScience/openrouteservice/pull/1418)
- 将 geotools 从 28.2 升级到 29.0 [#1422](https://github.com/GIScience/openrouteservice/pull/1422)
- 将 graphhopper 版本升级到 v4.6 [#1427](https://github.com/GIScience/openrouteservice/pull/1427)
- 交通数据的 map matching ([#1430](https://github.com/GIScience/openrouteservice/pull/1430))
- 将 kafka_2.13 从 3.4.0 升级到 3.4.1 ([#1463](https://github.com/GIScience/openrouteservice/issues/1463))

### 移除
- AccelerationWeighting ([#1454](https://github.com/GIScience/openrouteservice/pull/1454))

## [7.0.1] - 2023-03-08
### 修复
- 修复 tomcat 配置被覆盖的问题 ([#1311](https://github.com/GIScience/openrouteservice/issues/1311))
- 将 commons-io:commons-io 从 2.7 升级到 2.11.0 ([#1309](https://github.com/GIScience/openrouteservice/issues/1309))
- 将 com.fasterxml.jackson.core:jackson-core 从 2.13.3 升级到 2.14.2 ([#1307](https://github.com/GIScience/openrouteservice/issues/1307))
- 将 commons-io:commons-io 从 2.6 升级到 2.7 ([#1304](https://github.com/GIScience/openrouteservice/issues/1304))

## [7.0.0] - 2023-03-03
升级到 GraphHopper 4.0 [#1217](https://github.com/GIScience/openrouteservice/pull/1217)，它（除其他改进外）带来了：
- 更快的构建时间
- 更快的路线/可达性/matrix 计算
- 为所有 profile 改进的 osm 标签逻辑
- 更高的测试覆盖率
- 安全修复
- 稳定性修复

## [6.8.3] - 2023-03-02
### 修复
- wheelchair profile 排除标记为 highway:construction 的 way ([#1295](https://github.com/GIScience/openrouteservice/issues/1295))
- 支持列出多种车辆的通行限制标签 ([#1219](https://github.com/GIScience/openrouteservice/issues/1219))
- 将 org.glassfish.jaxb:jaxb-runtime 从 2.3.7 升级到 2.3.8
- 将 org.postgresql:postgresql 从 42.5.1 升级到 42.5.2

## [6.8.2] - 2023-02-22
### 修复
- csv_factor 和 csv_column API 参数的可见性 ([PR #1279](https://github.com/GIScience/openrouteservice/pull/1279))
- 将 org.apache.kafka:kafka_2.13 及相关包从 3.3.2 更新到 3.4.0
- 更新过时的 dockerfile 依赖 ([PR #1284](https://github.com/GIScience/openrouteservice/pull/1284))
- 通过将 glibc 版本降低到 2.29-r0 修复 docker 构建 ([PR #1287](https://github.com/GIScience/openrouteservice/pull/1287))

## [6.8.1] - 2023-02-08
### 新增
- 关于坐标 CRS 的 API 文档
### 修复
- 步行 profile 的 way 通行权限 ([#1227](https://github.com/GIScience/openrouteservice/issues/1227))
- 修复 swagger 文档 api 的安全漏洞并补充测试 ([PR #1257](https://github.com/GIScience/openrouteservice/pull/1257))
- 将 com.typesafe:config 从 1.4.1 更新到 1.4.2
- 将 log4j 更新到 2.19.0 版本，将 slf4j 更新到 2.0.6
- 将 junit:junit 从 4.13.1 更新到 4.13.2
- 将 org.postgresql:postgresql 从 42.4.3 更新到 42.5.1
- 将 com.fasterxml.jackson bundle 从 2.13.3 更新到 2.14.2
- 将 org.glassfish.jaxb:jaxb-runtime 从 2.3.1 更新到 2.3.7
- 将 org.apache.kafka bundle 从 2.5.1 更新到 3.3.2
- 将 org.apache.curator 从 4.1.0 更新到 5.4.0
- 将 me.tongfei.progressbar 从 0.5.5 更新到 0.9.5 
- 将 org.springframework.boot:spring-boot-starter-web 从 2.5.12 更新到 2.7.7 
- 将 org.springframework.boot:spring-boot-starter-tomcat 从 2.3.5.RELEASE 更新到 2.7.7 
- 将 com.typesafe:config 从 1.4.1 更新到 1.4.2
- 将 springfox-swagger2 更新到 3.0.0
- 将 org.ow2.asm:asm 从 9.0 更新到 9.4

## [6.8.0] - 2022-10-10
### 新增
- 关于不含高程数据的编码 polyline 的后端文档 ([#1094](https://github.com/GIScience/openrouteservice/issues/1094))
- 解码包含高程数据的 polyline 的 python 代码
- 捷克语支持（感谢 [trendspotter](https://github.com/trendspotter) 提供翻译）
- 时间相关路径规划支持步行和徒步
- 世界语支持（感谢 [ecxod](https://github.com/ecxod) 提供翻译）
- 罗马尼亚语支持（感谢 [ecxod](https://github.com/ecxod) 提供翻译）
- 文档中指向 YouTube docker 配置指南的链接（感谢 SyntaxByte）
- 用于 heat stress 的基于通用 CSV 的路径规划原型
- Shadow Routing 
### 移除
- 旧的 v1 API 代码及相关类
### 修复
- 允许自行车 profile 通行带 bicycle=yes 的 bridleway ([#1167](https://github.com/GIScience/openrouteservice/issues/1167))
- 改进日志文件设置错误信息 ([#1110](https://github.com/GIScience/openrouteservice/issues/1110)) 
- Dockerfile 现在会在中间目录不存在时创建它们 ([#1109](https://github.com/GIScience/openrouteservice/issues/1109))
- 修复 `IsochronesRequest` 模型的内部属性未被忽略而在 swagger 文件生成中出现的问题
- 从 API 文档中移除非参数的 `metricsStrings` ([#756](https://github.com/GIScience/openrouteservice/issues/756))
- 为 HGV profile 设置默认车辆类型 ([#816](https://github.com/GIScience/openrouteservice/issues/816))
- 补上缺失的 matchTraffic 重写 ([#1133](https://github.com/GIScience/openrouteservice/issues/1133))
- 修复 docker 文档中的拼写错误
- 修复经由 `waterway=lock_gate` 的步行路径规划 ([#1177](https://github.com/GIScience/openrouteservice/issues/1177))
- 修复用于开阔区域路径规划的 graph 构建器 ([#1186](https://github.com/GIScience/openrouteservice/issues/1186))
- 解决 hgv 扩展存储中的数据对齐问题，该问题偶尔导致 `ArrayIndexOutOfBoundsException` ([#1181](https://github.com/GIScience/openrouteservice/issues/1181))
- 修复 Places.md 中的少量拼写错误 ([#1196](https://github.com/GIScience/openrouteservice/issues/1196))
- 解决 HGV profile 的 matrix 失败问题 ([#1198](https://github.com/GIScience/openrouteservice/issues/1198))

### 变更
- docker 镜像现为多阶段构建 ([#1234](https://github.com/GIScience/openrouteservice/issues/1234))

## [6.7.1] - 2022-10-05
### 新增
- 可选的路由 profile 参数 `force_turn_costs` ([#1220](https://github.com/GIScience/openrouteservice/pull/1220))

## [6.7.0] - 2022-01-04
### 新增
- 添加 core matrix 算法
- 添加用于构建并发布 docker 镜像的新工作流 ([#1035](https://github.com/GIScience/openrouteservice/pull/1035))
- wheelchair 路径规划的可选 `encoder_options`：针对被归类为 problematic/preferred 的 way 的速度因子 ([#980](https://github.com/GIScience/openrouteservice/pull/980))
- wheelchair profile 的可选 routing API 参数 `allow_unsuitable` / `surface_quality_known` ([#980](https://github.com/GIScience/openrouteservice/pull/980))
- Docs 文件夹，汇总来自 openrouteservice-docs、wiki、README.md 以及 docker 子文件夹的文档
- `gr-config.json` 作为默认的 ors 配置选项，将取代 `app.config` ([#1017](https://github.com/GIScience/openrouteservice/issues/1017))
- 系统属性 `ors_config`，将取代 `ors_app_config` 属性 ([#1017](https://github.com/GIScience/openrouteservice/issues/1017))
- 环境变量 `ORS_CONFIG`，将取代 `ORS_APP_CONFIG` ([#1017](https://github.com/GIScience/openrouteservice/issues/1017))
- ors 配置读取优先级
    1. 系统属性 `ors_conf` > `ors_app_conf`
    2. 指向 class path 中文件的环境变量 `ORS_CONF` > `ORS_APP_CONF`
    3. class path 中的文件 `gr-config.json` > `app.config`
    4. 若以上均未指定则报错。
- 后端文档中添加 docker 配置的链接和信息
- wheelchair 路径规划选项文档中的 `minimum_width` ([#1080](https://github.com/GIScience/openrouteservice/pull/1080))
### 变更
- 更新 docker 配置所使用的 tomcat 版本 ([#1022](https://github.com/GIScience/openrouteservice/pull/1022))
- 将 `smoothness-type` 参数重构为 Enum ([#1007](https://github.com/GIScience/openrouteservice/issues/1007))
- 改进 wheelchair 路径规划 ([#980](https://github.com/GIScience/openrouteservice/pull/980))
- 即使指定了 `radius:-1` 仍找不到点时的错误信息 ([#979](https://github.com/GIScience/openrouteservice/issues/979))
- 调整标签过滤的格式
- 将配置格式和文件类型测试改为 JSON
- docker 的 `APP_CONFIG` 参数改为 `ORS_CONFIG` ([#1017](https://github.com/GIScience/openrouteservice/issues/1017))
- wheelchair 的默认最小 `surface-type` 改为 `sett` ([#1059](https://github.com/GIScience/openrouteservice/issues/1059))
- 默认道路 surface 值现为 "paved" 而非 "asphalt" ([#711](https://github.com/GIScience/openrouteservice/issues/711))
- `error_codes.md` 文档现在与其他后端文档放在一起 ([#1069](https://github.com/GIScience/openrouteservice/issues/1069))
- 移除 `*RequestHandlers` 中的重复代码 ([#1067](https://github.com/GIScience/openrouteservice/issues/1067))
- 扩充 `places` 请求与响应的类别列表
### 弃用
- `ors_app_config` 系统属性 ([#1017](https://github.com/GIScience/openrouteservice/issues/1017))
- `app.config` ors 配置文件名 ([#1017](https://github.com/GIScience/openrouteservice/issues/1017))
- `ORS_APP_CONF` 环境变量 ([#1017](https://github.com/GIScience/openrouteservice/issues/1017))
### 修复
- 修复行驶速度说明中的错误
- 修复 CALT 路径规划中失败的断言 ([#1047](https://github.com/GIScience/openrouteservice/issues/1047))
- 改进渡轮路线的行驶时间估算 ([#1037](https://github.com/GIScience/openrouteservice/issues/1037))
- 解析 HGV 车辆类型特定的通行限制时不再要求设置车辆参数 ([#1006](https://github.com/GIScience/openrouteservice/issues/1006))

## [6.6.4] - 2022-01-03
### 修复
- 将 log4j 更新到 2.17.1 版本
- 切换到 GH fork 版本 v0.13.15-4 

## [6.6.3] - 2021-12-15
### 修复
- 切换到 GH fork 版本 v0.13.15-3，以采用更新后的 log4j 版本

## [6.6.2] - 2021-12-15
### 修复
- 将 log4j 版本更新到 2.16.0，该版本修复了 [CVE-2021-44228](https://nvd.nist.gov/vuln/detail/CVE-2021-44228)

## [6.6.1] - 2021-07-05
### 修复
- 使 ORSKafkaConsumerInitContextListener 非阻塞
- 仅为完全位于 bbox 内的边初始化边中心性分数
- 指向旧文档的引用现在指向新文档的渲染版本

## [6.6.0] - 2021-06-08
### 新增
- `radiuses` 参数接受单值和长度为 1 的数组 ([#923](https://github.com/GIScience/openrouteservice/issues/923))
- 针对等时圈 range/interval 不匹配的有用错误信息
### 变更
- `maneuver` 对象中位置的坐标精度改为 6 位小数
### 修复
- 正确计算涉及时间相关速度的路线的行驶时间，不受所用权重影响 ([#956](https://github.com/GIScience/openrouteservice/issues/956))
- 用户提供的最高速度限制与 HGV 路由 profile 的兼容性 ([#955](https://github.com/GIScience/openrouteservice/issues/955))
- 澄清 "Point not found" 错误信息 ([#922](https://github.com/GIScience/openrouteservice/issues/922))
- 修正等时圈响应文档 ([#670](https://github.com/GIScience/openrouteservice/issues/670))
- 修复使用虚拟边构造等时圈几何的罕见缺陷。使用前检查边是否为虚拟边。
- 修复因 spring 为文档读取 getter 导致 centrality 文档中出现重复参数的问题
- 修复超胞子胞 ID 在存储中越界的缺陷

## [6.5.0] - 2021-05-17
### 新增
- 时间相关的基于 core 的路径规划算法
- 可针对单个权重禁用 core 中基于边的路由 ([#928](https://github.com/GIScience/openrouteservice/issues/928))
### 变更
- 低于 encoder 分辨率的速度值将一律存储为最低的非零值，而不再与将相应边的 access 设为 false 一起被舍入为零 ([#944](https://github.com/GIScience/openrouteservice/issues/944))
### 修复
- 不再考虑定义不清的 "maxspeed = 0" OSM 标签 ([#940](https://github.com/GIScience/openrouteservice/issues/940))
- 使用各国限速的 JSON 定义 ([#939](https://github.com/GIScience/openrouteservice/issues/939))
- 用于设置 core 路由激活 landmark 数量的配置文件参数 ([#930](https://github.com/GIScience/openrouteservice/issues/930))
- 确保默认回退算法使用带 beeline 近似的 A* ([#926](https://github.com/GIScience/openrouteservice/issues/926))
- 在路径规划请求中优先使用 graph 构建日期而非数据日期 ([#925](https://github.com/GIScience/openrouteservice/issues/925))
- 修正 BoundingBoxFactoryTest 的包声明 ([#933](https://github.com/GIScience/openrouteservice/issues/933))
- 对匈牙利语支持的若干修正（感谢 [debyos](https://github.com/debyos)）

## [6.4.4] - 2021-08-30
### 变更
- repo.heigit.org 的 URL 改为 HTTPS

## [6.4.3] - 2021-04-28
### 变更
- 减少由 spring 输出流处理引起的不必要警告信息 ([#899](https://github.com/GIScience/openrouteservice/issues/899)
### 修复
- 更改多个 range 时 fast isochrone 的计算行为

## [6.4.2] - 2021-04-21
### 新增
- 允许通过配置文件参数禁用 OSM 条件通行与速度 encoder
- 土耳其语支持（感谢 [kucar17](https://github.com/kucar17) 提供翻译）
### 变更
- app.config.sample 中 HGV profile 的速度计算设置现在与公开 API 一致 ([#806](https://github.com/GIScience/openrouteservice/issues/806))
### 修复
- 修复 core 边过滤器的并发缺陷，该缺陷导致 CALT graph 预处理期间崩溃 ([#905](https://github.com/GIScience/openrouteservice/issues/905))
- 修正等时圈 range 文档 ([#882](https://github.com/GIScience/openrouteservice/issues/676))
- 更新安装说明和用法以反映 v2 api ([#744](https://github.com/GIScience/openrouteservice/issues/744))
- 修正针对 location_type 参数的等时圈算法选择 ([#676](https://github.com/GIScience/openrouteservice/issues/676))
- 更新 readme 中指向客户端翻译的链接

## [6.4.1] - 2021-03-31
### 修复
- 修正错误的 matrix 响应文档 ([#873](https://github.com/GIScience/openrouteservice/issues/873))
- 修正连续相同坐标时 waypoint 索引错误的问题 ([#762](https://github.com/GIScience/openrouteservice/issues/762))
- 更改等时圈多边形计算以使用更多缓冲

## [6.4.0] - 2021-03-26
### 新增
- API 端点 "centrality"，用于计算给定边界框内节点的[介数中心性](https://en.wikipedia.org/wiki/Betweenness_centrality)值。中心性使用 Brandes 算法计算。
- 基于 core 的路径规划算法支持转向限制
### 变更
- 在与路径规划相同的方式下，使用 Querygraph 和虚拟节点进行等时圈计算
- 移除 Isochrones v1 api 测试
### 修复
- 修正包含跳过路段时路线距离限制的计算 ([#814](https://github.com/GIScience/openrouteservice/issues/814))
- 修正缺失的路段距离和时长 ([#695](https://github.com/GIScience/openrouteservice/issues/695))
- 修正请求等时圈交叉时无响应的问题 ([#675](https://github.com/GIScience/openrouteservice/issues/675))
- 修正启用 CH 的 profile 上无 bearing 时 continue_straight 选项的问题

## [6.3.7] - 2021-08-30
### 变更
- repo.heigit.org 的 URL 改为 HTTPS

## [6.3.6] - 2021-02-02
### 修复
- 在加入新构建器之前扩展所有先前限制多边形的坐标，以防止长多边形边被截断

## [6.3.5] - 2021-01-28
### 新增
- 在初始化时输出 run 文件以标记 graph 构建/加载完成
### 修复
- 明确在已有 app.config 的情况下首次启动容器的行为

## [6.3.4] - 2021-01-19
### 变更
- 全面改造 fast isochrones 的轮廓生成。修复边界边的异常行为。

## [6.3.3] - 2021-01-15
### 修复
- 更新 GH 中的 CGIAR URL（参见 GH fork [#35](https://github.com/GIScience/graphhopper/pull/35)）

## [6.3.2] - 2020-12-14
### 新增
- 使用 A* 的时间相关路径规划原型，会考虑 OSM 条件通行限制和限速
- 日语支持 ([#811](https://github.com/GIScience/openrouteservice/pull/811)，感谢 [higa4](https://github.com/higa4) 提供翻译)
### 变更
- 在 active cell 计算中提升 fast isochrones 的性能
### 修复
- 提高小间隔小等时圈几何的稳定性
- 更新依赖

## [6.3.1] - 2020-10-20
### 修复
- 更新有关在 Docker 中运行的文档 ([#798](https://github.com/GIScience/openrouteservice/issues/798))
- 处理 HillIndexStorage 与无高程数据组合的无效情况 ([#683](https://github.com/GIScience/openrouteservice/issues/683))
- 支持为 profile 关闭高程数据处理
- 修复 fast isochrones 预处理中的一个缺陷

## [6.3.0] - 2020-09-14
### 新增
- 基于预处理数据的新 fast isochrone 算法
### 修复
- 修复无效 extra info 请求的处理 ([#795](https://github.com/GIScience/openrouteservice/issues/795))

## [6.2.2] - 2021-08-30
### 变更
- repo.heigit.org 的 URL 改为 HTTPS

## [6.2.1] - 2020-08-13
### 新增
- 在路径规划前检查路径点是否位于不同国家，若位于不同国家且需避免所有边界则中止
### 修复
- 更新 Docker 流程以使用 Java 11 ([#777](https://github.com/GIScience/openrouteservice/issues/777))
- 在预处理中初始化 core 边过滤器时正确解析路由 profile 类别 ([#785](https://github.com/GIScience/openrouteservice/issues/785))

## [6.2.0] - 2020-07-15
### 新增
- directions API 的驾车 profile 新增 `maximum_speed` 参数，用于在配置文件设定的某个阈值之上指定速度限制。
- 波兰语翻译 ([#690](https://github.com/GIScience/openrouteservice/issues/690))
- 用于启用高程平滑的配置参数 ([#725](https://github.com/GIScience/openrouteservice/issues/725))
### 修复
- 修复设置了 bearings 参数时回退到动态路由方法的问题 ([#702](https://github.com/GIScience/openrouteservice/issues/702))
- 为桥梁和隧道启用高程插值 ([#685](https://github.com/GIScience/openrouteservice/issues/685))
- 修复 green 和 quiet 等软权重的时长计算错误
- 为 hgv profile 启用推荐权重，并使路由算法与请求的匹配更健壮 ([#755](https://github.com/GIScience/openrouteservice/issues/755))
### 变更
- 改进自行车和步行 profile 的推荐权重 ([#665](https://github.com/GIScience/openrouteservice/issues/665))
- 重构 AdditionWeighting
- 升级到 Java 11
### 弃用
- 使用推荐权重替代最快权重 ([#763](https://github.com/GIScience/openrouteservice/issues/763))

## [6.1.3] - 2021-09-03
### 变更
- 移除未使用的依赖

## [6.1.2] - 2021-08-30
### 变更
- repo.heigit.org 的 URL 改为 HTTPS

## [6.1.1] - 2020-06-02
### 新增
- 用于从 pbf 数据读取高程标签的配置选项
- 用于设置位置索引分辨率及坐标查找最大迭代次数的配置参数 ([#712](https://github.com/GIScience/openrouteservice/issues/712))
### 修复
- 移除原生与 docker 配置下两份 `app.config` 文件的维护负担 ([#742](https://github.com/GIScience/openrouteservice/issues/742))
- 允许在 extra info 参数中使用 green 和 noise ([#688](https://github.com/GIScience/openrouteservice/issues/688))
- 修复使用备选路线时 extra info 分组的问题 ([#681](https://github.com/GIScience/openrouteservice/issues/681))
- 修复 way surface/type 编码问题 ([#677](https://github.com/GIScience/openrouteservice/issues/677))
- 查询最短权重时，若有可用的 CH shortest 预处理则使用它
- 标记为 destination access 的道路对 hgv 的惩罚与对 car 的保持一致 ([#525](https://github.com/GIScience/openrouteservice/issues/525))
- 修复 Docker 配置中 JAVA_OPTS 和 CATALINA_OPTS 未被正确设置的问题 ([#696](https://github.com/GIScience/openrouteservice/issues/696))
- extra info 中的 suitability 值不再被低估 ([#722](https://github.com/GIScience/openrouteservice/issues/722))
- 修复 round trip 引用错误的 way point 值的问题 ([#724](https://github.com/GIScience/openrouteservice/issues/724))
- 修复自行车路径规划的单行道处理 ([#389](https://github.com/GIScience/openrouteservice/issues/389)) [通过集成 BikeCommonFlagEncoder 的 GH PR [#1769](https://github.com/graphhopper/graphhopper/pull/1769/files/ad4fe02d3d9b5deb66dc0b88d02b61b28b52871c)]
### 变更
- 重构算法选择流程
- 为 roundtrip 使用 ALT/A* Beeline。为步行 profile 启用 Core-ALT-only。
- 为所有 profile 启用基于推荐权重的 CH 和 Core-ALT 预处理。
- 重构 wheelchair 构建器
- 运行 Docker 容器现在会在宿主机上创建 `app.config`，因此可以从 Dockerhub 使用

## [6.1.0] - 2020-03-06
### 新增
- 希伯来语支持（感谢 [citizen-dror](https://github.com/GIScience/openrouteservice/commits?author=citizen-dror) 提供翻译）
- 用于按面积和/或范围限制 avoid_polygon 路径规划选项的配置选项 ([#629](https://github.com/GIScience/openrouteservice/issues/629))
- 用于在使用备选路线算法时限制 count 参数和距离的配置选项 ([#651](https://github.com/GIScience/openrouteservice/issues/651))
- 用于在使用 round trip 路径规划算法时限制距离的配置选项 ([#658](https://github.com/GIScience/openrouteservice/issues/658))
- 为自行车 profile 启用 CALT 路径规划算法 ([#662](https://github.com/GIScience/openrouteservice/issues/662))
- 用于随 API 响应发送条件系统消息的配置选项 ([#664](https://github.com/GIScience/openrouteservice/issues/664))
### 修复
- 使语言 API 参数更一致（ISO 639-1 代码与 IETF 标签）
- 可通过 API 选择尼泊尔语支持
- 修复包含高程时无效的 JSON 和 GeoJSON ([#640](https://github.com/GIScience/openrouteservice/issues/640))
- 为等时圈和 matrix 服务添加 graph 日期并修复输出为 0 的问题 ([#648](https://github.com/GIScience/openrouteservice/issues/648))
- 修复 graph 构建时的内存问题 ([#659](https://github.com/GIScience/openrouteservice/issues/659))
- 改进渡轮连接的 way category 分配 ([#678](https://github.com/GIScience/openrouteservice/issues/678))
### 变更
- 改进法语翻译（directions）
- 使 Docker 配置在自定义方面更灵活 ([#627](https://github.com/GIScience/openrouteservice/issues/627))
- 将 GraphHopper 更新到更新的版本（0.13）
- 为 green 和 quiet 路径规划的 API 描述补充更多细节 ([#632](https://github.com/GIScience/openrouteservice/issues/632))

## [6.0.2] - 2021-09-03
### 变更
- 移除未使用的依赖

## [6.0.1] - 2021-08-30
### 变更
- repo.heigit.org 的 URL 改为 HTTPS

## [6.0.0] - 2019-12-03
### 新增
- 印度尼西亚语翻译
- 允许在文件系统的任意位置通过 -Dors_app_config=<file> 指定配置文件
- 启用 round trip 路径规划 ([#391](https://github.com/GIScience/openrouteservice/issues/391))
- 在 API 中启用备选路线 ([#377](https://github.com/GIScience/openrouteservice/issues/377))
- 在响应中添加有关该 profile 的 graph 上次构建时间的信息 ([#542](https://github.com/GIScience/openrouteservice/issues/542))
- 添加最大吸附半径的默认值
### 修复
- 将 JAVA_OPTS 和 CATALINA_OPTS 作为 Docker 构建参数传递 ([#587](https://github.com/GIScience/openrouteservice/issues/587))
- waytype ferry 的编码 ([#573](https://github.com/GIScience/openrouteservice/issues/573))
- 重构 Core-ALT 算法，使其可在全局使用
### 变更
- 将 GraphHopper 更新到更新的版本（0.12）
- 重写 flag encoder 以使用 GraphHopper 0.12 提供的方法
- 重命名包以遵循命名规范
- 清理若干代码文件
### 弃用
- 移除地理编码端点和代码
- 移除 accessibility 端点和代码
- 从 servlet 过滤器中移除 Brotil encoder

## [5.0.2] - 2019-07-29
### 新增
- 在 api-tests 中添加 gpx schema 校验器，在修复 ([#496](https://github.com/GIScience/openrouteservice/issues/496)) 缺陷的同时测试所有 gpx 输出
- 添加有关路线所穿越国家的信息 ([#349](https://github.com/GIScience/openrouteservice/issues/349))
- 添加使用 sonarcloud 扫描 master（2019-11-29）
### 修复
- isochrone reachfactor 现在给出更符合实际的结果 ([#325](https://github.com/GIScience/openrouteservice/issues/325))
- 修复 api v2 错误的 gpx 头 ([#496](https://github.com/GIScience/openrouteservice/issues/496))
- 确保外部存储包含所有边 ID 的条目 ([#535](https://github.com/GIScience/openrouteservice/issues/535))
- 在 AvoidBordersCoreEdgeFilter 中调用 BordersStorage 前先检查其是否存在
- 在 LM 选择权重中考虑 shortcut 方向 ([#550](https://github.com/GIScience/openrouteservice/issues/550))
- 更新 Matrix api v2 响应以正确显示 sources ([#560](https://github.com/GIScience/openrouteservice/issues/560))
- 在 LM 选择权重中检查空指针 ([#550](https://github.com/GIScience/openrouteservice/issues/550))
- 在 app.config.sample 中权重选项使用逗号而非竖线 ([#564](https://github.com/GIScience/openrouteservice/issues/564))
- 路径规划时点未找到则更新点引用 ([#567](https://github.com/GIScience/openrouteservice/issues/567))
- 修复在路径规划中请求 extra info 时的并发问题 ([#571](https://github.com/GIScience/openrouteservice/issues/571))
### 变更
- 将步行和徒步 flag encoder 移入 ORS 核心系统 ([#440](https://github.com/GIScience/openrouteservice/issues/440))
- 移除路径优化代码 ([#499](https://github.com/GIScience/openrouteservice/issues/499))
- 生成小等时圈时缩短等时圈邻域点搜索的距离 ([#494](https://github.com/GIScience/openrouteservice/issues/494))
- 移除废弃的存储 ([#536](https://github.com/GIScience/openrouteservice/issues/536))
- 重构针对某些路径规划请求参数回退到与预处理无关的算法
- 从 app.config.sample 中移除一些默认 landmark 集

## [5.0.1] - 2019-04-08
### 新增
- CALT 路径规划算法——不用于生产 ([Issue #433](https://github.com/GIScience/openrouteservice/issues/433))
- 使 openrouteservice 的 docker 与 docker-compose 部署更可定制 ([Issue #434](https://github.com/GIScience/openrouteservice/issues/434))
- 添加在配置文件中预先定义通用及每个所用 profile 的标准最大搜索半径的功能 ([Issue #418](https://github.com/GIScience/openrouteservice/issues/418))
### 修复
- 修复 APIv2 的 GPX 输出。自 api v2 发布以来它一直是损坏的。([Issue #533](https://github.com/GIScience/openrouteservice/issues/533))
- 修复 GH fork 中的 SRTM URL ([#394](https://github.com/GIScience/openrouteservice/issues/394))
- 修复资源的 classpath 问题，Windows 构建现已可用 ([#489](https://github.com/GIScience/openrouteservice/issues/489))
- isochrone geojson bbox 现在符合格式要求 ([#493](https://github.com/GIScience/openrouteservice/issues/493))
- v2 等时圈现在遵循 app.config 中的 max_locations ([#482](https://github.com/GIScience/openrouteservice/issues/482))
- 更新文档以反映正确的等时圈平滑算法 ([Issue #471](https://github.com/GIScience/openrouteservice/issues/471))
- 当 geometry_simplify=true 时支持超过 2 个 waypoint ([#457](https://github.com/GIScience/openrouteservice/issues/457))
- 使 wheelchair profile 仅在 bridleway 被设置为可供步行或轮椅通行时才经过 ([#415](https://github.com/GIScience/openrouteservice/issues/415))
- 修复配置文件中设置 `routing_name` 时构建失败的缺陷 ([#424](https://github.com/GIScience/openrouteservice/issues/424))
- 修复途径穿越三个多边形的边境通行问题 ([#491](https://github.com/GIScience/openrouteservice/issues/491))
### 变更
- 更新 pom 以始终构建 ors.war ([Issue #432](https://github.com/GIScience/openrouteservice/issues/432))
- 替换与 Java >8 不兼容的包的使用 ([#474](https://github.com/GIScience/openrouteservice/issues/474))
- 更新 Matrix，改为限制可计算的最大路线数而非地点数 ([#518](https://github.com/GIScience/openrouteservice/issues/518))
### 弃用
- 移除为交通权重原型插入的代码，因为它未被使用，且使 GH 更新更复杂。


## [5.0.0] - 2019-02-25
### 新增
- 更新 api 代码以使用 Spring 框架，并添加 v2 api ([Issue #233](https://github.com/GIScience/openrouteservice/issues/233))
- 为路径规划中的 avoid_countries 选项支持 ISO 3166-1 Alpha-2 / Alpha-3 代码 ([Issue #195](https://github.com/GIScience/openrouteservice/issues/195))
- 支持自由手绘路线选项/跳过路段 ([Issue #167](https://github.com/GIScience/openrouteservice/issues/167))
- 在 matrix 服务中添加检查，确保请求的地点位于 graph 的边界区域内 ([Issue #408](https://github.com/GIScience/openrouteservice/issues/408))
- 使 openrouteservice 的 docker 与 docker-compose 部署更可定制 ([Issue #434](https://github.com/GIScience/openrouteservice/issues/434))
- 支持 GH alternative_route 算法 (Issue #377)
### 修复
- 修复此前无效果的 `geometry_simplify` 参数。`geometry_simplify` 与 `extra_info` 不兼容 ([#381](https://github.com/GIScience/openrouteservice/issues/381))
### 变更
- 将法国的乡村限速更新为 80km/h ([Issue #355](https://github.com/GIScience/openrouteservice/issues/355))
- 调整小等时圈的平滑与缓冲距离，以及针对更小等时圈的其他修复 ([Issue #382](https://github.com/GIScience/openrouteservice/issues/382))
- 更新 pom 以使用正确的 opengeo 仓库，并调整顺序使其位于列表最后，同时使用最新的 ORS-Graphhopper 库 ([Issue #398](https://github.com/GIScience/openrouteservice/issues/398))
- 添加 /directions 作为路径规划端点 ([Issue #384](https://github.com/GIScience/openrouteservice/issues/384))
- 移除以下避免要素：pavedroads、unpavedroads、tunnels、tracks 和 hills，以及设置最高速度的选项；对于自行车和步行 profile，移除指定 fitness level 和最大坡度等难度设置的选项 ([issue #396](https://github.com/GIScience/openrouteservice/issues/396))
- 更新 pom 以始终构建 ors.war ([Issue #432](https://github.com/GIScience/openrouteservice/issues/432))

## [4.7.2] - 2018-12-10
### 新增
- 为 RouteSearchParameters.class() 添加单元测试（[同时修复 Issue #291](https://github.com/GIScience/openrouteservice/issues/291)）
- 添加在路线响应中返回警告信息的能力，当基于扩展存储的警告条件被满足时，可用于向用户展示信息。
- 添加 RoadAccessRestrictions 扩展存储作为警告扩展存储，用于路线经过带通行限制的 way 时提示 ([Issue #342](https://github.com/GIScience/openrouteservice/issues/342))
### 修复
- 如果住宅区惩罚使速度降低到 <5，则将其设为 5
- 在 RouteSearchParameters 中新增 ParameterValueException，用于 profile 为 driving-car 且选项中设置了 profile_params 的情况 ([Issue #291](https://github.com/GIScience/openrouteservice/issues/291))
- 修复 API 测试以考虑新增的 ParameterValueException（[同时修复 Issue #291](https://github.com/GIScience/openrouteservice/issues/291)）
- 改进编码尺寸/重量道路限制的值的范围与分辨率，以便在设置相应 hgv 参数时能正确解析 ([fixes issue #263](https://github.com/GIScience/openrouteservice/issues/263))
- 修复路线位于南半球时的空 BBox 错误 ([Issue #348](https://github.com/GIScience/openrouteservice/issues/348))
- 考虑 hgv 子 profile 特有的通行限制 ([fixes issue #235](https://github.com/GIScience/openrouteservice/issues/235))
- 正确解析所有通行费，尤其是 hgv 专用的通行费 ([fixes issue #358](https://github.com/GIScience/openrouteservice/issues/358))
- 更新步行 way 过滤器的通行限制检查
### 变更
- 允许 car 和 hgv 通行 access=destination 道路 ([Issue #342](https://github.com/GIScience/openrouteservice/issues/342))

## [4.7.1] - 2018-10-24
### 新增
在 readme 中添加不使用 Docker 安装的说明 ([Issue #272](https://github.com/GIScience/openrouteservice/issues/272))
为等时圈 API 添加 area_units，因为原有单位具有误导性 ([Issue #272](https://github.com/GIScience/openrouteservice/issues/272))
### 修复
- 使用度量 crs 时等时圈的面积计算 ([Issue #130](https://github.com/GIScience/openrouteservice/issues/130))
- 降低 bike-regular 的最高速度以获得更符合实际的可达性分数 ([Issue #325](https://github.com/GIScience/openrouteservice/issues/325))
- 修复请求等时圈人口数据时的自相交多边形 ([Issue #297](https://github.com/GIScience/openrouteservice/issues/297))
- 将等时圈响应中的中心改为 graph 上吸附后的边坐标 ([Issue #336](https://github.com/GIScience/openrouteservice/issues/336))
- 启用 HGV 轴载限制 ([Issue #262](https://github.com/GIScience/openrouteservice/issues/262))
### 变更
- 修改供 docker 使用的 app.config.sample，以考虑拆分 profile ([Issue #320](https://github.com/GIScience/openrouteservice/issues/320))
- 修改 pom.xml 中的次要信息
- 更新 API 测试起始坐标，使其位于道路上 ([Issue #328](https://github.com/GIScience/openrouteservice/issues/328))

## [4.7.0] - 2018-10-10
### 新增
- 移除 locations 代码，因为该功能未来将由 openpoiservice 处理 ([Issue #120](https://github.com/GIScience/openrouteservice/issues/120))
- 移除 Geocoding 代码，因为该功能将由 geocoder 服务而非 ORS 内部处理
- 添加等时圈平滑选项 ([Issue #137](https://github.com/GIScience/openrouteservice/issues/137))
- 添加 osm way id 的 ExtraInfo 存储，以便该信息可以针对边进行存储（和访问）([Issue #217](https://github.com/GIScience/openrouteservice/issues/217))
- 添加新的 GeometryUtility 函数及其单元测试，用于计算一组坐标的 bbox ([Issue #241](https://github.com/GIScience/openrouteservice/issues/241))
- 支持 60 度北/南纬以上及以下的高程数据。运行自己的实例时请确保指定 `elevation_provider: multi`（而非仅 'cgiar'）([Issue #220](https://github.com/GIScience/openrouteservice/issues/220))
- 支持在各种路径生成过程之间保留高程源数据——在你的 app.config 中添加：`elevation_cache_clear: false`
- 支持新的左/右转指令

### 修复
- 修正重型车辆存储中判断车辆类型标志的逻辑 ([Issue #211](https://github.com/GIScience/openrouteservice/issues/211))
- 使 OSM "key:access" 标签值对重型车辆 profile 生效 ([Issue #209](https://github.com/GIScience/openrouteservice/issues/209))
- 修复 avoid polygons 排除了本应接受的道路的问题 ([Issue #95](https://github.com/GIScience/openrouteservice/issues/95))
- 更新代码以取消指令合并，因为合并会导致重要转向指令缺失 ([Issue #177](https://github.com/GIScience/openrouteservice/issues/177))
- 补上到达指令缺失的翻译 ([Issue #171](https://github.com/GIScience/openrouteservice/issues/171))
- 更新代码，使计算各边速度时考虑加速度 ([Issue #178](https://github.com/GIScience/openrouteservice/issues/178))
- 修复 gpx 中缺少 rte 标签的问题 ([Issue #196](https://github.com/GIScience/openrouteservice/issues/196))
- 修复 gpx 校验错误 ([Issue #168](https://github.com/GIScience/openrouteservice/issues/168))
- 添加单位换算，使等时圈响应使用用户指定的单位 ([issue #91](https://github.com/GIScience/openrouteservice/issues/91))
- 支持在错误响应中报告多个未找到的点 ([issue #246](https://github.com/GIScience/openrouteservice/issues/246))
- 修复错误的边界框错误 ([Issue #241](https://github.com/GIScience/openrouteservice/issues/241))
- 修复山地自行车 profile 从不使用 contraction hierarchies 的问题。

### 变更
- 将内部 graphhopper 库从 0.9.x 更新到 0.10.1，并减少自定义实现和特性的数量。
这意味着 openrouteservice 的本版本中某些先前特性不再可用。这些调整大部分
在底层进行，对任何人来说都不会察觉。话虽如此，当然有**一个例外**：
你需要为每种车辆创建单独的 profile。在此前的版本中，可以将多种车辆（如
bike、road bike、e-bike 和 mtb）合并到单个 ors-profile 中——这已不再可能。取而代之，你需要为 bike 创建一个
profile，为 mtb 创建一个，为 road bike 创建一个，依此类推。  
- 更新/重构 road bike flagencoder，使其更适合公路骑行爱好者。请注意，
生成的路线可能不符合当地法规——尤其是当 "Biking trails are obligated to be use"（必须使用自行车道）时
- 重构部分边过滤器，并清理初始化它们的代码
- 更新 wheelchair profile，以更好地处理宽度、路缘高度以及人行道
附着于道路等情况。([Issue #162](https://github.com/GIScience/openrouteservice/issues/162))
- 将 "Sand" surface 编码替换为 "Paving stone"
- 修改 bbox api 测试 ([Issue #241](https://github.com/GIScience/openrouteservice/issues/241))
- 修改内部 bbox 的计算方式 ([Issue #241](https://github.com/GIScience/openrouteservice/issues/241))
- 将许可证从 apache 2.0 改为 LGPL3 ([PR #296](https://github.com/GIScience/openrouteservice/issues/296))

### 弃用
- 从 web.xml 中移除对 locations 和 accessibility 服务的引用 ([Issue #186](https://github.com/GIScience/openrouteservice/issues/186))

## [4.5.1] - 2018-03-20
### 修复
- 修复等时圈请求超过最大范围时参数顺序颠倒的问题 ([Issue #126](https://github.com/GIScience/openrouteservice/issues/126))
- 将 geojson 导出中的坐标精度从 4 位小数修正为 6 位小数 ([Issue #136](https://github.com/GIScience/openrouteservice/issues/136))
- 修复导出为 geojson 时 instructions='false' 引发的错误 ([Issue #138](https://github.com/GIScience/openrouteservice/issues/138))
- 修复 geojson 输出中缺少 summary 的问题 ([Issue #139](https://github.com/GIScience/openrouteservice/issues/139))
- 修复在指令中使用较大环岛出口编号时的错误 ([Issue #145](https://github.com/GIScience/openrouteservice/issues/145))

### 变更
- 更新地点之间无法找到路线时的路径规划错误响应码 ([Issue #144](https://github.com/GIScience/openrouteservice/issues/144))
- 更新日志记录方式，使堆栈跟踪仅在启用调试日志时输出 ([Issue #148](https://github.com/GIScience/openrouteservice/issues/148))
- 更新未找到地址时地理编码的错误响应 ([Issue #134](https://github.com/GIScience/openrouteservice/issues/134))


## [4.5.0] - 2018-02-27
### 新增
- 新增限制路线不穿越所有边界、受控边界或特定国家边界的功能 ([Issue #41](https://github.com/GIScience/openrouteservice/issues/41))
- 为路径规划导出添加 GeoJson 导出 ([Issue #54](https://github.com/GIScience/openrouteservice/issues/54))
- 添加全局导出类，用于统一存放所有导出 ([Issue #123](https://github.com/GIScience/openrouteservice/issues/123))
- 在使用非标准权重时，可指定 matrix 请求的最大地点数 ([Issue #94](https://github.com/GIScience/openrouteservice/issues/94))

### 修复
- 修复环岛出口未被正确识别时的异常 ([Issue #89](https://github.com/GIScience/openrouteservice/issues/89))
- 在使用非标准权重时，可指定 matrix 请求的最大地点数 ([Issue #94](https://github.com/GIScience/openrouteservice/issues/94))
- 若反向地理编码未找到地址，Geocoder 现在返回 404 响应 ([Issue #113](https://github.com/GIScience/openrouteservice/issues/113))
- 修复错误码 ([Issue #109](https://github.com/GIScience/openrouteservice/issues/109))
- 正确查询等时圈的人口统计数据 ([Issue #106](https://github.com/GIScience/openrouteservice/issues/106))

### 变更
- 更改 RoutingProfile，以确保每当查询 pop_total 或 pop_area 时，两者都出现在属性中 ([Issue #106](https://github.com/GIScience/openrouteservice/issues/106))
- 包含绕行系数的响应现在使用 "detourfactor" 而非 "detour_factor" ([Issue #61](https://github.com/GIScience/openrouteservice/issues/61))
- 将 gpx 导出改为使用新的全局导出处理器 ([Issue #123](https://github.com/GIScience/openrouteservice/issues/123))

### 弃用
- getStatisticsOld | 与旧统计库关联 ([Issue #106](https://github.com/GIScience/openrouteservice/issues/106))
- geometryToWKB | 与旧统计库关联 ([Issue #106](https://github.com/GIScience/openrouteservice/issues/106))

## [4.4.2] - 2018-01-31
### 新增
- 支持获取 GPX 格式的路线 ([Issue #8](https://github.com/GIScience/openrouteservice/issues/8))
- 支持从 OSM 节点读取 HGV 标签 ([Issue #49](https://github.com/GIScience/openrouteservice/issues/49))
- 请求中无需再添加 optimisation 参数 ([PR #87](https://github.com/GIScience/openrouteservice/issues/87))
- 可选择在响应中返回用于生成 graph 的 osm 文件的 md5 ([Issue #48](https://github.com/GIScience/openrouteservice/issues/48))

### 修复
- 更新代码，在 continue_straight=true 时不使用空的 bearings ([Issue #51](https://github.com/GIScience/openrouteservice/issues/51))
- 修复仅当提供的 HGV 限制少于三项时才被考虑的问题 ([Issue #75](https://github.com/GIScience/openrouteservice/issues/75))
- RPHAST 性能优化 ([Issue #64](https://github.com/GIScience/openrouteservice/issues/64))
- 更新城市区域的时长计算 ([Issue #44](https://github.com/GIScience/openrouteservice/issues/44))
- 增大数据库连接的 hikari 连接池大小 ([PR #52](https://github.com/GIScience/openrouteservice/issues/52))

## [4.4.1] - 2017-10-12

### 新增
- 支持在不在内存中持有 graph 的情况下计算并刷写 graph。

### 修复
- 可选择为特定 waypoint 定义 bearings。

## [4.4.0] - 2017-10-09
### 新增
- 更新功能测试。
- 自行车 profile 可传入 bearings 参数。
- 自行车 profile 可传入 radiuses 参数。
- 自行车 profile 可传入 continue forward 参数。
- 路径规划中考虑 OSM 标签 smoothness=impassable。
- 在等时圈中添加统计提供者的首个原型。

### 修复
- 在 DownwardSearchEdgeFilter 中移除对最高节点的跟踪。
- 当 optimized=true 且无可用 profile 时无法找到合适的路由 profile。
- 移除 MultiTreeMetricsExtractor 中可能出现的 Hash 函数冲突。
- ConcaveBallsIsochroneMapBuilder 中的小幅提速修复。
- 修复 RPHAST 中位置位于单行道上的缺陷。
- 当传入 optimized=false 时考虑转向限制。

[unreleased]: https://github.com/GIScience/openrouteservice/compare/v1.0...HEAD
[1.0]: https://github.com/GIScience/openrouteservice/compare/v9.3.0...v1.0
[9.3.0]: https://github.com/GIScience/openrouteservice/compare/v9.2.0...v9.3.0
[9.2.0]: https://github.com/GIScience/openrouteservice/compare/v9.1.2...v9.2.0
[9.1.2]: https://github.com/GIScience/openrouteservice/compare/v9.1.1...v9.1.2
[9.1.1]: https://github.com/GIScience/openrouteservice/compare/v9.1.0...v9.1.1
[9.1.0]: https://github.com/GIScience/openrouteservice/compare/v9.0.0...v9.1.0
[9.0.0]: https://github.com/GIScience/openrouteservice/compare/v8.2.0...v9.0.0
[8.2.0]: https://github.com/GIScience/openrouteservice/compare/v8.1.2...v8.2.0
[8.1.3]: https://github.com/GIScience/openrouteservice/compare/v8.1.2...v8.1.3
[8.1.2]: https://github.com/GIScience/openrouteservice/compare/v8.1.1...v8.1.2
[8.1.1]: https://github.com/GIScience/openrouteservice/compare/v8.1.0...v8.1.1
[8.1.0]: https://github.com/GIScience/openrouteservice/compare/v8.0.1...v8.1.0
[8.0.1]: https://github.com/GIScience/openrouteservice/compare/v8.0.0...v8.0.1
[8.0.0]: https://github.com/GIScience/openrouteservice/compare/v7.1.1...v8.0.0
[7.1.1]: https://github.com/GIScience/openrouteservice/compare/v7.1.0...v7.1.1
[7.1.0]: https://github.com/GIScience/openrouteservice/compare/v7.0.1...v7.1.0
[7.0.1]: https://github.com/GIScience/openrouteservice/compare/v7.0.0...v7.0.1
[7.0.0]: https://github.com/GIScience/openrouteservice/compare/v6.8.3...v7.0.0
[6.8.3]: https://github.com/GIScience/openrouteservice/compare/v6.8.2...v6.8.3
[6.8.2]: https://github.com/GIScience/openrouteservice/compare/v6.8.1...v6.8.2
[6.8.1]: https://github.com/GIScience/openrouteservice/compare/v6.8.0...v6.8.1
[6.8.0]: https://github.com/GIScience/openrouteservice/compare/v6.7.1...v6.8.0
[6.7.1]: https://github.com/GIScience/openrouteservice/compare/v6.7.0...v6.7.1
[6.7.0]: https://github.com/GIScience/openrouteservice/compare/v6.6.4...v6.7.0
[6.6.4]: https://github.com/GIScience/openrouteservice/compare/v6.6.3...v6.6.4
[6.6.3]: https://github.com/GIScience/openrouteservice/compare/v6.6.2...v6.6.3
[6.6.2]: https://github.com/GIScience/openrouteservice/compare/v6.6.1...v6.6.2
[6.6.1]: https://github.com/GIScience/openrouteservice/compare/v6.6.0...v6.6.1
[6.6.0]: https://github.com/GIScience/openrouteservice/compare/v6.5.0...v6.6.0
[6.5.0]: https://github.com/GIScience/openrouteservice/compare/v6.4.4...v6.5.0
[6.4.4]: https://github.com/GIScience/openrouteservice/compare/v6.4.3...v6.4.4
[6.4.3]: https://github.com/GIScience/openrouteservice/compare/v6.4.2...v6.4.3
[6.4.2]: https://github.com/GIScience/openrouteservice/compare/v6.4.1...v6.4.2
[6.4.1]: https://github.com/GIScience/openrouteservice/compare/v6.4.0...v6.4.1
[6.4.0]: https://github.com/GIScience/openrouteservice/compare/v6.3.7...v6.4.0
[6.3.7]: https://github.com/GIScience/openrouteservice/compare/v6.3.6...v6.3.7
[6.3.6]: https://github.com/GIScience/openrouteservice/compare/v6.3.5...v6.3.6
[6.3.5]: https://github.com/GIScience/openrouteservice/compare/v6.3.4...v6.3.5
[6.3.4]: https://github.com/GIScience/openrouteservice/compare/v6.3.3...v6.3.4
[6.3.3]: https://github.com/GIScience/openrouteservice/compare/v6.3.2...v6.3.3
[6.3.2]: https://github.com/GIScience/openrouteservice/compare/v6.3.1...v6.3.2
[6.3.1]: https://github.com/GIScience/openrouteservice/compare/v6.3.0...v6.3.1
[6.3.0]: https://github.com/GIScience/openrouteservice/compare/v6.2.1...v6.3.0
[6.2.1]: https://github.com/GIScience/openrouteservice/compare/v6.2.0...v6.2.1
[6.2.0]: https://github.com/GIScience/openrouteservice/compare/v6.1.1...v6.2.0
[6.1.1]: https://github.com/GIScience/openrouteservice/compare/v6.1.0...v6.1.1
[6.1.0]: https://github.com/GIScience/openrouteservice/compare/v6.0.0...v6.1.0
[6.0.0]: https://github.com/GIScience/openrouteservice/compare/v5.0.2...v6.0.0
[5.0.2]: https://github.com/GIScience/openrouteservice/compare/v5.0.1...v5.0.2
[5.0.1]: https://github.com/GIScience/openrouteservice/compare/5.0.0...v5.0.1
[5.0.0]: https://github.com/GIScience/openrouteservice/compare/v4.7.2...5.0.0
[4.7.2]: https://github.com/GIScience/openrouteservice/compare/4.7.1...v4.7.2
[4.7.1]: https://github.com/GIScience/openrouteservice/compare/4.7.0...4.7.1
[4.7.0]: https://github.com/GIScience/openrouteservice/compare/4.5.1...4.7.0
[4.5.1]: https://github.com/GIScience/openrouteservice/compare/4.5.0...4.5.1
[4.5.0]: https://github.com/GIScience/openrouteservice/compare/4.4.2...4.5.0
[4.4.2]: https://github.com/GIScience/openrouteservice/compare/4.4.1...4.4.2
[4.4.1]: https://github.com/GIScience/openrouteservice/compare/4.4.0...4.4.1
[4.4.0]: https://github.com/GIScience/openrouteservice/compare/4.3.0...4.4.0
