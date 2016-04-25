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
package org.yamj.core.web.apis;

import static org.yamj.plugin.api.service.Constants.ALL;

import java.io.IOException;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.api.common.tools.ResponseTools;
import org.yamj.core.config.ConfigService;
import org.yamj.core.config.LocaleService;
import org.yamj.plugin.api.web.HTMLTools;
import org.yamj.plugin.api.web.SearchEngineTools;
import org.yamj.plugin.api.web.TemporaryUnavailableException;

@Service("imdbSearchEngine")
public class ImdbSearchEngine {

    private static final Logger LOG = LoggerFactory.getLogger(ImdbSearchEngine.class);
    private static final String OBJECT_MOVIE = "movie";
    private static final String OBJECT_PERSON = "person";
    private static final String CATEGORY_MOVIE = "movie";
    private static final String CATEGORY_TV = "tv";
    private static final String SEARCH_FIRST = "first";
    private static final String SEARCH_EXACT = "exact";
    private static final String HTML_SLASH_QUOTE = "/\"";
    private static final Pattern PERSON_REGEX = Pattern.compile(Pattern.quote("<link rel=\"canonical\" href=\"http://www.imdb.com/name/(nm\\d+)/\""));
    private static final Pattern TITLE_REGEX = Pattern.compile(Pattern.quote("<link rel=\"canonical\" href=\"http://www.imdb.com/title/(tt\\d+)/\""));

    private SearchEngineTools searchEngineTools;

    @Autowired
    private PoolingHttpClient httpClient;
    @Autowired
    private ConfigService configService;
    @Autowired
    private LocaleService localeService;
    
    @PostConstruct
    public void init() {
        LOG.trace("Initialize IMDb search engine");

        Locale locale = localeService.getLocaleForConfig("imdb");
        searchEngineTools = new SearchEngineTools(httpClient, locale);
    }

    /**
     * Retrieve the IMDb matching the specified movie name and year. This routine is based on a IMDb request.
     *
     * @param title
     * @param year
     * @param isTVShow flag to indicate if the searched movie is a TV show
     * @param throwTempError flag to indicate if error should be thrown if service is temporary not available 
     * @return the IMDb id
     */
    public String getImdbId(String title, int year, boolean isTVShow, boolean throwTempError) {
        return getImdbId(title, year, (isTVShow ? CATEGORY_TV : CATEGORY_MOVIE), throwTempError);
    }

    /**
     * Retrieve the IMDb matching the specified movie name and year. This routine is based on a IMDb request.
     *
     * @param title
     * @param year
     * @param categoryType the type of the category to search within
     * @param throwTempError flag to indicate if error should be thrown if service is temporary not available 
     * @return the IMDb id
     */
    private String getImdbId(String title, int year, String categoryType, boolean throwTempError) {
        String imdbId = getImdbIdFromImdb(title, year, OBJECT_MOVIE, categoryType, throwTempError);
        if (StringUtils.isBlank(imdbId)) {
            // try with search engines
            String imdbUrl;
            if (CATEGORY_TV.equals(categoryType)) {
                // leave out the year
                imdbUrl = searchEngineTools.searchURL(title, -1, "www.imdb.com/title", throwTempError);
            } else {
                imdbUrl = searchEngineTools.searchURL(title, year, "www.imdb.com/title", throwTempError);
            }
            imdbId = getImdbIdFromURL(imdbUrl, categoryType);
        }
        return imdbId;
    }

