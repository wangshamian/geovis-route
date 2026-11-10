/*
 *  Licensed to GIScience Research Group, Heidelberg University (GIScience)
 *
 *       http://www.giscience.uni-hd.de
 *       http://www.heigit.org
 *
 *  under one or more contributor license agreements. See the NOTICE file
 *  distributed with this work for additional information regarding copyright
 *  ownership. The GIScience licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.heigit.geovisroute.util;

public class UnitsConverter {
    private UnitsConverter() {
    }

    public static double sqMetersToSqMiles(double value) {
        return value * 3.86102e-7;
    }

    public static double sqMetersToSqKilometers(double value) {
        return value * 1e-6;
    }

    public static double metersToKilometers(double value) {
        return value * 0.001;
    }

    public static double metersToMiles(double value) {
        return value * 0.000621371;
    }

    /**
     * 将 OSM 的 width 值转换为以米表示的十进制数值。在 OSM 中，width 可能以多种不同单位存储，
     * 因此该方法尝试将它们全部转换为米。
     *
     * @param unprocessedLinearValue 获取到的 width 标签值
     * @return 转换为米后的 width 值
     */
    public static double convertOSMDistanceTagToMeters(String unprocessedLinearValue) {
        double processedLinearValue = -1d;

        // 有效值包括：
		/*
		processedLinearValue=x (默认，米)
		processedLinearValue=x m		(米)
		processedLinearValue=x km		(千米)
		processedLinearValue=x mi		(英里)
		processedLinearValue=x nmi		(海里)
		processedLinearValue=x'y"		(英尺和英寸)

		不过，许多人会省略空格，尽管本不应如此
		 */

        if (unprocessedLinearValue.contains(" ")) {
            // 处理带有明确单位的情况
            String[] split = unprocessedLinearValue.split(" ");
            if (split.length == 2) {
                try {
                    processedLinearValue = Double.parseDouble(split[0]);

                    switch (split[1]) {
                        case "m":
                            // 无需处理，已经是米
                            break;
                        case "km":
                            processedLinearValue = processedLinearValue / 0.001;
                            break;
                        case "cm":
                            processedLinearValue = processedLinearValue / 100.0;
                            break;
                        case "mi":
                            processedLinearValue = processedLinearValue / 0.000621371;
                            break;
                        case "nmi":
                            processedLinearValue = processedLinearValue / 0.000539957;
                            break;
                        default:
                            // 无效单位
                            processedLinearValue = -1d;
                    }
                } catch (Exception e) {
                    processedLinearValue = -1d;
                }
            }
        } else if (unprocessedLinearValue.contains("'") && unprocessedLinearValue.contains("\"")) {
            // 处理英尺和英寸的情况
            String[] split = unprocessedLinearValue.split("'");
            if (split.length == 2) {
                split[1] = split[1].replace("\"", "");
                try {
                    processedLinearValue = Double.parseDouble(split[0]) * 12d; // 1 英尺等于 12 英寸
                    processedLinearValue += Double.parseDouble(split[1]);

                    // 转换为米
                    processedLinearValue = processedLinearValue * 0.0254;

                } catch (Exception e) {
                    processedLinearValue = -1d;
                }
            }
        } else {
            // 尝试读取一个数字并假定其单位为米
            try {
                processedLinearValue = Double.parseDouble(unprocessedLinearValue);
            } catch (Exception e) {
                processedLinearValue = -1d;
            }
        }

        // 如果 processedLinearValue 仍为 -1，可能是使用了无效标签，因此尝试解析最常见的错误写法
        if (processedLinearValue == -1d) {
            // 注意顺序：3cm 同时以 cm 和 m 结尾，因此应先检查 cm
            try {
                if (unprocessedLinearValue.endsWith("cm")) {
                    String[] split = unprocessedLinearValue.split("cm");
                    if (split.length == 2) {
                        processedLinearValue = Double.parseDouble(split[0]) / 100f;
                    }
                } else if (unprocessedLinearValue.endsWith("km")) {
                    String[] split = unprocessedLinearValue.split("km");
                    if (split.length == 2) {
                        processedLinearValue = Double.parseDouble(split[0]) / 0.001f;
                    }
                } else if (unprocessedLinearValue.endsWith("nmi")) {
                    String[] split = unprocessedLinearValue.split("nmi");
                    if (split.length == 2) {
                        processedLinearValue = Double.parseDouble(split[0]) / 0.000539957;
                    }
                } else if (unprocessedLinearValue.endsWith("mi")) {
                    String[] split = unprocessedLinearValue.split("mi");
                    if (split.length == 2) {
                        processedLinearValue = Double.parseDouble(split[0]) / 0.000621371;
                    }
                } else if (unprocessedLinearValue.endsWith("m")) {
                    String[] split = unprocessedLinearValue.split("m");
                    if (split.length == 2) {
                        processedLinearValue = Double.parseDouble(split[0]);
                    }
                }
            } catch (NumberFormatException nfe) {
                // 数字无效，因此将其设为 "无效" 值
                processedLinearValue = -1d;
            }
        }

        // 如果该值大于 3，编码器需要更多位来存储，因此直接截断为 3
        if (processedLinearValue > 3)
            processedLinearValue = 3;

        return processedLinearValue;
    }

    /**
     * 将表示坡度的字符串转换为百分比坡度值。在 OSM 中，该标签值可能已经是百分比数值，
     * 也可能写作 "up"、"down"、"steep" 等形式，此时会假定一个坡度值。
     *
     * @param unprocessedInclineValue 从 incline 标签获取的值
     * @param declineToIncline        为 true 时始终返回正值，否则下坡会以负值返回
     * @return 百分比坡度值
     */
    public static int convertOSMInclineValueToPercentage(String unprocessedInclineValue, boolean declineToIncline) {

        if (unprocessedInclineValue != null) {
            double v = 0d;
            boolean isDegree = false;
            try {
                unprocessedInclineValue = unprocessedInclineValue.replace("%", "");
                unprocessedInclineValue = unprocessedInclineValue.replace(",", ".");
                if (unprocessedInclineValue.contains("°")) {
                    unprocessedInclineValue = unprocessedInclineValue.replace("°", "");
                    isDegree = true;
                }

                // 将文字描述替换为假定值
                unprocessedInclineValue = unprocessedInclineValue.replace("up", "10");
                unprocessedInclineValue = unprocessedInclineValue.replace("down", "-10");
                unprocessedInclineValue = unprocessedInclineValue.replace("yes", "10");
                unprocessedInclineValue = unprocessedInclineValue.replace("steep", "15");
                unprocessedInclineValue = unprocessedInclineValue.replace("no", "0");
                unprocessedInclineValue = unprocessedInclineValue.replace("+/-0", "0");
                v = Double.parseDouble(unprocessedInclineValue);
                if (isDegree) {
                    v = Math.tan(v) * 100;
                }
            } catch (Exception ex) {
                // 出现问题，返回 0 并假定无坡度
                v = 0;
            }

            if (declineToIncline && v < 0) {
                v = v * -1.0;
            }

            return (int) Math.round(v);
        }

        return 0;
    }
}
