# 路线属性

通过请求体参数 `attributes`，可以在 directions 请求中请求路段（segment）的额外属性：

```json
"attributes":["avgspeed","detourfactor","percentage"]
```

可选值如下：

| 值        | 描述                                                                                                                                                          |
|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| avgspeed     | 该值单位为 _km/h_，等于该路段在评分并应用各因子之后的平均速度。                                                        |
| detourfactor | 该值是一个 _factor_（系数），表示该路段的相对长度，即相对于路线段起点与终点之间直线距离的长度。 |
| percentage   | 该值单位为 _percent_（百分比），以路线长度的占比表示该路段长度。                                                                                | 

在响应中，这些额外属性可以在以下位置找到

```jsonpath
$.routes[*].segments[*].avgspeed
$.routes[*].segments[*].detourfactor
$.routes[*].segments[*].percentage
```
