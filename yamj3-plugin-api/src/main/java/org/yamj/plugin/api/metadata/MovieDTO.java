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
package org.yamj.plugin.api.metadata;

import java.util.*;
import org.apache.commons.lang3.StringUtils;

public class MovieDTO {

    private final Map<String, String> ids;
    private String title;
    private String originalTitle;
    private int year;
    private String plot;
    private String outline;
    private String tagline;
    private String quote;
    private String releaseCountry;
    private Date releaseDate;
    private int rating = -1;
    private Collection<String> genres = new HashSet<>();
    private Collection<String> studios = new HashSet<>();
    private Collection<String> countries = new HashSet<>();
    private Map<String,String> certifications = new HashMap<>();
    private Set<AwardDTO> awards = new HashSet<>();
    private List<CreditDTO> credits = new ArrayList<>();
    
    public MovieDTO(Map<String, String> ids) {
        this.ids = ids;
    }

    public Map<String, String> getIds() {
        return ids;
    }

    public MovieDTO addId(String source, String id) {
        if (StringUtils.isNotBlank(id)) {
            this.ids.put(source, id);
        }
        return this;
    }

    public String getTitle() {
        return title;
    }

    public MovieDTO setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public MovieDTO setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        return this;
    }

    public int getYear() {
        return year;
    }

    public MovieDTO setYear(int year) {
        this.year = year;
        return this;
    }

    public String getPlot() {
        return plot;
    }

    public MovieDTO setPlot(String plot) {
        this.plot = plot;
        return this;
    }

    public String getOutline() {
        return outline;
    }

    public MovieDTO setOutline(String outline) {
        this.outline = outline;
        return this;
    }

    public String getTagline() {
        return tagline;
    }

    public MovieDTO setTagline(String tagline) {
        this.tagline = tagline;
        return this;
    }

    public String getQuote() {
        return quote;
    }

    public MovieDTO setQuote(String quote) {
        this.quote = quote;
        return this;
    }

    public String getReleaseCountry() {
        return releaseCountry;
    }

    public MovieDTO setReleaseCountry(String releaseCountry) {
        this.releaseCountry = releaseCountry;
        return this;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public MovieDTO setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
        return this;
    }

    public int getRating() {
        return rating;
    }

    public MovieDTO setRating(int rating) {
        this.rating = rating;
        return this;
    }

    public Collection<String> getStudios() {
        return studios;
    }

    public MovieDTO setStudios(Collection<String> studios) {
        this.studios = studios;
        return this;
    }

    public MovieDTO addStudio(String studio) {
        if (studio != null) {
            this.studios.add(studio);
        }
        return this;
    }

    public Collection<String> getGenres() {
        return genres;
    }

    public MovieDTO setGenres(Collection<String> genres) {
        this.genres = genres;
        return this;
    }
    
    public MovieDTO addGenre(String genre) {
        if (genre != null) {
            this.genres.add(genre);
        }
        return this;
    }

    public Collection<String> getCountries() {
        return countries;
    }

    public MovieDTO setCountries(Collection<String> countries) {
        this.countries = countries;
        return this;
    }

    public MovieDTO addCountry(String country) {
        if (country != null) {
            this.countries.add(country);
        }
        return this;
    }
    
    public Map<String, String> getCertifications() {
        return certifications;
    }

    public MovieDTO addCertification(String country, String certificate) {
        if (StringUtils.isNotBlank(country) && StringUtils.isNotBlank(certificate)) {
            this.certifications.put(country, certificate);
        }
        return this;
    }

    public Set<AwardDTO> getAwards() {
        return awards;
    }

    public MovieDTO addAward(AwardDTO award) {
        if (award != null) {
            this.awards.add(award);
        }
        return this;
    }
    
    public List<CreditDTO> getCredits() {
        return credits;
    }

    public MovieDTO setCredits(List<CreditDTO> credits) {
        this.credits = credits;
        return this;
    }
    
    public MovieDTO addCredit(CreditDTO credit) {
        this.credits.add(credit);
        return this;
    }

    public MovieDTO addCredits(Collection<CreditDTO> credits) {
        if (credits != null && !credits.isEmpty()) {
            this.credits.addAll(credits);
        }
        return this;
    }
}