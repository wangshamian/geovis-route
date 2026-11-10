
# `ors.engine.elevation`

与构建图时的高程信息相关的设置。

| key          | type    | description                                                                                                                                                                                                                                                                                                                       | default value       |
|--------------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|
| preprocessed | boolean | 开启或关闭读取 OSM `ele` 节点标签。如果启用，将阻止 graphhopper 的高程查找，所有没有 `ele` 标签的节点将默认为 0。旨在与 ORS 预处理器结合使用                                                                                                                        | `false`             |
| cache_clear  | boolean | 高程数据一经下载即保留                                                                                                                                                                                                                                                                                                            | `false`             |
| provider     | string  | 高程提供程序的名称。可能的值为 `multi`、`cgiar` 或 `srtm`                                                                                                                                                                                                                                                                         | `multi`             |
| cache_path   | string  | 用于存储 SRTM 瓦片的目录路径                                                                                                                                                                                                                                                                                                      | `./elevation_cache` |
| data_access  | string  | 定义如何创建 DataAccess 对象。<br> - `MMAP`：内存映射存储 <br> - `RAM_STORE`：带安全/刷新选项的内存存储。可用于小区域 <br> 更多信息见[源代码](https://github.com/GIScience/graphhopper/blob/ors_4.0/core/src/main/java/com/graphhopper/storage/DAType.java) | `MMAP`              |
