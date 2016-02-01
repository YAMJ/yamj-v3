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
package org.yamj.core.service.trakttv;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.yamj.api.trakttv.model.Ids;

public class TraktTvMovie {

    private Ids ids;
    private String title;
    private String overview;
    private String tagline;
    private Integer year;
    private Double rating;
    private String certification;
    private Date releaseDate;
    private String releaseCountry;
    private List<String> genres;

    public Ids getIds() {
        return ids;
    }

    public void setIds(Ids ids) {
        this.ids = ids;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (StringUtils.isNotBlank(title)) {
            this.title = title;
        }
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        if (StringUtils.isNotBlank(overview)) {
            this.overview = overview;
        }
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        if (StringUtils.isNotBlank(tagline)) {
            this.tagline = tagline;
        }
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getCertification() {
        return certification;
    }

    public void setCertification(String certification) {
        this.certification = certification;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getReleaseCountry() {
        return releaseCountry;
    }

    public void setReleaseCountry(String releaseCountry) {
        this.releaseCountry = releaseCountry;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }
}
