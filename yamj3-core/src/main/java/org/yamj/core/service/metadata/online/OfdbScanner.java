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

import static org.yamj.core.tools.Constants.UTF8;

import java.io.IOException;
import java.util.*;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.api.common.tools.ResponseTools;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.database.model.type.OverrideFlag;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.web.HTMLTools;
import org.yamj.core.web.apis.ImdbSearchEngine;
import org.yamj.core.web.apis.SearchEngineTools;

@Service("ofdbScanner")
public class OfdbScanner implements IMovieScanner {

    private static final String SCANNER_ID = "ofdb";
    private static final Logger LOG = LoggerFactory.getLogger(OfdbScanner.class);
    private static final String HTML_FONT = "</font>";
    private static final String HTML_TABLE_END = "</table>";
    private static final String HTML_TR_START = "<tr";
    private static final String HTML_TR_END = "</tr>";

    private SearchEngineTools searchEngineTools;

    @Autowired
    private PoolingHttpClient httpClient;
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ImdbSearchEngine imdbSearchEngine;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private LocaleService localeService;
    
    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize OFDb scanner");

        searchEngineTools = new SearchEngineTools(httpClient, Locale.GERMANY);
        searchEngineTools.setSearchSites("google");

        // register this scanner
        onlineScannerService.registerMetadataScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        return getMovieId(videoData, false);
    }

    private String getMovieId(VideoData videoData, boolean throwTempError) {
        String ofdbUrl = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNotBlank(ofdbUrl)) {
            return ofdbUrl;
        }
        
        // get and check IMDb id
        String imdbId = videoData.getSourceDbId(ImdbScanner.SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            boolean searchImdb = configServiceWrapper.getBooleanProperty("ofdb.search.imdb", false);
            if (searchImdb) {
                // search IMDb id if not present (don't throw error if temporary not available)
                imdbId = this.imdbSearchEngine.getImdbId(videoData.getTitle(), videoData.getPublicationYear(), false, false);
                if (StringUtils.isNotBlank(imdbId)) {
                    LOG.debug("Found IMDb id {} for movie '{}'", imdbId, videoData.getTitle());
                    videoData.setSourceDbId(ImdbScanner.SCANNER_ID, imdbId);
                }
            }
        }

        if (StringUtils.isNotBlank(imdbId)) {
            // if IMDb id is present then use this
            ofdbUrl = getOfdbIdByImdbId(imdbId, throwTempError);
        }
        
        if (StringUtils.isBlank(ofdbUrl)) {
            // try by title and year
            ofdbUrl = getOfdbIdByTitleAndYear(videoData.getTitle(), videoData.getPublicationYear(), throwTempError);
        }

        if (StringUtils.isBlank(ofdbUrl) && videoData.isTitleOriginalScannable()) {
            // try by original title and year
            ofdbUrl = getOfdbIdByTitleAndYear(videoData.getTitleOriginal(), videoData.getPublicationYear(), throwTempError);
        }

        if (StringUtils.isBlank(ofdbUrl)) {
            // try with search engines (don't throw error if temporary not available)
            ofdbUrl = searchEngineTools.searchURL(videoData.getTitle(), videoData.getPublicationYear(), "www.ofdb.de/film", false);
        }
        
        videoData.setSourceDbId(SCANNER_ID, ofdbUrl);
        return ofdbUrl;
    }

    private String getOfdbIdByImdbId(String imdbId, boolean throwTempError) {
        try {
            DigestedResponse response = httpClient.requestContent("http://www.ofdb.de/view.php?page=suchergebnis&SText=" + imdbId + "&Kat=IMDb", UTF8);
            if (throwTempError && ResponseTools.isTemporaryError(response)) {
                throw new TemporaryUnavailableException("OFDb service is temporary not available: " + response.getStatusCode());
            } else if (ResponseTools.isNotOK(response)) {
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
            LOG.error("Failed retrieving OFDb url for IMDb id {}: {}", imdbId, ex.getMessage());
            LOG.trace("OFDb service error", ex);
        }
        return null;
    }

    private String getOfdbIdByTitleAndYear(String title, int year, boolean throwTempError) {
        if (year <= 0) {
            // title and year must be present for successful OFDb advanced search
            // expected are 2 search parameters minimum; so skip here if year is not valid
            return null;
        }

        try {
            StringBuilder sb = new StringBuilder("http://www.ofdb.de/view.php?page=fsuche&Typ=N&AB=-&Titel=");
            sb.append(HTMLTools.encodePlain(title));
            sb.append("&Genre=-&HLand=-&Jahr=");
            sb.append(year);
            sb.append("&Wo=-&Land=-&Freigabe=-&Cut=A&Indiziert=A&Submit2=Suche+ausf%C3%BChren");

            
            DigestedResponse response = httpClient.requestContent(sb.toString(), UTF8);
            if (throwTempError && ResponseTools.isTemporaryError(response)) {
                throw new TemporaryUnavailableException("OFDb service is temporary not available: " + response.getStatusCode());
            } else if (ResponseTools.isNotOK(response)) {
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
            LOG.trace("OFDb service error", ex);
        }
        return null;
    }

    @Override
    public ScanResult scanMovie(VideoData videoData) {
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("ofdb.throwError.tempUnavailable", Boolean.TRUE);

            String ofdbUrl = getMovieId(videoData, throwTempError);
    
            if (StringUtils.isBlank(ofdbUrl)) {
                LOG.debug("OFDb url not available: {}", videoData.getIdentifier());
                return ScanResult.MISSING_ID;
            }
    
            LOG.debug("OFDb url available ({}), updating video data", ofdbUrl);
            return updateMovie(videoData, ofdbUrl, throwTempError);
            
        } catch (TemporaryUnavailableException tue) {
            int maxRetries = this.configServiceWrapper.getIntProperty("ofdb.maxRetries.movie", 0);
            if (videoData.getRetries() < maxRetries) {
                LOG.info("OFDb service temporary not available; trigger retry: '{}'", videoData.getIdentifier());
                return ScanResult.RETRY;
            }
            
            LOG.warn("OFDb service temporary not available; no retry: '{}'", videoData.getIdentifier());
            return ScanResult.ERROR;
            
        } catch (IOException ioe) {
            LOG.error("OFDb service error: '{}': {}", videoData.getIdentifier(), ioe.getMessage());
            return ScanResult.ERROR;
        }
    }

    private ScanResult updateMovie(VideoData videoData, String ofdbUrl, boolean throwTempError) throws IOException {
        DigestedResponse response = httpClient.requestContent(ofdbUrl, UTF8);
        if (throwTempError && ResponseTools.isTemporaryError(response)) {
            throw new TemporaryUnavailableException("OFDb service is temporary not available: " + response.getStatusCode());
        } else if (ResponseTools.isNotOK(response)) {
            throw new OnlineScannerException("OFDb request failed: " + response.getStatusCode());
        }
        
        String xml = response.getContent();
        String title = HTMLTools.extractTag(xml, "<title>OFDb -", "</title>");
        // check for movie type change
        if (title.contains("[TV-Serie]")) {
            LOG.warn("{} is a TV Show, skipping", videoData.getIdentifier());
            return ScanResult.TYPE_CHANGE;
        }
        
        // set IMDb id if not set before
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
            response = httpClient.requestContent("http://www.ofdb.de/plot/" + plotMarker, UTF8);
            if (throwTempError && ResponseTools.isTemporaryError(response)) {
                throw new TemporaryUnavailableException("OFDb service failed to get plot: " + response.getStatusCode());
            } else if (ResponseTools.isNotOK(response)) {
                throw new OnlineScannerException("OFDb plot request failed: " + response.getStatusCode());
            }
            
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

        // scrape additional informations
        int beginIndex = xml.indexOf("view.php?page=film_detail");
        if (beginIndex < 0) {
            // nothing to do anymore
            return ScanResult.OK;
        }
        
        String detailUrl = "http://www.ofdb.de/" + xml.substring(beginIndex, xml.indexOf('\"', beginIndex));
        response = httpClient.requestContent(detailUrl, UTF8);
        if (throwTempError && ResponseTools.isTemporaryError(response)) {
            throw new TemporaryUnavailableException("OFDb service failed to get details: " + response.getStatusCode());
        } else if (ResponseTools.isNotOK(response)) {
            throw new OnlineScannerException("OFDb details request failed: " + response.getStatusCode());
        }
        // get detail XML
        xml = response.getContent();
        
        // resolve for additional informations
        List<String> tags = HTMLTools.extractHtmlTags(xml, "<!-- Rechte Spalte -->", HTML_TABLE_END, HTML_TR_START, HTML_TR_END);

        for (String tag : tags) {
            if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID) && tag.contains("Originaltitel")) {
                String scraped = HTMLTools.removeHtmlTags(HTMLTools.extractTag(tag, "class=\"Daten\">", HTML_FONT)).trim();
                videoData.setTitleOriginal(scraped, SCANNER_ID);
            }

            if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID) && tag.contains("Erscheinungsjahr")) {
                String scraped = HTMLTools.removeHtmlTags(HTMLTools.extractTag(tag, "class=\"Daten\">", HTML_FONT)).trim();
                videoData.setPublicationYear(MetadataTools.toYear(scraped), SCANNER_ID);
            }

            if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID) && tag.contains("Genre(s)")) {
                HashSet<String> genreNames = new HashSet<>();
                for (String genre : HTMLTools.extractHtmlTags(tag, "class=\"Daten\"", "</td>", "<a", "</a>")) {
                    genreNames.add(HTMLTools.removeHtmlTags(genre).trim());
                }
                videoData.setGenreNames(genreNames, SCANNER_ID);
            }

            if (OverrideTools.checkOverwriteCountries(videoData, SCANNER_ID) && tag.contains("Herstellungsland")) {
                Set<String> countryCodes = new HashSet<>();
                for (String country : HTMLTools.extractHtmlTags(tag, "class=\"Daten\"", "</td>", "<a", "</a>")) {
                    final String countryCode = localeService.findCountryCode(HTMLTools.removeHtmlTags(country).trim());
                    if (countryCode != null) countryCodes.add(countryCode);
                }
                videoData.setCountryCodes(countryCodes, SCANNER_ID);
            }
        }

        // DIRECTORS
        if (this.configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
            tags = HTMLTools.extractHtmlTags(xml, "<i>Regie</i>", HTML_TABLE_END, HTML_TR_START, HTML_TR_END);
            for (String tag : tags) {
                videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.DIRECTOR, extractName(tag)));
            }
        }

        // WRITERS
        if (this.configServiceWrapper.isCastScanEnabled(JobType.WRITER)) {
            tags = HTMLTools.extractHtmlTags(xml, "<i>Drehbuchautor(in)</i>", HTML_TABLE_END, HTML_TR_START, HTML_TR_END);
            for (String tag : tags) {
                videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.WRITER, extractName(tag)));
            }
        }

        // ACTORS
        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            tags = HTMLTools.extractHtmlTags(xml, "<i>Darsteller</i>", HTML_TABLE_END, HTML_TR_START, HTML_TR_END);
            for (String tag : tags) {
                videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.ACTOR, extractName(tag), extractRole(tag)));
            }
        }
        
        // everything is fine
        return ScanResult.OK;
    }

    private static String extractName(String tag) {
        String name = HTMLTools.extractTag(tag, "class=\"Daten\">", HTML_FONT);
        int akaIndex = name.indexOf("als <i>");
        if (akaIndex > 0) {
            name = name.substring(0, akaIndex);
        }
        return HTMLTools.removeHtmlTags(name);
    }

    private static String extractRole(String tag) {
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