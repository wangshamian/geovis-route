# Snapping 端点

snapping 端点可用于将点吸附到街道网络中与特定交通方式相对应的边上。

该端点以 JSON 或 GeoJSON 形式返回一个点列表，其中的点已吸附到图中最近的边。
如果在指定的搜索半径内找不到合适的吸附点，则返回 `null`。

路径规划 profile 必须作为路径参数指定。
需要吸附的点列表必须在请求体中作为 `locations` 参数指定，
其内容是一个经度/纬度元组列表。
请求体中另一个必需参数是以米为单位的 `radius`。

结果中包含的吸附点顺序与它们在请求中的原始位置顺序相同。

在下面的示例请求和结果中，第一个点无法在搜索半径内被吸附，
因此结果 `locations` 中的第一个条目为 `null`。

请求：
```shell
curl -X 'POST' \
  'http://localhost:8082/ors/v2/snap/driving-car/json' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "locations": [
    [
      8.681495,
      49.51461
    ],
    [
      8.686507,
      49.41943
    ]
  ],
  "id": "my_request",
  "radius": 300
}'
```

响应：
```json
{
  "locations": [
    null,
    {
      "location": [
        8.686507,
        49.41943
      ],
      "name": "Werderplatz",
      "snapped_distance": 0.01
    }
  ],
  "metadata": {
    "attribution": "openrouteservice.org, OpenStreetMap contributors",
    "service": "snap",
    "timestamp": 1702565781290,
    "query": {
      "locations": [
        [
          8.681495,
          49.51461
        ],
        [
          8.686507,
          49.41943
        ]
      ],
      "profile": "driving-car",
      "id": "my_request",
      "format": "json",
      "radius": 300
    },
    "engine": {
      "version": "8.0",
      "build_date": "2023-12-07T10:16:51Z",
      "graph_date": "2023-12-07T15:04:45Z"
    }
  }
}
```
