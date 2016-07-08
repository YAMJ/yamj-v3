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
package org.yamj.core.service.mediaimport;

import java.util.List;
import org.apache.commons.lang.math.NumberUtils;

class ImageImportTools {

    private static final String VIDEOIMAGE = ".videoimage";
    
    protected static boolean isGenericImage(final String name, final List<String> tokens) {
        return tokens.contains(name);
    }

    protected static boolean endsWithToken(final String name, final List<String> tokens) {
        for (String token : tokens) {
            if (name.endsWith(".".concat(token)) || name.endsWith("-".concat(token))) {
                return true;
            }
        }
        return false;
    }

    protected static boolean isSpecialImage(final String name, final String special, final List<String> tokens) {
        for (String token : tokens) {
            if (name.equals(special.concat(".").concat(token)) || name.equals(special.concat("-").concat(token))) {
                return true;
            }
        }
        return false;
    }
        
    protected static String stripToken(final String name, final List<String> tokens) {
        for (String token : tokens) {
            if (name.endsWith(".".concat(token)) || name.endsWith("-".concat(token))) {
                return name.substring(0, name.length() - token.length() -1);
            }
        }
        return name;
    }
    
    protected static String getBoxedSetName(final String stripped) {
        String boxedSetName = stripped.substring(4);
        int index = boxedSetName.lastIndexOf("_");
        if (index > -1) {
            boxedSetName = boxedSetName.substring(0, index);
        }
        return boxedSetName;
    }

    protected static final boolean isVideoImage(final String name) {
        return name.contains(VIDEOIMAGE);
    }
    
    protected static final int getVideoImagePart(final String name) {
        if (name.endsWith(VIDEOIMAGE)) {
            // no number so video image is for first episode
            return 1;
        }
        int lastIndex = name.lastIndexOf("_");
        if (lastIndex < 0) {
            // assume that video image is for first part
            return 1;
        }
        return Math.max(1, NumberUtils.toInt(name.substring(lastIndex+1)));
    }

    protected static final String getBaseNameFromVideoImage(final String name) {
        return name.substring(0, name.indexOf(VIDEOIMAGE));
    }

}
