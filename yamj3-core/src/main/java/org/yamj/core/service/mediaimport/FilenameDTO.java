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
package org.yamj.core.service.mediaimport;

import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.yamj.core.database.model.StageDirectory;
import org.yamj.core.database.model.StageFile;

/**
 * Container of parsed data from movie file name. Contains only information which could be possibly extracted from file name.
 *
 * @author Artem.Gratchev
 */
public class FilenameDTO {

    private static DecimalFormat PADDED_FORMAT = new DecimalFormat("000"); // Issue 190
    private final String name;
    private final String parentName;
    private final boolean directory;
    private String rest;
    private String title = null;
    private String cleanTitle = null;
    private int year = -1;
    private boolean extra = false;
    private int part = -1;
    private String partTitle = null;
    private String movieVersion = null;
    private String episodeTitle = null;
    private int season = -1;
    private final List<Integer> episodes = new ArrayList<>();
    private String audioCodec = null;
    private String videoCodec = null;
    private String container = null;
    private int fps = -1;
    private String hdResolution = null;
    private String videoSource = null;
    private final Map<String, String> idMap = new HashMap<>(2);
    private final Map<String, Integer> setMap = new HashMap<>(0);
    private final List<String> languages = new ArrayList<>(0);

    public FilenameDTO(StageFile stageFile) {
        this.name = stageFile.getFileName();
        this.parentName = FilenameUtils.getName(stageFile.getStageDirectory().getDirectoryPath());
        this.directory = false;
    }

    public FilenameDTO(StageDirectory stageDirectory) {
        this.name = FilenameUtils.getName(stageDirectory.getDirectoryPath());
        this.parentName = FilenameUtils.getName(stageDirectory.getParentDirectory().getDirectoryPath());
        this.directory = true;
    }

    public String getName() {
        return name;
    }

    public String getParentName() {
        return parentName;
    }

    public boolean isDirectory() {
        return directory;
    }

    public String getRest() {
        return rest;
    }

    public void setRest(String rest) {
        this.rest = rest;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = StringUtils.trim(title);
    }

    public String getCleanTitle() {
        return cleanTitle;
    }

    public void setCleanTitle(String cleanTitle) {
        this.cleanTitle = cleanTitle;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public boolean isExtra() {
        return extra;
    }

    public void setExtra(boolean extra) {
        this.extra = extra;
    }

    public int getPart() {
        return part;
    }

    public void setPart(int part) {
        this.part = part;
    }

    public String getPartTitle() {
        return partTitle;
    }

    public void setPartTitle(String partTitle) {
        this.partTitle = StringUtils.trim(partTitle);
    }

    public String getMovieVersion() {
        return movieVersion;
    }

    public void setMovieVersion(String movieVersion) {
        this.movieVersion = movieVersion;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public List<Integer> getEpisodes() {
        return episodes;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public String getHdResolution() {
        return hdResolution;
    }

    public void setHdResolution(String hdResolution) {
        this.hdResolution = hdResolution;
    }

    public String getVideoSource() {
        return videoSource;
    }

    public void setVideoSource(String videoSource) {
        this.videoSource = videoSource;
    }

    public Map<String,Integer> getSetMap() {
        return setMap;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public String getEpisodeTitle() {
        return episodeTitle;
    }

    public void setEpisodeTitle(String episodeTitle) {
        this.episodeTitle = StringUtils.trim(episodeTitle);
    }

    public void setId(String key, String id) {
        if (key != null && id != null && !StringUtils.equalsIgnoreCase(id, this.getId(key))) {
            this.idMap.put(key, id);
        }
    }

    public String getId(String key) {
        return idMap.get(key);
    }

    public Map<String, String> getIdMap() {
        return idMap;
    }

    public boolean isMovie() {
        return getEpisodes().isEmpty();
    }

    public String buildIdentifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCleanTitle());
        sb.append("_");
        sb.append(getYear() > -1 ? getYear() : "0000");
        return sb.toString();
    }

    public String buildEpisodeIdentifier(int episode) {
        StringBuilder sb = new StringBuilder();
        sb.append(getCleanTitle());
        sb.append("_");
        sb.append(getYear() > -1 ? getYear() : "0000");
        sb.append("_");
        sb.append(PADDED_FORMAT.format(season));
        sb.append("_");
        sb.append(PADDED_FORMAT.format(episode));
        return sb.toString();
    }

    public String buildSeasonIdentifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCleanTitle());
        sb.append("_");
        sb.append(getYear() > -1 ? getYear() : "0000");
        sb.append("_");
        sb.append(PADDED_FORMAT.format(season));
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Title=").append(title);
        sb.append("],[CleanTitle=").append(cleanTitle);
        sb.append("],[Year=").append(year);
        sb.append("],[Extra=").append(extra);
        sb.append("],[Part=").append(part);
        sb.append("],[PartTitle=").append(partTitle);
        sb.append("],[MovieVersion=").append(movieVersion);
        sb.append("],[Season=").append(season);
        sb.append("],[EpisodeCount=").append(episodes.size());
        sb.append("],[EpisodeTitle=").append(episodeTitle);
        sb.append("],[AudioCodec=").append(audioCodec);
        sb.append("],[VideoCodec=").append(videoCodec);
        sb.append("],[Container=").append(container);
        sb.append("],[FPS=").append(fps);
        sb.append("],[HDResolution=").append(hdResolution);
        sb.append("],[VideoSource=").append(videoSource);
        for (Entry<String,Integer> entry : this.setMap.entrySet()) {
            sb.append("],[Set=").append(entry.getKey()+","+entry.getValue());
        }
        sb.append("]");
        return sb.toString();
    }
}
