/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
@TypeDefs({
    @TypeDef(name = "statusType", typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "org.yamj.common.type.StatusType")}),
    @TypeDef(name = "stepType", typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "org.yamj.core.database.model.type.StepType")}),
    @TypeDef(name = "artworkType", typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "org.yamj.core.database.model.type.ArtworkType")}),
    @TypeDef(name = "overrideFlag", typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "org.yamj.core.database.model.type.OverrideFlag")}),
    @TypeDef(name = "jobType", typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "org.yamj.core.database.model.type.JobType")}),
    @TypeDef(name = "fileType", typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "org.yamj.core.database.model.type.FileType")})
})
package org.yamj.core.database.model;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.yamj.core.hibernate.usertypes.EnumStringUserType;
