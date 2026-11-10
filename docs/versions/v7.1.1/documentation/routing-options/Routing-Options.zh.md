# 路线规划选项

用于格式化为 json 对象的高级选项。结构请参阅[示例](Examples)。
可用参数如下：


- `avoid_features`：要避开的特性数组。
  可用的特性有：

  |     特性         | 适用的 profile                               |
  |:-------------------:|---------------------------------------------|
  | `highways`          | driving-\*                                  |
  | `tollways`          | driving-\*                                  |
  | `ferries`           | driving-\*、cycling-\*、foot-\*、wheelchair |
  | `fords`             | driving-\*、cycling-\*、foot-\*             |
  | `steps`             | cycling-\*、foot-\*、wheelchair             |


- `avoid_borders`：`"all"` 表示不跨越任何边界。`"controlled"` 表示可以跨越开放边界，但避开受管控的边界。仅适用于 `driving-*` profile。

- `avoid_countries`：在使用 `driving-*` profile 时要从路线规划中排除的国家 id 数组。可以与 `"avoid_borders": "controlled"` 一起使用。`"[11,193]"` 会排除奥地利和瑞士。国家列表和应用示例可在[国家列表](Country-List)中找到。

- `vehicle_type`（仅用于 `profile=driving-hgv`）：`hgv`、`bus`、`agricultural`、`delivery`、`forestry` 和 `goods`。需要用它才能让**车辆限制**生效。

- `profile_params`：指定额外的路线规划参数。

  - `weightings`：权重会让指定因素优先于最短路径。

    - `steepness_difficulty`：为 `cycling-*` profile 指定体能水平。
      - `level`：`0` = 新手，`1` = 中等，`2` = 业余，`3` = 专业。等级越高，偏好的坡度越大

    - `green`：为 `foot-*` profile 指定绿色（Green）因子。
      - `factor`：取值范围为 `0` 到 `1`。`0` 等同于普通路线规划。`1` 会优先选择穿过绿地的道路，而不是更短的路线。

    - `quiet`：为 `foot-*` profile 指定安静（Quiet）因子。
      - `factor`：取值范围为 `0` 到 `1`。`0` 等同于普通路线规划。`1` 会优先选择安静的道路，而不是更短的路线。

  - `restrictions`：为 `driving-hgv`、`wheelchair` 或 `cycling-*` profile 指定限制条件。

    - 对于 `cycling-*`：

      |  参数 | 描述                                                                                                                                                                               |
      |:----------:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
      | `gradient` | 仅用于避开 `hills` 或指定 `steepness_difficulty` 的情况。指定路线的最大坡度百分比。取值范围为 `1` 到 `15`。坡度更高的路线会被避开。 |

    - 对于 `driving-hgv`：
      *（你必须在 options 中为这些参数指定 `vehicle_type`）*

      |  参数 | 描述                                                                                                                       |
      |:----------:|-----------------------------------------------------------------------------------------------------------------------------------|
      | `length`   | 长度限制，单位为米。                                                                                                     |
      | `width`    | 宽度限制，单位为米。                                                                                                      |
      | `height`   | 高度限制，单位为米。                                                                                                     |
      | `axleload` | 轴重限制，单位为吨。                                                                                                     |
      | `weight`   | 重量限制，单位为吨。                                                                                                       |
      | `hazmat`   | 指定是否采用适合危险品运输的路线规划，并避开水源保护区。默认值为 false。 |

    - 对于 `wheelchair`：

      |       参数       | 描述                                                                                                                 |
      |:---------------------:|-----------------------------------------------------------------------------------------------------------------------------|
      |     `surface_type`    | 指定路线的[最低路面类型](http://wiki.openstreetmap.org/wiki/Key:surface)。默认为 `"cobblestone:flattened"`。 |
      |      `track_type`     | 指定路线的[最低等级](http://wiki.openstreetmap.org/wiki/Key:tracktype)。默认为 `"grade1"`。        |
      |   `smoothness_type`   | 指定路线的[最低平整度](http://wiki.openstreetmap.org/wiki/Key:smoothness)。默认为 `"good"`。    |
      | `maximum_sloped_kerb` | 指定斜路缘的最大高度，单位为米。取值为 `0.03`、`0.06`（默认）、`0.1` 或 `any`。              |
      |   `maximum_incline`   | 指定最大坡度百分比。取值为 `3`、`6`（默认）、`10`、`15` 或 `any`。                                      |
      |   `minimum_width`     | 指定道路的最小宽度，单位为米。                                                                            |


- `avoid_polygons`：包含路线中要避开的区域。格式为 [geojson polygon](http://geojson.org/geojson-spec.html#id4) 或 [geojson multipolygon](http://geojson.org/geojson-spec.html#id7)。

## URL 编码

要使用 curl 命令字符串，你必须对特殊字符进行编码。
你需要的值如下表所示：

  | 字符 |  {  |  \| |  }  |  "  |  [  |  ]  |
  |:---------:|:---:|:---:|:---:|:---:|:---:|:---:|
  |  编码 | %7B | %7C | %7D | %22 | %5B | %5D |

有时需要对[options 对象](#examples)使用。
