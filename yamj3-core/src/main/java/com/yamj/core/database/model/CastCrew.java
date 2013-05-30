package com.yamj.core.database.model;

import com.yamj.core.database.model.type.JobType;
import com.yamj.core.hibernate.usertypes.EnumStringUserType;
import java.io.Serializable;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Parameter;

@TypeDef(name = "jobType",
        typeClass = EnumStringUserType.class,
        parameters = {
    @Parameter(name = "enumClassName", value = "com.yamj.core.database.model.type.JobType")})
@Entity
@Table(name = "cast_crew")
public class CastCrew extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -3941301942248344131L;
    
    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_CASTCREW_PERSON")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;
    
    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_CASTCREW_VIDEODATA")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "videodata_id")
    private VideoData videoData;
    
    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_CASTCREW_SERIES")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "series_id")
    private Series series;
    
    @NaturalId
    @Type(type = "jobType")
    @Column(name = "job", nullable = false, length = 30)
    private JobType jobType;
    
    @Column(name = "role", length = 255)
    private String role;

    // GETTER and SETTER
    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
