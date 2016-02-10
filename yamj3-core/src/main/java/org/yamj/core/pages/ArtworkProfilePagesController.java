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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.scheduling.ArtworkProcessScheduler;

@Controller
@RequestMapping(value = "/profile")
public class ArtworkProfilePagesController extends AbstractPagesController {
 
    private static final Logger LOG = LoggerFactory.getLogger(ArtworkProfilePagesController.class);

    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private ArtworkProcessScheduler artworkProcessScheduler;
    
    @RequestMapping("/list")
    public ModelAndView profileList() {
        ModelAndView view = withInfo(new ModelAndView("profile/profile-list"));
        view.addObject("profilelist", artworkStorageService.getAllArtworkProfiles());
        return view;
    }

    @RequestMapping(value = "/generate/{id}", method = RequestMethod.GET)
    public ModelAndView generate(@PathVariable long id) {
        ModelAndView view = withInfo(new ModelAndView("profile/profile-list"));
        view.addObject("profilelist", artworkStorageService.getAllArtworkProfiles());

        try {
            int count = this.artworkStorageService.generateImagesForProfile(id);
            LOG.debug("Trigger rescan for {} generated images", count);
    
            if (count > 0) {
                // trigger artwork processing when something was updated
                artworkProcessScheduler.triggerProcess();
            }
            
            view.addObject(SUCCESS_MESSAGE, "Triggered regeneration of "+count+" images");
        } catch (Exception ex) {
            LOG.error("Failed generation trigger for profile "+id, ex);
            view.addObject(ERROR_MESSAGE, "Failed regeneration trigger of images");
        }

        return view;
    }
}
