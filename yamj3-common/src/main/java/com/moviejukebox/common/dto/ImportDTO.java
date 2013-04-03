package com.moviejukebox.common.dto;

import java.io.Serializable;

/**
 * Object for importing a directory of a library into the core server.
 */
public class ImportDTO implements Serializable {

    private static final long serialVersionUID = -4541107145393048608L;
    
    private String client;
    private String playerPath;
    private String baseDirectory;
    private StageDirectoryDTO stageDirectory;

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

    public StageDirectoryDTO getStageDirectory() {
		return stageDirectory;
	}

	public void setStageDirectory(StageDirectoryDTO stageDirectory) {
		this.stageDirectory = stageDirectory;
	}

	@Override
    public String toString() {
        return "ImportDTO{playerPath='" + playerPath + "', baseDirectory='" + baseDirectory + "', directory=" + stageDirectory.getPath() + "}";
    }
}
