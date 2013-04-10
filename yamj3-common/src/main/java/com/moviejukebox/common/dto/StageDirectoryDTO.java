package com.moviejukebox.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Object for importing stage directories into the core server.
 */
public class StageDirectoryDTO implements Serializable {

    private static final long serialVersionUID = 4310308645268137615L;
    private String path;
    private long date;
    private List<StageFileDTO> stageFiles = new ArrayList<StageFileDTO>(0);

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public List<StageFileDTO> getStageFiles() {
        return stageFiles;
    }

    public void setStageFiles(List<StageFileDTO> stageFiles) {
        this.stageFiles = stageFiles;
    }

    public void addStageFile(StageFileDTO stageFile) {
        this.stageFiles.add(stageFile);
    }

    @Override
    public String toString() {
        return "StageDirectoryDTO{" + "path=" + path + ", date=" + date + ", stageFiles=" + stageFiles.size() + '}';
    }
}
