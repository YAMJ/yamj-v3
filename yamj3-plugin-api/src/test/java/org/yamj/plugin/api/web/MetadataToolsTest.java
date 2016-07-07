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
package org.yamj.plugin.api.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.yamj.plugin.api.metadata.MetadataTools;

@SuppressWarnings("all")
public class MetadataToolsTest {

    @Test
    public void cleanRole() {
        assertEquals("Matthew", MetadataTools.cleanRole("Matthew (voice"));
        assertEquals("Matthew", MetadataTools.cleanRole("Matthew (uncredited"));
        assertEquals("Matthew", MetadataTools.cleanRole("Matthew (2 episodes"));
        assertEquals("Matthew", MetadataTools.cleanRole("Matthew (5 episodes, 2011) (voice)"));
        assertNull(MetadataTools.cleanRole("(voice)"));
    }
}
