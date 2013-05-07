package com.moviejukebox.core.service.plugin;

import com.moviejukebox.common.tools.PropertyTools;
import com.moviejukebox.core.database.model.Series;
import com.moviejukebox.core.database.model.dto.CreditDTO;
import com.omertron.thetvdbapi.TheTVDBApi;
import com.omertron.thetvdbapi.model.Actor;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("tvdbScanner")
public class TheTVDbScanner implements ISeriesScanner, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbScanner.class);
    private static final String TVDB_SCANNER_ID = "tvdb";
    private static TheTVDBApi tvdbApi;
    private static final String API_KEY = PropertyTools.getProperty("APIKEY.tvdb", "");
    private static final String DEFAULT_LANGUAGE = PropertyTools.getProperty("thetvdb.language", "en");
    private static final int YEAR_MIN = 1900;
    private static final int YEAR_MAX = 2050;
    @Autowired
    private PluginDatabaseService pluginDatabaseService;

    @Override
    public String getScannerName() {
        return TVDB_SCANNER_ID;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isNotBlank(API_KEY)) {
            try {
                tvdbApi = new TheTVDBApi(API_KEY);
                // register this scanner
                pluginDatabaseService.registerSeriesScanner(this);

            } catch (Exception ex) {
                LOG.error("Unable to initialise TheTVDbScanner, error: {}", ex.getMessage());
            }
        } else {
            LOG.error("Failed to initialise TheTVDbScanner, no API KEY available");
        }
    }

    @Override
    public String getSeriesId(Series series) {
        String id = series.getMoviedbId(TVDB_SCANNER_ID);

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

            series.setMoviedbId(TVDB_SCANNER_ID, tvdbSeries.getId());
            series.setMoviedbId(ImdbScanner.getScannerId(), tvdbSeries.getImdbId());
            series.setOutline(tvdbSeries.getOverview());
            series.setPlot(tvdbSeries.getOverview());

            try {
                series.addRating(TVDB_SCANNER_ID, (int) (Float.parseFloat(tvdbSeries.getRating()) * 10));
            } catch (NumberFormatException nfe) {
                LOG.warn("Failed to convert TVDB rating '{}' to an integer, error: {}", tvdbSeries.getRating(), nfe.getMessage());
            }

            String faDate = tvdbSeries.getFirstAired();
            if (StringUtils.isNotBlank(faDate)) {
                if (faDate.length() >= 4) {
                    series.setStartYear(Integer.parseInt(faDate.substring(0, 4)));
                }
            }
            series.setTitle(tvdbSeries.getSeriesName(), TVDB_SCANNER_ID);
            series.setUpdateTimestamp(new Date());

            // CAST & CREW
            CreditDTO credit;
            for (Actor person : tvdbApi.getActors(id)) {
                // TODO: Add people processing
                LOG.trace("Person: {}", person.toString());
            }

            return ScanResult.OK;
        } else {
            return ScanResult.MISSING_ID;
        }

    }
}
