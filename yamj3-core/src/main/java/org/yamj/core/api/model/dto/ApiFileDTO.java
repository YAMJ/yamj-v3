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
package org.yamj.core.api.model.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Date;

/**
 * Information on the physical file for the API
 *
 * @author Stuart
 */
@JsonInclude(Include.NON_DEFAULT) 
public class ApiFileDTO extends AbstractApiIdentifiableDTO {

    private Boolean extra;
    private Integer part = -1;
    private String partTitle;
    private String version;
    private String container;
    private String codec;
    private String codecFormat;
    private String codecProfile;
    private Integer bitrate = -1;
    private Integer overallBitrate = -1;
    private Float fps = -1F;
    private Integer width = -1;
    private Integer height = -1;
    private String aspectRatio;
    private Integer runtime;
    private String videoSource;
    private String fileName;
    private Date fileDate;
    private Long fileSize = -1L;
    private Long season;
    private Long episode;
    private List<ApiAudioCodecDTO> audioCodes = new ArrayList<ApiAudioCodecDTO>();

    public Boolean getExtra() {
        return extra;
    }

    public void setExtra(Boolean extra) {
        this.extra = extra;
    }

    public Integer getPart() {
        return part;
    }

    public void setPart(Integer part) {
        this.part = part;
    }

    public String getPartTitle() {
        return partTitle;
    }

    public void setPartTitle(String partTitle) {
        this.partTitle = partTitle;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getCodecFormat() {
        return codecFormat;
    }

    public void setCodecFormat(String codecFormat) {
        this.codecFormat = codecFormat;
    }

    public String getCodecProfile() {
        return codecProfile;
    }

    public void setCodecProfile(String codecProfile) {
        this.codecProfile = codecProfile;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    public Integer getOverallBitrate() {
        return overallBitrate;
    }

    public void setOverallBitrate(Integer overallBitrate) {
        this.overallBitrate = overallBitrate;
    }

    public Float getFps() {
        return fps;
    }

    public void setFps(Float fps) {
        this.fps = fps;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }

    public String getVideoSource() {
        return videoSource;
    }

    public void setVideoSource(String videoSource) {
        this.videoSource = videoSource;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Date getFileDate() {
        return fileDate;
    }

    public void setFileDate(Date fileDate) {
        this.fileDate = fileDate;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getSeason() {
        return season;
    }

    public void setSeason(Long season) {
        this.season = season;
    }

    public Long getEpisode() {
        return episode;
    }

    public void setEpisode(Long episode) {
        this.episode = episode;
    }

    public List<ApiAudioCodecDTO> getAudioCodes() {
        return audioCodes;
    }

    public void setAudioCodes(List<ApiAudioCodecDTO> audioCodes) {
        this.audioCodes = audioCodes;
    }
}
