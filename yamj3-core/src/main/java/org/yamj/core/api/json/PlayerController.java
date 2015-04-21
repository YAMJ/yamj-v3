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
package org.yamj.core.api.json;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.options.OptionsPlayer;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.database.model.player.PlayerInfo;
import org.yamj.core.database.model.player.PlayerPath;
import org.yamj.core.database.service.JsonApiStorageService;

@Controller
@ResponseBody
@RequestMapping(value = "/api/player/**", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
public class PlayerController {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerController.class);
    @Autowired
    private JsonApiStorageService api;

    /**
     * Get a list of the players
     *
     * @param options
     * @return
     */
    @RequestMapping("/list")
    public ApiWrapperList<PlayerInfo> playerList(@ModelAttribute("player") OptionsPlayer options) {
        ApiWrapperList<PlayerInfo> wrapper = new ApiWrapperList<>();

        // If not mode is specified, make it exact
        if (StringUtils.isBlank(options.getMode())) {
            options.setMode("EXACT");
        }
        wrapper.setOptions(options);
        wrapper.setResults(api.getPlayer(wrapper));
        wrapper.setStatusCheck();

        return wrapper;
    }

    /**
     * Add a new player
     *
     * @param name
     * @param deviceType
     * @param ipAddress
     * @return
     */
    @RequestMapping("/add")
    public ApiStatus playerAdd(
            @RequestParam(required = true, defaultValue = "") String name,
            @RequestParam(required = true, defaultValue = "") String deviceType,
            @RequestParam(required = true, defaultValue = "") String ipAddress) {

        ApiStatus status = new ApiStatus();
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(ipAddress) && StringUtils.isNotBlank(deviceType)) {
            LOG.info("Storing player '{}'", name);
            PlayerInfo player = new PlayerInfo();
            player.setName(name);
            player.setDeviceType(deviceType);
            player.setIpAddress(ipAddress);
            api.setPlayer(player);
            status.setStatus(200);
            status.setMessage("Successfully added '" + name + "'");
        } else {
            status.setStatus(400);
            status.setMessage("Invalid player information specified, player not added");
        }
        return status;
    }

    /**
     * Delete a player
     *
     * @param playerName
     * @return
     */
    @RequestMapping("/delete")
    public ApiStatus playerDelete(
            @RequestParam(required = true, defaultValue = "") String playerName) {
        ApiStatus status = new ApiStatus();
        if (StringUtils.isNotBlank(playerName)) {
            LOG.info("Deleting player '{}'", playerName);
            api.deletePlayer(playerName);
            status.setStatus(200);
            status.setMessage("Successfully deleted '" + playerName + "'");
        } else {
            status.setStatus(400);
            status.setMessage("Invalid name specified, player not deleted");
        }
        return status;
    }

    /**
     * Update a player
     *
     * @param name
     * @param deviceType
     * @param ipAddress
     * @return
     */
    @RequestMapping("/update")
    public ApiStatus playerUpdate(
            @RequestParam(required = true, defaultValue = "") String name,
            @RequestParam(required = true, defaultValue = "") String deviceType,
            @RequestParam(required = true, defaultValue = "") String ipAddress) {
        ApiStatus status = new ApiStatus();
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(ipAddress) && StringUtils.isNotBlank(deviceType)) {
            LOG.info("Updating player '{}'", name);
            PlayerInfo player = new PlayerInfo();
            player.setName(name);
            player.setDeviceType(deviceType);
            player.setIpAddress(ipAddress);
            api.setPlayer(player);
            status.setStatus(200);
            status.setMessage("Successfully updated '" + name + "'");
        } else {
            status.setStatus(400);
            status.setMessage("Invalid player information specified, player not updated");
        }
        return status;
    }

    @RequestMapping("/scan")
    public void playerScan() {
        List<PlayerInfo> players = getDummyPlayers(2, 3);

        for (PlayerInfo player : players) {
            LOG.info("Storing player: {}", player);
            api.storePlayer(player);
        }
        LOG.info("Player storage completed");
    }

    private static List<PlayerInfo> getDummyPlayers(int playerCount, int pathCount) {
        List<PlayerInfo> players = new ArrayList<>();

        for (int loopPlayer = 1; loopPlayer <= playerCount; loopPlayer++) {
            PlayerInfo p = new PlayerInfo();
            p.setIpAddress("192.168.0." + loopPlayer);
            p.setName("PCH-C200-" + loopPlayer);
            p.setDeviceType("network");

            for (int loopPath = 1; loopPath <= pathCount; loopPath++) {
                PlayerPath pp = new PlayerPath();
                pp.setSourcePath("http://some.path/" + loopPlayer + "-" + loopPath + "/");
                pp.setTargetPath("http://some.path/" + loopPlayer + "-" + loopPath + "/");
                p.addPath(pp);
            }
            players.add(p);
        }

        return players;
    }
}
