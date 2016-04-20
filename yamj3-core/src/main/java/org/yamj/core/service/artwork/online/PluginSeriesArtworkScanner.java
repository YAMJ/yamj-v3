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
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.plugin.api.artwork.SeriesArtworkScanner;
import org.yamj.plugin.api.metadata.dto.EpisodeDTO;
import org.yamj.plugin.api.metadata.dto.SeasonDTO;
import org.yamj.plugin.api.metadata.dto.SeriesDTO;

public class PluginSeriesArtworkScanner extends PluginArtworkScanner implements ISeriesArtworkScanner {

    private final SeriesArtworkScanner seriesArtworkScanner;
    
    public PluginSeriesArtworkScanner(SeriesArtworkScanner seriesArtworkScanner, LocaleService localeService) {
        super(localeService);
        this.seriesArtworkScanner = seriesArtworkScanner;
    }
    
    @Override
    public String getScannerName() {
        return seriesArtworkScanner.getScannerName();
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(Season season) {
        return createArtworkDetails(seriesArtworkScanner.getPosters(buildSeason(season)));
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(Series series) {
        return createArtworkDetails(seriesArtworkScanner.getPosters(buildSeries(series)));
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(Season season) {
        return createArtworkDetails(seriesArtworkScanner.getFanarts(buildSeason(season)));
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(Series series) {
        return createArtworkDetails(seriesArtworkScanner.getFanarts(buildSeries(series)));
    }

    @Override
    public List<ArtworkDetailDTO> getBanners(Season season) {
        return createArtworkDetails(seriesArtworkScanner.getBanners(buildSeason(season)));
    }

    @Override
    public List<ArtworkDetailDTO> getBanners(Series series) {
        return createArtworkDetails(seriesArtworkScanner.getBanners(buildSeries(series)));
    }

    @Override
    public List<ArtworkDetailDTO> getVideoImages(VideoData videoData) {
        return createArtworkDetails(seriesArtworkScanner.getVideoImages(buildEpisode(videoData)));
    }

    private static SeriesDTO buildSeries(Series series) {
        return new SeriesDTO(series.getIdMap())
            .setTitle(series.getTitle())
            .setOriginalTitle(series.getTitleOriginal())
            .setStartYear(series.getStartYear())
            .setEndYear(series.getEndYear());
    }

    private static SeasonDTO buildSeason(Season season) {
        return new SeasonDTO(season.getIdMap(), season.getSeason())
            .setTitle(season.getTitle())
            .setOriginalTitle(season.getTitleOriginal())
            .setYear(season.getPublicationYear())
            .setSeries(buildSeries(season.getSeries()));
    }

    private static EpisodeDTO buildEpisode(VideoData videoData) {
        return new EpisodeDTO(videoData.getIdMap(), videoData.getEpisode())
            .setTitle(videoData.getTitle())
            .setOriginalTitle(videoData.getTitleOriginal())
            .setSeason(buildSeason(videoData.getSeason()));
    }
}
