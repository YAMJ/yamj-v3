/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package org.yamj.core.database.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.model.StageFile;

@Service("commonStorageService")
public class CommonStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(CommonStorageService.class);
    
    @Autowired
    private CommonDao commonDao;

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<StageFile> getStageFilesToDelete() {
        final StringBuilder sb = new StringBuilder();
        sb.append("select f from StageFile f ");
        sb.append("where f.status = :deleted " );

        Map<String,Object> params = Collections.singletonMap("deleted", (Object)StatusType.DELETED);
        return commonDao.findByNamedParameters(sb, params);
    }
    
    /**
     * Delete a stage file and all associated entities.
     * 
     * @param stageFile
     * @return list of cached file names which must be deleted also
     */
    @Transactional
    public Set<String> deleteStageFile(StageFile stageFile) {
        if (StatusType.DELETED != stageFile.getStatus()) {
            // status must still be DELETED
            return Collections.emptySet();
        }
        
        LOG.debug("Delete: {}", stageFile);
        
        Set<String> filesToDelete;
        switch(stageFile.getFileType()) {
            case VIDEO:
                filesToDelete = this.deleteVideoStageFile(stageFile);
                break;
            case IMAGE:
                filesToDelete = this.deleteImageStageFile(stageFile);
                break;
            default:
                this.deleteCommonStageFile(stageFile);
                filesToDelete = Collections.emptySet();
                break;
        }
        return filesToDelete;
    }

    private Set<String> deleteVideoStageFile(StageFile stageFile) {
        // TODO needs implementation
        
        return Collections.emptySet();
    }

    private Set<String> deleteImageStageFile(StageFile stageFile) {
        // TODO needs implementation
        
        return Collections.emptySet();
    }
    
    private void deleteCommonStageFile(StageFile stageFile) {
        // just delete the stage file
        this.commonDao.deleteEntity(stageFile);
    }
}