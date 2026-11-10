# Snapping 端点

snapping 端点可用于将点吸附到街道网络的边上，适用于特定的交通方式。

该端点以 JSON 或 GeoJSON 返回吸附到图中最近边的点列表。
如果在指定的搜索半径内找不到合适的吸附点，则返回 `null`。

必须以路径参数的形式指定 routing profile。
要吸附的点列表必须以参数 `locations` 的形式在请求体中指定，
它是一个经度/纬度元组列表。
另一个必需的请求体参数是以米为单位的 `radius`。

结果中包含按请求中原有位置顺序排列的吸附点。

在下面的示例请求和结果中，第一个点在搜索半径内无法被吸附，
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
