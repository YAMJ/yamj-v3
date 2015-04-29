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

import java.util.List;
import java.util.ListIterator;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.api.options.OptionsPlayer;
import org.yamj.core.database.model.player.PlayerInfo;
import org.yamj.core.database.model.player.PlayerPath;
import org.yamj.core.hibernate.HibernateDao;

@Transactional
@Repository("playerDao")
public class PlayerDao extends HibernateDao {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerDao.class);

    /**
     * Get single player information using player name
     *
     * @param name
     * @return
     */
    public PlayerInfo getPlayerInfo(String name) {
        return (PlayerInfo) currentSession().byNaturalId(PlayerInfo.class)
                .using("name", name)
                .load();
    }

    /**
     * Get single player information using the ID
     *
     * @param id
     * @return
     */
    public PlayerInfo getPlayerInfo(Long id) {
        return (PlayerInfo) currentSession().byId(PlayerInfo.class)
                .load(id);
    }

    /**
     * Get a list of the players
     *
     * @return
     */
    public List<PlayerInfo> getPlayerList() {
        Session session = currentSession();
        Criteria criteria = session.createCriteria(PlayerInfo.class);
        // http://stackoverflow.com/a/4645549/443283
        criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    public List<PlayerInfo> getPlayerList(OptionsPlayer options) {
        Criteria criteria = currentSession().createCriteria(PlayerInfo.class);

        MatchMode mode;
        if (StringUtils.equalsIgnoreCase("START", options.getMode())) {
            mode = MatchMode.START;
        } else if (StringUtils.equalsIgnoreCase("END", options.getMode())) {
            mode = MatchMode.END;
        } else if (StringUtils.equalsIgnoreCase("EXACT", options.getMode())) {
            mode = MatchMode.EXACT;
        } else {
            // Default to ANY
            mode = MatchMode.ANYWHERE;
        }
        criteria.add(Restrictions.ilike("name", options.getSearch(), mode));
        return criteria.list();
    }

    /**
     * Save the player information
     *
     * @param player
     */
    public void storePlayer(PlayerInfo player) {
        storeEntity(player);
    }

    /**
     * Delete keys from the database
     *
     * @param playerId
     */
    public void deletePlayer(Long playerId) {
        PlayerInfo player = getById(PlayerInfo.class, playerId);
        LOG.debug("Deleting player '{}'", player.toString());
        deleteEntity(player);
        LOG.debug("Successfully deleted {}-'{}'", playerId, player.getName());
    }

    /**
     * Delete a path from a player
     *
     * @param playerId
     * @param pathId
     */
    public void deletePlayerPath(Long playerId, Long pathId) {
        LOG.info("Attempting to delete path ID {} from player ID {}", pathId, playerId);
        PlayerInfo player = getById(PlayerInfo.class, playerId);

        ListIterator<PlayerPath> iter = player.getPaths().listIterator();
        while (iter.hasNext()) {
            PlayerPath path = iter.next();
            if (path.getId() == pathId) {
                LOG.info("Deleting path: {}", path.toString());
                iter.remove();
                break;
            }
        }

        // Update the Player record
        storePlayer(player);
    }

}
