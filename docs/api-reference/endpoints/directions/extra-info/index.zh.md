# 额外信息

在请求路线时，可以请求多种 “extra info” 项，以获得关于路线的更多信息。
这些信息可能是路面、道路类型或 OpenStreetMap way ID 之类的内容。
下表详细列出了 https://api.openrouteservice.org 提供的路径规划中，每个 profile 可用的额外信息项。

## 在请求中指定额外信息

所需的额外信息可以在请求体参数 `extra_info` 中指定：

```json
"extra_info": [ "steepness", "waytype"]
```

下表列出了请求中可能的取值，以及 directions 响应中的键和可能取值：

| `extra_info` 中的请求值 | 描述                                                                                                                                  | 响应 `$.routes[*].extras` 中的键 | 响应值                                           |
|:-----------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------|-----------------------------------------------------------|
|           steepness           | 提供路线中各部分有多陡的信息                                                                                  | `steepness`                          | [陡峭度 ID](steepness.zh.md)                             |
|          suitability          | 基于路线特征和 profile 判断该道路的适宜程度                                                                | `suitability`                        | 1（不适宜）- 10（非常适宜）                       |
|            surface            | 沿途的路面覆盖情况                                                                                                         | `surface`                            | [路面 ID](surface.zh.md)                                 |
|          waycategory          | 路线各部分的特定类别（收费道路、高速公路、浅滩等）                                                                   | `waycategory`                        | [类别 ID](waycategory.zh.md)                            |
|            waytype            | 路线中所使用的道路和路径类型                                                                                          | `waytype`                            | [类型 ID](waytype.zh.md)                                    |
|           tollways            | 路线经过的所有收费道路。某条道路是否被标记为[收费道路](https://wiki.openstreetmap.org/wiki/Key:toll)取决于 `profile` | `tollways`                           | 0（非收费道路）或 1（收费道路）                             |
|        traildifficulty        | 路径各部分依据 sac 或山地自行车难度等级评定的难度                                                                      | `traildifficulty`                    | [难度 ID](trail-difficulty.zh.md)                     |
|             osmid             | 路线所使用的道路的 [OpenStreetMap way ID](https://wiki.openstreetmap.org/wiki/Elements#id)                                      | `osmId`¹                             |                                                           |
|    roadaccessrestrictions     | 关于可能存在通行限制的道路的信息（例如私人道路、仅限目的地）                                              | `roadaccessrestrictions`             | [限制 ID](road-access-restrictions.zh.md)           |
|          countryinfo          | 道路各部分所在的国家                                                                                                       | `countryinfo`                        | [国家 ID](/technical-details/country-list.md)         |
|             green             | 路线各部分有多 “绿”（受树木数量、公园、河流等因素影响）                                       | `green`                              | 0（绿化最少）- 10（绿化很多）       |
|             noise             | 路线各部分有多嘈杂（受与高速公路的邻近程度等因素影响）                                                       | `noise`                              | 0（安静）- 10（嘈杂）                                    |
|            shadow²            | 路线各部分有多向阳                                                                                                     | `shadow`                             | 0（完全处于阴影中）- 10（完全处于阳光下） |
|             csv²              | 实验性功能：来自所提供 csv 文件的通用额外信息                                                                    | `csv`                                | 自定义值                                             |

*¹ 注意请求与响应中的键不同！*

*² 由 HeiGIT 托管的公共 openrouteservice 中没有可用数据*


## 额外信息可用性

有些取值并非在所有路径规划 profile 中都可用：

|                  | steepness | suitability | surface | waycategory | waytype | tollways | traildifficulty | osmid | roadaccessrestrictions | countryinfo | green | noise | shadow |
|:-----------------|:---------:|:-----------:|:-------:|:-----------:|:-------:|:--------:|:---------------:|:-----:|:----------------------:|:-----------:|:-----:|:-----:|:------:|
| driving-car      |     x     |      x      |    x    |      x      |    x    |    x     |        x        |       |           x            |      x      |       |       |        |
| driving-hgv      |     x     |      x      |    x    |      x      |    x    |    x     |        x        |       |           x            |      x      |       |       |        |
| cycling-regular  |     x     |      x      |    x    |      x      |    x    |          |        x        |       |                        |             |       |       |        |
| cycling-mountain |     x     |      x      |    x    |      x      |    x    |          |        x        |       |                        |             |       |       |        |
| cycling-road     |     x     |      x      |    x    |      x      |    x    |          |        x        |       |                        |             |       |       |        |
| foot-walking     |     x     |      x      |    x    |      x      |    x    |          |        x        |       |                        |             |   x   |   x   |   x    |
| foot-hiking      |     x     |      x      |    x    |      x      |    x    |          |        x        |       |                        |             |   x   |   x   |   x    |
| wheelchair       |     x     |      x      |    x    |      x      |    x    |          |        x        |   x   |                        |             |       |       |        |


## 响应中的额外信息

所请求的额外信息可以在 directions 响应（JSON 端点）中的如下节点找到

```jsonpath
$.routes[*].extras
```

该节点为 object 类型。每个请求的 extra_info 都会以所请求的值作为键，表示为嵌套的对象条目，例如 `steepness`。
请注意，`extras` 对象中的某些键与请求值不同，参见上表！

下面是一个示例：

```json
"extras": {
  "waytypes": {
    "values": [
      [
        0,
        17,
        3
      ],
      [
        17,
        19,
        1
      ],
      [
        19,
        20,
        3
      ]
    ],
    "summary": [
      {
        "value": 3,
        "distance": 1285.2,
        "amount": 93.93
      },
      {
        "value": 1,
        "distance": 83,
        "amount": 6.07
      }
    ]
  },
  "steepness": {
    "values": [
      [
        0,
        20,
        0
      ]
    ],
    "summary": [
      {
        "value": 0,
        "distance": 1368.2,
        "amount": 100
      }
    ]
  }
}
```

每个条目的取值结构相同：都是包含 `values` 和 `summary` 条目的对象：

### `values`

一个长度为 3 的整数数组列表，表示所计算路线中具有相同所请求信息类型取值的一段连续区段。
每个列表中的三个值含义如下：

1. 该区段起始路点的索引 
2. 该区段结束路点的索引
3. 该区段额外信息度量（waytype、steepness 等）的取值

在上面的示例中，所计算的路线有三个具有不同 waytypes 的区段：

* 路点 0-17：waytype=3
* 路点 17-19：waytype=1
* 路点 19-20：waytype=3

并且只有一个区段（即整条路线）从路点 0-20，其 steepness=0。
 
### `summary`

一个包含 `value`、`distance`、`amount` 条目的对象列表，汇总了具有给定额外信息单元 `value` 的所有路段的总长度（`distance`）和相对长度（`amount`）。

在上面的示例中，所有 waytype=3 的区段汇总长度为 1285.2 m，
占所计算路线的 93.93%，并且整条路线 100% 的 steepness=0。
