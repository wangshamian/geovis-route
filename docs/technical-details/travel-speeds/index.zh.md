# 行驶速度
决定最快路线、等时圈与路线指示的行驶时间等要素的一个主要组成部分是行驶速度。该值根据不同配置以不同方式确定，并可能受到多方面因素的影响。

## 行驶时间计算

每个路段的行驶时间是这样计算的：使用不同[道路类型](https://wiki.openstreetmap.org/wiki/Key:highway)的限速，
并根据道路的不同
[路面](https://wiki.openstreetmap.org/wiki/Key:surface)或[路面
质量](https://wiki.openstreetmap.org/wiki/Key:tracktype)对其进行调整。如果在某一点有多个值适用，则使用最低值。

限速可以通过在[路径规划请求][api_docs]中设置 `maximum_speed` 参数来降低。
通过在 `attributes` 参数中添加 `avgspeed`，可以请求最终的[平均速度](/api-reference/endpoints/directions/route-attributes.zh.md)。

### 驾驶配置（汽车与 HGV）
任何道路的基础行驶速度都基于级联评估确定：
1. 当 OSM 中的 way 上存在明确标注该道路限速的标签（`maxspeed` 或 `maxspeed:forward / maxspeed:backward`）时，
   将其用作基础速度。
2. 如果该 way 带有 `zone:maxspeed` 或 `zone:traffic` 标签，则将其与
   [速度值文件][svf] 中 `max_speeds` 数组里的国家特定值进行比较。
   概览见[国家速度表](country-speeds)。
   该值随后被用作基础速度。
3. 如果以上两项都未设定基础速度，则由道路类型（`highway=`
   标签）决定基础速度。各项值定义在
   [速度值文件][svf] 中。
   [道路类型速度表](waytype-speeds)给出了概览。
4. 如果是小径（`highway=track`），则根据 `tracktype` 标签与[速度值文件][svf]中的值比较来确定基础速度。
   [小径类型速度表](tracktype-speeds)给出了概览。

如果确定的基础速度来自 `maxspeed` 标签或
[国家速度表](country-speeds)（即第 1 步或第 2 步），则会将其降低为原始值的 90%，
因为实际行驶速度更常见的情况是低于最大速度值。[道路类型](waytype-speeds)和
[小径类型](tracktype-speeds)的默认值本身已设置得相当保守，因此不做调整。

随后，还会根据若干其他因素进一步修正：
* 如果定义了路面（`surface=*`），则将路面值设为
  [速度值文件][svf]中定义的对应路面类型值。
  概览见[路面速度表](surface-speeds)。
* 尝试借助加速度修正因子或居住区惩罚来考虑居住区内降低的速度
* 如果驶入环岛则限制速度（依据车道数和
  环岛类型 - 迷你环岛 = 25km/h，1 车道 = 35km/h，2 车道及以上
= 40km/h）

### 自行车配置
自行车配置根据代码中基于路面、highway 类型和小径类型设定的若干默认值来确定速度。_尽管目前自行车配置的资源中确实存在
一个 speed_limits 文件，但这些值并未用于计算_

所用速度的默认值可在 [CommonBikeFlagEncoder][cbfe]（默认值和普通自行车配置）中找到，山地自行车配置见 [MountainBikeFlagEncoder][mbfe]，公路自行车见 [RoadBikeFlagEncoder][rbfe]，电动自行车见
[ElectroBikeFlagEncoder][ebfe]。

请注意，每个自行车配置在 gr-config.json 中设置 `consider_elevation=true` 时
计算出的最大下坡速度值各不相同。目前，该选项在 openrouteservice 在线
服务器上是关闭的，因为它可能导致不理想的路线。

### 步行配置
`foot-*` 配置（步行和徒步）在所有允许的道路类型上的行驶速度设为
5 km/h。
对于 [`sac_scale`](/api-reference/endpoints/directions/extra-info/trail-difficulty.zh.md) 高于
`hiking` 的 way，速度降低为 2 km/h。

允许的道路类型包括可安全使用的 way、最好避开（但仍允许）的 way，以及介于两者之间的其他允许 way：

  |        安全标签        |     建议避开标签     |     其他 highway 标签     |
  |:----------------------:|:----------------:|:-------------------:|
  |         footway        |   trunk          |       cycleway      |
  |         path           | trunk_link       |    unclassified     |
  |         steps          |  primary         |       road        |
  |         pedestrian     |  primary_link    |                   |
  |         living_street  |  secondary       |                   |
  |         track          |  secondary_link  |                   |
  |         residential    |  tertiary        |                   |
  |         service        |  tertiary_link   |                   |

### 轮椅配置
轮椅配置的基础速度为 4km/h，随后会根据
若干参数进行修正。因此，根据人行道的存在情况和道路类型，实际速度范围为 3 至 10km/h

[svf]: https://github.com/GIScience/openrouteservice/tree/main/ors-engine/src/main/resources/resources/services/routing/speed_limits
[cbfe]: https://github.com/GIScience/openrouteservice/blob/main/ors-engine/src/main/java/org/heigit/ors/routing/graphhopper/extensions/flagencoders/bike/CommonBikeFlagEncoder.java#L186
[mbfe]: https://github.com/GIScience/openrouteservice/blob/main/ors-engine/src/main/java/org/heigit/ors/routing/graphhopper/extensions/flagencoders/bike/MountainBikeFlagEncoder.java#L52
[rbfe]: https://github.com/GIScience/openrouteservice/blob/main/ors-engine/src/main/java/org/heigit/ors/routing/graphhopper/extensions/flagencoders/bike/RoadBikeFlagEncoder.java#L85
[ebfe]: https://github.com/GIScience/openrouteservice/blob/main/ors-engine/src/main/java/org/heigit/ors/routing/graphhopper/extensions/flagencoders/bike/ElectroBikeFlagEncoder.java#L41
[api_docs]: https://openrouteservice.org/dev/#/api-docs/v2/directions/{profile}/post
