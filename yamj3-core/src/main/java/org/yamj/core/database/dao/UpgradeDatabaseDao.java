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

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.hibernate.HibernateDao;

@Transactional
@Repository("fixDatabaseDao")
public class UpgradeDatabaseDao extends HibernateDao {

    private boolean existsColumn(String table, String column) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.COLUMNS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND COLUMN_NAME = '").append(column).append("'");
        Object object = currentSession().createSQLQuery(sb.toString()).uniqueResult();
        return (object != null);
    }

    @SuppressWarnings("unused")
    private boolean existsForeignKey(String table, String foreignKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.TABLE_CONSTRAINTS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND CONSTRAINT_TYPE = 'FOREIGN KEY' ");
        sb.append("AND CONSTRAINT_NAME = '").append(foreignKey).append("'");
        Object object = currentSession().createSQLQuery(sb.toString()).uniqueResult();
        return (object != null);
    }

    @SuppressWarnings("unused")
    private boolean existsUniqueIndex(String table, String indexName) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.TABLE_CONSTRAINTS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND CONSTRAINT_TYPE = 'UNIQUE' ");
        sb.append("AND CONSTRAINT_NAME = '").append(indexName).append("'");
        Object object = currentSession().createSQLQuery(sb.toString()).uniqueResult();
        return (object != null);
    }

    /**
     * Issues: #222
     * Date:   18.07.2015
     */
    public void patchTrailers() {
        if (existsColumn("videodata", "trailer_status")) {
            currentSession()
                .createSQLQuery("UPDATE videodata set trailer_status = 'NEW' where trailer_status=''")
                .executeUpdate();
        }

        if (existsColumn("series", "trailer_status")) {
            currentSession()
                .createSQLQuery("UPDATE series set trailer_status = 'NEW' where trailer_status=''")
                .executeUpdate();
        }

        if (existsColumn("trailer", "source_hash")) {
            currentSession()
                .createSQLQuery("UPDATE trailer set hash_code=source_hash")
                .executeUpdate();
            currentSession()
                .createSQLQuery("ALTER TABLE trailer DROP source_hash")
                .executeUpdate();
        }
    }
    
    /**
     * Issues: enhancement
     * Date:   21.07.2015
     */
    public void patchArtworkConfig() {
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='plugin_movie,alternate_movie,tmdb,fanarttv,yahoo' where config_key='yamj3.artwork.scanner.poster.movie.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='plugin_series,alternate_series,tvdb' where config_key='yamj3.artwork.scanner.poster.tvshow.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='plugin_movie,alternate_movie,tmdb' where config_key='yamj3.artwork.scanner.poster.boxset.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='plugin_movie,alternate_movie,tmdb,fanarttv' where config_key='yamj3.artwork.scanner.fanart.movie.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='plugin_series,alternate_series,tvdb' where config_key='yamj3.artwork.scanner.fanart.tvshow.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='plugin_movie,alternate_movie,tmdb' where config_key='yamj3.artwork.scanner.fanart.boxset.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='plugin_series,alternate_series,tvdb' where config_key='yamj3.artwork.scanner.banner.tvshow.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='plugin_movie,alternate_movie,tmdb' where config_key='yamj3.artwork.scanner.banner.boxset.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='plugin_series,alternate_series,tvdb' where config_key='yamj3.artwork.scanner.videoimage.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='plugin_person,alternate_person,tmdb' where config_key='yamj3.artwork.scanner.photo.priorities'")
            .executeUpdate();
    }
    
    /**
     * Issues: #234
     * Date:   24.07.2015
     */
    public void patchLocales() {
        currentSession()
            .createSQLQuery("DELETE FROM configuration where config_key='imdb.id.search.country'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='' where config_key='themoviedb.language'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='' where config_key='themoviedb.country'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='' where config_key='thetvdb.language'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='DE,FR,GB,US' where config_key='yamj3.certification.countries'")
            .executeUpdate();
    }
}
