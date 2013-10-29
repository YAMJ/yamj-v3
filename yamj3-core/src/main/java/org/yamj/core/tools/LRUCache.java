/*
 *      Copyright (c) 2004-2013 YAMJ Members
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K,V> {

    public static final int DEFAULT_MAX_SIZE = 100;
 
    private final Map<K, V> internal;
 
    public LRUCache() {
        this(DEFAULT_MAX_SIZE);
    }
 
    public LRUCache(final int maxSize) {
        this.internal = (Map<K, V>) Collections.synchronizedMap(new LinkedHashMap<K, V>(maxSize + 1, .75F, true) {
            private static final long serialVersionUID = 4464242524720551192L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
            {
                return size() > maxSize;
            }
        });
    }
 
    public V put(K key, V value) {
        return internal.put(key, value);
    }
 
    public V get(K key) {
        return internal.get(key);
    }
}
