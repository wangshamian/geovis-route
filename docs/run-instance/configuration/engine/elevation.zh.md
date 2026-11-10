
# `ors.engine.elevation`

与构建图时使用的高程信息相关的设置。

| key          | type    | description                                                                                                                                                                                                                                                                                                                       | default value       |
|--------------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|
| preprocessed | boolean | 切换是否读取 OSM `ele` 节点标签。如果启用，将阻止 graphhopper 的高程查询，所有没有 `ele` 标签的节点将默认为 0。旨在与 ORS 预处理器配合使用                                                                                                        | `false`             |
| cache_clear  | boolean | 高程数据下载后是否保留                                                                                                                                                                                                                                                                                   | `false`             |
| provider     | string  | 高程提供者的名称。可能的值有 `multi`、`cgiar` 或 `srtm`                                                                                                                                                                                                                                                 | `multi`             |
| cache_path   | string  | 存储 SRTM 瓦片的目录路径                                                                                                                                                                                                                                                                        | `./elevation_cache` |
| data_access  | string  | 定义如何创建 DataAccess 对象。 <br> - `MMAP`：内存映射存储 <br> - `RAM_STORE`：带安全/刷新选项的内存存储。可用于小范围区域 <br> 更多信息见[源代码](https://github.com/GIScience/graphhopper/blob/ors_4.0/core/src/main/java/com/graphhopper/storage/DAType.java) | `MMAP`              |
