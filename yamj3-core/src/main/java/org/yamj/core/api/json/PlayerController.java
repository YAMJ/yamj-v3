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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.options.OptionsPlayer;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.database.model.player.PlayerInfo;
import org.yamj.core.database.model.player.PlayerPath;
import org.yamj.core.database.service.JsonApiStorageService;

@RestController
@RequestMapping(value = "/api/player/**", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
public class PlayerController {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerController.class);
    @Autowired
    private JsonApiStorageService jsonApi;

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
        wrapper.setResults(jsonApi.getPlayerList(options));
        wrapper.setStatusCheck();

        return wrapper;
    }

    /**
     * Store a new player
     *
     * @param name
     * @param deviceType
     * @param ipAddress
     * @return
     */
    @RequestMapping("/store")
    public ApiStatus playerStore(
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
            jsonApi.storePlayer(player);
            status.setStatus(200);
            status.setMessage("Successfully stored '" + name + "'");
        } else {
            status.setStatus(400);
            status.setMessage("Invalid player information specified; player not stored");
        }
        return status;
    }

    /**
     * Delete a player
     *
     * @param playerId
     * @return
     */
    @RequestMapping("/delete")
    public ApiStatus playerDelete(
            @RequestParam(required = true, defaultValue = "") Long playerId) {
        ApiStatus status = new ApiStatus();
        if (playerId != null && playerId > 0) {
            LOG.info("Deleting player '{}'", playerId);
            jsonApi.deletePlayer(playerId);
            status.setStatus(200);
            status.setMessage("Successfully deleted '" + playerId + "'");
        } else {
            status.setStatus(400);
            status.setMessage("Invalid player ID specified; player not deleted");
        }
        return status;
    }

    /**
     * Store a player path.
     *
     * @param playerId
     * @param sourcePath
     * @param targetPath
     * @return the api status
     */
    @RequestMapping("/path/store")
    public ApiStatus playerPathStore(
        @RequestParam(required = true, defaultValue = "") Long playerId,
        @RequestParam(required = true, defaultValue = "") String sourcePath,
        @RequestParam(required = true, defaultValue = "") String targetPath)
    {
        ApiStatus status = new ApiStatus();
        if (playerId == null || playerId <= 0) {
            status.setStatus(400);
            status.setMessage("Invalid player ID specified; player path not stored");
        } else if (StringUtils.isBlank(sourcePath)) {
            status.setStatus(400);
            status.setMessage("Invalid source path; player path not stored");
        } else if (StringUtils.isBlank(targetPath)) {
            status.setStatus(400);
            status.setMessage("Invalid target path; player path not stored");
        } else {
            PlayerPath playerPath = new PlayerPath();
            playerPath.setSourcePath(sourcePath);
            playerPath.setTargetPath(targetPath);
            boolean stored = jsonApi.storePlayerPath(playerId, playerPath);
            if (stored) {
                status.setStatus(200);
                status.setMessage("Successfully stored player path for player " + playerId);
            } else {
                status.setStatus(400);
                status.setMessage("Player "+ playerId + " does not exist; player path not stored");
            }
        }
        return status;
    }

    /**
     * Remove a player path.
     *
     * @param playerId
     * @param pathId
     */
    @RequestMapping("/path/delete")
    public ApiStatus playerPathDelete(
        @RequestParam(required = true, defaultValue = "") Long playerId,
        @RequestParam(required = true, defaultValue = "") Long pathId)
    {
        ApiStatus status = new ApiStatus();
        if (playerId == null || playerId <= 0) {
            status.setStatus(400);
            status.setMessage("Invalid player ID specified, player path not removed");
        } else if (pathId == null || pathId <= 0) {
            status.setStatus(400);
            status.setMessage("Invalid path ID specified, player path not removed");
        } else {
            boolean deleted = jsonApi.deletePlayerPath(playerId, pathId);
            if (deleted) {
                status.setStatus(200);
                status.setMessage("Successfully removed player path '" + pathId + "' from player '" + playerId + "'");
            } else {
                status.setStatus(400);
                status.setMessage("Given player id or path id does not exist; player path not deleted");
            }
        }
        return status;
    }
}
