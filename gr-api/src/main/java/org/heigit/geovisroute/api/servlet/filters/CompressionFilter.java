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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class CompressionFilter implements Filter {
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (req instanceof HttpServletRequest request) {
            HttpServletResponse response = (HttpServletResponse) res;
            String acceptEncoding = request.getHeader("accept-encoding");

            if (acceptEncoding != null) {
                if (acceptEncoding.contains(ContentEncodingType.GZIP)) {
                    GZIPResponseWrapper wrappedResponse = new GZIPResponseWrapper(response);
                    chain.doFilter(req, wrappedResponse);
                    wrappedResponse.finishResponse();
                    return;
                } else if (acceptEncoding.contains(ContentEncodingType.DEFLATE)) {
                    // 未实现
                }
            }
            chain.doFilter(req, res);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // 无需操作
    }

    @Override
    public void destroy() {
        // 无需操作
    }
}
