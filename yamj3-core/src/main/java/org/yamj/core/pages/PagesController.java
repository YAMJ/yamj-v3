package org.yamj.core.pages;

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
import org.yamj.core.api.json.SystemInfoController;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.model.Configuration;

@Controller
public class PagesController {

    private static final Logger LOG = LoggerFactory.getLogger(PagesController.class);
    @Autowired
    SystemInfoController sic;
    @Autowired
    ConfigService configService;

    @RequestMapping(value = {"/", "/index"})
    public ModelAndView displayRoot() {
        ModelAndView view = new ModelAndView("index");
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
    //<editor-fold defaultstate="collapsed" desc="Configuration Pages">
    @RequestMapping(value = "/config/add")
    public ModelAndView configAddPage() {
        ModelAndView view = new ModelAndView("config-add");
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

        List<Configuration> configList = configService.getConfiguration();
        view.addObject("configlist", configList);

        return view;
    }

    @RequestMapping(value = "/config/edit/{key}", method = RequestMethod.GET)
    public ModelAndView configEditPage(@PathVariable String key) {
        ModelAndView view = new ModelAndView("config-edit");
        Configuration config = configService.getConfiguration(key);
        view.addObject("config", config);
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
}
