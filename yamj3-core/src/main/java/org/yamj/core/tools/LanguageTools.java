/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.util.KeywordMap;
import org.yamj.common.util.PatternUtils;
import org.yamj.common.util.TokensPatternMap;

@Component
public class LanguageTools {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageTools.class);
    
    /**
     * Mapping exact tokens to language.
     *
     * Strict mapping is case sensitive and must be obvious, it must avoid confusing movie name words and language markers.
     *
     * For example the English word "it" and Italian language marker "it", or "French" as part of the title and "french" as language
     * marker.
     *
     * However, described above is important only by file naming with token delimiters (see tokens description constants
     * TOKEN_DELIMITERS*). Language detection in non-token separated titles will be skipped automatically.
     *
     * Language markers, found with this pattern are counted as token delimiters (they will cut movie title)
     */
    private final static TokensPatternMap STRICT_LANGUAGE_MAP = new TokensPatternMap() {
        private static final long serialVersionUID = 3630995345545037071L;

        @Override
        protected void put(String key, Collection<String> tokens) {
            StringBuilder tokenBuilder = new StringBuilder();
            for (String s : tokens) {
                if (tokenBuilder.length() > 0) {
                    tokenBuilder.append('|');
                }
                tokenBuilder.append(Pattern.quote(s));
            }
            put(key, PatternUtils.tpatt(tokenBuilder.toString()));
        }
    };
    
    /**
     * Mapping loose language markers.
     *
     * The second pass of language detection is being started after movie title detection. Language markers will be scanned with
     * loose pattern in order to find out more languages without chance to confuse with movie title.
     *
     * Markers in this map are case insensitive.
     */
    private final  static TokensPatternMap LOOSE_LANGUAGE_MAP = new TokensPatternMap() {
        private static final long serialVersionUID = 1383819843117148442L;

        @Override
        protected void put(String key, Collection<String> tokens) {
            StringBuilder tokenBuilder = new StringBuilder();
            for (String token : tokens) {
                // Only add the token if it's not there already
                String quotedToken = Pattern.quote(token.toUpperCase());
                if (tokenBuilder.indexOf(quotedToken) < 0) {
                    if (tokenBuilder.length() > 0) {
                        tokenBuilder.append('|');
                    }
                    tokenBuilder.append(quotedToken);
                }
            }
            put(key, PatternUtils.iwpatt(tokenBuilder.toString()));
        }
    };

    @PostConstruct
    public void init() throws Exception {
        final KeywordMap languages = PropertyTools.getKeywordMap("language.detection.keywords", null);
        if (languages.size() > 0) {
            for (String lang : languages.getKeywords()) {
                String values = languages.get(lang);
                if (values != null) {
                    STRICT_LANGUAGE_MAP.put(lang, values);
                    LOOSE_LANGUAGE_MAP.put(lang, values);
                } else {
                    LOG.info("No values found for language code '{}'", lang);
                }
            }
        }
    }

    /**
     * Decode the language tag passed in, into standard YAMJ language code
     *
     * @param language
     * @return
     */
    public static String determineLanguage(String language) {
        for (Map.Entry<String, Pattern> e : STRICT_LANGUAGE_MAP.entrySet()) {
            Matcher matcher = e.getValue().matcher(language);
            if (matcher.find()) {
                return e.getKey();
            }
        }
        return language;
    }

    /**
     * Get the list of loose languages associated with a language
     *
     * @param language
     * @return
     */
    public static String getLanguageList(String language) {
        if (LOOSE_LANGUAGE_MAP.containsKey(language)) {
            Pattern langPatt = LOOSE_LANGUAGE_MAP.get(language);
            return langPatt.toString().toLowerCase();
        } else {
            return "";
        }
    }

    public static TokensPatternMap getStrictLanguageMap() {
        return STRICT_LANGUAGE_MAP;
    }

    public static TokensPatternMap getLooseLanguageMap() {
        return LOOSE_LANGUAGE_MAP;
    }
}
