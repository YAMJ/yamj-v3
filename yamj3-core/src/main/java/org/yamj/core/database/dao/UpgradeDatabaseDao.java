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

import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.config.LocaleService;
import org.yamj.core.hibernate.HibernateDao;
import org.yamj.core.tools.Constants;

@Transactional
@Repository("fixDatabaseDao")
public class UpgradeDatabaseDao extends HibernateDao {

    @Autowired
    private LocaleService localeService;

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
    public void patchLocaleConfig() {
        currentSession()
            .createSQLQuery("DELETE FROM configuration where config_key='imdb.id.search.country'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("DELETE FROM configuration where config_key='imdb.aka.preferred.country'")
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

    class CertEntry {
        public String country;
        public String cert;
        
        public CertEntry(String country, String cert)  {
            this.country = country;
            this.cert = cert;
        }
    }

    /**
     * Issues: #234
     * Date:   25.07.2015
     */
    public void patchCertifications() {
        if (!existsColumn("certification", "country")) return;
        
        // drop unique index
        if (existsUniqueIndex("certification", "UIX_CERTIFICATION_NATURALID")) {
            currentSession()
                .createSQLQuery("ALTER TABLE certification DROP index UIX_CERTIFICATION_NATURALID")
                .executeUpdate();
        }
        
        // retrieve certification
        Map<Long, CertEntry> certifications = new HashMap<>();
        List<Object[]> objects = currentSession().createSQLQuery("select id,country,certificate from certification").list();
        for (Object[] object : objects) {
            Long id = Long.valueOf(object[0].toString());
            String country = object[1].toString();
            String cert = object[2].toString();
            certifications.put(id, new CertEntry(country, cert));
        }
        
        // modify certifications
        Set<Long> ids = new HashSet<>(certifications.keySet());
        Set<Long> deletions = new HashSet<>();
        Map<Long,Long> moves = new HashMap<>();
        for (Long id : ids) {
            CertEntry ce = certifications.get(id);
            
            String code = localeService.findCountryCode(ce.country);
            if (StringUtils.isBlank(code)) {
                deletions.add(id);
                certifications.remove(id);
            } else {
                // check if same combination exists
                ce.country = code;
                certifications.put(id, ce);
                
                Map<Long,CertEntry> others = new HashMap<>(certifications);
                for (Entry<Long,CertEntry> other : others.entrySet()) {
                    if (!id.equals(other.getKey()) &&
                        StringUtils.equalsIgnoreCase(ce.country, other.getValue().country) &&
                        StringUtils.equalsIgnoreCase(ce.cert, other.getValue().cert))
                    {
                        moves.put(other.getKey(), id);
                        certifications.remove(other.getKey());
                    }
                }
            }
        }
        
        // move to left certifications
        for (Entry<Long,Long> move : moves.entrySet()) {
            currentSession()
            .createSQLQuery("UPDATE videodata_certifications set cert_id=:newId where cert_id=:oldId")
            .setLong("oldId", move.getKey())
            .setLong("newId", move.getValue())
            .executeUpdate();
            
            currentSession()
            .createSQLQuery("UPDATE series_certifications set cert_id=:newId where cert_id=:oldId")
            .setLong("oldId", move.getKey())
            .setLong("newId", move.getValue())
            .executeUpdate();
            
            currentSession()
            .createSQLQuery("DELETE FROM certification where id=:oldId")
            .setLong("oldId", move.getKey())
            .executeUpdate();
        }

        // delete certifications in database
        if (CollectionUtils.isNotEmpty(deletions)) {
            currentSession()
            .createSQLQuery("DELETE FROM videodata_certifications where cert_id in :ids")
            .setParameterList("ids", deletions)
            .executeUpdate();

            currentSession()
            .createSQLQuery("DELETE FROM series_certifications where cert_id in :ids")
            .setParameterList("ids", deletions)
            .executeUpdate();

            currentSession()
            .createSQLQuery("DELETE FROM certification where id in :ids")
            .setParameterList("ids", deletions)
            .executeUpdate();
        }

        // update certification codes
        for (Entry<Long,CertEntry> update : certifications.entrySet()) {
            currentSession()
            .createSQLQuery("UPDATE certification set country_code=:code where id=:id")
            .setLong("id", update.getKey())
            .setString("code", update.getValue().country)
            .executeUpdate();
        }

        // create new index
        currentSession()
        .createSQLQuery("CREATE UNIQUE INDEX UIX_CERTIFICATION_NATURALID on certification(country_code,certificate)")
        .executeUpdate();

        // drop old column
        currentSession()
        .createSQLQuery("ALTER TABLE certification DROP country")
        .executeUpdate();
    }

    class CountryEntry {
        public String name;
        public String targetXml;
        public String targetApi;
        
        public CountryEntry(String name, String targetXml, String targetApi)  {
            this.name = name;
            this.targetXml = targetXml;
            this.targetApi = targetApi;
        }
    }

    /**
     * Issues: #234
     * Date:   26.07.2015
     */
    public void patchCountries() {
        if (!existsColumn("country", "name")) return;
        
        // drop unique index
        if (existsUniqueIndex("country", "UIX_COUNTRY_NATURALID")) {
            currentSession()
                .createSQLQuery("ALTER TABLE country DROP index UIX_COUNTRY_NATURALID")
                .executeUpdate();
        }
        
        // retrieve countries
        Map<Long, CountryEntry> countries = new HashMap<>();
        List<Object[]> objects = currentSession().createSQLQuery("select id,name,target_xml,target_api from country").list();
        for (Object[] object : objects) {
            Long id = Long.valueOf(object[0].toString());
            String country = object[1].toString();
            String targetXml = (object[2] == null ? null : object[2].toString());
            String targetApi = (object[3] == null ? null : object[3].toString());
            countries.put(id, new CountryEntry(country, targetXml, targetApi));
        }
        
        // modify countries
        Set<Long> ids = new HashSet<>(countries.keySet());
        Set<Long> deletions = new HashSet<>();
        Map<Long,Long> moves = new HashMap<>();
        for (Long id : ids) {
            CountryEntry ce = countries.get(id);
            
            String code = localeService.findCountryCode(ce.targetApi);
            if (code == null) {
                code = localeService.findCountryCode(ce.targetXml);
            }
            if (code == null) {
                code = localeService.findCountryCode(ce.name);
            }
            
            if (StringUtils.isBlank(code)) {
                deletions.add(id);
                countries.remove(id);
            } else {
                // check if same combination exists
                ce.name = code;
                countries.put(id, ce);
                
                Map<Long,CountryEntry> others = new HashMap<>(countries);
                for (Entry<Long,CountryEntry> other : others.entrySet()) {
                    if (!id.equals(other.getKey()) &&
                        StringUtils.equalsIgnoreCase(ce.name, other.getValue().name))
                    {
                        moves.put(other.getKey(), id);
                        countries.remove(other.getKey());
                    }
                }
            }
        }
        
        // move to left countries
        for (Entry<Long,Long> move : moves.entrySet()) {
            currentSession()
            .createSQLQuery("UPDATE videodata_countries set country_id=:newId where country_id=:oldId")
            .setLong("oldId", move.getKey())
            .setLong("newId", move.getValue())
            .executeUpdate();
            
            currentSession()
            .createSQLQuery("UPDATE series_countries set country_id=:newId where country_id=:oldId")
            .setLong("oldId", move.getKey())
            .setLong("newId", move.getValue())
            .executeUpdate();
            
            currentSession()
            .createSQLQuery("DELETE FROM country where id=:oldId")
            .setLong("oldId", move.getKey())
            .executeUpdate();
        }

        // delete countries in database
        if (CollectionUtils.isNotEmpty(deletions)) {
            currentSession()
            .createSQLQuery("DELETE FROM videodata_countries where country_id in :ids")
            .setParameterList("ids", deletions)
            .executeUpdate();

            currentSession()
            .createSQLQuery("DELETE FROM series_countries where country_id in :ids")
            .setParameterList("ids", deletions)
            .executeUpdate();

            currentSession()
            .createSQLQuery("DELETE FROM country where id in :ids")
            .setParameterList("ids", deletions)
            .executeUpdate();
        }
        
        // update country codes
        for (Entry<Long,CountryEntry> update : countries.entrySet()) {
            currentSession()
            .createSQLQuery("UPDATE country set country_code=:code where id=:id")
            .setLong("id", update.getKey())
            .setString("code", update.getValue().name)
            .executeUpdate();
        }

        // create new index
        currentSession()
        .createSQLQuery("CREATE UNIQUE INDEX UIX_COUNTRY_NATURALID on country(country_code)")
        .executeUpdate();

        // drop old columns
        currentSession()
        .createSQLQuery("ALTER TABLE country DROP name")
        .executeUpdate();
        currentSession()
        .createSQLQuery("ALTER TABLE country DROP target_xml")
        .executeUpdate();
        currentSession()
        .createSQLQuery("ALTER TABLE country DROP target_api")
        .executeUpdate();
    }

    /**
     * Issues: #234
     * Date:   27.07.2015
     */
    public void patchReleaseCountryFilmo() {
        if (!existsColumn("participation", "release_state")) return;
        
        // retrieve countries
        Map<Long, String> filmo = new HashMap<>();
        List<Object[]> objects = currentSession().createSQLQuery("select id,release_state from participation where release_state is not null").list();
        for (Object[] object : objects) {
            Long id = Long.valueOf(object[0].toString());
            String country = object[1].toString();
            filmo.put(id, country);
        }
        
        // modify country codes
        Set<Long> ids = new HashSet<>(filmo.keySet());
        for (Long id : ids) {
            String code = localeService.findCountryCode(filmo.get(id));
            if (code == null) {
                filmo.remove(id);
            } else {
                filmo.put(id, code);
            }
        }
        
        // update country codes
        for (Entry<Long,String> update : filmo.entrySet()) {
            currentSession()
            .createSQLQuery("UPDATE participation set release_country_code=:code where id=:id")
            .setLong("id", update.getKey())
            .setString("code", update.getValue())
            .executeUpdate();
        }

        // drop old columns
        currentSession()
        .createSQLQuery("ALTER TABLE participation DROP release_state")
        .executeUpdate();
    }

    /**
     * Issues: #234
     * Date:   28.07.2015
     */
    public void patchLanguageAudioCodes() {
        if (!existsColumn("audio_codec", "language")) return;

        // retrieve codecs
        Map<Long, String> codecs = new HashMap<>();
        List<Object[]> objects = currentSession().createSQLQuery("select id,language from audio_codec").list();
        for (Object[] object : objects) {
            Long id = Long.valueOf(object[0].toString());
            String lang = object[1].toString();
            codecs.put(id, lang);
        }
        
        // modify country codes
        Set<Long> ids = new HashSet<>(codecs.keySet());
        for (Long id : ids) {
            String lang = codecs.get(id);
            if (Constants.UNDEFINED.equalsIgnoreCase(lang)) {
                codecs.put(id, Constants.LANGUAGE_UNTERTERMINED);
            } else {
                String code = localeService.findLanguageCode(lang);
                if (code == null) code = Constants.LANGUAGE_UNTERTERMINED;
                codecs.put(id, code);
            }
        }
        
        // update language codes
        for (Entry<Long,String> update : codecs.entrySet()) {
            currentSession()
            .createSQLQuery("UPDATE audio_codec set language_code=:code where id=:id")
            .setLong("id", update.getKey())
            .setString("code", update.getValue())
            .executeUpdate();
        }

        // drop old columns
        currentSession()
        .createSQLQuery("ALTER TABLE audio_codec DROP language")
        .executeUpdate();
    }

    /**
     * Issues: #234
     * Date:   28.07.2015
     */
    public void patchLanguageSubtitles() {
        if (!existsColumn("subtitle", "language")) return;

        // retrieve subtitles
        Map<Long, String> subtitles = new HashMap<>();
        List<Object[]> objects = currentSession().createSQLQuery("select id,language from subtitle").list();
        for (Object[] object : objects) {
            Long id = Long.valueOf(object[0].toString());
            String lang = object[1].toString();
            subtitles.put(id, lang);
        }
        
        // modify subtitles
        Set<Long> ids = new HashSet<>(subtitles.keySet());
        for (Long id : ids) {
            String lang = subtitles.get(id);
            if (Constants.UNDEFINED.equalsIgnoreCase(lang)) {
                subtitles.put(id, Constants.LANGUAGE_UNTERTERMINED);
            } else {
                String code = localeService.findLanguageCode(lang);
                if (code == null) code = Constants.LANGUAGE_UNTERTERMINED;
                subtitles.put(id, code);
            }
        }
        
        // update language codes
        for (Entry<Long,String> update : subtitles.entrySet()) {
            currentSession()
            .createSQLQuery("UPDATE subtitle set language_code=:code where id=:id")
            .setLong("id", update.getKey())
            .setString("code", update.getValue())
            .executeUpdate();
        }

        // drop old columns
        currentSession()
        .createSQLQuery("ALTER TABLE subtitle DROP language")
        .executeUpdate();
    }
}
