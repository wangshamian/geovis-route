# 越野路径（Off Road Routing）

越野路径是本实例在标准 openrouteservice 端点之外提供的扩展能力：**不依赖 OSM 路网**，直接依据数字高程模型（DEM）的坡度，在任意起终点之间搜索一条可通行的折线路径。

适用于山地、林区、荒野、海岛等无路网或路网稀疏区域，也适合快速判断两点之间“地形上是否走得通”。

::: tip 与 Directions 端点的区别

| 维度 | Directions（`/v2/directions`） | 越野路径（`/v2/offroad`） |
| --- | --- | --- |
| 依赖数据 | OSM 路网图 | SRTM 高程瓦片 |
| 通行判定 | 道路等级、交通规则、路面属性 | 网格坡度是否超过阈值 |
| 结果形态 | 贴合道路的 LineString + 转向指令 | 平滑折线，无转向指令 |
| 适用区域 | 有路网覆盖的区域 | 任意有高程数据的区域 |
| 计算耗时 | 毫秒级 | 数百毫秒至数秒（随网格数量增长） |

:::

## 1. 快速开始

### 1.1 调用接口

```bash
curl -X POST http://localhost:8082/ors/v2/offroad \
  -H "Content-Type: application/json" \
  -d '{
    "coordinates": [[121.517089, 25.047764], [121.572668, 25.026994]],
    "grid_interval": 300,
    "max_slope": 20,
    "vehicle": "offroad-car"
  }'
```

`coordinates` 使用 **WGS84 经纬度**，格式为 `[[lon, lat], ...]`，当前仅支持两个坐标（起点、终点），顺序任意。

### 1.2 使用演示页面

访问 <http://localhost:8082/ors/ors-cesium-test.html>，切换到「越野路径」标签页：

1. 在地球上点击选择起点，再点击选择终点；选满两个点后自动计算。
2. 页面会自动切换为「高德卫星影像」底图，便于判读地形。
3. 结果面板显示距离、预计耗时、坡度统计与障碍数量，下方绘制高程剖面图。
4. 可点击「演示」按钮使用内置的台北示例点。

## 2. 工作原理

### 2.1 总体流程

```
起终点
  ↓ ① 构造走廊式搜索范围
  ↓ ② 按 grid_interval 划分网格，逐格采样高程计算坡度
  ↓ ③ 坡度超过 max_slope 的网格标记为障碍
  ↓ ④ A* 八邻域搜索，得到格心路径
  ↓ ⑤ 视线拉直 → Douglas-Peucker 抽稀 → Chaikin 平滑
  ↓ ⑥ 等距重采样计算高程剖面与统计
输出：LineString + summary + elevation + barriers
```

### 2.2 走廊式搜索范围

如果直接用起终点包围盒扩边，长距离路径的搜索面积会呈平方增长。本实现改为按起终点直线距离 `D` 构造走廊：

```
h = clamp(0.5 × D, 1500 m, 8000 m)
h = max(h, 3 × grid_interval)          // 至少覆盖几个网格
搜索范围 = 起终点包围盒向四周各扩展 h
```

相比“包围盒放大 5 倍”，台北示例的搜索网格由约 4000 个降到约 1240 个，计算量与返回的障碍面数量同步下降。

### 2.3 网格与坡度计算

- 网格行方向为纬度（`y`），列方向为经度（`x`），行列数由 `经纬度跨度 / grid_interval` 向上取整得到。
- 每个网格内（含左下相邻区域）按 1/3 网格边长布设 **12 个规则采样点**，取其中最高点与最低点：

```
坡角 = atan(最高点与最低点的高差 / 两点的水平距离)
```

- 坡角 ≤ `max_slope` 的网格可通行，> `max_slope` 的网格记为障碍。
- **起终点所在网格始终放行**，避免用户选点恰好落在陡坡上直接无解。

### 2.4 A* 搜索

- 八邻域扩展，正交移动代价 1，斜向移动代价 `√2`。
- 启发函数使用 octile 距离，与移动代价一致，保证搜索到的是网格尺度上的最短路径：

```
h = (dx + dy) + (√2 − 2) × min(dx, dy)
```

- 斜向移动时，若两侧正交网格同时是障碍，则禁止穿越，避免从障碍拐角“挤过去”。
- 完成条件为出队节点等于终点网格；不存在可行路径时返回空几何。

### 2.5 路径后处理

格心连线天然带有阶梯状锯齿，且比真实直线略长，因此做了三步处理：

