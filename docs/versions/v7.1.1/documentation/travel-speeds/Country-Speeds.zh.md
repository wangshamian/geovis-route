# 各国速度
由于不同国家有不同的交通法规，如果 openstreetmap 中没有给出[最高
速度](http://wiki.openstreetmap.org/wiki/Key:maxspeed)标签，
我们会根据取自[各国限速](http://wiki.openstreetmap.org/wiki/Speed_limits)的以下键值来调整最高速度。
该表汇总了[速度值文件][svf]中的取值。

_（所有值单位均为 km/h）_

  |     国家     |         标签         | driving-car | driving-hgv |
  |:--------------:|:--------------------:|:-----------:|:-----------:|
  |   奥地利      |       AT:urban       |      50     |      50     |
  |                |       AT:rural       |     100     |      80     |
  |                |       AT:trunk       |     100     |      80     |
  |                |      AT:motorway     |     130     |      80     |
  | 瑞士    |       CH:urban       |      50     |      50     |
  |                |       CH:rural       |      80     |      80     |
  |                |       CH:trunk       |     100     |      80     |
  |                |      CH:motorway     |     120     |      80     |
  | 捷克共和国 |       CZ:urban       |      50     |      50     |
  |                |       CZ:rural       |      90     |      90     |
  |                |       CZ:trunk       |      80     |      80     |
  |                |      CZ:motorway     |      80     |      80     |
  |   丹麦      |       DK:urban       |      50     |      50     |
  |                |       DK:rural       |      80     |      80     |
  |                |      DK:motorway     |     130     |      80     |
  |   德国      |   DE:living_street   |       7     |       7     |
  |                |       DE:urban       |      50     |      50     |
  |                |       DE:rural       |     100     |      80     |
  |                |      DE:motorway     |     130     |      80     |
  |   芬兰      |       FI:urban       |      50     |      50     |
  |                |       FI:rural       |      80     |      80     |
  |                |       FI:trunk       |     100     |      80     |
  |                |      FI:motorway     |     120     |      80     |
  |   法国       |       FR:urban       |      50     |      50     |
  |                |       FR:rural       |      80     |      80     |
  |                |       FR:trunk       |     110     |      80     |
  |                |      FR:motorway     |     130     |      80     |
  |   希腊       |       GR:urban       |      50     |      50     |
  |                |       GR:rural       |      90     |      80     |
  |                |       GR:trunk       |     110     |      80     |
  |                |      GR:motorway     |     130     |      80     |
  |   匈牙利      |       HU:urban       |      50     |      50     |
  |                |       HU:rural       |      90     |      80     |
  |                |       HU:trunk       |     110     |      80     |
  |                |      HU:motorway     |     130     |      80     |
  |    意大利       |       IT:urban       |      50     |      50     |
  |                |       IT:rural       |      90     |      80     |
  |                |       IT:trunk       |     110     |      80     |
  |                |      IT:motorway     |     130     |      80     |
  |    日本       |      JP:national     |      60     |      60     |
  |                |      JP:motorway     |     100     |      80     |
  |   波兰       |   PL:living_street   |      20     |      20     |
  |                |       PL:urban       |      50     |      50     |
  |                |       PL:rural       |      90     |      80     |
  |                |      PL:motorway     |     140     |      80     |
  |   罗马尼亚       |       RO:urban       |      50     |      50     |
  |                |       RO:rural       |      90     |      80     |
  |                |       RO:trunk       |     100     |      80     |
  |                |      RO:motorway     |     130     |      80     |
  | 俄罗斯         |   RU:living_street   |      20     |      20     |
  |                |       RU:rural       |      90     |      80     |
  |                |       RU:urban       |      60     |      60     |
  |                |      RU:motorway     |     110     |      80     |
  |  斯洛伐克      |       SK:urban       |      50     |      50     |
  |                |       SK:rural       |      90     |      80     |
  |                |       SK:trunk       |      90     |      80     |
  |                |      SK:motorway     |      90     |      80     |
  |  斯洛文尼亚      |       SI:urban       |      50     |      50     |
  |                |       SI:rural       |      90     |      80     |
  |                |       SI:trunk       |     110     |      80     |
  |                |      SI:motorway     |     130     |      80     |
  |    西班牙       |       ES:urban       |      50     |      50     |
  |                |       ES:rural       |      90     |      80     |
  |                |       ES:trunk       |     100     |      80     |
  |                |      ES:motorway     |     120     |      80     |
  |   瑞典       |       SE:urban       |      50     |      50     |
  |                |       SE:rural       |      70     |      70     |
  |                |       SE:trunk       |      90     |      80     |
  |                |      SE:motorway     |     110     |      80     |
  | 英国 |     GB:nsl_single    |      95     |      90     |
  |                |      GB:nsl_dual     |     112     |      90     |
  |                |      GB:motorway     |     112     |      90     |
  | 乌克兰        |       UA:urban       |      60     |      60     |
  |                |       UA:rural       |      90     |      80     |
  |                |       UA:trunk       |     110     |      80     |
  |                |      UA:motorway     |     130     |      80     |
  | 乌兹别克斯坦     |   UZ:living_street   |      30     |      30     |
  |                |       UZ:urban       |      70     |      70     |
  |                |       UZ:rural       |     100     |      90     |
  |                |      UZ:motorway     |     110     |      90     |

[svf]: https://github.com/GIScience/openrouteservice/tree/master/ors-engine/src/main/resources/resources/services/routing/speed_limits
