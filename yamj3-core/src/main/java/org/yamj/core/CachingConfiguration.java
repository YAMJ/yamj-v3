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
package org.yamj.core;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.interceptor.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@EnableCaching
public class CachingConfiguration implements CachingConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(CachingConfiguration.class);

    private static final String ATTACHMENTS = "attachmentCache";
    private static final int TTL_10_MINUTES = 600;
    private static final int TTL_ONE_DAY = 86400;
    
    @Bean(destroyMethod="shutdown")
    public net.sf.ehcache.CacheManager ehCacheManager() {
        return net.sf.ehcache.CacheManager.create(
            new net.sf.ehcache.config.Configuration()
                // default cache
                .defaultCache(cacheConfig("default", 100, TTL_10_MINUTES))
                .cache(cacheConfig(ATTACHMENTS, 300, TTL_10_MINUTES))
                
                // caches for database objects
                .cache(cacheConfigDatabase(CachingNames.DB_GENRE, 50, TTL_ONE_DAY))
                .cache(cacheConfigDatabase(CachingNames.DB_STUDIO, 50, TTL_ONE_DAY))
                .cache(cacheConfigDatabase(CachingNames.DB_COUNTRY, 50, TTL_ONE_DAY))
                .cache(cacheConfigDatabase(CachingNames.DB_CERTIFICATION, 100, TTL_ONE_DAY))
                .cache(cacheConfigDatabase(CachingNames.DB_PERSON, 500, TTL_ONE_DAY))
                .cache(cacheConfigDatabase(CachingNames.DB_BOXEDSET, 50, TTL_ONE_DAY))
                .cache(cacheConfigDatabase(CachingNames.DB_AWARD, 50, TTL_ONE_DAY))
                .cache(cacheConfigDatabase(CachingNames.DB_STAGEFILE, 100, 180))
                .cache(cacheConfigDatabase(CachingNames.DB_ARTWORK_PROFILE, 50, TTL_ONE_DAY))
                .cache(cacheConfigDatabase(CachingNames.DB_ARTWORK_IMAGE, 2000, TTL_ONE_DAY))
            );
    }

    @Scope
    @Bean
    @Override
    public CacheManager cacheManager() {
        LOG.trace("Create new cache manager using ehcache");
        return new EhCacheCacheManager(ehCacheManager());
    }

    @Override
    public KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator();
    }

    @Override
    public CacheResolver cacheResolver() {
        return null;
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler();
    }
    
    private static CacheConfiguration cacheConfig(String name, int maxEntries, long timeToLiveSeconds) {
        return new CacheConfiguration()
            .name(name)
            .eternal(false)
            .maxEntriesLocalHeap(maxEntries)
            .timeToIdleSeconds(0)
            .timeToLiveSeconds(timeToLiveSeconds)
            .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE))
            .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
            .statistics(false);
    }

    private static CacheConfiguration cacheConfigDatabase(String name, int maxEntries, long timeToLiveSeconds) {
        return new CacheConfiguration()
            .name(name)
            .eternal(false)
            .maxEntriesLocalHeap(maxEntries)
            .timeToIdleSeconds(0)
            .timeToLiveSeconds(timeToLiveSeconds)
            .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE))
            .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
            .statistics(false);
    }

    @Bean
    public Cache attachmentCache() {
        return cacheManager().getCache(ATTACHMENTS);
    }
}
