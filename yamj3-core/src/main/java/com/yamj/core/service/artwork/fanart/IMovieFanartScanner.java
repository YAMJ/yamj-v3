package com.yamj.core.service.artwork.fanart;

public interface IMovieFanartScanner extends IFanartScanner {

    String getId(String title, int year);
    
    String getFanartUrl(String title, int year);
    
    String getFanartUrl(String id);
}
