package com.moviejukebox.common.util;

import java.util.*;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public abstract class TokensPatternMap extends HashMap<String, Pattern> {
    
	private static final long serialVersionUID = 2239121205124537392L;

    /**
     * Generate pattern using tokens from given string.
     *
     * @param key Language id.
     * @param tokensStr Tokens list divided by comma or space.
     */
    public void put(String key, String tokensStr) {
        List<String> tokens = new ArrayList<String>();
        for (String token : tokensStr.split("[ ,]+")) {
            token = StringUtils.trimToNull(token);
            if (token != null) {
                tokens.add(token);
            }
        }
        put(key, tokens);
    }

    public void putAll(List<String> keywords, Map<String, String> keywordMap) {
        for (String keyword : keywords) {
            // Just pass the keyword if the map is null
            if (keywordMap.get(keyword) == null) {
                put(keyword, keyword);
            } else {
                put(keyword, keywordMap.get(keyword));
            }
        }
    }

    /**
     * Generate pattern using tokens from given string.
     *
     * @param key Language id.
     * @param tokens Tokens list.
     */
    protected abstract void put(String key, Collection<String> tokens);
}
