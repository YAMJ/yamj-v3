/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package org.yamj.core.service.metadata.online;

import java.util.regex.Pattern;

public class ImdbSiteDataDefinition {

    private final String director;
    private final String cast;
    private final String releaseDate;
    private final String runtime;
    private final String aspectRatio;
    private final String country;
    private final String company;
    private final String genre;
    private final String quotes;
    private final String plot;
    private final String rated;
    private final String certification;
    private final String originalAirDate;
    private final String writer;
    private final String taglines;
    private final String originalTitle;
    private final Pattern personRegex;
    private final Pattern titleRegex;

    public ImdbSiteDataDefinition(
            String director,
            String cast,
            String releaseDate,
            String runtime,
            String aspectRatio,
            String country,
            String company,
            String genre,
            String quotes,
            String plot,
            String rated,
            String certification,
            String originalAirDate,
            String writer,
            String taglines,
            String originalTitle)
    {
        this.director = director;
        this.cast = cast;
        this.releaseDate = releaseDate;
        this.runtime = runtime;
        this.aspectRatio = aspectRatio;
        this.country = country;
        this.company = company;
        this.genre = genre;
        this.quotes = quotes;
        this.plot = plot;
        this.rated = rated;
        this.certification = certification;
        this.originalAirDate = originalAirDate;
        this.writer = writer;
        this.taglines = taglines;
        this.originalTitle = originalTitle;

        personRegex = Pattern.compile(Pattern.quote("<link rel=\"canonical\" href=\"http://www.imdb.com/name/(nm\\d+)/\""));
        titleRegex = Pattern.compile(Pattern.quote("<link rel=\"canonical\" href=\"http://www.imdb.com/title/(tt\\d+)/\""));
    }

    public String getDirector() {
        return director;
    }

    public String getCast() {
        return cast;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getRuntime() {
        return runtime;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public String getCountry() {
        return country;
    }

    public String getCompany() {
        return company;
    }

    public String getGenre() {
        return genre;
    }

    public String getQuotes() {
        return quotes;
    }

    public String getPlot() {
        return plot;
    }

    public String getRated() {
        return rated;
    }

    public String getCertification() {
        return certification;
    }

    public String getOriginalAirDate() {
        return originalAirDate;
    }

    public String getWriter() {
        return writer;
    }

    public String getTaglines() {
        return taglines;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public Pattern getPersonRegex() {
        return personRegex;
    }

    public Pattern getTitleRegex() {
        return titleRegex;
    }
}
