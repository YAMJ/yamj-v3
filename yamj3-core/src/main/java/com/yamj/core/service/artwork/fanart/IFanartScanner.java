package com.yamj.core.service.artwork.fanart;

import com.yamj.core.database.model.IMetadata;

public interface IFanartScanner {

    String getScannerName();
   
    String getFanartUrl(IMetadata metadata);
}
