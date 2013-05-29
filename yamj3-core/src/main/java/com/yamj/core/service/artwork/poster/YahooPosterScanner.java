package com.yamj.core.service.artwork.poster;

import com.yamj.common.tools.web.PoolingHttpClient;
import com.yamj.core.service.artwork.ArtworkScannerService;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("yahooPosterScanner")
public class YahooPosterScanner extends AbstractMoviePosterScanner
    implements InitializingBean
{
    private static final Logger LOG = LoggerFactory.getLogger(YahooPosterScanner.class);
    
    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private PoolingHttpClient httpClient;
    
    @Override
    public String getScannerName() {
        return "yahoo";
    }

    @Override
    public void afterPropertiesSet() {
        artworkScannerService.registerMoviePosterScanner(this);
    }

    @Override
    public String getId(String title, int year) {
        // No id from yahoo search, return title
        return title;
    }

    @Override
    public String getPosterUrl(String title, int year) {
        String posterUrl = null;
        try {
            StringBuilder sb = new StringBuilder("http://fr.images.search.yahoo.com/search/images?p=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append("+poster&fr=&ei=utf-8&js=1&x=wrt");

            String xml = httpClient.requestContent(sb.toString());
            int beginIndex = xml.indexOf("imgurl=");
            if (beginIndex > 0) {
                int endIndex = xml.indexOf("rurl=", beginIndex);
                if (endIndex > 0) {
                    posterUrl = URLDecoder.decode(xml.substring(beginIndex + 7, endIndex-1), "UTF-8");
                }
            }
        } catch (Exception error) {
            LOG.error("Failed retreiving poster URL from yahoo images : " + title, error);
        }

        return posterUrl;
    }

    @Override
    public String getPosterUrl(String id) {
        return getPosterUrl(id, -1);
    }
}
