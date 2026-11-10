# `ors.cors`

与跨源资源共享相关的属性：

| key               | type                          | description                                                                        | default value                                                                                                                  |
|-------------------|-------------------------------|------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| allowed_origins   | string / 逗号分隔列表 | 配置 Access-Control-Allow-Origins CORS 头。`*` 表示所有来源       | `*`                                                                                                                            |
| allowed_headers   | string / 逗号分隔列表 | 配置 Access-Control-Allow-Headers CORS 头。`*` 表示所有头     | `Content-Type, X-Requested-With, accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization` |
| preflight_max_age | int                           | 持续时间，单位为秒。指定 OPTIONS 响应被浏览器缓存的时长 | `600`                                                                                                                          |
