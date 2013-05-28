package com.yamj.core.database.model;

import com.yamj.core.hibernate.usertypes.EnumStringUserType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.yamj.common.type.StatusType;
import com.yamj.core.database.model.type.OverrideFlag;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

/**
 * Abstract implementation of an metadata object.
 */
@TypeDefs({
    @TypeDef(name = "overrideFlag",
            typeClass = EnumStringUserType.class,
            parameters = {@Parameter(name = "enumClassName", value = "com.yamj.core.database.model.type.OverrideFlag")}),
    @TypeDef(name = "statusType",
            typeClass = EnumStringUserType.class,
            parameters = {@Parameter(name = "enumClassName", value = "com.yamj.common.type.StatusType")})
})


@MappedSuperclass
public abstract class AbstractMetadata extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -556558470067852056L;

    /**
     * This is the video data identifier. This will be generated from a scanned file name.
     */
    @NaturalId
    @Column(name = "identifier", unique = true, length = 200)
    protected String identifier;
    
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

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public abstract void setOverrideFlag(OverrideFlag overrideFlag, String source);
}
