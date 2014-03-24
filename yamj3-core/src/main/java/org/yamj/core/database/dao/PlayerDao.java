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
package org.yamj.core.database.dao;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.type.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.api.model.builder.SqlScalars;
import org.yamj.core.api.options.OptionsPlayer;
import org.yamj.core.database.model.PlayerPathOld;
import org.yamj.core.database.model.player.PlayerInfo;
import org.yamj.core.database.model.player.PlayerPath;
import org.yamj.core.hibernate.HibernateDao;

@Service("playerDao")
public class PlayerDao extends HibernateDao {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerDao.class);

    @Transactional
    public void storePlayer(List<PlayerPathOld> playerList) {
        for (PlayerPathOld player : playerList) {
            storePlayer(player);
        }
    }

    @Transactional
    public void storePlayer(PlayerPathOld player) {

        LOG.debug("Checking for existing information on player '{}'", player.getName());
        PlayerPathOld existingPlayer = getByName(PlayerPathOld.class, player.getName());

        if (existingPlayer != null) {
            // Player already exists
            LOG.debug("Updating player information: '{}'", player.getName());
            existingPlayer.setIpDevice(player.getIpDevice());
            existingPlayer.setStoragePath(player.getStoragePath());
            updateEntity(existingPlayer);
        } else {
            LOG.debug("Storing new player: '{}'", player.getName());
            storeEntity(player);
        }
    }

    @Transactional
    public void storePlayer(PlayerInfo player) {
        LOG.debug("Checking for existing information on player '{}'", player.getName());
        PlayerInfo existingPlayer = getByName(PlayerInfo.class, player.getName());

        if (existingPlayer != null) {
            // Player already exists
            LOG.debug("Updating player information: '{}'", player.getName());
            existingPlayer.setIpAddress(player.getIpAddress());
            existingPlayer.clearPaths();
            for(PlayerPath path:player.getPaths()) {
                existingPlayer.addPath(path);
            }

            updateEntity(existingPlayer);
        } else {
            LOG.debug("Storing new player: '{}'", player.getName());
            storeEntity(player);
        }
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public List<PlayerInfo> getPlayerInfo(OptionsPlayer options) {
        Session session = getSession();
        Criteria criteria = session.createCriteria(PlayerInfo.class);
        return criteria.list();
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public List<PlayerPathOld> getPlayerEntries(OptionsPlayer options) {
        SqlScalars sqlScalars = new SqlScalars();

        sqlScalars.addToSql("SELECT name, ip_device AS ipDevice, storage_path AS storagePath");
        sqlScalars.addToSql("FROM player_path");
        // TODO: Add where clause
        sqlScalars.addToSql(options.getSearchString(true));
        sqlScalars.addToSql(options.getSortString());

        sqlScalars.addScalar("name", StringType.INSTANCE);
        sqlScalars.addScalar("ipDevice", StringType.INSTANCE);
        sqlScalars.addScalar("storagePath", StringType.INSTANCE);

        List<PlayerPathOld> players = executeQueryWithTransform(PlayerPathOld.class, sqlScalars, null);
        return players;
    }

    @Transactional
    public List<PlayerPathOld> getPlayerEntries(String player) {
        OptionsPlayer options = new OptionsPlayer();
        options.setPlayer(player);
        // Make the search exact
        options.setMode("EXACT");
        return getPlayerEntries(options);
    }

    /**
     * Delete keys from the database
     *
     * @param name
     */
    @Transactional
    public void deletePlayer(String name) {
        if (StringUtils.isNotBlank(name)) {
            PlayerPathOld player = getByName(PlayerPathOld.class, name);
            LOG.debug("Deleting player '{}'", player.toString());
            deleteEntity(player);
            LOG.debug("Successfully deleted '{}'", name);
        }
    }
}
