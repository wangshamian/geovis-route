# `ors.engine.profiles.<PROFILE-NAME>.repo`

`ors.engine.profiles.<PROFILE-NAME>.repo` 下的属性用于在图仓库中定位一个图。
如果*所有*这些属性都已设置，
并且图管理已
启用 [ors.engine.graph_management.enabled](/run-instance/configuration/engine/graph-management.md)，
openrouteservice 将使用指定的仓库来加载图数据。

| key                      | type   | description                                                                                                                                                | example values                                                                                        |
|--------------------------|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| repository_uri           | string | 基础 URL（对于 http 或 minio 仓库）或路径（对于文件系统仓库）                                                                       | * `http://some.domain.ors`<br/>* `file:///absolute/path`<br/>* `/absolute/path`<br/>* `relative/path` |
| repository_name          | string | 一个编辑性名称，代表图的目标群体或供应商。                                                                                  | `public`, `my-organization`                                                                           |
| repository_profile_group | string | 具有特定特性的一组配置，例如使用交通数据或预计算高级数据结构（如用于 fastisochrones） | `traffic`, `fastiso`                                                                                  |
| graph_extent             | string | 图覆盖的地理区域。这对应于用于构建图的 OSM PBF 文件。                                                  | `planet`                                                                                              |
| repository_user          | string | 用于在仓库进行认证的用户名（key_id）。目前仅由 minio 客户端使用。                                                      | `username`                                                                                            |
| repository_password      | string | 用于在仓库进行认证的密码（access_key）。目前仅由 minio 客户端使用。                                                   | `password`                                                                                            |

每个路由配置都可以有各自独立的仓库参数，
这使得可以拥有来自不同仓库
或不同地理区域的路由配置。

所有路由配置共有的仓库属性可以配置
在 [ors.engine.profile_default](/run-instance/configuration/engine/index.md) 中

更多信息请参见[图仓库客户端](/technical-details/graph-repo-client/)。
