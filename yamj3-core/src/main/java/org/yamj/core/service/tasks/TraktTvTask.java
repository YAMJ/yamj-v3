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
package org.yamj.core.service.tasks;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yamj.core.service.trakttv.TraktTvService;

/**
 * Task for periodical synchronization with Trakt.TV
 */
@Component
public class TraktTvTask implements ITask {

    private static final Logger LOG = LoggerFactory.getLogger(RecheckTask.class);

    @Autowired
    private ExecutionTaskService executionTaskService;
    @Autowired
    private TraktTvService traktTvService;
    
    @Value("${trakttv.collection.enabled:false}")
    private boolean collectionEnabled;
    @Value("${trakttv.push.enabled:false}")
    private boolean pushEnabled;
    @Value("${trakttv.pull.enabled:false}")
    private boolean pullEnabled;
    
    @Override
    public String getTaskName() {
        return "trakttv";
    }

    @PostConstruct
    public void init() {
        executionTaskService.registerTask(this);
    }

    @Override
    public void execute(String options) throws Exception {
        LOG.debug("Execute Trakt.TV task");

        if (traktTvService.isExpired()) {
            return;
        }

        // 1. collect
        if (collectionEnabled) {
            traktTvService.collectMovies();
            traktTvService.collectEpisodes();
        } else {
            LOG.debug("Trakt.TV collection is not enabled");
        }
        
        // 2. pull
        if (pullEnabled) {
            traktTvService.pullWatchedMovies();
            traktTvService.pullWatchedEpisodes();
        } else {
            LOG.debug("Trakt.TV pulling is not enabled");
        }

        // 2. push
        if (pushEnabled) {
            traktTvService.pushWatchedMovies();
            traktTvService.pushWatchedEpisodes();
        } else {
            LOG.debug("Trakt.TV pushing is not enabled");
        }

        LOG.debug("Finished Trakt.TV task");
    }
}
