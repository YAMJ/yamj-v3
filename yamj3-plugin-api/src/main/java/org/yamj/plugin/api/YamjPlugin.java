/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package org.yamj.plugin.api;

import org.yamj.plugin.api.common.PluginConfigService;

import org.yamj.api.common.http.CommonHttpClient;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;

public abstract class YamjPlugin extends Plugin {

    protected PluginConfigService configService;
    protected CommonHttpClient httpClient;
    
    public YamjPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    public final void setConfigService(PluginConfigService configService) {
        this.configService = configService;
    }

    public final void setHttpClient(CommonHttpClient httpClient) {
        this.httpClient = httpClient;
    }
}