# 标签过滤
在图构建过程中，openrouteservice 会查看赋予 OSM 道路的各类标签，以此判断该道路是否应被纳入路线图。例如在步行（foot）profile 中，任何标记为 `highway=motorway` 的道路都会被拒绝，不会纳入图，这意味着（对该 profile 而言）永远不会在这些道路上规划路线。

下表列出了初始过滤过程中会考虑哪些标签。`Reject` 表示该标签值说明这条道路被明确拒绝进入图构建，`Accept` 表示该标签说明这条道路应被纳入，`Conditional` 表示该标签会在过滤过程中被考虑，但接受还是拒绝取决于其他标签和逻辑。

**除非满足接受条件，否则所有道路在开始阶段都会被拒绝进入图构建！**

**各项的顺序很重要，因为在许多情况下，如果尚未被明确拒绝/接受，处理接近结束时还会引入一个更通用的“兜底”规则。**

记号说明：
_斜体词_ 是为相应小节定义的变量，代表若干个标签。  
`等宽代码` 是来自 openstreetmap 的标签。  
`[方括号]` 表示一组可能的标签。  
`*` 表示任意标签，`key != *` 表示缺少 `key`。

 
## 驾车 :car:

定义：  
_restrictedValues_ = `[private, agricultural, forestry, no, restricted, delivery, military, emergency]`  
_intendedValues_ = `[yes, permissive, destination]`  
_restrictions_ = `[motorcar, motor_vehicle, vehicle, access]`  
_firstValue_ = 从 _restrictions_ 中第一个出现的 key 的值

| 标签组合 | 拒绝 | 接受 | 条件性 |
| --------------- |:------:|:------:|:-----------:|
| `highway != *` AND `route != [shuttle_train, ferry]` | :heavy_check_mark: | | |
| `highway != *` AND `route = [shuttle_train, ferry]` AND _firstValue_ = _restrictedValues_ | :heavy_check_mark: | | |
| `highway != *` AND `route = [shuttle_train, ferry]` AND ( _firstValue_ = _intendedValues_ OR _firstValue_ != `*` AND `foot != *` AND `bicycle != *` ) | | :heavy_check_mark: | |
| `highway = track` AND `tracktype = *` | | | :heavy_check_mark: |
| `highway != [motorway, motorway_link, motorroad, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, unclassified, residential, living_street, service, road, track]` | :heavy_check_mark: | | |
| `impassable = yes` OR `[status, smoothness] = impassable` | :heavy_check_mark: | | |
| _firstValue_ = _restrictedValues_ | :heavy_check_mark: | | |
| _firstValue_ = _intendedValues_ | | :heavy_check_mark: | |
| `highway = ford` OR `ford = *` | | | :heavy_check_mark: |
| `maxwidth < 2` | :heavy_check_mark: | | |

## 驾驶 HGV :truck: :bus: 🚜

该 profile 与上述驾车逻辑的区别在于以下定义

_restrictedValues_ = `[private, no, restricted, military]`  
_intendedValues_ = `[yes, permissive, designated, destination, hgv, goods, bus, agricultural, forestry, delivery]`

以及以下新增规则：


| 标签组合 | 拒绝 | 接受 | 条件性 |
| --------------- |:------:|:------:|:-----------:|
| _restrictions_=_restrictedValues_ AND !(_restrictions_=_intendedValues_) AND !([hgv, goods, bus, agricultural, forestry, delivery]=_intendedValues_) | :heavy_check_mark: | | |

## cycling-regular :bike: [cycling-electric, cycling-road & cycling-mountain]

定义：  
_restrictions_ = `[bicycle, vehicle, access]`  
_restrictedValues_ = `[private, no, restricted, military, emergency]`  
_intendedValues_ = `[yes, designated, official, permissive]`

| 标签组合                                                                                                                                                                                                                      |       拒绝       |       接受       |    条件性     |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------:|:------------------:|:------------------:|
| (`man_made = pier` OR `railway = platform` OR ( `route = [shuttle_train, ferry]` AND (`bicycle = yes` OR ( `bicycle != *` AND `foot != *`)))) AND !(_restrictions_ = _restrictedValues_)                                             |                    | :heavy_check_mark: |                    |
| `highway != [cycleway, path, footway, pedestrian, track, service, residential, living_street, steps, unclassified, road, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link]`              | :heavy_check_mark: |                    |                    |
| `sac_scale = hiking` AND `highway = cycleway`                                                                                                                                                                                        |                    | :heavy_check_mark: |                    |
| _cycling-regular, cycling-electric:_<br/>`sac_scale != hiking`<br/><br/>_cycling-road:_<br/>`sac_scale = *`<br/><br/>_cycling-mountain:_<br/>`sac_scale != [hiking, mountain_hiking, demanding_mountain_hiking, alpine_hiking]`<br/> | :heavy_check_mark: |                    |                    |
| `bicycle = `_intendedValues_ OR `bicycle = dismount` OR `highway = cycleway` OR `bicycle_road = yes`                                                                                                                                 |                    | :heavy_check_mark: |                    |
| `highway = [motorway, motorway_link]` OR `motorroad = yes`                                                                                                                                                                           | :heavy_check_mark: |                    |                    |
| `highway = ford` OR `ford = *`                                                                                                                                                                                                       |                    |                    | :heavy_check_mark: |
| _restrictions_ = _restrictedValues_                                                                                                                                                                                                  | :heavy_check_mark: |                    |                    |
| ELSE `* = *`                                                                                                                                                                                                                         |                    | :heavy_check_mark: |                    |


