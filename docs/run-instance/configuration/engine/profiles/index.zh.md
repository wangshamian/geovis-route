# `ors.engine.profiles`

`profiles` 对象是一个映射，包含你要使用的每个配置的键-对象对。
openrouteservice 可以同时运行多个不同的配置，
每个配置都有自己的参数集合，
受系统可用内存的限制。

每个配置都有一个**配置名称**（即该映射中的键），
它被用作 API 中的_路径参数_
（配置 `truck` 的 directions 请求：`http://.../ors/v2/directions/truck/json`），
同时也作为该配置路由图的_目录名_。
注意，配置名称可以自由选择，
但不能包含你的操作系统上不能用于目录名的特殊字符。

每个配置都需要通过 `encoder_name` 属性指定一个**标志编码器**（flag encoder），
它决定在图构建期间如何将 OSM 数据转换为路由图。
具体而言，标志编码器定义了哪些 OSM 道路会被转换为图的边（取决于 OSM 标签）、
如何估算每条边的行驶速度，
以及存储来自其他标签的哪些信息。
所提供的标志编码器对应于特定的车辆类型，并且必须为每个配置设置。

| `encoder_name` 的可能值 |
|------------------------------------|
| `driving-car`                      |
| `driving-hgv`                      |
| `cycling-regular`                  |
| `cycling-mountain`                 |
| `cycling-road`                     |
| `cycling-electric`                 |
| `foot-walking`                     |
| `foot-hiking`                      |
| `wheelchair`                       |
| `public-transport`（实验性）  |

每个编码器名称都有一个**默认配置**，其名称和 `encoder_name` 与之对应，
并带有一套推荐且合理的配置选项。
默认配置只需启用即可使用，
例如 `ors.engine.profiles.driving-car.enabled=true`。
对于默认配置，设置 `encoder_name` 显然不是必需的。
你还可以通过覆盖[内部默认值](/run-instance/configuration/how-to-configure.md#internal-defaults)进一步定制这样的配置。

`ors.engine.profile_default` 对象用于为_所有配置_定义默认值。
它接受与 `profiles` 对象相同的属性，
但这些设置会应用于所有配置，除非被特定配置的设置覆盖。
通过设置 `ors.engine.profile_default.enabled=true`，你可以在自定义配置中激活所有配置，
但要注意，该设置也会启用所有默认配置。

::: warning
预定义默认配置的设置会覆盖你在 `ors.engine.profile_default` 中指定的设置！
详情请参见[为所有路由配置定义默认值](/run-instance/configuration/how-to-configure.md#defining-defaults-for-all-routing-profiles)。
要避免这种情况，请使用自定义名称来定义你的配置。
:::

| key          | type    | description                                                                                                                                                                   | default value |
|--------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| enabled      | boolean | 在 openrouteservice 各端点中启用或禁用该配置                                                                                                             | `false`       |
| encoder_name | string  | 用于此配置的编码器名称。可能的值仅限于上表中的值！                                                                               | _NA_          |
| graph_path   | string  | 存储图的目录根路径。默认为 `graphs`。会为每个配置创建一个与配置名称同名的子目录                      | `graphs`      |
| build        | object  | [图构建阶段](build.zh.md)的参数                                                                                                                           |               |
| repo         | object  | 关于[图仓库引用](repo.zh.md)的参数，用于通过[图仓库客户端](/technical-details/graph-repo-client/index.md)下载预计算的图 |               |
| service      | object  | 运行[服务](service.zh.md)时所需的、每个配置特有的参数                                                                                  |               |
