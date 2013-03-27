package com.moviejukebox.core.database.model;

import com.moviejukebox.core.database.model.type.FileStageType;
import com.moviejukebox.core.hibernate.usertypes.EnumStringUserType;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@TypeDef(name = "fileStageType", 
    typeClass = EnumStringUserType.class,
    parameters = {@Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.FileStageType")})

@Entity
@Table(name = "file_stage")
public class FileStage extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -6247352843375054146L;

    @NaturalId
    @Column(name = "scan_path", nullable = false, length = 255)
    private String scanPath;
    
    @NaturalId
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "file_date", nullable = false)
    private Date fileDate;
    
    @Column(name = "file_size", nullable = false)
    private long fileSize = -1;
    
    @Type(type = "fileStageType")
    @Column(name = "stage_type", nullable = false, length = 30)
    private FileStageType fileStageType;

    // GETTER and SETTER

    public String getScanPath() {
        return scanPath;
    }

    public void setScanPath(String scanPath) {
        this.scanPath = scanPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Date getFileDate() {
        return fileDate;
    }

    public void setFileDate(Date fileDate) {
        this.fileDate = fileDate;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public FileStageType getFileStageType() {
        return fileStageType;
    }

    public void setFileStageType(FileStageType fileStageType) {
        this.fileStageType = fileStageType;
    }

    
    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int PRIME = 17;
        int result = 1;
        result = PRIME * result + (this.scanPath == null?0:this.scanPath.hashCode());
        result = PRIME * result + (this.filePath == null?0:this.filePath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if ( this == other ) return true;
        if ( other == null ) return false;
        if ( !(other instanceof FileStage) ) return false;
        FileStage castOther = (FileStage)other;
        if (!StringUtils.equals(this.scanPath, castOther.scanPath)) return false;
        if (!StringUtils.equals(this.filePath, castOther.filePath)) return false;
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ID=");
        sb.append(getId());
        sb.append(", scanPath=");
        sb.append(getScanPath());
        sb.append(", filePath=");
        sb.append(getFilePath());
        sb.append(", fileDate=");
        sb.append(getFileDate());
        sb.append(", fileSize=");
        sb.append(getFileSize());
        sb.append("]");
        return sb.toString();
    }
}
