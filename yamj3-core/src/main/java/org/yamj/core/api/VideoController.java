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
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;

@Controller
@RequestMapping("/api/video")
public class VideoController {

    private static final Logger LOG = LoggerFactory.getLogger(VideoController.class);
    @Autowired
    private MediaDao mediaDao;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public @ResponseBody
    VideoData getVideoById(@PathVariable String id) {
        LOG.info("Getting video with ID '{}'", id);
        return mediaDao.getVideoData(Long.parseLong(id));
    }

    @RequestMapping(value = "/series/{id}", method = RequestMethod.GET)
    public @ResponseBody
    Series getSeriesById(@PathVariable String id) {
        LOG.info("Getting series with ID '{}'", id);
        return mediaDao.getSeries(Long.parseLong(id));
    }

    @RequestMapping(value = "/season/{id}", method = RequestMethod.GET)
    public @ResponseBody
    Season getSeasonById(@PathVariable String id) {
        LOG.info("Getting season with ID '{}'", id);
        return mediaDao.getSeason(Long.parseLong(id));
    }
}
