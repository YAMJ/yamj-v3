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

import static org.yamj.plugin.api.common.Constants.ALL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.yamj.common.model.YamjInfo;
import org.yamj.core.api.json.IndexController;
import org.yamj.core.api.model.CountGeneric;
import org.yamj.core.api.model.Skin;
import org.yamj.core.config.ConfigService;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.StorageType;
import org.yamj.core.service.trakttv.TraktTvPin;
import org.yamj.core.service.trakttv.TraktTvService;

@Controller
public class CommonPagesController extends AbstractPagesController {

    private static final Logger LOG = LoggerFactory.getLogger(CommonPagesController.class);
    
    @Autowired
    private IndexController indexController;
    @Autowired
    private ConfigService configService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired 
    private TraktTvService traktTvService;
    
    @RequestMapping(value = {"/", "/index"})
    public ModelAndView displayRoot() {
        return withInfo(new ModelAndView("index"));
    }

    /**
     * For testing pages
     *
     * @param name
     * @return
     */
    @RequestMapping("/test/{name}")
    public ModelAndView displayTest(@PathVariable String name) {
       return withInfo(new ModelAndView("test-" + name));
    }

    @RequestMapping("/system-info")
    public ModelAndView displaySystemInfo() {
        ModelAndView view = new ModelAndView("system-info");
        YamjInfo yi = systemInfoController.getYamjInfo("true");
        view.addObject("yi", yi);
        view.addObject("countlist", yi.getCounts());
        
        List<CountGeneric> jobList = indexController.getJobs(ALL);
        // Add some wording if there is an empty list
        if (jobList.isEmpty()) {
            CountGeneric noJobs = new CountGeneric();
            noJobs.setItem("No jobs found");
            noJobs.setCounter(0L);
            jobList.add(noJobs);
        }
        view.addObject("joblist", jobList);

        return view;
    }
    
    //<editor-fold defaultstate="collapsed" desc="Skins Pages">
    @RequestMapping("/skin-info")
    public ModelAndView skinInfo() {
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

        ModelAndView view = withInfo(new ModelAndView("skin-info", "skin-entity", new Skin()));
        view.addObject("skins", skins);
        return view;
    }

    @RequestMapping("/skin-download")
    public ModelAndView skinDownload(@ModelAttribute Skin skin) {
        ModelAndView view = withInfo(new ModelAndView("skin-download"));
        view.addObject("skin", skin);
        skin.setSkinDir(fileStorageService.getStoragePathSkin());
        String message = fileStorageService.storeSkin(skin);
        LOG.info(message);
        return view;
    }
    //</editor-fold>

    
    //<editor-fold defaultstate="collapsed" desc="TraktTV Pages">
    @RequestMapping("/trakttv-info")
    public ModelAndView trakttvInfo() {
        ModelAndView view = withInfo(new ModelAndView("trakttv-info", "pin-entity", new TraktTvPin()));
        view.addObject("trakttv", traktTvService.getTraktTvInfo());
        return view;
    }

    @RequestMapping("/trakttv-pin")
    public ModelAndView trakttvPin(@ModelAttribute TraktTvPin pin) {
        final String errorMessage;
        final String givenPin = pin.getPin();
        if (StringUtils.isBlank(givenPin) || givenPin.length() < 7) {
            errorMessage = "No valid pin provided";
        } else {
            errorMessage = this.traktTvService.authorizeWithPin(givenPin);
        }
                       
        ModelAndView view = withInfo(new ModelAndView("trakttv-info", "pin-entity", new TraktTvPin()));
        view.addObject("trakttv", traktTvService.getTraktTvInfo());
        view.addObject(ERROR_MESSAGE, errorMessage);
        return view;
    }
    //</editor-fold>
}
