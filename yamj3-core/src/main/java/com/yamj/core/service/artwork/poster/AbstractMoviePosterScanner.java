package com.yamj.core.service.artwork.poster;

import com.yamj.core.database.model.IMetadata;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMoviePosterScanner implements IMoviePosterScanner {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMoviePosterScanner.class);

    @Override
    public String getPosterUrl(IMetadata metadata) {
        String id = metadata.getSourcedbId(getScannerName());
        if (StringUtils.isBlank(id)) {
            if (StringUtils.isBlank(metadata.getTitleOriginal())) {
                id = getId(metadata.getTitle(), metadata.getYear());
            } else {
                id = getId(metadata.getTitleOriginal(), metadata.getYear());
                if (StringUtils.isBlank(id) && !StringUtils.equals(metadata.getTitleOriginal(), metadata.getTitle())) {
                    // didn't find the movie with the original title, try the normal title if it's different
                    id = this.getId(metadata.getTitle(), metadata.getYear());
                }
            }
            if (StringUtils.isNotBlank(id)) {
                LOG.debug("{} : ID found setting it to '{}'", getScannerName(), id);
                metadata.setSourcedbId(getScannerName(), id);
            }
        }

        if (!(StringUtils.isBlank(id) || "-1".equals(id) || "0".equals(id))) {
            return getPosterUrl(id);
        }

        return null;
    }
}
