package com.moviejukebox.filescanner.model;

import com.moviejukebox.common.dto.ImportDTO;
import com.moviejukebox.common.dto.StageDirectoryDTO;
import com.moviejukebox.common.type.StatusType;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Stuart
 */
public class Library implements Serializable {

    private boolean watch;
    private Statistics statistics;
    private Map<String, StageDirectoryDTO> directories;
    private Map<String, StatusType> directoryStatus;
    private ImportDTO importDTO;

    /**
     * Create a library
     */
    public Library() {
        this.watch = Boolean.FALSE;
        this.statistics = new Statistics();
        this.directories = new HashMap<String, StageDirectoryDTO>(1);
        this.directoryStatus = new HashMap<String, StatusType>(1);
        importDTO = new ImportDTO();
    }

    /**
     * Is the library being watched
     *
     * @return
     */
    public boolean isWatch() {
        return watch;
    }

    /**
     * Set the watch status of the library
     *
     * @param watch
     */
    public void setWatch(boolean watch) {
        this.watch = watch;
    }

    /**
     * Get the library statistics
     *
     * @return
     */
    public Statistics getStatistics() {
        return statistics;
    }

    /**
     * Set the library statistics to the passed value
     *
     * @param statistics
     */
    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    /**
     * Get the collection of the Stage Directories
     *
     * @return
     */
    public Map<String, StageDirectoryDTO> getDirectories() {
        return directories;
    }

    /**
     * Set the collection of Stage Directories to the passed value
     *
     * @param directories
     */
    public void setDirectories(Map<String, StageDirectoryDTO> directories) {
        this.directories = directories;

        directoryStatus.clear();
        for (String path : this.directories.keySet()) {
            addDirectoryStatus(path, StatusType.NEW);
        }
    }

    /**
     * Add a single StageDirectoryDTO
     *
     * @param stageDir
     */
    public void addDirectory(StageDirectoryDTO stageDir) {
        this.directories.put(stageDir.getPath(), stageDir);
        addDirectoryStatus(stageDir.getPath());
    }

    /**
     * Remove a StageDirectoryDTO from the collection
     *
     * @param stageDir
     */
    public void removeDirectory(StageDirectoryDTO stageDir) {
        this.directories.remove(stageDir.getPath());
        removeDirectoryStatus(stageDir.getPath());
    }

    /**
     * Remove a StageDirectoryDTO from the collection
     *
     * @param stageDirPath
     */
    public void removeDirectory(String stageDirPath) {
        this.directories.remove(stageDirPath);
        removeDirectoryStatus(stageDirPath);
    }

    /**
     * Get the stage directory for a path
     *
     * @param stageDirPath
     * @return
     */
    public StageDirectoryDTO getDirectory(String stageDirPath) {
        return directories.get(stageDirPath);
    }

    /**
     * Get the ImportDTO for the library
     *
     * @return
     */
    public ImportDTO getImportDTO() {
        return importDTO;
    }

    /**
     * Set the ImportDTO for the library
     *
     * @param importDTO
     */
    public void setImportDTO(ImportDTO importDTO) {
        this.importDTO = importDTO;
    }

    /**
     * Creates an ImportDTO for the given StageDirectoryDTO
     *
     * @param stageDir
     * @return
     */
    public ImportDTO getImportDTO(StageDirectoryDTO stageDir) {
        ImportDTO newImportDto = new ImportDTO();
        newImportDto.setBaseDirectory(importDTO.getBaseDirectory());
        newImportDto.setClient(importDTO.getClient());
        newImportDto.setPlayerPath(importDTO.getPlayerPath());
        newImportDto.setStageDirectory(stageDir);
        return newImportDto;
    }

    /**
     * Get the collection of statuses
     *
     * @return
     */
    public Map<String, StatusType> getDirectoryStatus() {
        return directoryStatus;
    }

    /**
     * Set the whole collection in one go
     *
     * @param directoryStatus
     */
    public void setDirectoryStatus(Map<String, StatusType> directoryStatus) {
        this.directoryStatus = directoryStatus;
    }

    /**
     * Add a status for the path
     *
     * @param path
     * @param status
     */
    public void addDirectoryStatus(String path, StatusType status) {
        this.directoryStatus.put(path, status);
    }

    /**
     * Add a status for the path.
     *
     * If the path already exists, this will set the status to UPDATED, otherwise it will be NEW
     *
     * @param path
     */
    public void addDirectoryStatus(String path) {
        if (directoryStatus.containsKey(path)) {
            // Set to updated
            addDirectoryStatus(path, StatusType.UPDATED);
        } else {
            // Set to updated
            addDirectoryStatus(path, StatusType.NEW);
        }
    }

    /**
     * Get the status for a single path
     *
     * @param path
     * @return
     */
    public StatusType findDirectoryStatus(String path) {
        if (directoryStatus.containsKey(path)) {
            return directoryStatus.get(path);
        } else {
            // Don't know if this is the right status to send here
            return StatusType.NEW;
        }
    }

    /**
     * Remove a status for the given path
     *
     * @param path
     */
    public void removeDirectoryStatus(String path) {
        this.directoryStatus.remove(path);
    }

    /**
     * Calculate the relative directory from the library base directory
     *
     * @param directory
     * @return
     */
    public String getRelativeDir(File directory) {
        return getRelativeDir(directory.getAbsolutePath());
    }

    /**
     * Calculate the relative directory from the library base directory
     *
     * @param directory
     * @return
     */
    public String getRelativeDir(String absolutePath) {
        if (absolutePath.startsWith(importDTO.getBaseDirectory())) {
            if (absolutePath.length() > importDTO.getBaseDirectory().length()) {
                return absolutePath.substring(importDTO.getBaseDirectory().length() + 1);
            } else {
                return FilenameUtils.getBaseName(absolutePath);
            }
        } else {
            return absolutePath;
        }

    }
}
