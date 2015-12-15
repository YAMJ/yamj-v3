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
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
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
public class ComingSoonScanner implements IMovieScanner, ISeriesScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ComingSoonScanner.class);
    private static final String SCANNER_ID = "comingsoon";
    private static final String COMINGSOON_BASE_URL = "http://www.comingsoon.it/";
    private static final String COMINGSOON_SEARCH_MOVIE = "film/?";
    private static final String COMINGSOON_SEARCH_MOVIE_PARAMS = "&genere=&nat=&regia=&attore=&orderby=&orderdir=asc&page=";
    private static final String COMINGSOON_SEARCH_SERIES = "serietv/ricerca/?";
    private static final String COMINGSOON_SEARCH_SERIES_PARAMS = "&genere=&attore=&orderby=&orderdir=asc&page=";
    private static final String COMONGSOON_TITLE_PARAM = "titolo=";
    private static final String COMINGSOON_YEAR_PARAM = "anno=";
    private static final String COMINGSOON_KEY_PARAM = "key=";
    private static final String COMINGSOON_MOVIE_URL = "film/scheda/?";
    private static final String COMINGSOON_SERIES_URL = "serietv/scheda/?";
    private static final String COMINGSOON_PERSONAGGI = "personaggi/";
    private static final int COMINGSOON_MAX_DIFF = 1000;
    private static final int COMINGSOON_MAX_SEARCH_PAGES = 5;
        
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
        comingSoonId = getComingSoonId(videoData.getTitle(), videoData.getPublicationYear(), false, throwTempError);

        // search coming soon site by original title
        if (isNoValidComingSoonId(comingSoonId) && videoData.isTitleOriginalScannable()) {
            comingSoonId = getComingSoonId(videoData.getTitleOriginal(), videoData.getPublicationYear(), false, throwTempError);
        }

        // search coming soon with search engine tools
        if (isNoValidComingSoonId(comingSoonId)) {
            comingSoonId = this.searchEngineTools.searchURL(videoData.getTitle(), videoData.getPublicationYear(), "www.comingsoon.it/film", throwTempError);
            int beginIndex = comingSoonId.indexOf("film/");
            if (beginIndex < 0) {
                comingSoonId = null;
            } else {
                beginIndex = comingSoonId.indexOf("/", beginIndex+6);
                int endIndex = comingSoonId.indexOf("/", beginIndex+1);
                if (beginIndex < endIndex) {
                    comingSoonId = comingSoonId.substring(beginIndex+1, endIndex);
                } else {
                    comingSoonId = null;
                }
            }
        }
        
        if (isNoValidComingSoonId(comingSoonId)) {
            return null;
        }
        
        videoData.setSourceDbId(SCANNER_ID, comingSoonId);
        return comingSoonId;
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
        final String url = COMINGSOON_BASE_URL + COMINGSOON_MOVIE_URL + COMINGSOON_KEY_PARAM + comingSoonId;
        DigestedResponse response = httpClient.requestContent(url, UTF8);
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

        // ORIGINAL TITLE
        if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID)) {
            videoData.setTitleOriginal(parseTitleOriginal(xml), SCANNER_ID);
        }

        // PLOT AND OUTLINE
        if (OverrideTools.checkOneOverwrite(videoData, SCANNER_ID, OverrideFlag.PLOT, OverrideFlag.OUTLINE)) {
            final String plot = parsePlot(xml);
            if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                videoData.setPlot(plot, SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                videoData.setOutline(plot, SCANNER_ID);
            }
        }
        
        // RATING
        videoData.addRating(SCANNER_ID, parseRating(xml));

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
            videoData.setCountryCodes(parseCountries(xml), SCANNER_ID);
        }
        
        // STUDIOS
        if (OverrideTools.checkOverwriteStudios(videoData, SCANNER_ID)) {
            videoData.setStudioNames(parseStudios(xml), SCANNER_ID);
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
            videoData.setGenreNames(parseGenres(xml), SCANNER_ID);
        }

        // DIRECTORS
        if (this.configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
            videoData.addCreditDTOS(parseCredits(xml, ">REGIA</span>:", JobType.DIRECTOR));
        }

        // WRITERS
        if (this.configServiceWrapper.isCastScanEnabled(JobType.WRITER)) {
            videoData.addCreditDTOS(parseCredits(xml, ">SCENEGGIATURA</span>:", JobType.WRITER));
        }

        // SOUND
        if (this.configServiceWrapper.isCastScanEnabled(JobType.SOUND)) {
            videoData.addCreditDTOS(parseCredits(xml, ">MUSICHE</span>:", JobType.SOUND));
        }

        // CAMERA
        if (this.configServiceWrapper.isCastScanEnabled(JobType.CAMERA)) {
            videoData.addCreditDTOS(parseCredits(xml, ">FOTOGRAFIA</span>:", JobType.CAMERA));
        }

        // EDITING
        if (this.configServiceWrapper.isCastScanEnabled(JobType.EDITING)) {
            videoData.addCreditDTOS(parseCredits(xml, ">MONTAGGIO</span>:", JobType.EDITING));
        }
        
        // CAST
        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            videoData.addCreditDTOS(parseActors(xml));
        }

        return ScanResult.OK;
    }

    @Override
    public String getSeriesId(Series series) {
        return getSeriesId(series, false);
    }

    private String getSeriesId(Series series, boolean throwTempError) {
        String comingSoonId = series.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNotBlank(comingSoonId)) {
            return comingSoonId;
        }
        
        // search coming soon site by title
        comingSoonId = getComingSoonId(series.getTitle(), series.getStartYear(), true, throwTempError);

        // search coming soon site by original title
        if (isNoValidComingSoonId(comingSoonId) && series.isTitleOriginalScannable()) {
            comingSoonId = getComingSoonId(series.getTitleOriginal(), series.getStartYear(), true, throwTempError);
        }

        // search coming soon with search engine tools
        if (isNoValidComingSoonId(comingSoonId)) {
            comingSoonId = this.searchEngineTools.searchURL(series.getTitle(), series.getStartYear(), "www.comingsoon.it/serietv", throwTempError);
            int beginIndex = comingSoonId.indexOf("serietv/");
            if (beginIndex < 0) {
                comingSoonId = null;
            } else {
                beginIndex = comingSoonId.indexOf("/", beginIndex+9);
                int endIndex = comingSoonId.indexOf("/", beginIndex+1);
                if (beginIndex < endIndex) {
                    comingSoonId = comingSoonId.substring(beginIndex+1, endIndex);
                } else {
                    comingSoonId = null;
                }
            }
        }
        
        if (isNoValidComingSoonId(comingSoonId)) {
            return null;
        }
        
        series.setSourceDbId(SCANNER_ID, comingSoonId);
        return comingSoonId;
    }

    @Override
    public ScanResult scanSeries(Series series) {
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("comingsoon.throwError.tempUnavailable", Boolean.TRUE);

            String comingSoonId = getSeriesId(series, throwTempError);
    
            if (isNoValidComingSoonId(comingSoonId)) {
                LOG.debug("ComingSoon ID not available: {}", series.getIdentifier());
                return ScanResult.MISSING_ID;
            }
    
            LOG.debug("ComingSoon ID available ({}), updating series", comingSoonId);
            return updateSeries(series, comingSoonId, throwTempError);
            
        } catch (TemporaryUnavailableException tue) {
            int maxRetries = this.configServiceWrapper.getIntProperty("comingsoon.maxRetries.movie", 0);
            if (series.getRetries() < maxRetries) {
                LOG.info("ComingSoon service temporary not available; trigger retry: '{}'", series.getIdentifier());
                return ScanResult.RETRY;
            }
            
            LOG.warn("ComingSoon service temporary not available; no retry: '{}'", series.getIdentifier());
            return ScanResult.ERROR;
            
        } catch (IOException ioe) {
            LOG.error("ComingSoon service error: '{}': {}", series.getIdentifier(), ioe.getMessage());
            return ScanResult.ERROR;
        }
    }
        
    private ScanResult updateSeries(Series series, String comingSoonId, boolean throwTempError) throws IOException {
        final String url = COMINGSOON_BASE_URL + COMINGSOON_SERIES_URL + COMINGSOON_KEY_PARAM + comingSoonId;
        DigestedResponse response = httpClient.requestContent(url, UTF8);
        if (throwTempError && ResponseTools.isTemporaryError(response)) {
            throw new TemporaryUnavailableException("ComingSoon service is temporary not available: " + response.getStatusCode());
        } else if (ResponseTools.isNotOK(response)) {
            throw new OnlineScannerException("ComingSoon request failed: " + response.getStatusCode());
        }
        String xml = response.getContent();
        
        // TITLE
        String title = null;
        if (OverrideTools.checkOverwriteTitle(series, SCANNER_ID)) {
            int beginIndex = xml.indexOf("<h1 itemprop=\"name\"");
            if (beginIndex < 0 ) {
                LOG.error("No title found at ComingSoon page. HTML layout has changed?");
                return ScanResult.NO_RESULT;
            }
            
            String tag = xml.substring(beginIndex, xml.indexOf(">", beginIndex)+1);
            title = HTMLTools.extractTag(xml, tag, "</h1>").trim();
            if (StringUtils.isBlank(title)) return ScanResult.NO_RESULT;

            title = WordUtils.capitalizeFully(title);
            series.setTitle(title, SCANNER_ID);
        }

        // ORIGINAL TITLE
        String titleOriginal = parseTitleOriginal(xml);
        if (OverrideTools.checkOverwriteOriginalTitle(series, SCANNER_ID)) {
            series.setTitleOriginal(titleOriginal, SCANNER_ID);
        }

        // PLOT AND OUTLINE
        String plot = parsePlot(xml);
        if (OverrideTools.checkOverwritePlot(series, SCANNER_ID)) {
            series.setPlot(plot, SCANNER_ID);
        }
        if (OverrideTools.checkOverwriteOutline(series, SCANNER_ID)) {
            series.setOutline(plot, SCANNER_ID);
        }

        // RATING
        series.addRating(SCANNER_ID, parseRating(xml));
        
        // YEAR
        if (OverrideTools.checkOverwriteYear(series, SCANNER_ID)) {
            String year = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">ANNO</span>:", "</li>")).trim();
            int intYear = NumberUtils.toInt(year, 0); 
            if (intYear > 1900) series.setStartYear(intYear, SCANNER_ID);
        } 
        
        // COUNTRY
        if (OverrideTools.checkOverwriteCountries(series, SCANNER_ID)) {
            series.setCountryCodes(parseCountries(xml), SCANNER_ID);
        }
        
        // STUDIOS
        if (OverrideTools.checkOverwriteStudios(series, SCANNER_ID)) {
            series.setStudioNames(parseStudios(xml), SCANNER_ID);
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(series, SCANNER_ID)) {
            series.setGenreNames(parseGenres(xml), SCANNER_ID);
        }

        // ACTORS
        Collection<CreditDTO> actors;
        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            actors = parseActors(xml);
        } else {
            actors = Collections.emptyList();
        }
        
        // scan seasons and episodes
        scanSeasons(series, comingSoonId, title, titleOriginal, plot, actors);
        
        return ScanResult.OK;
    }

    private void scanSeasons(Series series, String comingSoonId, String title, String titleOriginal, String plot, Collection<CreditDTO> actors) {
        
        for (Season season : series.getSeasons()) {
        
            String seasonXML = getSeasonXml(comingSoonId, season.getSeason());

            if (!season.isTvSeasonDone(SCANNER_ID)) {
                // same as series ID
                season.setSourceDbId(SCANNER_ID, comingSoonId);
                
                // use values from series
                if (OverrideTools.checkOverwriteTitle(season, SCANNER_ID)) {
                    season.setTitle(title, SCANNER_ID);
                }
                if (OverrideTools.checkOverwriteOriginalTitle(season, SCANNER_ID)) {
                    season.setTitleOriginal(titleOriginal, SCANNER_ID);
                }
                if (OverrideTools.checkOverwritePlot(season, SCANNER_ID)) {
                    season.setPlot(plot, SCANNER_ID);
                }
                if (OverrideTools.checkOverwriteOutline(season, SCANNER_ID)) {
                    season.setOutline(plot, SCANNER_ID);
                }
    
                // TODO start year from season XML for Italia
                
                // mark season as done
                season.setTvSeasonDone();
            }
            
            // scan episodes
            scanEpisodes(season, comingSoonId, seasonXML, actors);
        }
    }

    private void scanEpisodes(Season season, String comingSoonId, String seasonXML, Collection<CreditDTO> actors) {
        
        // parse episodes from season XML
        Map<Integer,EpisodeDTO> episodes = this.parseEpisodes(seasonXML);

        for (VideoData videoData : season.getVideoDatas()) {
            
            if (videoData.isTvEpisodeDone(SCANNER_ID)) {
                // nothing to do if already done
                continue;
            }

            EpisodeDTO episode = episodes.get(videoData.getEpisode());
            if (episode == null) {
                videoData.removeOverrideSource(SCANNER_ID);
                videoData.removeSourceDbId(SCANNER_ID);
                videoData.setTvEpisodeNotFound();
                continue;
            }
            
            // set coming soon id for episode
            videoData.setSourceDbId(SCANNER_ID, comingSoonId);
            
            if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                videoData.setTitle(episode.getTitle(), SCANNER_ID);
            }

            if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID)) {
                videoData.setTitleOriginal(episode.getTitleOriginal(), SCANNER_ID);
            }

            // add actors
            videoData.addCreditDTOS(actors);
            // add directors and writers
            videoData.addCreditDTOS(episode.getCredits());
            
            // mark episode as done
            videoData.setTvEpisodeDone();
        }
    }
    
    private String getSeasonXml(String comingSoonId, int season) {
        final String url = COMINGSOON_BASE_URL + "/serietv/scheda/" + comingSoonId + "/episodi/stagione-" + season + "/";

        String xml = null; 
        try {
            DigestedResponse response = httpClient.requestContent(url, UTF8);
            if (ResponseTools.isNotOK(response)) {
                LOG.error("ComingSoon request failed for episodes of season {}-{}: {}", comingSoonId, season, response.getStatusCode());
            } else {
                xml = response.getContent();
            }
        } catch (Exception ex) {
            LOG.error("ComingSoon episodes request failed", ex);
        }
        return xml;
    }
    
    private Map<Integer,EpisodeDTO> parseEpisodes(String seasonXML) {
        Map<Integer,EpisodeDTO> episodes = new HashMap<>();
        if (StringUtils.isBlank(seasonXML)) return episodes;
        
        List<String> tags = HTMLTools.extractTags(seasonXML, "BOX LISTA EPISODI SERIE TV", "BOX LISTA EPISODI SERIE TV", "<div class=\"box-contenitore", "<!-");
        for (String tag : tags) {
            System.err.println(tag);
            int episode = NumberUtils.toInt(HTMLTools.extractTag(tag, "episode=\"", "\""), -1);
            if (episode > -1) {
                EpisodeDTO dto = new EpisodeDTO();
                dto.setEpisode(episode);
                dto.setTitle(HTMLTools.extractTag(tag, "img title=\"", "\""));
                dto.setTitleOriginal(HTMLTools.extractTag(tag, " descrizione\">", "</div>"));
                
                if (this.configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
                    for (CreditDTO credit : parseEpisodeCredits(tag, ">REGIA</strong>:", JobType.DIRECTOR)) {
                        dto.addCredit(credit);
                    }
                }

                if (this.configServiceWrapper.isCastScanEnabled(JobType.WRITER)) {
                    for (CreditDTO credit : parseEpisodeCredits(tag, ">SCENEGGIATURA</strong>:", JobType.WRITER)) {
                        dto.addCredit(credit);
                    }
                }

                episodes.put(dto.getEpisode(), dto);
            }
        }
        
        return episodes;
    }
    
    private static class EpisodeDTO {
        
        private int episode;
        private String title;
        private String titleOriginal;
        private Collection<CreditDTO> credits = new HashSet<>();
        
        public int getEpisode() {
            return episode;
        }
        public void setEpisode(int episode) {
            this.episode = episode;
        }
        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }
        public String getTitleOriginal() {
            return titleOriginal;
        }
        public void setTitleOriginal(String titleOriginal) {
            this.titleOriginal = titleOriginal;
        }
        public Collection<CreditDTO> getCredits() {
            return credits;
        }
        public void addCredit(CreditDTO credit) {
            this.credits.add(credit);
        }
    }
    
    private static String parseTitleOriginal(String xml) {
        String titleOriginal = HTMLTools.extractTag(xml, "Titolo originale:", "</p>").trim();
        if (titleOriginal.startsWith("(")) {
            titleOriginal = titleOriginal.substring(1, titleOriginal.length() - 1).trim();
        }
        return titleOriginal;
    }
    
    private static String parsePlot(String xml) {
        int beginIndex = xml.indexOf("<div class=\"contenuto-scheda-destra");
        if (beginIndex < 0) return null;
        
        int endIndex = xml.indexOf("<div class=\"box-descrizione\"", beginIndex);
        if (endIndex < 0) return null;

        return  HTMLTools.stripTags(HTMLTools.extractTag(xml.substring(beginIndex, endIndex), "<p>", "</p>"));
    }
    
    private static int parseRating(String xml) {
        String rating = HTMLTools.extractTag(xml, "<span itemprop=\"ratingValue\">", "</span>");
        if (StringUtils.isNotBlank(rating)) {
            // Rating is 0 to 5, we normalize to 100
            return (int) (NumberUtils.toFloat(rating.replace(',', '.'), -1.0f) * 20);
        }
        return -1;
    }

    private Collection<String> parseCountries(String xml) {
        final String country = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">PAESE</span>:", "</li>")).trim();
        final String countryCode = localeService.findCountryCode(country);
        if (countryCode != null) return Collections.singleton(countryCode);
        return null;
    }
    
    private static Collection<String> parseStudios(String xml) {
        final String studioList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">PRODUZIONE</span>: ","</li>"));
        if (StringUtils.isBlank(studioList)) return null;
        
        Collection<String> studioNames = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(studioList, ",");
        while (st.hasMoreTokens()) {
            studioNames.add(st.nextToken().trim());
        }
        return studioNames;
    }
    
    private static Collection<String> parseGenres(String xml) {
        final String genreList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">GENERE</span>: ", "</li>"));
        if (StringUtils.isBlank(genreList)) return null;
        
        Collection<String> genreNames = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(genreList, ",");
        while (st.hasMoreTokens()) {
            genreNames.add(st.nextToken().trim());
        }
        return genreNames;
    }
    
    private static boolean isNoValidComingSoonId(String comingSoonId) {
        if (StringUtils.isBlank(comingSoonId)) return true;
        return StringUtils.equalsIgnoreCase(comingSoonId, "na");
    }
        
    private String getComingSoonId(String title, int year, boolean tvShow, boolean throwTempError) {
        return getComingSoonId(title, year, COMINGSOON_MAX_DIFF, tvShow, throwTempError);
    }

    private String getComingSoonId(String title, int year, int scoreToBeat, boolean tvShow, boolean throwTempError) {
        if (scoreToBeat == 0) return null;
        int currentScore = scoreToBeat;

        try {
            StringBuilder urlBase = new StringBuilder(COMINGSOON_BASE_URL);
            if (tvShow) {
                urlBase.append(COMINGSOON_SEARCH_SERIES);
            } else {
                urlBase.append(COMINGSOON_SEARCH_MOVIE);
            }
            urlBase.append(COMONGSOON_TITLE_PARAM);
            urlBase.append(HTMLTools.encodeUrl(title.toLowerCase()));

            urlBase.append("&").append(COMINGSOON_YEAR_PARAM);
            if (year > 0 ) {
                urlBase.append(year);
            }
            if (tvShow) {
                urlBase.append(COMINGSOON_SEARCH_SERIES_PARAMS);
            } else {
                urlBase.append(COMINGSOON_SEARCH_MOVIE_PARAMS);
            }
            int searchPage = 0;
            String comingSoonId = null;
            
            loop: while (searchPage++ < COMINGSOON_MAX_SEARCH_PAGES) {

                StringBuilder urlPage = new StringBuilder(urlBase);
                if (searchPage > 1) {
                    urlPage.append("&p=").append(searchPage);
                }

                LOG.debug("Fetching ComingSoon search page {}/{} - URL: {}", searchPage, COMINGSOON_MAX_SEARCH_PAGES, urlPage.toString());
                DigestedResponse response = httpClient.requestContent(urlPage.toString(), UTF8);
                if (throwTempError && ResponseTools.isTemporaryError(response)) {
                    throw new TemporaryUnavailableException("ComingSoon service is temporary not available: " + response.getStatusCode());
                } else if (ResponseTools.isNotOK(response)) {
                    LOG.error("Can't find ComingSoon ID due response status {}", response.getStatusCode());
                    return null;
                }

                List<String[]> resultList = parseComingSoonSearchResults(response.getContent(), tvShow);
                if (resultList.isEmpty()) {
                    break loop;
                }
                
                for (int i = 0; i < resultList.size() && currentScore > 0; i++) {
                    String lId = resultList.get(i)[0];
                    String lTitle = resultList.get(i)[1];
                    String lOrig = resultList.get(i)[2];
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
                String newComingSoonId = getComingSoonId(title, -1, currentScore, tvShow, throwTempError);
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
    private static List<String[]> parseComingSoonSearchResults(String xml, boolean tvShow) {
        final List<String[]> result = new ArrayList<>();
        
        int beginIndex = StringUtils.indexOfIgnoreCase(xml, "Trovate");
        int resultsFound = -1;
        if (beginIndex > 0) {
            int end = xml.indexOf((tvShow?" serie tv":" film"), beginIndex + 7);
            if (end > 0) {
                String tmp = HTMLTools.stripTags(xml.substring(beginIndex + 8, end));
                resultsFound = NumberUtils.toInt(tmp, -1);
            }
        }

        if (resultsFound < 0) {
            LOG.error("Couldn't find 'Trovate NNN "+(tvShow?"serie tv":"film")+" in archivio' string. Search page layout probably changed");
            return result;
        }
 
        List<String> searchResults = HTMLTools.extractTags(xml, "box-lista-cinema", "BOX FILM RICERCA", "<a h", "</a>", false);
        if (CollectionUtils.isEmpty(searchResults)) {
            return result;
        }
        
        LOG.debug("Search found {} results", searchResults.size());

        for (String searchResult : searchResults) {
            String comingSoonId = null;
            if (tvShow) {
                beginIndex = searchResult.indexOf("ref=\"/serietv/");
            } else {
                beginIndex = searchResult.indexOf("ref=\"/film/");
            }
            if (beginIndex >= 0) {
                comingSoonId = getComingSoonIdFromURL(searchResult);
            }
            if (StringUtils.isBlank(comingSoonId)) continue;

            String title = HTMLTools.extractTag(searchResult, "<div class=\"h5 titolo cat-hover-color anim25\">", "</div>");
            if (StringUtils.isBlank(title)) continue;
            
            String originalTitle = HTMLTools.extractTag(searchResult, "<div class=\"h6 sottotitolo\">", "</div>");
            originalTitle = StringUtils.trimToEmpty(originalTitle);
            if (originalTitle.startsWith("(")) originalTitle = originalTitle.substring(1, originalTitle.length() - 1).trim();
            
            String year = null;
            beginIndex = searchResult.indexOf("ANNO</span>:");
            if (beginIndex > 0) {
                int endIndex = searchResult.indexOf("</li>", beginIndex);
                if (endIndex > 0) {
                    year = searchResult.substring(beginIndex + 12, endIndex).trim();
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

    private static Collection<CreditDTO> parseCredits(String xml, String startTag, JobType jobType) {
        List<CreditDTO> credits = new ArrayList<>();
        for (String tag : HTMLTools.extractTags(xml, startTag, "</li>", "<a", "</a>", false)) {
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
                
                credits.add(new CreditDTO(SCANNER_ID, sourceId, jobType, name));
            }
        }
        return credits;
    }

    private static Collection<CreditDTO> parseActors(String xml) {
        List<CreditDTO> credits = new ArrayList<>();
        for (String tag : HTMLTools.extractTags(xml, "Il Cast</div>", "IL CAST -->", "<a href=\"/personaggi/", "</a>", false)) {
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
            
            credits.add(credit);
        }
        return credits;
    }

    private static Collection<CreditDTO> parseEpisodeCredits(String xml, String startTag, JobType jobType) {
        List<CreditDTO> credits = new ArrayList<>();
        for (String name : HTMLTools.extractTag(xml, startTag, "</li>").split(",")) {
            credits.add(new CreditDTO(SCANNER_ID, jobType, name));
        }
        return credits;
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
