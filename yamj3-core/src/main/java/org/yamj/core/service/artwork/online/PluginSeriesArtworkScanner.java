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
package org.yamj.core.service.artwork.online;

import java.util.List;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.plugin.api.artwork.ArtworkDTO;
import org.yamj.plugin.api.artwork.SeriesArtworkScanner;
import org.yamj.plugin.api.metadata.IEpisode;
import org.yamj.plugin.api.metadata.ISeason;
import org.yamj.plugin.api.metadata.ISeries;
import org.yamj.plugin.api.metadata.mock.EpisodeMock;
import org.yamj.plugin.api.metadata.mock.SeasonMock;
import org.yamj.plugin.api.metadata.mock.SeriesMock;

public class PluginSeriesArtworkScanner implements ISeriesArtworkScanner {

    private final SeriesArtworkScanner seriesArtworkScanner;
    
    public PluginSeriesArtworkScanner(SeriesArtworkScanner seriesArtworkScanner) {
        this.seriesArtworkScanner = seriesArtworkScanner;
    }
    
    @Override
    public String getScannerName() {
        return seriesArtworkScanner.getScannerName();
    }

    @Override
    public List<ArtworkDTO> getPosters(Season season) {
        return seriesArtworkScanner.getPosters(buildSeason(season));
    }

    @Override
    public List<ArtworkDTO> getPosters(Series series) {
        return seriesArtworkScanner.getPosters(buildSeries(series));
    }

    @Override
    public List<ArtworkDTO> getFanarts(Season season) {
        return seriesArtworkScanner.getFanarts(buildSeason(season));
    }

    @Override
    public List<ArtworkDTO> getFanarts(Series series) {
        return seriesArtworkScanner.getFanarts(buildSeries(series));
    }

    @Override
    public List<ArtworkDTO> getBanners(Season season) {
        return seriesArtworkScanner.getBanners(buildSeason(season));
    }

    @Override
    public List<ArtworkDTO> getBanners(Series series) {
        return seriesArtworkScanner.getBanners(buildSeries(series));
    }

    @Override
    public List<ArtworkDTO> getVideoImages(VideoData videoData) {
        return seriesArtworkScanner.getVideoImages(buildEpisode(videoData));
    }

    private static ISeries buildSeries(Series series) {
        SeriesMock mock = new SeriesMock(series.getIdMap());
        mock.setTitle(series.getTitle());
        mock.setOriginalTitle(series.getTitleOriginal());
        mock.setStartYear(series.getStartYear());
        mock.setEndYear(series.getEndYear());
        return mock;
    }

    private static ISeason buildSeason(Season season) {
        SeasonMock mock = new SeasonMock(season.getSeason(), season.getIdMap());
        mock.setTitle(season.getTitle());
        mock.setOriginalTitle(season.getTitleOriginal());
        mock.setYear(season.getPublicationYear());
        mock.setSeries(buildSeries(season.getSeries()));
        return mock;
    }

    private static IEpisode buildEpisode(VideoData videoData) {
        EpisodeMock mock = new EpisodeMock(videoData.getEpisode(), videoData.getIdMap());
        mock.setTitle(videoData.getTitle());
        mock.setOriginalTitle(videoData.getTitleOriginal());
        mock.setSeason(buildSeason(videoData.getSeason()));
        return mock;
    }
}
