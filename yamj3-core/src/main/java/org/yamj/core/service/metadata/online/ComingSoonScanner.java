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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.WordUtils;
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
import org.yamj.core.web.apis.SearchEngineTools;

@Service("comingSoonScanner")
public class ComingSoonScanner implements IMovieScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ComingSoonScanner.class);
    private static final String SCANNER_ID = "comingsoon";
    private static final String COMINGSOON_BASE_URL = "http://www.comingsoon.it/";
    private static final String COMINGSOON_SEARCH_URL = "film/?";
    private static final String COMINGSOON_FILM_URL = "Film/Scheda/?";
    private static final String COMINGSOON_PERSONAGGI = "personaggi/";
    private static final String COMINGSOON_SEARCH_PARAMS = "&genere=&nat=&regia=&attore=&orderby=&orderdir=asc&page=";
    private static final String COMONGSOON_TITLE_PARAM = "titolo=";
    private static final String COMINGSOON_YEAR_PARAM = "anno=";
    private static final String COMINGSOON_KEY_PARAM = "key=";
    private static final int COMINGSOON_MAX_DIFF = 1000;
    private static final int COMINGSOON_MAX_SEARCH_PAGES = 5;
    
    private Charset charset;
    private SearchEngineTools searchEngineTools;

    @Autowired
    private PoolingHttpClient httpClient;
    @Autowired
    private OnlineScannerService onlineScannerService;
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
        LOG.info("Initialize ComingSoon scanner");

        charset = Charset.forName("UTF-8");

        searchEngineTools = new SearchEngineTools(httpClient, Locale.ITALY);
        
        // register this scanner
        onlineScannerService.registerMetadataScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        return getMovieId(videoData, false);
    }

    private String getMovieId(VideoData videoData, boolean throwTempError) {
        String comingSoonId = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNotBlank(comingSoonId)) {
            return comingSoonId;
        }
        
        // search coming soon site by title
        comingSoonId = getComingSoonId(videoData.getTitle(), videoData.getPublicationYear(), throwTempError);
        if (isNoValidComingSoonId(comingSoonId)) {
            videoData.setSourceDbId(SCANNER_ID, comingSoonId);
            return comingSoonId;
        }

        if (isNoValidComingSoonId(comingSoonId) && videoData.isTitleOriginalScannable()) {
            comingSoonId = getComingSoonId(videoData.getTitleOriginal(), videoData.getPublicationYear(), throwTempError);
        }

        if (isNoValidComingSoonId(comingSoonId)) {
            comingSoonId = this.searchEngineTools.searchURL(videoData.getTitle(), videoData.getPublicationYear(), "www.comingsoon.it/film", throwTempError);
        
        }
        if (isNoValidComingSoonId(comingSoonId)) {
            return null;
        }
        
        videoData.setSourceDbId(SCANNER_ID, comingSoonId);
        return comingSoonId;
    }

    private static boolean isNoValidComingSoonId(String comingSoonId) {
        if (StringUtils.isBlank(comingSoonId)) return true;
        return StringUtils.equalsIgnoreCase(comingSoonId, "na");
    }
        
    private String getComingSoonId(String title, int year, boolean throwTempError) {
        return getComingSoonId(title, year, COMINGSOON_MAX_DIFF, throwTempError);
    }

    private String getComingSoonId(String title, int year, int scoreToBeat, boolean throwTempError) {
        if (scoreToBeat == 0) return null;
        int currentScore = scoreToBeat;

        try {
            StringBuilder urlBase = new StringBuilder(COMINGSOON_BASE_URL);
            urlBase.append(COMINGSOON_SEARCH_URL);
            urlBase.append(COMONGSOON_TITLE_PARAM);
            urlBase.append(URLEncoder.encode(title.toLowerCase(), "UTF-8"));

            urlBase.append("&").append(COMINGSOON_YEAR_PARAM);
            if (year > 0 ) {
                urlBase.append(year);
            }
            urlBase.append(COMINGSOON_SEARCH_PARAMS);

            int searchPage = 0;
            String comingSoonId = null;
            
            loop: while (searchPage++ < COMINGSOON_MAX_SEARCH_PAGES) {

                StringBuilder urlPage = new StringBuilder(urlBase);
                if (searchPage > 1) {
                    urlPage.append("&p=").append(searchPage);
                }

                LOG.debug("Fetching ComingSoon search page {}/{} - URL: {}", searchPage, COMINGSOON_MAX_SEARCH_PAGES, urlPage.toString());
                DigestedResponse response = httpClient.requestContent(urlPage.toString(), charset);
                if (throwTempError && ResponseTools.isTemporaryError(response)) {
                    throw new TemporaryUnavailableException("ComingSoon service is temporary not available: " + response.getStatusCode());
                } else if (ResponseTools.isNotOK(response)) {
                    LOG.error("Can't find ComingSoon ID due response status {}", response.getStatusCode());
                    return null;
                }

                List<String[]> movieList = parseComingSoonSearchResults(response.getContent());
                if (movieList.isEmpty()) {
                    break loop;
                }
                
                for (int i = 0; i < movieList.size() && currentScore > 0; i++) {
                    String lId = movieList.get(i)[0];
                    String lTitle = movieList.get(i)[1];
                    String lOrig = movieList.get(i)[2];
                    //String lYear = (String) movieList.get(i)[3];
                    int difference = compareTitles(title, lTitle);
                    int differenceOrig = compareTitles(title, lOrig);
                    difference = (differenceOrig < difference ? differenceOrig : difference);
                    if (difference < currentScore) {
                        if (difference == 0) {
                            LOG.debug("Found perfect match for: {}, {}", lTitle, lOrig);
                            searchPage = COMINGSOON_MAX_SEARCH_PAGES; //ends loop
                        } else {
                            LOG.debug("Found a match for: {}, {}, difference {}", lTitle, lOrig, difference);
                        }
                        comingSoonId = lId;
                        currentScore = difference;
                    }
                }
            }

            if (year>0 && currentScore>0) {
                LOG.debug("Perfect match not found, trying removing by year ...");
                String newComingSoonId = getComingSoonId(title, -1, currentScore, throwTempError);
                comingSoonId = (isNoValidComingSoonId(newComingSoonId) ? comingSoonId : newComingSoonId);
            }

            if (StringUtils.isNotBlank(comingSoonId)) {
                LOG.debug("Found valid ComingSoon ID: {}", comingSoonId);
            }

            return comingSoonId;

        } catch (IOException ex) {
            LOG.error("Failed retrieving ComingSoon id for title '{}': {}", title, ex.getMessage());
            LOG.trace("ComingSoon service error", ex);
            return null;
        }
    }

    /**
     * Parse the search results
     *
     * Search results end with "Trovati NNN Film" (found NNN movies).
     *
     * After this string, more movie URL are found, so we have to set a boundary
     *
     * @param xml
     * @return
     */
    private static List<String[]> parseComingSoonSearchResults(String xml) {
        final List<String[]> result = new ArrayList<>();
        
        int beginIndex = StringUtils.indexOfIgnoreCase(xml, "Trovate");
        int moviesFound = -1;
        if (beginIndex > 0) {
            int end = xml.indexOf(" film", beginIndex + 7);
            if (end > 0) {
                String tmp = HTMLTools.stripTags(xml.substring(beginIndex + 8, xml.indexOf(" film", beginIndex)));
                moviesFound = NumberUtils.toInt(tmp, -1);
            }
        }

        if (moviesFound < 0) {
            LOG.error("Couldn't find 'Trovate NNN film in archivio' string. Search page layout probably changed");
            return result;
        }
 
        List<String> films = HTMLTools.extractTags(xml, "box-lista-cinema", "BOX FILM RICERCA", "<a h", "</a>", false);
        if (CollectionUtils.isEmpty(films)) {
            return result;
        }
        
        LOG.debug("Search found {} movies", films.size());

        for (String film : films) {
            String comingSoonId = null;
            beginIndex = film.indexOf("ref=\"/film/");
            if (beginIndex >= 0) {
                comingSoonId = getComingSoonIdFromURL(film);
            }
            if (StringUtils.isBlank(comingSoonId)) continue;

            String title = HTMLTools.extractTag(film, "<div class=\"h5 titolo cat-hover-color anim25\">", "</div>");
            if (StringUtils.isBlank(title)) continue;
            
            String originalTitle = HTMLTools.extractTag(film, "<div class=\"h6 sottotitolo\">", "</div>");
            originalTitle = StringUtils.trimToEmpty(originalTitle);
            if (originalTitle.startsWith("(")) originalTitle = originalTitle.substring(1, originalTitle.length() - 1).trim();
            
            String year = null;
            beginIndex = film.indexOf("ANNO</span>:");
            if (beginIndex > 0) {
                int endIndex = film.indexOf("</li>", beginIndex);
                if (endIndex > 0) {
                    year = film.substring(beginIndex + 12, endIndex).trim();
                }
            }
            
            result.add(new String[]{comingSoonId, title, originalTitle, year});
        }

        return result;
    }

    private static String getComingSoonIdFromURL(String url) {
        int index = url.indexOf("/scheda");
        if (index > -1) {
            String stripped = url.substring(0, index);
            index = StringUtils.lastIndexOf(stripped, '/');
            if (index > -1) {
                return stripped.substring(index + 1);
            }
        }
        return null;
    }

    /**
     * Returns difference between two titles.
     *
     * Since ComingSoon returns strange results on some researches, difference
     * is defined as follows: abs(word count difference) - (searchedTitle wordcount - matched words)
     *
     * @param searchedTitle
     * @param returnedTitle
     * @return
     */
    private static int compareTitles(String searchedTitle, String returnedTitle) {
        if (StringUtils.isBlank(returnedTitle)) return COMINGSOON_MAX_DIFF;
        LOG.trace("Comparing {} and {}", searchedTitle, returnedTitle);

        String title1 = searchedTitle.toLowerCase().replaceAll("[,.\\!\\?\"']", "");
        String title2 = returnedTitle.toLowerCase().replaceAll("[,.\\!\\?\"']", "");
        return StringUtils.getLevenshteinDistance(title1, title2);
    }


    @Override
    public ScanResult scanMovie(VideoData videoData) {
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("comingsoon.throwError.tempUnavailable", Boolean.TRUE);

            String comingSoonId = getMovieId(videoData, throwTempError);
    
            if (isNoValidComingSoonId(comingSoonId)) {
                LOG.debug("ComingSoon ID not available: {}", videoData.getIdentifier());
                return ScanResult.MISSING_ID;
            }
    
            LOG.debug("ComingSoon ID available ({}), updating video data", comingSoonId);
            return updateMovie(videoData, comingSoonId, throwTempError);
            
        } catch (TemporaryUnavailableException tue) {
            int maxRetries = this.configServiceWrapper.getIntProperty("comingsoon.maxRetries.movie", 0);
            if (videoData.getRetries() < maxRetries) {
                LOG.info("ComingSoon service temporary not available; trigger retry: '{}'", videoData.getIdentifier());
                return ScanResult.RETRY;
            }
            
            LOG.warn("ComingSoon service temporary not available; no retry: '{}'", videoData.getIdentifier());
            return ScanResult.ERROR;
            
        } catch (IOException ioe) {
            LOG.error("ComingSoon service error: '{}': {}", videoData.getIdentifier(), ioe.getMessage());
            return ScanResult.ERROR;
        }
    }
        
    private ScanResult updateMovie(VideoData videoData, String comingSoonId, boolean throwTempError) throws IOException {
        final String url = COMINGSOON_BASE_URL + COMINGSOON_FILM_URL + COMINGSOON_KEY_PARAM + comingSoonId;
        DigestedResponse response = httpClient.requestContent(url, charset);
        if (throwTempError && ResponseTools.isTemporaryError(response)) {
            throw new TemporaryUnavailableException("ComingSoon service is temporary not available: " + response.getStatusCode());
        } else if (ResponseTools.isNotOK(response)) {
            throw new OnlineScannerException("ComingSoon request failed: " + response.getStatusCode());
        }
        
        String xml = response.getContent();

        // TITLE
        if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
            int beginIndex = xml.indexOf("<h1 itemprop=\"name\"");
            if (beginIndex < 0 ) {
                LOG.error("No title found at ComingSoon page. HTML layout has changed?");
                return ScanResult.NO_RESULT;
            }
            
            String tag = xml.substring(beginIndex, xml.indexOf(">", beginIndex)+1);
            String title = HTMLTools.extractTag(xml, tag, "</h1>").trim();
            if (StringUtils.isBlank(title)) return ScanResult.NO_RESULT;
            
            videoData.setTitle(WordUtils.capitalizeFully(title), SCANNER_ID);
        }

        // PLOT AND OUTLINE
        if (OverrideTools.checkOneOverwrite(videoData, SCANNER_ID, OverrideFlag.PLOT, OverrideFlag.OUTLINE)) {
            int beginIndex = xml.indexOf("<div class=\"contenuto-scheda-destra");
            if (beginIndex < 0) {
                LOG.error("No plot found at ComingSoon page. HTML layout has changed?");
                return ScanResult.NO_RESULT;
            }

            int endIndex = xml.indexOf("<div class=\"box-descrizione\"", beginIndex);
            if (endIndex < 0) {
                LOG.error("No plot found at ComingSoon page. HTML layout has changed?");
                return ScanResult.NO_RESULT;
            }

            final String xmlPlot = HTMLTools.stripTags(HTMLTools.extractTag(xml.substring(beginIndex, endIndex), "<p>", "</p>"));
            if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                videoData.setPlot(xmlPlot, SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                videoData.setOutline(xmlPlot, SCANNER_ID);
            }
        }
        
        // ORIGINAL TITLE
        if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID)) {
            String titleOriginal = HTMLTools.extractTag(xml, "Titolo originale:", "</p>").trim();
            if (titleOriginal.startsWith("(")) {
                titleOriginal = titleOriginal.substring(1, titleOriginal.length() - 1).trim();
            }
            videoData.setTitleOriginal(titleOriginal, SCANNER_ID);
        }

        // RATING
        String rating = HTMLTools.extractTag(xml, "<span itemprop=\"ratingValue\">", "</span>");
        if (StringUtils.isNotBlank(rating)) {
            int ratingInt = (int) (NumberUtils.toFloat(rating.replace(',', '.'), 0) * 20); // Rating is 0 to 5, we normalize to 100
            videoData.addRating(SCANNER_ID, ratingInt);
        }

        // RELEASE DATE
        String dateToParse = HTMLTools.stripTags(HTMLTools.extractTag(xml, "<time itemprop=\"datePublished\">", "</time>"));
        Date releaseDate = MetadataTools.parseToDate(dateToParse);
        if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
            videoData.setRelease(releaseDate, SCANNER_ID);
        }
        
        // YEAR
        if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID)) {
            String year = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">ANNO</span>:", "</li>")).trim();
            int intYear = NumberUtils.toInt(year, 0); 
            if (intYear > 1900) {
                videoData.setPublicationYear(intYear, SCANNER_ID);
            } else {
                videoData.setPublicationYear(MetadataTools.extractYearAsInt(releaseDate), SCANNER_ID);
            }
        } 
        
        // COUNTRY
        if (OverrideTools.checkOverwriteCountries(videoData, SCANNER_ID)) {
            final String country = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">PAESE</span>:", "</li>")).trim();
            final String countryCode = localeService.findCountryCode(country);
            if (countryCode != null) {
                videoData.setCountryCodes(Collections.singleton(countryCode), SCANNER_ID);
            }
        }
        
        // COMPANY
        if (OverrideTools.checkOverwriteStudios(videoData, SCANNER_ID)) {
            final String studioList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">PRODUZIONE</span>: ","</li>"));
            if (StringUtils.isNotBlank(studioList)) {
                Collection<String> studioNames = new ArrayList<>();
                StringTokenizer st = new StringTokenizer(studioList, ",");
                while (st.hasMoreTokens()) {
                    studioNames.add(st.nextToken().trim());
                }
                videoData.setStudioNames(studioNames, SCANNER_ID);
            }
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
            final String genreList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">GENERE</span>: ", "</li>"));
            if (StringUtils.isNotBlank(genreList)) {
                Collection<String> genreNames = new ArrayList<>();
                StringTokenizer st = new StringTokenizer(genreList, ",");
                while (st.hasMoreTokens()) {
                    genreNames.add(st.nextToken().trim());
                }
                videoData.setGenreNames(genreNames, SCANNER_ID);
            }
        }

        // DIRECTORS
        if (this.configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
            List<String> tags = HTMLTools.extractTags(xml, ">REGIA</span>:", "</li>", "<a", "</a>", false);
            addCrew(videoData, JobType.DIRECTOR, tags);
        }

        // WRITERS
        if (this.configServiceWrapper.isCastScanEnabled(JobType.WRITER)) {
            List<String> tags = HTMLTools.extractTags(xml, ">SCENEGGIATURA</span>:", "</li>", "<a", "</a>", false);
            addCrew(videoData, JobType.WRITER, tags);
        }

        // SOUND
        if (this.configServiceWrapper.isCastScanEnabled(JobType.SOUND)) {
            List<String> tags = HTMLTools.extractTags(xml, ">MUSICHE</span>:", "</li>", "<a", "</a>", false);
            addCrew(videoData, JobType.SOUND, tags);
        }

        // CAMERA
        if (this.configServiceWrapper.isCastScanEnabled(JobType.CAMERA)) {
            List<String> tags = HTMLTools.extractTags(xml, ">FOTOGRAFIA</span>:", "</li>", "<a", "</a>", false);
            addCrew(videoData, JobType.CAMERA, tags);
        }

        // EDITING
        if (this.configServiceWrapper.isCastScanEnabled(JobType.EDITING)) {
            List<String> tags = HTMLTools.extractTags(xml, ">MONTAGGIO</span>:", "</li>", "<a", "</a>", false);
            addCrew(videoData, JobType.EDITING, tags);
        }
        
        // CAST
        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            List<String> tags = HTMLTools.extractTags(xml, "Il Cast</div>", "<!-- /IL CAST -->", "<a href=\"/personaggi/", "</a>", false);
            for (String tag : tags) {
                String name = HTMLTools.extractTag(tag, "<div class=\"h6 titolo\">", "</div>");
                String role = HTMLTools.extractTag(tag, "<div class=\"h6 descrizione\">", "</div>");
                
                String sourceId = null;
                int beginIndex = tag.indexOf('/');
                if (beginIndex >-1) {
                    int endIndex = tag.indexOf('/', beginIndex+1);
                    if (endIndex > beginIndex) {
                        sourceId = tag.substring(beginIndex+1, endIndex);
                    }
                }
                
                CreditDTO credit = new CreditDTO(SCANNER_ID, sourceId, JobType.ACTOR, name, role);
                
                String posterURL = HTMLTools.extractTag(tag, "<img src=\"", "\"");
                if (posterURL.contains("http")) {
                    posterURL = posterURL.replace("_ico.jpg", ".jpg");
                    credit.addPhoto(SCANNER_ID, posterURL);
                }
                
                videoData.addCreditDTO(credit);
            }
        }

        return ScanResult.OK;
    }

    private static void addCrew(VideoData videoData, JobType jobType, List<String> tags) {
        for (String tag : tags) {
            int beginIndex = tag.indexOf(">");
            if (beginIndex > -1) {
                String name = tag.substring(beginIndex+1);
                
                String sourceId = null;
                beginIndex = tag.indexOf(COMINGSOON_PERSONAGGI);
                if (beginIndex > -1) {
                    beginIndex = tag.indexOf("/", beginIndex+COMINGSOON_PERSONAGGI.length()+1);
                    int endIndex = tag.indexOf("/", beginIndex+1);
                    if (endIndex > beginIndex) {
                        sourceId = tag.substring(beginIndex+1, endIndex);
                    }
                }
                
                videoData.addCreditDTO(new CreditDTO(SCANNER_ID, sourceId, jobType, name));
            }
        }
    }
    
    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(SCANNER_ID))) {
            return Boolean.TRUE;
        }

        // scan for IMDb ID
        ImdbScanner.scanImdbID(nfoContent, dto, ignorePresentId);

        LOG.trace("Scanning NFO for ComingSoon ID");

        try {
            int beginIndex = nfoContent.indexOf("?key=");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(nfoContent.substring(beginIndex + 5), "/ \n,:!&é\"'(--è_çà)=$");
                String sourceId = st.nextToken();
                LOG.debug("ComingSoon ID found in NFO: {}", sourceId);
                dto.addId(SCANNER_ID, sourceId);
                return Boolean.TRUE;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        LOG.debug("No ComingSoon ID found in NFO");
        return Boolean.FALSE;
    }
}
