package org.yamj.core.database.model;

import java.io.Serializable;
import javax.persistence.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "audio_codec",
    uniqueConstraints= @UniqueConstraint(name="UIX_AUDIOCODEC_NATURALID", columnNames={"mediafile_id", "counter"})
)
public class AudioCodec extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -6279878819525772005L;
    
    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mediafile_id", nullable = false)
    @ForeignKey(name = "FK_AUDIOCODEC_MEDIAFILE")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MediaFile mediaFile;
    
    @NaturalId(mutable = true)
    @Column(name = "counter", nullable = false)
    private int counter = -1;

    @Column(name = "codec", nullable = false)
    private String codec;

    @Column(name = "codec_format", nullable = false)
    private String codecFormat;

    @Column(name = "bitrate", nullable = false)
    private int bitRate = -1;

    @Column(name = "channels", nullable = false)
    private int channels = -1;
    
    @Column(name = "language")
    private String language;

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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
    
    
    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (mediaFile == null ? 0 : mediaFile.hashCode());
        result = prime * result + counter;
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
        if (!(other instanceof AudioCodec)) {
            return false;
        }
        AudioCodec castOther = (AudioCodec) other;
        // first check the id
        if ((this.getId() > 0) && (castOther.getId() > 0)) {
            return this.getId() == castOther.getId();
        }
        // check counter
        if (this.counter != castOther.counter) {
            return false;
        }
        // check media file
        if (this.mediaFile != null && castOther.mediaFile != null) {
            return this.mediaFile.equals(castOther.mediaFile);
        }
        // case if one media file is null
        return true;
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
        sb.append(", language=");
        sb.append(getLanguage());
        sb.append("]");
        return sb.toString();
    }
}
