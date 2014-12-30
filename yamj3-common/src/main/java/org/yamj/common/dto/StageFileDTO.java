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
package org.yamj.common.dto;

import java.io.File;
import java.io.Serializable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object for importing stage files into the core server.
 *
 * Final class, cannot be extended
 */
public final class StageFileDTO implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(StageFileDTO.class);
    private static final long serialVersionUID = 1088965998133678951L;

    private String fileName;
    private long fileSize;
    private long fileDate;
    private String content;
    
    public StageFileDTO() {
    }

    public StageFileDTO(File stageFile) {
        if (stageFile.isFile()) {
            this.fileName = stageFile.getName();
            this.fileSize = stageFile.length();
            setFileDate(stageFile.lastModified());
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getFileDate() {
        return fileDate;
    }

    public void setFileDate(long fileDate) {
        DateTime dt = new DateTime(fileDate);
        if (dt.isBeforeNow()) {
            this.fileDate = fileDate;
        } else {
            LOG.warn("File '{}' has a date greater than now, using current date", fileName);
            this.fileDate = DateTime.now().getMillis();
        }
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
