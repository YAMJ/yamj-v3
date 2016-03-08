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
package org.yamj.core.database.dao;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.springframework.stereotype.Repository;
import org.yamj.core.database.model.dto.TraktEpisodeDTO;
import org.yamj.core.database.model.dto.TraktMovieDTO;
import org.yamj.core.hibernate.HibernateDao;
import org.yamj.core.service.metadata.online.*;

@Repository("traktTvDao")
public class TraktTvDao extends HibernateDao {
    
    public Map<String,List<Long>> getAllMovieIds() {
        final Map<String,List<Long>> result = new HashMap<>();
        
        try (ScrollableResults scroll = currentSession().getNamedQuery("videoData.trakttv.movies")
                .setReadOnly(true)
                .scroll(ScrollMode.FORWARD_ONLY)
            )
        {
            String key;
            Long id;
            Object[] row;
            while (scroll.next()) {
                row = scroll.get();
                key = convertRowElementToString(row[0]);
                id = convertRowElementToLong(row[1]);
                
                List<Long> ids = result.get(key);
                if (ids==null) ids = new ArrayList<>();
                ids.add(id);
                result.put(key, ids);
            }
        }
        
        return result;
    }

    public Map<String,List<Long>> getAllEpisodeIds() {
        final Map<String,List<Long>> result = new HashMap<>();
        
        try (ScrollableResults scroll = currentSession().getNamedQuery("videoData.trakttv.episodes")
                .setReadOnly(true)
                .scroll(ScrollMode.FORWARD_ONLY)
            )
        {
            String key;
            Long id;
            Object[] row;
            while (scroll.next()) {
                row = scroll.get();
                key = convertRowElementToString(row[0]);
                id = convertRowElementToLong(row[1]);
                
                List<Long> ids = result.get(key);
                if (ids==null) ids = new ArrayList<>();
                ids.add(id);
                result.put(key, ids);
            }
        }
        
        return result;
    }

    public Collection<TraktMovieDTO> getCollectedMovies(Date checkDate) {
        final Map<Long,TraktMovieDTO> result = new HashMap<>();
        
        try (ScrollableResults scroll = currentSession().getNamedQuery("videoData.trakttv.collected.movies")
                .setTimestamp("checkDate", checkDate)
                .setReadOnly(true)
                .scroll(ScrollMode.FORWARD_ONLY)
            )
        {
            Object[] row;
            Long id;
            while (scroll.next()) {
                row = scroll.get();
                id = convertRowElementToLong(row[0]);
                
                TraktMovieDTO dto = result.get(id);
                if (dto == null) {
                    dto = new TraktMovieDTO();
                    dto.setId(id);
                    dto.setCollectDate(convertRowElementToDate(row[3]));
                    dto.setIdentifier(convertRowElementToString(row[4]));
                    
                    String origTitle = convertRowElementToString(row[6]);
                    if (StringUtils.isNotBlank(origTitle)) {
                        dto.setTitle(origTitle);
                    } else {
                        dto.setTitle(convertRowElementToString(row[5]));
                    }
                    
                    Integer year = convertRowElementToInteger(row[7]);
                    if (year != null && year.intValue() > 0) {
                        dto.setYear(year);
                    }
                }
                
                setMovieId(dto, row);
                result.put(id, dto);
            }
        }
        
        return result.values();
    }
    
    public Collection<TraktMovieDTO> getWatchedMovies(Date checkDate) {
        final Map<Long,TraktMovieDTO> result = new HashMap<>();
        
        try (ScrollableResults scroll = currentSession().getNamedQuery("videoData.trakttv.watched.movies")
                .setTimestamp("checkDate", checkDate)
                .setReadOnly(true)
                .scroll(ScrollMode.FORWARD_ONLY)
            )
        {
            Object[] row;
            Long id;
            while (scroll.next()) {
                row = scroll.get();
                id = convertRowElementToLong(row[0]);
                
                TraktMovieDTO dto = result.get(id);
                if (dto == null) {
                    dto = new TraktMovieDTO();
                    dto.setId(id);
                    dto.setWatchedDate(convertRowElementToDate(row[4]));
                    dto.setIdentifier(convertRowElementToString(row[5]));
                }
                
                setMovieId(dto, row);
                result.put(id, dto);
            }
        }
        
        return result.values();
    }

