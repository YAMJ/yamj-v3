package com.moviejukebox.core.service.plugin;

import com.moviejukebox.common.tools.PropertyTools;
import com.moviejukebox.core.database.model.Series;
import com.omertron.thetvdbapi.TheTVDBApi;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("thetvdbScanner")
public class TheTVDbScanner implements ISeriesScanner, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbScanner.class);
    private static final String TVDB_PLUGIN_ID = "tvdb";
    private static TheTVDBApi tvdbApi;
    private static final String API_KEY = PropertyTools.getProperty("APIKEY.tvdb", "");
    @Autowired
    private PluginDatabaseService pluginDatabaseService;

    @Override
    public String getScannerName() {
        return TVDB_PLUGIN_ID;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
          if (StringUtils.isNotBlank(API_KEY)) {
            try {
                tvdbApi = new TheTVDBApi(API_KEY);
                // register this scanner
                pluginDatabaseService.registerSeriesScanner(this);

            } catch (Exception ex) {
                LOG.error("Unable to initialise TheMovieDbScanner, error: {}", ex.getMessage());
            }
        } else {
            LOG.error("Failed to initialise TheMovieDbScanner, no API KEY available");
        }
  }

    @Override
    public String getSeriesId(Series series) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getSeriesId(String title, int year) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ScanResult scan(Series series) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
