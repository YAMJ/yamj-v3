package com.moviejukebox.core.database.dao;

import org.springframework.stereotype.Service;

import com.moviejukebox.core.database.model.Movie;
import com.moviejukebox.core.hibernate.ExtendedHibernateDaoSupport;

@Service("movieDao")
public class MovieDao extends ExtendedHibernateDaoSupport {

    public Movie getMovieId(long id) {
        return this.getHibernateTemplate().get(Movie.class, id);
    }
}
