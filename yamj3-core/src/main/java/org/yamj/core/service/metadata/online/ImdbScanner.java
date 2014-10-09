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
package org.yamj.core.service.metadata.online;

import com.moviejukebox.allocine.model.Movie;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.StringTools;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.model.AbstractMetadata;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.service.metadata.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.tools.web.HTMLTools;
import org.yamj.core.tools.web.PoolingHttpClient;

@Service("imdbScanner")
public class ImdbScanner implements IMovieScanner, ISeriesScanner, InitializingBean {

    public static final String SCANNER_ID = "imdb";
    private static final Logger LOG = LoggerFactory.getLogger(ImdbScanner.class);
    private static final String HTML_H5_END = ":</h5>";
    private static final String HTML_H5_START = "<h5>";
    private static final String HTML_DIV_END = "</div>";
    private static final String HTML_A_END = "</a>";
    private static final String HTML_A_START = "<a ";
    private static final String HTML_SLASH_PIPE = "\\|";
    private static final String HTML_SLASH_QUOTE = "/\"";
    private static final String HTML_QUOTE_GT = "\">";
    private static final String HTML_NAME = "name/";
    private static final String HTML_H4_END = ":</h4>";
    private static final String HTML_SITE_FULL = "http://www.imdb.com/";
    private static final String HTML_TITLE = "title/";
    private static final String HTML_BREAK = "<br/>";
    private static final String HTML_SPAN_END = "</span>";
    private static final String HTML_GT = ">";
    // Patterns for the name searching
    private static final String STRING_PATTERN_NAME = "(?:.*?)/name/(nm\\d+)/(?:.*?)'name'>(.*?)</a>(?:.*?)";
    private static final String STRING_PATTERN_CHAR = "(?:.*?)/character/(ch\\d+)/(?:.*?)>(.*?)</a>(?:.*)";
    private static final Pattern PATTERN_PERSON_NAME = Pattern.compile(STRING_PATTERN_NAME, Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_PERSON_CHAR = Pattern.compile(STRING_PATTERN_CHAR, Pattern.CASE_INSENSITIVE);

    private Charset charset;

    @Autowired
    private PoolingHttpClient httpClient;
    @Autowired
    private ImdbSearchEngine imdbSearchEngine;
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ConfigService configService;

    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        charset = Charset.forName("UTF-8");
        
        // register this scanner
        onlineScannerService.registerMovieScanner(this);
        onlineScannerService.registerSeriesScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        String imdbId = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            imdbId = getMovieId(videoData.getTitle(), videoData.getPublicationYear());
            videoData.setSourceDbId(SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getSeriesId(Series series) {
        String imdbId = series.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            int year = -1; // TODO: get form firsAired value
            imdbId = getSeriesId(series.getTitle(), year);
            series.setSourceDbId(SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getMovieId(String title, int year) {
        return imdbSearchEngine.getImdbId(title, year, false);
    }

    @Override
    public String getSeriesId(String title, int year) {
        return imdbSearchEngine.getImdbId(title, year, true);
    }

    @Override
    public ScanResult scan(VideoData videoData) {
        String imdbId = getMovieId(videoData);
        if (StringUtils.isBlank(imdbId)) {
            LOG.debug("IMDb id not available : {}", videoData.getTitle());
            return ScanResult.MISSING_ID;
        }

        String xml;
        try {
            xml = httpClient.requestContent(getImdbUrl(imdbId), charset);
            
            if (xml.contains("\"tv-extra\"") || xml.contains("\"tv-series-series\"")) {
                return ScanResult.TYPE_CHANGE;
            }
        } catch (Exception ex) {
            LOG.error("Failed to get content from IMDb", ex);
            return ScanResult.ERROR;
        }

        // common update
        ScanResult scanResult = this.updateCommon(videoData, xml);
        if (!ScanResult.OK.equals(scanResult)) {
            return scanResult;
        }

        try {
            // YEAR
            if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID)) {
                videoData.setPublicationYear(parseYear(xml), SCANNER_ID);
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

            // COUNTRY
            if (OverrideTools.checkOverwriteCountry(videoData, SCANNER_ID)) {
                videoData.setCountry(parseCountry(xml), SCANNER_ID);
            }

            // STUDIOS
            if (OverrideTools.checkOverwriteStudios(videoData, SCANNER_ID)) {
                videoData.setStudioNames(parseStudios(imdbId), SCANNER_ID);
            }

            // GENRES
            if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
                videoData.setGenreNames(parseGenres(xml), SCANNER_ID);
            }

            // CERTIFICATIONS
            parseCertifications(videoData, imdbId);

        } catch (Exception ex) {
            LOG.error("Scanning error for IMDb ID " + imdbId, ex);
            return ScanResult.ERROR;
        }

        return ScanResult.OK;
    }

    @Override
    public ScanResult scan(Series series) {
        String imdbId = getSeriesId(series);
        if (StringUtils.isBlank(imdbId)) {
            LOG.debug("IMDb id not available: {}", series.getTitle());
            return ScanResult.MISSING_ID;
        }

        String xml;
        try {
            String url = getImdbUrl(imdbId);
            xml = httpClient.requestContent(url, charset);
        } catch (Exception ex) {
            LOG.error("Failed to get content from IMDb", ex);
            return ScanResult.ERROR;
        }

        // common update
        ScanResult scanResult = this.updateCommon(series, xml);
        if (!ScanResult.OK.equals(scanResult)) {
            return scanResult;
        }
        
        try {
            // TODO
        } catch (Exception ex) {
            LOG.error("Scanning error for IMDb ID " + imdbId, ex);
            return ScanResult.ERROR;
        }
        
        return ScanResult.OK;
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

    private ScanResult updateCommon(AbstractMetadata metadata, String xml) {
        String title = HTMLTools.extractTag(xml, "<title>");
        if ((metadata instanceof VideoData) && StringUtils.contains(title, "(TV Series")) {
            return ScanResult.TYPE_CHANGE;
        }

        if (StringUtils.endsWithIgnoreCase(title, " - imdb")) {
            title = title.substring(0, title.length() - 7);
        } else if (StringUtils.startsWithIgnoreCase(title, "imdb - ")) {
            title = title.substring(7);
        }

        // remove the (VG) or (V) tags from the title
        title = title.replaceAll(" \\([VG|V]\\)$", "");

        //String yearPattern = "(?i).\\((?:TV.|VIDEO.)?(\\d{4})(?:/[^\\)]+)?\\)";
        String yearPattern = "(?i).\\((?:TV.|VIDEO.)?(\\d{4})";
        Pattern pattern = Pattern.compile(yearPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(title);
        if (matcher.find()) {
            String sYear = matcher.group(1);
            if (OverrideTools.checkOverwriteYear(metadata, SCANNER_ID)) {
                if (metadata instanceof VideoData) {
                    if (StringUtils.isNumeric(sYear)) {
                        int year = Integer.parseInt(sYear);
                        ((VideoData)metadata).setPublicationYear(year, SCANNER_ID);
                    }
                } else {
                    // TODO need to set start/end? 
                    if (StringUtils.isNumeric(sYear)) {
                        int year = Integer.parseInt(sYear);
                        ((Series)metadata).setStartYear(year, SCANNER_ID);
                    }
                }
            }

            // remove the year from the title
            title = title.substring(0, title.indexOf(matcher.group(0)));
        }

        if (OverrideTools.checkOverwriteTitle(metadata, SCANNER_ID)) {
            metadata.setTitle(title, SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOriginalTitle(metadata, SCANNER_ID)) {
            String originalTitle = title;
            if (xml.indexOf("<span class=\"title-extra\">") > -1) {
                originalTitle = HTMLTools.extractTag(xml, "<span class=\"title-extra\">", "</span>");
                if (originalTitle.indexOf("(original title)") > -1) {
                    originalTitle = originalTitle.replace(" <i>(original title)</i>", "");
                } else {
                    originalTitle = title;
                }
            }
            metadata.setTitleOriginal(originalTitle, SCANNER_ID);
        }

        return ScanResult.OK;
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
        Set<String> genres = new LinkedHashSet<String>();
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

    private static int parseYear(String xml) {
        int year = -1; 
        try {
            Pattern getYear = Pattern.compile("(?:\\s*" + "\\((\\d{4})(?:/[^\\)]+)?\\)|<a href=\"/year/(\\d{4}))");
            Matcher m = getYear.matcher(xml);
    
            if (m.find()) {
                year = MetadataTools.extractYearAsInt(m.group(1));
            }
    
            // second approach
            if (year <= 0) {
                year = MetadataTools.extractYearAsInt(HTMLTools.extractTag(xml, "<a href=\"/year/", 1));
            }
            
            // third approach
            if (year <= 0) {
                String fullReleaseDate = HTMLTools.getTextAfterElem(xml, HTML_H5_START + "Original Air Date" + HTML_H5_END, 0);
                if (StringUtils.isNotBlank(fullReleaseDate)) {
                    year = MetadataTools.extractYearAsInt(fullReleaseDate.split(" ")[2]);
                }
            }
        } catch (Exception ignore) {}
        return year;
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
    
    private static String parseCountry(String xml) {
        for (String country : HTMLTools.extractTags(xml, "Country" + HTML_H4_END, HTML_DIV_END, "<a href=\"", HTML_A_END)) {
            return HTMLTools.removeHtmlTags(country);
            // TODO set more countries in movie
        }
        return null;
    }

    private Set<String> parseStudios(String imdbId) {
        Set<String> studios = new LinkedHashSet<String>();
        try {
            String xml = httpClient.requestContent(getImdbUrl(imdbId, "companycredits"), charset);
            List<String> tags = HTMLTools.extractTags(xml, "Production Companies</h4>", "</ul>", HTML_A_START, HTML_A_END);
            for (String tag : tags) {
                studios.add(HTMLTools.removeHtmlTags(tag));
            }
        } catch (Exception ex) {
            LOG.trace("Failed to retrieve company credits", ex);
        }
        return studios;
    }

    private void parseCertifications(VideoData videoData, String imdbId) {
        try {
            // use the default site definition for the certification, because the local versions don't have the parentalguide page
            String xml = httpClient.requestContent(getImdbUrl(imdbId, "parentalguide#certification"), charset);

            String mpaa = HTMLTools.extractTag(xml, "<h5><a href=\"/mpaa\">MPAA</a>:</h5>", 1);
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
                        videoData.addCertificationInfo("MPAA", mpaa.substring(start, pos));
                    }
                }
            }

            String preferredCountry = this.configService.getProperty("yamj3.scan.preferredCountry", "USA");
            String certification = getPreferredValue(HTMLTools.extractTags(xml, HTML_H5_START + "Certification" + HTML_H5_END, HTML_DIV_END,
                    "<a href=\"/search/title?certificates=", HTML_A_END), true, preferredCountry);
            videoData.addCertificationInfo(preferredCountry, certification);
        } catch (Exception ex) {
            LOG.trace("Failed to retrieve certification", ex);
        }
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
    
    private String getPreferredValue(List<String> values, boolean useLast, String preferredCountry) {
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
}