# Openrouteservice API 基准测试

Openrouteservice API 基准测试套件提供了用于测量和优化 openrouteservice 实例性能的工具。这套工具帮助你了解自己的部署如何应对负载、找出性能瓶颈，并协助进行容量规划。

## 什么是 Openrouteservice API 基准测试？

API 基准测试是指在各种条件下系统性地测试 API 性能的过程，目的是：

- 测量不同负载下的响应时间
- 确定最大吞吐能力
- 找出性能瓶颈与临界点
- 比较不同的配置设置
- 验证性能要求

## 何时使用这些工具

在以下情况下，你应考虑使用这些基准测试工具：

- 搭建新的 openrouteservice 生产环境
- 更改硬件资源或配置
- 测试不同的路径规划配置或算法
- 实现自定义模型或修改
- 针对预期的流量增长做规划
- 排查性能问题

## 可用工具

openrouteservice 基准测试套件由两个主要部分组成：

### 1. [坐标生成工具](./coordinate-generators.zh.md)

这些工具帮助你生成逼真的测试数据，作为基准测试的输入：

- **Route Generator** - 为路径规划请求创建起点-终点坐标对
- **Snapping Generator** - 创建吸附到路网上的坐标

### 2. [API 基准测试工具](./api-benchmarks.zh.md)

这些工具模拟特定场景，用于负载与性能测试：

- **Isochrones Range Load Test** - 在启用 range 选项的各种负载条件下测试 Isochrones API。
- **Routing Algorithm Load Test** - 使用不同路径规划算法测试 Directions API 并比较性能。

## 快速上手

要开始对 openrouteservice 实例进行基准测试：

1. 首先，准备好你的 openrouteservice 实例，或确保能够访问远程 API。
2. 使用[坐标生成工具](./coordinate-generators.zh.md)为基准测试场景创建测试坐标。
3. 然后，使用这些坐标通过 [API 基准测试工具](./api-benchmarks.zh.md)运行基准测试
4. 分析结果，并按需调整配置

这两个工具都提供了关于使用选项和示例的详细文档，帮助你为具体用例创建有意义的性能测试。
