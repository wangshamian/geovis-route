# 通行速度
决定最快路线、等时圈通行时间以及路线指示等内容的另一个主要因素是通行速度。该值因 profile 不同而以不同方式确定，并且会受多种因素影响。

## 通行时间计算

通行时间按每个路段计算：使用不同[道路类型](https://wiki.openstreetmap.org/wiki/Key:highway)的限速，
并根据道路的不同[等级](https://wiki.openstreetmap.org/wiki/Key:tracktype)或
[路面](https://wiki.openstreetmap.org/wiki/Key:surface)进行调整。
如果任一位置有多个值适用，则采用其中最低的值。

可以通过在[请求][apidocs]中设置 `maximum_speed` 参数来降低限速。
通过把 `avgspeed` 添加到 `attributes` 参数，可以请求最终的[平均速度](../Route-Attributes#avgspeed)。

### 驾驶 profile（car 和 HGV）
任何道路的基础通行速度都基于逐级判断：
1. 当 OSM 中该道路上存在明确标注限速的标签（`maxspeed` 或 `maxspeed:forward / maxspeed:backward`）时，
   以此作为基础速度。
2. 如果该道路带有 `zone:maxspeed` 或 `zone:traffic` 标签，则将其与
   [速度值文件][svf] 中 `max_speeds` 数组里针对各国的取值进行比较。
   概览可参见[各国速度表](Country-Speeds)。
   然后把该值用作基础速度。
3. 如果以上两项都未能设定基础速度，则由道路类型（`highway=`
   标签）决定基础速度。取值在
   [速度值文件][svf] 中定义。
   [道路类型速度表](Waytype-Speeds)给出了概览。
4. 如果是一条小径（`highway=track`），则根据 `tracktype` 标签与
   [速度值文件][svf] 中的取值比较来确定基础速度。
   [小径等级速度表](Tracktype-Speeds)给出了概览。

如果确定出的基础速度来自 `maxspeed` 标签或
[各国速度表](Country-Speeds)（即第 1 步或第 2 步），则会降为其原值的 90%，
因为实际行驶速度低于限速值的情况更为常见。[道路类型](Waytype-Speeds)和
[小径等级](Tracktype-Speeds)的默认值本身已经设置得较为保守，因此不做调整。

在此之后，还会根据若干其他因素进一步修正：
* 如果定义了路面（`surface=*`），则把速度设为
  [速度值文件][svf] 中定义的对应路面类型取值。
  概览可参见[路面速度表](Surface-Speeds)。
* 尝试通过加速度修正或住宅区惩罚项来考虑住宅区内降低的车速
* 在进入环岛时限制车速（依据车道数和环岛类型——迷你环岛 = 25km/h，1 条车道 = 35km/h，2 条及以上车道
= 40km/h）

### 自行车 profile
自行车 profile 依据代码中基于路面、highway 类型和小径等级设置的一系列默认值来确定速度。
_尽管目前自行车 profile 的资源中确实存在 speed_limits 文件，但这些值
并未用于计算_

所使用的速度可以在 [CommonBikeFlagEncoder][cbfe] 中查看默认值和普通自行车 profile，在 [MountainBikeFlagEncoder][mbfe] 中查看山地自行车 profile，在 [RoadBikeFlagEncoder][rbfe] 中查看公路自行车，在
[ElectroBikeFlagEncoder][ebfe] 中查看电动自行车。

请注意，当在 gr-config.json 中设置 `consider_elevation=true` 时，会计算最大下坡速度，且各自行车 profile 的该值各不相同。
目前，该选项在 openrouteservice 的线上服务器中已关闭，因为它可能导致不理想的路线。

### 步行 profile
`foot-*` profile（步行与徒步）的通行速度在所有允许的道路类型上均设为
5 km/h。
对于 [`sac_scale`](../extra-info/Trail-Difficulty) 高于
`hiking` 的道路，速度降至 2 km/h。

允许的道路类型包括可安全使用的道路、最好避开（但仍允许）的道路，以及介于两者之间的其他允许道路：

  |        安全标签       |  避开标签      |  其他 highway 标签 |
  |:----------------------:|:----------------:|:-------------------:|
  |         footway        |   trunk          |       cycleway      |
  |         path           | trunk_link       |    unclassified     |
  |         steps          |  primary         |       road        |
  |         pedestrian     |  primary_link    |                   |
  |         living_street  |  secondary       |                   |
  |         track          |  secondary_link  |                   |
  |         residential    |  tertiary        |                   |
  |         service        |  tertiary_link   |                   |

### 轮椅 profile
轮椅 profile 的基础速度为 4km/h，随后会基于若干参数进行修正。因此，根据人行道的存在情况和道路类型，实际速度可在 3 到 10km/h 之间变化

[svf]: https://github.com/GIScience/openrouteservice/tree/master/ors-engine/src/main/resources/resources/services/routing/speed_limits
[cbfe]: https://github.com/GIScience/openrouteservice/blob/master/ors-engine/src/main/java/org/heigit/ors/routing/graphhopper/extensions/flagencoders/bike/CommonBikeFlagEncoder.java#L186
[mbfe]: https://github.com/GIScience/openrouteservice/blob/master/ors-engine/src/main/java/org/heigit/ors/routing/graphhopper/extensions/flagencoders/bike/MountainBikeFlagEncoder.java#L52
[rbfe]: https://github.com/GIScience/openrouteservice/blob/master/ors-engine/src/main/java/org/heigit/ors/routing/graphhopper/extensions/flagencoders/bike/RoadBikeFlagEncoder.java#L85
[ebfe]: https://github.com/GIScience/openrouteservice/blob/master/ors-engine/src/main/java/org/heigit/ors/routing/graphhopper/extensions/flagencoders/bike/ElectroBikeFlagEncoder.java#L41
[apidocs]: https://openrouteservice.org/dev/#/api-docs/v2/directions/{profile}/post
