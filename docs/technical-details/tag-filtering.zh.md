# 标签过滤

在图构建过程中，openrouteservice 会查看赋予 OSM way 的各种标签，以此判断该 way 是否应被纳入路径规划图。例如，在步行配置中，任何被标记为 `highway=motorway` 的 way 都会被拒绝，不会进入图，这意味着（对于该配置而言）它们永远不会被用于路径计算。

下列表格列出了初始过滤过程中会考虑哪些标签。
`Reject` 表示该标签值意味着该 way 被明确拒绝用于图构建。
`Accept` 表示该标签意味着该 way 应被纳入。
`Conditional` 表示该标签在过滤过程中会被考虑，
但接受/拒绝取决于其他标签和逻辑，例如 [`:conditional` 后缀](https://wiki.openstreetmap.org/wiki/Conditional_restrictions)。

对时间性限制的支持仅限于季节性通行限制，例如 `vehicle:conditional=no @ (Nov 1-Mar 31)`。这些限制会在图构建时被解析。
其他依赖时间的条件限制目前不予考虑。这尤其意味着，那些通常开放但条件性关闭的 way（反之亦然）会分别保持接受或拒绝状态。

**条目的顺序很重要，因为在许多情况下，如果尚未被明确拒绝/接受，处理流程接近末尾时会引入一个更通用的“兜底”判断。**

记法：  
_斜体单词_ 是为相应小节定义的变量，用于归组若干标签。  
`monospace code` 是来自 OpenStreetMap 的标签。  
`[方括号]` 表示一组可能的标签。  
`*` 表示任意标签，`key != *` 表示 `key` 不存在。

 
## 汽车驾驶

定义：  
_restrictedValues_ = `[private, agricultural, forestry, no, restricted, delivery, military, emergency]`  
_intendedValues_ = `[yes, permissive, destination]`  
_restrictions_ = `[motorcar, motor_vehicle, vehicle, access]`  
_firstValue_ = _restrictions_ 中第一个出现的键的值  
_wayTypesWithDefaultSpeed_ = `[motorway, motorway_link, motorroad, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, unclassified, residential, living_street, service, road, track]`  

| 标签组合                                                                                                                                              |        拒绝        |        接受        |        条件        |
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
| 默认情况：**若未被条件性限制则接受**                                                                                                                  |                    |                    | :heavy_check_mark: |

## 货车驾驶

该配置与上述汽车驾驶逻辑的区别在于以下定义

定义：  
_restrictedValues_ = `[private, no, restricted, military]`  
_intendedValues_ = `[yes, permissive, designated, destination, hgv, goods, bus, agricultural, forestry, delivery]`  
_restrictions_ = `[hgv, motorcar, motor_vehicle, vehicle, access]`  

以及以下用于替代 `ford` 检查的附加规则：

| 标签组合                                                                                                                                               |        拒绝        |  接受  |        条件        |
|--------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------:|:------:|:------------------:|
| !(_restrictions_=_intendedValues_) AND (`highway = ford` OR `ford = *`)                                                                                |                    |        | :heavy_check_mark: |
| !(_restrictions_=_intendedValues_) AND _restrictions_=_restrictedValues_ AND !(`[hgv, goods, bus, agricultural, forestry, delivery]`=_intendedValues_) | :heavy_check_mark: |        |                    |
| 默认情况：**若未被条件性限制则接受**                                                                                                                   |                    |        | :heavy_check_mark: |

## cycling-regular [cycling-electric、cycling-road 与 cycling-mountain]

定义：  
_restrictions_ = `[bicycle, vehicle, access]`  
_restrictedValues_ = `[private, no, restricted, military, emergency]`  
_intendedValues_ = `[yes, designated, official, permissive]`  
_wayTypesWithDefaultSpeed_ = `[unclassified, tertiary_link, primary_link, bridleway, tertiary, living_street, trunk, motorway_link, steps, motorway, secondary, path, residential, road, service, footway, pedestrian, track, secondary_link, cycleway, trunk_link, primary, construction]`  

| 标签组合                                                                                                                                                                                                                             |        拒绝        |        接受        |        条件        |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------:|:------------------:|:------------------:|
| `highway != *` AND (`man_made = pier` OR `railway = platform` OR ( `route = [shuttle_train, ferry]` AND (`bicycle = yes` OR ( `bicycle != *` AND `foot != *`)))) AND !(_restrictions_ = _restrictedValues_)                          |                    | :heavy_check_mark: |                    |
| `highway !=` _wayTypesWithDefaultSpeed_                                                                                                                                                                                              | :heavy_check_mark: |                    |                    |
| `sac_scale = hiking` AND `highway = cycleway`                                                                                                                                                                                        |                    | :heavy_check_mark: |                    |
| _cycling-regular, cycling-electric:_<br/>`sac_scale != hiking`<br/><br/>_cycling-road:_<br/>`sac_scale = *`<br/><br/>_cycling-mountain:_<br/>`sac_scale != [hiking, mountain_hiking, demanding_mountain_hiking, alpine_hiking]`<br/> | :heavy_check_mark: |                    |                    |
| `bicycle = `_intendedValues_ OR `bicycle = dismount` OR `highway = cycleway` OR `bicycle_road = yes`                                                                                                                                 |                    | :heavy_check_mark: |                    |
| `highway = [motorway, motorway_link, bridleway, construction]` OR `motorroad = yes`                                                                                                                                                  | :heavy_check_mark: |                    |                    |
| `highway = ford` OR `ford = *`                                                                                                                                                                                                       |                    |                    | :heavy_check_mark: |
| _restrictions_ = _restrictedValues_ **若未被条件性允许**                                                                                                                                                                             | :heavy_check_mark: |                    |                    |
| 默认情况：**若未被条件性限制则接受**                                                                                                                                                                                                |                    |                    | :heavy_check_mark: |


## 步行

定义：  
_restrictions_ = `[foot, access]`  
_restrictedValues_ = `[private, no, restricted, military, emergency]`  
_intendedValues_ = `[yes, designated, permissive, official]`  

| 标签组合                                                                                                                                                                                                                |        拒绝        |  接受  |        条件        |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------:|:------:|:------------------:|
| `sac_scale != hiking`                                                                                                                                                                                                   | :heavy_check_mark: |        |                    |
| `foot = ` _intendedValues_                                                                                                                                                                                              |                    |        | :heavy_check_mark: |
| _restrictions_ = _restrictedValues_                                                                                                                                                                                     |                    |        | :heavy_check_mark: |
| `sidewalk = [yes, both, left, right]`                                                                                                                                                                                   |                    |        | :heavy_check_mark: |
| `highway != [footway, path, steps, pedestrian, living_street, track, residential, service, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, cycleway, unclassified, road]` | :heavy_check_mark: |        |                    |
| `motorroad = yes`                                                                                                                                                                                                       | :heavy_check_mark: |        |                    |
| `highway = ford` OR `ford = *`                                                                                                                                                                                          |                    |        | :heavy_check_mark: |
| 默认情况：**若未被条件性限制则接受**                                                                                                                                                                                    |                    |        | :heavy_check_mark: |


以下规则仅在该 way 未提供 highway 标签时适用

| 标签组合                                                                      |        拒绝        |        接受        |        条件        |
|-------------------------------------------------------------------------------|:------------------:|:------------------:|:------------------:|
| `route = [shuttle_train, ferry]` AND (`foot != *` OR `foot = yes`)            |                    | :heavy_check_mark: |                    |
| `railway = platform`                                                          |                    | :heavy_check_mark: |                    |
| `man_made = pier`                                                             |                    | :heavy_check_mark: |                    |
| `waterway = lock_gate` AND `foot =` _intendedValues_                          |                    | :heavy_check_mark: |                    |
| _restrictions_ = _restrictedValues_ **若未被条件性允许则拒绝**                |                    |                    | :heavy_check_mark: |
| 默认情况：                                                                    | :heavy_check_mark: |                    |                    |


## 徒步

与[步行](#foot)相同，区别在于当存在 `highway` 标签时对 `sac_scale` 的检查不同。

| 标签组合                                                                           |        拒绝        |  接受  |       条件        |
|------------------------------------------------------------------------------------|:------------------:|:------:|:-----------:|
| `sac_scale != [hiking, mountain_hiking, demanding_mountain_hiking, alpine_hiking]` | :heavy_check_mark: |        |             |


## 轮椅

定义：  
_restrictions_ = `[foot, wheelchair, access]`  
_restrictedValues_ = `[private, no, restricted, military, emergency]`  
_intendedValues_ = `[yes, limited, designated, permissive, official]`  

无论 `highway` 值是什么，都会首先执行以下检查：
| 标签组合                                                                                                                                                                                             |        拒绝        |        接受        |        条件        |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------:|:------------------:|:------------------:|
| _restrictions_ = _restrictedValues_ AND _restrictions_ != _intendedValues_ AND `sidewalk != [yes, both, left, right]`                                                                                | :heavy_check_mark: |                    |                    |

如果该 way 上存在 `highway`：

| 标签组合                                                                                                                                                                                                         |        拒绝        |        接受        |        条件        |
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
| 默认情况：                                                                                                                                                                                                       |                    | :heavy_check_mark: |                    |

以下规则仅在该 way 未提供 `highway` 标签时适用

| 标签组合                                                                                      |        拒绝        |        接受        |       条件        |
|-----------------------------------------------------------------------------------------------|:------------------:|:------------------:|:-----------:|
| `route = [shuttle_train, ferry]` AND `wheelchair =` _intendedValues_                          |                    | :heavy_check_mark: |             |
| `route = [shuttle_train, ferry]` AND `wheelchair =` _restrictedValues_                        | :heavy_check_mark: |                    |             |
| `route = [shuttle_train, ferry]` AND `foot =` _intendedValues_                                |                    | :heavy_check_mark: |             |
| `route = [shuttle_train, ferry]` AND `foot =` _restrictedValues_                              | :heavy_check_mark: |                    |             |
| (`public_transport = platform` OR `railway = platform`) AND `wheelchair =` _intendedValues_   |                    | :heavy_check_mark: |             |
| (`public_transport = platform` OR `railway = platform`) AND `wheelchair =` _restrictedValues_ | :heavy_check_mark: |                    |             |
| (`public_transport = platform` OR `railway = platform`) AND `foot =` _intendedValues_         |                    | :heavy_check_mark: |             |
| (`public_transport = platform` OR `railway = platform`) AND `foot =` _restrictedValues_       | :heavy_check_mark: |                    |             |
| 默认情况：                                                                                    | :heavy_check_mark: |                    |             |
