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
package org.heigit.geovisroute.isochrones.statistics.postgresql;

import com.graphhopper.util.Helper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.log4j.Logger;
import org.heigit.geovisroute.exceptions.InternalServerException;
import org.heigit.geovisroute.isochrones.Isochrone;
import org.heigit.geovisroute.isochrones.IsochronesErrorCodes;
import org.heigit.geovisroute.isochrones.statistics.StatisticsProvider;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * 此类处理人口统计查询。它将内部 api 调用转换为 SQL 语句，
 * 并在存放人口数据的 PostgreSQL 服务器上执行查询。
 *
 * @author OpenRouteServiceTeam
 * @author Julian Psotta, julian@openrouteservice.org
 */
public class PostgresSQLStatisticsProvider implements StatisticsProvider {
    private static final Logger LOGGER = Logger.getLogger(PostgresSQLStatisticsProvider.class.getName());

    private static final String PARAM_KEY_PASS = "password";

    private String tableName = null;
    private String geomColumn = null;
    private HikariDataSource dataSource;
    private String postgisVersion = null;

    /**
     * 此函数根据 gr-config.json 中的设置初始化与服务器的连接。
     * 连接使用 {@link HikariDataSource} 对象建立，配置数据来自 gr-config.json。
     *
     * @param parameters 保存 gr-config.json 中服务器配置数据的 {@link Map}。
     * @throws Exception
     */
    @Override
    public void init(Map<String, Object> parameters) throws Exception {
        dataSource = null;
        tableName = null;
        geomColumn = null;
        postgisVersion = null;

        String value = (String) parameters.get("table_name");
        if (Helper.isEmpty(value))
            throw new InternalServerException(IsochronesErrorCodes.UNKNOWN, "'table_name' parameter can not be null or empty.");
        else
            tableName = value;

        value = (String) parameters.get("geometry_column");
        if (Helper.isEmpty(value))
            throw new InternalServerException(IsochronesErrorCodes.UNKNOWN, "'geometry_column' parameter can not be null or empty.");
        else
            geomColumn = value;

        value = (String) parameters.get("postgis_version");
        if (Helper.isEmpty(value))
            LOGGER.debug("No PostGIS version provided");
        else
            postgisVersion = value;

        //https://github.com/pgjdbc/pgjdbc/pull/772
        org.postgresql.Driver.isRegistered();

        HikariConfig config = new HikariConfig();
        String port = "5432";
        if (parameters.containsKey("port"))
            port = Integer.toString((Integer) parameters.get("port"));
        config.setJdbcUrl("jdbc:postgresql://%s:%s/%s".formatted(parameters.get("host"), port, parameters.get("db_name")));
        config.setDataSourceClassName(PGSimpleDataSource.class.getName());
        config.addDataSourceProperty("databaseName", parameters.get("db_name"));
        config.addDataSourceProperty("user", parameters.get("user"));
        if (parameters.containsKey(PARAM_KEY_PASS))
            config.addDataSourceProperty(PARAM_KEY_PASS, parameters.get(PARAM_KEY_PASS));
        config.addDataSourceProperty("serverName", parameters.get("host"));
        config.addDataSourceProperty("portNumber", parameters.get("port"));
        if (parameters.containsKey("max_pool_size"))
            config.setMaximumPoolSize((Integer) parameters.get("max_pool_size"));
        config.setMinimumIdle(1);
        config.setConnectionTestQuery("SELECT 1");

        dataSource = new HikariDataSource(config);
    }

    /**
     * 此函数关闭 {@link HikariDataSource} 连接。
     *
     * @throws Exception
     */
    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    /**
     * 该函数接收一个 {@link Isochrone} 以及保存 api attributes 变量中所设属性参数的 {@link String}[] 作为输入。
     * 目前只能查询 pop_area 和 pop_total，可同时或单独查询。
     *
     * @param isochrone  作为输入的 {@link Isochrone}。
     * @param properties 作为输入、保存属性参数的 {@link String}[]。
     * @return 返回一个 double[]，按 attributes 中所要求的顺序保存所需的值。
     * @throws Exception 如果查询未返回任何值或 sql 有误，将抛出 {@link Exception}。
     */
    @Override
    public double[] getStatistics(Isochrone isochrone, String[] properties) throws Exception {
        int nProperties = properties.length;
        double[] res = new double[nProperties];
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            String sql = null;
            for (String property : properties) {
                String polyGeom = isochrone.getGeometry().toText();
                if ("total_pop".equals(property)) {
                    // 较新版本的 PostGIS 具有不同的汇总方法
                    if (postgisVersion != null && Float.parseFloat(postgisVersion) > 2.4) {
                        sql = "SELECT ROUND((ST_SummaryStatsAgg(ST_Clip(" + geomColumn + ", poly), 1, TRUE, 1)).sum::numeric, 0) AS total_pop FROM " + tableName + ", ST_Transform(ST_GeomFromText('" + polyGeom + "', 4326), 54009) AS poly WHERE ST_Intersects(poly, " + geomColumn + ") GROUP BY poly;";
                    } else {
                        sql = "SELECT ROUND(SUM((ST_SummaryStats(ST_Clip(" + geomColumn + ", poly))).sum)) AS total_pop FROM " + tableName + ", ST_Transform(ST_GeomFromText('" + polyGeom + "', 4326), 954009) AS poly WHERE ST_Intersects(poly, " + geomColumn + ") GROUP BY poly;";
                    }
                }
            }
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(sql);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                // 检查 resultSet 是否包含值
                if (resultSet.next()) {
                    // 检查每个属性的结果是否包含值
                    int propertyCounter = 0;
                    for (String property : properties) {
                        int i = 0;
                        // 检查每个值
                        while (i < nProperties) {
                            // 获取列名
                            String columnName = resultSet.getMetaData().getColumnName(i + 1);
                            // 如果某个值与当前属性匹配，则将其设置到 result[] 中的正确位置
                            if (columnName.equals(property))
                                res[propertyCounter] = resultSet.getDouble(i + 1);
                            i++;
                        }
                        propertyCounter++;
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
            throw new InternalServerException(IsochronesErrorCodes.UNKNOWN, "Unable to retrieve data from the data source.");
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return res;

    }

    /**
     * 返回要用于查询的驱动名称，供 {@link org.heigit.geovisroute.isochrones.statistics.StatisticsProviderFactory} 使用。
     *
     * @return 以 {@link String} 格式返回提供者名称。
     */
    @Override
    public String getName() {
        return "postgresql";
    }
}
