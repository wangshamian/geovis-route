# Matrix 端点

Matrix 端点计算多个起点与终点位置之间按 profile 特定的距离和时间矩阵。

除了 routing profile 的参数、所需的度量（距离或时间）以及其他一些参数（参见 [API Playground](https://openrouteservice.org/dev/#/api-docs/matrix_service)）之外，
matrix 请求中最重要且必需的内容是一个位置列表（坐标元组），例如：

```json
{
  "locations": [
    [8.66044,49.41571],
    [8.67842,49.40664],
    [8.68370,49.40854],
    [8.69014,49.40778]
  ]
}
```

为了指定要计算这些位置中哪些到哪些之间的度量，可以在请求中指定参数 `sources` 和 `destinations`：

```json
"sources": ["0", "1"],
"destinations": ["2", "3"]
```

这两个参数值数组中的数字表示同一请求中 locations 数组的索引。
在上面的示例中，将计算从第一个（0）和第二个（1）位置到第三个（2）和第四个（3）位置的时长：

    0 -> 2
    0 -> 3
    1 -> 2
    1 -> 3

在响应中，结果包含在节点 `durations` 中。
其中第一个数组包含从第一个起点（0）到两个终点（2 和 3）的时长，
第二个数组包含从第二个起点（1）到两个终点（2 和 3）的时长：

```json
  "durations": [
    [            // durations from first source to each destination
      448.82,    //     0 -> 2
      553.01     //     0 -> 3
    ],
    [            // durations from second source to each destination
      142.68,    //     1 -> 2
      246.88     //     1 -> 3
    ]
  ]
```

如果请求中未指定 `sources` 或 `destinations`，则二者都默认为 “所有位置”。
例如，如果 locations 数组有 5 个条目，
sources 数组只有一个条目 `[0]`，
而缺少 `destinations`，那么将计算从位置 0 到位置列表中所有位置的时长。

结果 `durations` 列表中的第一个条目表示从位置 0 到其自身的时长，为 `0`。

要指定计算距离还是时长（或两者），可以相应地设置 `metrics` 参数。

关于所有请求参数和响应类型的详细信息，请参见 [API Playground](https://openrouteservice.org/dev/#/api-docs/matrix_service)。
