# Extra Info

在请求路线时，可以请求若干“extra info”项，以获得关于该路线的更多信息。这些信息可能包括路面、小径等级或 OpenStreetMap way ID 等。下表详细列出了**在 https://api.openrouteservice.org 提供的路线规划中**，每个 profile 可用的 extra info 项。

| 参数 | 描述 | 编码信息 |
|:---------:|:------------|:-----------------|
| steepness | 提供路线各部分陡峭程度的信息 | [陡峭度 ID](Steepness)
| suitability | 根据路线特征和 profile 判断该道路的适合程度 | 1（不适合）- 10（非常适合）
| surface | 沿途的路面覆盖情况 | [路面 ID](Surface)
| waycategory | 路线各部分的特定类别（收费道路、高速公路、浅滩等） | [类别 ID](Waycategory)
| waytype | 路线中使用的道路和小径类型 | [类型 ID](Waytype)
| tollways  | 路线穿过的任何收费道路。某条道路是否被标记为[收费道路](https://wiki.openstreetmap.org/wiki/Key:toll)取决于 `profile` | 0（非收费道路）或 1（收费道路） |
| traildifficulty | 依据 sac 或山地自行车分级判断道路各部分的难度 | [难度 ID](Trail-Difficulty)  |
| osmid | 路线所用道路的 OpenStreetMap way ID |   |
| roadaccessrestrictions | 关于可能存在通行限制的道路的信息（例如私家道路、仅限目的地通行） | [限制 ID](Road-Access-Restrictions) |
| countryinfo | 道路各部分所在的国家 | [国家 ID](../routing-options/Country-List)
| green | 路线各部分的“绿色”程度（受树木数量、公园、河流等因素影响） | 0（几乎没有绿地）- 10（大量绿地） |
| noise | 路线各部分的噪声程度（受与高速公路距离等因素影响） | 0（安静）- 10（嘈杂） |

# Extra Info 可用性

|      |    steepness  | suitability  | surface  | waycategory  | waytype | tollways | traildifficulty | osmid | roadaccessrestrictions | countryinfo | green | noise |
|:-----------:|:----------------:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| driving-car | x | x | x | x | x | x | x |   | x | x |   |   |
| driving-hgv | x | x | x | x | x | x | x |   | x | x |   |   |
| cycling-regular | x | x | x | x | x |   | x |   |   |   |   |   |
| cycling-mountain | x | x | x | x | x |   | x |   |   |   |   |   |
| cycling-road | x | x | x | x | x |   | x |   |   |   |   |   |
| foot-walking | x | x | x | x | x |   | x |   |   |   | x | x |
| foot-hiking | x | x | x | x | x |   | x |   |   |   | x | x |
| wheelchair | x | x | x | x | x |   | x | x |   |   |   |   |
