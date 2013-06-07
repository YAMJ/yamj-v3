/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.ArtworkDao;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.dto.QueueDTO;

@Service("artworkStorageService")
public class ArtworkStorageService {

    @Autowired
    private CommonDao commonDao;
    @Autowired
    private ArtworkDao artworkDao;

    @Transactional(propagation = Propagation.REQUIRED)
    public void save(Object entity) {
        this.commonDao.saveEntity(entity);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void update(Object entity) {
        this.commonDao.updateEntity(entity);
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getArtworkQueueForScanning(final int maxResults) {
        final StringBuilder sql = new StringBuilder();
        sql.append("select distinct art.id,art.artwork_type,art.create_timestamp,art.update_timestamp ");
        sql.append("from artwork art ");
        sql.append("left outer join videodata vd on vd.id=art.videodata_id ");
        sql.append("left outer join season sea on sea.id=art.series_id ");
        sql.append("left outer join series ser on ser.id=art.series_id ");
        sql.append("where art.status = 'NEW' ");
        sql.append("and (vd.status is null or vd.status='DONE') ");
        sql.append("and (sea.status is null or sea.status='DONE') ");
        sql.append("and (ser.status is null or ser.status='DONE') ");

        return artworkDao.getArtworkQueue(sql, maxResults);
    }

    @Transactional(readOnly = true)
    public Artwork getRequiredArtwork(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("from Artwork art ");
        sb.append("left outer join fetch art.videoData ");
        sb.append("left outer join fetch art.season ");
        sb.append("left outer join fetch art.series ");
        sb.append("where art.id = :id");

        @SuppressWarnings("unchecked")
        List<Artwork> objects = this.commonDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void errorArtwork(Long id) {
         Artwork artwork = artworkDao.getArtwork(id);
        if (artwork != null) {
            artwork.setStatus(StatusType.ERROR);
            artworkDao.updateEntity(artwork);
        }
    }
}
