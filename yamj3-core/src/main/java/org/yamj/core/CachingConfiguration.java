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

    private static final String ALLOCINE_SEARCH = "allocineSearchCache";
    private static final String ALLOCINE_INFO = "allocineInfoCache";
    private static final String TVDB = "tvdbCache";
    private static final String TMDB_ARTWORK = "tmdbArtworkCache";
    private static final String ATTACHMENTS = "attachmentCache";
    private static final String IMDB_WEBPAGE = "imdbWebpageCache";
    private static final String IMDB_ARTWORK = "imdbArtworkCache";
    
    @Bean(destroyMethod="shutdown")
    public net.sf.ehcache.CacheManager ehCacheManager() {
        return net.sf.ehcache.CacheManager.create(
            new net.sf.ehcache.config.Configuration()
                // default cache
                .defaultCache(cacheConfig("default", 1000, 600, MemoryStoreEvictionPolicy.LRU))
                
                // API caches
                .cache(cacheConfig(ALLOCINE_SEARCH, 100, 300, MemoryStoreEvictionPolicy.LFU))
                .cache(cacheConfig(ALLOCINE_INFO, 400, 1800, MemoryStoreEvictionPolicy.LRU))
                .cache(cacheConfig(TVDB, 500, 1800, MemoryStoreEvictionPolicy.LRU))
                .cache(cacheConfig(TMDB_ARTWORK, 100, 1800, MemoryStoreEvictionPolicy.LFU))
                .cache(cacheConfig(ATTACHMENTS, 300, 3600, MemoryStoreEvictionPolicy.LRU))
                .cache(cacheConfig(IMDB_WEBPAGE, 50, 86400, MemoryStoreEvictionPolicy.LFU))
                .cache(cacheConfig(IMDB_ARTWORK, 100, 1800, MemoryStoreEvictionPolicy.LFU))
                
                // caches for database objects
                .cache(cacheConfigDatabase(DatabaseCache.GENRE, 50, 86400))
                .cache(cacheConfigDatabase(DatabaseCache.STUDIO, 50, 86400))
                .cache(cacheConfigDatabase(DatabaseCache.COUNTRY, 50, 86400))
                .cache(cacheConfigDatabase(DatabaseCache.CERTIFICATION, 100, 86400))
                .cache(cacheConfigDatabase(DatabaseCache.PERSON, 500, 86400))
                .cache(cacheConfigDatabase(DatabaseCache.BOXEDSET, 50, 86400))
                .cache(cacheConfigDatabase(DatabaseCache.AWARD, 50, 86400))
                .cache(cacheConfigDatabase(DatabaseCache.STAGEFILE, 100, 180))
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
    
    private static CacheConfiguration cacheConfig(String name, int maxEntries, long timeToLiveSeconds, MemoryStoreEvictionPolicy memoryStoreEvictionPolicy) {
        return new CacheConfiguration()
            .name(name)
            .eternal(false)
            .maxEntriesLocalHeap(maxEntries)
            .timeToIdleSeconds(0)
            .timeToLiveSeconds(timeToLiveSeconds)
            .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE))
            .memoryStoreEvictionPolicy(memoryStoreEvictionPolicy)
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
    public Cache allocineSearchCache() {
        return cacheManager().getCache(ALLOCINE_SEARCH);
    }

    @Bean
    public Cache allocineInfoCache() {
        return cacheManager().getCache(ALLOCINE_INFO);
    }

    @Bean
    public Cache tvdbCache() {
        return cacheManager().getCache(TVDB);
    }

    @Bean
    public Cache tmdbArtworkCache() {
        return cacheManager().getCache(TVDB);
    }

    @Bean
    public Cache attachmentCache() {
        return cacheManager().getCache(ATTACHMENTS);
    }

    @Bean
    public Cache imdbWebpageCache() {
        return cacheManager().getCache(IMDB_WEBPAGE);
    }

    @Bean
    public Cache imdbArtworkCache() {
        return cacheManager().getCache(IMDB_ARTWORK);
    }
}
