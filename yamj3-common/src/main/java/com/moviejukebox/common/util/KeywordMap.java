package com.moviejukebox.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Store list (ordered) and keyword map.
 */
public class KeywordMap extends HashMap<String, String> {

    private static final long serialVersionUID = 1L;
    
    private final transient List<String> keywords = new ArrayList<String>();

    public List<String> getKeywords() {
        return keywords;
    }
}
