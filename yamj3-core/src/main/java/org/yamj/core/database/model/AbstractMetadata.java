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

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

import org.apache.commons.lang3.StringUtils;
import org.yamj.core.database.model.type.OverrideFlag;

/**
 * Abstract implementation of an metadata object.
 */
@MappedSuperclass
@SuppressWarnings("unused")
public abstract class AbstractMetadata extends AbstractScannable
        implements IMetadata {

    private static final long serialVersionUID = -556558470067852056L;
    
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
    
    // CONSTRUCTORS
    
    public AbstractMetadata() {
        super();
    }
    
    public AbstractMetadata(String identifier) {
        super(identifier);
    }
    
    // GETTER and SETTER
    
    @Override
    public final String getTitle() {
        return title;
    }

    protected final void setTitle(String title) {
        this.title = title;
    }

    public final void setTitle(String title, String source) {
        if (StringUtils.isNotBlank(title)) {
            this.title = title.trim();
            setOverrideFlag(OverrideFlag.TITLE, source);
        }
    }

    public void removeTitle(String source) {
        if (hasOverrideSource(OverrideFlag.TITLE, source)) {
            String[] splitted = this.getIdentifier().split("_");
            this.title = splitted[0];
            removeOverrideFlag(OverrideFlag.TITLE);
        }
    }

    @Override
    public final String getTitleOriginal() {
        return titleOriginal;
    }

    protected final void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public final void setTitleOriginal(String titleOriginal, String source) {
        if (StringUtils.isNotBlank(titleOriginal)) {
            this.titleOriginal = titleOriginal.trim();
            setOverrideFlag(OverrideFlag.ORIGINALTITLE, source);
        }
    }

    public void removeTitleOriginal(String source) {
        if (hasOverrideSource(OverrideFlag.ORIGINALTITLE, source)) {
            String[] splitted = this.getIdentifier().split("_");
            this.titleOriginal = splitted[0];
            removeOverrideFlag(OverrideFlag.ORIGINALTITLE);
        }
    }

    public boolean isTitleOriginalScannable() {
        if (StringUtils.isBlank(this.titleOriginal)) {
            return false;
        }
        return !StringUtils.equalsIgnoreCase(this.title, this.titleOriginal);
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

    public final void removePlot(String source) {
        if (hasOverrideSource(OverrideFlag.PLOT, source)) {
            this.plot = null;
            removeOverrideFlag(OverrideFlag.PLOT);
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

    public final void removeOutline(String source) {
        if (hasOverrideSource(OverrideFlag.OUTLINE, source)) {
            this.outline = null;
            removeOverrideFlag(OverrideFlag.OUTLINE);
        }
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
}
