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
package org.yamj.core.service.metadata.online;

import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.model.IEpisode;
import org.yamj.plugin.api.model.ISeason;
import org.yamj.plugin.api.model.type.JobType;

public class WrapperEpisode implements IEpisode {

    private final WrapperSeason wrapperSeason;
    private final VideoData videoData;
    private final LocaleService localeService;
    private final IdentifierService identifierService;

    public WrapperEpisode(WrapperSeason wrapperSeason, VideoData videoData, LocaleService localeService, IdentifierService identifierService) {
        this.wrapperSeason = wrapperSeason;
        this.videoData = videoData;
        this.localeService = localeService;
        this.identifierService = identifierService;
    }

    public String getScannerName() {
        return this.wrapperSeason.getScannerName();
    }

    @Override
    public int getNumber() {
        return videoData.getEpisode();
    }

    @Override
    public String getId(String source) {
        return videoData.getSourceDbId(source);
    }

    @Override
    public void addId(String source, String id) {
        if (getScannerName().equalsIgnoreCase(source)) { 
            videoData.setSourceDbId(source, id);
        } else if (StringUtils.isBlank(videoData.getSourceDbId(source))) {
            videoData.setSourceDbId(source, id);
        }
    }

    @Override
    public String getTitle() {
        return videoData.getTitle();
    }

    @Override
    public void setTitle(String title) {
        if (OverrideTools.checkOverwriteTitle(videoData, getScannerName())) {
            videoData.setTitle(title, getScannerName());
        }
    }

    @Override
    public String getOriginalTitle() {
        return videoData.getTitleOriginal();
    }

    @Override
    public void setOriginalTitle(String originalTitle) {
        if (OverrideTools.checkOverwriteOriginalTitle(videoData, getScannerName())) {
            videoData.setTitleOriginal(originalTitle, getScannerName());
        }
    }

    @Override
    public void setPlot(String plot) {
        if (OverrideTools.checkOverwritePlot(videoData, getScannerName())) {
            videoData.setPlot(plot, getScannerName());
        }
    }

    @Override
    public void setOutline(String outline) {
        if (OverrideTools.checkOverwriteOutline(videoData, getScannerName())) {
            videoData.setOutline(outline, getScannerName());
        }
    }

    @Override
    public void setTagline(String tagline) {
        if (OverrideTools.checkOverwriteTagline(videoData, getScannerName())) {
            videoData.setTagline(tagline, getScannerName());
        }
    }

    @Override
    public void setQuote(String quote) {
        if (OverrideTools.checkOverwriteQuote(videoData, getScannerName())) {
            videoData.setTagline(quote, getScannerName());
        }
    }

    @Override
    public void setRelease(Date releaseDate) {
        this.setRelease(null, releaseDate);
    }

    @Override
    public void setRelease(String country, Date releaseDate) {
        if (releaseDate != null && OverrideTools.checkOverwriteReleaseDate(videoData, getScannerName())) {
            String countryCode = localeService.findCountryCode(country);
            videoData.setRelease(countryCode, releaseDate, getScannerName());
        }
    }

    @Override
    public void setRating(int rating) {
        videoData.addRating(getScannerName(), rating);
    }

    @Override
    public void addCredit(JobType jobType, String name) {
        addCredit(null, jobType, name);
    }

    @Override
    public void addCredit(JobType jobType, String name, String role) {
        addCredit(null, jobType, name, role);
    }

    @Override
    public void addCredit(JobType jobType, String name, String role, boolean voiceRole) {
        addCredit(null, jobType, name, role, voiceRole);
    }

    @Override
    public void addCredit(JobType jobType, String name, String role, String photoUrl) {
        addCredit(null, jobType, name, role, photoUrl);
    }
    
    @Override
    public void addCredit(String id, JobType jobType, String name) {
        CreditDTO creditDTO = this.identifierService.createCredit(getScannerName(), id, jobType, name);
        videoData.addCreditDTO(creditDTO);
    }

    @Override
    public void addCredit(String id, JobType jobType, String name, String role) {
        CreditDTO creditDTO = this.identifierService.createCredit(getScannerName(), id, jobType, name, role);
        videoData.addCreditDTO(creditDTO);
    }

    @Override
    public void addCredit(String id, JobType jobType, String name, String role, boolean voiceRole) {
        CreditDTO creditDTO = this.identifierService.createCredit(getScannerName(), id, jobType, name, role);
        if (voiceRole && creditDTO != null ) {
            creditDTO.setVoice(voiceRole);
        }
        videoData.addCreditDTO(creditDTO);
    }

    @Override
    public void addCredit(String id, JobType jobType, String name, String role, String photoUrl) {
        CreditDTO creditDTO = this.identifierService.createCredit(getScannerName(), id, jobType, name, role);
        if (photoUrl != null && creditDTO != null) {
            creditDTO.addPhoto(getScannerName(), photoUrl);
        }
        videoData.addCreditDTO(creditDTO);
    }


    @Override
    public boolean isDone() {
        return videoData.isTvEpisodeDone(getScannerName());
    }

    @Override
    public void setDone() {
        videoData.setTvEpisodeDone();
    }

    @Override
    public void setNotFound() {
        videoData.removeOverrideSource(getScannerName());
        videoData.removeSourceDbId(getScannerName());
        videoData.setTvEpisodeNotFound();
    }

    @Override
    public ISeason getSeason() {
        return wrapperSeason;
    }
}
