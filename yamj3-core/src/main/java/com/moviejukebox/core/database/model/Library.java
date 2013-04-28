package com.moviejukebox.core.database.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "library")
public class Library extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -3086992329257871600L;
    @NaturalId
    @Column(name = "client", nullable = false, length = 100)
    private String client;
    @NaturalId
    @Column(name = "player_path", nullable = false, length = 500)
    private String playerPath;
    @Column(name = "base_directory", nullable = false, length = 500)
    private String baseDirectory;
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "last_scanned", nullable = false)
    private Date lastScanned;

    // GETTER and SETTER
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

    public Date getLastScanned() {
        return lastScanned;
    }

    public void setLastScanned(Date lastScanned) {
        this.lastScanned = lastScanned;
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        final int PRIME = 17;
        int result = 1;
        result = PRIME * result + (this.client == null ? 0 : this.client.hashCode());
        result = PRIME * result + (this.playerPath == null ? 0 : this.playerPath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof Library)) {
            return false;
        }
        Library castOther = (Library) other;

        if (!StringUtils.equals(this.client, castOther.client)) {
            return false;
        }
        return StringUtils.equals(this.playerPath, castOther.playerPath);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Library [ID=");
        sb.append(getId());
        sb.append(", client=");
        sb.append(getClient());
        sb.append(", playerPath=");
        sb.append(getPlayerPath());
        sb.append(", baseDirectory=");
        sb.append(getBaseDirectory());
        sb.append(", lastScanned=");
        sb.append(getLastScanned());
        sb.append("]");
        return sb.toString();
    }
}
