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
package org.heigit.geovisroute.common;

import java.net.HttpURLConnection;

public class StatusCode {
    /**
     * 状态码（200），表示请求正常成功。
     */
    public static final int OK = HttpURLConnection.HTTP_OK;
    /**
     * 状态码（400），表示客户端发送的请求存在语法错误。
     */
    public static final int BAD_REQUEST = HttpURLConnection.HTTP_BAD_REQUEST;
    /**
     * 状态码（405），表示 <code><em>Request-Line</em></code> 中指定的方法
     * 不被 <code><em>Request-URI</em></code> 所标识的资源允许。
     */
    public static final int METHOD_NOT_ALLOWED = HttpURLConnection.HTTP_BAD_METHOD;

    /**
     * 状态码（500），表示 HTTP 服务器内部发生错误，
     * 导致其无法完成该请求。
     */
    public static final int INTERNAL_SERVER_ERROR = HttpURLConnection.HTTP_INTERNAL_ERROR;

    /**
     * 状态码（501），表示 HTTP 服务器不支持
     * 完成该请求所需的功能。
     */
    public static final int NOT_IMPLEMENTED = HttpURLConnection.HTTP_NOT_IMPLEMENTED;

    /**
     * 状态码（503），表示端点暂时过载，
     * 无法处理该请求。
     */
    public static final int SERVICE_UNAVAILABLE = HttpURLConnection.HTTP_UNAVAILABLE;

    /**
     * 状态码（404），表示请求已被处理，
     * 但未找到任何信息（例如地理编码请求未找到对应地址）
     */
    public static final int NOT_FOUND = HttpURLConnection.HTTP_NOT_FOUND;

    private StatusCode() {
    }
}
