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
import javax.annotation.Resource;
import org.junit.Test;
import org.yamj.core.AbstractTest;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.plugin.api.artwork.ArtworkDTO;

public class ImdbArtworkScannerTest extends AbstractTest {

    @Resource(name = "imdbArtworkScanner")
    private ImdbArtworkScanner imdbArtworkScanner;

    @Test
    public void testMoviePosters() {
        VideoData videoData = new VideoData();
        videoData.setSourceDbId(imdbArtworkScanner.getScannerName(), "tt0499549");

        List<ArtworkDTO> dtos = imdbArtworkScanner.getPosters(videoData);
        logArtworks(ArtworkType.POSTER, dtos, getClass());
    }

    @Test
    public void testMovieFanarts() {
        VideoData videoData = new VideoData();
        videoData.setSourceDbId(imdbArtworkScanner.getScannerName(), "tt0499549");

        List<ArtworkDTO> dtos = imdbArtworkScanner.getFanarts(videoData);
        logArtworks(ArtworkType.FANART, dtos, getClass());
    }
}