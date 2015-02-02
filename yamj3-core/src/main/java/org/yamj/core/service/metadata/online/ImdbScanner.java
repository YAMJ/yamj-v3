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
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.core.configuration.ConfigServiceWrapper;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.AwardDTO;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.tools.web.*;

@Service("imdbScanner")
public class ImdbScanner implements IMovieScanner, ISeriesScanner, IPersonScanner {

    public static final String SCANNER_ID = "imdb";

    private static final Logger LOG = LoggerFactory.getLogger(ImdbScanner.class);
    private static final String HTML_H5_END = ":</h5>";
    private static final String HTML_H5_START = "<h5>";
    private static final String HTML_DIV_END = "</div>";
    private static final String HTML_A_END = "</a>";
    private static final String HTML_A_START = "<a ";
    private static final String HTML_SLASH_PIPE = "\\|";
    private static final String HTML_NAME = "name/";
    private static final String HTML_H4_END = ":</h4>";
    private static final String HTML_SITE_FULL = "http://www.imdb.com/";
    private static final String HTML_TITLE = "title/";
    private static final String HTML_SPAN_END = "</span>";
    private static final String HTML_TABLE_END = "</table>";
    private static final String HTML_TD_END = "</td>";
    private static final String HTML_GT = ">";
    private static final Pattern PATTERN_PERSON_DOB = Pattern.compile("(\\d{1,2})-(\\d{1,2})");

    private Charset charset;

    @Autowired
    private PoolingHttpClient httpClient;
    @Autowired
    private ImdbSearchEngine imdbSearchEngine;
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;

    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize IMDb scanner");

        charset = Charset.forName("UTF-8");

