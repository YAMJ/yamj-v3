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

import static org.yamj.core.tools.Constants.ALL;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.yamj.common.model.YamjInfo;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.api.json.IndexController;
import org.yamj.core.api.json.SystemInfoController;
import org.yamj.core.api.model.CountGeneric;
import org.yamj.core.api.model.Skin;
import org.yamj.core.api.options.OptionsConfig;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.Configuration;
import org.yamj.core.database.model.player.PlayerInfo;
import org.yamj.core.database.model.player.PlayerPath;
import org.yamj.core.database.service.JsonApiStorageService;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.StorageType;

@Controller
public class PagesController {

    private static final Logger LOG = LoggerFactory.getLogger(PagesController.class);
    @Autowired
    private SystemInfoController sic;
    @Autowired
    private ConfigService configService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private IndexController index;
    @Autowired
    private JsonApiStorageService jsonApi;

    @RequestMapping(value = {"/", "/index"})
    public ModelAndView displayRoot() {
        ModelAndView view = new ModelAndView("index");
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        return view;
    }

    /**
     * For testing pages
     *
     * @param name
     * @return
     */
    @RequestMapping("/test/{name}")
    public ModelAndView displayTest(@PathVariable String name) {
        ModelAndView view = new ModelAndView("test-" + name);
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        return view;
    }

    //<editor-fold defaultstate="collapsed" desc="System Info Page">
    @RequestMapping("/system-info")
    public ModelAndView displaySystemInfo() {
        ModelAndView view = new ModelAndView("system-info");
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        view.addObject("countlist", yi.getCounts());
        return view;
    }
    //</editor-fold>

    @RequestMapping("/count/job")
    public ModelAndView displayCountJob() {
        ModelAndView view = new ModelAndView("count-job");
        YamjInfo yi = sic.getYamjInfo("false");
        view.addObject("yi", yi);

        List<CountGeneric> jobList = index.getJobs(ALL);
        // Add some wording if there is an empty list
        if (jobList.isEmpty()) {
            CountGeneric noJobs = new CountGeneric();
            noJobs.setItem("No jobs found!");
            noJobs.setCounter(0L);
            jobList.add(noJobs);
        }

        view.addObject("countlist", jobList);
        return view;
    }

    //<editor-fold defaultstate="collapsed" desc="Configuration Pages">
    @RequestMapping("/config/add")
    public ModelAndView configAddPage() {
        ModelAndView view = new ModelAndView("config-add");
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        view.addObject("config", new Configuration());
        return view;
    }

    @RequestMapping("/config/add/process")
    public ModelAndView configAdd(@ModelAttribute Configuration config) {
        ModelAndView view = new ModelAndView("redirect:/config/list");
        LOG.info("Adding config: {}", config.toString());
        configService.setProperty(config.getKey(), config.getValue());
        LOG.info("Configuration was successfully added.");
        return view;
    }