1. **视线拉直**：从当前锚点出发，尽量连接到最远的、连线全程落在可通行网格内的路径点（每格采样 4 次校验）。
2. **Douglas-Peucker 抽稀**：垂直距离阈值取 `0.5 × grid_interval`，在等距投影的米制坐标上计算。
3. **Chaikin 平滑**：对抽稀后的折线做一次切角平滑，使拐角过渡自然；首尾点保持为真实选点。

::: warning 注意
抽稀后的路径点数量可能很少（例如地形平缓时为 2 个点，即起终点直连），这属于正常结果，表示直线方向确实不存在坡度超限的网格。
:::

### 2.6 高程剖面与坡度统计

- 沿最终路径按等距重采样，采样间距不小于 30 m，最多返回 128 个采样点。
- 每个采样点包含累计距离、高程与原始 WGS84 经纬度，供前端剖面与地图联动使用。
- 剖面坡度由相邻采样点计算，与网格坡度是两个不同尺度的度量：

| 指标 | 计算方式 | 尺度 |
| --- | --- | --- |
| 网格坡度（通行判定） | 格内 12 点最高/最低点高差 ÷ 水平距离 | 约 `grid_interval` |
| 剖面坡度（`summary.max_slope`） | 相邻剖面点高差 ÷ 水平距离 | 约 30–100 m |

因此 `summary.max_slope` **可能大于** `max_slope`，后者是判定阈值，前者是路径沿线的实际地形坡度。

## 3. 接口说明

### 3.1 请求

`POST /v2/offroad`，`Content-Type: application/json`

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `coordinates` | `number[][]` | 是 | — | `[[lon, lat], ...]`，至少 2 个点，当前只取首尾两个点 |
| `grid_interval` | `number` | 否 | `300` | 网格边长（米），允许范围 `10 ~ 1000` |
| `max_slope` | `number` | 否 | `20` | 最大可通行坡度（度），允许范围 `1 ~ 60` |
| `vehicle` | `string` | 否 | `offroad-car` | 交通工具，用于估算耗时，可选值见下表 |

交通工具与平均通行速度：

| `vehicle` | 含义 | 平均速度 |
| --- | --- | --- |
| `offroad-car` | 越野车 | 20 km/h |
| `offroad-truck` | 越野卡车 | 15 km/h |
| `motorcycle` | 摩托车 | 35 km/h |
| `bicycle` | 自行车 | 12 km/h |
| `foot` | 徒步 | 5 km/h |

`vehicle` 只影响 `summary.duration` 的估算，不改变路径几何与坡度判定。

### 3.2 响应

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `geometry` | GeoJSON LineString | 路径折线，WGS84 经纬度，顺序为起点 → 终点 |
| `grid_interval` | number | 实际使用的网格边长 |
| `max_slope` | number | 实际使用的坡度阈值 |
| `vehicle` | string | 实际使用的交通工具 |
| `speed_kmh` | number | 该交通工具的平均速度（km/h） |
| `point_count` | number | 路径折线点数量 |
| `summary.distance` | number | 路径长度（米），保留 1 位小数 |
| `summary.duration` | number | 预计耗时（秒），按所选交通工具的平均速度估算 |
| `summary.max_slope` | number | 剖面最大坡度（度） |
| `summary.avg_slope` | number | 剖面平均坡度（度），按距离加权 |
| `summary.ascend` | number | 累计爬升（米） |
| `summary.descend` | number | 累计下降（米） |
| `summary.barrier_count` | number | 坡度超限的网格数量（合并前） |
| `message` | string | 仅在无可行路径时返回，说明可调整的方向 |
| `elevation.distance` | number | 剖面总长度（米） |
| `elevation.min` / `elevation.max` | number | 沿线最低 / 最高高程（米） |
| `elevation.samples` | object[] | 剖面采样点：`distance`（米）、`ele`（米）、`lon`、`lat` |
| `barriers` | GeoJSON Polygon / MultiPolygon | 坡度超限区域，相邻网格已合并为面 |

当高程数据不可用时，响应中不会包含 `elevation` 与 `summary` 中的坡度/爬升字段。

### 3.3 响应示例

```json
{
  "geometry": {
    "type": "LineString",
    "coordinates": [
      [121.517089, 25.047764],
      [121.572668, 25.026994]
    ]
  },
  "grid_interval": 300.0,
  "max_slope": 20.0,
  "vehicle": "offroad-car",
  "speed_kmh": 20.0,
  "point_count": 2,
  "summary": {
    "distance": 6057.0,
    "duration": 1090,
    "max_slope": 35.5,
    "avg_slope": 3.6,
    "ascend": 227.0,
    "descend": 168.0,
    "barrier_count": 26
  },
  "elevation": {
    "distance": 6057.0,
    "min": 5.0,
    "max": 88.0,
    "ascend": 227.0,
    "descend": 168.0,
    "max_slope": 35.5,
    "avg_slope": 3.6,
    "samples": [
      { "distance": 0.0, "ele": 6.0, "lon": 121.517089, "lat": 25.047764 },
      { "distance": 476.9, "ele": 12.0, "lon": 121.521465, "lat": 25.046129 }
    ]
  },
  "barriers": {
    "type": "MultiPolygon",
    "coordinates": [ [ [ [121.5159, 25.0451], [121.5186, 25.0451], [121.5186, 25.0480], [121.5159, 25.0451] ] ] ]
  }
}
```

