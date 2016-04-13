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
package org.yamj.plugin.api.scanner;

import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.plugin.api.YamjPlugin;
import ro.fortsoft.pf4j.PluginWrapper;

public abstract class YamjOnlinePlugin extends YamjPlugin {

    protected PoolingHttpClient poolingHttpClient;
    
    public YamjOnlinePlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void setPoolingHttpClient(PoolingHttpClient poolingHttpClient) {
        this.poolingHttpClient = poolingHttpClient;
    }

}