## 步行

| 标签组合 | 拒绝 | 接受 | 条件性 |
| --------------- |:------:|:------:|:-----------:|
| `sac_scale != hiking` | :heavy_check_mark: | | |
| `foot = [yes, designated, official, permissive]` | | :heavy_check_mark: | |
| `foot = [private, no, restricted, military, emergency]` | | | :heavy_check_mark: |
| `access = [private, no, restricted, military, emergency]` | | | :heavy_check_mark: |
| `sidewalk = [yes, both, left, right]` | | :heavy_check_mark: | |
| `highway != [footway, path, steps, pedestrian, living_street, track, residential, service, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, cycleway, unclassified, road]` | :heavy_check_mark: | | |
| `motorroad = yes` | :heavy_check_mark: | | |
| `highway = ford` OR `ford = *` | | | :heavy_check_mark: |

以下规则仅适用于该道路未提供 highway 标签的情况

| 标签组合 | 拒绝 | 接受 | 条件性 |
| --------------- |:------:|:------:|:-----------:|
| `route = [shuttle_train, ferry]` AND (`foot != *` OR `foot = yes`) | | :heavy_check_mark: | |
| `railway = platform` | | :heavy_check_mark: | |
| `man_made = pier` | | :heavy_check_mark: | |

## 徒步

| 标签组合 | 拒绝 | 接受 | 条件性 |
| --------------- |:------:|:------:|:-----------:|
| `sac_scale != [hiking, mountain_hiking, demanding_mountain_hiking, alpine_hiking]` | :heavy_check_mark: | | |
| `foot = [yes, designated, official, permissive]` | | :heavy_check_mark: | |
| `foot = [private, no, restricted, military, emergency]` | | | :heavy_check_mark: |
| `access = [private, no, restricted, military, emergency]` | | | :heavy_check_mark: |
| `sidewalk = [yes, both, left, right]` | | :heavy_check_mark: | |
| `highway != [footway, path, steps, pedestrian, living_street, track, residential, service, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, cycleway, unclassified, road]` | :heavy_check_mark: | | |
| `motorroad = yes` | :heavy_check_mark: | | |
| `highway = ford` OR `ford = *` | | | :heavy_check_mark: |

以下规则仅适用于该道路未提供 highway 标签的情况

| 标签组合 | 拒绝 | 接受 | 条件性 |
| --------------- |:------:|:------:|:-----------:|
| `route = [shuttle_train, ferry]` AND (`foot != *` OR `foot = yes`) | | :heavy_check_mark: | |
| `railway = platform` | | :heavy_check_mark: | |
| `man_made = pier` | | :heavy_check_mark: | |

## 轮椅

| 标签组合 | 拒绝 | 接受 | 条件性 |
| --------------- |:------:|:------:|:-----------:|
| `motorroad = yes` | :heavy_check_mark: | | |
| `wheelchair = [yes, designated, official, permissive, limited]` | | :heavy_check_mark: | |
| `wheelchair = [private, no, restricted]` | :heavy_check_mark: | | |
| `highway = steps` | :heavy_check_mark: | | |
| `foot = [yes, designated, official, permissive, limited]` | | :heavy_check_mark: | |
| `foot = [private, no, restricted]` | :heavy_check_mark: | | |
| `sac_scale = *` | :heavy_check_mark: | | |
| `sidewalk = [yes, both, left, right]` | | :heavy_check_mark: |
| `sidewalk = [no, none, separate, detached]` AND `highway = [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, road]` | :heavy_check_mark: | | |
| `highway = ford` OR `ford = *` | | | :heavy_check_mark: |
| (`bicycle = [designated, official]` OR `horse = [designated, official]`) AND `[foot, access, wheelchair] = [yes, designated, official, permissive, limited]` | :heavy_check_mark: | | |
| `highway = [bridleway, cycleway]` | | | :heavy_check_mark: |
| `highway = [footway, pedestrian, living_street, residential, unclassified, service, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, road path, track]` | | :heavy_check_mark: | |

以下规则仅适用于该道路未提供 highway 标签的情况

| 标签组合 | 拒绝 | 接受 | 条件性 |
| --------------- |:------:|:------:|:-----------:|
| `route = [shuttle_train, ferry]` AND `wheelchair = [yes, designated, official, permissive, limited]` | | :heavy_check_mark: | |
| `route = [shuttle_train, ferry]` AND `wheelchair = [private, no, restricted]` | :heavy_check_mark: | | |
| `route = [shuttle_train, ferry]` AND `foot = [yes, designated, official, permissive, limited]` | | :heavy_check_mark: | |
| `route = [shuttle_train, ferry]` AND `foot = [private, no, restricted]` | :heavy_check_mark: | | |
| (`public_transport = platform` OR `railway = platform`) AND `wheelchair = [yes, designated, official, permissive, limited]` | | :heavy_check_mark: | |
| (`public_transport = platform` OR `railway = platform`) AND `wheelchair = [private, no, restricted]` | :heavy_check_mark: | | |
| (`public_transport = platform` OR `railway = platform`) AND `foot = [yes, designated, official, permissive, limited]` | | :heavy_check_mark: | |
| (`public_transport = platform` OR `railway = platform`) AND `foot = [private, no, restricted]` | :heavy_check_mark: | | |
