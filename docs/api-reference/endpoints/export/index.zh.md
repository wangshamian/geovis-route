---
cbf: [2,4,5]
---

# Export 端点

:::warning 注意
该端点不在公共 API 中提供，
但你可以在运行自己的 openrouteservice 实例时使用。
你可以借助 [swagger-ui](/api-reference/index.md#swagger-ui) 轻松创建请求。
:::

导出不同交通方式的基础图。

在请求中，所需的 routing profile 作为倒数第二个路径参数指定，所需的格式作为
最后一个路径参数指定。必须在请求体中定义感兴趣区域的边界框。

结果既可以是节点和边的 JSON，也可以是边的
[TopoJSON](https://github.com/topojson/topojson-specification/tree/master)，后者还能表示
由 OSM 数据推导出的边的实际几何。

为更清楚地呈现两种格式之间的差异，下面列出的示例响应都基于同一份
网络抽取数据。

## JSON 响应

下面是一个针对 `driving-car` profile 基础图的示例请求：

```shell
curl -X 'POST' \
  'http://localhost:8082/ors/v2/export/driving-car/json' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "bbox": [
    [
      8.681495,
      49.41461
    ],
    [ 
      8.686507,
      49.41943
    ]
  ]
}'
```

json 响应包含该 routing profile 相关的边界框内的节点和边。
边条目 `weight` 包含最快的汽车行驶时长，单位为秒。

上述请求的 json 响应如下：

```json
{
    "nodes": [
        {
            "nodeId": 1168,
            "location": [
                8.686742,
                49.427883
            ]
        },
        {
            "nodeId": 1169,
            "location": [
                8.687107,
                49.427982
            ]
        },
        {
            "nodeId": 1378,
            "location": [
                8.685439,
                49.427668
            ]
        },
        {
            "nodeId": 1381,
            "location": [
                8.687425,
                49.428074
            ]
        },
        {
            "nodeId": 2134,
            "location": [
                8.685844,
                49.427873
            ]
        },
        {
            "nodeId": 2135,
            "location": [
                8.686699,
                49.427997
            ]
        },
        {
            "nodeId": 1162,
            "location": [
                8.68758,
                49.428213
            ]
        },
        {
            "nodeId": 1165,
            "location": [
                8.685955,
                49.428631
            ]
        },
        {
            "nodeId": 1166,
            "location": [
                8.685896,
                49.42772
            ]
        },
        {
            "nodeId": 1167,
            "location": [
                8.686352,
                49.427783
            ]
        }
    ],
    "edges": [
        {
            "fromId": 1168,
            "toId": 1169,
            "weight": "6.872"
        },
        {
            "fromId": 1169,
            "toId": 1168,
            "weight": "6.872"
        },
        {
            "fromId": 2134,
            "toId": 2135,
            "weight": "15.234"
        },
        {
            "fromId": 2135,
            "toId": 2134,
            "weight": "15.234"
        },
        {
            "fromId": 1166,
            "toId": 1167,
            "weight": "8.1"
        },
        {
            "fromId": 1167,
            "toId": 1166,
            "weight": "8.1"
        },
        {
            "fromId": 1168,
            "toId": 2135,
            "weight": "3.15"
        },
        {
            "fromId": 2135,
            "toId": 1168,
            "weight": "3.15"
        },
        {
            "fromId": 1378,
            "toId": 1166,
            "weight": "8.07"
        },
        {
            "fromId": 1166,
            "toId": 1378,
            "weight": "8.07"
        },
        {
            "fromId": 1381,
            "toId": 1162,
            "weight": "4.759"
        },
        {
            "fromId": 1162,
            "toId": 1381,
            "weight": "4.759"
        },
        {
            "fromId": 1169,
            "toId": 1381,
            "weight": "6.042"
        },
        {
            "fromId": 1381,
            "toId": 1169,
            "weight": "6.042"
        },
        {
            "fromId": 2134,
            "toId": 1166,
            "weight": "4.191"
        },
        {
            "fromId": 1166,
            "toId": 2134,
            "weight": "4.191"
        },
        {
            "fromId": 2135,
            "toId": 1165,
            "weight": "23.852"
        },
        {
            "fromId": 1165,
            "toId": 2134,
            "weight": "22.137"
        },
        {
            "fromId": 1168,
            "toId": 1167,
            "weight": "7.259"
        },
        {
            "fromId": 1167,
            "toId": 1168,
            "weight": "7.259"
        }
    ],
    "nodes_count": 10,
    "edges_count": 20
}
```

请注意，可双向通行的边被描述为两条 `fromId` 与 `toId` 相反的边。该示例
网络由 10 个节点、18 条边（表示节点之间 9 条双向连接）以及两条
表示仅可单向通行连接（单行道）的边组成。

## TopoJSON 响应

[TopoJSON](https://github.com/topojson/topojson-specification/tree/master) 响应包含边界框内
带有底层道路网络几何的边。请求 TopoJSON 时，你可以额外传入一个
可选参数 `geometry`，它接受布尔值（默认为 `true`），用于控制是返回边的实际几何，还是返回省略
所有中间节点的直线表示。TopoJSON 格式可以直接用包括 [QGIS](https://qgis.org) 或 [geojson.io](http://geojson.io) 在内的多种工具加载和可视化。

![开发服务器用法](/topojson_qgis.png "在 QGIS 中的导出结果"){ style="display: block; margin: 0 auto"}

要充分利用该功能，你的 openrouteservice 实例需要配置为对相应 profile 启用
`OsmId` [外部存储特性](/run-instance/configuration/engine/profiles/build#ext-storages)。

下面是一个针对 `driving-car` profile 的 TopoJSON 图的示例请求：

```shell
curl -X 'POST' \
  'http://localhost:8082/ors/v2/export/driving-car/topojson' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "bbox": [
    [
      8.681495,
      49.41461
    ],
    [
      8.686507,
      49.41943
    ]
  ],
  "geometry": "true"
}'
```  

### 包含 OSM ID

在 TopoJSON 响应中，每条弧（arc）都对应路径规划图中的一条边。如果被查询的 openrouteservice profile
已存储原始地图数据的 OSM ID（即在配置中启用了 `OsmId` 存储），则 `GeometryCollection` 中的每个
对象都表示一组共享相同 ID 因而具有相同属性的边：

- `osm_id` 引用底层道路几何的 OpenStreetMap id。
- `both_directions` 是布尔值，指定该边是否可双向通行。
- `speed` 包含沿该边定义方向行驶时该边的通行速度。
- `speed_reverse` 包含沿相反方向行驶时该边的通行速度（仅当
  `both_directions` 为 true 时存在）。
- `ors_ids` 是 ors 边 id 的列表。
- `ors_nodes` 是所涉及 ors 节点 id 的列表。
- `distances` 包含每条边的距离列表。

```json
{
    "type": "Topology",
    "objects": {
        "network": {
            "type": "GeometryCollection",
            "geometries": [
                {
                    "type": "LineString",
                    "properties": {
                        "osm_id": 24837547,
                        "both_directions": true,
                        "speed": 15.0,
                        "speed_reverse": 15.0,
                        "ors_ids": [
                            1498
                        ],
                        "ors_nodes": [
                            2135,
                            2134
                        ],
                        "distances": [
                            63.476
                        ]
                    },
                    "arcs": [
                        0
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "osm_id": 42806217,
                        "both_directions": true,
                        "speed": 15.0,
                        "speed_reverse": 15.0,
                        "ors_ids": [
                            8017,
                            8018,
                            8019
                        ],
                        "ors_nodes": [
                            1168,
                            1169,
                            1381,
                            1162
                        ],
                        "distances": [
                            28.632,
                            25.173,
                            19.828
                        ]
                    },
                    "arcs": [
                        1,
                        2,
                        3
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "osm_id": 42806216,
                        "both_directions": true,
                        "speed": 15.0,
                        "speed_reverse": 15.0,
                        "ors_ids": [
                            8016
                        ],
                        "ors_nodes": [
                            1166,
                            1167
                        ],
                        "distances": [
                            33.748
                        ]
                    },
                    "arcs": [
                        4
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "osm_id": 52956623,
                        "both_directions": true,
                        "speed": 15.0,
                        "speed_reverse": 15.0,
                        "ors_ids": [
                            8909
                        ],
                        "ors_nodes": [
                            2134,
                            1166
                        ],
                        "distances": [
                            17.461
                        ]
                    },
                    "arcs": [
                        5
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "osm_id": 24000937,
                        "both_directions": true,
                        "speed": 15.0,
                        "speed_reverse": 15.0,
                        "ors_ids": [
                            719
                        ],
                        "ors_nodes": [
                            2135,
                            1168
                        ],
                        "distances": [
                            13.127
                        ]
                    },
                    "arcs": [
                        6
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "osm_id": 42806218,
                        "both_directions": true,
                        "speed": 15.0,
                        "speed_reverse": 15.0,
                        "ors_ids": [
                            8020
                        ],
                        "ors_nodes": [
                            1168,
                            1167
                        ],
                        "distances": [
                            30.247
                        ]
                    },
                    "arcs": [
                        7
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "osm_id": 52956625,
                        "both_directions": false,
                        "speed": 15.0,
                        "ors_ids": [
                            8911
                        ],
                        "ors_nodes": [
                            2135,
                            1165
                        ],
                        "distances": [
                            99.385
                        ]
                    },
                    "arcs": [
                        8
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "osm_id": 305482793,
                        "both_directions": true,
                        "speed": 15.0,
                        "speed_reverse": 15.0,
                        "ors_ids": [
                            18296
                        ],
                        "ors_nodes": [
                            1378,
                            1166
                        ],
                        "distances": [
                            33.626
                        ]
                    },
                    "arcs": [
                        9
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "osm_id": 37831084,
                        "both_directions": false,
                        "speed": 15.0,
                        "ors_ids": [
                            7456
                        ],
                        "ors_nodes": [
                            1165,
                            2134
                        ],
                        "distances": [
                            92.239
                        ]
                    },
                    "arcs": [
                        10
                    ]
                }
            ]
        }
    },
    "arcs": [
        [
            [
                8.686699,
                49.427997
            ],
            [
                8.686342,
                49.427932
            ],
            [
                8.685844,
                49.427873
            ]
        ],
        [
            [
                8.686742,
                49.427883
            ],
            [
                8.687107,
                49.427982
            ]
        ],
        [
            [
                8.687107,
                49.427982
            ],
            [
                8.687425,
                49.428074
            ]
        ],
        [
            [
                8.687425,
                49.428074
            ],
            [
                8.687534,
                49.428133
            ],
            [
                8.68758,
                49.428213
            ]
        ],
        [
            [
                8.685896,
                49.42772
            ],
            [
                8.686352,
                49.427783
            ]
        ],
        [
            [
                8.685844,
                49.427873
            ],
            [
                8.685896,
                49.42772
            ]
        ],
        [
            [
                8.686699,
                49.427997
            ],
            [
                8.686742,
                49.427883
            ]
        ],
        [
            [
                8.686742,
                49.427883
            ],
            [
                8.686352,
                49.427783
            ]
        ],
        [
            [
                8.686699,
                49.427997
            ],
            [
                8.686558,
                49.428345
            ],
            [
                8.686455,
                49.428512
            ],
            [
                8.686352,
                49.428567
            ],
            [
                8.686085,
                49.428622
            ],
            [
                8.685955,
                49.428631
            ]
        ],
        [
            [
                8.685439,
                49.427668
            ],
            [
                8.685646,
                49.427682
            ],
            [
                8.685896,
                49.42772
            ]
        ],
        [
            [
                8.685955,
                49.428631
            ],
            [
                8.685879,
                49.428609
            ],
            [
                8.685782,
                49.42854
            ],
            [
                8.685709,
                49.428405
            ],
            [
                8.685766,
                49.428125
            ],
            [
                8.685844,
                49.427873
            ]
        ]
    ],
    "bbox": [
        8.685439,
        49.427668,
        8.68758,
        49.428631
    ]
}
```

该数据集与 JSON 示例中是同一份网络抽取数据。请注意，有两个 `LineString` 元素被标记为
`"both_directions": false`，表示各有 1 条弧的单行道，其余 9 条弧对应
JSON 响应中的 9 对边。

该格式减少了边及其各自几何的信息冗余。数组 `ors_ids` 包含
openrouteservice 内的边 id。请注意，在 openrouteservice 中边始终是双向的，因此 `ors_ids` 数组
包含两个方向上的边 id。

### 不包含 OSM ID

如果在没有 `OsmId` 存储的 profile 上请求 TopoJSON，响应中的每个几何都由
一条弧组成，并且具有不同的属性集合：

- `weight` 表示行驶时长，单位为秒。
- `node_from` 是该边起点的 ors 节点 id。
- `node_to` 是该边终点的 ors 节点 id。

```json
{
    "type": "Topology",
    "objects": {
        "network": {
            "type": "GeometryCollection",
            "geometries": [
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "6.872",
                        "node_from": 1168,
                        "node_to": 1169
                    },
                    "arcs": [
                        0
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "6.872",
                        "node_from": 1169,
                        "node_to": 1168
                    },
                    "arcs": [
                        -1
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "15.234",
                        "node_from": 2134,
                        "node_to": 2135
                    },
                    "arcs": [
                        1
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "15.234",
                        "node_from": 2135,
                        "node_to": 2134
                    },
                    "arcs": [
                        -2
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "8.1",
                        "node_from": 1166,
                        "node_to": 1167
                    },
                    "arcs": [
                        2
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "8.1",
                        "node_from": 1167,
                        "node_to": 1166
                    },
                    "arcs": [
                        -3
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "3.15",
                        "node_from": 1168,
                        "node_to": 2135
                    },
                    "arcs": [
                        3
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "3.15",
                        "node_from": 2135,
                        "node_to": 1168
                    },
                    "arcs": [
                        -4
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "8.07",
                        "node_from": 1378,
                        "node_to": 1166
                    },
                    "arcs": [
                        4
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "8.07",
                        "node_from": 1166,
                        "node_to": 1378
                    },
                    "arcs": [
                        -5
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "4.759",
                        "node_from": 1381,
                        "node_to": 1162
                    },
                    "arcs": [
                        5
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "4.759",
                        "node_from": 1162,
                        "node_to": 1381
                    },
                    "arcs": [
                        -6
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "6.042",
                        "node_from": 1169,
                        "node_to": 1381
                    },
                    "arcs": [
                        6
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "6.042",
                        "node_from": 1381,
                        "node_to": 1169
                    },
                    "arcs": [
                        -7
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "4.191",
                        "node_from": 2134,
                        "node_to": 1166
                    },
                    "arcs": [
                        7
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "4.191",
                        "node_from": 1166,
                        "node_to": 2134
                    },
                    "arcs": [
                        -8
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "23.852",
                        "node_from": 2135,
                        "node_to": 1165
                    },
                    "arcs": [
                        8
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "22.137",
                        "node_from": 1165,
                        "node_to": 2134
                    },
                    "arcs": [
                        9
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "7.259",
                        "node_from": 1168,
                        "node_to": 1167
                    },
                    "arcs": [
                        10
                    ]
                },
                {
                    "type": "LineString",
                    "properties": {
                        "weight": "7.259",
                        "node_from": 1167,
                        "node_to": 1168
                    },
                    "arcs": [
                        -11
                    ]
                }
            ]
        }
    },
    "arcs": [
        [
            [
                8.686742,
                49.427883
            ],
            [
                8.687107,
                49.427982
            ]
        ],
        [
            [
                8.685844,
                49.427873
            ],
            [
                8.686342,
                49.427932
            ],
            [
                8.686699,
                49.427997
            ]
        ],
        [
            [
                8.685896,
                49.42772
            ],
            [
                8.686352,
                49.427783
            ]
        ],
        [
            [
                8.686742,
                49.427883
            ],
            [
                8.686699,
                49.427997
            ]
        ],
        [
            [
                8.685439,
                49.427668
            ],
            [
                8.685646,
                49.427682
            ],
            [
                8.685896,
                49.42772
            ]
        ],
        [
            [
                8.687425,
                49.428074
            ],
            [
                8.687534,
                49.428133
            ],
            [
                8.68758,
                49.428213
            ]
        ],
        [
            [
                8.687107,
                49.427982
            ],
            [
                8.687425,
                49.428074
            ]
        ],
        [
            [
                8.685844,
                49.427873
            ],
            [
                8.685896,
                49.42772
            ]
        ],
        [
            [
                8.686699,
                49.427997
            ],
            [
                8.686558,
                49.428345
            ],
            [
                8.686455,
                49.428512
            ],
            [
                8.686352,
                49.428567
            ],
            [
                8.686085,
                49.428622
            ],
            [
                8.685955,
                49.428631
            ]
        ],
        [
            [
                8.685955,
                49.428631
            ],
            [
                8.685879,
                49.428609
            ],
            [
                8.685782,
                49.42854
            ],
            [
                8.685709,
                49.428405
            ],
            [
                8.685766,
                49.428125
            ],
            [
                8.685844,
                49.427873
            ]
        ],
        [
            [
                8.686742,
                49.427883
            ],
            [
                8.686352,
                49.427783
            ]
        ]
    ],
    "bbox": [
        8.685439,
        49.427668,
        8.68758,
        49.428631
    ]
}
```

请注意，弧的数量与包含 OSM ID 的响应中弧的数量相同，因此几何
可以在没有冗余的情况下表示，并且几何对每条可双向通行的边仅包含成对的 `LineString` 元素，
这与 JSON 响应类似。