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
package org.yamj.core.scheduling;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yamj.core.service.delete.DeletionService;

@Component
public class DeletionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(DeletionScheduler.class);
    
    @Autowired
    private DeletionService deletionService;
    
    private final AtomicBoolean watchProcess = new AtomicBoolean(false);

    public void triggerProcess() {
        LOG.trace("Trigger deletion process");
        watchProcess.set(true);
    }

    @Async
    @Scheduled(initialDelay = 2000, fixedDelay = 1000)
    public void runProcess() {
        if (watchProcess.getAndSet(false)) deletionService.executeAllDeletions();
    }
}
