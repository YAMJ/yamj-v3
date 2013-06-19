/**
 * Provides the domain model.
 */
@TypeDefs({
    @TypeDef(name = "statusType", typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "org.yamj.common.type.StatusType")}),
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
