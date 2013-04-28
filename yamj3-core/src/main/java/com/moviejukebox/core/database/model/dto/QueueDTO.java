package com.moviejukebox.core.database.model.dto;

import java.util.Date;

public class QueueDTO {

    private Long id;
    private Date date;
    private String type;

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

    private String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isVideoDataElement() {
        return "videodata".equals(this.getType());
    }

    public boolean isSeasonElement() {
        return "season".equals(this.getType());
    }

    public boolean isSeriesElement() {
        return "series".equals(this.getType());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QueueDTO [ID=");
        sb.append(getId());
        sb.append(", mediaType=");
        sb.append(getType());
        sb.append("]");
        return sb.toString();
    }
}
