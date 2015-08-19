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
    private Map<String,String> languageDisplayMap = new HashMap<>();
    private Map<String,String> countryLookupMap = new HashMap<>();
    private Map<String,String> countryDisplayMap = new HashMap<>();
    
    @Autowired
    private ConfigService configService;

    @PostConstruct
    public void init() {
        String langCode;
        String countryCode;
        String langISO3;
        String countryISO3;
        String displayLanguage;
        String displayCountry;
        String altCode;
        String altDisplay;
        
        for (Locale locale : LocaleUtils.availableLocaleSet()) {
            langCode = StringUtils.trimToNull(locale.getLanguage());
            countryCode = StringUtils.trimToNull(locale.getCountry());
            try {
                langISO3 = StringUtils.trimToNull(locale.getISO3Language());
            } catch (Exception e) {
               langISO3 = null; 
            }
            try {
                countryISO3 = StringUtils.trimToNull(locale.getISO3Country());
            } catch (Exception e) {
                countryISO3 = null; 
            }
            displayLanguage = StringUtils.trimToNull(locale.getDisplayLanguage());
            displayCountry = StringUtils.trimToNull(locale.getDisplayCountry());

            if (langCode != null) {
                languageLookupMap.put(langCode, langCode);
                if (langISO3 != null) {
                    languageLookupMap.put(langISO3, langCode);
                }
                if (displayLanguage != null) {
                    languageLookupMap.put(displayLanguage, langCode);
                    languageDisplayMap.put(langCode+"_"+langCode, displayLanguage);
                }
            }

            if (countryCode != null) {
                countryLookupMap.put(countryCode, countryCode);
                if (countryISO3 != null) {
                    countryLookupMap.put(countryISO3, countryCode);
                }
                if (displayCountry != null) {
                    countryLookupMap.put(displayCountry, countryCode);
                    if (langCode != null) {
                        countryDisplayMap.put(langCode+"_"+countryCode, displayCountry);
                    }
                }
            }

            for (Locale alternate : LocaleUtils.availableLocaleList()) {
                altCode = StringUtils.trimToNull(alternate.getLanguage());

                if (langCode != null) {
                    altDisplay = StringUtils.trimToNull(locale.getDisplayLanguage(alternate));
                    if (altDisplay != null) {
                        languageLookupMap.put(altDisplay, langCode);
                        if (altCode != null) {
                            languageDisplayMap.put(altCode+"_"+langCode, altDisplay);
                        }
                    }
                }

                if (countryCode != null) {
                    altDisplay = StringUtils.trimToNull(locale.getDisplayCountry(alternate));
                    if (altDisplay != null) {
                        countryLookupMap.put(altDisplay, countryCode);
                        if (altCode != null) {
                            countryDisplayMap.put(altCode+"_"+countryCode, altDisplay);
                        }
                    }
                }
            }
        }

        // additional languages from properties file
        try (InputStream inStream = getClass().getResourceAsStream("/iso639.xcode.properties")) {
            Properties props = new Properties();
            props.load(inStream);
            for (Entry<Object,Object> prop : props.entrySet()) {
                String key = StringUtils.replace(prop.getKey().toString(), "_", " ");
                languageLookupMap.put(key, prop.getValue().toString());
            }
        } catch (Exception e) {
            LOG.error("Failed to load language code properties: {}", e.getMessage());
        }

        // additional language display from properties file
        try (InputStream inStream = getClass().getResourceAsStream("/iso639.xdisplay.properties")) {
            Properties props = new Properties();
            props.load(inStream);
            for (Entry<Object,Object> prop : props.entrySet()) {
                languageDisplayMap.put(prop.getKey().toString(), prop.getValue().toString());
            }
        } catch (Exception e) {
            LOG.error("Failed to load language display properties: {}", e.getMessage());
        }

        // the ISO 639 catalogs
        for (String lang : new String[]{"", "de"}) {
            StringBuilder stream = new StringBuilder();
            stream.append("/iso639.");
            if (StringUtils.isNotBlank(lang)) {
                stream.append(lang).append(".");
            }
            stream.append("properties");
            
            try (InputStream inStream = getClass().getResourceAsStream(stream.toString())) {
                Properties props = new Properties();
                props.load(inStream);
                for (Entry<Object,Object> prop : props.entrySet()) {
                    // map from name to code
                    languageLookupMap.put(prop.getValue().toString(), prop.getKey().toString());
                    if (StringUtils.isBlank(lang)) {
                        // map from code to name
                        languageDisplayMap.put(prop.getKey().toString(), prop.getValue().toString());
                    } else {
                        // map from language plus code to name
                        languageDisplayMap.put(lang + "_" + prop.getKey().toString(), prop.getValue().toString());
                    }
                }
            } catch (Exception e) {
                LOG.error("Failed to load {}: {}", stream, e.getMessage());
            }
        }

        // additional countries from properties file
        try (InputStream inStream = getClass().getResourceAsStream("/iso3166.xcode.properties")) {
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
        try (InputStream inStream = getClass().getResourceAsStream("/iso3166.xdisplay.properties")) {
            Properties props = new Properties();
            props.load(inStream);
            for (Entry<Object,Object> prop : props.entrySet()) {
                countryDisplayMap.put(prop.getKey().toString(), prop.getValue().toString());
            }
        } catch (Exception e) {
            LOG.error("Failed to load country display properties: {}", e.getMessage());
        }

        // the ISO 3166 catalogs
        for (String lang : new String[]{"","de","fr"}) {
            StringBuilder stream = new StringBuilder();
            stream.append("/iso3166.");
            if (StringUtils.isNotBlank(lang)) {
                stream.append(lang).append(".");
            }
            stream.append("properties");
            
            try (InputStream inStream = getClass().getResourceAsStream(stream.toString())) {
                Properties props = new Properties();
                props.load(inStream);
                for (Entry<Object,Object> prop : props.entrySet()) {
                    // map from name to code
                    countryLookupMap.put(prop.getValue().toString(), prop.getKey().toString());
                    if (StringUtils.isBlank(lang)) {
                        // map from code to name
                        countryDisplayMap.put(prop.getKey().toString(), prop.getValue().toString());
                    } else {
                        // map from language plus code to name
                        countryDisplayMap.put(lang + "_" + prop.getKey().toString(), prop.getValue().toString());
                    }
                }
            } catch (Exception e) {
                LOG.error("Failed to load {}: {}", stream, e.getMessage());
            }
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
        LOG.info("YAMY lookup languages:  {}", languageLookupMap.size());
        LOG.info("YAMY display languages: {}", languageDisplayMap.size());
        LOG.info("YAMY lookup countries:  {}", countryLookupMap.size());
        LOG.info("YAMY display countries: {}", countryDisplayMap.size());
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
        String country = configService.getProperty(config+".country");
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

    public String getDisplayLanguage(final String inLanguage, final String languageCode) {
        if (languageCode == null) return null;
        final String langCode = languageCode.toLowerCase();
        String inLangCode = (inLanguage == null ? yamjLocale.getLanguage() : inLanguage.toLowerCase());
        
        // fast way
        String display = this.languageDisplayMap.get(inLangCode + "_" + langCode);
        if (display != null) return display;
        
        // slower way
        inLangCode = findLanguageCode(inLanguage);
        if (inLangCode == null) inLangCode = yamjLocale.getLanguage();
        display = this.languageDisplayMap.get(inLangCode + "_" + langCode);
        if (display != null) return display;

        // search for language code only
        display = this.languageDisplayMap.get(langCode);
        if (display != null) return display;

        // just return language code
        return languageCode;
    }

    public String getDisplayCountry(final String inLanguage, final String countryCode) {
        if (countryCode == null) return null;
        final String cCode = countryCode.toUpperCase();
        String inLangCode = (inLanguage == null ? yamjLocale.getLanguage() : inLanguage.toLowerCase());
        
        // fast way
        String display = this.countryDisplayMap.get(inLangCode + "_" + cCode);
        if (display != null) return display;
            
        // slower way
        inLangCode = findLanguageCode(inLanguage);
        if (inLangCode == null) inLangCode = yamjLocale.getLanguage();
        display = this.countryDisplayMap.get(inLangCode + "_" + cCode);
        if (display != null) return display;

        // search for country code only
        display = this.countryDisplayMap.get(cCode);
        if (display != null) return display;

        // just return country code
        return cCode;
    }
    
    public Set<String> getCertificationCountryCodes() {
        return this.getCertificationCountryCodes(yamjLocale);
    }

    public Set<String> getCertificationCountryCodes(Locale locale) {
        Set<String> result = new HashSet<>();
        List<String> countries = this.configService.getPropertyAsList("yamj3.certification.countries", locale.getCountry());
        for (String country : countries) {
            String countryCode = this.findCountryCode(country);
            if (countryCode != null) result.add(countryCode);
        }
        return result;
    }
    
    public Set<String> getCountryNames(String countryCode) {
        Set<String> result = new HashSet<>();
        if (StringUtils.isNotBlank(countryCode)) {
            for (Entry<String,String> entry : countryLookupMap.entrySet()) {
                if (entry.getValue().equals(countryCode)) {
                    result.add(entry.getKey());
                }
            }
        }
        return result;
    }
}

