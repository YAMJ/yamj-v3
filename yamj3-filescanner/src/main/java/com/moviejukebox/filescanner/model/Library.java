package com.moviejukebox.filescanner.model;

import com.moviejukebox.common.dto.ImportDTO;
import com.moviejukebox.common.dto.StageDirectoryDTO;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Library implements Serializable {

    private boolean watch;
    private LibraryStatistics statistics;
    private Map<String, StageDirectoryDTO> directories;
    private ImportDTO importDTO;

    public Library() {
        this.watch = Boolean.FALSE;
        this.statistics = new LibraryStatistics();
        this.directories = new HashMap<String, StageDirectoryDTO>(1);
        importDTO = new ImportDTO();
    }

    public boolean isWatch() {
        return watch;
    }

    public void setWatch(boolean watch) {
        this.watch = watch;
    }

    public LibraryStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(LibraryStatistics statistics) {
        this.statistics = statistics;
    }

    public Map<String, StageDirectoryDTO> getDirectories() {
        return directories;
    }

    public void setDirectories(Map<String, StageDirectoryDTO> directories) {
        this.directories = directories;
    }

    public void addDirectory(StageDirectoryDTO stageDir) {
        this.directories.put(stageDir.getPath(), stageDir);
    }

    public void removeDirectory(StageDirectoryDTO stageDir) {
        this.directories.remove(stageDir.getPath());
    }

    public void removeDirectory(String stageDirPath) {
        this.directories.remove(stageDirPath);
    }

    public ImportDTO getImportDTO() {
        return importDTO;
    }

    public void setImportDTO(ImportDTO importDTO) {
        this.importDTO = importDTO;
    }

}
