# 路线属性

通过请求体参数 `attributes`，可以在 directions 请求中请求路线段的附加属性：

```json
"attributes":["avgspeed","detourfactor","percentage"]
```

可用的取值有：

| 取值        | 描述                                                                                                                                                          |
|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| avgspeed     | 该值以 _km/h_ 为单位，表示按坡度分级并应用各项因子后，该路段（way segment）的平均速度。                                                                              |
| detourfactor | 该值是一个 _系数_，表示该路线段的相对长度，即路线段长度与其起点和终点之间直线距离长度之比。                                                                          |
| percentage   | 该值以 _百分比_ 为单位，表示该段长度占路线总长度的比例。                                                                                                              | 

在响应中，这些附加属性可以在以下位置找到

```jsonpath
$.routes[*].segments[*].avgspeed
$.routes[*].segments[*].detourfactor
$.routes[*].segments[*].percentage
```
