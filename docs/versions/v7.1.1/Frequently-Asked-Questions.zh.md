---
title: FAQ
---

# 常见问题解答

## 为什么我的 ors 会报告 `Could not find point`？

这是一条经常遇到的错误信息：
```
Could not find point 0: 25.3531986 51.5214311 within a radius of 350.0 meters.;
Could not find point 1: 25.3524229 51.4627229 within a radius of 350.0 meters.
```

造成该问题的主要原因有三个，下面按从最常见到最不常见的顺序列出。

1. 如果两个点都找不到，很可能只是把纬度和经度写反了。我们的
   API 要求坐标按 [lon,lat] 的顺序提供，具体说明见文档
   （点击帮助按钮可查看参数信息）。输出同样是 [lon,lat] 顺序，这符合
   GeoJSON 规范。

2. 给定的起点和终点距离任何可路由的道路都超过 350m。
   我们的 API 中吸附到路段的最大距离为 350m。
 对于本地部署，可以通过配置参数
  `maximum_snapping_radius` 和 `location_index_resolution` 自定义该值。详情请参阅
  [配置](installation/Configuration)。

3. 起点和终点的 lon,lat 顺序正确，且位于某条可路由道路 350m 范围内。
   这种情况通常只会出现在本地部署中。
   一般意味着 ors 正尝试在一个尚未为其构建图（graph）的区域中做路线规划。
   如果能够找到海德堡（德国）的路线，说明 ors 仍在默认数据集上运行。

---

## openrouteservice 中的 OSM 数据何时更新？

Openrouteservice 基于 `planet.osm.pbf` 文件构建数据。根据
[osm-wiki](https://wiki.openstreetmap.org/wiki/Planet.osm) 的说明，这些文件需要两天才能生成，
并且每周更新一次。

由于 `planet` 文件相当大（目前超过 60GB），
要确认下载是否正确、文件是否以任何方式损坏、以及它是否确实是新文件，
需要做不少工作。这一过程的一部分由 OSM 负责，另一部分由 openrouteservice 完成。

一旦最新的 `planet` 文件放到 openrouteservice 服务器上，
还需要先对它进行预处理，openrouteservice 才能开始构建用于路线规划的图。

构建过程本身[相当消耗资源](installation/System-Requirements)。
对九个 profile 中的任何一个而言，大约需要两天。结合前面提到的资源需求，
这意味着重新构建全部 profile 大约需要一周时间。

图构建完成后，生产实例还需要加载它们。由于这应当安排在低流量时段进行，
所以也计划为每周执行一次。

总结一下：如果你对 OSM 做了任何改动，大约要等一周才会被纳入 `planet` 文件。
该文件每周被读取一次，构建需要一周，重新加载图也是每周一次。

如果一切都按预期衔接，改动大约会在两到三周内反映到 openrouteservice 中。

不过，如果中间任何环节出了问题，通常意味着至少延迟一周——前提是问题被立即发现并修复。
如果改动在一个月内仍未生效，也不必担心。
