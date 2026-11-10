# 翻译
如果你发现翻译有任何问题，或者想为 openrouteservice 的路线指示添加新语言，请在 [`ors-engine/src/main/resources/resources/locales`](https://github.com/GIScience/openrouteservice/tree/main/ors-engine/src/main/resources/resources/locales) 文件夹中添加/编辑文件。你可以将 [`ors_en.resources`](https://github.com/GIScience/openrouteservice/blob/main/ors-engine/src/main/resources/resources/locales/ors_en-US.resources) 文件用作模板，了解需要包含哪些信息。
作为参考，每条指示都有两种格式——一种包含具名地点（即 47 Berliner Straße），另一种不包含。重要的是在文本中保留 `{way_name}` 标签，但应将其移动到正确的位置。
为了告诉 openrouteservice 正在使用哪种语言，你应修改文件名以包含 ISO 标识符（例如奥地利德语的文件名应为 `ors_de_AT.resources`）。

自更新到 v2 API 以来，我们使用了 Spring 框架。因此，你还需要对一个声明哪些参数可以传入 API 的 java 文件做小幅修改。该文件位于 [`ors-api/src/main/java/org/heigit/ors/api/APIEnums.java`](https://github.com/GIScience/openrouteservice/blob/main/ors-api/src/main/java/org/heigit/ors/api/APIEnums.java)，需要更新的部分是 `public enum Languages` 枚举。如果（例如）你想把波兰语添加到语言列表中，还需要在此处向列表添加类似 `PL("pl")` 的条目，其中 `("...")` 部分就是调用 API 选择该语言时要传入的内容。因此最终你会得到：
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
