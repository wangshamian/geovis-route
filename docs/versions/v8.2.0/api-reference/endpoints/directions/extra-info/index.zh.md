# 额外信息

请求路线时，可以请求若干“额外信息”条目，以便获得关于路线的更多信息。
这些信息可能是诸如路面、道路类型或 OpenStreetMap way ID 之类的内容。
下表详细列出了在 https://api.openrouteservice.org 提供的路径规划中，每个 profile 可用的额外信息条目。

## 在请求中指定额外信息

所需的额外信息可以在请求体参数 `extra_info` 中指定：

```json
"extra_info": [ "steepness", "waytype"]
```

下表列出了请求中可用的取值，以及 directions 响应中对应的键和可能的取值：

| `extra_info` 中的请求取值 | 描述                                                                                                                                  | 响应中的键 `$.routes[*].extras` | 响应取值                                              |
|:-----------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------|--------------------------------------------------------------|
|           steepness           | 提供路线各部分的陡峭程度信息                                                                                                                 | `steepness`                          | [Steepness IDs](steepness.zh.md)                             |
|          suitability          | 根据路线特征和 profile 判断该道路的适宜程度                                                                                                  | `suitability`                        | 1（不适宜）- 10（非常适宜）                                  |
|            surface            | 沿途的路面覆盖类型                                                                                                                           | `surface`                            | [Surface IDs](surface.zh.md)                                 |
|          waycategory          | 路线各部分的具体类别（收费道路、高速公路、浅滩等）                                                                                           | `waycategory`                        | [Category IDs](waycategory.zh.md)                            |
|            waytype            | 路线中使用的道路和小径类型                                                                                                                   | `waytypes`¹                          | [Type IDs](waytype.zh.md)                                    |
|           tollways            | 路线经过的任何收费道路。某条道路是否被标记为[收费道路](https://wiki.openstreetmap.org/wiki/Key:toll)取决于 `profile`                       | `tollways`                           | 0（非收费道路）或 1（收费道路）                              |
|        traildifficulty        | 依据 sac 或山地自行车难度等级判断路线各部分的难度                                                                                             | `traildifficulty`                    | [Difficulty IDs](trail-difficulty.zh.md)                     |
|             osmid             | 路线所用道路的 [OpenStreetMap way ID](https://wiki.openstreetmap.org/wiki/Elements#id)                                                       | `osmId`¹                             |                                                              |
|    roadaccessrestrictions     | 关于可能存在通行限制的道路的信息（例如私人道路、仅限目的地通行）                                                                             | `roadaccessrestrictions`             | [Restrictions IDs](road-access-restrictions.zh.md)           |
|          countryinfo          | 道路各部分所在的国家                                                                                                                         | `countryinfo`                        | [Country IDs](../../../../technical-details/country-list.zh.md) |
|             green             | 路线各部分的“绿化”程度（受树木数量、公园、河流等因素影响）                                                                                    | `green`                              | 0（绿化空间极少）- 10（绿化空间很多）                        |
|             noise             | 路线各部分的嘈杂程度（受与高速公路的距离等因素影响）                                                                                         | `noise`                              | 0（安静）- 10（嘈杂）                                        |
|            shadow²            | 路线各部分的日照情况                                                                                                                         | `shadow`                             | 0（完全处于阴影中）- 10（完全处于阳光下）                    |
|             csv²              | 实验性功能：从提供的 csv 文件中获取通用额外信息                                                                                              | `csv`                                | 自定义取值                                                   |

*¹ 请注意请求和响应中的键并不相同！*

*² 由 HeiGIT 托管的公共 openrouteservice 中没有相关数据*


## 额外信息可用性

有些取值并非在所有路径规划 profile 中都可用：

|                  | steepness | suitability | surface | waycategory | waytype | tollways | traildifficulty | osmid | roadaccessrestrictions | countryinfo | green | noise | shadow |
|:-----------------|:---------:|:-----------:|:-------:|:-----------:|:-------:|:--------:|:---------------:|:-----:|:----------------------:|:-----------:|:-----:|:-----:|:------:|
| driving-car      |     x     |      x      |    x    |      x      |    x    |    x     |        x        |       |           x            |      x      |       |       |        |
| driving-hgv      |     x     |      x      |    x    |      x      |    x    |    x     |        x        |       |           x            |      x      |       |       |        |
| cycling-regular  |     x     |      x      |    x    |      x      |    x    |          |        x        |       |                        |             |       |       |        |
| cycling-mountain |     x     |      x      |    x    |      x      |    x    |          |        x        |       |                        |             |       |       |        |
| cycling-road     |     x     |      x      |    x    |      x      |    x    |          |        x        |       |                        |             |       |       |        |
| cycling-electric |     x     |      x      |    x    |      x      |    x    |          |        x        |       |                        |             |       |       |        |
| foot-walking     |     x     |      x      |    x    |      x      |    x    |          |        x        |       |                        |             |   x   |   x   |   x    |
| foot-hiking      |     x     |      x      |    x    |      x      |    x    |          |        x        |       |                        |             |   x   |   x   |   x    |
| wheelchair       |     x     |      x      |    x    |      x      |    x    |          |        x        |   x   |                        |             |       |       |        |


## 响应中的额外信息

所请求的额外信息可以在 directions 响应（JSON 端点）的以下节点中找到

```jsonpath
$.routes[*].extras
```

该节点类型为对象。每个被请求的 extra_info 都以所请求的取值作为键，表示为一个嵌套对象条目，例如 `steepness`。
请注意，`extras` 对象中的某些键与请求的取值不同，参见上表！

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

每个条目的取值结构相同：都是一个包含 `values` 和 `summary` 条目的对象：

### `values`

一个由长度为 3 的整数数组组成的列表，每个数组表示计算出的路线中具有相同请求信息类型取值的一段连续区间。
每个列表中的三个值含义如下：

1. 该区间起始路径点的索引
2. 该区间结束路径点的索引
3. 该区间的额外信息度量值（waytype、steepness 等）

在上面的示例中，计算出的路线有三段具有不同 waytype 的区间：

* 路径点 0-17：waytype=3
* 路径点 17-19：waytype=1
* 路径点 19-20：waytype=3

另有一段（即整条路线）从路径点 0-20，其 steepness=0。
 
### `summary`

一个由包含 `value`、`distance`、`amount` 条目的对象组成的列表，汇总了额外信息度量值
为某一给定 `value` 的所有路段的总长度（`distance`）和相对长度（`amount`）。

在上面的示例中，所有 waytype=3 的区间汇总长度为 1285.2 m，
占计算路线总长的 93.93%，而整条路线的 steepness 均为 0。
