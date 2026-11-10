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
package org.heigit.geovisroute.routing.graphhopper.extensions.reader.borders;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.log4j.Logger;
import org.heigit.geovisroute.geojson.GeometryJSON;
import org.heigit.geovisroute.util.CSVUtility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.io.*;
import java.util.*;

public class CountryBordersReader implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(CountryBordersReader.class);

    public static final String INTERNATIONAL_NAME = "INTERNATIONAL";
    public static final String INTERNATIONAL_ID = "-1";
    public static final String KEY_PROPERTIES = "properties";
    private static final String NAME_FIELD = "name";
    private static final String HIERARCHY_ID_FIELD = "hierarchy";

    private final String borderFile;
    private final String idsPath;
    private final String openPath;

    private final HashMap<String, CountryInfo> ids = new HashMap<>();
    private final HashMap<String, ArrayList<String>> openBorders = new HashMap<>();
    private final Map<String, Short> isoCodes = new HashMap<>();
    private Map<Short, String> names = new HashMap<>();
    private final HashMap<Long, CountryBordersHierarchy> hierarchies = new HashMap<>();

    // 包级可见，用于测试
    static CountryBordersReader currentInstance = null;

    /**
     * 空构造函数，不读取任何数据 —— 用户必须显式传入信息
     */
    public CountryBordersReader() {
        borderFile = "";
        idsPath = "";
        openPath = "";

        currentInstance = this;
    }

    /**
     * 创建 CountryBordersReader 对象，并读入国界、id 和开放国界的数据。
     *
     * @param filepath 国界（多边形）数据的路径
     * @param idsPath  包含国家数字标识（及英文名称）的 csv 文件路径
     * @param openPath 包含具有开放国界的国家名称对的 csv 文件路径
     */
    public CountryBordersReader(String filepath, String idsPath, String openPath) throws IOException {
        borderFile = filepath;
        this.idsPath = idsPath;
        this.openPath = openPath;

        try {
            if (!"".equals(borderFile)) {
                JSONObject data = readBordersData();
                LOGGER.info("Border geometries read");

                createGeometries(data);
            }

            readIds();
            LOGGER.info("Border ids data read");

            readOpenBorders();
            LOGGER.info("Border openness data read");
        } catch (IOException ioe) {
            // 读取数据时出现问题
            LOGGER.error("Could not access file(s) required for border crossing analysis");
            throw ioe;
        }
        currentInstance = this;
    }

    // 用于测试 mock
    public void addHierarchy(Long id, CountryBordersHierarchy hierarchy) {
        if (!hierarchies.containsKey(id)) {
            hierarchies.put(id, hierarchy);
        }
    }

    // 用于测试 mock
    public void addId(String id, String localName, String englishName, String cca2, String cca3) {
        if (!ids.containsKey(localName)) {
            ids.put(localName, new CountryInfo(id, localName, englishName));
            isoCodes.put(cca2.trim().toUpperCase(), Short.parseShort(id));
            isoCodes.put(cca3.trim().toUpperCase(), Short.parseShort(id));
        }
    }

    /**
     * 向开放国界列表中添加一条开放国界记录。如果尚不存在，则会为两个方向都创建记录
     * （即传入德国和法国会生成两条记录 —— 德国->法国 和 法国->德国）。
     *
     * @param country1
     * @param country2
     */
    public void addOpenBorder(String country1, String country2) {
        if (openBorders.containsKey(country1)) {
            // 该键已存在，因此现在在不存在的情况下添加第二个国家
            if (!openBorders.get(country1).contains(country2)) {
                openBorders.get(country1).add(country2);
            }
        } else {
            ArrayList<String> c2 = new ArrayList<>();
            c2.add(country2);
            openBorders.put(country1, c2);
        }

        if (openBorders.containsKey(country2)) {// 该键已存在，因此现在在不存在的情况下添加第二个国家
            if (!openBorders.get(country2).contains(country1)) {
                openBorders.get(country2).add(country1);
            }
        } else {
            ArrayList<String> c1 = new ArrayList<>();
            c1.add(country1);
            openBorders.put(country2, c1);
        }
    }

    /**
     * 从 GeoJSON 文件中读取表示不同国家边界的几何。理想情况下，该文件应
     * 使用许多按层次划分的小对象来组织。
     * <p>
     * 如果文件是 .tar.gz 格式，则会先解压，然后将解压后的数据读入
     * JSON 对象。
     *
     * @return 表示文件内容的 (Geo)JSON 对象
     */
    private JSONObject readBordersData() throws IOException {
        String data = "";

        BufferedReader buf = null;
        TarArchiveInputStream tis = null;
        try (InputStream is = new FileInputStream(borderFile)) {
            if (borderFile.endsWith(".tar.gz")) {
                // 处理的是压缩文件
                tis = new TarArchiveInputStream(
                        new GzipCompressorInputStream(
                                new BufferedInputStream(is)
                        )
                );

                TarArchiveEntry entry;
                StringBuilder sb = new StringBuilder();

                while ((entry = tis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        byte[] bytes = new byte[(int) entry.getSize()];
                        while (tis.read(bytes) > 0)
                            sb.append(new String(bytes));
                    }
                }
                data = sb.toString();
            } else {
                // 假定是普通文件，逐行读取

                buf = new BufferedReader(new InputStreamReader(is));

                String line = "";
                StringBuilder sb = new StringBuilder();

                while ((line = buf.readLine()) != null) {
                    sb.append(line);
                }

                data = sb.toString();
            }
        } catch (IOException ioe) {
            LOGGER.warn("Cannot access borders file!");
            throw ioe;
        } finally {
            try {
                if (tis != null)
                    tis.close();
                if (buf != null)
                    buf.close();
            } catch (IOException ioe) {
                LOGGER.warn("Error closing file reader buffers!");
            }
        }

        return new JSONObject(data);
    }

    /**
     * 根据 GeoJSON 对象生成几何。这些 CountryBordersPolygon 存储在 CountryBordersHierarchy
     * 对象中，以加快查找过程。
     *
     * @param json
     */
    private void createGeometries(JSONObject json) {
        JSONArray features = json.getJSONArray("features");

        int objectCount = 0;
        int hierarchyCount = 0;

        int len = features.length();

        for (int i = 0; i < len; i++) {
            try {
                JSONObject obj = features.getJSONObject(i);
                Geometry geom = GeometryJSON.parse(obj.getJSONObject("geometry"));

                // 还需要国家的 id 及其层次 id
                String id = obj.getJSONObject(KEY_PROPERTIES).getString(NAME_FIELD);

                Long hId = -1L;

                // 如果没有层次信息，则将层次 id 默认设为 1
                if (obj.getJSONObject(KEY_PROPERTIES).has(HIERARCHY_ID_FIELD))
                    hId = obj.getJSONObject(KEY_PROPERTIES).getLong(HIERARCHY_ID_FIELD);

                // 创建国界对象
                CountryBordersPolygon c = new CountryBordersPolygon(id, geom);

                // 添加到层次结构中
                if (c != null) {
                    if (!hierarchies.containsKey(hId)) {
                        hierarchies.put(hId, new CountryBordersHierarchy());
                        hierarchyCount++;
                    }

                    hierarchies.get(hId).add(c);
                    objectCount++;
                }

            } catch (Exception e) {
                LOGGER.warn("Error reading country polygon from borders file!" + e.getMessage());
            }
        }

        LOGGER.info(objectCount + " countries read in " + hierarchyCount + " hiearchies");
    }

    /**
     * 用于获取给定点所在的国家对象列表。如果该点位于重叠区域中，
     * 则可能返回多个国家。
     *
     * @param c 想要查询其所属国家的点
     * @return 该点所在几何范围内的 CountryBorderPolygon 数组。
     */
    public CountryBordersPolygon[] getCountry(Coordinate c) {
        ArrayList<CountryBordersPolygon> countries = new ArrayList<>();
        Iterator it = hierarchies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, CountryBordersHierarchy> pair = (Map.Entry) it.next();
            CountryBordersHierarchy h = pair.getValue();
            if (h.inBbox(c)) {
                // 现在需要检查各个国家
                List<CountryBordersPolygon> ps = h.getPolygons();
                for (CountryBordersPolygon cp : ps) {
                    if (cp.inBbox(c) && cp.inArea(c)) {
                        countries.add(cp);
                    }
                }
            }
        }

        return countries.toArray(new CountryBordersPolygon[0]);
    }

    /**
     * 用于获取给定点可能所在的国家对象列表。如果该点位于重叠区域中，
     * 则可能返回多个。该方法基于包围盒进行判断，因此返回的国家
     * 可能实际上并不包围该点。应使用该方法快速近似判断
     * 某国家是否可能是包含该点的候选。
     *
     * @param c 想要查询其所属国家的点
     * @return 该点所在几何范围内的 CountryBorderPolygon 数组。
     */
    public CountryBordersPolygon[] getCandidateCountry(Coordinate c) {
        ArrayList<CountryBordersPolygon> countries = new ArrayList<>();
        Iterator it = hierarchies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, CountryBordersHierarchy> pair = (Map.Entry) it.next();
            CountryBordersHierarchy h = pair.getValue();
            if (h.inBbox(c)) {
                // 现在需要检查各个国家
                List<CountryBordersPolygon> ps = h.getPolygons();
                for (CountryBordersPolygon cp : ps) {
                    if (cp.inBbox(c)) {
                        countries.add(cp);
                    }
                }
            }
        }

        return countries.toArray(new CountryBordersPolygon[0]);
    }

    /**
     * 获取国家的唯一标识符（在构造函数中从 CSV 文件读取）
     *
     * @param name 国家的本地名称
     * @return 唯一标识符
     */
    public String getId(String name) {
        if (name.equals(INTERNATIONAL_NAME))
            return INTERNATIONAL_ID;

        if (ids.containsKey(name))
            return ids.get(name).id;
        else
            return "";
    }

    /**
     * 获取国家的英文名称（从 id CSV 读取）
     *
     * @param name 国家的本地名称
     * @return 国家的英文名称
     */
    public String getEngName(String name) {
        if (name.equals(INTERNATIONAL_NAME))
            return INTERNATIONAL_NAME;

        if (ids.containsKey(name))
            return ids.get(name).nameEng;
        else
            return "";
    }

    public String getName(short id) {
        return names.get(id);
    }

    /**
     * 获取两个指定国家之间的国界是开放还是封闭
     *
     * @param c1 国界的第一个国家（英文名称）
     * @param c2 国界的第二个国家（英文名称）
     * @return
     */
    public boolean isOpen(String c1, String c2) {
        if (openBorders.containsKey(c1)) {
            return openBorders.get(c1).contains(c2);
        } else if (openBorders.containsKey(c2))
            return openBorders.get(c2).contains(c1);

        return false;
    }

    /**
     * 通过 ISO 3166-1 Alpha-2 / Alpha-3 代码获取国家 ID。这是一个静态方法，使用该类最后创建的实例。
     * （通常每个服务器实例只应有一个实例，即使不是，所使用的
     * 国界数据也应相同）
     *
     * @param code 要查询的代码
     * @return 国家的 ID，若未找到则返回 0
     */
    public static short getCountryIdByISOCode(String code) {
        return currentInstance != null ? currentInstance.isoCodes.getOrDefault(code.toUpperCase(), (short) 0) : 0;
    }

    /**
     * 从 id csv 中读取信息。包括唯一标识符、国家的本地名称以及
     * 国家的英文名称。可选地从第 4 和第 5 列读取 ISO 代码（预期分别包含
     * Alpha-2 和 Alpha-3 代码），供 getCountryIdByISOCode 方法使用。
     */
    private void readIds() {
        // 首先读取 csv 文件
        List<List<String>> data = CSVUtility.readFile(idsPath);

        // 遍历并存入 hashmap
        int countries = 0;
        int isoCCA2 = 0;
        int isoCCA3 = 0;
        for (List<String> col : data) {
            short shortID = 0;
            try {
                shortID = Short.parseShort(col.get(0));
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid country ID " + col.get(0));
                continue;
            }
            if (col.size() >= 3) {
                ids.put(col.get(1), new CountryInfo(col.get(0), col.get(1), col.get(2)));
                names.put(shortID, col.get(2));
                countries++;
            }
            if (col.size() >= 4 && !col.get(3).trim().isEmpty()) {
                isoCodes.put(col.get(3).trim().toUpperCase(), shortID);
                isoCCA2++;
            }
            if (col.size() == 5 && !col.get(4).trim().isEmpty()) {
                isoCodes.put(col.get(4).trim().toUpperCase(), shortID);
                isoCCA3++;
            }
        }
        LOGGER.info(countries + " country IDs read");
        if (isoCCA2 > 0) {
            if (isoCCA2 < countries) {
                LOGGER.warn((countries - isoCCA2) + " countries have no ISO 3166-1 CCA2 code assigned.");
            } else {
                LOGGER.info("ISO 3166-1 CCA2 codes enabled for all countries");
            }
        }
        if (isoCCA3 > 0) {
            if (isoCCA3 < countries) {
                LOGGER.warn((countries - isoCCA3) + " countries have no ISO 3166-1 CCA3 code assigned.");
            } else {
                LOGGER.info("ISO 3166-1 CCA3 codes enabled for all countries");
            }
        }
    }

    /**
     * 读取两个国家之间的国界是否开放的信息。如果某条国界出现在文件中，
     * 则它是开放国界。
     */
    private void readOpenBorders() {
        // 首先读取 csv 文件
        List<List<String>> data = CSVUtility.readFile(openPath);

        // 遍历并存入 hashmap
        for (List<String> col : data) {
            if (col.size() == 2) {
                // 查看是否已存在起始国家
                if (!openBorders.containsKey(col.get(0))) {
                    openBorders.put(col.get(0), new ArrayList<>());
                }
                openBorders.get(col.get(0)).add(col.get(1));
            }
        }
    }

    public void serialize(File file) {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file))) {
            objectOutputStream.writeObject(this);
            objectOutputStream.flush();
        } catch (Exception exception) {
            LOGGER.error("Could not write the CountryBordersReader object to file: " + exception.getMessage());
        }
    }

    public static CountryBordersReader deserialize(File file) {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file))) {
            currentInstance = (CountryBordersReader) objectInputStream.readObject();
        } catch (Exception exception) {
            LOGGER.error("Could not read the CountryBordersReader object from file: " + exception.getMessage());
        }
        return currentInstance;
    }

    /**
     * 持有类，用于存储从 ids csv 读取的国家信息。
     */
    private static class CountryInfo implements Serializable {
        private String id;
        private String name;
        private String nameEng;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNameEng() {
            return nameEng;
        }

        public void setNameEng(String nameEng) {
            this.nameEng = nameEng;
        }

        public CountryInfo(String id, String name, String nameEng) {
            this.id = id;
            this.name = name;
            this.nameEng = nameEng;
        }
    }


}
