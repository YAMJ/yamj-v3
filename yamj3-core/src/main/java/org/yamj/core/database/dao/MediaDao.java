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
package org.yamj.core.database.dao;

import static org.yamj.core.database.Literals.LITERAL_NAME;
import java.util.List;
import org.hibernate.type.StringType;
import org.springframework.stereotype.Repository;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.builder.SqlScalars;
import org.yamj.core.api.model.dto.ApiNameDTO;
import org.yamj.core.api.options.OptionsSingleType;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.database.model.MediaFile;
import org.yamj.core.hibernate.HibernateDao;

@Repository("mediaDao")
public class MediaDao extends HibernateDao {

    public MediaFile getMediaFile(String fileName) {
        return currentSession().byNaturalId(MediaFile.class).using("fileName", fileName).load();
    }

    public List<ApiNameDTO> getVideoSources(ApiWrapperList<ApiNameDTO> wrapper) {
        OptionsSingleType options = (OptionsSingleType) wrapper.getOptions();

        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT DISTINCT mf.video_source as name FROM mediafile mf ");
        
        if (options.getType() != null) {
            sqlScalars.addToSql("JOIN mediafile_videodata mv ON mf.id=mv.mediafile_id ");
            if (MetaDataType.MOVIE == options.getType()) {
                sqlScalars.addToSql("JOIN videodata vd ON vd.id=mv.videodata_id and vd.episode < 0 ");
            } else {
                sqlScalars.addToSql("JOIN videodata vd ON vd.id=mv.videodata_id and vd.episode > -1 ");
            }
        } 
        
        sqlScalars.addToSql("WHERE mf.video_source is not null ");
        sqlScalars.addToSql(options.getSortString("mf.video_source"));
        
        sqlScalars.addScalar(LITERAL_NAME, StringType.INSTANCE);

        return executeQueryWithTransform(ApiNameDTO.class, sqlScalars, wrapper);
    }
}
