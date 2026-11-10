# Status 端点

:::warning 注意
该端点不在公共 API 中提供，但你可以在运行自己的 openrouteservice 实例时使用它。
:::

GET 请求 http://localhost:8082/ors/v2/status（主机和端口取决于具体部署）会返回关于正在运行实例的基本信息：

* `languages`：可用的语言
* `engine`：openrouteservice 的构建日期和版本
* `profiles`：可用的路径规划 profile，以及存储和已配置限制的信息
* `services`：已激活的服务

[//]: # (TODO: engine git die ORS version aus, nicht die eigentliche engine version, die wir dann auch im graph management verwenden, oder?)

 
:::details 以下是一个示例响应： 
```json
{
    "languages": [
        "cs",
        "cs-cz",
        "de",
        "de-de",
        "en",
        "en-us",
        "eo",
        "eo-eo",
        "es",
        "es-es",
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
        "zh",
        "zh-cn"
    ],
    "engine": {
        "build_date": "2023-12-15T14:31:27Z",
        "version": "8.0"
    },
    "profiles": {
        "profile 1": {
            "storages": {
                "WayCategory": {
                    "gh_profile": "car_ors_fastest_with_turn_costs"
                },
                "HeavyVehicle": {
                    "gh_profile": "car_ors_fastest_with_turn_costs"
                },
                "WaySurfaceType": {
                    "gh_profile": "car_ors_fastest_with_turn_costs"
                },
                "RoadAccessRestrictions": {
                    "gh_profile": "car_ors_fastest_with_turn_costs",
                    "use_for_warnings": "true"
                }
            },
            "profiles": "driving-car",
            "creation_date": "",
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
:::
