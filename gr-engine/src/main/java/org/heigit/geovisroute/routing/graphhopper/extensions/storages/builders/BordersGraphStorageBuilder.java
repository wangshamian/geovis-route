/*  This file is part of Openrouteservice.
 *
 *  Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.

 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.

 *  You should have received a copy of the GNU Lesser General Public License along with this library;
 *  if not, see <https://www.gnu.org/licenses/>.
 */
package org.heigit.geovisroute.routing.graphhopper.extensions.storages.builders;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.storage.GraphExtension;
import com.graphhopper.util.EdgeIteratorState;
import org.apache.log4j.Logger;
import org.heigit.geovisroute.routing.graphhopper.extensions.reader.borders.CountryBordersPolygon;
import org.heigit.geovisroute.routing.graphhopper.extensions.reader.borders.CountryBordersReader;
import org.heigit.geovisroute.routing.graphhopper.extensions.storages.BordersGraphStorage;
import org.heigit.geovisroute.util.ErrorLoggingUtility;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

import static org.heigit.geovisroute.routing.graphhopper.extensions.storages.BordersGraphStorage.*;

/**
 * 用于构建边界图扩展的类，该扩展支持对跨越国界的路线进行限制
 *
 * @author Adam Rousell
 */
public class BordersGraphStorageBuilder extends AbstractGraphStorageBuilder {
    static final Logger LOGGER = Logger.getLogger(BordersGraphStorageBuilder.class.getName());

    private static final String PARAM_KEY_IDS = "ids";
    private static final String PARAM_KEY_BOUNDARIES = "boundaries";
    private static final String PARAM_KEY_OPEN_BORDERS = "openborders";
    private static final String TAG_KEY_COUNTRY = "country";
    private static final String TAG_KEY_COUNTRY1 = "country1";
    private static final String TAG_KEY_COUNTRY2 = "country2";
    private static final int EMPTY_NODE = -1;
    private static final int TOWER_NODE = -2;
    private HashMap<Integer, String> wayNodeTags;

    private BordersGraphStorage storage;
    private CountryBordersReader cbReader;
    private boolean preprocessed;
    private final GeometryFactory gf;

    public static final String BUILDER_NAME = "Borders";

    public BordersGraphStorageBuilder() {
        gf = new GeometryFactory();
    }

    /**
     * 初始化边界图扩展 <br/><br/>
     * 该过程所需文件从 gr-config.json 获取，并传给 CountryBordersReader 对象，
     * 由该对象存储过程所需的信息（即国家几何图形与边界类型）
     *
     * @param graphhopper
     * @return
     * @throws Exception
     */
    @Override
    public GraphExtension init(GraphHopper graphhopper) throws Exception {
        if (storage != null)
            throw new Exception("GraphStorageBuilder has been already initialized.");

        File expectedStorageFileLocation1 = Path.of(graphhopper.getGraphHopperLocation() + "/ext_borders").toFile();
        File expectedStorageFileLocation2 = Path.of(graphhopper.getGraphHopperLocation() + "/ext_borders_cbr").toFile();

        if (this.cbReader == null && (!expectedStorageFileLocation1.exists() || !expectedStorageFileLocation2.exists())) {
            cbReader = createCountryBordersReader();
            cbReader.serialize(expectedStorageFileLocation2);
        }
        if (cbReader == null && expectedStorageFileLocation2.exists()) {
            cbReader = CountryBordersReader.deserialize(expectedStorageFileLocation2);
        }
        storage = new BordersGraphStorage();

        return storage;
    }

    private CountryBordersReader createCountryBordersReader() throws IOException {
        String bordersFile = "";
        String countryIdsFile = "";
        String openBordersFile = "";

        preprocessed = Boolean.TRUE.equals(parameters.getPreprocessed());

        if (!preprocessed) {
            if (parameters.getBoundaries() != null) {
                bordersFile = parameters.getBoundaries().toString();
            } else {
                ErrorLoggingUtility.logMissingConfigParameter(BordersGraphStorageBuilder.class, PARAM_KEY_BOUNDARIES);
                // 缺少该信息就无法继续
                throw new MissingResourceException("An OSM file enriched with country tags or a boundary geometry file is needed to use the borders extended storage!", BordersGraphStorage.class.getName(), PARAM_KEY_BOUNDARIES);
            }
        }

        if (parameters.getIds() != null)
            countryIdsFile = parameters.getIds().toString();
        else
            ErrorLoggingUtility.logMissingConfigParameter(BordersGraphStorageBuilder.class, PARAM_KEY_IDS);

        if (parameters.getOpenborders() != null)
            openBordersFile = parameters.getOpenborders().toString();
        else
            ErrorLoggingUtility.logMissingConfigParameter(BordersGraphStorageBuilder.class, PARAM_KEY_OPEN_BORDERS);

        // 读取包含所有国家边界多边形的文件
        return new CountryBordersReader(bordersFile, countryIdsFile, openBordersFile);
    }

