package org.yamj.core.service.artwork.fanart;

import org.yamj.core.database.model.IMetadata;

public interface IFanartScanner {

    String getScannerName();
   
    String getFanartUrl(IMetadata metadata);
}
