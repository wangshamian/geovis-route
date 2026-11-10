# Health 端点

:::warning 注意
该端点不在公共 API 中提供，但你可以在运行自己的 openrouteservice 实例时使用它。
:::

GET 请求 http://localhost:8082/ors/v2/health（主机和端口可能不同）会返回正在运行实例的当前健康或就绪状态，
共有两个可能的取值：

* `not ready`：openrouteservice 实例仍在构建图
* `ready`：所有必需的图均已完成计算，openrouteservice 实例已准备好处理空间请求
