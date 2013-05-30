package com.yamj.core.service.artwork.poster;

import com.yamj.core.database.model.IMetadata;

public interface IPosterScanner {

    String getScannerName();
   
    String getPosterUrl(IMetadata metadata);
}
