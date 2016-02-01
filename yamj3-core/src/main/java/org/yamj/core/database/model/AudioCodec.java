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
package org.yamj.core.database.model;

import java.io.Serializable;
import javax.persistence.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "audio_codec",
        uniqueConstraints = @UniqueConstraint(name = "UIX_AUDIOCODEC_NATURALID", columnNames = {"mediafile_id", "counter"})
)
public class AudioCodec extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -6279878819525772005L;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mediafile_id", nullable = false, foreignKey = @ForeignKey(name = "FK_AUDIOCODEC_MEDIAFILE"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MediaFile mediaFile;

    @NaturalId(mutable = true)
    @Column(name = "counter", nullable = false)
    private int counter = -1;

    @Column(name = "codec", nullable = false, length = 50)
    private String codec;

    @Column(name = "codec_format", nullable = false, length = 50)
    private String codecFormat;

    @Column(name = "bitrate", nullable = false)
    private int bitRate = -1;

    @Column(name = "channels", nullable = false)
    private int channels = -1;

    @Column(name = "language_code", nullable = false, length = 4)
    private String languageCode;

    // GETTER AND SETTER
    
    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getCodecFormat() {
        return codecFormat;
    }

    public void setCodecFormat(String codecFormat) {
        this.codecFormat = codecFormat;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getMediaFile())
                .append(getCounter())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AudioCodec)) {
            return false;
        }
        AudioCodec other = (AudioCodec) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getCounter(), other.getCounter())
                .append(getMediaFile(), other.getMediaFile())
                .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AudioCodec [ID=");
        sb.append(getId());
        if (getMediaFile() != null && Hibernate.isInitialized(getMediaFile())) {
            sb.append(", mediaFile='");
            sb.append(getMediaFile().getFileName());
            sb.append("'");
        }
        sb.append("', counter=");
        sb.append(getCounter());
        sb.append(", codec=");
        sb.append(getCodec());
        sb.append(", format=");
        sb.append(getCodecFormat());
        sb.append(", bitRate=");
        sb.append(getBitRate());
        sb.append(", channels=");
        sb.append(getChannels());
        sb.append(", languageCode=");
        sb.append(getLanguageCode());
        sb.append("]");
        return sb.toString();
    }
}
