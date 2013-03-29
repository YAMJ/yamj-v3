package com.moviejukebox.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Object for importing library into the core server.
 */
public class LibraryDTO implements Serializable {

    private static final long serialVersionUID = -4541107145393048608L;
    
    private String client;
    private String playerPath;
    private String baseDirectory;
    private List<StageDirectoryDTO> stageDirectories = new ArrayList<StageDirectoryDTO>(0);

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getPlayerPath() {
        return playerPath;
    }

    public void setPlayerPath(String playerPath) {
        this.playerPath = playerPath;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public List<StageDirectoryDTO> getStageDirectories() {
        return stageDirectories;
    }

    public void setStageDirectories(List<StageDirectoryDTO> stageDirectories) {
        this.stageDirectories = stageDirectories;
    }

    public void addStageDirectory(StageDirectoryDTO stageDirectory) {
        this.stageDirectories.add(stageDirectory);
    }

    @Override
    public String toString() {
        return "LibraryDTO{playerPath='" + playerPath + "', baseDirectory='" + baseDirectory + "', directories=" + stageDirectories.size() + "}";
    }
}
