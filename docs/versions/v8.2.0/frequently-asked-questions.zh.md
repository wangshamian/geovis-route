---
title: 常见问题
---

# 常见问题

## 为什么我的 ors 报 `Could not find point`？

这是一条很常见的错误信息：
```
Could not find point 0: 25.3531986 51.5214311 within a radius of 350.0 meters.;
Could not find point 1: 25.3524229 51.4627229 within a radius of 350.0 meters.
```

出现该问题的原因主要有三种，下面按出现频率从高到低列出。

1. 如果两个点都找不到，很可能只是把经度（Long）和纬度（Lat）弄反了。我们的
   API 期望坐标按文档中描述的 [lon,lat] 顺序传入
   （可点击帮助按钮查看参数说明）。输出同样遵循 GeoJSON 规范，
   也是 [lon,lat] 顺序。

2. 给定的起点和终点距离任何可通行道路超过 350m。我们的 API 中，吸附到路段
   的最大距离为 350m。
   本地部署时可以通过
  `maximum_snapping_radius` 和 `location_index_resolution` 配置参数进行自定义。详情参见
  [配置](run-instance/configuration/ors/engine/profiles.zh.md)。

3. 起点和终点按正确的 lon,lat 顺序传入，且与可通行道路的距离在 350m 以内。
   这种情况应该只会出现在本地部署中。
   通常这意味着 ors 正在尝试在一个尚未为其构建图（graph）的区域中规划路线。
   如果能够找到海德堡（德国）的路线，说明 ors 仍在默认数据集上运行。

## openrouteservice 中的 OSM 数据何时更新？

openrouteservice 使用 `planet.osm.pbf` 文件构建其数据。根据
[osm-wiki](https://wiki.openstreetmap.org/wiki/Planet.osm) 的说明，这些文件
需要两天时间构建，并且每周更新一次。

由于 `planet` 文件相当大（目前超过 60GB），因此需要做一些工作
来确保下载正确、文件在任何意义上都没有损坏，并且确实是新的。
这一过程的一部分由 OSM 负责，一部分由 openrouteservice 完成。

一旦最新的 `planet` 文件到达 openrouteservice 服务器上，就需要先对其进行预处理，
之后 openrouteservice 才能开始构建用于路径规划的图。

构建过程本身[相当消耗
资源](run-instance/system-requirements.zh.md)。对于九个 profile 中的任意一个，
大约需要两天时间。按照前面提到的资源需求，
这意味着重新构建全部 profile 大约需要一周。

图构建完成后，生产实例必须加载它们。由于这一操作应当
安排在低流量时段，因此也被安排为每周执行一次。

总结一下：如果你在 OSM 中做了任何修改，大约需要一周时间才会被包含进
`planet` 文件中。该文件每周被读取一次，构建需要一周，重新加载图也是每周一次。

如果一切都按预期顺利进行，变更应该会在两到三周内反映到
openrouteservice 中。

不过，如果任何环节出了问题，通常意味着至少会延迟一周
（假设问题被立即发现并修复）。如果变更在一个月内仍未反映出来，也无需担心。

## 我遇到错误 `Native memory allocation (mmap) failed to map 16384 bytes for committing reserved memory`

请参见[系统要求](run-instance/system-requirements.zh.md#memory-mapping-in-large-builds-with-a-containerized-openrouteservice-instance)中的内存映射章节。

## 明明有路，为什么路径规划不工作？

导致路径规划看起来“错误”的原因有很多，因为有些道路存在却没有被使用。
在很多情况下，这更多是数据问题而非路径规划问题。
一种非常常见的情况与 OSM 中的 `barrier=*` 节点有关。

这个问题最常见的表现形式有两种：

1. 明明存在轮渡，且应该更快/更短，却没有被采用
2. 住宅区域内的道路无法从外部进入。

这两种情况下，通往轮渡码头或住宅区域的道路上可能有一个带有 `barrier=*` 标签的节点。
相应的障碍物设置是为了禁止未经授权的进入。
因此，如果只标注了 `barrier=*` 而没有更多信息，openrouteservice 就不会在这些节点上规划路线。

这种行为常常被误解为“错误”，因为在很多情况下障碍物默认是可以通过的，
或者可以购买门票通过。
虽然对于查看地图的人来说往往一目了然，但 openrouteservice 无法知道这一点。
在这样的障碍物上规划路线相当于做一种假设，而 openrouteservice 不会做这种假设。

存在[大量](https://wiki.openstreetmap.org/wiki/Key:access#List_of_possible_values)
[选项](https://wiki.openstreetmap.org/wiki/Key:locked)可以
[启用](https://wiki.openstreetmap.org/wiki/Tag:access%3Ddestination)路径规划，
但这些必须在数据中设置，而不是在路径规划引擎中设置。

## 为什么我自己实例中 matrix 请求的响应时间比在线 API 慢这么多
取决于请求的参数，openrouteservice 会使用
不同的路径规划算法以及不同的预处理来计算
结果。

对于 matrix 计算，ors 使用非常快速的 RPHAST 算法，它基于
所谓的*收缩层次*（Contraction Hierarchies，简称 CH）。虽然使用
CH 能大幅加快 matrix 计算，但预处理它们的代价相当高。
因此，默认情况下不会计算它们，必须手动开启。
关于如何操作的文档可以参见[此处](https://giscience.github.io/openrouteservice/run-instance/configuration/ors/engine/profiles#methods-ch)。

如果没有预先准备 CH，matrix 计算将回退到 Dijkstra
算法，该算法要慢得多，也是响应缓慢的原因。

## 我的配额何时以及如何重置？
当你查看我们的[套餐](https://openrouteservice.org/plans/)时，你会看到
我们的端点同时设有每日和每分钟的请求限制。

所有剩余配额都会显示在你的[开发者面板](https://openrouteservice.org/dev/#/home)中。

每日限额在你首次发起任何请求之后开始计算，24 小时后重置。
因此，你的 24 小时窗口可能会随着天数推移而发生偏移。

举个例子，我们来看 `directions` 端点及其默认限制：每天 2000 次请求。
如果你今天下午 3 点请求了 1000 次 directions，明天上午 10 点尝试
请求 1500 次，那么最后 500 次会失败，因为你的配额要到明天下午 3 点才重置。
下午 3 点之后，你又能发起最多 2000 次请求，但如果你在下午 6 点这样做，
就必须等到后天的下午 6 点才能再请求 directions。

每分钟限额以滑动窗口方式执行，即任意连续 60 秒内
最多只能包含 40 次 directions 请求。

如果达到每日限额，你会收到 `403 - Forbidden` HTTP 错误。
如果达到每分钟限额，你会收到 `429 - Too many requests` HTTP 错误。
剩余每日配额也可以通过程序化方式查询，比较 `x-ratelimit-remaining` 和 `x-ratelimit-reset` 响应头即可。
