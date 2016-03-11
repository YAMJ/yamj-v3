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

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.pages.form.ArtworkProfileForm;
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
    public ModelAndView generate(@PathVariable Long id) {
        ModelAndView view = withInfo(new ModelAndView("profile/profile-list"));
        view.addObject("profilelist", artworkStorageService.getAllArtworkProfiles());

        try {
            int count = this.artworkStorageService.generateImagesForProfile(id);
    
            if (count > 0) {
                LOG.debug("Trigger regeneration of {} images", count);
                artworkProcessScheduler.trigger();
                view.addObject(SUCCESS_MESSAGE, "Triggered regeneration of "+count+" images");
            } else {
                view.addObject(SUCCESS_MESSAGE, "No image regeneration needed");
            }            
        } catch (Exception ex) {
            LOG.error("Failed generation trigger for profile "+id, ex);
            view.addObject(ERROR_MESSAGE, "Failed regeneration trigger of images");
        }

        return view;
    }

    @RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
    public ModelAndView profileEditPage(@PathVariable Long id) {
        ArtworkProfile profile = artworkStorageService.getArtworkProfile(id);
        if (profile == null) {
            return new ModelAndView("redirect:/profile/list");
        }

        ArtworkProfileForm form = new ArtworkProfileForm();
        form.setId(profile.getId());
        form.setProfileName(profile.getProfileName());
        form.setArtworkType(profile.getArtworkType());
        form.setMetaDataType(profile.getMetaDataType());
        form.setWidth(Integer.toString(profile.getWidth()));
        form.setHeight(Integer.toString(profile.getHeight()));
        form.setScalingType(profile.getScalingType());
        form.setReflection(profile.isReflection());
        form.setRoundedCorners(profile.isRoundedCorners());
        form.setQuality(Integer.toString(profile.getQuality()));
        
        ModelAndView view = withInfo(new ModelAndView("profile/profile-edit"));
        view.addObject("profile", form);
        return view;
    }
    
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public ModelAndView profileEditUpdate(@ModelAttribute("profile") ArtworkProfileForm form) {
        LOG.trace("Submitted form: {}", form);
        
        // holds the error message
        String errorMessage = null;
        
        int width = NumberUtils.toInt(form.getWidth());
        int height = NumberUtils.toInt(form.getHeight());
        if (width == 0 || height == 0) {
            errorMessage = "Invalid image size defined";
        }
        
        int quality = NumberUtils.toInt(form.getQuality());
        if (quality == 0 || quality > 100) {
            errorMessage = "Invalid image quality defined";
        }
        
        if (errorMessage == null) {
            ArtworkProfile profile = artworkStorageService.getArtworkProfile(form.getId());
            profile.setWidth(width);
            profile.setHeight(height);
            profile.setScalingType(form.getScalingType());
            profile.setReflection(form.isReflection());
            profile.setRoundedCorners(form.isRoundedCorners());
            profile.setQuality(quality);
            artworkStorageService.updateArtworkProfile(profile);
            return new ModelAndView("redirect:/profile/list");
        }
        
        ModelAndView view = withInfo(new ModelAndView("profile/profile-edit"));
        view.addObject("profile", form);
        view.addObject(ERROR_MESSAGE, errorMessage);
        return view;
    }
}
