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
package org.yamj.plugin.api.service;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

public class EhCacheWrapper {

    private final Ehcache ehcache;
    
    public EhCacheWrapper(Ehcache ehcache) {
        this.ehcache = ehcache;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Element element = this.ehcache.get(key);
        Object value = (element != null ? element.getObjectValue() : null);
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    public void store(String key, Object value) {
        if (value != null) {
            this.ehcache.putIfAbsent(new Element(key, value));
        }
    }
}