        // register this scanner
        onlineScannerService.registerMovieScanner(this);
        onlineScannerService.registerSeriesScanner(this);
        onlineScannerService.registerPersonScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        return getMovieId(videoData, false);
    }

    private String getMovieId(VideoData videoData, boolean throwTempError) {
        String imdbId = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            imdbId = imdbSearchEngine.getImdbId(videoData.getTitle(), videoData.getPublicationYear(), false, throwTempError);
            videoData.setSourceDbId(SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getSeriesId(Series series) {
        return getSeriesId(series, false);
    }

    private String getSeriesId(Series series, boolean throwTempError) {
        String imdbId = series.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            imdbId = imdbSearchEngine.getImdbId(series.getTitle(), series.getStartYear(), true, throwTempError);
            series.setSourceDbId(SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public ScanResult scan(VideoData videoData) {
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("imdb.throwError.tempUnavailable", Boolean.TRUE);

            String imdbId = getMovieId(videoData, throwTempError);
            
            if (StringUtils.isBlank(imdbId)) {
                LOG.debug("IMDb id not available : {}", videoData.getTitle());
                return ScanResult.MISSING_ID;
            }

            LOG.debug("IMDb id available ({}), updating movie", imdbId);
            return updateVideoData(videoData, imdbId, throwTempError);
            
        } catch (TemporaryUnavailableException tue) {
            // check retry
            int maxRetries = this.configServiceWrapper.getIntProperty("imdb.maxRetries.movie", 0);
            if (videoData.getRetries() < maxRetries) {
                LOG.info("IMDb service temporary not available; trigger retry: '{}'", videoData.getTitle());
                return ScanResult.RETRY;
            }
            LOG.warn("IMDb service temporary not available; no retry: '{}'", videoData.getTitle());
            return ScanResult.ERROR;
            
        } catch (IOException ioe) {
            LOG.error("IMDb service error: '" + videoData.getTitle() + "'", ioe);
            return ScanResult.ERROR;
        }
    }

    private ScanResult updateVideoData(VideoData videoData, String imdbId, boolean throwTempError) throws IOException {
        DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId), charset);
        if (throwTempError && ResponseTools.isTemporaryError(response)) {
            throw new TemporaryUnavailableException("IMDb service is temporary not available: " + response.getStatusCode());
        } else if (ResponseTools.isNotOK(response)) {
            throw new OnlineScannerException("IMDb request failed: " + response.getStatusCode());
        }

        // check type change
        String xml = response.getContent();
        if (xml.contains("\"tv-extra\"") || xml.contains("\"tv-series-series\"")) {
            return ScanResult.TYPE_CHANGE;
        }
        if (StringUtils.contains(HTMLTools.extractTag(xml, "<title>"), "(TV Series")) {
            return ScanResult.TYPE_CHANGE;
        }

        // get header tag
        String headerXml = HTMLTools.extractTag(xml, "<h1 class=\"header\">", "</h1>");

        // TITLE
        if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
            videoData.setTitle(parseTitle(headerXml), SCANNER_ID);
        }

        // ORIGINAL TITLE
        if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID)) {
            videoData.setTitleOriginal(parseOriginalTitle(headerXml), SCANNER_ID);
        }

        // YEAR
        if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID)) {
            videoData.setPublicationYear(parseYear(headerXml), SCANNER_ID);
        }

        // PLOT
        if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
            videoData.setPlot(parsePlot(xml), SCANNER_ID);
        }

        // OUTLINE
        if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
            videoData.setOutline(parseOutline(xml), SCANNER_ID);
        }

        // TAGLINE
        if (OverrideTools.checkOverwriteTagline(videoData, SCANNER_ID)) {
            videoData.setTagline(parseTagline(xml), SCANNER_ID);
        }

        // QUOTE
        if (OverrideTools.checkOverwriteQuote(videoData, SCANNER_ID)) {
            videoData.setQuote(parseQuote(xml), SCANNER_ID);
        }

        // RATING
        String srtRating = HTMLTools.extractTag(xml, "star-box-giga-star\">", HTML_DIV_END).replace(",", ".");
        int intRating = parseRating(HTMLTools.stripTags(srtRating));
        // try another format for the rating
        if (intRating == -1) {
            srtRating = HTMLTools.extractTag(xml, "star-bar-user-rate\">", HTML_SPAN_END).replace(",", ".");
            intRating = parseRating(HTMLTools.stripTags(srtRating));
        }
        videoData.addRating(SCANNER_ID, intRating);

        // TOP250
        String strTop = HTMLTools.extractTag(xml, "Top 250 #");
        if (StringUtils.isNumeric(strTop)) {
            videoData.setTopRank(NumberUtils.toInt(strTop, -1));
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
            videoData.setGenreNames(parseGenres(xml), SCANNER_ID);
        }

        // STUDIOS
        if (OverrideTools.checkOverwriteStudios(videoData, SCANNER_ID)) {
            videoData.setStudioNames(parseStudios(imdbId), SCANNER_ID);
        }

        // COUNTRIES
        if (OverrideTools.checkOverwriteCountries(videoData, SCANNER_ID)) {
            videoData.setCountryNames(parseCountries(xml), SCANNER_ID);
        }
        
        // CERTIFICATIONS
        videoData.setCertificationInfos(parseCertifications(imdbId));

        // RELEASE DATE
        parseReleaseData(videoData, imdbId);

        // CAST and CREW
        parseCastCrew(videoData, imdbId);

        // AWARDS
        if (configServiceWrapper.getBooleanProperty("imdb.movie.awards", Boolean.FALSE)) {
            videoData.addAwards(parseAwards(imdbId), SCANNER_ID);
        }
        
        return ScanResult.OK;
    }


    @Override
    public ScanResult scan(Series series) {
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("imdb.throwError.tempUnavailable", Boolean.TRUE);

            String imdbId = getSeriesId(series, throwTempError);
            
            if (StringUtils.isBlank(imdbId)) {
                LOG.debug("IMDb id not available: {}", series.getTitle());
                return ScanResult.MISSING_ID;
            }

            LOG.debug("IMDb id available ({}), updating series", imdbId);
            return updateSeries(series, imdbId, throwTempError);
            
        } catch (TemporaryUnavailableException tue) {
            // check retry
            int maxRetries = this.configServiceWrapper.getIntProperty("imdb.maxRetries.tvshow", 0);
            if (series.getRetries() < maxRetries) {
                LOG.info("IMDb service temporary not available; trigger retry: '{}'", series.getTitle());
                return ScanResult.RETRY;
            }
            LOG.warn("IMDb service temporary not available; no retry: '{}'", series.getTitle());
            return ScanResult.ERROR;
            
        } catch (IOException ioe) {
            LOG.error("IMDb service error: '" + series.getTitle() + "'", ioe);
            return ScanResult.ERROR;
        }
    }

    private ScanResult updateSeries(Series series, String imdbId, boolean throwTempError) throws IOException {
        DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId), charset);
        if (throwTempError && ResponseTools.isTemporaryError(response)) {
            throw new TemporaryUnavailableException("IMDb service is temporary not available: " + response.getStatusCode());
        } else if (ResponseTools.isNotOK(response)) {
            throw new OnlineScannerException("IMDb request failed: " + response.getStatusCode());
        }
        
        // get content
        final String xml = response.getContent();
        
        // get header tag
        final String headerXml = HTMLTools.extractTag(xml, "<h1 class=\"header\">", "</h1>");

        // TITLE
        String title = parseTitle(headerXml);
        if (OverrideTools.checkOverwriteTitle(series, SCANNER_ID)) {
            series.setTitle(title, SCANNER_ID);
        }

        // ORIGINAL TITLE
        String titleOriginal = parseOriginalTitle(headerXml);
        if (OverrideTools.checkOverwriteOriginalTitle(series, SCANNER_ID)) {
            series.setTitleOriginal(titleOriginal, SCANNER_ID);
        }

        // START YEAR and END YEAR
        if (OverrideTools.checkOverwriteYear(series, SCANNER_ID)) {
            parseYears(headerXml, series);
        }

        // PLOT
        String plot = parsePlot(xml);
        if (OverrideTools.checkOverwritePlot(series, SCANNER_ID)) {
            series.setPlot(plot, SCANNER_ID);
        }

        // OUTLINE
        String outline = parseOutline(xml);
        if (OverrideTools.checkOverwriteOutline(series, SCANNER_ID)) {
            series.setOutline(outline, SCANNER_ID);
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(series, SCANNER_ID)) {
            series.setGenreNames(parseGenres(xml), SCANNER_ID);
        }

        // STUDIOS
        if (OverrideTools.checkOverwriteStudios(series, SCANNER_ID)) {
            series.setStudioNames(parseStudios(imdbId), SCANNER_ID);
        }

        // COUNTRIES
        if (OverrideTools.checkOverwriteCountries(series, SCANNER_ID)) {
            series.setCountryNames(parseCountries(xml), SCANNER_ID);
        }

        // CERTIFICATIONS
        series.setCertificationInfos(parseCertifications(imdbId));

        // RELEASE DATE
        parseReleaseData(series, imdbId);

        // AWARDS
        if (configServiceWrapper.getBooleanProperty("imdb.tvshow.awards", Boolean.FALSE)) {
            series.addAwards(parseAwards(imdbId), SCANNER_ID);
        }

        // scan seasons
        this.scanSeasons(series, imdbId, title, titleOriginal, plot, outline);

        return ScanResult.OK;
    }

    private void scanSeasons(Series series, String imdbId, String title, String titleOriginal, String plot, String outline) {
        for (Season season : series.getSeasons()) {

            // use values from series
            if (OverrideTools.checkOverwriteTitle(season, SCANNER_ID)) {
                season.setTitle(title, SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteOriginalTitle(season, SCANNER_ID)) {
                season.setTitle(titleOriginal, SCANNER_ID);
            }
            if (OverrideTools.checkOverwritePlot(season, SCANNER_ID)) {
                season.setPlot(plot, SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteOutline(season, SCANNER_ID)) {
                season.setOutline(outline, SCANNER_ID);
            }

            Map<Integer, ImdbEpisodeDTO> episodes = null;
            if (OverrideTools.checkOverwriteYear(season, SCANNER_ID)) {
                episodes = getEpisodes(imdbId, season.getSeason());

                Date publicationYear = null;
                for (ImdbEpisodeDTO episode : episodes.values()) {
                    if (publicationYear == null) {
                        publicationYear = episode.getAirDate();
                    } else if (episode.getAirDate() != null) {
                        if (publicationYear.after(episode.getAirDate())) {
                            // previous episode
                            publicationYear = episode.getAirDate();
                        }
                    }
                }
                season.setPublicationYear(MetadataTools.extractYearAsInt(publicationYear), SCANNER_ID);
            }

            // mark season as done
            season.setTvSeasonDone();

            // only scan episodes if not done before
            if (!season.isTvEpisodesScanned(SCANNER_ID)) {
                if (episodes == null) {
                    episodes = getEpisodes(imdbId, season.getSeason());
                }

                for (VideoData videoData : season.getVideoDatas()) {
                    if (videoData.isTvEpisodeDone(SCANNER_ID)) {
                        // nothing to do if already done
                        continue;
                    }

                    // scan episode
                    this.scanEpisode(videoData, episodes.get(videoData.getEpisode()));
                }
            }
        }
    }

    private void scanEpisode(VideoData videoData, ImdbEpisodeDTO dto) {
        if (dto == null) {
            videoData.setTvEpisodeNotFound();
            return;
        } else if (StringUtils.isBlank(dto.getImdbId())) {
            // set other values
            if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                videoData.setTitle(dto.getTitle(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                videoData.setReleaseDate(dto.getAirDate(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                videoData.setPlot(dto.getOutline(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                videoData.setOutline(dto.getOutline(), SCANNER_ID);
            }
            videoData.setTvEpisodeNotFound();
            return;
        }

        try {
            DigestedResponse response = httpClient.requestContent(getImdbUrl(dto.getImdbId()), charset);
            if (ResponseTools.isNotOK(response)) {
                LOG.error("Can't find episode due response status {}: {}", response.getStatusCode(), dto.getImdbId());
                videoData.setTvEpisodeNotFound();
                return;
            }

            // set IMDb id
            videoData.setSourceDbId(SCANNER_ID, dto.getImdbId());

            // get header tag
            final String xml = response.getContent();
            final String headerXml = HTMLTools.extractTag(xml, "<h1 class=\"header\">", "</h1>");

            // TITLE
            if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                String title = parseTitle(headerXml);
                if (StringUtils.isBlank(title)) {
                    title = dto.getTitle();
                }
                videoData.setTitle(title, SCANNER_ID);
            }

            // ORIGINAL TITLE
            if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID)) {
                videoData.setTitleOriginal(parseOriginalTitle(headerXml), SCANNER_ID);
            }

            // RELEASE DATE (First Aired)
            if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                Date releaseDate = parseFirstAiredDate(headerXml);
                if (releaseDate == null) {
                    releaseDate = dto.getAirDate();
                }
                videoData.setReleaseDate(releaseDate, SCANNER_ID);
            }

            // PLOT
            if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                String plot = parsePlot(xml);
                if (StringUtils.isBlank(plot)) {
                    plot = dto.getOutline();
                }
                videoData.setPlot(plot, SCANNER_ID);
            }

            // OUTLINE
            if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                String outline = parseOutline(xml);
                if (StringUtils.isBlank(outline)) {
                    outline = dto.getOutline();
                }
                videoData.setOutline(outline, SCANNER_ID);
            }

            // TAGLINE
            if (OverrideTools.checkOverwriteTagline(videoData, SCANNER_ID)) {
                videoData.setTagline(parseTagline(xml), SCANNER_ID);
            }

            // QUOTE
            if (OverrideTools.checkOverwriteQuote(videoData, SCANNER_ID)) {
                videoData.setQuote(parseQuote(xml), SCANNER_ID);
            }

            // CAST and CREW
            parseCastCrew(videoData, dto.getImdbId());

        } catch (Exception ex) {
            LOG.error("Failed to scan episode: " + dto.getImdbId(), ex);
            videoData.setTvEpisodeNotFound();
        }
    }

    private Map<Integer, ImdbEpisodeDTO> getEpisodes(String imdbId, int season) {
        Map<Integer, ImdbEpisodeDTO> episodes = new HashMap<>();

        DigestedResponse response;
        try {
            response = httpClient.requestContent(getImdbUrl(imdbId, "episodes?season=" + season), charset);
            if (ResponseTools.isNotOK(response)) {
                LOG.error("Can't find episodes due response status {}: {}", response.getStatusCode(), imdbId);
                return episodes;
            }
        } catch (Exception ex) {
            LOG.error("Failed to get episodes for season: " + imdbId, ex);
            return episodes;
        }
            
        // scrape episode tags
        List<String> tags = HTMLTools.extractTags(response.getContent(), "<h3 id=\"episode_top\"", "<h2>See also</h2>", "<div class=\"info\" itemprop=\"episodes\"", "<div class=\"clear\"");

        for (String tag : tags) {
            // scrape episode number
            int episode = -1;

            int episodeIdx = tag.indexOf("<meta itemprop=\"episodeNumber\"");
            if (episodeIdx >= 0) {
                int beginIndex = episodeIdx + ("<meta itemprop=\"episodeNumber\" content=\"".length());
                int endIndex = tag.indexOf("\"", beginIndex);
                if (endIndex >= 0) {
                    episode = NumberUtils.toInt(tag.substring(beginIndex, endIndex), -1);
                }
            }

            // scrape IMDb id
            if (episode > -1) {
                ImdbEpisodeDTO dto = new ImdbEpisodeDTO();
                dto.setEpisode(episode);

                // set title
                String value = HTMLTools.extractTag(tag, "itemprop=\"name\">", HTML_A_END);
                dto.setTitle(value);

                // set air date
                value = HTMLTools.extractTag(tag, "<div class=\"airdate\">", HTML_DIV_END);
                dto.setAirDate(MetadataTools.parseToDate(StringUtils.trimToNull(value)));

                // set outline
                value = HTMLTools.extractTag(tag, "<div class=\"item_description\" itemprop=\"description\">", HTML_DIV_END);
                dto.setOutline(StringUtils.trimToNull(HTMLTools.removeHtmlTags(value)));

                // set source id
                int beginIndex = tag.indexOf("/tt");
                if (beginIndex != -1) {
                    StringTokenizer st = new StringTokenizer(tag.substring(beginIndex + 1), "/ \n,:!&Ã©\"'(--Ã¨_Ã§Ã )=$");
                    dto.setImdbId(st.nextToken());
                }

                episodes.put(episode, dto);
            }
        }

        return episodes;
    }

    private static String getImdbUrl(String imdbId) {
        return getImdbUrl(imdbId, null);
    }

    private static String getImdbUrl(String imdbId, String site) {
        String url = HTML_SITE_FULL + HTML_TITLE + imdbId + "/";
        if (site != null) {
            url = url + site;
        }
        return url;
    }

    private void parseReleaseData(AbstractMetadata metadata, String imdbId) {

        String releaseInfoXML = null;

        // RELEASE DATE
        if (metadata instanceof VideoData) {
            VideoData videoData = (VideoData) metadata;
            if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                // load the release page from IMDb
                releaseInfoXML = getReleasInfoXML(releaseInfoXML, imdbId);
                if (releaseInfoXML != null) {
                    String preferredCountry = this.configServiceWrapper.getProperty("imdb.aka.preferred.country", "USA");
                    Pattern pRelease = Pattern.compile("(?:.*?)\\Q" + preferredCountry + "\\E(?:.*?)\\Qrelease_date\">\\E(.*?)(?:<.*?>)(.*?)(?:</a>.*)");
                    Matcher mRelease = pRelease.matcher(releaseInfoXML);

                    if (mRelease.find()) {
                        String strReleaseDate = mRelease.group(1) + " " + mRelease.group(2);
                        Date releaseDate = MetadataTools.parseToDate(strReleaseDate);
                        videoData.setReleaseDate(releaseDate, SCANNER_ID);
                    }
                }
            }
        }

        // ORIGINAL TITLE / AKAS
        // Store the AKA list
        Map<String, String> akas = null;

        if (OverrideTools.checkOverwriteOriginalTitle(metadata, SCANNER_ID)) {
            // load the release page from IMDb
            releaseInfoXML = getReleasInfoXML(releaseInfoXML, imdbId);
            if (releaseInfoXML != null) {
                // get the AKAs from release info XML
                akas = getAkaMap(akas, releaseInfoXML);

                String foundValue = null;
                for (Map.Entry<String, String> aka : akas.entrySet()) {
                    if (StringUtils.indexOfIgnoreCase(aka.getKey(), "original title") > 0) {
                        foundValue = aka.getValue().trim();
                        break;
                    }
                }

                metadata.setTitleOriginal(foundValue, SCANNER_ID);
            }
        }

        // TITLE for preferred country from AKAS
        boolean akaScrapeTitle = configServiceWrapper.getBooleanProperty("imdb.aka.scrape.title", Boolean.FALSE);
        if (akaScrapeTitle && OverrideTools.checkOverwriteTitle(metadata, SCANNER_ID)) {
            List<String> akaIgnoreVersions = configServiceWrapper.getPropertyAsList("imdb.aka.ignore.versions", "");
            String preferredCountry = this.configServiceWrapper.getProperty("imdb.aka.preferred.country", "USA");
            String fallbacks = configServiceWrapper.getProperty("imdb.aka.fallback.countries", "");

            List<String> akaMatchingCountries;
            if (StringUtils.isBlank(fallbacks)) {
                akaMatchingCountries = Collections.singletonList(preferredCountry);
            } else {
                akaMatchingCountries = Arrays.asList((preferredCountry + "," + fallbacks).split(","));
            }

            // load the release page from IMDb
            releaseInfoXML = getReleasInfoXML(releaseInfoXML, imdbId);
            if (releaseInfoXML != null) {
                // get the AKAs from release info XML
                akas = getAkaMap(akas, releaseInfoXML);

                String foundValue = null;
                // NOTE: First matching country is the preferred country
                for (String matchCountry : akaMatchingCountries) {

                    if (StringUtils.isBlank(matchCountry)) {
                        // must be a valid country setting
                        continue;
                    }

                    for (Map.Entry<String, String> aka : akas.entrySet()) {
                        int startIndex = aka.getKey().indexOf(matchCountry);
                        if (startIndex > -1) {
                            String extracted = aka.getKey().substring(startIndex);
                            int endIndex = extracted.indexOf('/');
                            if (endIndex > -1) {
                                extracted = extracted.substring(0, endIndex);
                            }

                            boolean valid = Boolean.TRUE;
                            for (String ignore : akaIgnoreVersions) {
                                if (StringUtils.isNotBlank(ignore) && StringUtils.containsIgnoreCase(extracted, ignore.trim())) {
                                    valid = Boolean.FALSE;
                                    break;
                                }
                            }

                            if (valid) {
                                foundValue = aka.getValue().trim();
                                break;
                            }
                        }
                    }

                    if (foundValue != null) {
                        // we found a title for the country matcher
                        break;
                    }
                }

                metadata.setTitle(foundValue, SCANNER_ID);
            }
        }
    }

    private String getReleasInfoXML(final String releaseInfoXML, final String imdbId) {
        if (releaseInfoXML != null) {
            return releaseInfoXML;
        }

        try {
            final DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId, "releaseinfo"), charset);
            if (ResponseTools.isOK(response)) {
                return response.getContent();
            }
            LOG.warn("Requesting release infos failed with status {}: {}", response.getStatusCode(), imdbId);
        } catch (Exception ex) {
            LOG.error("Requesting release infos failed: " + imdbId, ex);
        }
        return null;                
    }

    private static Map<String, String> getAkaMap(Map<String, String> akas, String releaseInfoXML) {
        // The AKAs are stored in the format "title", "country"
        // therefore we need to look for the preferredCountry and then work backwards
        if (akas == null) {
            // Just extract the AKA section from the page
            List<String> akaList = HTMLTools.extractTags(releaseInfoXML, "<a id=\"akas\" name=\"akas\">", HTML_TABLE_END, "<td>", HTML_TD_END, Boolean.FALSE);
            return buildAkaMap(akaList);
        }
        return null;
    }
        
    
    /**
     * Parse the rating
     *
     * @param rating
     * @return
     */
    private static int parseRating(String rating) {
        StringTokenizer st = new StringTokenizer(rating, "/ ()");
        return MetadataTools.parseRating(st.nextToken());
    }

    private static Set<String> parseGenres(String xml) {
        Set<String> genres = new LinkedHashSet<>();
        for (String genre : HTMLTools.extractTags(xml, "Genres" + HTML_H4_END, HTML_DIV_END)) {
            // check normally for the genre
            String iGenre = HTMLTools.getTextAfterElem(genre, "<a");
            // sometimes the genre is just "{genre}</a>???" so try and remove the trailing element
            if (StringUtils.isBlank(iGenre) && genre.contains(HTML_A_END)) {
                iGenre = genre.substring(0, genre.indexOf(HTML_A_END));
            }
            genres.add(iGenre);
        }
        return genres;
    }

    private static String parsePlot(String xml) {
        String plot = HTMLTools.extractTag(xml, "<h2>Storyline</h2>", "<em class=\"nobr\">");
        plot = HTMLTools.removeHtmlTags(plot);
        if (StringUtils.isNotBlank(plot)) {
            // See if the plot has the "metacritic" text and remove it
            int pos = plot.indexOf("Metacritic.com)");
            if (pos > 0) {
                plot = plot.substring(pos + "Metacritic.com)".length());
            }
            plot = plot.trim();
        }
        return plot;
    }

    private static String parseOutline(String xml) {
        // the new outline is at the end of the review section with no preceding text
        String outline = HTMLTools.extractTag(xml, "<p itemprop=\"description\">", "</p>");
        return cleanStringEnding(HTMLTools.removeHtmlTags(outline)).trim();
    }

    private static String parseTagline(String xml) {
        int startTag = xml.indexOf("<h4 class=\"inline\">Tagline" + HTML_H4_END);
        if (startTag != -1) {
            // We need to work out which of the two formats to use, this is dependent on which comes first "<span" or "</div"
            String endMarker;
            if (StringUtils.indexOf(xml, "<span", startTag) < StringUtils.indexOf(xml, HTML_DIV_END, startTag)) {
                endMarker = "<span";
            } else {
                endMarker = HTML_DIV_END;
            }

            // Now look for the right string
            String tagline = HTMLTools.extractTag(xml, "<h4 class=\"inline\">Tagline" + HTML_H4_END, endMarker);
            tagline = HTMLTools.stripTags(tagline);
            return cleanStringEnding(tagline);
        }
        return null;
    }

    private static String parseQuote(String xml) {
        for (String quote : HTMLTools.extractTags(xml, "<h4>Quotes</h4>", "<span class=\"", "<br", "<br")) {
            if (quote != null) {
                quote = HTMLTools.stripTags(quote);
                return cleanStringEnding(quote);
            }
        }
        return null;
    }

    private static String parseTitle(String xml) {
        String title = HTMLTools.extractTag(xml, "<span class=\"itemprop\" itemprop=\"name\">", "</span>");
        return StringUtils.trimToNull(title);
    }

    private static String parseOriginalTitle(String xml) {
        String originalTitle = HTMLTools.extractTag(xml, "<span class=\"title-extra\">", "</span>");
        StringUtils.remove(originalTitle, "<i>(original title)</i>");
        StringUtils.remove(originalTitle, "\"");
        return StringUtils.trimToNull(originalTitle);
    }

    private static Date parseFirstAiredDate(String xml) {
        String date = HTMLTools.extractTag(xml, "<span class=\"nobr\">", "</span>");
        date = StringUtils.remove(date, "(");
        date = StringUtils.remove(date, ")");
        date = StringUtils.trimToNull(date);
        return MetadataTools.parseToDate(date);
    }

    private static int parseYear(String xml) {
        String date = HTMLTools.extractTag(xml, "<span class=\"nobr\">", "</span>");
        date = StringUtils.remove(date, "(");
        date = StringUtils.remove(date, ")");
        date = StringUtils.trimToNull(date);
        return MetadataTools.extractYearAsInt(date);
    }

    private static void parseYears(String xml, Series series) {
        String years = HTMLTools.extractTag(xml, "<span class=\"nobr\">", "</span>");
        years = StringUtils.remove(years, "(");
        years = StringUtils.remove(years, ")");
        years = StringUtils.trimToEmpty(years);
        String[] parts = years.split("-");
        if (parts.length > 0) {
            series.setStartYear(MetadataTools.extractYearAsInt(parts[0]), SCANNER_ID);
            if (parts.length > 1) {
                series.setEndYear(MetadataTools.extractYearAsInt(parts[1]), SCANNER_ID);
            }
        }
    }

    private static Set<String> parseCountries(String xml) {
        Set<String> countryNames = new LinkedHashSet<>();
        for (String country : HTMLTools.extractTags(xml, "Country" + HTML_H4_END, HTML_DIV_END, "<a href=\"", HTML_A_END)) {
            String countryName = HTMLTools.removeHtmlTags(country);
            if (StringUtils.isNotBlank(countryName)) {
                countryNames.add(countryName.trim());
            }
        }
        return countryNames;
    }

    private Set<String> parseStudios(String imdbId) {
        Set<String> studios = new LinkedHashSet<>();
        try {
            DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId, "companycredits"), charset);
            if (ResponseTools.isNotOK(response)) {
                LOG.warn("Requesting studios failed with status {}: {}", response.getStatusCode(), imdbId);
            } else {
                List<String> tags = HTMLTools.extractTags(response.getContent(), "Production Companies</h4>", "</ul>", HTML_A_START, HTML_A_END);
                for (String tag : tags) {
                    studios.add(HTMLTools.removeHtmlTags(tag));
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed to retrieve studios: " + imdbId, ex);
        }
        return studios;
    }

    private Map<String, String> parseCertifications(String imdbId) {
        Map<String, String> certificationInfos = new HashMap<>();

        try {
            DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId, "parentalguide#certification"), charset);
            if (ResponseTools.isNotOK(response)) {
                LOG.warn("Requesting certifications failed with status {}: {}", response.getStatusCode(), imdbId);
            } else {
                if (this.configServiceWrapper.getBooleanProperty("yamj3.certification.mpaa", false)) {
                    String mpaa = HTMLTools.extractTag(response.getContent(), "<h5><a href=\"/mpaa\">MPAA</a>:</h5>", 1);
                    if (StringUtils.isNotBlank(mpaa)) {
                        String key = "Rated ";
                        int pos = mpaa.indexOf(key);
                        if (pos != -1) {
                            int start = key.length();
                            pos = mpaa.indexOf(" on appeal for ", start);
                            if (pos == -1) {
                                pos = mpaa.indexOf(" for ", start);
                            }
                            if (pos != -1) {
                                certificationInfos.put("MPAA", mpaa.substring(start, pos));
                            }
                        }
                    }
                }

                List<String> countries = this.configServiceWrapper.getCertificationCountries();
                if (CollectionUtils.isNotEmpty(countries)) {
                    List<String> tags = HTMLTools.extractTags(response.getContent(), HTML_H5_START + "Certification" + HTML_H5_END, HTML_DIV_END,
                            "<a href=\"/search/title?certificates=", HTML_A_END);
                    for (String country : countries) {
                        String certificate = getPreferredValue(tags, true, country);
                        certificationInfos.put(country, certificate);
                    }
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed to retrieve certifications: " + imdbId, ex);
        }
        return certificationInfos;
    }

    private Set<AwardDTO> parseAwards(String imdbId) {
        HashSet<AwardDTO> awards = new HashSet<>();
  
        try {
            DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId, "awards"), charset);
            if (ResponseTools.isNotOK(response)) {
                LOG.warn("Requesting certifications failed with status {}: {}", response.getStatusCode(), imdbId);
            } else if (response.getContent().contains("<h1 class=\"header\">Awards</h1>")) {
                List<String> awardBlocks = HTMLTools.extractTags(response.getContent(), "<h1 class=\"header\">Awards</h1>", "<div class=\"article\"", "<h3>", "</table>", false);

                for (String awardBlock : awardBlocks) {
                    //String realEvent = awardBlock.substring(0, awardBlock.indexOf('<')).trim();
                    String event = StringUtils.trimToEmpty(HTMLTools.extractTag(awardBlock, "<span class=\"award_category\">", "</span>"));
  
                    String tmpString = HTMLTools.extractTag(awardBlock, "<a href=", HTML_A_END).trim();
                    tmpString = tmpString.substring(tmpString.indexOf('>') + 1).trim();
                    int year = NumberUtils.isNumber(tmpString) ? Integer.parseInt(tmpString) : -1;
  
                    boolean awardWon = true;
                    for (String outcomeBlock : HTMLTools.extractHtmlTags(awardBlock, "<table class=", null, "<tr>", "</tr>")) {
                        String outcome = HTMLTools.extractTag(outcomeBlock, "<b>", "</b>");
                        
                        if (StringUtils.isNotBlank(outcome)) {
                            awardWon = outcome.equalsIgnoreCase("won");
                        }
                        
                        String category = StringUtils.trimToEmpty(HTMLTools.extractTag(outcomeBlock, "<td class=\"award_description\">", "<br />"));
                        // Check to see if there was a missing title and just the name in the result
                        if (category.contains("href=\"/name/")) {
                            category = StringUtils.trimToEmpty(HTMLTools.extractTag(outcomeBlock, "<span class=\"award_category\">", "</span>"));
                        }
                        
                        awards.add(new AwardDTO(event, category, year).setWon(awardWon).setNominated(!awardWon));
                    }
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed to retrieve awards: " + imdbId, ex);
        }
        return awards;
    }

    /**
     * Remove the "see more" or "more" values from the end of a string
     *
     * @param uncleanString
     * @return
     */
    private static String cleanStringEnding(String uncleanString) {
        int pos = uncleanString.indexOf("more");
        // First let's check if "more" exists in the string
        if (pos > 0) {
            if (uncleanString.endsWith("more")) {
                return uncleanString.substring(0, uncleanString.length() - 4).trim();
            }

            pos = uncleanString.toLowerCase().indexOf("see more");
            if (pos > 0) {
                return uncleanString.substring(0, pos).trim();
            }
        }

        pos = uncleanString.toLowerCase().indexOf("see full summary");
        if (pos > 0) {
            return uncleanString.substring(0, pos).trim();
        }

        return uncleanString.trim();
    }

    private static String getPreferredValue(List<String> values, boolean useLast, String preferredCountry) {
        String value = null;

        if (useLast) {
            Collections.reverse(values);
        }

        for (String text : values) {
            String country = null;

            int pos = text.indexOf(':');
            if (pos != -1) {
                country = text.substring(0, pos);
                text = text.substring(pos + 1);
            }
            pos = text.indexOf('(');
            if (pos != -1) {
                text = text.substring(0, pos).trim();
            }

            if (country == null) {
                if (StringUtils.isEmpty(value)) {
                    value = text;
                }
            } else if (country.equals(preferredCountry)) {
                value = text;
                // No need to continue scanning
                break;
            }
        }
        return HTMLTools.stripTags(value);
    }

    /**
     * Create a map of the AKA values
     *
     * @param list
     * @return
     */
    private static Map<String, String> buildAkaMap(List<String> list) {
        Map<String, String> map = new LinkedHashMap<>();
        int i = 0;
        do {
            try {
                String key = list.get(i++);
                String value = list.get(i++);
                map.put(key, value);
            } catch (Exception ignore) {
                i = -1;
            }
        } while (i != -1);
        return map;
    }

    private void parseCastCrew(VideoData videoData, String imdbId) {
        try {
            DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId, "fullcredits"), charset);
            if (ResponseTools.isNotOK(response)) {
                LOG.warn("Requesting full credits failed with status {}: {}", response.getStatusCode(), imdbId);
                return;
            }
            final String xml = response.getContent();
            
            // DIRECTORS
            if (this.configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
                for (String creditsMatch : "Directed by|Director".split(HTML_SLASH_PIPE)) {
                    parseCredits(videoData, JobType.DIRECTOR, xml, creditsMatch + "&nbsp;</h4>");
                }
            }

            // WRITERS
            if (this.configServiceWrapper.isCastScanEnabled(JobType.WRITER)) {
                for (String creditsMatch : "Writing Credits|Writer".split(HTML_SLASH_PIPE)) {
                    parseCredits(videoData, JobType.WRITER, xml, creditsMatch);
                }
            }

            // ACTORS
            if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
                boolean skipFaceless = configServiceWrapper.getBooleanProperty("imdb.skip.faceless", Boolean.FALSE);
                for (String actorBlock : HTMLTools.extractTags(xml, "<table class=\"cast_list\">", HTML_TABLE_END, "<td class=\"primary_photo\"", "</tr>")) {
                    // skip faceless persons ('loadlate' is present for actors with photos)
                    if (skipFaceless && !actorBlock.contains("loadlate")) {
                        continue;
                    }

                    int nmPosition = actorBlock.indexOf("/nm");
                    String personId = actorBlock.substring(nmPosition + 1, actorBlock.indexOf("/", nmPosition + 1));
                    String name = HTMLTools.stripTags(HTMLTools.extractTag(actorBlock, "itemprop=\"name\">", HTML_SPAN_END));
                    String character = HTMLTools.stripTags(HTMLTools.extractTag(actorBlock, "<td class=\"character\">", HTML_TD_END));

                    if (StringUtils.isNotBlank(name) && StringUtils.indexOf(character, "uncredited") == -1) {
                        character = MetadataTools.fixActorRole(character);
                        CreditDTO creditDTO = new CreditDTO(SCANNER_ID, JobType.ACTOR, name, character, personId);
                        videoData.addCreditDTO(creditDTO);
                    }
                }
            }

            // CAMERA
            if (this.configServiceWrapper.isCastScanEnabled(JobType.CAMERA)) {
                for (String creditsMatch : "Cinematography by".split(HTML_SLASH_PIPE)) {
                    parseCredits(videoData, JobType.CAMERA, xml, creditsMatch);
                }
            }

            // PRODUCERS
            if (this.configServiceWrapper.isCastScanEnabled(JobType.PRODUCER)) {
                for (String creditsMatch : "Produced by|Casting By|Casting by".split(HTML_SLASH_PIPE)) {
                    parseCredits(videoData, JobType.PRODUCER, xml, creditsMatch);
                }
            }

            // SOUND
            if (this.configServiceWrapper.isCastScanEnabled(JobType.SOUND)) {
                for (String creditsMatch : "Music by".split(HTML_SLASH_PIPE)) {
                    parseCredits(videoData, JobType.SOUND, xml, creditsMatch);
                }
            }

            // ART
            if (this.configServiceWrapper.isCastScanEnabled(JobType.ART)) {
                for (String creditsMatch : "Production Design by|Art Direction by|Set Decoration by".split(HTML_SLASH_PIPE)) {
                    parseCredits(videoData, JobType.ART, xml, creditsMatch);
                }
            }

            // EDITING
            if (this.configServiceWrapper.isCastScanEnabled(JobType.EDITING)) {
                for (String creditsMatch : "Film Editing by".split(HTML_SLASH_PIPE)) {
                    parseCredits(videoData, JobType.EDITING, xml, creditsMatch);
                }
            }

            // COSTUME_MAKEUP
            if (this.configServiceWrapper.isCastScanEnabled(JobType.COSTUME_MAKEUP)) {
                for (String creditsMatch : "Costume Design by".split(HTML_SLASH_PIPE)) {
                    parseCredits(videoData, JobType.COSTUME_MAKEUP, xml, creditsMatch);
                }
            }

        } catch (Exception ex) {
            LOG.error("Failed to scan cast crew: " + imdbId, ex);
        }
    }

    private static void parseCredits(VideoData videoData, JobType jobType, String xml, String creditsMatch) {
        if (StringUtils.indexOf(xml, HTML_GT + creditsMatch) > 0) {
            for (String member : HTMLTools.extractTags(xml, HTML_GT + creditsMatch, HTML_TABLE_END, HTML_A_START, HTML_A_END, Boolean.FALSE)) {
                int beginIndex = member.indexOf("href=\"/name/");
                if (beginIndex > -1) {
                    String personId = member.substring(beginIndex + 12, member.indexOf("/", beginIndex + 12));
                    String name = StringUtils.trimToEmpty(member.substring(member.indexOf(HTML_GT, beginIndex) + 1));
                    if (!name.contains("more credit") && StringUtils.containsNone(name, "<>:/")) {
                        CreditDTO creditDTO = new CreditDTO(SCANNER_ID, jobType, name, null, personId);
                        videoData.addCreditDTO(creditDTO);
                    }
                }
            }
        }
    }

    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        return scanImdbID(nfoContent, dto, ignorePresentId);
    }

    public static boolean scanImdbID(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(SCANNER_ID))) {
            return Boolean.TRUE;
        }

        LOG.trace("Scanning NFO for IMDb ID");

        try {
            int beginIndex = nfoContent.indexOf("/tt");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(nfoContent.substring(beginIndex + 1), "/ \n,:!&Ã©\"'(--Ã¨_Ã§Ã )=$");
                String sourceId = st.nextToken();
                LOG.debug("IMDb ID found in NFO: {}", sourceId);
                dto.addId(SCANNER_ID, sourceId);
                return Boolean.TRUE;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        try {
            int beginIndex = nfoContent.indexOf("/Title?");
            if (beginIndex != -1 && beginIndex + 7 < nfoContent.length()) {
                StringTokenizer st = new StringTokenizer(nfoContent.substring(beginIndex + 7), "/ \n,:!&Ã©\"'(--Ã¨_Ã§Ã )=$");
                String sourceId = "tt" + st.nextToken();
                LOG.debug("IMDb ID found in NFO: {}", sourceId);
                dto.addId(SCANNER_ID, sourceId);
                return Boolean.TRUE;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        LOG.debug("No IMDb ID found in NFO");
        return Boolean.FALSE;
    }

    @Override
    public String getPersonId(Person person) {
        return getPersonId(person, false);
    }

    private String getPersonId(Person person, boolean throwTempError) {
        String imdbId = person.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNotBlank(imdbId)) {
            return imdbId;
        }
        if (StringUtils.isNotBlank(person.getName())) {
            imdbId = this.imdbSearchEngine.getImdbPersonId(person.getName(), throwTempError);
            person.setSourceDbId(SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public ScanResult scan(Person person) {
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("imdb.throwError.tempUnavailable", Boolean.TRUE);

            String imdbId = getPersonId(person, throwTempError);
            
            if (StringUtils.isBlank(imdbId)) {
                LOG.debug("IMDb id not available: {}", person.getName());
                return ScanResult.MISSING_ID;
            }

            LOG.debug("IMDb id available ({}), updating person", imdbId);
            return updatePerson(person, imdbId, throwTempError);
            
        } catch (TemporaryUnavailableException tue) {
            // check retry
            int maxRetries = this.configServiceWrapper.getIntProperty("imdb.maxRetries.person", 0);
            if (person.getRetries() < maxRetries) {
                LOG.info("IMDb service temporary not available; trigger retry: '{}'", person.getName());
                return ScanResult.RETRY;
            }
            LOG.warn("IMDb service temporary not available; no retry: '{}'", person.getName());
            return ScanResult.ERROR;
            
        } catch (IOException ioe) {
            LOG.error("IMDb service error: '" + person.getName() + "'", ioe);
            return ScanResult.ERROR;
        }
    }

    private ScanResult updatePerson(Person person, String imdbId, boolean throwTempError) throws IOException {
        DigestedResponse response = httpClient.requestContent(HTML_SITE_FULL + HTML_NAME + imdbId + "/", charset);
        if (throwTempError && ResponseTools.isTemporaryError(response)) {
            throw new TemporaryUnavailableException("IMDb service is temporary not available: " + response.getStatusCode());
        } else if (ResponseTools.isNotOK(response)) {
            throw new OnlineScannerException("IMDb request failed: " + response.getStatusCode());
        }

        final String xml = response.getContent();
        
        if (OverrideTools.checkOverwriteName(person, SCANNER_ID)) {
            // We can work out if this is the new site by looking for " - IMDb" at the end of the title
            String title = HTMLTools.extractTag(xml, "<title>");
            // Check for the new version and correct the title if found.
            if (title.toLowerCase().endsWith(" - imdb")) {
                title = title.substring(0, title.length() - 7);
            }
            if (title.toLowerCase().startsWith("imdb - ")) {
                title = title.substring(7);
            }

            person.setName(title, SCANNER_ID);
        }

        if (xml.contains("id=\"img_primary\"")) {
            LOG.trace("Looking for image on webpage for {}", person.getName());
            String photoURL = HTMLTools.extractTag(xml, "id=\"img_primary\"", HTML_TD_END);
            if (photoURL.contains("http://ia.media-imdb.com/images")) {
                photoURL = "http://ia.media-imdb.com/images" + HTMLTools.extractTag(photoURL, "src=\"http://ia.media-imdb.com/images", "\"");
                if (StringUtils.isNotBlank(photoURL)) {
                    person.addPhotoURL(photoURL, SCANNER_ID);
                }
            }
        } else {
            LOG.trace("No image found on webpage for {}", person.getName());
        }

        response = httpClient.requestContent(HTML_SITE_FULL + HTML_NAME + imdbId + "/bio", charset);
        if (throwTempError && ResponseTools.isTemporaryError(response)) {
            throw new TemporaryUnavailableException("IMDb service is temporary not available: " + response.getStatusCode());
        } else if (ResponseTools.isNotOK(response)) {
            throw new OnlineScannerException("IMDb biography request failed: " + response.getStatusCode());
        }

        int endIndex;
        int beginIndex;
        final String bio = response.getContent();
        
        if (OverrideTools.checkOverwriteBirthDay(person, SCANNER_ID)) {
            beginIndex = bio.indexOf(">Date of Birth</td>");
            if (beginIndex > -1) {
                StringBuilder date = new StringBuilder();
                endIndex = bio.indexOf(">Date of Death</td>");
                beginIndex = bio.indexOf("birth_monthday=", beginIndex);
                if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                    Matcher m = PATTERN_PERSON_DOB.matcher(bio.substring(beginIndex + 15, beginIndex + 20));
                    if (m.find()) {
                        date.append(m.group(2)).append("-").append(m.group(1));
                    }
                }

                beginIndex = bio.indexOf("birth_year=", beginIndex);
                if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                    if (date.length() > 0) {
                        date.append("-");
                    }
                    date.append(bio.substring(beginIndex + 11, beginIndex + 15));
                }

                person.setBirthDay(MetadataTools.parseToDate(date.toString()), SCANNER_ID);
            }
        }

        if (OverrideTools.checkOverwriteBirthPlace(person, SCANNER_ID)) {
            beginIndex = bio.indexOf(">Date of Birth</td>");
            if (beginIndex > -1) {
                beginIndex = bio.indexOf("birth_place=", beginIndex);
                String place;
                if (beginIndex > -1) {
                    place = HTMLTools.extractTag(bio, "birth_place=", HTML_A_END);
                    int start = place.indexOf('>');
                    if (start > -1 && start < place.length()) {
                        place = place.substring(start + 1);
                    }
                    person.setBirthPlace(place, SCANNER_ID);
                }
            }
        }

        if (OverrideTools.checkOverwriteDeathDay(person, SCANNER_ID)) {
            beginIndex = bio.indexOf(">Date of Death</td>");
            if (beginIndex > -1) {
                StringBuilder date = new StringBuilder();
                endIndex = bio.indexOf(">Mini Bio (1)</h4>", beginIndex);
                beginIndex = bio.indexOf("death_monthday=", beginIndex);
                if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                    Matcher m = PATTERN_PERSON_DOB.matcher(bio.substring(beginIndex + 15, beginIndex + 20));
                    if (m.find()) {
                        date.append(m.group(2));
                        date.append("-");
                        date.append(m.group(1));
                    }
                }
                beginIndex = bio.indexOf("death_date=", beginIndex);
                if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                    if (date.length() > 0) {
                        date.append("-");
                    }
                    date.append(bio.substring(beginIndex + 11, beginIndex + 15));
                }
                person.setDeathDay(MetadataTools.parseToDate(date.toString()), SCANNER_ID);
            }
        }

        if (OverrideTools.checkOverwriteBirthName(person, SCANNER_ID)) {
            beginIndex = bio.indexOf(">Birth Name</td>");
            if (beginIndex > -1) {
                beginIndex += 20;
                String name = bio.substring(beginIndex, bio.indexOf(HTML_TD_END, beginIndex));
                person.setBirthName(HTMLTools.decodeHtml(name), SCANNER_ID);
            }
        }

        if (OverrideTools.checkOverwriteBiography(person, SCANNER_ID)) {
            if (bio.contains(">Mini Bio (1)</h4>")) {
                String biography = HTMLTools.extractTag(bio, ">Mini Bio (1)</h4>", "<em>- IMDb Mini Biography");
                person.setBiography(HTMLTools.removeHtmlTags(biography), SCANNER_ID);
            }
        }

        return ScanResult.OK;
    }
}
