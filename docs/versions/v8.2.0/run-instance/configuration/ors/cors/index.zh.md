# `ors.cors`

与跨源资源共享相关的属性：

| key               | type                          | description                                                                        | default value                                                                                                                  |
|-------------------|-------------------------------|------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| allowed_origins   | string / comma separated list | 配置 Access-Control-Allow-Origins CORS 头。`*` 表示所有源                       | `*`                                                                                                                            |
| allowed_headers   | string / comma separated list | 配置 Access-Control-Allow-Headers CORS 头。`*` 表示所有头部                       | `Content-Type, X-Requested-With, accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization` |
| preflight_max_age | int                           | 持续时间（秒）。指定浏览器缓存 OPTIONS 响应的时长                                  | `600`                                                                                                                          |
