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
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.api.model.builder.SqlScalars;
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

    public void savePlayer(PlayerInfo player) {
        storeEntity(player);
    }

    public void storePlayer(List<PlayerInfo> playerList) {
        for (PlayerInfo player : playerList) {
            storePlayer(player);
        }
    }

    public void storePlayer(PlayerInfo player) {
        LOG.debug("Checking for existing information on player '{}'", player.getName());
        PlayerInfo existingPlayer = getPlayerInfo(player.getName());

        if (existingPlayer != null) {
            // Player already exists
            LOG.debug("Updating player information: {}-{}", player.getId(), player.getName());
            existingPlayer.setDeviceType(player.getDeviceType());
            existingPlayer.setIpAddress(player.getIpAddress());
            existingPlayer.clearPaths();
            for (PlayerPath path : player.getPaths()) {
                existingPlayer.addPath(path);
                storeEntity(path);
            }

            updateEntity(existingPlayer);
        } else {
            LOG.debug("Storing new player: '{}'", player.getName());
            storeEntity(player);
        }
    }

    @SuppressWarnings("unused")
    public List<PlayerInfo> getPlayerInfo(OptionsPlayer options) {
        Session session = currentSession();
        Criteria criteria = session.createCriteria(PlayerInfo.class);
        return criteria.list();
    }

    public List<PlayerInfo> getPlayerEntries(OptionsPlayer options) {
        SqlScalars sqlScalars = new SqlScalars();

        sqlScalars.addToSql("SELECT id, name, device_type AS deviceType, ip_address AS ipAddress");
        sqlScalars.addToSql("FROM player_info");
        // TODO: Add where clause
        sqlScalars.addToSql(options.getSearchString(true));
        sqlScalars.addToSql(options.getSortString());

        sqlScalars.addScalar("id", IntegerType.INSTANCE);
        sqlScalars.addScalar("name", StringType.INSTANCE);
        sqlScalars.addScalar("deviceType", StringType.INSTANCE);
        sqlScalars.addScalar("ipAddress", StringType.INSTANCE);

        List<PlayerInfo> players = executeQueryWithTransform(PlayerInfo.class, sqlScalars, null);
        return players;
    }

    public List<PlayerInfo> getPlayerEntries(String playerName) {
        OptionsPlayer options = new OptionsPlayer();
        options.setPlayer(playerName);
        // Make the search exact
        options.setMode("EXACT");
        return getPlayerEntries(options);
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
        savePlayer(player);
    }

}
