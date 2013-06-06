/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.api;

import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.model.Certification;
import org.yamj.core.database.model.Genre;
import org.yamj.core.database.model.Studio;

@Controller
@RequestMapping("/api")
public class CommonController {

    private static final Logger LOG = LoggerFactory.getLogger(CommonController.class);
    @Autowired
    private CommonDao commonDao;

    @RequestMapping(value = "/genre/{name}", method = RequestMethod.GET)
    @ResponseBody
    @Transactional(readOnly = true)
    public Genre getGenre(@PathVariable String name) {
        LOG.info("Getting genre '{}'", name);
        return commonDao.getGenre(name);
    }

    @RequestMapping(value = "/certification/{name}", method = RequestMethod.GET)
    @ResponseBody
    @Transactional(readOnly = true)
    public Certification getCertification(@PathVariable String name) {
        LOG.info("Getting certification '{}'", name);
        return commonDao.getCertification(name);
    }

    @RequestMapping(value = "/studio/{name}", method = RequestMethod.GET)
    @ResponseBody
    @Transactional(readOnly = true)
    public Studio getStudio(@PathVariable String name) {
        LOG.info("Getting studio '{}'", name);
        return commonDao.getStudio(name);
    }
}
