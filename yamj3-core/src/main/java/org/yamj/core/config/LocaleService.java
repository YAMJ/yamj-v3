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

import java.io.InputStream;
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
    private Map<String,String> displayLanguageLookupMap = new HashMap<>();
    private Map<String,String> displayCountryLookupMap = new HashMap<>();
    
    @Autowired
    private ConfigService configService;

    @PostConstruct
    public void init() {
        for (Locale locale : LocaleUtils.availableLocaleList()) {
            if (StringUtils.isBlank(locale.getLanguage())) {
                continue;
            }
            
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

            key = locale.getCountry();
            if (StringUtils.isEmpty(key)) {
                countryLookupMap.put(key, locale.getCountry());
            }
            
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
                if (StringUtils.isBlank(alternate.getLanguage())) {
                    continue;
                }

                key = locale.getDisplayLanguage(alternate);
                if (StringUtils.isNotEmpty(key)) {
                    languageLookupMap.put(key, locale.getLanguage());

                    String altLang = alternate.getLanguage();
                    String lang = locale.getLanguage();
                    displayLanguageLookupMap.put(new String(altLang+"_"+lang).toLowerCase(), key);
                    
                    // try ISO 3
                    try {
                        altLang = alternate.getISO3Language();
                        if (StringUtils.isNotBlank(altLang)) {
                            displayLanguageLookupMap.put(new String(altLang+"_"+lang).toLowerCase(), key);
                        }
                    } catch (Exception ignore) {/*ignore*/}
                }

                key = locale.getDisplayCountry(alternate);
                if (StringUtils.isNotEmpty(key)) {
                    countryLookupMap.put(key, locale.getCountry());

                    String altLang = alternate.getLanguage();
                    String country = locale.getCountry();
                    displayCountryLookupMap.put(new String(altLang+"_"+country).toLowerCase(), key);
                    
                    // try ISO 3
                    try {
                        altLang = alternate.getISO3Language();
                        if (StringUtils.isNotBlank(altLang)) {
                            displayCountryLookupMap.put(new String(altLang+"_"+country).toLowerCase(), key);
                        }
                    } catch (Exception ignore) {/*ignore*/}
                }
            }
        }

        // additional languages from properties file
        try (InputStream inStream = getClass().getResourceAsStream("/languages.code.properties")) {
            Properties props = new Properties();
            props.load(inStream);
            for (Entry<Object,Object> prop : props.entrySet()) {
                languageLookupMap.put(prop.getKey().toString().toLowerCase(), prop.getValue().toString());
            }
        } catch (Exception e) {
            LOG.error("Failed to load language code properties: {}", e.getMessage());
        }

        // additional language display from properties file
        try (InputStream inStream = getClass().getResourceAsStream("/languages.display.properties")) {
            Properties props = new Properties();
            props.load(inStream);
            for (Entry<Object,Object> prop : props.entrySet()) {
                displayLanguageLookupMap.put(prop.getKey().toString().toLowerCase(), prop.getValue().toString());
            }
        } catch (Exception e) {
            LOG.error("Failed to load language display properties: {}", e.getMessage());
        }

        // additional countries from properties file
        try (InputStream inStream = getClass().getResourceAsStream("/countries.code.properties")) {
            Properties props = new Properties();
            props.load(inStream);
            for (Entry<Object,Object> prop : props.entrySet()) {
                String key = StringUtils.replace(prop.getKey().toString(), "_", " ");
                countryLookupMap.put(key, prop.getValue().toString());
            }
        } catch (Exception e) {
            LOG.error("Failed to load country code properties: {}", e.getMessage());
        }
        

        // additional country display from properties file
        try (InputStream inStream = getClass().getResourceAsStream("/countries.display.properties")) {
            Properties props = new Properties();
            props.load(inStream);
            for (Entry<Object,Object> prop : props.entrySet()) {
                displayCountryLookupMap.put(prop.getKey().toString().toLowerCase(), prop.getValue().toString());
            }
        } catch (Exception e) {
            LOG.error("Failed to load country display properties: {}", e.getMessage());
        }

        // build default locale
        String language = PropertyTools.getProperty("yamj3.language");
        if (StringUtils.isEmpty(language)) {
            language = Locale.getDefault().getLanguage();
        } else {
            language = findLanguageCode(language);
            if (StringUtils.isEmpty(language)) {
                language = Locale.getDefault().getLanguage();
            }
        }
        String country = PropertyTools.getProperty("yamj3.country");
        if (StringUtils.isEmpty(country)) {
            country = Locale.getDefault().getCountry();
        } else {
            country = findCountryCode(country);
            if (StringUtils.isEmpty(country)) {
                country = Locale.getDefault().getCountry();
            }
        }
        
        // default locale for YAMJ
        yamjLocale = new Locale(language, country);
        
        LOG.info("YAMY default: language={}, country={}", language, country);
        LOG.info("YAMY localized languages: {}", languageLookupMap.size());
        LOG.info("YAMY displayed languages: {}", displayLanguageLookupMap.size());
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

    public String getDisplayCountry(final String inLanguage, final String countryCode) {
        String langCode = (inLanguage == null ? yamjLocale.getLanguage() : inLanguage);
        
        // fast way
        String key = new String(langCode + "_" + countryCode).toLowerCase();
        String display = this.displayCountryLookupMap.get(key);
        if (display != null) return display;
            
        // slower way
        langCode = findLanguageCode(inLanguage);
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

