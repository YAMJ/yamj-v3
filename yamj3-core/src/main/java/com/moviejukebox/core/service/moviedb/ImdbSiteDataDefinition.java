package com.moviejukebox.core.service.moviedb;

import java.nio.charset.Charset;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class ImdbSiteDataDefinition {

    private final String site;
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
    private final Charset charset;
    private final Pattern personRegex;
    private final Pattern titleRegex;

    public ImdbSiteDataDefinition(String site,
            String charsetName,
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
            String originalTitle) {
        this.site = site;
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

        if (StringUtils.isBlank(charsetName)) {
            this.charset = Charset.defaultCharset();
        } else {
            this.charset = Charset.forName(charsetName);
        }

        personRegex = Pattern.compile(Pattern.quote("<link rel=\"canonical\" href=\"" + this.getSite() + "name/(nm\\d+)/\""));
        titleRegex = Pattern.compile(Pattern.quote("<link rel=\"canonical\" href=\"" + this.getSite() + "title/(tt\\d+)/\""));
    }

    public String getSite() {
        return site;
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

    public Charset getCharset() {
        return charset;
    }

    public Pattern getPersonRegex() {
        return personRegex;
    }

    public Pattern getTitleRegex() {
        return titleRegex;
    }
}
