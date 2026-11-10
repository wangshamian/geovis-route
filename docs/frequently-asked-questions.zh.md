---
title: FAQ
---

# 常见问题

## 为什么我的 openrouteservice 实例会报 `Could not find point`？

这是一条经常遇到的错误信息：
```
Could not find point 0: 25.3531986 51.5214311 within a radius of 350.0 meters.;
Could not find point 1: 25.3524229 51.4627229 within a radius of 350.0 meters.
```

造成该问题的原因主要有三种，按出现频率从高到低排列。

1. 如果两个点都找不到，可能只是你把纬度和经度弄反了。我们的
   API 按照文档所述，期望坐标采用 [lon,lat] 顺序
   （查看帮助按钮以获取参数信息）。输出同样按照
   GeoJSON 规范采用 [lon,lat] 顺序。

2. 给定的起点和终点距任何可路由道路都超过 350m。在我们的 API 中，吸附到路段的
   最大距离为 350m。
 对于本地安装，可以通过
  `maximum_snapping_radius` 和 `location_index_resolution` 配置参数进行自定义。详见
   [配置](run-instance/configuration/engine/profiles/index.zh.md)。

3. 起点和终点以正确的 lon,lat 顺序传入，并且距可路由道路在
   350m 以内。这种情况应只会在本地安装时发生。
   通常这意味着 ors 正在尝试在尚未构建图数据的区域内进行路径规划。
   如果能够找到 Heidelberg（德国）范围内的路线，说明 openrouteservice 仍在
   使用默认数据集运行。

## openrouteservice 中的 OSM 数据何时更新？

Openrouteservice 基于 `planet.osm.pbf` 文件构建数据。根据
[osm-wiki](https://wiki.openstreetmap.org/wiki/Planet.osm) 的说明，这些文件
需要两天时间构建，并且每周更新一次。

由于 `planet` 文件相当大（目前超过 60GB），要确保下载正确、文件没有以任何方式
损坏且确实是新版本，需要做一些工作。这一过程的某些部分由
OSM 负责，部分由 openrouteservice 负责。

一旦最新的 `planet` 文件到达 openrouteservice 服务器，就需要
对其进行预处理，之后 openrouteservice 才能开始构建用于路径规划的图。

构建过程本身[相当消耗
资源](run-instance/system-requirements.zh.md)。对九个 profile 中的任何一个而言，大约都需要
两天时间。按照前述资源要求，
这意味着所有 profile 全部重建大约需要一周。

图构建完成后，生产实例必须加载它们。由于
这应安排在低流量时段进行，因此也计划为每周一次。

总结一下：如果你在 OSM 中做了任何修改，大约需要一周时间才会被纳入
`planet` 文件。该文件每周读取一次，构建需要一周，而重新加载图也是每周一次。

如果一切都按预期进行，变更应会在
两到三周内反映到 openrouteservice 中。

然而，如果中间任何环节出了问题，通常意味着至少延迟一周，
前提是问题被立即发现并修复。如果变更在一个月内仍未反映，也无需担心。

## 我遇到错误 `Native memory allocation (mmap) failed to map 16384 bytes for committing reserved memory`

请参阅[系统要求](run-instance/system-requirements.zh.md#memory-mapping-in-large-builds-with-a-containerized-openrouteservice-instance)中的内存映射章节。

## 明明有道路，为什么路径规划不生效？

道路确实存在却未被使用，导致路径规划看起来"出错"，原因有很多。
在许多情况下，这更多是数据问题而非路径规划问题。
一种非常常见的情况与 OSM 中的 `barrier=*` 节点有关。

该问题最常见的表现形式有两种：

1. 渡轮虽然存在且本应更快/更短，却未被采用
2. 住宅区域内的道路无法从外部进入。

在两种情况下，通往渡轮码头或住宅区域的道路上可能存在带有 `barrier=*` 标签的节点。
相应的障碍物被设置为禁止未授权通行。
因此，如果除了 `barrier=*` 之外没有更多信息，openrouteservice 将不会跨越它们进行路径规划。

这种行为常被误认为是 _错误的_，因为在许多情况下障碍物默认是可通行的，或者可以购票通过。
虽然对人类查看地图而言这往往显而易见，但 openrouteservice 无法知道这一点。
跨越此类障碍物进行路径规划属于一种 openrouteservice 不会做出的假设。

有[很多](https://wiki.openstreetmap.org/wiki/Key:access#List_of_possible_values)
[选项](https://wiki.openstreetmap.org/wiki/Key:locked)可以
[启用](https://wiki.openstreetmap.org/wiki/Tag:access%3Ddestination)路径规划，
但它们必须在数据中设置，而不是在路径规划引擎中设置。

## 为什么我自己实例中的 matrix 请求响应时间比 live API 高那么多？

取决于请求参数，openrouteservice 会使用
不同的路径规划算法及不同的预处理来计算出
结果。

对于 matrix 计算，openrouteservice 使用非常快速的 RPHAST 算法，它
基于所谓的 _Contraction Hierarchies_（简称 CH）。虽然使用
CH 能大幅加速 matrix 计算，但准备它们的代价相当高。
因此，它们默认不会被计算，必须手动开启。
相关文档可参见[此处](https://giscience.github.io/openrouteservice/run-instance/configuration/ors/engine/profiles#methods-ch)。

如果 CH 未经过准备，matrix 计算将回退到 Dijkstra
算法，它要慢得多，正是响应缓慢的原因。

## 我的配额何时以及如何重置？

查看我们的[套餐](https://openrouteservice.org/plans/)时，你会看到
我们的端点设有每日和每分钟请求上限。

所有剩余配额都显示在你的[开发者仪表盘](https://openrouteservice.org/dev/#/home)中。

每日限额自你首次发起请求起，在 24 小时后重置。
因此，你的 24 小时窗口可能会随日期推移而变化。

举个例子，我们来看 `directions` 端点及其每天 2000 次请求的默认限额。
如果你今天下午 3 点请求了 1000 次 directions，并在明天上午 10 点尝试
请求 1500 次，那么其中最后 500 次将会失败，因为你的
配额要到明天下午 3 点才会重置。下午 3 点之后，你可以再次发起最多 2000 次请求，
但如果你下午 6 点这样做，就需要等到后天下午 6 点才能再请求
任何 directions。

每分钟限额以滑动窗口方式强制执行，意味着任意
连续的 60 秒时段内最多只能包含 40 次 directions 请求。

如果触及每日限额，你会收到 `403 - Forbidden` HTTP 错误。
如果触及每分钟限额，你会收到 `429 - Too many requests` HTTP 错误。
剩余的每日配额也可以通过程序方式查询，比较 `x-ratelimit-remaining` 和 `x-ratelimit-reset` 响应头即可。
