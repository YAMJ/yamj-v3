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
package org.yamj.core.pages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.yamj.core.database.model.player.PlayerInfo;
import org.yamj.core.database.model.player.PlayerPath;
import org.yamj.core.database.service.JsonApiStorageService;

@Controller
@RequestMapping("/player")
public class PlayerPagesController extends AbstractPagesController {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerPagesController.class);
    
    @Autowired
    private JsonApiStorageService jsonApi;
    
    @RequestMapping("/list")
    public ModelAndView playerList() {
        ModelAndView view = withInfo(new ModelAndView("player/player-list"));
        view.addObject("playerlist", jsonApi.getPlayerList());
        return view;
    }

    @RequestMapping("/add")
    public ModelAndView playerAddPage() {
        ModelAndView view = withInfo(new ModelAndView("player/player-add"));
        view.addObject("player", new PlayerInfo());
        return view;
    }

    @RequestMapping("/add/process")
    public ModelAndView playerAdd(@ModelAttribute PlayerInfo player) {
        ModelAndView view = new ModelAndView("redirect:/player/list");

        LOG.info("Adding player: {}", player.toString());
        jsonApi.storePlayer(player);
        LOG.info("Player was successfully added.");

        return view;
    }

    @RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
    public ModelAndView playerEditPage(@PathVariable Long id) {
        ModelAndView view = withInfo(new ModelAndView("player/player-edit"));
        view.addObject("player", jsonApi.getPlayerInfo(id));
        return view;
    }

    @RequestMapping(value = "/edit/{id}", method = RequestMethod.POST)
    public ModelAndView playerEditUpdate(@PathVariable Long id, @ModelAttribute("player") PlayerInfo player) {
        ModelAndView view = new ModelAndView("redirect:/player/list");

        PlayerInfo exisiting = jsonApi.getPlayerInfo(id);
        LOG.info("Updating player: {}-{}", exisiting.getId(), exisiting.getName());
        exisiting.setDeviceType(player.getDeviceType());
        exisiting.setIpAddress(player.getIpAddress());
        jsonApi.storePlayer(exisiting);
        LOG.info("Player was successfully edited.");
        return view;
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    public ModelAndView playerDelete(@PathVariable Long id) {
        ModelAndView view = new ModelAndView("redirect:/player/list");

        LOG.info("Deleting player '{}'", id);
        jsonApi.deletePlayer(id);
        LOG.info("Player was successfully deleted.");
        return view;
    }

    @RequestMapping(value = "/details/{id}", method = RequestMethod.GET)
    public ModelAndView playerDetails(@PathVariable Long id) {
        ModelAndView view = withInfo(new ModelAndView("player/player-details"));

        PlayerInfo player = jsonApi.getPlayerInfo(id);
        view.addObject("player", player);
        view.addObject("pathlist", player.getPaths());

        return view;
    }

    @RequestMapping(value = "/scan", method = RequestMethod.GET)
    public ModelAndView playerScan(ModelAndView view) {
        LOG.info("Player Scan");
        //TODO: Add the scan
        return view;
    }

    @RequestMapping(value = "/scan", method = RequestMethod.POST)
    public ModelAndView playerScanned() {
        LOG.info("Player Scanning...");
        //TODO: Add the scan

        ModelAndView view = withInfo(new ModelAndView("player/player-scan"));
        view.addObject("playerlist", jsonApi.getPlayerList());
        return view;
    }

    @RequestMapping(value = "/add-path/{id}", method = RequestMethod.GET)
    public ModelAndView playerAddPath(@PathVariable Long id) {
        ModelAndView view = withInfo(new ModelAndView("player/player-path-add"));
        view.addObject("player", jsonApi.getPlayerInfo(id));
        view.addObject("playerPath", new PlayerPath());
        return view;
    }

    @RequestMapping(value = "/add-path/process/{id}", method = RequestMethod.POST)
    public ModelAndView playerAddPath(@PathVariable Long id, @ModelAttribute PlayerPath playerPath) {
        ModelAndView view = new ModelAndView("redirect:/player/details/" + id);
        
        LOG.info("Updating player '{}' with new path: {}", id, playerPath.toString());
        jsonApi.storePlayerPath(id, playerPath);
        LOG.info("Player was successfully updated");
        
        return view;
    }

    @RequestMapping(value = "/delete-path/{playerId}/{pathId}", method = RequestMethod.GET)
    public ModelAndView playerDeletePath(@PathVariable Long playerId, @PathVariable Long pathId) {
        ModelAndView view = new ModelAndView("redirect:/player/details/" + playerId);

        LOG.info("Deleting path '{}' for player '{}'", pathId, playerId);
        jsonApi.deletePlayerPath(playerId, pathId);
        LOG.info("Path was successfully deleted");
        
        return view;
    }

    @RequestMapping(value = "/edit-path/{playerId}/{pathId}", method = RequestMethod.GET)
    public ModelAndView playerEditPath(@PathVariable Long playerId, @PathVariable Long pathId) {
        ModelAndView view = withInfo(new ModelAndView("player/player-path-edit"));
        
        PlayerInfo player = jsonApi.getPlayerInfo(playerId);
        view.addObject("player", player);

        for (PlayerPath path : player.getPaths()) {
            if (path.getId() == pathId) {
                view.addObject("path", path);
                break;
            }
        }

        return view;
    }

    @RequestMapping(value = "/edit-path/{playerId}/{pathId}", method = RequestMethod.POST)
    public ModelAndView playerEditPath(@PathVariable Long playerId, @PathVariable Long pathId, @ModelAttribute("path") PlayerPath playerPath) {
        ModelAndView view = new ModelAndView("redirect:/player/details/" + playerId);

        LOG.info("Updating player path: {}-{}", playerId, pathId);
        this.jsonApi.storePlayerPath(playerId, pathId, playerPath);
        LOG.info("Path was successfully updated");
        
        return view;
    }
}
