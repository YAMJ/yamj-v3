package org.yamj.core.api;

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
    public Genre getGenre(@PathVariable String name) {
        LOG.info("Getting genre '{}'", name);
        return commonDao.getGenre(name);
    }

    @RequestMapping(value = "/certification/{name}", method = RequestMethod.GET)
    @ResponseBody
    public Certification getCertification(@PathVariable String name) {
        LOG.info("Getting certification '{}'", name);
        return commonDao.getCertification(name);
    }

    @RequestMapping(value = "/studio/{name}", method = RequestMethod.GET)
    @ResponseBody
    public Studio getStudio(@PathVariable String name) {
        LOG.info("Getting studio '{}'", name);
        return commonDao.getStudio(name);
    }
}
