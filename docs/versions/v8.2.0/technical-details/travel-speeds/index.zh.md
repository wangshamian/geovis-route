# 行驶速度
决定最快路线、等时圈的行驶时间以及路线导航指令等信息的主要因素之一就是行驶速度。该取值的确定方式因配置方案（profile）而异，并且可能受多方面因素影响。

## 行驶时间计算

行驶时间按路段计算：先取不同[道路类型（waytype）](https://wiki.openstreetmap.org/wiki/Key:highway)的限速值，再根据道路的[路面](https://wiki.openstreetmap.org/wiki/Key:surface)或[路面状况](https://wiki.openstreetmap.org/wiki/Key:tracktype)进行修正。如果某处同时适用多个取值，则采用其中的最低值。

限速可以通过在 [directions 请求][api_docs] 中设置 `maximum_speed` 参数来降低。最终的[平均速度](../../api-reference/endpoints/directions/route-attributes.zh.md)可以通过在 `attributes` 参数中加入 `avgspeed` 来获取。

### 机动车配置方案（car 与 HGV）
任意道路的基础行驶速度都基于一套逐级判断的流程：
1. 如果 OSM 中该道路带有明确声明限速的标签（`maxspeed` 或
   `maxspeed:forward / maxspeed:backward`），则将其作为基础速度。
2. 如果该道路带有 `zone:maxspeed` 或 `zone:traffic` 标签，则将其与
   [speed value files][svf] 中 `max_speeds` 数组里的国别取值进行比较。
   概览可参见[国别速度表](country-speeds)。
   随后将该值作为基础速度。
3. 如果以上两者都未能确定基础速度，则由道路类型（`highway=`
   标签）决定基础速度。取值定义在
   [speed value files][svf] 中。
   [道路类型速度表](waytype-speeds)给出了概览。
4. 如果是小径（track，`highway=track`），则根据 `tracktype` 标签与
   [speed value files][svf] 中的取值比较来确定基础速度。
   [tracktype 速度表](tracktype-speeds)给出了概览。

如果确定出的基础速度来自 `maxspeed` 标签或[国别速度表](country-speeds)（即第 1 步或第 2 步），则会将其降至原值的 90%，因为实际行驶速度低于最高限速的情况更为常见。[道路类型（waytype）](waytype-speeds)与 [tracktype](tracktype-speeds) 的默认值本身已取得较为保守，因此不做调整。

在此之后，还会根据一系列其他因素做进一步修正：
* 如果定义了路面（`surface=*`），则把该路段速度设置为 [speed value files][svf]
  中对应路面类型的取值。
  概览可参见[路面速度表](surface-speeds)。
* 尝试通过加速度修正因子或居住区惩罚（residential penalty）来体现居住区内的降速
* 进入环岛时对速度设置上限（依据车道数与环岛类型——迷你环岛 = 25km/h，1 车道 = 35km/h，2 车道及以上
= 40km/h）

### 自行车配置方案
自行车配置方案依据代码中针对路面、highway 类型与 track 类型设定的一系列默认值来确定速度。_尽管目前在自行车配置方案的资源目录中确实存在 speed_limits 文件，但这些取值并未参与计算_

所用速度可参见：[CommonBikeFlagEncoder][cbfe] 中的默认值与普通自行车配置方案，[MountainBikeFlagEncoder][mbfe] 对应山地车配置方案，[RoadBikeFlagEncoder][rbfe] 对应公路车，[ElectroBikeFlagEncoder][ebfe] 对应电动车。

请注意，各自行车配置方案的最大下坡速度取值不同，该值在 gr-config.json 中设置 `consider_elevation=true` 时才会计算。目前 openrouteservice 线上服务器关闭了该选项，因为它可能导致不理想的路线。

### 步行配置方案
`foot-*` 配置方案（步行与徒步）在所有允许的道路类型上的行驶速度均设为
5 km/h。
对于 [`sac_scale`](../../api-reference/endpoints/directions/extra-info/trail-difficulty.zh.md) 高于
`hiking` 的道路，速度降至 2 km/h。

允许的道路类型包括可安全通行的道路、最好避开（但仍然允许）的道路，以及介于两者之间的其他允许道路：

  |        安全标签        |  避开标签        |  其他 highway 标签 |
  |:----------------------:|:----------------:|:-------------------:|
  |         footway        |   trunk          |       cycleway      |
  |         path           | trunk_link       |    unclassified     |
  |         steps          |  primary         |       road        |
  |         pedestrian     |  primary_link    |                   |
  |         living_street  |  secondary       |                   |
  |         track          |  secondary_link  |                   |
  |         residential    |  tertiary        |                   |
  |         service        |  tertiary_link   |                   |

### 轮椅配置方案
轮椅配置方案的基础速度为 4km/h，之后会根据若干参数进行修正。因此，依据人行道的有无与道路类型的不同，实际速度可能在 3 到 10km/h 之间。

[svf]: https://github.com/GIScience/openrouteservice/tree/main/ors-engine/src/main/resources/resources/services/routing/speed_limits
[cbfe]: https://github.com/GIScience/openrouteservice/blob/main/ors-engine/src/main/java/org/heigit/ors/routing/graphhopper/extensions/flagencoders/bike/CommonBikeFlagEncoder.java#L186
[mbfe]: https://github.com/GIScience/openrouteservice/blob/main/ors-engine/src/main/java/org/heigit/ors/routing/graphhopper/extensions/flagencoders/bike/MountainBikeFlagEncoder.java#L52
[rbfe]: https://github.com/GIScience/openrouteservice/blob/main/ors-engine/src/main/java/org/heigit/ors/routing/graphhopper/extensions/flagencoders/bike/RoadBikeFlagEncoder.java#L85
[ebfe]: https://github.com/GIScience/openrouteservice/blob/main/ors-engine/src/main/java/org/heigit/ors/routing/graphhopper/extensions/flagencoders/bike/ElectroBikeFlagEncoder.java#L41
[api_docs]: https://openrouteservice.org/dev/#/api-docs/v2/directions/{profile}/post
