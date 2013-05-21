package com.yamj.core.database.model.dto;

import com.yamj.core.database.model.type.JobType;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class CreditDTO {

    private String name;
    private JobType jobType;
    private String role;
    private String aka;
    private String sourcedb;
    private String sourcedbId;

    public CreditDTO() {
    }

    public CreditDTO(JobType jobType, String name) {
        this(jobType, name, null);
    }

    public CreditDTO(JobType jobType, String name, String role) {
        this.jobType = jobType;
        this.name = name;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getAka() {
        return aka;
    }

    public void setAka(String aka) {
        this.aka = aka;
    }

    public String getSourcedb() {
        return sourcedb;
    }

    public void setSourcedb(String sourcedb) {
        this.sourcedb = sourcedb;
    }

    public String getSourcedbId() {
        return sourcedbId;
    }

    public void setSourcedbId(String sourcedbId) {
        this.sourcedbId = sourcedbId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
