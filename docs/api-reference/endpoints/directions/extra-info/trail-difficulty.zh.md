# 路线难度

directions 响应中以下位置取值的取值列表

```jsonpath
$.routes[*].extras.traildifficulty.values
```

该额外信息提供[徒步](https://wiki.openstreetmap.org/wiki/Key:sac_scale)以及[山地自行车](https://wiki.openstreetmap.org/wiki/Key:mtb:scale)路线的难度信息。

| 值 | foot-\*                              | cycling-\* |
|:-----:|-------------------------------------|-------------|
|   0   | 无 tag                              | 无 tag      |
|   1   | sac_scale=hiking                    | mtb:scale=0 |
|   2   | sac_scale=mountain_hiking           | mtb:scale=1 |
|   3   | sac_scale=demanding_mountain_hiking | mtb:scale=2 |
|   4   | sac_scale=alpine_hiking             | mtb:scale=3 |
|   5   | sac_scale=demanding_alpine_hiking   | mtb:scale=4 |
|   6   | sac_scale=difficult_alpine_hiking   | mtb:scale=5 |
|   7   |                 ---                 | mtb:scale=6 |

[//]: # (keep in sync with TrailDifficultyScaleGraphStorageBuilder.getSacScale)
