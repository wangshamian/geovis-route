# 路径规划选项

通过请求体参数 `options`，可以为 directions 请求指定高级路径规划选项。

`options` 参数是一个 JSON 对象，可以同时设置此处提到的多个参数。
关于其结构请参阅[示例](#examples)。

::: warning
下面部分选项依赖图中包含的辅助数据，参见[外部存储](../../../run-instance/configuration/ors/engine/profiles.zh.md#ext_storages)
:::

## 可用参数

### `options.avoid_borders`
字符串值，用于指定要避开哪些边界。仅适用于 **`driving-*`** profile。

| 取值          | 描述                                  |
|----------------|----------------------------------------------|
| `"all"`        | 不跨越任何边界                               |
| `"controlled"` | 可以跨越开放边界，但避开受管控的边界         | 

### `options.avoid_countries` 
一个整数数组，指定在使用 **`driving-*`** profile 进行路径规划时要排除的国家 id。可与 `"avoid_borders": "controlled"` 一起使用。
国家列表及应用示例可参见[国家列表](../../../technical-details/country-list.zh.md)。

`"[11,193]"` 表示排除奥地利和瑞士。 

### `options.avoid_features`
一个字符串数组，指定要避开的要素。可用的要素有：

| 要素    | 可用 profile                               |
|------------|---------------------------------------------|
| `highways` | driving-\*                                  |
| `tollways` | driving-\*                                  |
| `ferries`  | driving-\*, cycling-\*, foot-\*, wheelchair |
| `fords`    | driving-\*, cycling-\*, foot-\*             |
| `steps`    | cycling-\*, foot-\*, wheelchair             |

### `options.avoid_polygons` 
包含路线中需要避开的区域。格式为 [geojson polygon](https://datatracker.ietf.org/doc/html/rfc7946#appendix-A.3) 或 [geojson multipolygon](https://datatracker.ietf.org/doc/html/rfc7946#appendix-A.6)。

### `options.profile_params`
一个对象，包含除 `driving-car` 之外所有 profile 的额外路径规划参数：

#### `options.profile_params.weightings`
加权会优先考虑指定的因素，而非最短路径。
其值是一个对象，可以包含以下属性：

* `steepness_difficulty`：整数，指定 **`cycling-*`** profile 的体能水平。该值越大，偏好的坡度也越陡。

    | 取值 | 体能水平 |
    |-------|---------------|
    | `0`   | Novice        |
    | `1`   | Moderate      |
    | `2`   | Amateur       |
    | `3`   | Pro           | 

* `green`：整数值，指定 **`foot-*`** profile 的绿色因子。

    | 取值 | 绿色因子                                         |
    |-------|------------------------------------------------------|
    | `0`   | normal routing                                       |
    | `1`   | prefer ways through green areas over a shorter route |


* `quiet`：整数值，指定 **`foot-*`** profile 的安静因子。
    
    | 取值 | 安静因子                           |
    |-------|----------------------------------------|
    | `0`   | normal routing                         |
    | `1`   | prefer quiet ways over a shorter route |

#### `options.profile_params.restrictions` 

[//]: # (see RequestProfileParamsRestrictions)

一个对象，用于为 `cycling-*`、`driving-hgv` 或 `wheelchair` profile 指定限制条件。

* 对于 `driving-hgv`：

  | 参数  | 类型    | 描述                                                                                                                       |
  |------------|---------|-----------------------------------------------------------------------------------------------------------------------------------|
  | `length`   | Number  | 长度限制，单位为米。                                                                                                              |
  | `width`    | Number  | 宽度限制，单位为米。                                                                                                              |
  | `height`   | Number  | 高度限制，单位为米。                                                                                                              |
  | `axleload` | Number  | 轴载荷限制，单位为吨。                                                                                                            |
  | `weight`   | Number  | 重量限制，单位为吨。                                                                                                              |
  | `hazmat`   | Boolean | 指定是否使用适用于危险品运输的路径规划，并避开水源保护区。默认为 false。                                                          |

* 对于 `wheelchair`：

  | 参数             | 类型    | 描述                                                                                                                 |
  |-----------------------|---------|-----------------------------------------------------------------------------------------------------------------------------|
  | `surface_type`        | String  | 指定最低的[路面类型](http://wiki.openstreetmap.org/wiki/Key:surface)。默认为 `"cobblestone:flattened"`。                   |
  | `track_type`          | String  | 指定路线的最低[质量](http://wiki.openstreetmap.org/wiki/Key:tracktype)。默认为 `"grade1"`。                                 |
  | `smoothness_type`     | String  | 指定路线的最低[平滑度](http://wiki.openstreetmap.org/wiki/Key:smoothness)。默认为 `"good"`。                                |
  | `maximum_sloped_kerb` | Number  | 指定倾斜路缘石的最大高度，单位为米。取值为 `0.03`、`0.06`（默认）、`0.1` 或 `any`。                                        |
  | `maximum_incline`     | Integer | 指定最大坡度，以百分比表示。可取 `3`、`6`（默认）、`10`、`15` 或 `any`。                                                    |
  | `minimum_width`       | Number  | 指定道路的最小宽度，单位为米。                                                                                              |

### `options.round-trip` 

一个包含往返路线（round-trip）规格的对象：

| 参数 | 类型    | 描述                                                                                              |
|-----------|---------|----------------------------------------------------------------------------------------------------------|
| `length`  | Number  | 目标路线长度，单位为 m（请注意这是一个期望值，实际结果可能不同）。                                       |
| `points`  | Integer | 路线上使用的点数。取值越大，生成的路线越接近环形。                                                       |
| `seed`    | Integer | 用于为生成路线的总体方向添加随机性的种子（可选）。                                                       |

### `options.vehicle_type`
仅适用于 `profile=driving-hgv`。
它是**车辆限制**生效所必需的（参见[标签过滤](../../../technical-details/tag-filtering.zh.md#driving-hgv)）。
可能的取值：

* `hgv`
* `bus`
* `agricultural`
* `delivery`
* `forestry` 
* `goods`



## 示例

以下是一些 `options` 示例，分别以易读格式和压缩后的 JSON 形式给出：

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

### 边界限制

带有边界限制的路径规划选项对象示例：

#### _完全不跨越国界_

```json
{"avoid_borders":"all"}
```

#### _不跨越受管控的边界（例如美国 - 加拿大），但允许跨越开放边界（例如法国 - 德国）_

```json
{"avoid_borders":"controlled"}
```

#### _不经过奥地利或瑞士_

```json
{"avoid_countries": [11,193]}
```

#### _可以通过开放边界，但不进入瑞士_

```json
{"avoid_borders": "controlled","avoid_countries": [193]}
```
