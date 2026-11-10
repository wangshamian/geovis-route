# 路径规划选项

通过请求体参数 `options`，可以为 directions 请求指定高级的路径规划选项。

`options` 参数是一个 JSON 对象，可以同时设置这里提到的多个参数。
结构请参考[示例](#examples)。


## 可用参数

### `options.avoid_borders`
字符串值，用于指定要避开哪些边境。仅适用于 **`driving-*`** profile。

| 值          | 描述                                  |
|----------------|----------------------------------------------|
| `"all"`        | 不跨越任何边境                       |
| `"controlled"` | 可跨越开放边境，但避开受管控的边境 | 

### `options.avoid_countries` 
一个由国家 id 组成的整数数组，用于在使用 **`driving-*`** profile 进行路径规划时排除这些国家。可以与 `"avoid_borders": "controlled"` 一起使用。
国家列表和应用示例可在[国家列表](/technical-details/country-list.md)中找到。

`"[11,193]"` 表示排除奥地利和瑞士。 

### `options.avoid_features`
一个由要避开的要素组成的字符串数组。可用要素如下：

| 要素    | 适用于                               |
|------------|---------------------------------------------|
| `highways` | driving-\*                                  |
| `tollways` | driving-\*                                  |
| `ferries`  | driving-\*、cycling-\*、foot-\*、wheelchair |
| `fords`    | driving-\*、cycling-\*、foot-\*             |
| `steps`    | cycling-\*、foot-\*、wheelchair             |

### `options.avoid_polygons` 
包含路线中需要避开的区域。格式为 [geojson polygon](https://datatracker.ietf.org/doc/html/rfc7946#appendix-A.3) 或 [geojson multipolygon](https://datatracker.ietf.org/doc/html/rfc7946#appendix-A.6)。

### `options.profile_params`
一个对象，包含除 `driving-car` 外所有 profile 的附加路径规划参数：

#### `options.profile_params.weightings`
权重会让指定的因素优先于最短路径。
其值是一个对象，可包含以下属性：

* `steepness_difficulty`：整数，指定 **`cycling-*`** profile 的体能水平。偏好的坡度随该值增大而增大。

    | 值 | 体能水平 |
    |-------|---------------|
    | `0`   | 新手        |
    | `1`   | 中等      |
    | `2`   | 业余       |
    | `3`   | 专业           | 

* `green`：整数值，指定 **`foot-*`** profile 的 Green 因子。

    | 值 | Green 因子                                         |
    |-------|------------------------------------------------------|
    | `0`   | 常规路径规划                                       |
    | `1`   | 相较更短的路线，更偏好穿过绿地的道路 |


* `quiet`：整数值，指定 **`foot-*`** profile 的 Quiet 因子。
    
    | 值 | Quiet 因子                           |
    |-------|----------------------------------------|
    | `0`   | 常规路径规划                         |
    | `1`   | 相较更短的路线，更偏好安静的道路 |

#### `options.profile_params.restrictions` 

[//]: # (see RequestProfileParamsRestrictions)

一个对象，用于指定 `cycling-*`、`driving-hgv` 或 `wheelchair` profile 的限制条件。

* 对于 `driving-hgv`：

  | 参数  | 类型    | 描述                                                                                                                       |
  |------------|---------|-----------------------------------------------------------------------------------------------------------------------------------|
  | `length`   | Number  | 长度限制，单位为米。                                                                                                     |
  | `width`    | Number  | 宽度限制，单位为米。                                                                                                      |
  | `height`   | Number  | 高度限制，单位为米。                                                                                                     |
  | `axleload` | Number  | 轴重限制，单位为吨。                                                                                                     |
  | `weight`   | Number  | 重量限制，单位为吨。                                                                                                       |
  | `hazmat`   | Boolean | 指定是否使用适合运输危险货物的路径规划并避开水源保护区。默认值为 false。 |

* 对于 `wheelchair`：

  | 参数             | 类型    | 描述                                                                                                                 |
  |-----------------------|---------|-----------------------------------------------------------------------------------------------------------------------------|
  | `surface_type`        | String  | 指定最小的[路面类型](http://wiki.openstreetmap.org/wiki/Key:surface)。默认值为 `"cobblestone:flattened"`。 |
  | `track_type`          | String  | 指定路线的[最低质量](http://wiki.openstreetmap.org/wiki/Key:tracktype)。默认值为 `"grade1"`。        |
  | `smoothness_type`     | String  | 指定路线的最低[平滑度](http://wiki.openstreetmap.org/wiki/Key:smoothness)。默认值为 `"good"`。    |
  | `maximum_sloped_kerb` | Number  | 指定倾斜路缘的最大高度，单位为米。取值为 `0.03`、`0.06`（默认）、`0.1` 或 `any`。              |
  | `maximum_incline`     | Integer | 指定最大坡度（百分比）。取值为 `3`、`6`（默认）、`10`、`15` 或 `any`。                                      |
  | `minimum_width`       | Number  | 指定道路的最小宽度，单位为米。                                                                            |

### `options.round-trip` 

一个对象，包含往返路线的规格：

| 参数 | 类型    | 描述                                                                                              |
|-----------|---------|----------------------------------------------------------------------------------------------------------|
| `length`  | Number  | 路线的目标长度，单位为 m（注意这是期望值，实际结果可能不同）。 |
| `points`  | Integer | 路线上使用的点数。值越大，生成的路线越接近环形。                     |
| `seed`    | Integer | 用于为生成路线的整体方向添加随机性的种子（可选）。       |

### `options.vehicle_type`
仅用于 `profile=driving-hgv`。
它是让**车辆限制**生效所必需的（
参见 [tag 过滤](/technical-details/tag-filtering.md#driving-hgv)）。
可选值：

* `hgv`
* `bus`
* `agricultural`
* `delivery`
* `forestry` 
* `goods`



## 示例

一些 `options` 示例，提供可读格式与压缩格式的 JSON 形式：

### 对于 `profile=driving-car`：

```json
{"avoid_features":["ferries","tollways"]}
```

### 对于 `profile=cycling-*`：

```json
{
  "avoid_features": ["steps"],
  "profile_params": {
      "weightings": {
          "steepness_difficulty": 2
      }
  },
  "avoid_polygons": {
    "coordinates": [
      [
        [
          8.683223,
          49.41971
        ],
        [
          8.68322,
          49.41635
        ],
        [
          8.68697,
          49.41635
        ],
        [
          8.68697,
          49.41971
        ],
        [
          8.683223,
          49.41971
        ]
      ]
    ],
    "type": "Polygon"
  }
}
```
```json
{"avoid_features":["steps"],"profile_params":{"weightings":{"steepness_difficulty":2}},"avoid_polygons":{"coordinates":[[[8.683223,49.41971],[8.68322,49.41635],[8.68697,49.41635],[8.68697,49.41971],[8.683223,49.41971]]],"type":"Polygon"}}
```

### 对于 `profile=foot-*`：

```json
{
    "avoid_features": ["fords","ferries"],
    "profile_params": {
        "weightings": {
            "green": {
                "factor": 0.8
              },
            "quiet": {
                "factor": 1.0
            }
        }
    }
}
```
```json
{"avoid_features":["fords","ferries"],"profile_params":{"weightings":{"green":{"factor":0.8},"quiet":{"factor":1.0}}}}
```

### 对于 `profile=driving-hgv`：

```json
{
    "avoid_features": ["ferries","tollways"],
    "profile_params": {
        "restrictions": {
            "length": 30,
            "width": 30,
            "height": 3,
            "axleload": 4,
            "weight": 3,
            "hazmat": true
        }
    }
}
```
```json
{"avoid_features":["ferries","tollways"],"profile_params":{"restrictions":{"length":30,"width":30,"height":3,"axleload":4,"weight":3,"hazmat":true}}}
```

### 对于 `profile=wheelchair`：

```json
{
    "avoid_features": ["ferries","steps"],
    "profile_params": {
        "restrictions": {
            "surface_type": "cobblestone:flattened",
            "track_type": "grade1",
            "smoothness_type": "good",
            "maximum_sloped_kerb": 0.06,
            "maximum_incline": 6
        }
    }
}
```

```json
{"avoid_features":["ferries","steps"],"profile_params":{"restrictions":{"surface_type":"cobblestone:flattened","track_type":"grade1","smoothness_type":"good","maximum_sloped_kerb":0.06,"maximum_incline":6}}}
```

### 边境限制

带有边境限制的 routing options 对象示例：

#### _完全不跨越国境_

```json
{"avoid_borders":"all"}
```

#### _不跨越受管控的边境（例如美国 - 加拿大），但允许跨越开放边境（例如法国 - 德国）_

```json
{"avoid_borders":"controlled"}
```

#### _不经过奥地利或瑞士_

```json
{"avoid_countries": [11,193]}
```

#### _通过开放边境，但不进入瑞士_

```json
{"avoid_borders": "controlled","avoid_countries": [193]}
```
