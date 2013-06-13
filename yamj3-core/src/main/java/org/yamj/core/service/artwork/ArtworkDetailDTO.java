package org.yamj.core.service.artwork;

public class ArtworkDetailDTO {

    private final String source;
    private final String url;
    private String language = null;
    private int rating = -1;
    
    public ArtworkDetailDTO(String source, String url) {
        this.source = source;
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public String getUrl() {
        return url;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ArtworkDetailDTO [Source=");
        sb.append(getSource());
        sb.append(", url=");
        sb.append(getUrl());
        sb.append(", language=");
        sb.append(getLanguage());
        sb.append(", rating=");
        sb.append(getRating());
        sb.append("]");
        return sb.toString();
    }
}
