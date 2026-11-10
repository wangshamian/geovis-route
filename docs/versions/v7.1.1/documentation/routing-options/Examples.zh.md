# 示例

**如果你的请求在不带 options 对象时可以正常工作，带上它却返回错误：请尝试对 options 对象做 [%-编码](Routing-Options#url-encoding)！**

以下是一些 options 示例，分别以易读形式与压缩后的 JSON 形式给出：

对于 `profile=driving-car`：

```json
{
    "avoid_features": ["ferries", "tollways"]
}
```
`{"avoid_features":["ferries","tollways"]}`

对于 `profile=cycling-*`：

```json
{
  "avoid_features": ["steps"],
  "profile_params": {
      "weightings": {
          "steepness_difficulty": 2
      }
  },
  "avoid_polygons": {
      "type": "Polygon",
      "coordinates": [
          [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]
   ]}
}
```
`{"avoid_features":["steps"],"profile_params":{"weightings":{"steepness_difficulty":2}}}},"avoid_polygons":{"type":"Polygon","coordinates":[[[100.0,0.0],[101.0,0.0],[101.0,1.0],[100.0,1.0],[100.0,0.0]]]}}`

对于 `profile=foot-*`：

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
    },
    "avoid_polygons": {  
        "type": "Polygon",
        "coordinates": [
            [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]
     ]}
}
```
`{"avoid_features":["fords","ferries"],"profile_params":{"weightings":{"green":{"factor":0.8},"quiet":{"factor":1.0}}},"avoid_polygons":{"type":"Polygon","coordinates":[[[100.0,0.0],[101.0,0.0],[101.0,1.0],[100.0,1.0],[100.0,0.0]]]}}`

对于 `profile=driving-hgv`：

```json
{
    "avoid_features": ["ferries","tollways"],
    "vehcile_type": "hgv",
    "profile_params": {
        "restrictions": {
            "length": 30,
            "width": 30,
            "height": 3,
            "axleload": 4,
            "weight": 3,
            "hazmat": true
        }
    },
    "avoid_polygons": {  
        "type": "Polygon",
        "coordinates": [
            [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]
     ]}
}
```
`{"avoid_features":["ferries","tollways"],"vehicle_type":"hgv","profile_params":{"restrictions":{"length":30,"width":30,"height":3,"axleload":4,"weight":3,"hazmat":true}},"avoid_polygons":{"type":"Polygon","coordinates":[[[100.0,0.0],[101.0,0.0],[101.0,1.0],[100.0,1.0],[100.0,0.0]]]}}`

对于 `profile=wheelchair`：

```json
{
    "avoid_features": ["ferries","steps"],
    "profile_params": {
        "restrictions": {
            "surface_type": "cobblestone:flattened",
            "track_type": "grade1",
            "smoothness_type": "good",
            "maximum_sloped_curb": 0.06,
            "maximum_incline": 6
        }
    }
}
```
`{"avoid_features":["ferries","steps"],"profile_params":{"restrictions":{"surface_type":"cobblestone:flattened","track_type":"grade1","smoothness_type":"good","maximum_sloped_curb":0.06,"maximum_incline":6}}}`

## 边界限制

带边界限制的路线规划选项对象示例：

*完全不跨越国界：*

```json
{
    "avoid_borders":"all"
}
```

`{"avoid_borders":"all"}`

*不跨越受管控的边界（例如美国—加拿大），但允许跨越开放边界（例如法国—德国）：*

```json
{
    "avoid_borders":"controlled"
}
```

`{"avoid_borders":"controlled"}`

*不途经奥地利或瑞士：*

```json
{
    "avoid_countries": [1,120]
}
```

`{"avoid_countries": [1,120]}`

*可以通过开放边界，但不得进入瑞士：*

```json
{
    "avoid_borders": "controlled",
    "avoid_countries": [193]
}
```

`{"avoid_borders": "controlled","avoid_countries": [193]}`
