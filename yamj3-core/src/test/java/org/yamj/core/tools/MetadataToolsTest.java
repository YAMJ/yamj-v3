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

public class MetadataToolsTest {

    @Test
    public void testSplitFullName() {
        System.err.println(MetadataTools.splitFullName("David O'Meara"));
        System.err.println(MetadataTools.splitFullName("Marco van Basten"));
        System.err.println(MetadataTools.splitFullName("Willbur Van de Pömpel"));
        System.err.println(MetadataTools.splitFullName("Marilyn de Queiroz"));
        System.err.println(MetadataTools.splitFullName("Jim Yingst"));
        System.err.println(MetadataTools.splitFullName("Sting"));
        System.err.println(MetadataTools.splitFullName("Rebecca Romijn-Stamos"));
        System.err.println(MetadataTools.splitFullName("Steve zu Dingsda"));
        System.err.println(MetadataTools.splitFullName("Holla die Waldfee"));
        System.err.println(MetadataTools.splitFullName("Edward Samuel Norton"));
    }
}
