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
package org.yamj.core.database.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.OverrideFlag;

/**
 * Abstract implementation of an metadata object.
 */
@MappedSuperclass
@SuppressWarnings("unused")
public abstract class AbstractMetadata extends AbstractAuditable
        implements IMetadata, Serializable {

    private static final long serialVersionUID = -556558470067852056L;
    
    /**
     * This will be generated from a scanned file name.
     */
    @NaturalId
    @Column(name = "identifier", length = 200, nullable = false)
    private String identifier;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "title_original", length = 255)
    private String titleOriginal;

    @Column(name = "title_sort", nullable = false, length = 255)
    private String titleSort;

    @Lob
    @Column(name = "plot", length = 50000)
    private String plot;
    
    @Lob
    @Column(name = "outline", length = 50000)
    private String outline;
    
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "last_scanned")
    private Date lastScanned;

    @Column(name = "retries", nullable = false)
    private int retries = 0;

    @Transient
    private Set<String> modifiedSources;
    
    // GETTER and SETTER
    
    public final String getIdentifier() {
        return identifier;
    }

    protected final void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public final String getTitle() {
        return title;
    }

    private final void setTitle(String title) {
        this.title = title;
    }

    public final void setTitle(String title, String source) {
        if (StringUtils.isNotBlank(title)) {
            this.title = title.trim();
            setOverrideFlag(OverrideFlag.TITLE, source);
        }
    }

    @Override
    public final String getTitleOriginal() {
        return titleOriginal;
    }

    private final void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public final void setTitleOriginal(String titleOriginal, String source) {
        if (StringUtils.isNotBlank(titleOriginal)) {
            this.titleOriginal = titleOriginal.trim();
            setOverrideFlag(OverrideFlag.ORIGINALTITLE, source);
        }
    }

    @Override
    public String getTitleSort() {
        return titleSort;
    }

    public void setTitleSort(String titleSort) {
        this.titleSort = titleSort;
    }

    public final String getPlot() {
        return plot;
    }

    private final void setPlot(String plot) {
        this.plot = plot;
    }

    public final void setPlot(String plot, String source) {
        if (StringUtils.isNotBlank(plot)) {
            this.plot = plot.trim();
            setOverrideFlag(OverrideFlag.PLOT, source);
        }
    }

    public final String getOutline() {
        return outline;
    }

    private final void setOutline(String outline) {
        this.outline = outline;
    }

    public final void setOutline(String outline, String source) {
        if (StringUtils.isNotBlank(outline)) {
            this.outline = outline.trim();
            setOverrideFlag(OverrideFlag.OUTLINE, source);
        }
    }

    @Override
    public final StatusType getStatus() {
        return status;
    }
    
    public final void setStatus(StatusType status) {
        this.status = status;
    }

    @Override
    public final Date getLastScanned() {
        return lastScanned;
    }

    public final void setLastScanned(Date lastScanned) {
        this.lastScanned = lastScanned;
    }

    @Override
    public final int getRetries() {
        return retries;
    }

    public final void setRetries(int retries) {
        this.retries = retries;
    }
    
    @Override
    public final int getYear() {
        if (this instanceof VideoData) {
            return ((VideoData) this).getPublicationYear();
        } else if (this instanceof Season) {
            return ((Season) this).getPublicationYear();
        } else if (this instanceof Series) {
            return ((Series) this).getStartYear();
        }
        return -1;
    }

    @Override
    public int getEpisodeNumber() {
        return -1;
    }

    @Override
    public boolean isMovie() {
        return false;
    }

    protected final void addModifiedSource(String sourceDb) {
        if (modifiedSources == null) modifiedSources = new HashSet<>();
        modifiedSources.add(sourceDb);
    }

    public final boolean hasModifiedSource() {
        return CollectionUtils.isNotEmpty(modifiedSources);
    }
    
    public final Set<String> getModifiedSources() {
        return modifiedSources;
    }

    // ABSTRACT DECLARATIONS
    
    abstract boolean removeOverrideSource(String sourceDb);
}
