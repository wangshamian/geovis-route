package org.heigit.geovisroute.util;



/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */


/**
 * 该类实现快速、线程安全的 double 值格式化，
 * 可指定小数位数。
 * <p>
 * 格式化方法的约定如下：
 * 若源值的绝对值大于或等于 1，
 * 则使用 decimals 参数定义小数位数；否则，
 * 使用 precision 参数定义小数位数。
 * <p>
 * 示例如下（假设 decimals 为 4、precision 为 8）：
 * <ul>
 * <li>0.0 应输出为 "0"
 * <li>0.1 应输出为 "0.1"
 * <li>1234.1 应输出为 "1234.1"
 * <li>1234.1234567 应输出为 "1234.1235"（注意末尾的 5！发生了舍入！）
 * <li>1234.00001 应输出为 "1234"
 * <li>0.00001 应输出为 "0.00001"（这里可以看到 "precision" 参数的效果）
 * <li>0.00000001 应输出为 "0.00000001"
 * <li>0.000000001 应输出为 "0"
 * </ul>
 * <p>
 * 原作者为 Julien Aym&eacute;。
 */
public final class DoubleFormatUtil {

    private DoubleFormatUtil() {
    }

    /**
     * 按给定精度对源值进行舍入，
     * 并将舍入后的值写入目标缓冲区
     *
     * @param source    要舍入的源值
     * @param decimals  舍入所用的小数位数（当 abs(source) &ge; 1.0 时使用）
     * @param precision 舍入所用的精度（当 abs(source) &lt; 1.0 时使用）
     * @param target    写入的目标缓冲区
     */
    public static void formatDouble(double source, int decimals, int precision, StringBuilder target) {
        int scale = (Math.abs(source) >= 1.0) ? decimals : precision;
        if (tooManyDigitsUsed(source, scale) || tooCloseToRound(source, scale)) {
            formatDoublePrecise(source, decimals, precision, target);
        } else {
            formatDoubleFast(source, decimals, precision, target);
        }
    }

    /**
     * 按给定精度对源值进行舍入，
     * 并将舍入后的值写入目标缓冲区
     * <p>
     * 该方法内部使用源值的字符串表示，
     * 以避免任何双精度计算误差。
     *
     * @param source    要舍入的源值
     * @param decimals  舍入所用的小数位数（当 abs(source) &ge; 1.0 时使用）
     * @param precision 舍入所用的精度（当 abs(source) &lt; 1.0 时使用）
     * @param target    写入的目标缓冲区
     */
    public static void formatDoublePrecise(double source, int decimals, int precision, StringBuilder target) {
        if (isRoundedToZero(source, decimals, precision)) {
            // 将始终舍入为 0
            target.append('0');
            return;
        } else if (Double.isNaN(source) || Double.isInfinite(source)) {
            // 无法格式化
            target.append(source);
            return;
        }

        boolean negative = source < 0.0;
        if (negative) {
            source = -source;
            // 只处理一次
            target.append('-');
        }
        int scale = (source >= 1.0) ? decimals : precision;

        // 要精确格式化 double，唯一的方法是使用 double 的字符串
        // 表示，然后对其执行数学整数运算。
        String s = Double.toString(source);
        if (source >= 1e-3 && source < 1e7) {
            // double 的普通表示形式："intPart.decimalPart"
            int dot = s.indexOf('.');
            String decS = s.substring(dot + 1);
            int decLength = decS.length();
            if (scale >= decLength) {
                if ("0".equals(decS)) {
                    // 源值是数学整数
                    target.append(s, 0, dot);
                } else {
                    target.append(s);
                    // 去除末尾的零
                    for (int l = target.length() - 1; l >= 0 && target.charAt(l) == '0'; l--) {
                        target.setLength(l);
                    }
                }
                return;
            } else if (scale + 1 < decLength) {
                // 忽略不必要的数字
                decLength = scale + 1;
                decS = decS.substring(0, decLength);
            }
            long intP = Long.parseLong(s.substring(0, dot));
            long decP = Long.parseLong(decS);
            format(target, scale, intP, decP);
        } else {
            // double 的科学计数法表示形式："x.xxxxxEyyy"
            int dot = s.indexOf('.');
            assert dot >= 0;
            int exp = s.indexOf('E');
            assert exp >= 0;
            int exposant = Integer.parseInt(s.substring(exp + 1));
            String intS = s.substring(0, dot);
            String decS = s.substring(dot + 1, exp);
            int decLength = decS.length();
            if (exposant >= 0) {
                int digits = decLength - exposant;
                if (digits <= 0) {
                    // 无小数部分，
                    // 不涉及舍入
                    target.append(intS);
                    target.append(decS);
                    target.append("0".repeat(-digits));
                } else if (digits <= scale) {
                    // 小数部分的精度低于标度，
                    // 不涉及舍入
                    target.append(intS);
                    target.append(decS, 0, exposant);
                    target.append('.');
                    target.append(decS.substring(exposant));
                } else {
                    // 小数位数大于标度，
                    // 涉及舍入
                    long intP = Long.parseLong(intS) * tenPow(exposant) + Long.parseLong(decS.substring(0, exposant));
                    long decP = Long.parseLong(decS.substring(exposant, exposant + scale + 1));
                    format(target, scale, intP, decP);
                }
            } else {
                // 只提供了小数部分
                exposant = -exposant;
                int digits = scale - exposant + 1;
                if (digits < 0) {
                    target.append('0');
                } else if (digits == 0) {
                    long decP = Long.parseLong(intS);
                    format(target, scale, 0L, decP);
                } else if (decLength < digits) {
                    long decP = Long.parseLong(intS) * tenPow(decLength + 1) + Long.parseLong(decS) * 10;
                    format(target, exposant + decLength, 0L, decP);
                } else {
                    long subDecP = Long.parseLong(decS.substring(0, digits));
                    long decP = Long.parseLong(intS) * tenPow(digits) + subDecP;
                    format(target, scale, 0L, decP);
                }
            }
        }
    }

