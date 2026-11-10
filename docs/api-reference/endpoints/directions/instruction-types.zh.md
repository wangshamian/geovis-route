# 指示类型

[//]: # (keep in sync with org.heigit.ors.routing.instructions.InstructionType; PT_ENTER, PT_TRANSFER, PT_EXIT are replaced with street names )


下表给出了路线某个路段（segment）的某个步骤（step）中，directions 响应 `type` 字段里指示类型的编码。

json 响应中的 JSON-Path：
```jsonpath
$.routes[*].segments[*].steps[*].type
```

geojson 响应中的 JSON-Path：
```jsonpath
$.features[*].properties.segments[*].steps[*].type
```

| 值 | 编码         |
|-------|------------------|
| 0     | 左转             |
| 1     | 右转            |
| 2     | 急左转       |
| 3     | 急右转      |
| 4     | 稍向左转      |
| 5     | 稍向右转     |
| 6     | 直行         |
| 7     | 进入环岛 |
| 8     | 驶出环岛  |
| 9     | 掉头           |
| 10    | 终点             |
| 11    | 出发           |
| 12    | 靠左行驶        |
| 13    | 靠右行驶       |
