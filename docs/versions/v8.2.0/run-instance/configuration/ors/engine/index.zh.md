
# `ors.engine`

引擎属性在启动时构建图所需的。

::: warning
如果你设置了 `profile_default` 设置，它们可能不会被考虑！
这将在下一个补丁版本中修复。
作为变通方法，你可以将所有 `profile_default` 设置移动到你需要它们生效的具体配置中。
:::

| key                | type    | description                                                                                                                                                                                                                                                                                          | default value                              |
|--------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|
| init_threads       | number  | 用于初始化（构建/加载）图的线程数。数值越高需要越多 RAM                                                                                                                                                                                                                                              | `1`                                        |
| preparation_mode   | boolean | 如果设置，将构建图和预处理，但随后应用程序会立即关闭，不启动任何服务                                                                                                                                                                                                                                 | `false`                                    |
| source_file        | string  | 要使用的 OSM 文件，支持的格式有 `.osm`、`.osm.gz`、`.osm.zip` 和 `.pbf`                                                                                                                                                                                                                              | `ors-api/src/test/files/heidelberg.osm.gz` |
| graphs_root_path   | string  | 用于存储图的目录根路径                                                                                                                                                                                                                                                                               | `./graphs`                                 |
| graphs_data_access | string  | 定义如何创建 DataAccess 对象。<br> - `MMAP`：内存映射存储 <br> - `RAM_STORE`：带安全/刷新选项的内存存储。<br> 更多信息见[源代码](https://github.com/GIScience/graphhopper/blob/ors_4.0/core/src/main/java/com/graphhopper/storage/DAType.java) | `RAM_STORE`                                |
| elevation          | object  | 参见[高程属性](elevation.md)                                                                                                                                                                                                                                                                      |                                            |
| profile_default    | object  | 在[配置属性](profiles.md)中描述                                                                                                                                                                                                                                                                   |                                            |
| profiles           | object  | 在[配置属性](profiles.md)中描述                                                                                                                                                                                                                                                                   |                                            |
