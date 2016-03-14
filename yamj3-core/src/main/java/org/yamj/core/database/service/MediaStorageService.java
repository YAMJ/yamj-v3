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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        List<MediaFile> objects = this.commonDao.namedQueryById(MediaFile.QUERY_REQUIRED, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional
    public void errorMediaFile(Long id) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("status", StatusType.ERROR);
        commonDao.executeUpdate(MediaFile.UPDATE_STATUS, params);
    } 
    
    @Transactional
    public void updateMediaFile(MediaFile mediaFile) {
        commonDao.storeAll(mediaFile.getAudioCodecs());
        commonDao.updateEntity(mediaFile);
    }
}
