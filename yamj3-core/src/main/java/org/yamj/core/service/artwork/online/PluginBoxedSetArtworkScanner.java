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
import org.yamj.core.database.model.BoxedSet;
import org.yamj.plugin.api.artwork.ArtworkDTO;
import org.yamj.plugin.api.artwork.BoxedSetArtworkScanner;
import org.yamj.plugin.api.model.IBoxedSet;
import org.yamj.plugin.api.model.mock.BoxedSetMock;

public class PluginBoxedSetArtworkScanner implements IBoxedSetArtworkScanner {

    private final BoxedSetArtworkScanner boxedSetArtworkScanner;
    
    public PluginBoxedSetArtworkScanner(BoxedSetArtworkScanner boxedSetArtworkScanner) {
        this.boxedSetArtworkScanner = boxedSetArtworkScanner;
    }
    
    @Override
    public String getScannerName() {
        return boxedSetArtworkScanner.getScannerName();
    }

    @Override
    public List<ArtworkDTO> getPosters(BoxedSet boxedSet) {
        return boxedSetArtworkScanner.getPosters(buildBoxedSet(boxedSet));
    }

    @Override
    public List<ArtworkDTO> getFanarts(BoxedSet boxedSet) {
        return boxedSetArtworkScanner.getFanarts(buildBoxedSet(boxedSet));
    }

    @Override
    public List<ArtworkDTO> getBanners(BoxedSet boxedSet) {
        return boxedSetArtworkScanner.getBanners(buildBoxedSet(boxedSet));
    }
    
    private static IBoxedSet buildBoxedSet(BoxedSet boxedSet) {
        BoxedSetMock mock = new BoxedSetMock(boxedSet.getIdMap());
        mock.setName(boxedSet.getName());
        return mock;
    } 
}
