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
package org.yamj.core.config;

import java.util.*;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;

@Service("localeService")
public class LocaleService  {

    private static final Logger LOG = LoggerFactory.getLogger(LocaleService.class);

    private Locale yamjLocale = Locale.getDefault();
    private Map<String,String> languageLookupMap = new HashMap<>();
    private Map<String,String> countryLookupMap = new HashMap<>();
    private Map<String,String> displayCountryLookupMap = new HashMap<>();
    
    @Autowired
    private ConfigService configService;
    @Autowired
    private Properties countryProperties;
    
    @PostConstruct
    public void init() {
        for (String countryCode : Locale.getISOCountries()) {
            for (Locale locale : LocaleUtils.languagesByCountry(countryCode)) {
                languageLookupMap.put(locale.getLanguage(), locale.getLanguage());

                String key = locale.getDisplayLanguage();
                if (StringUtils.isNotEmpty(key)) {
                    languageLookupMap.put(key, locale.getLanguage());
                }
                
                try {
                    key = locale.getISO3Language();
                    if (StringUtils.isNotEmpty(key)) {
                        languageLookupMap.put(key, locale.getLanguage());
                    }
                } catch (Exception e) {/*ignore*/}
                
                countryLookupMap.put(locale.getCountry(), locale.getCountry());

                key = locale.getDisplayCountry();
                if (StringUtils.isNotEmpty(key)) {
                    countryLookupMap.put(key, locale.getCountry());
                }

                try {
                    key = locale.getISO3Country();
                    if (StringUtils.isNotEmpty(key)) {
                        countryLookupMap.put(key, locale.getCountry());
                    }
                } catch (Exception e) {/*ignore*/}
                
                for (Locale alternate : LocaleUtils.availableLocaleList()) {
                    key = locale.getDisplayLanguage(alternate);
                    if (StringUtils.isNotEmpty(key)) {
                        languageLookupMap.put(key, locale.getLanguage());
                    }

                    key = locale.getDisplayCountry(alternate);
                    if (StringUtils.isNotEmpty(key)) {
                        countryLookupMap.put(key, locale.getCountry());
                        
                        final String lang = alternate.getLanguage();
                        final String country = locale.getCountry();
                        displayCountryLookupMap.put(new String(lang+"_"+country).toLowerCase(), key);
                    }
                }
            }
        }
        
        // additional countries from properties file
        for (Entry<Object,Object> prop : countryProperties.entrySet()) {
            String key = StringUtils.replace(prop.getKey().toString(), "_", " ");
            countryLookupMap.put(key, prop.getValue().toString());
        }
        
        // build default locale
        String language = PropertyTools.getProperty("yamj3.language");
        if (StringUtils.isBlank(language)) {
            language = Locale.getDefault().getLanguage();
        } else {
            language = findLanguageCode(language);
            if (StringUtils.isBlank(language)) {
                language = Locale.getDefault().getLanguage();
            }
        }
        String country = PropertyTools.getProperty("yamj3.country");
        if (StringUtils.isBlank(country)) {
            country = Locale.getDefault().getCountry();
        } else {
            country = findCountryCode(country);
            if (StringUtils.isBlank(country)) {
                country = Locale.getDefault().getCountry();
            }
        }
        
        yamjLocale = new Locale(language, country);
        
        LOG.info("YAMY default: language={}, country={}", language, country);
        LOG.info("YAMY localized languages: {}", languageLookupMap.size());
        LOG.info("YAMY localized countries: {}", countryLookupMap.size());
        LOG.info("YAMY displayed countries: {}", displayCountryLookupMap.size());
    }
    
    public String findLanguageCode(String language) {
        if (StringUtils.isBlank(language)) {
            return null;
        }
        
        String languageCode = languageLookupMap.get(language);
        if (languageCode != null) return languageCode;
        
        // check case insensitive
        for (Entry<String,String> entry : languageLookupMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(language)) {
                return entry.getValue();
            }
        }
        
        LOG.warn("No language code found for language '{}'", language);
        return null;
    }

    public String findCountryCode(String country) {
        if (StringUtils.isBlank(country)) {
            return null;
        }
        
        String countryCode = countryLookupMap.get(country);
        if (countryCode != null) return countryCode;
        
        // check case insensitive
        for (Entry<String,String> entry : countryLookupMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(country)) {
                return entry.getValue();
            }
        }

        LOG.warn("No country code found for country '{}'", country);
        return null;
    }
    
    public Locale getLocaleForConfig(String config) {
        if (StringUtils.isBlank(config)) {
            return yamjLocale;
        }

        String language = configService.getProperty(config+".language");
        if (StringUtils.isBlank(language)) {
            language = yamjLocale.getLanguage();
        } else {
            language = findLanguageCode(language);
            if (StringUtils.isBlank(language)) {
                language = yamjLocale.getLanguage();
            }
        }
        String country = PropertyTools.getProperty(config+".country");
        if (StringUtils.isBlank(country)) {
            country = yamjLocale.getCountry();
        } else {
            country = findCountryCode(country);
            if (StringUtils.isBlank(country)) {
                country = yamjLocale.getCountry();
            }
        }
        LOG.trace("Locale for {}: language={}, country={}", config, language, country);
        return new Locale(language, country);            
    }

    public String getDisplayCountry(final String language, final String countryCode) {
        String langCode = (language == null ? yamjLocale.getLanguage() : language);
        
        // fast way
        String key = new String(langCode + "_" + countryCode).toLowerCase();
        String display = this.displayCountryLookupMap.get(key);
        if (display != null) return display;
            
        // slower way
        langCode = findLanguageCode(language);
        if (langCode == null) langCode = yamjLocale.getLanguage();
        key = new String(langCode + "_" + countryCode).toLowerCase();
        display = this.displayCountryLookupMap.get(key);
        
        if (display == null) return countryCode;
        return display;
    }
    
    public Set<String> getCertificationCountryCodes() {
        return this.getCertificationCountryCodes(yamjLocale);
    }

    public Set<String> getCertificationCountryCodes(Locale defaultLocale) {
        Set<String> result = new HashSet<>();
        List<String> countries = this.configService.getPropertyAsList("yamj3.certification.countries", defaultLocale.getCountry());
        for (String country : countries) {
            String countryCode = this.findCountryCode(country);
            if (countryCode != null) {
                result.add(countryCode);
            }
        }
        return result;
    }
    
    public Set<String> getCountryNames(String countryCode) {
        Set<String> result = new HashSet<>();
        for (Entry<String,String> entry : countryLookupMap.entrySet()) {
            if (entry.getValue().equals(countryCode)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}

