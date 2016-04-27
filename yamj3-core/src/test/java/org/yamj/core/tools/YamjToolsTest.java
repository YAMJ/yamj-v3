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
package org.yamj.core.tools;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YamjToolsTest {

    private static final Logger LOG = LoggerFactory.getLogger(YamjToolsTest.class);

    @Test
    public void testSplitFullName() {
        LOG.info("Splitted person: {}", YamjTools.splitFullName("David O'Meara"));
        LOG.info("Splitted person: {}", YamjTools.splitFullName("Marco van Basten"));
        LOG.info("Splitted person: {}", YamjTools.splitFullName("Willbur Van de Pömpel"));
        LOG.info("Splitted person: {}", YamjTools.splitFullName("Marilyn de Queiroz"));
        LOG.info("Splitted person: {}", YamjTools.splitFullName("Jim Yingst"));
        LOG.info("Splitted person: {}", YamjTools.splitFullName("Sting"));
        LOG.info("Splitted person: {}", YamjTools.splitFullName("Rebecca Romijn-Stamos"));
        LOG.info("Splitted person: {}", YamjTools.splitFullName("Steve zu Dingsda"));
        LOG.info("Splitted person: {}", YamjTools.splitFullName("Holla die Waldfee"));
        LOG.info("Splitted person: {}", YamjTools.splitFullName("Edward Samuel Norton"));
    }
}
