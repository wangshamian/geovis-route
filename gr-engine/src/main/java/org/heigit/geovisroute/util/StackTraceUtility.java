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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * 用于将异常的堆栈轨迹以 String 形式返回的简单工具。
 */
public final class StackTraceUtility {
    private static final Logger LOGGER = Logger.getLogger(StackTraceUtility.class.getName());

    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

    /**
     * 为堆栈轨迹定义一种以 String 形式表示的自定义格式。
     */
    public static String getCustomStackTrace(Throwable aThrowable) {
        //添加类名以及传给构造函数的任何消息
        final StringBuilder result = new StringBuilder("BOO-BOO: ");
        result.append(aThrowable.toString());
        final String NEW_LINE = System.getProperty("line.separator");
        result.append(NEW_LINE);

        //添加堆栈轨迹的每个元素
        for (StackTraceElement element : aThrowable.getStackTrace()) {
            result.append(element);
            result.append(NEW_LINE);
        }
        return result.toString();
    }

    /**
     * 演示输出。
     */
    public static void main(String... aArguments) {
        final Throwable throwable = new IllegalArgumentException("Blah");
        LOGGER.info(getStackTrace(throwable));
        LOGGER.info(getCustomStackTrace(throwable));
    }
} 