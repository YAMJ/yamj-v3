package com.moviejukebox.core.database.model.dto;

import com.moviejukebox.core.database.model.type.JobType;

public class CreditDTO {

    private String name;
    private JobType jobType;
    private String role;
    private String aka;
    private String moviedb;
    private String moviedbId;

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

    public String getMoviedb() {
        return moviedb;
    }

    public void setMoviedb(String moviedb) {
        this.moviedb = moviedb;
    }

    public String getMoviedbId() {
        return moviedbId;
    }

    public void setMoviedbId(String moviedbId) {
        this.moviedbId = moviedbId;
    }
}