    /**
     * @param personName
     * @param movieId
     * @return
     */
    public String getImdbPersonId(String personName, String movieId,  boolean throwTempError) {
        try {
            if (StringUtils.isNotBlank(movieId)) {
                StringBuilder sb = new StringBuilder("http://www.imdb.com/")
                    .append("search/name?name=")
                    .append(HTMLTools.encodeUrl(personName))
                    .append("&role=")
                    .append(movieId);

                LOG.debug("Querying IMDB for '{}'", sb.toString());
                DigestedResponse response = httpClient.requestContent(sb.toString());
                if (ResponseTools.isOK(response)) {
                    
                    // Check if this is an exact match (we got a person page instead of a results list)
                    Matcher personMatch = PERSON_REGEX.matcher(response.getContent());
                    if (personMatch.find()) {
                        LOG.debug("IMDb returned one match '{}'", personMatch.group(1));
                        return personMatch.group(1);
                    }
    
                    String firstPersonId = HTMLTools.extractTag(HTMLTools.extractTag(response.getContent(), "<tr class=\"even detailed\">", "</tr>"), "<a href=\"/name/", HTML_SLASH_QUOTE);
                    if (StringUtils.isNotBlank(firstPersonId)) {
                        return firstPersonId;
                    }
                } else if (throwTempError && ResponseTools.isTemporaryError(response)) {
                    throw new TemporaryUnavailableException("IMDb service temporary not available: " + response.getStatusCode());
                }
            }

            return getImdbPersonId(personName, throwTempError);
        } catch (IOException ex) {
            LOG.error("Failed retrieving IMDb Id for person '{}': {}", personName, ex.getMessage());
            LOG.trace("IMDb search error", ex);
            return null;
        }
    }

    /**
     * Get the IMDb ID for a person
     *
     * @param personName
     * @return
     */
    public String getImdbPersonId(String personName, boolean throwTempError) {
        String imdbId = getImdbIdFromImdb(personName.toLowerCase(), -1, OBJECT_PERSON, ALL, throwTempError);
        if (StringUtils.isBlank(imdbId)) {
            String imdbUrl = searchEngineTools.searchURL(personName, -1, "www.imdb.com/name", throwTempError);
            imdbId = getImdbIdFromURL(imdbUrl, OBJECT_PERSON);
        }
        return imdbId;
    }

    private static String getImdbIdFromURL(String url, String objectType) {
        if (StringUtils.isBlank(url)) {
            return null;
        }

        String imdbId = StringUtils.EMPTY;
        int beginIndex = url.indexOf(objectType.equals(OBJECT_MOVIE) ? "/title/tt" : "/name/nm");
        if (beginIndex > -1) {
            int index;
            if (objectType.equals(OBJECT_MOVIE)) {
                index = beginIndex + 7;
            } else {
                index = beginIndex + 6;
            }
            StringTokenizer st = new StringTokenizer(url.substring(index), HTML_SLASH_QUOTE);
            imdbId = st.nextToken();
        }

        if (imdbId.startsWith(objectType.equals(OBJECT_MOVIE) ? "tt" : "nm")) {
            LOG.debug("Found IMDb ID '{}'", imdbId);
            return imdbId;
        }
        return null;
    }

