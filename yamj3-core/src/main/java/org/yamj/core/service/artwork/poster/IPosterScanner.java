package org.yamj.core.service.artwork.poster;

import org.yamj.core.database.model.IMetadata;

public interface IPosterScanner {

    String getScannerName();
   
    String getPosterUrl(IMetadata metadata);
}
