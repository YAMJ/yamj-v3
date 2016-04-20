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
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.plugin.api.artwork.MovieArtworkScanner;
import org.yamj.plugin.api.metadata.dto.MovieDTO;

public class PluginMovieArtworkScanner extends PluginArtworkScanner implements IMovieArtworkScanner {

    private final MovieArtworkScanner movieArtworkScanner;
    
    public PluginMovieArtworkScanner(MovieArtworkScanner movieArtworkScanner, LocaleService localeService) {
        super(localeService);
        this.movieArtworkScanner = movieArtworkScanner;
    }
    
    @Override
    public String getScannerName() {
        return movieArtworkScanner.getScannerName();
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(VideoData videoData) {
        return createArtworkDetails(movieArtworkScanner.getPosters(buildMovie(videoData)));
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(VideoData videoData) {
        return createArtworkDetails(movieArtworkScanner.getFanarts(buildMovie(videoData)));
    }
    
    private static MovieDTO buildMovie(VideoData videoData) {
        return new MovieDTO(videoData.getIdMap())
            .setTitle(videoData.getTitle())
            .setOriginalTitle(videoData.getTitleOriginal())
            .setYear(videoData.getYear());
    }
}
