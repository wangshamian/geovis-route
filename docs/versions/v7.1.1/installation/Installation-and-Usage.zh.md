# 安装与使用

## 通过 Docker 安装

我们建议使用 docker 来安装并启动 openrouteservice 后端。简而言之，运行以下命令即可完成全部工作。

```bash
wget https://raw.githubusercontent.com/GIScience/openrouteservice/master/docker-compose.yml
docker compose up
```

这会从 dockerhub 拉取 openrouteservice 的最新发布版本，并使用示例配置和提供的测试 OSM 文件将其启动。
你也可以修改配置和源文件设置，以符合自己的需求。更多细节请查看[使用 Docker 运行](Running-with-Docker)一节。
关于自定义的更多说明可参阅[高级 Docker 设置](Advanced-Docker-Setup)

## 其他资源

* [从源码构建](Building-from-Source)
* [系统要求](System-Requirements)
* [配置](Configuration)

## 使用
Openrouteservice 为不同的空间用途提供了一组端点。它们由 [Java servlet 容器中的 Tomcat](https://github.com/GIScience/openrouteservice/blob/master/ors-api/WebContent/WEB-INF/web.xml) 提供。默认情况下，你可以通过以下地址查询这些服务：

- `http://localhost:8080/ors/v2/directions`
- `http://localhost:8080/ors/v2/isochrones`
- `http://localhost:8080/ors/v2/matrix`

请注意，通过 maven 运行的 Tomcat 默认使用 `8082` 端口。
