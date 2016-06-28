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
package org.yamj.core.api.model.builder;

import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.dto.ApiArtworkDTO;
import org.yamj.core.api.model.dto.ApiVideoDTO;

public final class KeyMaker {

    private KeyMaker() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String makeKey(ApiArtworkDTO dto) {
        return makeKey(dto.getSource(), dto.getId());
    }

    public static String makeKey(ApiVideoDTO dto) {
        return makeKey(dto.getVideoType(), dto.getId());
    }

    public static String makeKey(MetaDataType videoType, long id) {
        StringBuilder key = new StringBuilder();
        key.append(videoType.toString());
        key.append("-");
        key.append(id);
        return key.toString();
    }
}
