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
import org.yamj.core.database.model.VideoData;

@Controller
@RequestMapping("/api/video")
public class VideoController {

    private static final Logger LOG = LoggerFactory.getLogger(VideoController.class);
    @Autowired
    private MediaDao mediaDao;

    @RequestMapping(value = "/id={id}", method = RequestMethod.GET)
    public @ResponseBody
    VideoData getVideoById(@PathVariable String id) {
        LOG.info("Attempting to get video with ID '{}'", id);
        VideoData video =mediaDao.getVideoData(Long.parseLong(id));
        return video;
    }
}
