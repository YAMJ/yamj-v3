package org.yamj.core.database.model.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class RatingDTO {

    private String sourcedb;
    private String rating;

    public RatingDTO(String sourcedb, String rating) {
        this.sourcedb = sourcedb;
        this.rating = rating;
    }
    
    public String getSourcedb() {
        return sourcedb;
    }

    public void setSourcedb(String sourcedb) {
        this.sourcedb = sourcedb;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
