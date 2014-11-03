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
package org.yamj.core.service.metadata.nfo;

import org.yamj.core.service.metadata.tools.MetadataTools;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;

public final class InfoDTO {

    private boolean changed = false;
    private boolean tvShow = false;
    private List<String> skipOnlineScans = new ArrayList<String>(0);
    private Map<String, String> ids = new HashMap<String, String>(2);
    private Map<String, Integer> setInfos = new HashMap<String, Integer>(2);
    private Map<String, String> certificationInfos = new HashMap<String, String>(1);
    private Set<CreditDTO> credits = new LinkedHashSet<CreditDTO>(10);
    private Set<String> genres = new LinkedHashSet<String>(5);
    private Set<InfoEpisodeDTO> episodes = new HashSet<InfoEpisodeDTO>();
    private Set<String> posterURLs = new HashSet<String>(0);
    private Set<String> fanartURLs = new HashSet<String>(0);
    private Set<String> trailerURLs= new HashSet<String>(0);
    private boolean watched = false;
    private String title;
    private String titleOriginal;
    private String titleSort;
    private int year = -1;
    private Date releaseDate;
    private int rating = -1;
    private int top250 = -1;
    private String runtime;
    private String plot;
    private String outline;
    private String tagline;
    private String quote;
    private String company;
    private String onlineScanner;

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
        this.changed = true;
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

    public String getId(String sourceDb) {
        return ids.get(sourceDb);
    }
    
    public void setIds(Map<String, String> ids) {
        this.ids = ids;
    }

    public void addId(String sourceDb, String sourceId) {
        if (StringUtils.isNotBlank(sourceDb) && StringUtils.isNotBlank(sourceId)) {
            if ("-1".equals(sourceId)) {
                // skip online scan
                if (!this.skipOnlineScans.contains("all")) {
                    this.skipOnlineScans.add(sourceDb);
                }
            } else {
                this.ids.put(sourceDb, sourceId);
                this.skipOnlineScans.remove(sourceDb);
            }
            this.changed = true;
        }
    }

    public boolean isWatched() {
        return watched;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
        this.changed = true;
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
            this.changed = true;
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
        int testYear = MetadataTools.extractYearAsInt(year);
        if (testYear > 0)  {
            this.year = testYear;
            this.changed = true;
        }
    }
    
    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        if (releaseDate != null) {
            this.releaseDate = releaseDate;
            this.changed = true;
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

    public int getTop250() {
        return top250;
    }

    public void setTop250(int top250) {
        if (top250 >= 0) {
            this.top250 = top250;
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

    public Map<String,String> getCertificationInfos() {
        return certificationInfos;
    }

    public void addCertificatioInfo(String country, String certification) {
        if (StringUtils.isNotBlank(country) && StringUtils.isNotBlank(certification)) {
            this.certificationInfos.put(country.trim(), certification.trim());
            this.changed = true;
        }
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        if (StringUtils.isNotBlank(plot)) {
            this.plot = plot.trim();
            this.changed = true;
        }
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        if (StringUtils.isNotBlank(outline)) {
            this.outline = outline.trim();
            this.changed = true;
        }
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        if (StringUtils.isNotBlank(tagline)) {
            this.tagline = tagline.trim();
            this.changed = true;
        }
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        if (StringUtils.isNotBlank(quote)) {
            this.quote = quote.trim();
            this.changed = true;
        }
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        if (StringUtils.isNotBlank(company)) {
            this.company = company.trim();
            this.changed = true;
        }
    }

    public Set<String> getGenres() {
        return genres;
    }

    public void adGenre(String genre) {
        if (StringUtils.isNotBlank(genre)) {
            this.genres.add(genre.trim());
            this.changed = true;
        }
    }

    public Map<String, Integer> getSetInfos() {
        return setInfos;
    }

    public void addSetInfo(String setName) {
        this.addSetInfo(setName, null);
    }

    public void addSetInfo(String setName, Integer order) {
        if (StringUtils.isNotBlank(setName)) {
            this.setInfos.put(setName, order);
            this.changed = true;
        }
    }

    public Set<String> getTrailerURLs() {
        return trailerURLs;
    }

    public void addTrailerURL(String trailerURL) {
        if (StringUtils.isNotBlank(trailerURL)) {
            this.trailerURLs.add(trailerURL.trim());
            this.changed = true;
        }
    }

    public Set<String> getPosterURLs() {
        return posterURLs;
    }

    public void addPosterURL(String posterURL) {
        if (StringUtils.isNotBlank(posterURL)) {
            this.posterURLs.add(posterURL.trim());
            this.changed = true;
        }
    }

    public Set<String> getFanartURLs() {
        return fanartURLs;
    }

    public void addFanartURL(String fanartURL) {
        if (StringUtils.isNotBlank(fanartURL)) {
            this.fanartURLs.add(fanartURL);
            this.changed = true;
        }
    }

    public Set<CreditDTO> getCredits() {
        return credits;
    }

    public void addDirector(String director) {
        if (StringUtils.isNotBlank(director)) {
            this.credits.add(new CreditDTO(NfoScannerService.SCANNER_ID, JobType.DIRECTOR, director.trim()));
            this.changed = true;
        }
    }

    public void addWriter(String writer) {
        if (StringUtils.isNotBlank(writer)) {
            this.credits.add(new CreditDTO(NfoScannerService.SCANNER_ID, JobType.WRITER, writer.trim()));
            this.changed = true;
        }
    }

    public void addActor(String actor, String role, String photoURL) {
        if (StringUtils.isNotBlank(actor)) {
            CreditDTO credit = new CreditDTO(NfoScannerService.SCANNER_ID, JobType.ACTOR, actor.trim(), role);
            credit.addPhotoURL(photoURL, NfoScannerService.SCANNER_ID);
            this.credits.add(credit);
            this.changed = true;
        }
    }
    
    public void addEpisode(InfoEpisodeDTO episodeDTO) {
        if (episodeDTO != null && episodeDTO.isValid()) {
            this.episodes.add(episodeDTO);
            this.changed = true;
        }
    }
    
    public InfoEpisodeDTO getEpisode(int season, int episode) {
        for (InfoEpisodeDTO dto : this.episodes) {
            if (dto.isSameEpisode(season, episode)) {
                return dto;
            }
        }
        return null;
    }

    public Set<InfoEpisodeDTO> getEpisodes(int season) {
        Set<InfoEpisodeDTO> episodeDTOs = new HashSet<InfoEpisodeDTO>();
        for (InfoEpisodeDTO dto : this.episodes) {
            if (dto.getSeason() == season) {
                episodeDTOs.add(dto);
            }
        }
        return episodeDTOs;
    }
    
    public Date getSeasonYear(int season) {
        Date yearDate = null;
        for (InfoEpisodeDTO episodeDTO : this.getEpisodes(season)) {
            Date parsedDate = episodeDTO.getFirstAired();
            if (parsedDate != null) {
                if (yearDate == null) {
                    yearDate = parsedDate;
                } else if (parsedDate.before(yearDate)) {
                    yearDate = parsedDate;
                }
            }
        }
        return yearDate;
    }

    public String getOnlineScanner() {
        return onlineScanner;
    }

    public void setOnlineScanner(String onlineScanner) {
        this.onlineScanner = onlineScanner;
    }
}
