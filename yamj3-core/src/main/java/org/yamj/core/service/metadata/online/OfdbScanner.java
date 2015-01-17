/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package org.yamj.core.service.metadata.online;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.core.configuration.ConfigServiceWrapper;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.database.model.type.OverrideFlag;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.service.metadata.online.imdb.ImdbScanner;
import org.yamj.core.service.metadata.online.imdb.ImdbSearchEngine;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.tools.web.HTMLTools;
import org.yamj.core.tools.web.PoolingHttpClient;
import org.yamj.core.tools.web.ResponseTools;
import org.yamj.core.tools.web.SearchEngineTools;

@Service("ofdbScanner")
public class OfdbScanner implements IMovieScanner {

    private static final String SCANNER_ID = "ofdb";
    private static final Logger LOG = LoggerFactory.getLogger(OfdbScanner.class);
    private static final String HTML_FONT = "</font>";
    private static final String HTML_TABLE_END = "</table>";
    private static final String HTML_TR_START = "<tr";
    private static final String HTML_TR_END = "</tr>";

    private Charset charset;
    private SearchEngineTools searchEngineTools;

    @Autowired
    private PoolingHttpClient httpClient;
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ImdbSearchEngine imdbSearchEngine;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;

    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() throws Exception {
        LOG.info("Initialize OFDb scanner");

        charset = Charset.forName("UTF-8");

        searchEngineTools = new SearchEngineTools(httpClient, "de");
        searchEngineTools.setSearchSites("google");

        // register this scanner
        onlineScannerService.registerMovieScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        String ofdbId = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(ofdbId)) {

            // get and check IMDb id
            String imdbId = videoData.getSourceDbId(ImdbScanner.SCANNER_ID);
            if (StringUtils.isBlank(imdbId)) {
                boolean searchImdb = configServiceWrapper.getBooleanProperty("ofdb.search.imdb", false);
                if (searchImdb) {
                    // search IMDb id if not present
                    imdbId = this.imdbSearchEngine.getImdbId(videoData.getTitle(), videoData.getPublicationYear(), false);
                    if (StringUtils.isNotBlank(imdbId)) {
                        LOG.debug("Found IMDb id {} for movie '{}'", imdbId, videoData.getTitle());
                        videoData.setSourceDbId(ImdbScanner.SCANNER_ID, imdbId);
                    }
                }
            }

            // find by IMDb id
            if (StringUtils.isNotBlank(imdbId)) {
                // if IMDb id is present then use this
                ofdbId = getOfdbIdByImdbId(imdbId);
            }
            if (StringUtils.isBlank(ofdbId)) {
                // try by title and year
                ofdbId = getMovieId(videoData.getTitle(), videoData.getPublicationYear());
            }
            videoData.setSourceDbId(SCANNER_ID, ofdbId);
        }
        return ofdbId;
    }

    @Override
    public String getMovieId(String title, int year) {
        // try with OFDb search
        String ofdbId = getOfdbIdByTitleAndYear(title, year);
        if (StringUtils.isBlank(ofdbId)) {
            // try with search engines
            ofdbId = searchEngineTools.searchURL(title, year, "www.ofdb.de/film");
        }
        return ofdbId;
    }

    private String getOfdbIdByImdbId(String imdbId) {
        try {
            DigestedResponse response = httpClient.requestContent("http://www.ofdb.de/view.php?page=suchergebnis&SText=" + imdbId + "&Kat=IMDb", charset);
            if (ResponseTools.isNotOK(response)) {
                LOG.error("Can't find movie id for imdb id due response status {}: {}", response.getStatusCode(), imdbId);
                return null;
            }

            final String xml = response.getContent();
            
            int beginIndex = xml.indexOf("Ergebnis der Suchanfrage");
            if (beginIndex < 0) {
                return null;
            }

            beginIndex = xml.indexOf("film/", beginIndex);
            if (beginIndex != -1) {
                StringBuilder sb = new StringBuilder();
                sb.append("http://www.ofdb.de/");
                sb.append(xml.substring(beginIndex, xml.indexOf('\"', beginIndex)));
                return sb.toString();
            }

        } catch (IOException ex) {
            LOG.error("Failed retreiving OFDb url for IMDb id '{}': {}", imdbId, ex.getMessage());
            LOG.trace("Scanner error", ex);
        }
        return null;
    }

    private String getOfdbIdByTitleAndYear(String title, int year) {
        if (year <= 0) {
            // title and year must be present for successful OFDb advanced search
            // expected are 2 search parameters minimum; so skip here if year is not valid
            return null;
        }

        try {
            StringBuilder sb = new StringBuilder("http://www.ofdb.de/view.php?page=fsuche&Typ=N&AB=-&Titel=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append("&Genre=-&HLand=-&Jahr=");
            sb.append(year);
            sb.append("&Wo=-&Land=-&Freigabe=-&Cut=A&Indiziert=A&Submit2=Suche+ausf%C3%BChren");

            
            DigestedResponse response = httpClient.requestContent(sb.toString(), charset);
            if (ResponseTools.isNotOK(response)) {
                LOG.error("Can't find movie id by title and year due response status {}: '{}'-{}", response.getStatusCode(), title, year);
                return null;
            }

            final String xml = response.getContent();
            
            int beginIndex = xml.indexOf("Liste der gefundenen Fassungen");
            if (beginIndex < 0) {
                return null;
            }

            beginIndex = xml.indexOf("href=\"film/", beginIndex);
            if (beginIndex < 0) {
                return null;
            }

            sb.setLength(0);
            sb.append("http://www.ofdb.de/");
            sb.append(xml.substring(beginIndex + 6, xml.indexOf("\"", beginIndex + 10)));
            return sb.toString();

        } catch (IOException ex) {
            LOG.error("Failed retrieving OFDb url for title '{}': {}", title, ex.getMessage());
            LOG.trace("Scanner error", ex);
        }
        return null;
    }

    @Override
    public ScanResult scan(VideoData videoData) {
        String ofdbUrl = getMovieId(videoData);

        if (StringUtils.isBlank(ofdbUrl)) {
            LOG.debug("OFDb url not available: {}", videoData.getTitle());
            return ScanResult.MISSING_ID;
        }

        LOG.debug("OFDb url available ({}), updating video data", ofdbUrl);
        return updateVideoData(videoData, ofdbUrl);
    }

    private ScanResult updateVideoData(VideoData videoData, String ofdbUrl) {
        ScanResult scanResult = ScanResult.OK;

        try {
            DigestedResponse response = httpClient.requestContent(ofdbUrl, charset);
            if (ResponseTools.isNotOK(response)) {
                // check retry
                int maxRetries = this.configServiceWrapper.getIntProperty("ofdb.maxRetries.movie", 0);
                if (videoData.getRetries() < maxRetries && ResponseTools.isTemporaryError(response)) {
                    LOG.info("Temporary error accessing OFDb; retry later for {}", ofdbUrl);
                    return ScanResult.RETRY;
                }
                LOG.error("Can't find movie data due response status {}: {}", response.getStatusCode(), ofdbUrl);
                return ScanResult.ERROR;
            }
            
            final String xml = response.getContent();
            
            String title = HTMLTools.extractTag(xml, "<title>OFDb -", "</title>");
            // check for movie type change
            if (title.contains("[TV-Serie]")) {
                LOG.warn("{} is a TV Show, skipping", videoData.getTitle());
                return ScanResult.TYPE_CHANGE;
            }

            // retrieve IMDb id if not set
            String imdbId = videoData.getSourceDbId(ImdbScanner.SCANNER_ID);
            if (StringUtils.isBlank(imdbId)) {
                imdbId = HTMLTools.extractTag(xml, "href=\"http://www.imdb.com/Title?", "\"");
                videoData.setSourceDbId(ImdbScanner.SCANNER_ID, "tt" + imdbId);
            }

            if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                String titleShort = HTMLTools.extractTag(xml, "<title>OFDb -", "</title>");
                if (titleShort.indexOf('(') > 0) {
                    // strip year from title
                    titleShort = titleShort.substring(0, titleShort.lastIndexOf('(')).trim();
                }
                videoData.setTitle(titleShort, SCANNER_ID);
            }

            // scrape plot and outline
            String plotMarker = HTMLTools.extractTag(xml, "<a href=\"plot/", 0, "\"");
            if (StringUtils.isNotBlank(plotMarker) && OverrideTools.checkOneOverwrite(videoData, SCANNER_ID, OverrideFlag.PLOT, OverrideFlag.OUTLINE)) {
                try {
                    response = httpClient.requestContent("http://www.ofdb.de/plot/" + plotMarker, charset);
                    if (ResponseTools.isNotOK(response)) {
                        // check retry
                        int maxRetries = this.configServiceWrapper.getIntProperty("ofdb.maxRetries.movie", 0);
                        if (videoData.getRetries() < maxRetries && ResponseTools.isTemporaryError(response)) {
                            LOG.info("Temporary error accessing OFDb; retry later for {}", ofdbUrl);
                            scanResult = ScanResult.RETRY;
                        } else {
                            LOG.error("Can't find movie plot due response status {}: {}", response.getStatusCode(), ofdbUrl);
                            scanResult = ScanResult.ERROR;
                        }
                    } else {
                        int firstindex = response.getContent().indexOf("gelesen</b></b><br><br>") + 23;
                        int lastindex = response.getContent().indexOf(HTML_FONT, firstindex);
                        String plot = response.getContent()
                                              .substring(firstindex, lastindex)
                                              .replaceAll("<br />", " ")
                                              .trim();
    
                        if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                            videoData.setPlot(plot, SCANNER_ID);
                        }
    
                        if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                            videoData.setOutline(plot, SCANNER_ID);
                        }
                    }
                } catch (IOException ex) {
                    LOG.error("Failed retrieving plot '{}': {}", ofdbUrl, ex.getMessage());
                    LOG.trace("Scanner error", ex);
                    scanResult = ScanResult.ERROR;
                }
            }

            // scrape additional informations
            int beginIndex = xml.indexOf("view.php?page=film_detail");
            if (beginIndex != -1) {
                String detailUrl = "http://www.ofdb.de/" + xml.substring(beginIndex, xml.indexOf('\"', beginIndex));
                response = httpClient.requestContent(detailUrl, charset);
                if (ResponseTools.isNotOK(response)) {
                    // check retry
                    int maxRetries = this.configServiceWrapper.getIntProperty("ofdb.maxRetries.movie", 0);
                    if (videoData.getRetries() < maxRetries && ResponseTools.isTemporaryError(response)) {
                        LOG.info("Temporary error accessing OFDb; retry later for {}", ofdbUrl);
                        return ScanResult.RETRY;
                    }
                    LOG.error("Can't find movie details due response status {}: {}", response.getStatusCode(), ofdbUrl);
                    return ScanResult.ERROR;
                }

                final String detailXml = response.getContent();
                
                // resolve for additional informations
                List<String> tags = HTMLTools.extractHtmlTags(detailXml, "<!-- Rechte Spalte -->", HTML_TABLE_END, HTML_TR_START, HTML_TR_END);

                for (String tag : tags) {
                    if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID) && tag.contains("Originaltitel")) {
                        String scraped = HTMLTools.removeHtmlTags(HTMLTools.extractTag(tag, "class=\"Daten\">", HTML_FONT)).trim();
                        videoData.setTitleOriginal(scraped, SCANNER_ID);
                    }

                    if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID) && tag.contains("Erscheinungsjahr")) {
                        String scraped = HTMLTools.removeHtmlTags(HTMLTools.extractTag(tag, "class=\"Daten\">", HTML_FONT)).trim();
                        videoData.setPublicationYear(MetadataTools.toYear(scraped), SCANNER_ID);
                    }

                    if (OverrideTools.checkOverwriteCountry(videoData, SCANNER_ID) && tag.contains("Herstellungsland")) {
                        List<String> scraped = HTMLTools.extractHtmlTags(tag, "class=\"Daten\"", "</td>", "<a", "</a>");
                        if (!scraped.isEmpty()) {
                            // TODO set more countries in movie
                            videoData.setCountry(HTMLTools.removeHtmlTags(scraped.get(0)).trim(), SCANNER_ID);
                        }
                    }

                    if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID) && tag.contains("Genre(s)")) {
                        List<String> scraped = HTMLTools.extractHtmlTags(tag, "class=\"Daten\"", "</td>", "<a", "</a>");
                        HashSet<String> genreNames = new HashSet<>();
                        for (String genre : scraped) {
                            genreNames.add(HTMLTools.removeHtmlTags(genre).trim());
                        }
                        videoData.setGenreNames(genreNames, SCANNER_ID);
                    }
                }

                // DIRECTORS
                if (this.configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
                    if (detailXml.contains("<i>Regie</i>")) {
                        tags = HTMLTools.extractHtmlTags(detailXml, "<i>Regie</i>", HTML_TABLE_END, HTML_TR_START, HTML_TR_END);
                        for (String tag : tags) {
                            videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.DIRECTOR, extractName(tag)));
                        }
                    }
                }

                // WRITERS
                if (this.configServiceWrapper.isCastScanEnabled(JobType.WRITER)) {
                    if (detailXml.contains("<i>Drehbuchautor(in)</i>")) {
                        tags = HTMLTools.extractHtmlTags(detailXml, "<i>Drehbuchautor(in)</i>", HTML_TABLE_END, HTML_TR_START, HTML_TR_END);
                        for (String tag : tags) {
                            videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.WRITER, extractName(tag)));
                        }
                    }
                }

                // ACTORS
                if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
                    if (detailXml.contains("<i>Darsteller</i>")) {
                        tags = HTMLTools.extractHtmlTags(detailXml, "<i>Darsteller</i>", HTML_TABLE_END, HTML_TR_START, HTML_TR_END);
                        for (String tag : tags) {
                            videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.ACTOR, extractName(tag), extractRole(tag)));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            LOG.error("Failed retrieving movie data '{}': {}", ofdbUrl, ex.getMessage());
            LOG.trace("Scanner error", ex);
            scanResult = ScanResult.ERROR;
        }
        return scanResult;
    }

    private String extractName(String tag) {
        String name = HTMLTools.extractTag(tag, "class=\"Daten\">", HTML_FONT);
        int akaIndex = name.indexOf("als <i>");
        if (akaIndex > 0) {
            name = name.substring(0, akaIndex);
        }
        return HTMLTools.removeHtmlTags(name);
    }

    private String extractRole(String tag) {
        String role = HTMLTools.extractTag(tag, "class=\"Normal\">", HTML_FONT);
        role = HTMLTools.removeHtmlTags(role);
        if (role.startsWith("... ")) {
            role = role.substring(4);
        }
        return role;
    }

    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(SCANNER_ID))) {
            return Boolean.TRUE;
        }

        // scan for IMDb ID
        ImdbScanner.scanImdbID(nfoContent, dto, ignorePresentId);

        LOG.trace("Scanning NFO for OFDb url");

        try {
            int beginIndex = nfoContent.indexOf("http://www.ofdb.de/film/");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(nfoContent.substring(beginIndex), " \n\t\r\f!&é\"'(èçà)=$<>");
                String sourceId = st.nextToken();
                LOG.debug("OFDb url found in NFO: {}", sourceId);
                dto.addId(SCANNER_ID, sourceId);
                return Boolean.TRUE;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        LOG.debug("No OFDb url found in NFO");
        return Boolean.FALSE;
    }
}