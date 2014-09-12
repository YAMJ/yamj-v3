/*
 *      Copyright (c) 2004-2014 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.tools.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.http.CommonHttpClient;
import org.yamj.common.tools.PropertyTools;

public class SearchEngineTools {

    private static final Logger LOG = LoggerFactory.getLogger(SearchEngineTools.class);
    // Literals
    private static final String HTTP = "http://";
    private static final String UTF8 = "UTF-8";
    private static final String SITE = "+site%3A";
    private static final String PAREN_RIGHT = "%29";
    private static final String PAREN_LEFT = "+%28";
    
    private final CommonHttpClient httpClient;
    private final Charset charset;
    
    private LinkedList<String> searchSites;
    private String country;
    private String searchSuffix = "";
    private String language;
    private String googleHost = "www.google.com";
    private String yahooHost = "search.yahoo.com";
    private String bingHost = "www.bing.com";
    private String blekkoHost = "www.blekko.com";
    private String lycosHost = "search.lycos.com";

    public SearchEngineTools(CommonHttpClient httpClient) {
        this(httpClient, "us");
    }

    public SearchEngineTools(CommonHttpClient httpClient, String country) {
        this.httpClient = httpClient;
        this.charset = Charset.forName("UTF-8");
        
        // sites to search for URLs
        searchSites = new LinkedList<String>();
        searchSites.addAll(Arrays.asList(PropertyTools.getProperty("yamj3.searchengine.sites", "google,yahoo,bing,blekko,lycos").split(",")));

        // country specific presets
        if ("de".equalsIgnoreCase(country)) {
            this.country = "de";
            language = "de";
            googleHost = "www.google.de";
            yahooHost = "de.search.yahoo.com";
            lycosHost = "search.lycos.de";
        } else if ("it".equalsIgnoreCase(country)) {
            this.country = "it";
            language = "it";
            googleHost = "www.google.it";
            yahooHost = "it.search.yahoo.com";
            bingHost = "it.bing.com";
            lycosHost = "search.lycos.it";
        } else if ("se".equalsIgnoreCase(country)) {
            this.country = "se";
            language = "sv";
            googleHost = "www.google.se";
            yahooHost = "se.search.yahoo.com";
            lycosHost = "search.lycos.se";
        } else if ("pl".equalsIgnoreCase(country)) {
            this.country = "pl";
            language = "pl";
            googleHost = "www.google.pl";
            yahooHost = "pl.search.yahoo.com";
        } else if ("ru".equalsIgnoreCase(country)) {
            this.country = "ru";
            language = "ru";
            googleHost = "www.google.ru";
            yahooHost = "ru.search.yahoo.com";
        } else if ("il".equalsIgnoreCase(country)) {
            this.country = "il";
            language = "il";
            googleHost = "www.google.co.il";
        } else if ("fr".equalsIgnoreCase(country)) {
            this.country = "fr";
            language = "fr";
            googleHost = "www.google.fr";
        } else if ("nl".equalsIgnoreCase(country)) {
            this.country = "nl";
            language = "nl";
            googleHost = "www.google.nl";
            lycosHost = "search.lycos.nl";
        }
    }

    public void setSearchSites(String searchSites) {
        this.searchSites.clear();
        this.searchSites.addAll(Arrays.asList(searchSites.split(",")));
    }

    public void setSearchSuffix(String searchSuffix) {
        this.searchSuffix = searchSuffix;
    }

    public String searchURL(String title, int year, String site) {
        return searchURL(title, year, site, null);
    }

    public String searchURL(String title, int year, String site, String additional) {
        String url;

        String engine = getNextSearchEngine();
        if ("yahoo".equalsIgnoreCase(engine)) {
            url = searchUrlOnYahoo(title, year, site, additional);
        } else if ("bing".equalsIgnoreCase(engine)) {
            url = searchUrlOnBing(title, year, site, additional);
        } else if ("blekko".equalsIgnoreCase(engine)) {
            url = searchUrlOnBlekko(title, year, site, additional);
        } else if ("lycos".equalsIgnoreCase(engine)) {
            url = searchUrlOnLycos(title, year, site, additional);
        } else {
            url = searchUrlOnGoogle(title, year, site, additional);
        }

        return url;
    }

    private String getNextSearchEngine() {
        String engine = searchSites.remove();
        searchSites.addLast(engine);
        return engine;
    }

    public int countSearchSites() {
        return searchSites.size();
    }

    public String searchUrlOnGoogle(String title, int year, String site, String additional) {
        LOG.debug("Searching '{}' on google; site={}", title, site);

        try {
            StringBuilder sb = new StringBuilder(HTTP);
            sb.append(googleHost);
            sb.append("/search?");
            if (language != null) {
                sb.append("hl=");
                sb.append(language);
                sb.append("&");
            }
            sb.append("q=");
            sb.append(URLEncoder.encode(title, UTF8));
            if (year > 0) {
                sb.append(PAREN_LEFT);
                sb.append(year);
                sb.append(PAREN_RIGHT);
            }
            sb.append(SITE);
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, UTF8));
            }
            String xml = httpClient.requestContent(sb.toString(), charset);

            int beginIndex = xml.indexOf(HTTP + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (IOException error) {
            LOG.error("Failed retrieving link url by google search {}", title, error);
        }
        return null;
    }

    public String searchUrlOnYahoo(String title, int year, String site, String additional) {
        LOG.debug("Searching '{}' on yahoo; site={}", title, site);

        try {
            StringBuilder sb = new StringBuilder(HTTP);
            sb.append(yahooHost);
            sb.append("/search?vc=");
            if (country != null) {
                sb.append(country);
                sb.append("&rd=r2");
            }
            sb.append("&ei=UTF-8&p=");
            sb.append(URLEncoder.encode(title, UTF8));
            if (year > 0) {
                sb.append(PAREN_LEFT);
                sb.append(year);
                sb.append(PAREN_RIGHT);
            }
            sb.append(SITE);
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, UTF8));
            }

            String xml = httpClient.requestContent(sb.toString(), charset);

            int beginIndex = xml.indexOf("//" + site + searchSuffix);
            if (beginIndex != -1) {
                String link = xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
                if (StringUtils.isNotBlank(link)) {
                    return "http:" + link;
                }
            }
        } catch (IOException error) {
            LOG.error("Failed retrieving link url by yahoo search '{}'", title, error);
        }
        return null;
    }

    public String searchUrlOnBing(String title, int year, String site, String additional) {
        LOG.debug("Searching '{}' on bing; site={}", title, site);

        try {
            StringBuilder sb = new StringBuilder(HTTP);
            sb.append(bingHost);
            sb.append("/search?q=");
            sb.append(URLEncoder.encode(title, UTF8));
            if (year > 0) {
                sb.append(PAREN_LEFT);
                sb.append(year);
                sb.append(PAREN_RIGHT);
            }
            sb.append(SITE);
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, UTF8));
            }
            if (country != null) {
                sb.append("&cc=");
                sb.append(country);
                sb.append("&filt=rf");
            }

            String xml = httpClient.requestContent(sb.toString(), charset);

            int beginIndex = xml.indexOf(HTTP + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (IOException error) {
            LOG.error("Failed retrieving link url by bing search {}", title, error);
        }
        return null;
    }

    public String searchUrlOnBlekko(String title, int year, String site, String additional) {
        LOG.debug("Searching '{}' on blekko; site={}", title, site);

        try {
            StringBuilder sb = new StringBuilder(HTTP);
            sb.append(blekkoHost);
            sb.append("/ws/?q=");
            sb.append(URLEncoder.encode(title, UTF8));
            if (year > 0) {
                sb.append(PAREN_LEFT);
                sb.append(year);
                sb.append(PAREN_RIGHT);
            }
            sb.append(SITE);
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, UTF8));
            }

            String xml = httpClient.requestContent(sb.toString(), charset);

            int beginIndex = xml.indexOf(HTTP + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (IOException error) {
            LOG.error("Failed retrieving link url by bing search {}", title, error);
        }
        return null;
    }

    public String searchUrlOnLycos(String title, int year, String site, String additional) {
        LOG.debug("Searching '{}' on lycos; site={}", title, site);

        try {
            StringBuilder sb = new StringBuilder(HTTP);
            sb.append(lycosHost);
            if ("it".equalsIgnoreCase(country)) {
                sb.append("/?tab=web&Search=Cerca&searchArea=loc&query=");
            } else if ("se".equalsIgnoreCase(country)) {
                sb.append("/?tab=web&Search=S%C3%B6ka&searchArea=loc&query=");
            } else if ("nl".equalsIgnoreCase(country)) {
                sb.append("/?tab=web&Search=Zoeken&searchArea=loc&query=");
            } else {
                sb.append("/web/?q=");
            }
            sb.append(URLEncoder.encode(title, UTF8));
            if (year > 0) {
                sb.append(PAREN_LEFT);
                sb.append(year);
                sb.append(PAREN_RIGHT);
            }
            sb.append(SITE);
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, UTF8));
            }

            String xml = httpClient.requestContent(sb.toString(), charset);

            int beginIndex = xml.indexOf(HTTP + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (IOException error) {
            LOG.error("Failed retrieving link url by lycos search {}", title, error);
        }
        return null;
    }
}