    /**
     * 用自定义构建的 CountryBordersReader 覆盖当前的 reader。
     *
     * @param cbr 要使用的 CountryBordersReader 对象
     */
    public void setBordersBuilder(CountryBordersReader cbr) {
        cbReader = cbr;
    }

    @Override
    public void processWay(ReaderWay way) {
        LOGGER.warn("Borders requires geometry for the way!");
    }

    /**
     * 处理从 reader 读取的 way，并判断它是否跨越国界。若跨越，则存储标识其所跨国家的
     * 国家名称。对于预处理数据，从节点标签中提取国家代码，否则执行国家的几何查询。
     *
     * @param way 正在处理的 OSM way
     * @param coords 表示该 way 几何形状的坐标数组
     * @param nodeTags 内部节点 ID 到其标签的映射
     */
    @Override
    public void processWay(ReaderWay way, Coordinate[] coords, Map<Integer, Map<String, String>> nodeTags) {
        if (cbReader == null) {
            return;
        }

        if (preprocessed) {
            extractNodeToCountryMapping(nodeTags);
        } else {
            lookupCountriesAndSetWayTags(way, coords);
        }
    }

    private void extractNodeToCountryMapping(Map<Integer, Map<String, String>> nodeTags) {
        wayNodeTags = new HashMap<>();
        if (nodeTags == null) {
            return;
        }

        nodeTags.forEach((internalNodeId, tagPairs) -> {
            int nodeId = convertTowerNodeId(internalNodeId);
            if (nodeId != EMPTY_NODE) {
                wayNodeTags.put(nodeId, tagPairs.get(TAG_KEY_COUNTRY));
            }
        });
    }

    private void lookupCountriesAndSetWayTags(ReaderWay way, Coordinate[] coords) {
        String[] countries = findBorderCrossing(coords);
        if (countries.length > 1 && !countries[0].equals(countries[1])) {
            way.setTag(TAG_KEY_COUNTRY1, countries[0]);
            way.setTag(TAG_KEY_COUNTRY2, countries[1]);
        } else if (countries.length == 1) {
            way.setTag(TAG_KEY_COUNTRY1, countries[0]);
            way.setTag(TAG_KEY_COUNTRY2, countries[0]);
        }
    }

    private int convertTowerNodeId(int id) {
        if (id < TOWER_NODE)
            return -id - 3;

        return EMPTY_NODE;
    }

    /**
     * 处理边并将其存储到图中。
     * <p>
     * 对于预处理数据，从存储于边的节点上的节点标签中提取国家代码。
     * 对于非预处理数据，从 way 中读取国家标签（在 processWay 方法中引入）。
     * 随后确定边界穿越类型（0 表示无边界，1 表示受控边界，2 表示开放边界）
     * 并将该信息存储到图中。
     *
     * @param way  从 OSM reader 获取的 OSM way。该 way 对应要处理的边
     * @param edge 要处理的图边
     */
    @Override
    public void processEdge(ReaderWay way, EdgeIteratorState edge) {
        if (storage == null) {
            return;
        }

        short countryId1 = 0;
        short countryId2 = 0;

        if (preprocessed) {
            countryId1 = getCountryIdForNode(edge.getBaseNode());
            countryId2 = getCountryIdForNode(edge.getAdjNode());
        } else {
            countryId1 = getCountryIdFromWay(way, TAG_KEY_COUNTRY1);
            countryId2 = getCountryIdFromWay(way, TAG_KEY_COUNTRY2);
        }

        short borderType = getBorderType(countryId1, countryId2);
        storage.setEdgeValue(edge.getEdge(), borderType, countryId1, countryId2);
    }

    private short getCountryIdForNode(int nodeId) {
        String countryCode = wayNodeTags.getOrDefault(nodeId, "");
        try {
            return CountryBordersReader.getCountryIdByISOCode(countryCode);
        } catch (Exception ignore) {
            return 0;
        }
    }

    private short getCountryIdFromWay(ReaderWay way, String tagKey) {
        String countryValue = way.getTag(tagKey);
        try {
            return Short.parseShort(cbReader.getId(countryValue));
        } catch (Exception ignore) {
            return 0;
        }
    }

