package org.yamj.core.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.database.dao.ArtworkDao;
import org.yamj.core.database.model.Artwork;

@Controller
@RequestMapping("/api/artwork")
public class ArtworkController {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkController.class);
    @Autowired
    private ArtworkDao artworkDao;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public @ResponseBody
    Artwork getArtworkById(@PathVariable String id) {
        LOG.info("Getting artwork with ID '{}'", id);
        return artworkDao.getArtwork(Long.parseLong(id));
    }
}
