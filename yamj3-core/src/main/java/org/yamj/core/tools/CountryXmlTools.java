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
public class CountryXmlTools {

    private static final Logger LOG = LoggerFactory.getLogger(CountryXmlTools.class);

    private static final Map<String,String> COUNTRIES_MAP = new HashMap<>();

    @Autowired
    private CommonStorageService commonStorageService;

    @PostConstruct
    public void init() {
        String countryFileName = PropertyTools.getProperty("yamj3.country.fileName");
        if (StringUtils.isBlank(countryFileName)) {
            LOG.trace("No valid country file name configured");
            return;
        }
        if (!StringUtils.endsWithIgnoreCase(countryFileName, "xml")) {
            LOG.warn("Invalid country file name specified: {}", countryFileName);
            return;
        }

        File xmlFile;
        if (StringUtils.isBlank(FilenameUtils.getPrefix(countryFileName))) {
            // relative path given
            String path = System.getProperty("yamj3.home");
            if (StringUtils.isEmpty(path)) {
                path = ".";
            }
            xmlFile = new File(FilenameUtils.concat(path, countryFileName));
        } else  {
            // absolute path given
            xmlFile = new File(countryFileName);
        }

        if (!xmlFile.exists() || !xmlFile.isFile()) {
            LOG.warn("Countries file does not exist: {}", xmlFile.getPath());
            return;
        }
        if (!xmlFile.canRead()) {
            LOG.warn("Countries file not readble: {}", xmlFile.getPath());
            return;
        }

        LOG.debug("Initialize countries from file: {}", xmlFile.getPath());

        try {
            XMLConfiguration c = new XMLConfiguration(xmlFile);

            List<HierarchicalConfiguration> countries = c.configurationsAt("country");
            for (HierarchicalConfiguration country : countries) {
                String masterCountry = country.getString("[@name]");
                List<Object> subCountries = country.getList("subcountry");
                for (Object subCountry : subCountries) {
                    LOG.debug("New genre added to map: {} -> {}", subCountry, masterCountry);
                    COUNTRIES_MAP.put(((String)subCountry).toLowerCase(), masterCountry);
                }
            }

            try {
                this.commonStorageService.updateCountriesXml(COUNTRIES_MAP);
            } catch (Exception ex) {
                LOG.warn("Failed update countries xml in database", ex);
            }
        } catch (Exception ex) {
            LOG.error("Failed parsing country input file: " + xmlFile.getPath(), ex);
        }
    }

    public static String getMasterCountry(String country) {
        if (country == null) {
            return null;
        }
        return COUNTRIES_MAP.get(country.toLowerCase());
    }
}
