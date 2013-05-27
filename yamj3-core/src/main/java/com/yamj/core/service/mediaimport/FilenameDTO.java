package com.yamj.core.service.mediaimport;

import java.text.DecimalFormat;

import com.yamj.core.database.model.StageDirectory;
import com.yamj.core.database.model.StageFile;
import java.util.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Container of parsed data from movie file name. Contains only information which could be possibly extracted from file name.
 *
 * @author Artem.Gratchev
 */
public class FilenameDTO {

    private static DecimalFormat paddedFormat = new DecimalFormat("000"); // Issue 190
    private final String name;
    private final String parentName;
    private final boolean directory;
    private String rest;
    private String title = null;
    private int year = -1;
    private String partTitle = null;
    private String episodeTitle = null;
    private int season = -1;
    private final List<Integer> episodes = new ArrayList<Integer>();
    private int part = -1;
    private boolean extra = false;
    private String audioCodec = null;
    private String videoCodec = null;
    private String container = null;
    private int fps = -1;
    private String hdResolution = null;
    private String videoSource = null;
    private Map<String, String> idMap = new HashMap<String, String>(2);

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

    public static class SetDTO {

        private String title = null;
        private int index = -1;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }
    private final List<SetDTO> sets = new ArrayList<SetDTO>();
    private final List<String> languages = new ArrayList<String>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getPartTitle() {
        return partTitle;
    }

    public void setPartTitle(String partTitle) {
        this.partTitle = partTitle;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getPart() {
        return part;
    }

    public void setPart(int part) {
        this.part = part;
    }

    public List<Integer> getEpisodes() {
        return episodes;
    }

    public boolean isExtra() {
        return extra;
    }

    public void setExtra(boolean extra) {
        this.extra = extra;
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

    public List<SetDTO> getSets() {
        return sets;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public String getEpisodeTitle() {
        return episodeTitle;
    }

    public void setEpisodeTitle(String episodeTitle) {
        this.episodeTitle = episodeTitle;
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
        sb.append(getTitle());
        sb.append("_");
        sb.append(getYear() > -1 ? getYear() : "0000");
        return sb.toString();
    }

    public String buildEpisodeIdentifier(int episode) {
        StringBuilder sb = new StringBuilder();
        sb.append(getTitle());
        sb.append("_");
        sb.append(getYear() > -1 ? getYear() : "0000");
        sb.append("_");
        sb.append(paddedFormat.format(getSeason()));
        sb.append("_");
        sb.append(paddedFormat.format(episode));
        return sb.toString();
    }

    public String buildSeasonIdentifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTitle());
        sb.append("_");
        sb.append(getYear() > -1 ? getYear() : "0000");
        sb.append("_");
        sb.append(paddedFormat.format(getSeason()));
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Title=").append(title);
        sb.append("],[Year=").append(year);
        sb.append("],[PartTitle=").append(partTitle);
        sb.append("],[Season=").append(season);
        sb.append("],[EpisodeCount=").append(episodes.size());
        sb.append("],[EpisodeTitle=").append(episodeTitle);
        sb.append("],[Part=").append(part);
        sb.append("],[Extra=").append(extra);
        sb.append("],[AudioCodec=").append(audioCodec);
        sb.append("],[VideoCodec=").append(videoCodec);
        sb.append("],[Container=").append(container);
        sb.append("],[FPS=").append(fps);
        sb.append("],[HDResolution=").append(hdResolution);
        sb.append("],[VideoSource=").append(videoSource);
        sb.append("]");
        return sb.toString();
    }
}
