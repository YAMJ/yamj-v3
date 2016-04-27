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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.model.IEpisode;
import org.yamj.plugin.api.model.ISeason;
import org.yamj.plugin.api.model.ISeries;

public class WrapperSeason implements ISeason {

    private final WrapperSeries wrapperSeries;
    private final Season season;
    private final LocaleService localeService;
    private final IdentifierService identifierService;
    private List<IEpisode> episodes;

    public WrapperSeason(WrapperSeries wrapperSeries, Season season, LocaleService localeService, IdentifierService identifierService) {
        this.wrapperSeries = wrapperSeries;
        this.season = season;
        this.localeService = localeService;
        this.identifierService = identifierService;
    }

    public String getScannerName() {
        return this.wrapperSeries.getScannerName();
    }

    @Override
    public int getNumber() {
        return season.getSeason();
    }
    
    @Override
    public String getId(String source) {
        return season.getSourceDbId(source);
    }

    @Override
    public void addId(String source, String id) {
        if (getScannerName().equalsIgnoreCase(source)) { 
            season.setSourceDbId(source, id);
        } else if (StringUtils.isBlank(season.getSourceDbId(source))) {
            season.setSourceDbId(source, id);
        }
    }

    @Override
    public String getTitle() {
        return season.getTitle();
    }

    @Override
    public void setTitle(String title) {
        if (OverrideTools.checkOverwriteTitle(season, getScannerName())) {
            season.setTitle(title, getScannerName());
        }
    }

    @Override
    public String getOriginalTitle() {
        return season.getTitleOriginal();
    }

    @Override
    public void setOriginalTitle(String originalTitle) {
        if (OverrideTools.checkOverwriteOriginalTitle(season, getScannerName())) {
            season.setTitleOriginal(originalTitle, getScannerName());
        }
    }

    @Override
    public int getYear() {
        return season.getPublicationYear();
   }

    @Override
    public void setYear(int year) {
        if (OverrideTools.checkOverwriteYear(season, getScannerName())) {
            season.setPublicationYear(year, getScannerName());
        }
    }

    @Override
    public void setPlot(String plot) {
        if (OverrideTools.checkOverwritePlot(season, getScannerName())) {
            season.setPlot(plot, getScannerName());
        }
    }

    @Override
    public void setOutline(String outline) {
        if (OverrideTools.checkOverwriteOutline(season, getScannerName())) {
            season.setOutline(outline, getScannerName());
        }
    }

    @Override
    public void setRating(int rating) {
        season.addRating(getScannerName(), rating);
    }

    @Override
    public boolean isDone() {
        return season.isTvSeasonDone(getScannerName());
    }

    @Override
    public void setDone() {
        season.setTvSeasonDone();
    }

    @Override
    public void setNotFound() {
        season.removeOverrideSource(getScannerName());
        season.removeSourceDbId(getScannerName());
        season.setTvSeasonNotFound();
    }

    @Override
    public ISeries getSeries() {
        return wrapperSeries;
    }

    @Override
    public Collection<IEpisode> getEpisodes() {
        if (this.episodes == null) {
            this.episodes = new ArrayList<>();
            for (VideoData videoData : season.getVideoDatas()) {
                WrapperEpisode wrapper = new WrapperEpisode(this, videoData, localeService, identifierService);
                this.episodes.add(wrapper);
            }
        }
        return this.episodes;
    }
}
