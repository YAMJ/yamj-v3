package com.moviejukebox.core.tools;

import com.moviejukebox.common.util.KeywordMap;
import com.moviejukebox.common.util.PatternUtils;
import com.moviejukebox.common.util.TokensPatternMap;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("languageTools")
public class LanguageTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageTools.class);

    /**
     * Mapping exact tokens to language.
     *
     * Strict mapping is case sensitive and must be obvious, it must avoid
     * confusing movie name words and language markers.
     *
     * For example the English word "it" and Italian language marker "it", or
     * "French" as part of the title and "french" as language marker.
     *
     * However, described above is important only by file naming with token
     * delimiters (see tokens description constants TOKEN_DELIMITERS*). Language
     * detection in non-token separated titles will be skipped automatically.
     *
     * Language markers, found with this pattern are counted as token delimiters
     * (they will cut movie title)
     */
    private final TokensPatternMap strictLanguageMap = new TokensPatternMap() {
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
     * The second pass of language detection is being started after movie title
     * detection. Language markers will be scanned with loose pattern in order
     * to find out more languages without chance to confuse with movie title.
     *
     * Markers in this map are case insensitive.
     */
    private final TokensPatternMap looseLanguageMap = new TokensPatternMap() {
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

    public LanguageTools() {
        final KeywordMap languages = PropertyTools.getKeywordMap("language.detection.keywords", null);
        if (languages.size() > 0) {
            for (String lang : languages.getKeywords()) {
                String values = languages.get(lang);
                if (values != null) {
                    strictLanguageMap.put(lang, values);
                    looseLanguageMap.put(lang, values);   
                } else {
                    LOGGER.info("No values found for language code: " + lang);
                }
            }
        }
    }
    
    /**
     * Decode the language tag passed in, into standard YAMJ language code
     */
    public String determineLanguage(String language) {
        for (Map.Entry<String, Pattern> e : strictLanguageMap.entrySet()) {
            Matcher matcher = e.getValue().matcher(language);
            if (matcher.find()) {
                return e.getKey();
            }
        }
        return language;
    }

    /**
     * Get the list of loose languages associated with a language
     */
    public String getLanguageList(String language) {
        if (looseLanguageMap.containsKey(language)) {
            Pattern langPatt = looseLanguageMap.get(language);
            return langPatt.toString().toLowerCase();
        } else {
            return "";
        }
    }
    
    public TokensPatternMap getStrictLanguageMap() {
        return this.strictLanguageMap;
    }

    public TokensPatternMap getLooseLanguageMap() {
        return this.looseLanguageMap;
    }
}