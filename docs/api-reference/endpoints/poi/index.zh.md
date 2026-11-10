# 兴趣点（POI）服务

:::warning 注意
该端点不属于 openrouteservice，而属于我们的公共 API。运行自己的 openrouteservice 实例时不可用。
:::

[openpoiservice](https://github.com/GIScience/openpoiservice) 是来自 HeiGIT 的独立服务，返回某几何体周边区域内的
兴趣点。

它是一个独立的基于 flask 的 python 应用，位于
另一个 [GitHub 仓库](https://github.com/GIScience/openpoiservice)中，在那里你还可以找到更详细的
文档。

所有请求参数与返回类型的完整、简洁概览可在
[API Playground](https://openrouteservice.org/dev/#/api-docs/pois) 中找到。

## POI 类别

存储在 openpoiservice 数据库中的 POI 按类别组和类别进行组织。下面给出了包含 ``category_group_ids``
及下级 ``category_ids`` 相关名称的表格。

### category_group_ids

类别组

| 名称                                                        | 值 |
|:------------------------------------------------------------|:-----:|
| [accommodation](#accommodation-100)                         |  100  |
| [animals](#animals-120)                                     |  120  |
| [arts_and_culture](#arts-and-culture-130)                   |  130  |
| [education](#education-150)                                 |  150  |
| [facilities](#facilities-160)                               |  160  |
| [financial](#financial-190)                                 |  190  |
| [healthcare](#healthcare-200)                               |  200  |
| [historic](#historic-220)                                   |  220  |
| [leisure_and_entertainment](#leisure-and-entertainment-260) |  260  |
| [natural](#natural-330)                                     |  330  |
| [public_places](#public-places-360)                         |  360  |
| [service](#service-390)                                     |  390  |
| [shops](#shops-420)                                         |  420  |
| [sustenance](#sustenance-560)                               |  560  |
| [transport](#transport-580)                                 |  580  |
| [tourism](#tourism-620)                                     |  620  |

### category_ids

按组列出的类别

#### accommodation : 100

| 名称           | 值 |
|:---------------|:-----:|
| alpine_hut     |  101  |
| apartment      |  102  |
| camp_site      |  103  |
| caravan_site   |  104  |
| chalet         |  105  |
| guest_house    |  106  |
| hostel         |  107  |
| hotel          |  108  |
| motel          |  109  |
| wilderness_hut |  110  |

#### animals : 120

| 名称            | 值 |
|:----------------|:-----:|
| animal_boarding |  121  |
| animal_shelter  |  122  |
| veterinary      |  123  |
| pet             |  124  |

#### arts_and_culture : 130

| 名称             | 值 |
|:-----------------|:-----:|
| arts_centre      |  131  |
| gallery          |  132  |
| library          |  133  |
| museum           |  134  |
| place_of_worship |  135  |
| studio           |  136  |

#### education : 150

| 名称            | 值 |
|:----------------|:-----:|
| college         |  151  |
| driving_school  |  152  |
| kindergarten    |  153  |
| language_school |  154  |
| music_school    |  155  |
| school          |  156  |
| university      |  157  |

#### facilities : 160

| 名称              | 值 |   | 名称                  | 值 |
|:------------------|:-----:|---|:----------------------|-------|
| compressed_air    |  161  |   | recycling             | 172   |
| bench             |  162  |   | recycling_station     | 173   |
| emergency_phone   |  163  |   | sanitary_dump_station | 174   |
| clock             |  164  |   | shelter               | 175   |
| defibrillator     |  165  |   | shower                | 176   |
| drinking_water    |  166  |   | table                 | 177   |
| fire_hydrant      |  167  |   | telephone             | 178   |
| hunting_stand     |  168  |   | toilets               | 179   |
| internet_cafe     |  169  |   | waste_basket          | 180   |
| kneipp_water_cure |  170  |   | waste_disposal        | 181   |
| post_box          |  171  |   | water_point           | 182   |

#### financial : 190

| 名称             | 值 |
|:-----------------|:-----:|
| atm              |  191  |
| bank             |  192  |
| bureau_de_change |  193  |

#### healthcare : 200

| 名称                   | 值 |
|:-----------------------|:-----:|
| baby_hatch             |  201  |
| clinic                 |  202  |
| dentist                |  203  |
| doctors                |  204  |
| emergency_access_point |  205  |
| hospital               |  206  |
| nursing_home           |  207  |
| pharmacy               |  208  |
| retirement_home        |  209  |
| social_facility        |  210  |
| blood_donation         |  211  |

#### historic : 220

| 名称                | 值 |   | 名称           | 值 |   | 名称              | 值 |
|:--------------------|:-----:|---|----------------|-------|---|-------------------|-------|
| aircraft            |  221  |   | farm           | 231   |   | optical_telegraph | 241   |
| aqueduct            |  222  |   | fort           | 232   |   | pillory           | 242   |
| archaeological_site |  223  |   | gallows        | 233   |   | ruins             | 243   |
| castle              |  224  |   | highwater_mark | 234   |   | rune_stone        | 244   |
| cannon              |  225  |   | locomotive     | 235   |   | ship              | 245   |
| city_gate           |  226  |   | manor          | 236   |   | tomb              | 246   |
| citywalls           |  227  |   | memorial       | 237   |   | wayside_cross     | 247   |
| battlefield         |  228  |   | milestone      | 238   |   | wayside_shrine    | 248   |
| boundary_stone      |  229  |   | monastery      | 239   |   | wreck             | 249   |
| building            |  230  |   | monument       | 240   |   |                   |       |

#### leisure_and_entertainment : 260

| 名称                | 值 |   | 名称           | 值 |   | 名称          | 值 |   | 名称          | 值 |   | 名称          | 值 |
|:--------------------|:-----:|---|----------------|-------|---|---------------|-------|---|---------------|-------|---|---------------|-------|
| adult_gaming_centre |  261  |   | fitness_centre | 271   |   | picnic_table  | 281   |   | swimming_area | 291   |   | dojo          | 301   |
| amusement_arcade    |  262  |   | garden         | 272   |   | pitch         | 282   |   | swimming_pool | 292   |   | gambling      | 302   |
| beach_resort        |  263  |   | golf_course    | 273   |   | playground    | 283   |   | track         | 293   |   | nightclub     | 303   |
| bandstand           |  264  |   | hackerspace    | 274   |   | raceway       | 284   |   | turkish_bath  | 294   |   | planetarium   | 304   |
| bird_hide           |  265  |   | horse_riding   | 275   |   | public_bath   | 285   |   | water_park    | 295   |   | social_centre | 305   |
| common              |  266  |   | ice_ring       | 276   |   | sauna         | 286   |   | wildlife_hide | 296   |   | spa           | 306   |
| dance               |  267  |   | marina         | 277   |   | slipway       | 287   |   | brothel       | 297   |   | stripclub     | 307   |
| dog_park            |  268  |   | miniature_golf | 278   |   | sports_centre | 288   |   | casino        | 298   |   | aquarium      | 308   |
| firepit             |  269  |   | nature_reserve | 279   |   | stadium       | 289   |   | cinema        | 299   |   | theme_park    | 309   |
| fishing             |  270  |   | park           | 280   |   | summer_camp   | 290   |   | dive_centre   | 300   |   | zoo           | 310   |

#### natural : 330

| 名称          | 值 |
|:--------------|:-----:|
| cave_entrance |  331  |
| beach         |  332  |
| geyser        |  333  |
| hill          |  334  |
| peak          |  335  |
| rock          |  336  |
| saddle        |  337  |
| spring        |  338  |
| volcano       |  339  |
| water         |  340  |

#### public_places : 360

| 名称             | 值 |
|:-----------------|:-----:|
| embassy          |  361  |
| crematorium      |  362  |
| community_centre |  363  |
| courthouse       |  364  |
| coworking_space  |  365  |
| crypt            |  366  |
| fire_station     |  367  |
| grave_yard       |  368  |
| police           |  369  |
| post_office      |  370  |
| prison           |  371  |
| ranger_station   |  372  |
| rescue_station   |  373  |
| townhall         |  374  |

#### service : 390

| 名称         | 值 |
|:-------------|:-----:|
| beauty       |  391  |
| estate_agent |  392  |
| dry_cleaning |  393  |
| glaziery     |  394  |
| hairdresser  |  395  |
| laundry      |  396  |
| massage      |  397  |
| photo_booth  |  398  |
| tailor       |  399  |
| tattoo       |  400  |

#### shops : 420

| 名称              | 值 | 名称             | 值 | 名称                | 值 | 名称                  | 值 | 名称          | 值 | 名称            | 值 |
|:------------------|:-----:|------------------|-------|---------------------|-------|-----------------------|-------|---------------|-------|-----------------|-------|
| agrarian          |  421  | curtain          | 441   | e-cigarette         | 461   | hifi                  | 481   | optician      | 502   | tiles           | 522   |
| alcohol           |  422  | cheese           | 442   | farm                | 462   | houseware             | 482   | organic       | 503   | tobacco         | 523   |
| antiques          |  423  | chemist          | 443   | fashion             | 463   | hunting               | 483   | outdoor       | 504   | toys            | 524   |
| art               |  424  | chocolate        | 444   | fishing             | 464   | jewelry               | 485   | paint         | 505   | trophy          | 525   |
| bag               |  425  | clock            | 445   | florist             | 465   | leather               | 486   | pastry        | 506   | tyres           | 526   |
| bakery            |  426  | clocks           | 446   | funeral_directors   | 466   | locksmith             | 487   | perfumery     | 507   | variety_store   | 527   |
| bed               |  427  | clothes          | 447   | furniture           | 467   | kiosk                 | 488   | photo         | 508   | vending_machine | 528   |
| beverages         |  428  | coffee           | 448   | games               | 468   | kitchen               | 489   | pyrotechnics  | 509   | video           | 529   |
| bicycle           |  429  | computer         | 449   | garden_centre       | 469   | lamps                 | 490   | radiotechnics | 510   | video_games     | 530   |
| books             |  430  | confectionery    | 450   | garden_furniture    | 470   | lottery               | 491   | seafood       | 511   | watches         | 531   |
| boutique          |  431  | convenience      | 451   | gas                 | 471   | mall                  | 492   | second_hand   | 512   | weapons         | 532   |
| brewing_supplies  |  432  | copyshop         | 452   | general             | 472   | marketplace           | 493   | security      | 513   | wine            | 533   |
| business_machines |  433  | cosmetics        | 453   | gift                | 473   | medical_supply        | 494   | shoes         | 514   |                 |       |
| butcher           |  434  | dairy            | 454   | greengrocer         | 474   | mobile_phone          | 495   | spices        | 515   |                 |       |
| cafe              |  435  | deli             | 455   | grocery             | 475   | model                 | 496   | sports        | 516   |                 |       |
| camera            |  436  | department_store | 456   | interior_decoration | 476   | motorcycle            | 497   | stationery    | 517   |                 |       |
| candles           |  437  | doityourself     | 457   | hairdresser_supply  | 477   | music                 | 498   | supermarket   | 518   |                 |       |
| car               |  438  | electrical       | 458   | hardware            | 478   | musical_instrument    | 499   | swimming_pool | 519   |                 |       |
| car_parts         |  439  | electronics      | 459   | hearing_aids        | 479   | nutrition_supplements | 500   | tea           | 520   |                 |       |
| carpet            |  440  | erotic           | 460   | herbalist           | 480   | newsagent             | 501   | ticket        | 521   |                 |       |

#### sustenance : 560

| 名称           | 值 |
|:---------------|:-----:|
| bar            |  561  |
| bbq            |  562  |
| biergarten     |  563  |
| café           |  564  |
| drinking_water |  565  |
| fast_food      |  566  |
| food_court     |  567  |
| ice_cream      |  568  |
| pub            |  569  |
| restaurant     |  570  |

#### transport : 580

| 名称                   | 值 |   | 名称               | 值 |   | 名称             | 值 |
|:-----------------------|:-----:|---|--------------------|-------|---|------------------|-------|
| aerodrome              |  581  |   | car_sharing        | 591   |   | parking          | 601   |
| aeroport               |  582  |   | car_wash           | 592   |   | parking_entrance | 602   |
| bicycle_parking        |  583  |   | charging_station   | 593   |   | parking_space    | 603   |
| bicycle_rental         |  584  |   | ev_charging        | 594   |   | station          | 604   |
| bicycle_repair_station |  585  |   | ferry_terminal     | 595   |   | tram_stop        | 605   |
| boat_sharing           |  586  |   | fuel               | 596   |   | taxi             | 606   |
| bus_station            |  587  |   | halt               | 597   |   | platform         | 607   |
| bus_stop               |  588  |   | helipad            | 598   |   | stop_position    | 608   |
| car_rental             |  589  |   | heliport           | 599   |   | stop_area        | 609   |
| car_repair             |  590  |   | motorcycle_parking | 600   |   | station          | 610   |

请注意，`station` 既作为火车站（604）出现一次，也作为公共交通站（610）出现一次。

#### tourism : 620

| 名称          | 值 |
|:--------------|:-----:|
| artwork       |  621  |
| attraction    |  622  |
| fountain      |  623  |
| information   |  624  |
| picnic_site   |  625  |
| travel_agency |  626  |
| viewpoint     |  627  |
