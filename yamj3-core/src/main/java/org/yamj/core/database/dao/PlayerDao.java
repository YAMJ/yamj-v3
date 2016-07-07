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

import static org.hibernate.criterion.CriteriaSpecification.DISTINCT_ROOT_ENTITY;
import static org.yamj.core.database.Literals.LITERAL_NAME;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.yamj.core.api.options.OptionsPlayer;
import org.yamj.core.database.model.player.PlayerInfo;
import org.yamj.core.hibernate.HibernateDao;

@Repository("playerDao")
public class PlayerDao extends HibernateDao {

    public List<PlayerInfo> getPlayerList() {
        Criteria criteria = currentSession().createCriteria(PlayerInfo.class);
        // http://stackoverflow.com/a/4645549/443283
        criteria.setResultTransformer(DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    public List<PlayerInfo> getPlayerList(OptionsPlayer options) {
        Criteria criteria = currentSession().createCriteria(PlayerInfo.class);
        criteria.setResultTransformer(DISTINCT_ROOT_ENTITY);
        
        if (StringUtils.isNotBlank(options.getPlayerName())) { 
            MatchMode mode;
            if ("START".equalsIgnoreCase(options.getMode())) {
                mode = MatchMode.START;
            } else if ("END".equalsIgnoreCase(options.getMode())) {
                mode = MatchMode.END;
            } else if ("EXACT".equalsIgnoreCase(options.getMode())) {
                mode = MatchMode.EXACT;
            } else {
                // Default to ANY
                mode = MatchMode.ANYWHERE;
            }
            criteria.add(Restrictions.ilike(LITERAL_NAME, options.getSearch(), mode));
        }
        
        return criteria.list();
    }
}
