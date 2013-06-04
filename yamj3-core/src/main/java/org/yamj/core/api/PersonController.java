package org.yamj.core.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.database.dao.MediaDao;
import org.yamj.core.database.model.Person;

@Controller
@RequestMapping("/api/person")
public class PersonController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonController.class);
    @Autowired
    private MediaDao mediaDao;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Person getVideoById(@PathVariable String id) {
        LOG.info("Getting person with ID '{}'", id);
        return mediaDao.getPerson(Long.parseLong(id));
    }
}
