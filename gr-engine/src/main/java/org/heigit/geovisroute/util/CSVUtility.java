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
package org.heigit.geovisroute.util;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于从 CSV 文件读取数据的辅助类。基于以下代码实现：
 * https://www.mkyong.com/java/how-to-read-and-parse-csv-file-in-java/
 */
public class CSVUtility {
    private static final Logger LOGGER = Logger.getLogger(CSVUtility.class);

    private static final char DEFAULT_SEPARATOR = ',';
    private static final char DEFAULT_QUOTE = '"';
    private static final boolean HAS_HEADER = true;

    private CSVUtility() {
    }

    public static List<List<String>> readFile(String file) {
        return readFile(file, HAS_HEADER);
    }

    /**
     * 使用给定的文件名、分隔符、引号字符以及是否存在表头，从 CSV 文件读取数据
     *
     * @param file   CSV 文件
     * @param header 是否忽略 CSV 的第一行
     * @return 由各行组成的 ArrayList，其中每个元素是包含各列值的 ArrayList
     */
    public static List<List<String>> readFile(String file, boolean header) {
        // 打开 CSV 文件
        String ln = "";

        List<List<String>> lines = new ArrayList<>();
        boolean headerRead = false;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            while ((ln = br.readLine()) != null) {
                if (header && lines.isEmpty() && !headerRead) {
                    headerRead = true;
                    continue;
                }

                lines.add(parseLine(ln));
            }
        } catch (IOException ioe) {
            LOGGER.error("Could not read from file: " + file);
        }

        return lines;
    }

    private static ArrayList<String> parseLine(String csvLine) {
        return parseLine(csvLine, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
    }

    /**
     * 解析从 CSV 文件读取的一行文本。该行会被拆分为多个值。
     *
     * @param csvLine     CSV 文件中的一行
     * @param separator   值之间的分隔符
     * @param customQuote 用于包裹字符串的字符
     * @return
     */
    private static ArrayList<String> parseLine(String csvLine, char separator, char customQuote) {

        ArrayList<String> result = new ArrayList<>();

        //如果为空则返回！
        if (csvLine == null || csvLine.isEmpty()) {
            return result;
        }

        if (customQuote == ' ') {
            customQuote = DEFAULT_QUOTE;
        }

        if (separator == ' ') {
            separator = DEFAULT_SEPARATOR;
        }

        StringBuilder curVal = new StringBuilder();
        boolean inQuotes = false;
        boolean startCollectChar = false;
        boolean doubleQuotesInColumn = false;

        char[] chars = csvLine.toCharArray();

        for (char ch : chars) {

            if (inQuotes) {
                startCollectChar = true;
                if (ch == customQuote) {
                    inQuotes = false;
                    doubleQuotesInColumn = false;
                } else {

                    //修复：允许在自定义引号内出现 ""
                    if (ch == '\"') {
                        if (!doubleQuotesInColumn) {
                            curVal.append(ch);
                            doubleQuotesInColumn = true;
                        }
                    } else {
                        curVal.append(ch);
                    }

                }
            } else {
                if (ch == customQuote) {

                    inQuotes = true;

                    if (chars[0] != '"' && customQuote != '\"') {
                        curVal.append('"');
                    }

                    //列中的双引号会进入此分支！
                    if (startCollectChar) {
                        curVal.append('"');
                    }

                } else if (ch == separator) {

                    result.add(curVal.toString());

                    curVal = new StringBuilder();
                    startCollectChar = false;

                } else if (ch == '\r') {
                    //忽略 LF 字符
                } else if (ch == '\n') {
                    //结束，跳出！
                    break;
                } else {
                    curVal.append(ch);
                }
            }

        }

        result.add(curVal.toString());

        return result;
    }
}