    private short getBorderType(short countryId1, short countryId2) {
        if (countryId1 == countryId2) {
            return NO_BORDER;
        }
        String countryName1 = cbReader.getName(countryId1);
        String countryName2 = cbReader.getName(countryId2);

        return cbReader.isOpen(countryName1, countryName2) ? OPEN_BORDER : CONTROLLED_BORDER;
    }

    /**
     * 标识扩展名称的方法，该名称用于各种构建过程中
     *
     * @return 此扩展的名称。
     */
    @Override
    public String getName() {
        return BUILDER_NAME;
    }

    /**
     * 用于识别某条 way 所处国家的方法。基本上遍历从文件中读取的国家边界，
     * 然后进行几何计算，以确定该 way 的每个节点位于哪个国家。
     *
     * @param coords 该 way 的坐标
     * @return 表示各节点所处国家的字符串数组。若该 way 仅
     * 位于一个国家，则只返回一个名称。
     */
    public String[] findBorderCrossing(Coordinate[] coords) {

        ArrayList<CountryBordersPolygon> countries = new ArrayList<>();

        boolean hasInternational = false;
        boolean overlap = false;

        // 遍历 linestring 的各点，检查它们位于哪个国家
        int lsLen = coords.length;
        if (lsLen > 1) {
            for (int i = 0; i < lsLen; i++) {
                // 确保它是有效点
                Coordinate c = coords[i];
                if (!Double.isNaN(c.x) && !Double.isNaN(c.y)) {
                    CountryBordersPolygon[] cnts = cbReader.getCandidateCountry(c);
                    for (CountryBordersPolygon cbp : cnts) {
                        // 此检查针对 bbox，因为它是检测是否存在跨越可能性最快的方式
                        if (!countries.contains(cbp)) {
                            countries.add(cbp);
                        }
                    }

                    // 如果该点没有任何候选国家，则说明至少有一个点位于国际区域
                    if (cnts.length == 0)
                        hasInternational = true;
                }
            }
        }
        // 现在获取确定包含该 way 的国家 - 虽然这需要再次迭代，但比下一阶段的
        // linestring 检查更快
        if (countries.size() > 1) {
            ArrayList<CountryBordersPolygon> temp = new ArrayList<>();

            for (int i = 0; i < lsLen; i++) {
                // 遍历线上的每个点并检查其所在国家。除非存在重叠，
                // 否则这里应只有 1 个
                Coordinate c = coords[i];
                if (!Double.isNaN(c.x) && !Double.isNaN(c.y)) {
                    // 检查每个候选国家
                    boolean found = false;
                    int countriesFound = 0;

                    for (CountryBordersPolygon cbp : countries) {
                        if (cbp.inArea(c)) {
                            found = true;
                            countriesFound++;
                            if (!temp.contains(cbp)) {
                                // 此时我们只想添加尚不存在的国家。基本上，如果列表中已有同名
                                // 的边界多边形，就不要再添加该国家
                                temp.add(cbp);
                            }
                        }
                    }

                    if (countriesFound > 1) {
                        overlap = true;
                    }

                    if (!found) {
                        hasInternational = true;
                    }
                }
            }


            // 替换该列表
            countries = temp;
        }

        // 现在我们得到了节点所在所有国家的列表 - 如果不止一个，则很可能
        // 跨越了边界，但并不确定，因为某些争议区域中国家会重叠，从而可能并未跨越
        // 任何边界。
        if (countries.size() > 1 && overlap) {
            boolean crosses = false;
            // 构建 linestring
            LineString ls = gf.createLineString(coords);
            // 检查是否实际跨越了边界，不过我们只对重叠的多边形执行此操作
            for (CountryBordersPolygon cp : countries) {
                // 我们只想在所有点都位于两个国家时执行此检查，因为这意味着
                // 重叠
                if (cp.crossesBoundary(ls)) {
                    // 它跨越了边界
                    crosses = true;
                    break;
                }
            }

            if (!crosses) {
                // 我们想表明它位于同一个国家，因此只返回一个国家
                CountryBordersPolygon cp = countries.get(0);
                countries.clear();
                countries.add(cp);
            }
        }

        // 现在获取国家名称
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < countries.size(); i++) {
            if (!names.contains(countries.get(i).getName()))
                names.add(countries.get(i).getName());
        }

        // 如果存在国际区域内的点且至少有一个国家名称，则说明它是边界
        if (hasInternational && !countries.isEmpty()) {
            names.add(CountryBordersReader.INTERNATIONAL_NAME);
        }

        return names.toArray(new String[0]);
    }

    public CountryBordersReader getCbReader() {
        return cbReader;
    }
}
