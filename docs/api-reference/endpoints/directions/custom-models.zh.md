# 自定义模型

请求体参数 `custom_model` 允许设置特定参数，以影响路线计算过程中的边权重。

该参数仅对在构建图时设置了所需 encoder 选项的 profile 上的 directions 请求可用。**该功能仍处于实验阶段，目前我们的公共 API 上任何 profile 都不可用**。
你可以在[自己的 openrouteservice 实例](/run-instance/)上，通过在 `encoder_options` 中[为 profile 启用该功能](/run-instance/configuration/engine/profiles/build.md#encoder-options)来使用它。

`custom_model` 参数是一个 JSON 对象，下面的示例展示了它在 directions 端点请求体中的结构：

```json
{
    "preference": "custom",
    "coordinates": [
        [
            8.681495,
            49.41461
        ],
        [
            8.687872,
            49.420318
        ]
    ],
    "custom_model": {
        "speed": [
            {
                "if": true,
                "limit_to": 100
            }
        ],
        "priority": [
            {
                "if": "road_class == MOTORWAY",
                "multiply_by": 0
            }
        ],
        "distance_influence": 100
    }
}
```

## 可用参数

| 参数            | 类型   | 描述                                                                                                                                                 |
|----------------------|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `distance_influence` | Number | 每公里[可接受的绕路](#weighting-function)所要求节省的时间（秒），用于确定[距离影响因子](#weighting-function) |
| `speed`              | Array  | 一组用于确定[速度因子](#weighting-function)的[规则](#speed-and-priority-rules)                                                                   |
| `priority`           | Array  | 一组用于确定[优先级因子](#weighting-function)的[规则](#speed-and-priority-rules)                                                                |
| `areas`              | Object | [GeoJSON Feature](#areas) 的映射，供基于区域的[规则](#speed-and-priority-rules)使用                                                                     |

## 基本原理

自定义模型会为图中的每条边调用特殊的权重函数，并用一个综合的权重函数替换计算中通常使用的 `shortest` 或 `fastest` 权重，该综合权重函数会考虑自定义模型的参数。

openrouteservice 针对步行和自行车 profile 的优先级权重（表示相较于机动车道路更偏好专用步道或专用自行车道等）可以叠加在自定义模型的权重“之上”。
因此，带有自定义模型的路线请求中，`preference` 参数的可用取值为 `custom` 和 `recommended`。如果请求同时使用了 `shortest` 或 `fastest` 偏好和自定义模型值，则 preference 的值将被忽略。

## 权重函数

每条边的权重函数计算如下：

weight = distance / (speed * `speed factor` * `priority factor`) + distance * `distance influence factor`

其中 `speed factor`、`priority factor` 和 `distance influence factor` 是由自定义模型相应参数推导出的值，distance 和 speed 是图中为每条边存储的值。

`distance_influence` 参数用于计算 `distance influence factor`，它依据行驶距离影响边的权重。若设为 0，则权重函数中不考虑距离。若设为大于 0 的值，则距离会先乘以推导出的因子，再加到权重中。这样做的结果是，可以在距离与速度之间设定一个偏好比例。自定义模型中提供的值对应于每公里绕路所要求节省的时间（秒）。例如，如果你把 `distance_influence` 设为 60，这意味着只有当一条行驶时间更短的路线每多行驶一公里至少节省一分钟时，才会返回该路线；因此，如果为了走高速公路而不是普通道路而绕路增加了比如 2 公里，只有当这样做至少节省 2 分钟时，该路线才会被视为“更好”的路线。

`speed factor` 和 `priority factor` 会与边的速度相乘。二者都依据自定义模型中相应参数 `speed` 和 `priority` 所提供的规则计算，区别在于 `speed factor` 还会改变行驶时间的计算，而 `priority factor` 仅用于在路线计算时影响边的权重，不改变行驶时间。这两个参数都期望一个对象数组，其中每个对象都是一条由条件和要执行的操作组成的规则。下一节将对此作更详细的说明。

## 速度与优先级规则

参数 `speed` 和 `priority` 是规则数组，分别应用于边的速度和优先级。
数组中的所有规则都按提供顺序依次应用。规则定义为具有以下结构的对象：

```json
{
    "operation_key": "operation_value",
    "condition_key": "condition_value"
}
```

### 操作

`operation_key` 是一个字符串，有两种可能取值。`operation_value` 是一个数字。

| `operation_key` | `operation_value`                                           |
|-----------------|-------------------------------------------------------------|
| `multiply_by`   | 与行驶速度相乘的因子，例如 `0`、`0.5` 等 |
| `limit_to`      | 限速，单位为 km/h，例如 `70`                              |

如果键为 `multiply_by`，则该值会被解释为一个因子，乘以边上存储的速度，以确定该边的权重。

如果该规则位于 `speed` 数组中，该因子也会应用于行驶时间计算中所用的速度值，因此取值为 `0.5` 意味着在受影响的边上以正常速度的一半行驶。

如果该规则位于 `priority` 数组中，该因子仅应用于边的权重，而不影响时间计算；你可以把这个数字理解为决定受影响边的有利程度的因子。`0` 表示完全避开该边，`1` 表示无倾向；其间的任何数字都可以表示有利程度，数字越小表示越不利。

如果键为 `limit_to`，则该值会被解释为受影响边上可行驶的最高速度。速度更高的边其速度会被调整，图中存储的行驶速度更低的边则不受该规则影响。

### 条件

`condition_key` 是一个字符串，可能取值为 `if`、`else_if` 和 `else`。`else_if` 和 `else` 需要前面有一条以 `if` 或 `else_if` 为条件键的规则，并且仅当前一条规则的条件未匹配时才会应用。

`condition_value` 可以是字符串或布尔值。如果是字符串，它可以是 `in_[AREA_NAME]` 的形式（详见[下文小节](#areas)），也可以描述一条可求值为 true 或 false 的逻辑语句。该逻辑语句可以是一个变量与值的简单比较，例如 `road_class == MOTORWAY`，也可以是使用 Java 布尔运算符的更复杂语句，例如
`max_speed <= 30 && (road_environment == TUNNEL || roundabout)`。

这些语句中可以使用的变量称为 `encoded values`，不同的 profile 可用的变量不同。可用的 `encoded values` 可以在 [status 端点](/api-reference/endpoints/status/)的响应中的数组
`profiles.<PROFILE NAME>.encoded_values` 里找到。下面列出了部分示例 `encoded values` 及其可用取值。请注意该列表并不完整，可用的变量取决于具体的 profile。

| 名称               | 类型      | 描述                                                               |
|--------------------|-----------|---------------------------------------------------------------------------|
| `road_class`       | Enum      | MOTORWAY、TRUNK、PRIMARY、SECONDARY、TRACK、STEPS、CYCLEWAY、FOOTWAY、…… |
| `road_environment` | Enum      | ROAD、FERRY、BRIDGE、TUNNEL、……                                          |
| `road_access`      | Enum      | DESTINATION、DELIVERY、PRIVATE、NO、……                                   |
| `smoothness`       | Enum      | EXCELLENT、GOOD、INTERMEDIATE、……                                        |
| `roundabout`       | Boolean   | 该边是否属于环岛                                      |
| `get_off_bike`     | Boolean   | 该边是否被标记为“需要下车推行”                     |
| `max_speed`        | Numerical | 最高速度，单位为 km/h                                                         |
| `bike_network`     | Enum      | MISSING、INTERNATIONAL、NATIONAL、REGIONAL、LOCAL、OTHER                  |
| `foot_network`     | Enum      | MISSING、INTERNATIONAL、NATIONAL、REGIONAL、LOCAL、OTHER                  |

枚举类型的 encoded values 可以使用 `==` 和 `!=` 运算符，而数值类型的 encoded values 可以使用
`==`、`!=`、`<`、`<=`、`>`、`>=` 运算符。布尔类型的 encoded values 不需要运算符，但可以使用
`==`、`!=` 和 `!` 运算符。

### 区域

基于区域的规则通过在 `priority` 和/或 `speed` 数组中使用形式为 `in_[AREA_NAME]` 的 `condition_value`，并在自定义模型的 `areas` 对象中提供相应的同名对象来定义。

每个区域对象都必须是一个 GeoJSON `Feature` 对象。不支持 `bbox` 成员及其他外部成员（如 `properties`）。feature 对象的 `geometry` 成员必须为 `"type": "Polygon"`。

下面是一个使用基于区域规则的自定义模型示例，它将该多边形所定义区域内所有边的优先级设为零（完全避开）：

```json
{
    "priority": [
        {
            "if": "in_custom_area_1",
            "multiply_by": 0
        }
    ],
    "areas": {
        "custom_area_1": {
            "type": "Feature",
            "geometry": {
                "type": "Polygon",
                "coordinates": [
                    [
                        [
                            8.7062144,
                            49.4077481
                        ],
                        [
                            8.7068045,
                            49.4108196
                        ],
                        [
                            8.7132203,
                            49.4117201
                        ],
                        [
                            8.7139713,
                            49.4084322
                        ],
                        [
                            8.7062144,
                            49.4077481
                        ]
                    ]
                ]
            }
        }
    }
}
```

## 更多示例

下面是一些自定义模型示例，用以说明 `custom_model` 参数可以如何使用。

#### 全局限速 80 km/h，例如用于 car profile

```json
{
    "speed": [
        {
            "if": true,
            "limit_to": 80
        }
    ]
}
```

#### 尽量（进一步）避开高速公路和隧道，同时避免把这两条规则都应用到隧道内的高速公路上

```json
{
    "priority": [
        {
            "if": "road_class == MOTORWAY",
            "multiply_by": 0.7
        },
        {
            "else_if": "road_environment == TUNNEL",
            "multiply_by": 0.5
        }
    ]
}
```

#### 避免需要下车推行

```json
{
    "priority": [
        {
            "if": "get_off_bike",
            "multiply_by": 0
        }
    ]
}
```

#### 优先停留在徒步路线上

```json
{
    "priority": [
        {
            "if": "foot_network == MISSING",
            "multiply_by": 0.2
        }
    ]
}
```
