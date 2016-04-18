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
    public String getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    public void setTitle(String title, String source) {
        if (StringUtils.isNotBlank(title)) {
            setTitle(title.trim());
            setOverrideFlag(OverrideFlag.TITLE, source);
        }
    }

    public void removeTitle(String source) {
        if (hasOverrideSource(OverrideFlag.TITLE, source)) {
            String[] splitted = getIdentifier().split("_");
            setTitle(splitted[0]);
            removeOverrideFlag(OverrideFlag.TITLE);
        }
    }

    @Override
    public String getTitleOriginal() {
        return titleOriginal;
    }

    protected void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public void setTitleOriginal(String titleOriginal, String source) {
        if (StringUtils.isNotBlank(titleOriginal)) {
            setTitleOriginal(titleOriginal.trim());
            setOverrideFlag(OverrideFlag.ORIGINALTITLE, source);
        }
    }

    public void removeTitleOriginal(String source) {
        if (hasOverrideSource(OverrideFlag.ORIGINALTITLE, source)) {
            String[] splitted = getIdentifier().split("_");
            setTitleOriginal(splitted[0]);
            removeOverrideFlag(OverrideFlag.ORIGINALTITLE);
        }
    }

    public final boolean isTitleOriginalScannable() {
        if (StringUtils.isBlank(getTitleOriginal())) {
            return false;
        }
        return !StringUtils.equalsIgnoreCase(getTitle(), getTitleOriginal());
    }
    
    @Override
    public String getTitleSort() {
        return titleSort;
    }

    public void setTitleSort(String titleSort) {
        this.titleSort = titleSort;
    }

    public String getPlot() {
        return plot;
    }

    private void setPlot(String plot) {
        this.plot = plot;
    }

    public void setPlot(String plot, String source) {
        if (StringUtils.isNotBlank(plot)) {
            setPlot(plot.trim());
            setOverrideFlag(OverrideFlag.PLOT, source);
        }
    }

    public void removePlot(String source) {
        if (hasOverrideSource(OverrideFlag.PLOT, source)) {
            setPlot(null);
            removeOverrideFlag(OverrideFlag.PLOT);
        }
    }

    public String getOutline() {
        return outline;
    }

    private void setOutline(String outline) {
        this.outline = outline;
    }

    public void setOutline(String outline, String source) {
        if (StringUtils.isNotBlank(outline)) {
            setOutline(outline.trim());
            setOverrideFlag(OverrideFlag.OUTLINE, source);
        }
    }

    public void removeOutline(String source) {
        if (hasOverrideSource(OverrideFlag.OUTLINE, source)) {
            setOutline(null);
            removeOverrideFlag(OverrideFlag.OUTLINE);
        }
    }

    @Override
    public int getYear() {
        if (this instanceof VideoData) {
            return ((VideoData) this).getPublicationYear();
        } else if (this instanceof Season) {
            return ((Season) this).getPublicationYear();
        } else if (this instanceof Series) {
            return ((Series) this).getStartYear();
        }
        return -1;
    }
}
