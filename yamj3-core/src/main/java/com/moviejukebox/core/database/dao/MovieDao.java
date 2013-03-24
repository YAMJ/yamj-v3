package com.moviejukebox.core.database.dao;

import org.springframework.stereotype.Service;

import com.moviejukebox.core.database.model.VideoData;
import com.moviejukebox.core.hibernate.ExtendedHibernateDaoSupport;

@Service("movieDao")
public class MovieDao extends ExtendedHibernateDaoSupport {

    public VideoData getMovieId(long id) {
        return this.getHibernateTemplate().get(VideoData.class, id);
    }
}