    public Collection<TraktEpisodeDTO> getCollectedEpisodes(Date checkDate) {
        final Map<Long,TraktEpisodeDTO> result = new HashMap<>();
        
        try (ScrollableResults scroll = currentSession().getNamedQuery("videoData.trakttv.collected.episodes")
                .setTimestamp("checkDate", checkDate)
                .setReadOnly(true)
                .scroll(ScrollMode.FORWARD_ONLY)
            )
        {
            Object[] row;
            Long id;
            while (scroll.next()) {
                row = scroll.get();
                id = convertRowElementToLong(row[0]);
                
                TraktEpisodeDTO dto = result.get(id);
                if (dto == null) {
                    dto = new TraktEpisodeDTO();
                    dto.setId(id);
                    dto.setCollectDate(convertRowElementToDate(row[3]));
                    dto.setIdentifier(convertRowElementToString(row[4]));
                    dto.setSeason(convertRowElementToInteger(row[5]));
                    dto.setEpisode(convertRowElementToInteger(row[6]));
                    
                    String origTitle = convertRowElementToString(row[8]);
                    if (StringUtils.isNotBlank(origTitle)) {
                        dto.setTitle(origTitle);
                    } else {
                        dto.setTitle(convertRowElementToString(row[7]));
                    }
                    
                    Integer year = convertRowElementToInteger(row[9]);
                    if (year != null && year.intValue() > 0) {
                        dto.setYear(year);
                    }
                }
                
                setMovieId(dto, row);
                result.put(id, dto);
            }
        }
        
        return result.values();
    }

    public Collection<TraktEpisodeDTO> getWatchedEpisodes(Date checkDate) {
        final Map<Long,TraktEpisodeDTO> result = new HashMap<>();
        
        try (ScrollableResults scroll = currentSession().getNamedQuery("videoData.trakttv.watched.episodes")
                .setTimestamp("checkDate", checkDate)
                .setReadOnly(true)
                .scroll(ScrollMode.FORWARD_ONLY)
            )
        {
            Object[] row;
            Long id;
            while (scroll.next()) {
                row = scroll.get();
                id = convertRowElementToLong(row[0]);
                
                TraktEpisodeDTO dto = result.get(id);
                if (dto == null) {
                    dto = new TraktEpisodeDTO();
                    dto.setId(id);
                    dto.setWatchedDate(convertRowElementToDate(row[4]));
                    dto.setSeason(convertRowElementToInteger(row[5]));
                    dto.setEpisode(convertRowElementToInteger(row[6]));
                    dto.setIdentifier(convertRowElementToString(row[7]));
                }
                
                setMovieId(dto, row);
                result.put(id, dto);
            }
        }
        
        return result.values();
    }

    private static void setMovieId(TraktMovieDTO dto, Object[] row) {
        final String source = convertRowElementToString(row[1]);
        if (TraktTvScanner.SCANNER_ID.equals(source)) {
            dto.setTrakt(convertRowElementToInteger(row[2]));
        } else if (ImdbScanner.SCANNER_ID.equals(source)) {
            dto.setImdb(convertRowElementToString(row[2]));
        } else if (TheMovieDbScanner.SCANNER_ID.equals(source)) {
            dto.setTmdb(convertRowElementToInteger(row[2]));
        }
    }

    private static void setMovieId(TraktEpisodeDTO dto, Object[] row) {
        final String source = convertRowElementToString(row[1]);
        if (TraktTvScanner.SCANNER_ID.equals(source)) {
            dto.setTrakt(convertRowElementToInteger(row[2]));
        } else if (ImdbScanner.SCANNER_ID.equals(source)) {
            dto.setImdb(convertRowElementToString(row[2]));
        } else if (TheMovieDbScanner.SCANNER_ID.equals(source)) {
            dto.setTmdb(convertRowElementToInteger(row[2]));
        } else if (TheTVDbScanner.SCANNER_ID.equals(source)) {
            dto.setTvdb(convertRowElementToInteger(row[2]));
        } else if (TVRageScanner.SCANNER_ID.equals(source)) {
            dto.setTvRage(convertRowElementToString(row[2]));
        }
    }
}
