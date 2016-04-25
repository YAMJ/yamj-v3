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
package org.yamj.plugin.api.service.mock;

import org.yamj.plugin.api.metadata.MovieScanner;
import org.yamj.plugin.api.metadata.PersonScanner;
import org.yamj.plugin.api.metadata.SeriesScanner;
import org.yamj.plugin.api.service.PluginMetadataService;

public class PluginMetadataServiceMock implements PluginMetadataService {

    @Override
    public MovieScanner getMovieScanner(String source) {
        return null;
    }

    @Override
    public SeriesScanner getSeriesScanner(String source) {
        return null;
    }

    @Override
    public PersonScanner getPersonScanner(String source) {
        return null;
    }
}
