package com.moviejukebox.common.dto;

import java.io.Serializable;

/**
 * Object for importing files into the core server.
 */
public class FileImportDTO implements Serializable {

    private static final long serialVersionUID = 2515787873612820679L;

    private String playerPath;
    private String filePath;
    private long lasModificationTimestamp;

    public String getPlayerPath() {
        return playerPath;
    }

    public void setPlayerPath(String playerPath) {
        this.playerPath = playerPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getLasModificationTimestamp() {
        return lasModificationTimestamp;
    }

    public void setLasModificationTimestamp(long lasModificationTimestamp) {
        this.lasModificationTimestamp = lasModificationTimestamp;
    }
}
