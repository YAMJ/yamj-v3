package com.yamj.core.service.plugin;

import com.omertron.thetvdbapi.TheTVDBApi;
import com.omertron.thetvdbapi.model.Actor;
import com.omertron.thetvdbapi.model.Episode;
import com.yamj.common.tools.PropertyTools;
import com.yamj.common.type.StatusType;
import com.yamj.core.database.model.Season;
import com.yamj.core.database.model.Series;
import com.yamj.core.database.model.VideoData;
import com.yamj.core.database.model.dto.CreditDTO;
import com.yamj.core.database.model.type.JobType;
import com.yamj.core.tools.OverrideTools;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("tvdbScanner")
public class TheTVDbScanner implements ISeriesScanner, InitializingBean {

    public static final String TVDB_SCANNER_ID = "tvdb";
    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbScanner.class);
    private static final String DEFAULT_LANGUAGE = PropertyTools.getProperty("thetvdb.language", "en");
    private static final int YEAR_MIN = 1900;
    private static final int YEAR_MAX = 2050;

    @Autowired
    private PluginDatabaseService pluginDatabaseService;
    @Autowired
    private TheTVDBApi tvdbApi;

    @Override
    public String getScannerName() {
        return TVDB_SCANNER_ID;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // register this scanner
        pluginDatabaseService.registerSeriesScanner(this);
    }

    @Override
    public String getSeriesId(Series series) {
        String id = series.getSourcedbId(TVDB_SCANNER_ID);

        if (StringUtils.isBlank(id)) {
            return getSeriesId(series.getTitle(), series.getStartYear());
        }

        return id;
    }

    @Override
    public String getSeriesId(String title, int year) {
        String id = "";
        if (StringUtils.isNotBlank(title)) {
            List<com.omertron.thetvdbapi.model.Series> seriesList = tvdbApi.searchSeries(title, DEFAULT_LANGUAGE);
            if (seriesList != null) {
                com.omertron.thetvdbapi.model.Series series = null;
                for (com.omertron.thetvdbapi.model.Series s : seriesList) {
                    if (s.getFirstAired() != null && !s.getFirstAired().isEmpty() && (year > YEAR_MIN && year < YEAR_MAX)) {
                        DateTime firstAired = DateTime.parse(s.getFirstAired());
                        firstAired.getYear();
                        if (firstAired.getYear() == year) {
                            series = s;
                            break;
                        }
                    } else {
                        series = s;
                        break;
                    }
                }

                if (series != null) {
                    id = series.getId();
                }
            }
        }
        return id;
    }

    @Override
    public ScanResult scan(Series series) {
        String id = getSeriesId(series);

        if (StringUtils.isNotBlank(id)) {
            com.omertron.thetvdbapi.model.Series tvdbSeries = tvdbApi.getSeries(id, DEFAULT_LANGUAGE);

            series.setSourcedbId(TVDB_SCANNER_ID, tvdbSeries.getId());
            series.setSourcedbId(ImdbScanner.IMDB_SCANNER_ID, tvdbSeries.getImdbId());

            if (OverrideTools.checkOverwriteTitle(series, TVDB_SCANNER_ID)) {
                series.setTitle(tvdbSeries.getSeriesName(), TVDB_SCANNER_ID);
            }

            if (OverrideTools.checkOverwritePlot(series, TVDB_SCANNER_ID)) {
                series.setPlot(tvdbSeries.getOverview(), TVDB_SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwriteOutline(series, TVDB_SCANNER_ID)) {
                series.setOutline(tvdbSeries.getOverview(), TVDB_SCANNER_ID);
            }

            // TODO more values
            
            if (StringUtils.isNumeric(tvdbSeries.getRating())) {
                try {
                    series.addRating(TVDB_SCANNER_ID, (int) (Float.parseFloat(tvdbSeries.getRating()) * 10));
                } catch (NumberFormatException nfe) {
                    LOG.warn("Failed to convert TVDB rating '{}' to an integer, error: {}", tvdbSeries.getRating(), nfe.getMessage());
                }
            }

            String faDate = tvdbSeries.getFirstAired();
            if (StringUtils.isNotBlank(faDate)) {
                if (faDate.length() >= 4) {
                    series.setStartYear(Integer.parseInt(faDate.substring(0, 4)));
                }
            }

            // CAST & CREW
            
            List<CreditDTO> actors = new ArrayList<CreditDTO>();
            for (Actor actor : tvdbApi.getActors(id)) {
                actors.add(new CreditDTO(JobType.ACTOR, actor.getName(), actor.getRole()));
            }

            // SCAN SEASONS
            
            this.scanSeasons(series, tvdbSeries, actors);

            return ScanResult.OK;
        } else {
            return ScanResult.MISSING_ID;
        }

    }
    
    private void scanSeasons(Series series, com.omertron.thetvdbapi.model.Series tvdbSeries, List<CreditDTO> actors) {
        
        for (Season season : series.getSeasons()) {
            
            // update season values if not done before
            if (!StatusType.DONE.equals(season.getStatus())) {
                // use values from series
                if (OverrideTools.checkOverwriteTitle(season, TVDB_SCANNER_ID)) {
                    season.setTitle(tvdbSeries.getSeriesName(), TVDB_SCANNER_ID);
                }

                if (OverrideTools.checkOverwritePlot(season, TVDB_SCANNER_ID)) {
                    season.setPlot(tvdbSeries.getOverview(), TVDB_SCANNER_ID);
                }
                
                if (OverrideTools.checkOverwriteOutline(season, TVDB_SCANNER_ID)) {
                    season.setOutline(tvdbSeries.getOverview(), TVDB_SCANNER_ID);
                }

                // TODO common usable format
                season.setFirstAired(tvdbSeries.getFirstAired());

                // set status of season in process to allow alternate scan
                season.setStatus(StatusType.PROCESSED);
            }

            // scan episodes
            this.scanEpisodes(season, actors);
        }
    }
    
    private void scanEpisodes(Season season, List<CreditDTO> actors) {
        if (CollectionUtils.isEmpty(season.getVideoDatas())) {
            return;
        }

        String seriesId = season.getSeries().getSourcedbId(TVDB_SCANNER_ID);
        List<Episode> episodeList = tvdbApi.getSeasonEpisodes(seriesId, season.getSeason(), DEFAULT_LANGUAGE);
        
        for (VideoData videoData : season.getVideoDatas()) {

            // update episode values if not done before
            if (!StatusType.DONE.equals(videoData.getStatus())) {
                
                Episode episode = this.findEpisode(episodeList, season.getSeason(), videoData.getEpisode());
                if (episode == null) {
                    videoData.setStatus(StatusType.MISSING);
                } else {

                    if (OverrideTools.checkOverwriteTitle(videoData, TVDB_SCANNER_ID)) {
                        videoData.setTitle(episode.getEpisodeName(), TVDB_SCANNER_ID);
                    }
                    
                    if (OverrideTools.checkOverwritePlot(videoData, TVDB_SCANNER_ID)) {
                        videoData.setPlot(episode.getOverview(), TVDB_SCANNER_ID);
                    }

                    // cast and crew
                    videoData.addCredditDTOS(actors);
                    
                    for (String director : episode.getDirectors()) {
                        videoData.addCreditDTO(new CreditDTO(JobType.DIRECTOR, director));
                    }
                    for (String writer : episode.getWriters()) {
                        videoData.addCreditDTO(new CreditDTO(JobType.WRITER, writer));
                    }
                    for (String guestStar : episode.getGuestStars()) {
                        videoData.addCreditDTO(new CreditDTO(JobType.ACTOR, guestStar, "Guest Star"));
                    }

                    // TODO more values

                    // set status of video data in process to allow alternate scan
                    videoData.setStatus(StatusType.PROCESSED);
                }
            }
        }
    }

    /**
     * Locate the specific episode from the list of episodes
     *
     * @param episodeList
     * @param seasonNumber
     * @param episodeNumber
     * @return
     */
    private Episode findEpisode(List<Episode> episodeList, int seasonNumber, int episodeNumber) {
        if (CollectionUtils.isEmpty(episodeList)) {
            return null;
        }

        for (Episode episode : episodeList) {
            if (episode.getSeasonNumber() == seasonNumber && episode.getEpisodeNumber() == episodeNumber) {
                return episode;
            }
        }
        return null;
    }
}
