
# ors.messages

可按简单规则随 API 响应一起发送的系统消息。

`ors.messages` 块期望一个元素列表，每个元素包含以下内容：

| key       | type    | description                                                      | example value      |
|-----------|---------|------------------------------------------------------------------|--------------------|
| active    | boolean | 启用或禁用此消息                                 | `true`             |
| text      | string  | 消息文本                                                 | `The message text` |
| condition | list    | 可选；可以包含下表任一条件 |                    |

## condition

`ors.messages.condition` 下的属性：

| key                | value                                                                                                                | description                                                         |
|--------------------|----------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| time_before        | ISO 8601 日期时间字符串                                                                                             | 如果服务器本地时间早于给定时间点，则发送消息     |
| time_after         | ISO 8601 日期时间字符串                                                                                             | 如果服务器本地时间晚于给定时间点，则发送消息      |
| api_version        | 1 或 2                                                                                                               | 如果通过请求的 API 版本与值匹配，则发送消息         |
| api_format         | 包含输出格式（"json"、"geojson"、"gpx"）的字符串，以逗号分隔                                               | 如果请求的输出格式与值匹配，则发送消息               |
| request_service    | 包含服务名称（"routing"、"matrix"、"isochrones"、"snap"）的字符串，以逗号分隔                               | 如果请求的服务与给定名称之一匹配，则发送消息    |
| request_profile    | 包含配置名称的字符串，以逗号分隔                                                                           | 如果请求的配置与给定名称之一匹配，则发送消息    |
| request_preference | 包含偏好设置（路由的 weightings、矩阵的 metrics、等时圈的 rangetype）名称的字符串，以逗号分隔 | 如果请求的偏好设置与给定名称之一匹配，则发送消息 |

如果给出多个条件，则必须全部满足才会触发发送相应消息。

示例：

```
messages:
  - active: true
    text: This message would be sent with every routing bike fastest request
    condition:
      - request_service: routing
      - request_profile: cycling-regular,cycling-mountain,cycling-road,cycling-electric
      - request_preference: fastest
  - active: true
    text: This message would be sent with every request for geojson response
    condition:
      - api_format: geojson
  - active: true
    text: This message would be sent with every request on API v1 from January 2020 until June 2050
    condition:
      - api_version: 1
      - time_after: 2020-01-01T00:00:00Z
      - time_before: 2050-06-01T00:00:00Z
  - active: true
    text: This message would be sent with every request
```
