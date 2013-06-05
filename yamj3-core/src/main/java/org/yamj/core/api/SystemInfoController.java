/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.common.model.YamjInfo;

@Controller
@RequestMapping("/system")
public class SystemInfoController {

    private static final YamjInfo yamjInfo = new YamjInfo(SystemInfoController.class);

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    public String getSystemUp() {
        StringBuilder sb = new StringBuilder("YAMJ v3 is running, uptime ");
        sb.append(yamjInfo.getUptime());
        return sb.toString();
    }

    @RequestMapping(value = "/info", method = RequestMethod.GET)
    @ResponseBody
    public YamjInfo getYamjInfo() {
        return yamjInfo;
    }

}
