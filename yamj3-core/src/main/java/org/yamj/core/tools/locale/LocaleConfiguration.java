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
package org.yamj.core.tools.locale;

import java.util.Locale;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocaleConfiguration  {

    private static final Logger LOG = LoggerFactory.getLogger(LocaleConfiguration.class);

    @Value("${yamj3.language:null}")
    private String language;

    @Value("${yamj3.country:null}")
    private String country;
  
    @Bean
    public Locale yamjLocale() {
        Locale locale;
        if (StringUtils.isBlank(language) || StringUtils.isBlank(country)) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(language, country);
            if (!LocaleUtils.isAvailableLocale(locale)) {
                locale = Locale.getDefault();
            }
        }
        LOG.info("YAMJ locale: language={}, country={}", locale.getLanguage(), locale.getCountry());
        return locale;
    }
}

