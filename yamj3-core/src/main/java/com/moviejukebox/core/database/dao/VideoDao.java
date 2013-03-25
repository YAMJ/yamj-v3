package com.moviejukebox.core.database.dao;

import org.springframework.stereotype.Service;

import com.moviejukebox.core.database.model.VideoData;
import com.moviejukebox.core.hibernate.ExtendedHibernateDaoSupport;

@Service("videoDao")
public class VideoDao extends ExtendedHibernateDaoSupport {

    public VideoData getVideoData(long id) {
        return this.getHibernateTemplate().get(VideoData.class, id);
    }
}
