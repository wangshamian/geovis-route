# Status 端点

:::warning 注意
该端点不在公共 API 中提供，但你可以在运行自己的 openrouteservice 实例时使用。
:::

GET 请求 http://localhost:8082/ors/v2/status（主机和端口取决于具体配置）返回正在运行的实例的基本信息：

* `languages`：可用语言
* `engine`：
    * `version`：正在运行的 openrouteservice 的发布版本
    * `build_date`：正在运行的 openrouteservice 的构建日期
    * `graph_version`：图结构及构建逻辑的版本。该参数是软件的一部分，无法配置。具有相同 `graph_version` 的图相互兼容，例如版本 Y 的 openrouteservice 可以加载由更早的版本 X 构建的图，前提是两个版本的 `graph_version` 相同。如果启用了[图管理](/run-instance/configuration/engine/graph-management.md)，该参数还用于在图仓库中查找图。
* `profiles`：可用（已启用）的 routing profile。
  profile 名称对应于 `ors.engine.profiles` 之下的配置键。
  profile 名称在 API 请求中用作路径参数，并用作图目录的目录名。
  每个 profile 都会显示一些基本信息：
    * `encoder_name`：车辆类型
    * `encoded_values`：可在[自定义模型](/api-reference/endpoints/directions/custom-models)中使用的可用 encoded values 列表
    * `osm_date`：用于构建图的 osm pbf 文件的时间戳。这通常是最新纳入变更的日期。
    * `graph_build_date`：该 routing profile 开始构建图的日期。
    * `storages`：已配置的存储
    * `limits`：已配置的限制
* `services`：已激活的服务。端点 `status` 和 `health` 不包含在此。

这是一个示例响应：
```json
{
    "languages": [
        "cs",
        "cs-cz",
        "da",
        "da-dk",
        "de",
        "de-de",
        "en",
        "en-us",
        "eo",
        "eo-eo",
        "es",
        "es-es",
        "fi",
        "fi-fi",
        "fr",
        "fr-fr",
        "gr",
        "gr-gr",
        "he",
        "he-il",
        "hu",
        "hu-hu",
        "id",
        "id-id",
        "it",
        "it-it",
        "ja",
        "ja-jp",
        "nb",
        "nb-no",
        "ne",
        "ne-np",
        "nl",
        "nl-nl",
        "pl",
        "pl-pl",
        "pt",
        "pt-pt",
        "ro",
        "ro-ro",
        "ru",
        "ru-ru",
        "tr",
        "tr-tr",
        "ua",
        "ua-ua",
        "vi",
        "vi-vn",
        "zh",
        "zh-cn"
    ],
    "engine": {
        "build_date": "2024-12-11T12:44:36Z",
        "graph_version": "1",
        "version": "9.1.0"
    },
    "profiles": {
        "car": {
            "storages": {
                "WayCategory": {
                    "enabled": true
                },
                "HeavyVehicle": {
                    "restrictions": true,
                    "enabled": true
                },
                "WaySurfaceType": {
                    "enabled": true
                },
                "RoadAccessRestrictions": {
                    "useForWarnings": true,
                    "enabled": true
                }
            },
            "encoder_name": "driving-car",
            "encoded_values": [
                "road_environment",
                "car_ors_fastest_with_turn_costs_subnetwork",
                "car_ors_fastest_subnetwork",
                "car_ors_shortest_with_turn_costs_subnetwork",
                "car_ors_shortest_subnetwork",
                "car_ors_recommended_with_turn_costs_subnetwork",
                "car_ors_recommended_subnetwork",
                "roundabout",
                "road_class",
                "road_class_link",
                "max_speed",
                "road_access",
                "car_ors$access",
                "car_ors$average_speed",
                "car_ors$turn_cost"
            ],
            "graph_build_date": "2024-10-28T14:42:49Z",
            "osm_date": "2023-10-11T20:21:48Z",
            "limits": {
                "maximum_distance": 100000,
                "maximum_waypoints": 50,
                "maximum_distance_dynamic_weights": 100000,
                "maximum_distance_avoid_areas": 100000
            }
        },
        "pedestrian": {
            "encoder_name": "foot-walking",
            "encoded_values": [
                "road_environment",
                "pedestrian_ors_fastest_subnetwork",
                "pedestrian_ors_shortest_subnetwork",
                "pedestrian_ors_recommended_subnetwork",
                "roundabout",
                "road_class",
                "road_class_link",
                "max_speed",
                "road_access",
                "foot_network",
                "pedestrian_ors$access",
                "pedestrian_ors$average_speed",
                "pedestrian_ors$priority"
            ],
            "graph_build_date": "2024-10-11T11:08:44Z",
            "osm_date": "2024-01-22T21:21:14Z",
            "limits": {
                "maximum_distance": 100000,
                "maximum_waypoints": 50,
                "maximum_distance_dynamic_weights": 100000,
                "maximum_distance_avoid_areas": 100000
            }
        }
    },
    "services": [
        "routing",
        "isochrones",
        "matrix",
        "snap"
    ]
}
```
