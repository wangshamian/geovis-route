1. 检查以下事项
    - 所有相关 PR 均已合并
    - 文档为最新状态
    - CHANGELOG 为最新状态
    - 如果是大版本发布：用于公告/博客的发布说明已准备就绪
2. 选择新的发布版本号：我们使用由 `<major>.<minor>.<patch>` 各部分组成的版本号，下文简写为 `X.Y.Z`。如果发布只包含缺陷修复，则只变更 patch 版本。包含新功能或非强制性的额外 API 请求参数等新增内容的发布为 minor 版本。如果存在破坏性变更，例如需要修改现有配置文件或 API 请求，则递增 major 版本。
3. 创建一个名为 `releases/vX.Y.Z` 的 Release 分支，并
   a. 将 [package.json](package.json) 中的 `version` 属性更新为新的发布版本号
   b. 按如下方式更新 CHANGELOG.md：
    1. 将 unreleased 改为新的发布版本号
    2. 添加今天的日期
    3. 将 unreleased 链接改为比较新版本：
       [unreleased]: https://github.com/GIScience/openrouteservice/compare/vnew...HEAD
    4. 在下方添加新的比较链接
       [new]: https://github.com/GIScience/openrouteservice/compare/vlast...vnew
    5. 再次检查 issue 链接是否有效
    6. 添加带有如上所有子章节的 [unreleased] 章节
       b. 使用以下命令更新 POM 中的版本号
       `./mvnw versions:set -DnewVersion=X.Y.Z`
       或在主 POM 及所有子 POM 中手动设置
       c. 以 chore 或 build 类型提交更改，并推送
       d. 以
       `chore: release vX.Y.Z`
       为标题创建并合并 PR
3. 在分支 `release/vX.Y.Z` 合并到 main 之后，在 Github 上起草新的 release。
   自动生成发布说明并人工整理。
   这也会创建新的 `vX.Y.Z` tag。
4. 在工作流完成后，检查以下资产是否存在：
    - docker-compose.yml（使用新版本）
    - gr-config.env
    - gr-config.yml
    - ors.jar
    - ors.war
    - Source code (zip)
    - Source code (tar.gz)
5. 检查 docker 镜像是否已正确创建：
    - `vX.Y.Z` 现在应已存在
    - `latest` 应指向新镜像
    - `vX` 应指向新镜像
    - 目前已为 openrouteservice/openrouteservice 配置好。若还要把 docker 镜像复制到 heigit/openrouteservice，请执行以下操作：
    ```shell
    docker pull openrouteservice/openrouteservice:vX.Y.Z
    docker tag openrouteservice/openrouteservice:vX.Y.Z heigit/openrouteservice:vX.Y.Z
    docker push heigit/openrouteservice:vX.Y.Z
    docker tag heigit/openrouteservice:vX.Y.Z heigit/openrouteservice:latest
    docker push heigit/openrouteservice:latest
    ```
6. 更改 latest 和 vX tag：
   a. 在 github 上删除 tag
   b. 在本地删除 tag：
    - git tag -d latest
    - git tag -d vX
      c. 在本地新的 main HEAD 上重新创建 tag
    - git tag latest
    - git tag vX
      d. 推送新 tag
    - git push origin tag latest
    - git push origin tag vX
7. 使用以下命令将 POM 中的版本更新为 X.Y.Z-SNAPSHOT
   ./mvnw versions:set -DnewVersion=X.Y.Z-SNAPSHOT
8. 检查是否需要进行对外宣传、公告等，并据此执行。