    /**
     * 如果给定源值将被舍入为零，则返回 true
     *
     * @param source    要舍入的源值
     * @param decimals  舍入所用的小数位数（当 abs(source) &ge; 1.0 时使用）
     * @param precision 舍入所用的精度（当 abs(source) &lt; 1.0 时使用）
     * @return 如果源值将被舍入为零，则为 true
     */
    private static boolean isRoundedToZero(double source, int decimals, int precision) {
        // 使用 4.999999999999999 而不是 5，因为在某些情况下 5.0 / 1eN > 5e-N（例如 N = 37、42、45、66 等）
        return source == 0.0 || Math.abs(source) < 4.999999999999999 / tenPowDouble(Math.max(decimals, precision) + 1);
    }

    /**
     * 常用的 10 的幂（以避免 Math.pow(10, n) 的开销）
     */
    private static final long[] POWERS_OF_TEN_LONG = new long[19];
    private static final double[] POWERS_OF_TEN_DOUBLE = new double[30];

    static {
        POWERS_OF_TEN_LONG[0] = 1L;
        for (int i = 1; i < POWERS_OF_TEN_LONG.length; i++) {
            POWERS_OF_TEN_LONG[i] = POWERS_OF_TEN_LONG[i - 1] * 10L;
        }
        for (int i = 0; i < POWERS_OF_TEN_DOUBLE.length; i++) {
            POWERS_OF_TEN_DOUBLE[i] = Double.parseDouble("1e" + i);
        }
    }

    /**
     * 返回 10 的 n 次幂
     *
     * @param n 要获取的 10 的幂次
     * @return 10 的 n 次幂
     */
    public static long tenPow(int n) {
        if (n < 0) throw new AssertionError();
        return n < POWERS_OF_TEN_LONG.length ? POWERS_OF_TEN_LONG[n] : (long) Math.pow(10, n);
    }

    private static double tenPowDouble(int n) {
        assert n >= 0;
        return n < POWERS_OF_TEN_DOUBLE.length ? POWERS_OF_TEN_DOUBLE[n] : Math.pow(10, n);
    }

    /**
     * 辅助方法，用于执行 formatDoublePrecise 中所用的自定义舍入
     *
     * @param target 写入的目标缓冲区
     * @param scale  预期的舍入标度
     * @param intP   源值的整数部分
     * @param decP   源值的小数部分，截断到 scale + 1 位
     */
    private static void format(StringBuilder target, int scale, long intP, long decP) {
        if (decP != 0L) {
            // decP 是源值的小数部分，截断到 scale + 1 位。
            // 自定义舍入：加 5
            decP += 5L;
            decP /= 10L;
            if (decP >= tenPowDouble(scale)) {
                intP++;
                decP -= tenPow(scale);
            }
            if (decP != 0L) {
                // 去除末尾的零
                while (decP % 10L == 0L) {
                    decP = decP / 10L;
                    scale--;
                }
            }
        }
        target.append(intP);
        if (decP != 0L) {
            target.append('.');
            // 当 scale 小于 18 时使用 tenPow 而不是 tenPowDouble，
            // 因为将 decP 转换为 double 可能造成精度损失：
            // 例如当 decP = 9999999999999999L、scale = 17 时，
            while (scale > 0 && (scale > 18 ? decP < tenPowDouble(--scale) : decP < tenPow(--scale))) {
                // 插入前导零
                target.append('0');
            }
            target.append(decP);
        }
    }

