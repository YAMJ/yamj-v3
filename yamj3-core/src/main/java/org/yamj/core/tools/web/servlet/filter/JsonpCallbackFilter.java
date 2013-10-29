/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.tools.web.servlet.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JsonpCallbackFilter implements Filter {

    @Override
    public void init(FilterConfig fConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        Map<String, String[]> parms = httpRequest.getParameterMap();

        if (parms.containsKey("callback")) {
            OutputStream out = httpResponse.getOutputStream();

            GenericResponseWrapper wrapper = new GenericResponseWrapper(httpResponse);

            chain.doFilter(request, wrapper);

            out.write((parms.get("callback")[0] + "(").getBytes());
            out.write(wrapper.getData());
            out.write(");".getBytes());

            wrapper.setContentType("text/javascript;charset=UTF-8");

            out.close();
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }
}