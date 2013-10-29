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

public class LRUTimedCache<K,V> {

    public static final int DEFAULT_MAX_SIZE = 100;
    public static final int DEFAULT_TIMEOUT_IN_SECONDS = 600;
    
    private final Map<K, CachedObject> internal;
    private final long timeoutInMs;
    
    public LRUTimedCache() {
        this(DEFAULT_MAX_SIZE, DEFAULT_TIMEOUT_IN_SECONDS);
    }
 
    private class CachedObject {
        
        private final V value;
        private final long creationTime;
        
        CachedObject(V value) {
            this.value = value;
            this.creationTime = System.currentTimeMillis();
        }

        V value() {
            return value;
        }

        boolean isTimedOut(long timeout) {
            return ((creationTime + timeout) < System.currentTimeMillis());
        }
    }
    
    public LRUTimedCache(final int maxSize, final int defaultTimeout) {
        this.timeoutInMs = (defaultTimeout * 1000);
        this.internal = (Map<K, CachedObject>) Collections.synchronizedMap(new LinkedHashMap<K, CachedObject>(maxSize + 1, .75F, true) {
            private static final long serialVersionUID = 4464242524720551192L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CachedObject> entry)
            {
                return size() > maxSize;
            }
        });
    }
 
    public V put(K key, V value) {
        CachedObject cached = new CachedObject(value);
        CachedObject previous = internal.put(key, cached);
        if (previous == null || previous.isTimedOut(this.timeoutInMs)) {
            return null;
        }
        return previous.value();
    }
 
    public V get(K key) {
        CachedObject cached = internal.get(key);
        if (cached == null) {
            return null;
        } else if (cached.isTimedOut(this.timeoutInMs)) {
            internal.remove(key);
            return null;
        }
        return cached.value();
    }
}
