# 陡峭度

directions 响应中以下位置取值的取值列表

```jsonpath
$.routes[*].extras.steepness.values
```

| 值 |           编码 |
|:-----:|-------------------:|
|  -5   |      下坡 >=16% |
|  -4   | 下坡 10% - <16% |
|  -3   |  下坡 7% - <10% |
|  -2   |   下坡 4% - <7% |
|  -1   |   下坡 1% - <4% |
|   0   |   上坡 0% - <1% |
|   1   |   上坡 1% - <4% |
|   2   |   上坡 4% - <7% |
|   3   |  上坡 7% - <10% |
|   4   | 上坡 10% - <16% |
|   5   |      上坡 >=16% |

[//]: # (keep in sync with org.heigit.ors.routing.util.SteepnessUtil.getCategory )