    @RequestMapping("/config/list")
    public ModelAndView configList() {
        ModelAndView view = new ModelAndView("config-list");

        List<Configuration> configList = configService.getConfigurations(new OptionsConfig());
        Collections.sort(configList, new Comparator<Configuration>() {
            @Override
            public int compare(Configuration o1, Configuration o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        view.addObject("configlist", configList);

        return view;
    }

    @RequestMapping(value = "/config/edit/{key}", method = RequestMethod.GET)
    public ModelAndView configEditPage(@PathVariable String key) {
        ModelAndView view = new ModelAndView("config-edit");
        if (StringUtils.isNotBlank(key)) {
            Configuration config = configService.getConfiguration(key);
            if (config != null) {
                view.addObject("config", config);
            }
        }
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        return view;
    }

    @RequestMapping(value = "/config/edit/{key}", method = RequestMethod.POST)
    public ModelAndView configEditUpdate(@ModelAttribute("config") Configuration config) {
        ModelAndView view = new ModelAndView("redirect:/config/list");
        LOG.info("Updating config: {}", config.toString());
        configService.setProperty(config.getKey(), config.getValue());
        LOG.info("Config was successfully edited.");
        return view;
    }

    @RequestMapping(value = "/config/delete/{key}", method = RequestMethod.GET)
    public ModelAndView configDelete(@PathVariable String key) {
        ModelAndView view = new ModelAndView("redirect:/config/list");

        LOG.info("Deleting config for '{}'", key);
        configService.deleteProperty(key);
        LOG.info("Config was successfully deleted.");
        return view;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Player Path Pages">
    @RequestMapping("/player/list")
    public ModelAndView playerList() {
        ModelAndView view = new ModelAndView("player-list");

        List<PlayerInfo> playerList = jsonApi.getPlayerList();
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        view.addObject("playerlist", playerList);

        return view;
    }

    @RequestMapping("/player/add")
    public ModelAndView playerAddPage() {
        ModelAndView view = new ModelAndView("player-add");
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        view.addObject("player", new PlayerInfo());

        return view;
    }

    @RequestMapping("/player/add/process")
    public ModelAndView playerAdd(@ModelAttribute PlayerInfo player) {

        ModelAndView view = new ModelAndView("redirect:/player/list");
        LOG.info("Adding player: {}", player.toString());
        jsonApi.storePlayer(player);
        LOG.info("Player was successfully added.");

        return view;
    }

    @RequestMapping(value = "/player/edit/{id}", method = RequestMethod.GET)
    public ModelAndView playerEditPage(@PathVariable Long id) {
        ModelAndView view = new ModelAndView("player-edit");
        PlayerInfo player = jsonApi.getPlayerInfo(id);
        view.addObject("player", player);
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        return view;
    }

    @RequestMapping(value = "/player/edit/{id}", method = RequestMethod.POST)
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

    @RequestMapping(value = "/player/delete/{id}", method = RequestMethod.GET)
    public ModelAndView playerDelete(@PathVariable Long id) {
        ModelAndView view = new ModelAndView("redirect:/player/list");

        LOG.info("Deleting player '{}'", id);
        jsonApi.deletePlayer(id);
        LOG.info("Player was successfully deleted.");
        return view;
    }

    @RequestMapping(value = "/player/details/{id}", method = RequestMethod.GET)
    public ModelAndView playerDetails(@PathVariable Long id) {
        ModelAndView view = new ModelAndView("player-details");

        PlayerInfo player = jsonApi.getPlayerInfo(id);
        view.addObject("player", player);
        view.addObject("pathlist", player.getPaths());

        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);

        return view;
    }

    @RequestMapping(value = "/player/scan", method = RequestMethod.GET)
    public ModelAndView playerScan(ModelAndView view) {
        LOG.info("Player Scan");
        //TODO: Add the scan
        return view;
    }

    @RequestMapping(value = "/player/scan", method = RequestMethod.POST)
    public ModelAndView playerScanned() {
        LOG.info("Player Scanning...");
        //TODO: Add the scan

        ModelAndView view = new ModelAndView("player-scan");
        List<PlayerInfo> playerList = jsonApi.getPlayerList();

        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        view.addObject("playerlist", playerList);

        return view;
    }

    @RequestMapping(value = "/player/add-path/{id}", method = RequestMethod.GET)
    public ModelAndView playerAddPath(@PathVariable Long id) {
        ModelAndView view = new ModelAndView("player-path-add");

        YamjInfo yi = sic.getYamjInfo("false");
        view.addObject("yi", yi);
        view.addObject("player", jsonApi.getPlayerInfo(id));
        view.addObject("playerPath", new PlayerPath());

        return view;
    }

    @RequestMapping(value = "/player/add-path/process/{id}", method = RequestMethod.POST)
    public ModelAndView playerAddPath(@PathVariable Long id, @ModelAttribute PlayerPath playerPath) {
        ModelAndView view = new ModelAndView("redirect:/player/details/" + id);
        
        LOG.info("Updating player '{}' with new path: {}", id, playerPath.toString());
        jsonApi.storePlayerPath(id, playerPath);
        LOG.info("Player was successfully updated.");
        
        return view;
    }

    @RequestMapping(value = "/player/delete-path/{playerId}/{pathId}", method = RequestMethod.GET)
    public ModelAndView playerDeletePath(@PathVariable Long playerId, @PathVariable Long pathId) {
        ModelAndView view = new ModelAndView("redirect:/player/details/" + playerId);

        LOG.info("Deleting path '{}' for player '{}'", pathId, playerId);
        jsonApi.deletePlayerPath(playerId, pathId);
        LOG.info("Path was successfully deleted.");
        return view;
    }

    @RequestMapping(value = "/player/edit-path/{playerId}/{pathId}", method = RequestMethod.GET)
    public ModelAndView playerEditPath(@PathVariable Long playerId, @PathVariable Long pathId) {
        ModelAndView view = new ModelAndView("player-path-edit");
        PlayerInfo player = jsonApi.getPlayerInfo(playerId);
        view.addObject("player", player);

        for (PlayerPath path : player.getPaths()) {
            if (path.getId() == pathId) {
                view.addObject("path", path);
                break;
            }
        }

        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        return view;
    }

    @RequestMapping(value = "/player/edit-path/{playerId}/{pathId}", method = RequestMethod.POST)
    public ModelAndView playerEditPath(@PathVariable Long playerId, @PathVariable Long pathId, @ModelAttribute("path") PlayerPath playerPath) {
        ModelAndView view = new ModelAndView("redirect:/player/details/" + playerId);

        LOG.info("Updating player path: {}-{}", playerId, pathId);
        this.jsonApi.storePlayerPath(playerId, pathId, playerPath);
        LOG.info("Path was successfully updated.");
        return view;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Skins Pages">
    @RequestMapping("/skin-info")
    public ModelAndView skinInfo() {
        ModelAndView view = new ModelAndView("skin-info", "skin-entity", new Skin());
        List<String> dirNames = fileStorageService.getDirectoryList(StorageType.SKIN, ".");
        List<Skin> skins = new ArrayList<>(dirNames.size());
        for (String dir : dirNames) {
            // Skip directories that start with "."
            if (dir.startsWith(".")) {
                continue;
            }
            Skin skin = new Skin();
            skin.setPath(dir);
            skin.setSkinDir(fileStorageService.getStoragePathSkin());
            skin.readSkinInformation();
            LOG.info("Skin: {}", skin.toString());
            skins.add(skin);
        }

        view.addObject("skins", skins);
        view.addObject("yi", sic.getYamjInfo("true"));
        return view;
    }

    @RequestMapping("/skin-download")
    public ModelAndView skinDownload(@ModelAttribute Skin skin) {
        ModelAndView view = new ModelAndView("skin-download");
        view.addObject("yi", sic.getYamjInfo("true"));
        view.addObject("skin", skin);
        skin.setSkinDir(fileStorageService.getStoragePathSkin());
        String message = fileStorageService.storeSkin(skin);
        LOG.info(message);
        return view;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="TraktTV">
    @RequestMapping("/trakttv/info")
    public ModelAndView trakttvInfo() {
        ModelAndView view = new ModelAndView("trakttv-info");
        view.addObject("yi", sic.getYamjInfo("true"));
        view.addObject("trakttv-scrobble", String.valueOf(PropertyTools.getBooleanProperty("trakttv.scrobble", Boolean.FALSE)));
        return view;
    }
    //</editor-fold>
}
