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
package org.yamj.core.tools;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.database.service.CommonStorageService;

@Component
public class GenreXmlTools {

    private static final Logger LOG = LoggerFactory.getLogger(GenreXmlTools.class);

    private static final Map<String,String> GENRES_MAP = new HashMap<>();

    @Autowired
    private CommonStorageService commonStorageService;

    @PostConstruct
    public void init() {
        String genreFileName = PropertyTools.getProperty("yamj3.genre.fileName");
        if (StringUtils.isBlank(genreFileName)) {
            LOG.trace("No valid genre file name configured");
            return;
        }
        if (!StringUtils.endsWithIgnoreCase(genreFileName, "xml")) {
            LOG.warn("Invalid genre file name specified: {}", genreFileName);
            return;
        }

        File xmlFile;
        if (StringUtils.isBlank(FilenameUtils.getPrefix(genreFileName))) {
            // relative path given
            String path = System.getProperty("yamj3.home");
            if (StringUtils.isEmpty(path)) {
                path = ".";
            }
            xmlFile = new File(FilenameUtils.concat(path, genreFileName));
        } else  {
            // absolute path given
            xmlFile = new File(genreFileName);
        }

        if (!xmlFile.exists() || !xmlFile.isFile()) {
            LOG.warn("Genres file does not exist: {}", xmlFile.getPath());
            return;
        }
        if (!xmlFile.canRead()) {
            LOG.warn("Genres file not readble: {}", xmlFile.getPath());
            return;
        }

        LOG.debug("Initialize genres from file: {}", xmlFile.getPath());

        try {
            XMLConfiguration c = new XMLConfiguration(xmlFile);

            List<HierarchicalConfiguration> genres = c.configurationsAt("genre");
            for (HierarchicalConfiguration genre : genres) {
                String masterGenre = genre.getString("[@name]");
                List<Object> subGenres = genre.getList("subgenre");
                for (Object subGenre : subGenres) {
                    LOG.debug("New genre added to map: {} -> {}", subGenre, masterGenre);
                    GENRES_MAP.put(((String)subGenre).toLowerCase(), masterGenre);
                }
            }

            try {
                this.commonStorageService.updateGenresXml(GENRES_MAP);
            } catch (Exception ex) {
                LOG.warn("Failed update genres xml in database", ex);
            }
        } catch (Exception ex) {
            LOG.error("Failed parsing genre input file: " + xmlFile.getPath(), ex);
        }
    }

    public static String getMasterGenre(String genre) {
        if (genre == null) {
            return null;
        }
        return GENRES_MAP.get(genre.toLowerCase());
    }
}
