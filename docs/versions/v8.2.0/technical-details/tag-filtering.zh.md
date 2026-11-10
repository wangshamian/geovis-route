# 标签过滤

在图（graph）构建过程中，openrouteservice 会检查赋予 OSM 道路的各种标签，以此判断该道路是否应被纳入路由图。例如，在步行（foot）配置方案中，任何标记为 `highway=motorway` 的道路都会被拒绝、不会纳入图中，这意味着（对该配置方案而言）永远无法在这些道路上规划路线。

以下表格列出了初始过滤过程中会考虑的标签。
`Reject` 表示该标签取值说明该道路被明确拒绝参与图构建。
`Accept` 表示该标签说明该道路应被纳入。
`Conditional` 表示该标签在过滤过程中会被考虑，
但接受或拒绝还取决于其他标签与逻辑，例如 [`:conditional` 后缀](https://wiki.openstreetmap.org/wiki/Conditional_restrictions)。

对时间性限制的支持仅限于季节性的通行限制，例如 `vehicle:conditional=no @ (Nov 1-Mar 31)`。这类限制会在图构建阶段解析处理。
其他随时间变化的条件性限制目前不予考虑。这意味着，尤其要注意：常年开放但附条件关闭的道路，或者相反情况，仍会保持接受或拒绝的状态。

**条目顺序很重要，因为在许多情况下，如果尚未被明确拒绝或接受，处理过程末尾会引入一个更通用的“兜底”判断。**

记号说明：  
_斜体词_ 是为相应小节定义的变量，用于归并多个标签。  
`等宽代码` 是来自 OpenStreetMap 的标签。  
`[方括号]` 表示可能取值的范围。  
`*` 表示任意标签，`key != *` 表示不存在 `key`。

 
## Driving car

定义：  
_restrictedValues_ = `[private, agricultural, forestry, no, restricted, delivery, military, emergency]`  
_intendedValues_ = `[yes, permissive, destination]`  
_restrictions_ = `[motorcar, motor_vehicle, vehicle, access]`  
_firstValue_ = 按 _restrictions_ 顺序遇到的第一个键所对应的值  
_wayTypesWithDefaultSpeed_ = `[motorway, motorway_link, motorroad, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, unclassified, residential, living_street, service, road, track]`  

| 标签组合                                                                                                                                       |       Reject       |       Accept       |    Conditional     |
|-------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------:|:------------------:|:------------------:|
| `highway != *` AND `route != [shuttle_train, ferry]`                                                                                                  | :heavy_check_mark: |                    |                    |
| `highway != *` AND `route = [shuttle_train, ferry]` AND _firstValue_ = _restrictedValues_                                                             | :heavy_check_mark: |                    |                    |
| `highway != *` AND `route = [shuttle_train, ferry]` AND ( _firstValue_ = _intendedValues_ OR _firstValue_ != `*` AND `foot != *` AND `bicycle != *` ) |                    | :heavy_check_mark: |                    |
| `highway = track` AND `tracktype > grade3`                                                                                                            | :heavy_check_mark: |                    |                    |
| `highway !=` _wayTypesWithDefaultSpeed_                                                                                                               | :heavy_check_mark: |                    |                    |
| `impassable = yes` OR `[status, smoothness] = impassable`                                                                                             | :heavy_check_mark: |                    |                    |
| _firstValue_ = _restrictedValues_                                                                                                                     |                    |                    | :heavy_check_mark: |
| _firstValue_ = _intendedValues_                                                                                                                       |                    | :heavy_check_mark: |                    |
| `highway = ford` OR `ford = *`                                                                                                                        |                    |                    | :heavy_check_mark: |
| `maxwidth < 2`                                                                                                                                        | :heavy_check_mark: |                    |                    |
| 默认情况：**若未受条件限制则接受**                                                                                                                    |                    |                    | :heavy_check_mark: |

## Driving HGV

该配置方案与上述 Driving car 的逻辑不同之处在于以下定义

定义：  
_restrictedValues_ = `[private, no, restricted, military]`  
_intendedValues_ = `[yes, permissive, designated, destination, hgv, goods, bus, agricultural, forestry, delivery]`  
_restrictions_ = `[hgv, motorcar, motor_vehicle, vehicle, access]`  

以及以下取代 `ford` 判断的附加规则：

| 标签组合                                                                                                                                                |       Reject       | Accept |    Conditional     |
|--------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------:|:------:|:------------------:|
| !(_restrictions_=_intendedValues_) AND (`highway = ford` OR `ford = *`)                                                                                |                    |        | :heavy_check_mark: |
| !(_restrictions_=_intendedValues_) AND _restrictions_=_restrictedValues_ AND !(`[hgv, goods, bus, agricultural, forestry, delivery]`=_intendedValues_) | :heavy_check_mark: |        |                    |
| 默认情况：**若未受条件限制则接受**                                                                                                                       |                    |        | :heavy_check_mark: |

## cycling-regular [cycling-electric, cycling-road & cycling-mountain]

定义：  
_restrictions_ = `[bicycle, vehicle, access]`  
_restrictedValues_ = `[private, no, restricted, military, emergency]`  
_intendedValues_ = `[yes, designated, official, permissive]`  
_wayTypesWithDefaultSpeed_ = `[unclassified, tertiary_link, primary_link, bridleway, tertiary, living_street, trunk, motorway_link, steps, motorway, secondary, path, residential, road, service, footway, pedestrian, track, secondary_link, cycleway, trunk_link, primary, construction]`  

| 标签组合                                                                                                                                                                                                                             |       Reject       |       Accept       |    Conditional     |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------:|:------------------:|:------------------:|
| `highway != *` AND (`man_made = pier` OR `railway = platform` OR ( `route = [shuttle_train, ferry]` AND (`bicycle = yes` OR ( `bicycle != *` AND `foot != *`)))) AND !(_restrictions_ = _restrictedValues_)                          |                    | :heavy_check_mark: |                    |
| `highway !=` _wayTypesWithDefaultSpeed_                                                                                                                                                                                              | :heavy_check_mark: |                    |                    |
| `sac_scale = hiking` AND `highway = cycleway`                                                                                                                                                                                        |                    | :heavy_check_mark: |                    |
| _cycling-regular, cycling-electric:_<br/>`sac_scale != hiking`<br/><br/>_cycling-road:_<br/>`sac_scale = *`<br/><br/>_cycling-mountain:_<br/>`sac_scale != [hiking, mountain_hiking, demanding_mountain_hiking, alpine_hiking]`<br/> | :heavy_check_mark: |                    |                    |
| `bicycle = `_intendedValues_ OR `bicycle = dismount` OR `highway = cycleway` OR `bicycle_road = yes`                                                                                                                                 |                    | :heavy_check_mark: |                    |
| `highway = [motorway, motorway_link, bridleway, construction]` OR `motorroad = yes`                                                                                                                                                  | :heavy_check_mark: |                    |                    |
| `highway = ford` OR `ford = *`                                                                                                                                                                                                       |                    |                    | :heavy_check_mark: |
| _restrictions_ = _restrictedValues_ **若未获条件许可**                                                                                                                                                                                | :heavy_check_mark: |                    |                    |
| 默认情况：**若未受条件限制则接受**                                                                                                                                                                                                   |                    |                    | :heavy_check_mark: |


## Foot

定义：  
_restrictions_ = `[foot, access]`  
_restrictedValues_ = `[private, no, restricted, military, emergency]`  
_intendedValues_ = `[yes, designated, permissive, official]`  

| 标签组合                                                                                                                                                                                                                 |       Reject       | Accept |    Conditional     |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------:|:------:|:------------------:|
| `sac_scale != hiking`                                                                                                                                                                                                   | :heavy_check_mark: |        |                    |
| `foot = ` _intendedValues_                                                                                                                                                                                              |                    |        | :heavy_check_mark: |
| _restrictions_ = _restrictedValues_                                                                                                                                                                                     |                    |        | :heavy_check_mark: |
| `sidewalk = [yes, both, left, right]`                                                                                                                                                                                   |                    |        | :heavy_check_mark: |
| `highway != [footway, path, steps, pedestrian, living_street, track, residential, service, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, cycleway, unclassified, road]` | :heavy_check_mark: |        |                    |
| `motorroad = yes`                                                                                                                                                                                                       | :heavy_check_mark: |        |                    |
| `highway = ford` OR `ford = *`                                                                                                                                                                                          |                    |        | :heavy_check_mark: |
| 默认情况：**若未受条件限制则接受**                                                                                                                                                                                       |                    |        | :heavy_check_mark: |


以下规则仅在道路未提供 highway 标签时适用

| 标签组合                                                                   |       Reject       |       Accept       |    Conditional     |
|-------------------------------------------------------------------------------|:------------------:|:------------------:|:------------------:|
| `route = [shuttle_train, ferry]` AND (`foot != *` OR `foot = yes`)            |                    | :heavy_check_mark: |                    |
| `railway = platform`                                                          |                    | :heavy_check_mark: |                    |
| `man_made = pier`                                                             |                    | :heavy_check_mark: |                    |
| `waterway = lock_gate` AND `foot =` _intendedValues_                          |                    | :heavy_check_mark: |                    |
| _restrictions_ = _restrictedValues_ **若未获条件许可**则拒绝                    |                    |                    | :heavy_check_mark: |
| 默认情况：                                                                     | :heavy_check_mark: |                    |                    |


## Hiking

除在已有 `highway` 标签时对 `sac_scale` 的检查不同外，其余与 [Foot](#foot) 相同。

| 标签组合                                                                            |       Reject       | Accept | Conditional |
|------------------------------------------------------------------------------------|:------------------:|:------:|:-----------:|
| `sac_scale != [hiking, mountain_hiking, demanding_mountain_hiking, alpine_hiking]` | :heavy_check_mark: |        |             |


## Wheelchair

定义：  
_restrictions_ = `[foot, wheelchair, access]`  
_restrictedValues_ = `[private, no, restricted, military, emergency]`  
_intendedValues_ = `[yes, limited, designated, permissive, official]`  

无论 `highway` 取值如何，都会先执行以下检查：
| 标签组合                                                                                                                                                                                              |       Reject       |       Accept       |    Conditional     |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------:|:------------------:|:------------------:|
| _restrictions_ = _restrictedValues_ AND _restrictions_ != _intendedValues_ AND `sidewalk != [yes, both, left, right]`                                                                                | :heavy_check_mark: |                    |                    |

如果道路上存在 `highway` 标签：

| 标签组合                                                                                                                                                                                                          |       Reject       |       Accept       |    Conditional     |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------:|:------------------:|:------------------:|
| `sac_scale = [mountain_hiking, demanding_mountain_hiking, alpine_hiking, demanding_alpine_hiking, difficult_alpine_hiking]`                                                                                      | :heavy_check_mark: |                    |                    |
| `surface = [ sand, salt, grass, snow, earth, dirt, ice, mud]`                                                                                                                                                    | :heavy_check_mark: |                    |                    |
| `smoothness = [bad, very_bad, horrible, very_horrible]`                                                                                                                                                          | :heavy_check_mark: |                    |                    |
| `tracktype = [grade4, grade5]`                                                                                                                                                                                   | :heavy_check_mark: |                    |                    |
| `wheelchair =` _intendedValues_                                                                                                                                                                                  |                    | :heavy_check_mark: |                    |
| `wheelchair =` _restrictedValues_                                                                                                                                                                                | :heavy_check_mark: |                    |                    |
| `highway = [steps, construction]`                                                                                                                                                                                | :heavy_check_mark: |                    |                    |
| `foot =` _intendedValues_                                                                                                                                                                                        |                    | :heavy_check_mark: |                    |
| `foot =` _restrictedValues_                                                                                                                                                                                      | :heavy_check_mark: |                    |                    |
| `sidewalk = [yes, both, left, right]`                                                                                                                                                                            |                    | :heavy_check_mark: |                    |
| `sidewalk = [no, none, separate]`                                                                                                                                                                                | :heavy_check_mark: |                    |                    |
| `motorroad = yes`                                                                                                                                                                                                | :heavy_check_mark: |                    |                    |
| `highway = ford` OR `ford = *`                                                                                                                                                                                   |                    |                    | :heavy_check_mark: |
| `highway = bridleway` AND `foot !=` _intendedValues_ AND `wheelchair !=` _intendedValues_                                                                                                                        | :heavy_check_mark: |                    |                    |
| `highway = [footway, pedestrian, living_street, residential, unclassified, service, tertiary, tertiary_link, road, trunk, trunk_link, primary, primary_link, secondary, secondary_link, path, track, bridleway]` |                    | :heavy_check_mark: |                    |
| 默认情况：                                                                                                                                                                                                        |                    | :heavy_check_mark: |                    |

以下规则仅在道路未提供 `highway` 标签时适用

| 标签组合                                                                                       |       Reject       |       Accept       | Conditional |
|-----------------------------------------------------------------------------------------------|:------------------:|:------------------:|:-----------:|
| `route = [shuttle_train, ferry]` AND `wheelchair =` _intendedValues_                          |                    | :heavy_check_mark: |             |
| `route = [shuttle_train, ferry]` AND `wheelchair =` _restrictedValues_                        | :heavy_check_mark: |                    |             |
| `route = [shuttle_train, ferry]` AND `foot =` _intendedValues_                                |                    | :heavy_check_mark: |             |
| `route = [shuttle_train, ferry]` AND `foot =` _restrictedValues_                              | :heavy_check_mark: |                    |             |
| (`public_transport = platform` OR `railway = platform`) AND `wheelchair =` _intendedValues_   |                    | :heavy_check_mark: |             |
| (`public_transport = platform` OR `railway = platform`) AND `wheelchair =` _restrictedValues_ | :heavy_check_mark: |                    |             |
| (`public_transport = platform` OR `railway = platform`) AND `foot =` _intendedValues_         |                    | :heavy_check_mark: |             |
| (`public_transport = platform` OR `railway = platform`) AND `foot =` _restrictedValues_       | :heavy_check_mark: |                    |             |
| 默认情况：                                                                                     | :heavy_check_mark: |                    |             |
