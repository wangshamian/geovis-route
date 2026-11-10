# 翻译
如果你发现翻译中有任何问题，或者想为 openrouteservice 的指示添加一门新语言，请在 [`src/main/resources/resources/locales`](https://github.com/GIScience/openrouteservice/tree/master/ors-engine/src/main/resources/resources/locales) 文件夹中添加/编辑文件。你可以把 [`ors_en.resources`](https://github.com/GIScience/openrouteservice/openrouteservice/src/main/resources/resources/locales/ors_en-US.resources) 文件作为模板，了解需要包含哪些信息。
作为参考，每条指示都有两种格式——一种带有具名地点（例如 47 Berlinerstraße），另一种不带。文本中保留 `{way_name}` 标签很重要，但应把它移到正确的位置。
为了让 openrouteservice 知道使用的是哪种语言，你应该修改文件名以包含 ISO 标识符（例如奥地利德语的文件名应为 `ors_de_AT.resources`）。

自更新到 v2 API 以来，我们使用了 Spring 框架。因此，你还需要对一个声明哪些参数可以传入 API 的 Java 文件做少量修改。该文件位于 [`openrouteservice/src/main/java/heigit/ors/api/requests/common/APIEnums.java`](https://github.com/GIScience/openrouteservice/blob/master/ors-engine/src/main/java/org/heigit/ors/api/requests/common/APIEnums.java)，需要更新的部分是 `public enum Languages` 枚举。如果（举例来说）你想把波兰语添加到语言中，你还需要在此处向列表中添加类似 `PL("pl")` 的内容，其中 `("...")` 部分就是调用 API 选择该语言时要传入的值。所以最终你会得到：
```java
public enum Languages {
    public enum Languages {
        CS("cs"),
        CS_CZ("cs-cz"),
        DE("de"),
        DE_DE("de-de"),
        EN("en"),
        EN_US("en-us"),
        EO("eo"),
        EO_EO("eo-eo"),
        ES("es"),
        ES_ES("es-es"),
        FR("fr"),
        FR_FR("fr-fr"),
        GR("gr"),
        GR_GR("gr-gr"),
        HE("he"),
        HE_IL("he-il"),
        HU("hu"),
        HU_HU("hu-hu"),
        ID("id"),
        ID_ID("id-id"),
        IT("it"),
        IT_IT("it-it"),
        JA("ja"),
        JA_JP("ja-jp"),
        NE("ne"),
        NE_NP("ne-np"),
        NL("nl"),
        NL_NL("nl-nl"),
        PL("pl"),
        PL_PL("pl-pl"),
        PT("pt"),
        PT_PT("pt-pt"),
        RO("ro"),
        RO_RO("ro-ro"),
        RU("ru"),
        RU_RU("ru-ru"),
        TR("tr"),
        TR_TR("tr-tr"),
        ZH("zh"),
        ZH_CN("zh-cn");
```