    /**
     * 按给定精度对源值进行舍入，
     * 并将舍入后的值写入目标缓冲区
     * <p>
     * 该方法内部使用双精度计算与舍入，
     * 因此结果可能不准确（条件参见 formatDouble 方法）。
     *
     * @param source    要舍入的源值
     * @param decimals  舍入所用的小数位数（当 abs(source) &ge; 1.0 时使用）
     * @param precision 舍入所用的精度（当 abs(source) &lt; 1.0 时使用）
     * @param target    写入的目标缓冲区
     */
    public static void formatDoubleFast(double source, int decimals, int precision, StringBuilder target) {
        if (isRoundedToZero(source, decimals, precision)) {
            // 将始终舍入为 0
            target.append('0');
            return;
        } else if (Double.isNaN(source) || Double.isInfinite(source)) {
            // 无法格式化
            target.append(source);
            return;
        }

        boolean isPositive = source >= 0.0;
        source = Math.abs(source);
        int scale = (source >= 1.0) ? decimals : precision;

        long intPart = (long) Math.floor(source);
        double tenScale = tenPowDouble(scale);
        double fracUnroundedPart = (source - intPart) * tenScale;
        long fracPart = Math.round(fracUnroundedPart);
        if (fracPart >= tenScale) {
            intPart++;
            fracPart = Math.round(fracPart - tenScale);
        }
        if (fracPart != 0L) {
            // 去除末尾的零
            while (fracPart % 10L == 0L) {
                fracPart = fracPart / 10L;
                scale--;
            }
        }

        if (intPart != 0L || fracPart != 0L) {
            // 非零值
            if (!isPositive) {
                // 负值，插入符号
                target.append('-');
            }
            // 追加整数部分
            target.append(intPart);
            if (fracPart != 0L) {
                // 追加小数部分
                target.append('.');
                // 插入前导零
                while (scale > 0 && fracPart < tenPowDouble(--scale)) {
                    target.append('0');
                }
                target.append(fracPart);
            }
        } else {
            target.append('0');
        }
    }

    /**
     * 返回给定值的指数
     *
     * @param value 要获取指数的值
     * @return 该值的指数
     */
    public static int getExponant(double value) {
        // 参见 Double.doubleToRawLongBits 的 javadoc 或 IEEE-754 规范
        // 以了解该算法
        long exp = Double.doubleToRawLongBits(value) & 0x7ff0000000000000L;
        exp = exp >> 52;
        return (int) (exp - 1023L);
    }

    /**
     * 如果认为该舍入会使用 double 的过多位数，
     * 因而不适合快速舍入，则返回 true
     *
     * @param source 要舍入的源值
     * @param scale  舍入所用的标度
     * @return 如果舍入可能使用过多位数，则为 true
     */
    private static boolean tooManyDigitsUsed(double source, int scale) {
        // 若 scale >= 308，则 10^308 ~= Infinity
        double decExp = Math.log10(source);
        return scale >= 308 || decExp + scale >= 14.5;
    }

    /**
     * 如果认为给定源值过于接近给定标度的舍入值，则返回 true。
     *
     * @param source 要舍入的源值
     * @param scale  舍入所用的标度
     * @return 如果源值可能在该标度处被舍入，则为 true
     */
    private static boolean tooCloseToRound(double source, int scale) {
        source = Math.abs(source);
        long intPart = (long) Math.floor(source);
        double fracPart = (source - intPart) * tenPowDouble(scale);
        double decExp = Math.log10(source);
        double range = decExp + scale >= 12 ? .1 : .001;
        double distanceToRound1 = Math.abs(fracPart - Math.floor(fracPart));
        double distanceToRound2 = Math.abs(fracPart - Math.floor(fracPart) - 0.5);
        return distanceToRound1 <= range || distanceToRound2 <= range;
        // .001 范围：完全是任意设定的范围，
        // 使用该值进行 10e7 次随机测试从未失败
        // 可能依赖于 JVM 或体系结构
    }
}
