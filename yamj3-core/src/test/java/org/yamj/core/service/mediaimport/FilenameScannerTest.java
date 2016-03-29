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
package org.yamj.core.service.mediaimport;

import static org.junit.Assert.assertEquals;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.yamj.core.AbstractTest;
import org.yamj.core.database.model.StageDirectory;
import org.yamj.core.database.model.StageFile;

public class FilenameScannerTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(FilenameScannerTest.class);
    
    @Autowired
    private FilenameScanner scanner;

    private static StageFile createStageFile(String fileName) {
        StageDirectory dir = new StageDirectory();
        dir.setDirectoryPath("/movies");
        StageFile file = new StageFile();
        file.setStageDirectory(dir);
        file.setBaseName(FilenameUtils.removeExtension(fileName));
        file.setExtension(FilenameUtils.getExtension(fileName));
        return file;
    }
    
    @Test
    public void testFilenameMovieVersion_1() {
        String fileName = "Shrek (Director's Cut).bdrip.mkv";
        FilenameDTO dto = new FilenameDTO(createStageFile(fileName));
        scanner.scan(dto);
        LOG.info("testFilenameMovieVersion_1: {}", dto);
        assertEquals("Shrek", dto.getTitle());
        assertEquals("Director's Cut", dto.getMovieVersion());
        assertEquals("BluRay", dto.getVideoSource());
    }

    @Test
    public void testFilenameMovieVersion_2() {
        String fileName = "Avatar (2009) (Extended Version).sdtv.mkv";
        FilenameDTO dto = new FilenameDTO(createStageFile(fileName));
        scanner.scan(dto);
        LOG.info("testFilenameMovieVersion_2: {}", dto);
        assertEquals("Avatar", dto.getTitle());
        assertEquals(2009, dto.getYear());
        assertEquals("Extended Version", dto.getMovieVersion());
        assertEquals("SDTV", dto.getVideoSource());
    }
    
    @Test
    public void testFilenameExtra() {
        String fileName = "Skrek 2 [EXTRA Shrek 2 3D].bdrip.mkv";
        FilenameDTO dto = new FilenameDTO(createStageFile(fileName));
        scanner.scan(dto);
        LOG.info("testFilenameExtra: {}", dto);
        assertEquals("Skrek 2", dto.getTitle());
        assertEquals("EXTRA Shrek 2 3D", dto.getPartTitle());
        assertEquals(Boolean.TRUE, dto.isExtra());
        assertEquals("BluRay", dto.getVideoSource());
    }

    @Test
    public void testFilenameTrailer() {
        String fileName = "Skrek 2 [TRAILER Shrek 2].bdrip.mkv";
        FilenameDTO dto = new FilenameDTO(createStageFile(fileName));
        scanner.scan(dto);
        LOG.info("testFilenameTrailer: {}", dto);
        assertEquals("Skrek 2", dto.getTitle());
        assertEquals("TRAILER Shrek 2", dto.getPartTitle());
        assertEquals(Boolean.TRUE, dto.isExtra());
        assertEquals("BluRay", dto.getVideoSource());
    }

    @Test
    public void testFilenamePart() {
        String fileName = "Skrek 2 (Extended Cut) [Part1 - Der Erste Teil].bdrip.mkv";
        FilenameDTO dto = new FilenameDTO(createStageFile(fileName));
        scanner.scan(dto);
        LOG.info("testFilenamePart: {}", dto);
        assertEquals("Skrek 2", dto.getTitle());
        assertEquals("Extended Cut", dto.getMovieVersion());
        assertEquals(1, dto.getPart());
        assertEquals("Der Erste Teil", dto.getPartTitle());
        assertEquals("BluRay", dto.getVideoSource());
    }

    @Test
    public void testFilenameSet() {
        //String fileName = "Star Wars I [SET Star Wars].bdrip.mk";
        String fileName = "Le Seigneur des anneaux le retour du roi [SET Le seigneur des anneaux-3] - 720p.bluray.x264.mkv";
        FilenameDTO dto = new FilenameDTO(createStageFile(fileName));
        scanner.scan(dto);
        LOG.info("testFilenameSet: {}", dto);
        assertEquals("Le Seigneur des anneaux le retour du roi", dto.getTitle());
        assertEquals("BluRay", dto.getVideoSource());
        assertEquals("720p", dto.getHdResolution());
        assertEquals("H.264", dto.getVideoCodec());
        assertEquals(1, dto.getSetMap().size());
        assertEquals("Le seigneur des anneaux", dto.getSetMap().keySet().iterator().next());
        assertEquals(Integer.valueOf(3), dto.getSetMap().values().iterator().next());
    }
}
