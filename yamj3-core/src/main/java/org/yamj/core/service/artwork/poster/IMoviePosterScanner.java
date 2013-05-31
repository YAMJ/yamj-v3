package org.yamj.core.service.artwork.poster;

public interface IMoviePosterScanner extends IPosterScanner {

    String getId(String title, int year);
    
    String getPosterUrl(String title, int year);
    
    String getPosterUrl(String id);
}
