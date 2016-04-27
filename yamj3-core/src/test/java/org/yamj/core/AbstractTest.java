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
package org.yamj.core;

import java.util.List;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.AwardDTO;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.plugin.api.artwork.ArtworkDTO;
import org.yamj.plugin.api.model.type.ArtworkType;

@ContextConfiguration(locations = {"classpath:spring-test.xml"})
public abstract class AbstractTest extends AbstractJUnit4SpringContextTests {

    @BeforeClass
    public static void startUp() {
        System.setProperty("spring.profiles.active", "memory");
    }
    
    @SuppressWarnings("rawtypes")
    protected static void logCredits(Season season, Class scannerClass) {
        for (VideoData videoData : season.getVideoDatas()) {
            logCredits(videoData, scannerClass);
        }
    }
    
    @SuppressWarnings("rawtypes")
    protected static void  logCredits(VideoData videoData, Class scannerClass) {
        Logger LOG = LoggerFactory.getLogger(scannerClass);
        LOG.info("VideoData {}: {} credits", videoData.getTitle(), videoData.getCreditDTOS().size());
        for (CreditDTO credit : videoData.getCreditDTOS()) {
            LOG.info("{}: {} {}", credit.getJobType(), credit.getName(), (credit.getRole() == null)?null:"("+credit.getRole()+")");
        }
    }

    @SuppressWarnings("rawtypes")
    protected static void  logAwards(VideoData videoData, Class scannerClass) {
        Logger LOG = LoggerFactory.getLogger(scannerClass);
        LOG.info("VideoData {}: {} awards", videoData.getTitle(), videoData.getAwardDTOS().size());
        for (AwardDTO award : videoData.getAwardDTOS()) {
            LOG.info("{} - {}: {} (won: {})", award.getEvent(), award.getCategory(), award.getYear(), award.isWon());
        }
    }

    @SuppressWarnings("rawtypes")
    protected static void logArtworks(ArtworkType type, List<ArtworkDTO> dtos, Class scannerClass) {
        Logger LOG = LoggerFactory.getLogger(scannerClass);
        if (dtos != null) {
            for (ArtworkDTO dto : dtos) {
                LOG.info("{}: {}", type, dto);
            }
        }
    }
}