package org.yamj.core.database.model;

import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.OverrideFlag;
import org.yamj.core.hibernate.usertypes.EnumStringUserType;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.*;

/**
 * Abstract implementation of an metadata object.
 */
@TypeDefs({
    @TypeDef(name = "overrideFlag",
            typeClass = EnumStringUserType.class,
            parameters = {
        @Parameter(name = "enumClassName", value = "org.yamj.core.database.model.type.OverrideFlag")}),
    @TypeDef(name = "statusType",
            typeClass = EnumStringUserType.class,
            parameters = {
        @Parameter(name = "enumClassName", value = "org.yamj.common.type.StatusType")})
})
@MappedSuperclass
public abstract class AbstractMetadata extends AbstractAuditable
        implements IMetadata, Serializable {

    private static final long serialVersionUID = -556558470067852056L;
    /**
     * This will be generated from a scanned file name.
     */
    @NaturalId
    @Column(name = "identifier", length = 200, nullable = false)
    protected String identifier;
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    @Column(name = "title_original", length = 255)
    private String titleOriginal;
    @Lob
    @Column(name = "plot", length = 50000)
    private String plot;
    @Lob
    @Column(name = "outline", length = 50000)
    private String outline;
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;

    // GETTER and SETTER
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    public void setTitle(String title, String source) {
        if (!StringUtils.isBlank(title)) {
            setTitle(title);
            setOverrideFlag(OverrideFlag.TITLE, source);
        }
    }

    @Override
    public String getTitleOriginal() {
        return titleOriginal;
    }

    private void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public void setTitleOriginal(String titleOriginal, String source) {
        if (StringUtils.isNotBlank(titleOriginal)) {
            setTitleOriginal(titleOriginal);
            setOverrideFlag(OverrideFlag.ORIGINALTITLE, source);
        }
    }

    public String getPlot() {
        return plot;
    }

    private void setPlot(String plot) {
        this.plot = plot;
    }

    public void setPlot(String plot, String source) {
        if (StringUtils.isNotBlank(plot)) {
            setPlot(plot);
            setOverrideFlag(OverrideFlag.PLOT, source);
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
            setOutline(outline);
            setOverrideFlag(OverrideFlag.OUTLINE, source);
        }
    }

    @Override
    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    @Override
    public final int getYear() {
        if (this instanceof VideoData) {
            return ((VideoData) this).getPublicationYear();
        } else if (this instanceof Series) {
            return ((Series) this).getStartYear();
        }
        // TODO season get year from first aired date
        return -1;
    }

    @Override
    public final int getSeasonNumber() {
        if (this instanceof Season) {
            return ((Season) this).getSeason();
        }
        return -1;
    }

    @Override
    public final int getEpisodeNumber() {
        if (this instanceof VideoData) {
            return ((VideoData) this).getEpisode();
        }
        return -1;
    }

    public abstract String getOverrideSource(OverrideFlag overrideFlag);

    public abstract void setOverrideFlag(OverrideFlag overrideFlag, String source);
}
