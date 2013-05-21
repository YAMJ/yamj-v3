package com.yamj.core.database.model.dto;

import com.yamj.core.database.model.type.MetaDataType;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class QueueDTO {

    private Long id;
    private Date date;
    private MetaDataType type;

    // GETTER and SETTER
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public MetaDataType getType() {
        return type;
    }

    public void setType(String type) {
        this.type = MetaDataType.fromString(type);
    }

    public void setType(MetaDataType type) {
        this.type = type;
    }

    public boolean isType(MetaDataType type) {
        return this.type == type;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
