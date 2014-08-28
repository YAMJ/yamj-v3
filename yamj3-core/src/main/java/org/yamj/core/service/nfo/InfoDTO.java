/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package org.yamj.core.service.nfo;

import java.util.*;
import org.apache.commons.lang3.StringUtils;

public final class InfoDTO {

    private boolean changed = false;
    private boolean tvShow = false;
    private List<String> skipOnlineScans = new ArrayList<String>();
    private Map<String, String> ids = new HashMap<String, String>();
    private String title;
    private String titleOriginal;
    private String titleSort;
    private int year;
    private int rating;
    private String runtime;
    private String certification;
    private String plot;
    private String outline;
    private String tagline;
    private String quote;
    private String company;
    private Set<String> genres = new HashSet<String>();

    public InfoDTO() {
        this(false);
    }

    public InfoDTO(boolean tvShow) {
        this.tvShow = tvShow;
    }

    public boolean isChanged() {
        return changed;
    }

    public boolean isTvShow() {
        return tvShow;
    }

    public void setTvShow(boolean tvShow) {
        if (this.tvShow != tvShow) {
            this.tvShow = tvShow;
            this.changed = true;
        }
    }

    public void setSkipAllOnlineScans() {
        this.skipOnlineScans.clear();
        this.skipOnlineScans.add("all");
    }

    public String getSkipOnlineScans() {
        if (this.skipOnlineScans.isEmpty()) {
            return null;
        }
        return StringUtils.join(this.skipOnlineScans, ";");
    }

    public Map<String, String> getIds() {
        return ids;
    }

    public void addId(String sourceDb, String sourceId) {
        if (StringUtils.isNotBlank(sourceDb)
                && StringUtils.isNotBlank(sourceId)) {
            if ("-1".equals(sourceId)) {
                // skip online scan
                if (!this.skipOnlineScans.contains("all")) {
                    this.skipOnlineScans.add(sourceDb);
                }
            } else {
                this.ids.put(sourceDb, sourceId);
            }
            this.changed = true;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (StringUtils.isNotBlank(title)) {
            this.title = title;
            this.changed = true;
        }
    }

    public String getTitleOriginal() {
        return titleOriginal;
    }

    public void setTitleOriginal(String titleOriginal) {
        if (StringUtils.isNotBlank(titleOriginal)) {
            this.titleOriginal = titleOriginal;
        }
    }

    public String getTitleSort() {
        return titleSort;
    }

    public void setTitleSort(String titleSort) {
        if (StringUtils.isNotBlank(titleSort)) {
            this.titleSort = titleSort;
            this.changed = true;
        }
    }

    public int getYear() {
        return year;
    }

    public void setYear(String year) {
        if (StringUtils.isNumeric(year) && year.length() == 4) {
            try {
                this.year = Integer.parseInt(year);
                this.changed = true;
            } catch (Exception e) {
                // ignore integer parse error
            }
        }
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        if (rating >= 0) {
            this.rating = rating;
            this.changed = true;
        }
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        if (StringUtils.isNotBlank(runtime)) {
            this.runtime = runtime;
            this.changed = true;
        }
    }

    public String getCertification() {
        return certification;
    }

    public void setCertification(String certification) {
        if (StringUtils.isNotBlank(certification)) {
            this.certification = certification;
            this.changed = true;
        }
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        if (StringUtils.isNotBlank(plot)) {
            this.plot = plot;
            this.changed = true;
        }
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        if (StringUtils.isNotBlank(outline)) {
            this.outline = outline;
            this.changed = true;
        }
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        if (StringUtils.isNotBlank(tagline)) {
            this.tagline = tagline;
            this.changed = true;
        }
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        if (StringUtils.isNotBlank(quote)) {
            this.quote = quote;
            this.changed = true;
        }
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        if (StringUtils.isNotBlank(company)) {
            this.company = company;
            this.changed = true;
        }
    }

    public Set<String> getGenres() {
        return genres;
    }

    public void adGenre(String genre) {
        if (StringUtils.isNotBlank(genre)) {
            this.genres.add(genre);
            this.changed = true;
        }
    }
}
