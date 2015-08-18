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

import java.math.BigInteger;
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
import org.yamj.core.tools.MetadataTools;

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

    private boolean existsIndex(String table, String indexName) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.STATISTICS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND INDEX_NAME = '").append(indexName).append("'");
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
        
        currentSession()
            .createSQLQuery("ALTER TABLE trailer MODIFY COLUMN url VARCHAR(1000)")
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

    /**
     * Issues: #234, #237, enhancements
     * Date:   07.08.2015
     */
    public void patchConfiguration() {
        // enhancements
        currentSession()
            .createSQLQuery("DELETE FROM configuration where config_key='imdb.skip.faceless'")
            .executeUpdate();
        
        // #234
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

        // #237
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='movie_scanner,tmdb,fanarttv,yahoo' where config_key='yamj3.artwork.scanner.poster.movie.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='series_scanner,tvdb' where config_key='yamj3.artwork.scanner.poster.tvshow.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='movie_scanner,tmdb' where config_key='yamj3.artwork.scanner.poster.boxset.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='movie_scanner,tmdb,fanarttv' where config_key='yamj3.artwork.scanner.fanart.movie.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='series_scanner,tvdb' where config_key='yamj3.artwork.scanner.fanart.tvshow.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='movie_scanner,tmdb' where config_key='yamj3.artwork.scanner.fanart.boxset.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='series_scanner,tvdb' where config_key='yamj3.artwork.scanner.banner.tvshow.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='movie_scanner,tmdb' where config_key='yamj3.artwork.scanner.banner.boxset.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='series_scanner,tvdb' where config_key='yamj3.artwork.scanner.videoimage.priorities'")
            .executeUpdate();
        currentSession()
            .createSQLQuery("UPDATE configuration set config_value='person_scanner,tmdb' where config_key='yamj3.artwork.scanner.photo.priorities'")
            .executeUpdate();
    }

    /**
     * Issues: enhancement
     * Date:   10.08.2015
     */
    public void patchArtworkLocated() {
        if (existsIndex("artwork_located", "IX_ARTWORKLOCATED_DOWNLOAD")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_located DROP INDEX IX_ARTWORKLOCATED_DOWNLOAD")
            .executeUpdate();
        }

        if (existsUniqueIndex("artwork_located", "UIX_ARTWORKLOCATED_NATURALID")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_located DROP FOREIGN KEY FK_ARTWORKLOCATED_ARTWORK")
            .executeUpdate();

            currentSession()
            .createSQLQuery("ALTER TABLE artwork_located DROP INDEX UIX_ARTWORKLOCATED_NATURALID")
            .executeUpdate();

            currentSession()
            .createSQLQuery("ALTER TABLE artwork_located ADD CONSTRAINT FK_ARTWORKLOCATED_ARTWORK FOREIGN KEY (artwork_id) REFERENCES artwork (id)")
            .executeUpdate();
        }

        currentSession()
        .createSQLQuery("UPDATE artwork_located SET source='file' WHERE source is null and stagefile_id is not null")
        .executeUpdate();
        
        currentSession()
        .createSQLQuery("ALTER TABLE artwork_located MODIFY COLUMN source VARCHAR(50) NOT NULL")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE artwork_located MODIFY COLUMN hash_code VARCHAR(100) NOT NULL")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE artwork_located ADD UNIQUE INDEX UIX_ARTWORKLOCATED_NATURALID(artwork_id,stagefile_id,source,hash_code)")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE artwork_located MODIFY COLUMN url VARCHAR(1000)")
        .executeUpdate();
        
        currentSession()
        .createSQLQuery("UPDATE artwork_located set image_type='JPG' where image_type=''")
        .executeUpdate();

        if (!existsColumn("artwork_located", "language")) return;

        currentSession()
        .createSQLQuery("UPDATE artwork_located set language_code=language where language is not null")
        .executeUpdate();
        
        currentSession()
        .createSQLQuery("ALTER TABLE artwork_located DROP language")
        .executeUpdate();
    }

    /**
     * Issues: enhancement
     * Date:   10.08.2015
     */
    public void patchBoxedSetIdentifier() {
        // retrieve boxed sets
        Map<Long, String> boxedSets = new HashMap<>();
        List<Object[]> objects = currentSession().createSQLQuery("select id,name from boxed_set where identifier=''").list();
        for (Object[] object : objects) {
            Long id = Long.valueOf(object[0].toString());
            String name = object[1].toString();
            boxedSets.put(id, name);
        }

        if (boxedSets.isEmpty()) {
            // nothing to do anymore
            return;
        }
        
        // drop unique index
        if (existsUniqueIndex("boxed_set", "UIX_BOXEDSET_NATURALID")) {
            currentSession()
            .createSQLQuery("ALTER TABLE boxed_set DROP index UIX_BOXEDSET_NATURALID")
            .executeUpdate();
        }

        // update language codes
        for (Entry<Long,String> update : boxedSets.entrySet()) {
            String identifier = MetadataTools.cleanIdentifier(update.getValue());
            
            currentSession()
            .createSQLQuery("UPDATE boxed_set set identifier=:identifier where id=:id")
            .setLong("id", update.getKey())
            .setString(IDENTIFIER, identifier)
            .executeUpdate();
        }

        // create new index
        currentSession()
        .createSQLQuery("CREATE UNIQUE INDEX UIX_BOXEDSET_NATURALID on boxed_set(identifier)")
        .executeUpdate();
    }
    
    /**
     * Issues: database schema
     * Date:   10.08.2015
     */
    public void patchStudio() {
        if (existsUniqueIndex("studio", "UK_STUDIO_NATURALID")) {
            currentSession()
            .createSQLQuery("ALTER TABLE studio DROP index UK_STUDIO_NATURALID")
            .executeUpdate();
        }
    }

    /**
     * Issues: #193
     * Date:   10.08.2015
     */
    public void patchMediaFileWatched() {
        if (!existsColumn("mediafile", "watched_file")) return;
        
        // retrieve media file with watched file
        List<BigInteger> ids = currentSession()
            .createSQLQuery("select id from mediafile where watched_file=:watched")
            .setBoolean("watched", Boolean.TRUE)
            .list();

        for (BigInteger id : ids) {
            Date watchedFileDate = (Date)currentSession()
                .createSQLQuery("SELECT max(sf.file_date) FROM stage_file sf WHERE sf.mediafile_id =:id and sf.status != 'DELETED'")
                .setLong("id", id.longValue())
                .uniqueResult();
            
            if (watchedFileDate != null) {
                currentSession()
                .createSQLQuery("UPDATE mediafile set watched_file_date=:watchedFileDate where id=:id")
                .setLong("id", id.longValue())
                .setTimestamp("watchedFileDate", watchedFileDate)
                .executeUpdate();
            }
        }
        
        // update watched api date
        currentSession()
        .createSQLQuery("UPDATE mediafile set watched_api_date=update_timestamp where watched_api=:watched")
        .setBoolean("watched", Boolean.TRUE)
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE mediafile DROP watched_file")
        .executeUpdate();
    }

    /**
     * Issues: enhancement
     * Date:   15.08.2015
     */
    public void patchDatabaseLongVarchars() {
        currentSession()
        .createSQLQuery("ALTER TABLE trailer MODIFY COLUMN url VARCHAR(2000)")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE videodata MODIFY COLUMN tagline VARCHAR(2000)")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE videodata MODIFY COLUMN quote VARCHAR(2000)")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE stage_file MODIFY COLUMN full_path VARCHAR(1000)")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE player_path MODIFY COLUMN source_path VARCHAR(1000)")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE player_path MODIFY COLUMN target_path VARCHAR(1000)")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE artwork_generated MODIFY COLUMN cache_filename VARCHAR(500)")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE library MODIFY COLUMN player_path VARCHAR(1000)")
        .executeUpdate();
        
        currentSession()
        .createSQLQuery("ALTER TABLE library MODIFY COLUMN base_directory VARCHAR(1000)")
        .executeUpdate();
    }
}