### 3.4 错误响应

错误统一返回 HTTP 400，响应体为 `{"error": "错误说明"}`：

| 场景 | 错误信息 |
| --- | --- |
| 坐标不足 | `coordinates 至少需要起点和终点两个坐标` |
| 坐标格式错误 | `坐标格式应为 [经度, 纬度]` |
| 坐标为空或 NaN | `坐标不能为空或 NaN` |
| 坐标越界 | `坐标超出范围：经度需在 -180~180，纬度需在 -90~90` |
| 网格间距越界 | `grid_interval 需在 10~1000 米之间` |
| 坡度阈值越界 | `max_slope 需在 1~60 度之间` |
| 交通工具不支持 | `不支持的 vehicle：{value}，可选值：...` |
| 网格数量超限 | `网格数量 {n} 超过上限 250000，请增大 grid_interval 或缩短起终点距离` |
| 采样阶段超时 | `越野路径计算超时（超过 30000 毫秒），请增大网格间距或缩短起终点距离` |
| 搜索阶段超时 | `越野路径搜索超时（超过 30000 毫秒），请增大网格间距或缩短起终点距离` |

以下情况返回 HTTP 200 而非错误：坡度阈值过严导致起终点之间不存在可行路径。此时 `geometry.coordinates` 为空数组，`point_count` 为 0，并附带 `message` 字段说明原因。

## 4. 参数选择与调优

### 4.1 grid_interval 怎么选

网格越小越精细，但计算量与障碍数量按面积增长：

```
h     = max(clamp(0.5 × D, 1500, 8000), 3 × grid_interval)
cols  = ceil((经度跨度 × 111320 × cos(纬度) + 2h) / grid_interval)
rows  = ceil((纬度跨度 × 111320 + 2h) / grid_interval)
网格数 ≈ rows × cols
```

| 场景 | 建议值 | 说明 |
| --- | --- | --- |
| 城市周边、十公里级 | `200 ~ 300` | 默认值，响应通常在 1 秒内 |
| 山地、峡谷等起伏剧烈 | `100 ~ 150` | 能识别更细的山脊与沟谷 |
| 大范围踏勘、几十公里 | `500 ~ 1000` | 减少网格数量，结果更粗略 |
| 小范围精查（数公里内） | `20 ~ 50` | 注意网格上限，可能返回 400 |

网格总数上限为 **250000**，超限直接返回 400，不会进入计算。单次计算超时上限为 **30000 ms**。

### 4.2 max_slope 怎么选

| 通行对象 | 建议阈值 |
| --- | --- |
| 常规越野车 | `15 ~ 20` |
| 硬派越野 / 履带车 | `25 ~ 30` |
| 徒步 / 山地作业 | `30 ~ 40` |
| 快速连通性判断 | `45 ~ 60` |

阈值过小时，山地示例可能只剩极少数连通网格，容易出现“无可行路径”；此时应优先提高阈值，而不是继续缩小网格。

### 4.3 常见现象

| 现象 | 原因与处理 |
| --- | --- |
| 路径是两点直连（2 个点） | 直线方向无坡度超限网格，属正常结果；可减小 `grid_interval` 观察细节 |
| `summary.max_slope` 大于 `max_slope` | 两者尺度不同，见 2.6 节 |
| 返回 400 网格数量超限 | 增大 `grid_interval` 或缩短起终点距离 |
| 返回空路径 | 提高 `max_slope`、增大 `grid_interval`，或更换起终点 |
| 障碍区域覆盖大片区域 | 该区域地形普遍陡峭，或阈值设置过低 |

## 5. 演示页面说明

页面地址：`http://localhost:8082/ors/ors-cesium-test.html`，选择「越野路径」标签页。

### 5.1 控件

| 控件 | 默认值 | 说明 |
| --- | --- | --- |
| 网格间距 | `300` | 对应请求参数 `grid_interval`，修改后在已有起终点时自动重算 |
| 最大坡度 | `20` | 对应请求参数 `max_slope`，单位度，修改后自动重算 |
| 交通工具 | `越野车` | 对应请求参数 `vehicle`，仅影响预计耗时，修改后自动重算 |
| 障碍显示 | 勾选 | 仅切换障碍面可见性，不触发重算 |
| 地形起伏 | `2x` | 三维地形垂直夸张系数，范围 `1 ~ 5x`，1x 为真实比例 |
| 取消 | — | 中止当前计算（AbortController），计算过程中显示已等待秒数 |

