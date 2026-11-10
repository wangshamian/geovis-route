# 贡献指南

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [贡献指南](#contribution-guidelines)
	- [Issue](#issues)
	- [Pull Request](#pull-requests)
		- [Pull request 指南](#pull-request-guidelines)
		- [贡献代码](#contributing-code)

<!-- /TOC -->

**首先，感谢你使用 openrouteservice，并为让它对每个人都更好而做出贡献。**

Openrouteservice 已存在十多年，因此它经历了许多变化，采纳了新的方法论和技术，也见证了多位开发者的来来去去。
为了尽量保持 openrouteservice 代码库的整洁与稳定，我们有一些尽量遵循的指南。

## Issue

在创建新 issue 之前，请先搜索是否已有相同 issue！
选择[最合适的 issue 模板](https://github.com/GIScience/openrouteservice/issues/new)并提供所需信息。
对于常见问题，请改用[我们的社区论坛](https://ask.openrouteservice.org)。

## Pull Request

我们 :heart: pull request！我们也希望让提交历史更整洁、更可持续，惠及贡献者和我们这些维护者。

### Pull request 指南

我们希望所有 pull request 都遵守以下规则：

- PR 必须**关闭一个 issue**。如果还没有与你相关的 issue，请[创建一个](https://github.com/GIScience/openrouteservice/issues/new)
- 从 [main](https://github.com/GIScience/openrouteservice/tree/main) 分支创建分支
- 按照 `<[hotfix/bugfix/feat/algo]>/<issue#>-<purpose>` 命名你的分支，例如 `feat/#381-simplify_geometry`
- 如果你引入新的函数/类，请编写单元测试或 API 测试
- **将提交数量控制在最少**，即使用 [`git commit --amend [--no-edit]`](https://www.atlassian.com/git/tutorials/rewriting-history#git-commit--amend)
- 使用有意义的提交信息，例如 `commit -m "Include geometry_simplify in API and core code"`
- 如果你的分支需要从基分支更新，请使用 [`rebase`](https://blog.algolia.com/master-git-rebase/#rebasing-on-the-base-branch)，例如

```
git checkout feat/#123-add-great-feature
git stash
git rebase main
git stash pop
```

与其他人在同一分支上协作时，使用 `rebase` 命令要格外小心。

**不要**将 `main` 分支合并到你的功能分支。

### 贡献代码

关于如何在本地为开发搭建、部署和测试 **openrouteservice** 的简要说明，请参阅我们的[安装说明](https://giscience.github.io/openrouteservice/run-instance/building-from-source)。

虽然对贡献的代码没有具体规则，但我们有一些应被采纳的通用风格：

- 为变量、方法和类使用**描述性名称**
- **尽量少用行内注释**：代码应当自解释。**绝不**用注释来注释掉代码块
- 使用 **JDoc** docstring 为方法和类提供上下文
- **单元测试！！**
- 在相应的 `apitests` 包中添加 API 功能时编写 **API 测试**
- 保持**方法模块化**：宁可方法较短且职责单一，也不要数百行代码
