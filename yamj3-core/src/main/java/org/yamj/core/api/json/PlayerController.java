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
package org.yamj.core.api.json;

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
import org.yamj.core.database.model.PlayerPath;
import org.yamj.core.database.service.JsonApiStorageService;

@Controller
@RequestMapping("/api/player/**")
public class PlayerController {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerController.class);
    @Autowired
    private JsonApiStorageService api;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<PlayerPath> playerList(@ModelAttribute("player") OptionsPlayer options) {
        ApiWrapperList<PlayerPath> wrapper = new ApiWrapperList<PlayerPath>();

        // If not mode is specified, make it exact
        if (StringUtils.isBlank(options.getMode())) {
            options.setMode("EXACT");
        }
        wrapper.setOptions(options);
        wrapper.setResults(api.getPlayer(wrapper));
        wrapper.setStatusCheck();

        return wrapper;
    }

    @RequestMapping(value = "/add", method = RequestMethod.GET)
    @ResponseBody
    public ApiStatus playerAdd(
            @RequestParam(required = true, defaultValue = "") String name,
            @RequestParam(required = true, defaultValue = "") String ipDevice,
            @RequestParam(required = true, defaultValue = "") String storagePath) {

        ApiStatus status = new ApiStatus();
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(ipDevice) && StringUtils.isNotBlank(storagePath)) {
            LOG.info("Storing player '{}'", name);
            PlayerPath pp = new PlayerPath();
            pp.setName(name);
            pp.setIpDevice(ipDevice);
            pp.setStoragePath(storagePath);
            api.setPlayer(pp);
            status.setStatus(200);
            status.setMessage("Successfully added '" + name + "'");
        } else {
            status.setStatus(400);
            status.setMessage("Invalid player information specified, player not added");
        }
        return status;
    }

    @RequestMapping(value = "/delete", method = RequestMethod.GET)
    @ResponseBody
    public ApiStatus playerDelete(
            @RequestParam(required = true, defaultValue = "") String player) {
        ApiStatus status = new ApiStatus();
        if (StringUtils.isNotBlank(player)) {
            LOG.info("Deleting player '{}'", player);
            api.deletePlayer(player);
            status.setStatus(200);
            status.setMessage("Successfully deleted '" + player + "'");
        } else {
            status.setStatus(400);
            status.setMessage("Invalid name specified, player not deleted");
        }
        return status;
    }

    @RequestMapping(value = "/update", method = RequestMethod.GET)
    @ResponseBody
    public ApiStatus playerUpdate(
            @RequestParam(required = true, defaultValue = "") String name,
            @RequestParam(required = true, defaultValue = "") String ipDevice,
            @RequestParam(required = true, defaultValue = "") String storagePath) {
        ApiStatus status = new ApiStatus();
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(ipDevice) && StringUtils.isNotBlank(storagePath)) {
            LOG.info("Updating player '{}'", name);
            PlayerPath pp = new PlayerPath();
            pp.setName(name);
            pp.setIpDevice(ipDevice);
            pp.setStoragePath(storagePath);
            api.setPlayer(pp);
            status.setStatus(200);
            status.setMessage("Successfully updated '" + name + "'");
        } else {
            status.setStatus(400);
            status.setMessage("Invalid player information specified, player not updated");
        }
        return status;
    }

}