### 5.2 结果面板

- 第一行：路径长度与预计耗时。
- 第二行：交通工具、路径点数、网格间距、坡度阈值。
- 第三行：剖面最大/平均坡度、累计爬升、累计下降。
- 第四行：障碍网格数量。
- 下方：高程剖面图。

### 5.3 高程剖面联动

鼠标在剖面图上移动时：

- 剖面图显示对应位置的竖向指示线与高亮点；
- 文本提示当前位置的累计距离与海拔；
- 三维地球上同步显示该采样点的高程标记与数值。

### 5.4 结果导出

| 按钮 | 格式 | 说明 |
| --- | --- | --- |
| 下载 GPX | `.gpx` | 轨迹点来自高程剖面，含 `<ele>` 高程值 |
| 下载 GeoJSON | `.geojson` | `FeatureCollection`，属性包含参数、统计信息与高程摘要 |

::: tip 坐标说明
接口与导出文件均使用 **WGS84** 原始坐标。页面上的「高德 GCJ-02 与 WGS84 互转」开关只影响地图显示与点击选点，不会改变请求与导出数据。
:::

## 6. 性能与限制

### 6.1 性能特征

- 主要开销在网格高程采样（每格 12 次 `getEle`），SRTM 瓦片命中缓存后单次请求通常在数百毫秒级。
- 相邻障碍网格使用 JTS 并集合并，避免返回上千个细小多边形。
- 请求处理在服务端 **串行执行**：SRTM 高程数据源在创建瓦片时不是线程安全的，并发访问同一瓦片会抛 `DataAccess ... has already been created`。前端通过 AbortController 取消旧请求，可减少排队。

### 6.2 已知限制

- 仅支持起点与终点两个坐标，不支持途经点、多点路径。
- 只依据坡度判定通行，不考虑水体、建筑、保护区、道路等级、限高等地物与规则。
- 障碍区域表示“坡度超限网格”，不是“绝对不可通行”的权威结论。
- 结果不含转向指令、路段属性、时间依赖等 Directions 端点具备的信息。
- 预计耗时按所选交通工具的固定平均速度估算，未考虑坡度、路面等实际通行条件。
- 网格分辨率决定地形细节上限；`grid_interval` 越大，越可能漏掉窄山脊、冲沟等局部地形。

## 7. 服务端配置

### 7.1 高程数据源

越野路径不要求路网图包含高程：

1. 若已加载的路网图带高程（3D 图），直接复用其 `ElevationProvider`；
2. 否则按 `ors.engine.elevation` 配置创建独立的 SRTM 数据源并缓存复用。

相关配置示例（`gr-config.yml`）：

```yaml
ors:
  engine:
    elevation:
      cache_path: ./elevation_cache
      data_access: MMAP
```

### 7.2 内部保护参数

`OffRoader` 还支持两个未在 REST 层暴露的 hint，仅供内部调用或后续扩展使用：

| Hint | 默认值 | 说明 |
| --- | --- | --- |
| `max_cells` | `250000` | 单次请求允许的最大网格数量 |
| `timeout_ms` | `30000` | 单次请求的计算超时时间 |

### 7.3 启动示例

```bash
java -jar gr-api/target/ors.jar \
  --logging.file.name=./logs/ors-app.log \
  --ors.engine.graphs_data_access=RAM_STORE
```

## 8. 相关代码

| 位置 | 作用 |
| --- | --- |
| `gr-api/src/main/java/org/heigit/geovisroute/api/controllers/OffRoadAPI.java` | REST 入口、参数校验、统计与高程剖面 |
| `gr-engine/src/main/java/com/graphhopper/offroad/OffRoader.java` | 走廊构造、网格生成、A*、路径平滑、障碍合并 |
| `gr-engine/src/main/java/com/graphhopper/offroad/OffRoadGrid.java` | 网格数据模型 |
| `gr-engine/src/main/java/com/graphhopper/offroad/util/GeoUtils.java` | 距离计算与网格坡度计算 |
| `gr-engine/src/main/java/com/graphhopper/offroad/util/GeoBounds.java` | 边界归一化与按距离扩展 |
| `gr-engine/src/main/java/com/graphhopper/OffRoadResponse.java` | 响应载体 |
| `gr-api/src/main/resources/static/ors-cesium-test.html` | 演示页面（越野路径标签页） |
