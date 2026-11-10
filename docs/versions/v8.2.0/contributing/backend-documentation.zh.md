# 搭建后端文档

后端文档使用 [vitepress](https://vitepress.dev/guide/getting-started) 构建。

要求：
- [node 版本 20](https://nodejs.org/en/download)

## 本地搭建

要在本地运行带热重载的开发版本，请运行

```bash
npm install
npm run docs:dev
```

要将静态文件构建到 `docs/.vitepress/dist` 并在本地预览生产版本，请使用

```bash
npm run build-preview
```

::: info
为了让 Markdown 链接在你的 IDE（例如 Intellij）中正确解析，请右键点击 `docs` 文件夹并将
其标记为 'Sources Root'。
:::

## 配置与主题

站点配置（例如侧边栏、导航……）位于 `docs/.vitepress/config.js`。
对其所做的更改请遵循 [vitepress 参考文档](https://vitepress.dev/reference/site-config)。

自定义主题位于 `docs/.vitepress/theme`。
详情请参阅 [vitepress 指南](https://vitepress.dev/guide/extending-default-theme)。

## 已部署版本

文档通过 `.github/workflows/deploy-docs.yml` 中的 GitHub Action 部署，
使用 [GitHub Pages](https://pages.github.com/)，
访问地址为 [giscience.github.io/openrouteservice](https://giscience.github.io/openrouteservice/)。
