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
    private static final long serialVersionUID = 2L;
    private String fileName;
    private long fileSize;
    private long fileDate;

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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
