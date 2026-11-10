# `ors.engine`

Engine 属性在启动时构建图阶段使用。

openrouteservice 可以同时运行多个不同的配置，每个配置都有自己的一组参数。
这些配置在配置文件的 `profiles` 部分中定义。`profile_default` 部分用于
为所有配置定义默认值。`profiles` 部分用于为各个
配置定义特定的值。更多信息请参见[配置](/run-instance/configuration/engine/profiles/index.md)。

| key                | type    | description                                                                                                                                                                                                                                                                                          | default value |
|--------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| init_threads       | number  | 用于初始化（构建/加载）图的线程数。数值越大需要越多 RAM                                                                                                                                                                                                       | `1`           |
| preparation_mode   | boolean | 如果设置，将构建图和预处理数据，但随后应用程序会立即关闭，不启动任何服务                                                                                                                                                            | `false`       |
| config_output      | string  | 如果设置，默认配置值将写入以该值为名的文件。随后应用程序会立即关闭，不启动任何服务。详情请参见[配置](/run-instance/configuration/how-to-configure.md#configuration-defaults-output)页面。      |               |
| graphs_data_access | string  | 定义如何创建 DataAccess 对象。 <br> - `MMAP`：内存映射存储 <br> - `RAM_STORE`：带安全/刷新选项的内存存储。<br> 更多信息见[源代码](https://github.com/GIScience/graphhopper/blob/ors_4.0/core/src/main/java/com/graphhopper/storage/DAType.java) | `RAM_STORE`   |
| elevation          | object  | 参见[高程属性](elevation.zh.md)                                                                                                                                                                                                                                                             |               |
| graph_management   | object  | 参见[图管理属性](graph-management.zh.md)                                                                                                                                                                                                                                               |               |
| profile_default    | object  | 在[配置](/run-instance/configuration/engine/profiles/index.md)中说明                                                                                                                                                                                                                        |               |
| profiles           | object  | 在[配置](/run-instance/configuration/engine/profiles/index.md)中说明                                                                                                                                                                                                                        |               |
