/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.service.artwork.poster;

import java.util.List;
import javax.annotation.Resource;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.yamj.core.service.artwork.ArtworkDetailDTO;

@ContextConfiguration(locations = {"classpath:spring-test.xml"})
public class YahooPosterScannerTest extends AbstractJUnit4SpringContextTests {

    @Resource(name = "yahooPosterScanner")
    private YahooPosterScanner yahooPosterScanner;

    @Test
    public void testPosterUrl() {
        List<ArtworkDetailDTO> dtos = yahooPosterScanner.getPosters("Avatar", 2009);
        if (dtos != null) {
            for (ArtworkDetailDTO dto : dtos) {
                System.err.println(dto);
            }
        }
    }
}