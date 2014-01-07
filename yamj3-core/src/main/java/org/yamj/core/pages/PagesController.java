package org.yamj.core.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.yamj.common.model.YamjInfo;
import org.yamj.core.api.json.IndexController;
import org.yamj.core.api.json.SystemInfoController;
import org.yamj.core.api.model.Skin;
import org.yamj.core.api.options.OptionsPlayer;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.model.Configuration;
import org.yamj.core.database.model.PlayerPath;
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

    //<editor-fold defaultstate="collapsed" desc="System Info Page">
    @RequestMapping(value = "/system-info")
    public ModelAndView displaySystemInfo() {
        ModelAndView view = new ModelAndView("system-info");
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        view.addObject("countlist", yi.getCounts());
        return view;
    }
    //</editor-fold>

    @RequestMapping(value = "/count/job")
    public ModelAndView displayCountJob() {
        ModelAndView view = new ModelAndView("count-job");
        YamjInfo yi = sic.getYamjInfo("false");
        view.addObject("yi", yi);
        view.addObject("countlist", index.getJobs("all"));
        return view;
    }

    //<editor-fold defaultstate="collapsed" desc="Configuration Pages">
    @RequestMapping(value = "/config/add")
    public ModelAndView configAddPage() {
        ModelAndView view = new ModelAndView("config-add");
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        view.addObject("config", new Configuration());
        return view;
    }

    @RequestMapping(value = "/config/add/process")
    public ModelAndView configAdd(@ModelAttribute Configuration config) {

        ModelAndView view = new ModelAndView("redirect:/config/list");
        LOG.info("Adding config: {}", config.toString());
        configService.setProperty(config.getKey(), config.getValue());
        String message = "Configuration was successfully added.";
        view.addObject("message", message);

        return view;
    }

    @RequestMapping(value = "/config/list")
    public ModelAndView configList() {
        ModelAndView view = new ModelAndView("config-list");

        List<Configuration> configList = configService.getConfiguration("");
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
        List<Configuration> configList = configService.getConfiguration(key);
        if (!configList.isEmpty()) {
            view.addObject("config", configList.get(0));
        }
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        return view;
    }

    @RequestMapping(value = "/config/edit/{key}", method = RequestMethod.POST)
    public ModelAndView configEditUpdate(@ModelAttribute("config") Configuration config, @PathVariable String key) {
        ModelAndView view = new ModelAndView("redirect:/config/list");
        LOG.info("Updating config: {}", config.toString());
        configService.setProperty(config.getKey(), config.getValue());
        String message = "Config was successfully edited.";
        view.addObject("message", message);
        return view;
    }

    @RequestMapping(value = "/config/delete/{key}", method = RequestMethod.GET)
    public ModelAndView configDelete(@PathVariable String key) {
        ModelAndView view = new ModelAndView("redirect:/config/list");

        LOG.info("Deleting config for '{}'", key);
        configService.deleteProperty(key);
        String message = "Config was successfully deleted.";
        view.addObject("message", message);
        return view;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Player Path Pages">
    @RequestMapping(value = "/player/add")
    public ModelAndView playerAddPage() {
        ModelAndView view = new ModelAndView("player-add");
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        view.addObject("player", new PlayerPath());
        return view;
    }

    @RequestMapping(value = "/player/add/process")
    public ModelAndView playerAdd(@ModelAttribute PlayerPath player) {

        ModelAndView view = new ModelAndView("redirect:/player/list");
        LOG.info("Adding player: {}", player.toString());
        jsonApi.setPlayer(player);
        String message = "Player was successfully added.";
        view.addObject("message", message);

        return view;
    }

    @RequestMapping(value = "/player/list")
    public ModelAndView playerList() {
        ModelAndView view = new ModelAndView("player-list");

        OptionsPlayer options = new OptionsPlayer();

        List<PlayerPath> playerList = jsonApi.getPlayer(options);
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        view.addObject("playerlist", playerList);

        return view;
    }

    @RequestMapping(value = "/player/edit/{name}", method = RequestMethod.GET)
    public ModelAndView playerEditPage(@PathVariable String name) {
        ModelAndView view = new ModelAndView("player-edit");
        List<PlayerPath> playerList = jsonApi.getPlayer(name);
        if (!playerList.isEmpty()) {
            view.addObject("player", playerList.get(0));
        }
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        return view;
    }

    @RequestMapping(value = "/player/edit/{name}", method = RequestMethod.POST)
    public ModelAndView playerEditUpdate(@ModelAttribute("player") PlayerPath player, @PathVariable String name) {
        ModelAndView view = new ModelAndView("redirect:/player/list");
        LOG.info("Updating player: {}", player.toString());
        jsonApi.setPlayer(player);
        String message = "Player was successfully edited.";
        view.addObject("message", message);
        return view;
    }

    @RequestMapping(value = "/player/delete/{name}", method = RequestMethod.GET)
    public ModelAndView playerDelete(@PathVariable String name) {
        ModelAndView view = new ModelAndView("redirect:/player/list");

        LOG.info("Deleting player '{}'", name);
        jsonApi.deletePlayer(name);
        String message = "Player was successfully deleted.";
        view.addObject("message", message);
        return view;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Skins Pages">
    @RequestMapping(value = "/skin-info")
    public ModelAndView skinInfo() {
        ModelAndView view = new ModelAndView("skin-info", "skin-entity", new Skin());
        List<String> dirNames = fileStorageService.getDirectoryList(StorageType.SKIN, ".");
        List<Skin> skins = new ArrayList<Skin>(dirNames.size());
        for (String dir : dirNames) {
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

    @RequestMapping(value = "/skin-download")
    public ModelAndView skinDownload(@ModelAttribute Skin skin) {
        ModelAndView view = new ModelAndView("skin-download");
        view.addObject("yi", sic.getYamjInfo("true"));
        view.addObject("skin", skin);
        skin.setSkinDir(fileStorageService.getStoragePathSkin());
        String message = fileStorageService.storeSkin(skin);
        view.addObject("message", message);
        return view;
    }
    //</editor-fold>
}
