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
package org.yamj.core.database.dao;

import java.util.List;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.springframework.stereotype.Service;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.model.Parameters;
import org.yamj.core.api.model.SqlScalars;
import org.yamj.core.database.model.*;
import org.yamj.core.hibernate.HibernateDao;

@Service("commonDao")
public class CommonDao extends HibernateDao {

    public Genre getGenre(String name) {
        return getByName(Genre.class, name);
    }

    public List<Genre> getGenres(Parameters params) {
        return getList(Genre.class, params);
    }

    public List<Genre> getGenreFilename(ApiWrapperList<Genre> wrapper, String filename) {
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT g.id, g.name");
        sqlScalars.addToSql("FROM mediafile m, mediafile_videodata mv, videodata v, videodata_genres vg, genre g");
        sqlScalars.addToSql("WHERE m.id=mv.mediafile_id");
        sqlScalars.addToSql("AND mv.videodata_id=v.id");
        sqlScalars.addToSql("AND v.id = vg.data_id");
        sqlScalars.addToSql("AND vg.genre_id=g.id");
        sqlScalars.addToSql("AND m.file_name=:filename");

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("name", StringType.INSTANCE);

        sqlScalars.addParameter("filename", filename);

        return executeQueryWithTransform(Genre.class, sqlScalars, wrapper);
    }

    public Certification getCertification(String name) {
        return getByName(Certification.class, name);
    }

    public List<Certification> getCertifications(Parameters params) {
        return getList(Certification.class, params);
    }

    public BoxedSet getBoxedSet(String name) {
        return getByName(BoxedSet.class, name);
    }

    public List<BoxedSet> getBoxedSets(Parameters params) {
        return getList(BoxedSet.class, params);
    }

    public Studio getStudio(String name) {
        return getByName(Studio.class, name);
    }

    public List<Studio> getStudios(Parameters params) {
        return getList(Studio.class, params);
    }
}
