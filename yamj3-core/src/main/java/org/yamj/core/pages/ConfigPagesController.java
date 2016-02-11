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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.yamj.core.api.options.OptionsConfig;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.Configuration;

@Controller
@RequestMapping(value = "/config")
public class ConfigPagesController extends AbstractPagesController {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigPagesController.class);
    
    @Autowired
    private ConfigService configService;

    @RequestMapping("/add")
    public ModelAndView configAddPage() {
        ModelAndView view = withInfo(new ModelAndView("config/config-add"));
        view.addObject("config", new Configuration());
        return view;
    }

    @RequestMapping("/add/process")
    public ModelAndView configAdd(@ModelAttribute Configuration config) {
        LOG.info("Adding config: {}", config.toString());
        configService.setProperty(config.getKey(), config.getValue());
        LOG.trace("Configuration was successfully added.");

        return new ModelAndView("redirect:/config/list");
    }

    @RequestMapping("/list")
    public ModelAndView configList() {
        List<Configuration> configList = configService.getConfigurations(new OptionsConfig());
        Collections.sort(configList, new Comparator<Configuration>() {
            @Override
            public int compare(Configuration o1, Configuration o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        ModelAndView view = withInfo(new ModelAndView("config/config-list"));
        view.addObject("configlist", configList);
        return view;
    }

    @RequestMapping(value = "/edit/{key}", method = RequestMethod.GET)
    public ModelAndView configEditPage(@PathVariable String key) {
        if (StringUtils.isBlank(key)) {
            return new ModelAndView("redirect:/config/list");
        }

        Configuration config = configService.getConfiguration(key);
        if (config == null) {
            return new ModelAndView("redirect:/config/list");
        }
        
        ModelAndView view = withInfo(new ModelAndView("config/config-edit"));
        view.addObject("config", config);
        return view;
    }

    @RequestMapping(value = "/edit/{key}", method = RequestMethod.POST)
    public ModelAndView configEditUpdate(@ModelAttribute("config") Configuration config) {
        LOG.info("Updating config: {}", config.toString());
        configService.setProperty(config.getKey(), config.getValue());
        LOG.trace("Config was successfully edited.");
        
        return new ModelAndView("redirect:/config/list");
    }

    @RequestMapping(value = "/delete/{key}", method = RequestMethod.GET)
    public ModelAndView configDelete(@PathVariable String key) {
        LOG.info("Deleting config for '{}'", key);
        configService.deleteProperty(key);
        LOG.trace("Config was successfully deleted.");
        
        return new ModelAndView("redirect:/config/list");
    }
}
