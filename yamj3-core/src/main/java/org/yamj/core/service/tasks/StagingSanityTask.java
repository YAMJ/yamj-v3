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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yamj.core.scheduling.DeletionScheduler;
import org.yamj.core.service.various.StagingService;

/**
 * Task for checking artwork sanity.
 */
@Component
public class StagingSanityTask implements ITask {

    private static final Logger LOG = LoggerFactory.getLogger(StagingSanityTask.class);

    @Value("${yamj3.check.stagingSanity:false}")
    private boolean checkStagingSanity;

    @Autowired
    private ExecutionTaskService executionTaskService;
    @Autowired
    private StagingService stagingService;
    @Autowired
    private DeletionScheduler deletionScheduler;
    
    @Override
    public String getTaskName() {
        return "stagingsanity";
    }

    @PostConstruct
    public void init() {
        executionTaskService.registerTask(this);
    }

    @Override
    public void execute(String options) throws Exception {
        if (!checkStagingSanity) {
            // staging sanity is disabled
            return;
        }
        
        LOG.debug("Execute staging sanity task");
        final long startTime = System.currentTimeMillis();

        try {
            List<Long> stageFileIds = new ArrayList<>();
            for (Long rootId : this.stagingService.getRootDirectories()) {
                stageFileIds.addAll(this.stagingService.findNotExistingStageFiles(rootId));
            }
            
            if (!stageFileIds.isEmpty()) {
                LOG.info("Found {} not existing stage files", stageFileIds.size());
                this.stagingService.markStageFilesAsDeleted(stageFileIds);

                // trigger deletion run
                deletionScheduler.trigger();
            }
        } catch (Exception ex) {
            LOG.error("Failed staging sanity check", ex);
        }

        LOG.debug("Finished staging sanity task after {} ms", System.currentTimeMillis()-startTime);
    }
}
