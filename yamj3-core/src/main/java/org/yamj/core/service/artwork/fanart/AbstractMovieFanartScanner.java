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
package org.yamj.core.service.artwork.fanart;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.database.model.IMetadata;

public abstract class AbstractMovieFanartScanner implements IMovieFanartScanner {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMovieFanartScanner.class);
    protected static final int FANART_MAX_RESULTS = PropertyTools.getIntProperty("artwork.scanner.fanart.maxResults", 1);

    @Override
    public List<String> getFanartURLs(IMetadata metadata) {
        String id = metadata.getSourceDbId(getScannerName());
        if (StringUtils.isBlank(id)) {
            if (StringUtils.isBlank(metadata.getTitleOriginal())) {
                id = getId(metadata.getTitle(), metadata.getYear());
            } else {
                id = getId(metadata.getTitleOriginal(), metadata.getYear());
                if (StringUtils.isBlank(id) && !StringUtils.equals(metadata.getTitleOriginal(), metadata.getTitle())) {
                    // didn't find the movie with the original title, try the normal title if it's different
                    id = this.getId(metadata.getTitle(), metadata.getYear());
                }
            }
            if (StringUtils.isNotBlank(id)) {
                LOG.debug("{} : ID found setting it to '{}'", getScannerName(), id);
                metadata.setSourceDbId(getScannerName(), id);
            }
        }

        if (!(StringUtils.isBlank(id) || "-1".equals(id) || "0".equals(id))) {
            return getFanartURLs(id);
        }

        return null;
    }
}