    /**
     * Retrieve the IMDb matching the specified movie name and year. This routine is base on a IMDb request.
     */
    private String getImdbIdFromImdb(String title, int year, String objectType, String categoryType, boolean throwTempError) {
        String searchMatch = configService.getProperty("imdb.id.search.match", "regular");

        StringBuilder sb = new StringBuilder("http://www.imdb.com/");
        sb.append("find?q=");
        sb.append(HTMLTools.encodeUrl(title));

        if (year > 0) {
            sb.append("+%28").append(year).append("%29");
        }
        sb.append("&s=");
        if (objectType.equals(OBJECT_MOVIE)) {
            sb.append("tt");
            if (categoryType.equals(CATEGORY_MOVIE)) {
                sb.append("&ttype=ft");
            } else if (categoryType.equals(CATEGORY_TV)) {
                sb.append("&ttype=tv");
            }
        } else {
            sb.append("nm");
        }
        sb.append("&site=aka");

        LOG.debug("Querying IMDb for '{}'", sb.toString());
        String xml;
        try {
            DigestedResponse response = httpClient.requestContent(sb.toString());
            if (throwTempError && ResponseTools.isTemporaryError(response)) {
                throw new TemporaryUnavailableException("IMDb service temporary not available: " + response.getStatusCode());
            } else if (ResponseTools.isNotOK(response)) {
                LOG.error("Can't find IMDb id due response status {} for '{}'", response.getStatusCode(), title);
                return null;
            }
            xml = response.getContent();
        } catch (IOException ex) {
            LOG.error("Failed retrieving IMDb Id for '{}': {}", title, ex.getMessage());
            LOG.trace("IMDb search error", ex);
            return null;
        }

        // Check if this is an exact match (we got a movie page instead of a results list)
        Pattern titleRegex;
        if (objectType.equals(OBJECT_MOVIE)) {
            titleRegex = TITLE_REGEX;
        } else {
            titleRegex = PERSON_REGEX;
        }

        Matcher titleMatch = titleRegex.matcher(xml);
        if (titleMatch.find()) {
            LOG.debug("IMDb returned one match '{}'", titleMatch.group(1));
            return titleMatch.group(1);
        }

        String searchName = HTMLTools.extractTag(HTMLTools.extractTag(xml, ";ttype=ep\">", "\"</a>.</li>"), "<b>", "</b>").toLowerCase();
        final String formattedName;
        final String formattedYear;
        final String formattedExact;

        if (SEARCH_FIRST.equalsIgnoreCase(searchMatch)) {
            // first match so nothing more to check
            formattedName = null;
            formattedYear = null;
            formattedExact = null;
        } else if (StringUtils.isNotBlank(searchName)) {
            if (year > 0 && searchName.endsWith(")") && searchName.contains("(")) {
                searchName = searchName.substring(0, searchName.lastIndexOf('(') - 1);
                formattedName = searchName.toLowerCase();
                formattedYear = "(" + year + ")";
                formattedExact = formattedName + "</a> " + formattedYear;
            } else {
                formattedName = searchName.toLowerCase();
                formattedYear = "</a>";
                formattedExact = formattedName + formattedYear;
            }
        } else {
            formattedName = HTMLTools.encodePlain(title).replace("+", " ").toLowerCase();
            if (year > 0) {
                formattedYear = "(" + year + ")";
                formattedExact = formattedName + "</a> " + formattedYear;
            } else {
                formattedYear = "</a>";
                formattedExact = formattedName + formattedYear;

            }
            searchName = formattedExact;
        }

        for (String searchResult : HTMLTools.extractTags(xml, "<table class=\"findList\">", "</table>", "<td class=\"result_text\">", "</td>", false)) {
            boolean foundMatch = false;
            if (SEARCH_FIRST.equalsIgnoreCase(searchMatch)) {
                // first result matches
                foundMatch = true;
            } else if (SEARCH_EXACT.equalsIgnoreCase(searchMatch)) {
                // exact match
                foundMatch = (searchResult.toLowerCase().contains(formattedExact));
            } else {
                // regular match: name and year match independent from each other
                int nameIndex = searchResult.toLowerCase().indexOf(formattedName);
                if (nameIndex != -1) {
                    foundMatch = (searchResult.indexOf(formattedYear) > nameIndex);
                }
            }

            if (foundMatch) {
                return HTMLTools.extractTag(searchResult, "<a href=\"" + (objectType.equals(OBJECT_MOVIE) ? "/title/" : "/name/"), "/");
            }
            for (String otherResult : HTMLTools.extractTags(searchResult, "</';\">", "</p>", "<p class=\"find-aka\">", "</em>", false)) {
                if (otherResult.toLowerCase().contains("\"" + searchName + "\"")) {
                    return HTMLTools.extractTag(searchResult, "/images/b.gif?link=" + (objectType.equals(OBJECT_MOVIE) ? "/title/" : "/name/"), "/';\">");
                }
            }
        }

        // alternate search for person ID
        if (objectType.equals(OBJECT_PERSON)) {
            String firstPersonId = HTMLTools.extractTag(HTMLTools.extractTag(xml, "<table><tr> <td valign=\"top\">", "</td></tr></table>"), "<a href=\"/name/", HTML_SLASH_QUOTE);
            if (StringUtils.isBlank(firstPersonId)) {
                // alternate approach
                int beginIndex = xml.indexOf("<a href=\"/name/nm");
                if (beginIndex > -1) {
                    StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 15), HTML_SLASH_QUOTE);
                    firstPersonId = st.nextToken();
                }
            }

            if (firstPersonId.startsWith("nm")) {
                LOG.debug("Found IMDb ID '{}'", firstPersonId);
                return firstPersonId;
            }
        }

        LOG.debug("Failed to find a match on IMDb");
        return null;
    }
}
