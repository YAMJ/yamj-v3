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
package org.yamj.core.database.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.model.MediaFile;
import org.yamj.core.database.model.dto.QueueDTO;

@Service("mediaStorageService")
public class MediaStorageService {

    @Autowired
    private CommonDao commonDao;

    @Transactional
    public void update(Object entity) {
        this.commonDao.updateEntity(entity);
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getMediaFileQueue(final int maxResults) {
        return commonDao.getQueueIdOnly(MediaFile.QUERY_QUEUE, maxResults);
    }

    @Transactional(readOnly = true)
    public MediaFile getRequiredMediaFile(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("from MediaFile mf ");
        sb.append("left outer join fetch mf.audioCodecs ");
        sb.append("left outer join fetch mf.subtitles ");
        sb.append("left outer join fetch mf.stageFiles ");
        sb.append("where mf.id = :id" );

        List<MediaFile> objects = this.commonDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional
    public void errorMediaFile(Long id) {
        MediaFile mediaFile = commonDao.getById(MediaFile.class, id);
        if (mediaFile != null) {
            mediaFile.setStatus(StatusType.ERROR);
            commonDao.updateEntity(mediaFile);
        }
    }
    
    @Transactional
    public void updateMediaFile(MediaFile mediaFile) {
        commonDao.storeAll(mediaFile.getAudioCodecs());
        commonDao.updateEntity(mediaFile);
    }
}
