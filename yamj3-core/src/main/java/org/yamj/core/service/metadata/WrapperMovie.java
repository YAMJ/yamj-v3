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
package org.yamj.core.service.metadata;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.model.IMovie;
import org.yamj.plugin.api.model.type.JobType;

public class WrapperMovie implements IMovie {

    private final VideoData videoData;
    private final LocaleService localeService;
    private final IdentifierService identifierService;
    private String scannerName;

    public WrapperMovie(VideoData videoData, LocaleService localeService, IdentifierService identifierService) {
        this.videoData = videoData;
        this.localeService = localeService;
        this.identifierService = identifierService;
    }
   
    public void setScannerName(String scannerName) {
        this.scannerName = scannerName;
    }
    
    @Override
    public String getId(String source) {
        return videoData.getSourceDbId(source);
    }

    @Override
    public void addId(String source, String id) {
        if (scannerName.equalsIgnoreCase(source)) { 
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
        if (OverrideTools.checkOverwriteTitle(videoData, scannerName)) {
            videoData.setTitle(title, scannerName);
        }
    }

    @Override
    public String getOriginalTitle() {
        return videoData.getTitleOriginal();
    }

    @Override
    public void setOriginalTitle(String originalTitle) {
        if (OverrideTools.checkOverwriteOriginalTitle(videoData, scannerName)) {
            videoData.setTitleOriginal(originalTitle, scannerName);
        }
    }

    @Override
    public int getYear() {
        return videoData.getPublicationYear();
   }

    @Override
    public void setYear(int year) {
        if (OverrideTools.checkOverwriteYear(videoData, scannerName)) {
            videoData.setPublicationYear(year, scannerName);
        }
    }

    @Override
    public void setPlot(String plot) {
        if (OverrideTools.checkOverwritePlot(videoData, scannerName)) {
            videoData.setPlot(plot, scannerName);
        }
    }

    @Override
    public void setOutline(String outline) {
        if (OverrideTools.checkOverwriteOutline(videoData, scannerName)) {
            videoData.setOutline(outline, scannerName);
        }
    }

    @Override
    public void setTagline(String tagline) {
        if (OverrideTools.checkOverwriteTagline(videoData, scannerName)) {
            videoData.setTagline(tagline, scannerName);
        }
    }

    @Override
    public void setQuote(String quote) {
        if (OverrideTools.checkOverwriteQuote(videoData, scannerName)) {
            videoData.setTagline(quote, scannerName);
        }
    }

    @Override
    public void setRelease(String country, Date releaseDate) {
        if (OverrideTools.checkOverwriteReleaseDate(videoData, scannerName)) {
            String countryCode = localeService.findCountryCode(country);
            videoData.setRelease(countryCode, releaseDate, scannerName);
        }
    }

    @Override
    public void setRating(int rating) {
        videoData.addRating(scannerName, rating);
    }

    @Override
    public void setStudios(Collection<String> studios) {
        if (OverrideTools.checkOverwriteStudios(videoData, scannerName)) {
            videoData.setStudioNames(studios, scannerName);
        }
    }

    @Override
    public void setGenres(Collection<String> genres) {
        if (OverrideTools.checkOverwriteGenres(videoData, scannerName)) {
            videoData.setGenreNames(genres, scannerName);
        }
    }

    @Override
    public void setCountries(Collection<String> countries) {
        if (countries != null && OverrideTools.checkOverwriteCountries(videoData, scannerName)) {
            final Set<String> countryCodes = new HashSet<>(countries.size());
            for (String country : countries) {
                final String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) {
                    countryCodes.add(countryCode);
                }
            }
            videoData.setCountryCodes(countryCodes, scannerName);
        }
    }

    @Override
    public void addCertification(String country, String certificate) {
        String countryCode = localeService.findCountryCode(country);
        videoData.addCertificationInfo(countryCode, certificate);
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
        CreditDTO creditDTO = this.identifierService.createCredit(scannerName, id, jobType, name);
        videoData.addCreditDTO(creditDTO);
    }

    @Override
    public void addCredit(String id, JobType jobType, String name, String role) {
        CreditDTO creditDTO = this.identifierService.createCredit(scannerName, id, jobType, name, role);
        videoData.addCreditDTO(creditDTO);
    }

    @Override
    public void addCredit(String id, JobType jobType, String name, String role, boolean voiceRole) {
        CreditDTO creditDTO = this.identifierService.createCredit(scannerName, id, jobType, name, role);
        if (voiceRole && creditDTO != null ) {
            creditDTO.setVoice(voiceRole);
        }
        videoData.addCreditDTO(creditDTO);
    }

    @Override
    public void addCredit(String id, JobType jobType, String name, String role, String photoUrl) {
        CreditDTO creditDTO = this.identifierService.createCredit(scannerName, id, jobType, name, role);
        if (photoUrl != null && creditDTO != null) {
            creditDTO.addPhoto(scannerName, photoUrl);
        }
        videoData.addCreditDTO(creditDTO);
    }

    @Override
    public void addCollection(String name, String id) {
        final String identifier = this.identifierService.cleanIdentifier(name);
        if (StringUtils.isNotBlank(identifier)) {
            videoData.addBoxedSetDTO(scannerName, identifier, name, Integer.valueOf(-1), id);
        }
    }

    @Override
    public void addAward(String event, String category, int year) {
        addAward(event, category, year, true, false);
    }

    @Override
    public void addAward(String event, String category, int year, boolean won, boolean nominated) {
        videoData.addAwardDTO(scannerName, event, category, year, won, nominated);
    }

    @Override
    public void setTopRank(int topRank) {
        videoData.setTopRank(topRank);
    }
}
