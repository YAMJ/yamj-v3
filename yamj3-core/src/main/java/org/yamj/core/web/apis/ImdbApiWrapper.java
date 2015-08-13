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
package org.yamj.core.web.apis;

import com.omertron.imdbapi.ImdbApi;
import com.omertron.imdbapi.model.ImdbCredit;
import com.omertron.imdbapi.model.ImdbImage;
import com.omertron.imdbapi.model.ImdbPerson;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.yamj.core.CachingNames;

@Service
public class ImdbApiWrapper {

    private final Lock imdbApiLock = new ReentrantLock(true);

    @Autowired
    private ImdbApi imdbApi;

    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId}")
    public List<ImdbCredit> getFullCast(String imdbId) {
        List<ImdbCredit> fullCast;
        imdbApiLock.lock();
        try {
            // use US locale to check for uncredited cast
            imdbApi.setLocale(Locale.US);
            fullCast = imdbApi.getFullCast(imdbId);
        } finally {
            imdbApiLock.unlock();
        }
        return (fullCast == null ? new ArrayList<ImdbCredit>() : fullCast);
    }

    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId}")
    public ImdbPerson getActorDetails(String imdbId, Locale locale) {
        ImdbPerson imdbPerson;
        imdbApiLock.lock();
        try {
            imdbApi.setLocale(locale);
            imdbPerson = imdbApi.getActorDetails(imdbId);
        } finally {
            imdbApiLock.unlock();
        }
        return (imdbPerson == null ? new ImdbPerson() : imdbPerson);
    }

    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId}")
    public List<ImdbImage> getTitlePhotos(String imdbId) {
        List<ImdbImage> titlePhotos = imdbApi.getTitlePhotos(imdbId);
        return (titlePhotos == null ? new ArrayList<ImdbImage>() : titlePhotos);
    }
}   
