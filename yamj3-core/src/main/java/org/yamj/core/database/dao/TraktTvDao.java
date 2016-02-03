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
package org.yamj.core.database.dao;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.type.LongType;
import org.springframework.stereotype.Repository;
import org.yamj.api.trakttv.model.Ids;
import org.yamj.core.api.model.builder.SqlScalars;
import org.yamj.core.hibernate.HibernateDao;
import org.yamj.core.service.metadata.online.ImdbScanner;
import org.yamj.core.service.metadata.online.TheMovieDbScanner;
import org.yamj.core.service.metadata.online.TraktTvScanner;

@Repository("traktTvDao")
public class TraktTvDao extends HibernateDao {
    
    public List<Long> findMovieByIDs(Ids ids) {
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addParameter("trakTvScanner", TraktTvScanner.SCANNER_ID);
        sqlScalars.addParameter("trakTvId", ids.trakt().toString());

        sqlScalars.addToSql("SELECT distinct vid.videodata_id FROM videodata_ids vid");
        sqlScalars.addToSql("WHERE (vid.sourcedb=:trakTvScanner AND vid.sourcedb_id=:trakTvId)");
        if (StringUtils.isNotBlank(ids.imdb())) {
            sqlScalars.addToSql("OR (vid.sourcedb=:imdbScanner AND vid.sourcedb_id=:imdbId)");
            sqlScalars.addParameter("imdbScanner", ImdbScanner.SCANNER_ID);
            sqlScalars.addParameter("imdbId", ids.imdb());
        }
        if (ids.tmdb() != null) {
            sqlScalars.addToSql("OR (vid.sourcedb=:tmdbScanner AND vid.sourcedb_id=:tmdbId)");
            sqlScalars.addParameter("tmdbScanner", TheMovieDbScanner.SCANNER_ID);
            sqlScalars.addParameter("tmdbId", ids.tmdb().toString());
        }

        sqlScalars.addScalar("videodata_id", LongType.INSTANCE);
        return executeQueryWithTransform(Long.class, sqlScalars, null);
    }
}
