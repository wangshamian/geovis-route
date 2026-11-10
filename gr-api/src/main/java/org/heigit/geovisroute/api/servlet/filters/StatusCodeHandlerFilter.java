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
package org.heigit.geovisroute.api.servlet.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.heigit.geovisroute.api.servlet.requests.StatusCodeCaptureWrapper;

import java.io.IOException;

public class StatusCodeHandlerFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 不做任何处理
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException {
        StatusCodeCaptureWrapper responseWrapper = new StatusCodeCaptureWrapper((HttpServletResponse) response);
        Throwable exception = null;

        try {
            chain.doFilter(request, responseWrapper);
        } catch (ServletException e) {
            exception = e.getRootCause();
        } catch (Throwable e) { // NOSONAR 这是 UnhandledExceptionHandler - 我们需要捕获它
            exception = e;
        }

        if (exception != null) {
            // 如有需要，添加进一步的异常处理
        }

        // 	刷新以阻止 Servlet 容器再添加任何响应头或内容
        response.flushBuffer();
    }

    @Override
    public void destroy() {
        // 不做任何处理
    }
}
