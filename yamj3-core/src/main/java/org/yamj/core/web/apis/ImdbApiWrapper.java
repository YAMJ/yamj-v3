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

import static org.yamj.plugin.api.service.Constants.SOURCE_IMDB;
import static org.yamj.plugin.api.service.Constants.UTF8;

import com.omertron.imdbapi.ImdbApi;
import com.omertron.imdbapi.ImdbException;
import com.omertron.imdbapi.model.*;
import java.io.IOException;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.api.common.tools.ResponseTools;
import org.yamj.core.CachingNames;
import org.yamj.core.config.ConfigService;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.dto.AwardDTO;
import org.yamj.plugin.api.metadata.MetadataTools;
import org.yamj.plugin.api.web.HTMLTools;
import org.yamj.plugin.api.web.TemporaryUnavailableException;

@Service
public class ImdbApiWrapper {
    
    private static final Logger LOG = LoggerFactory.getLogger(ImdbApiWrapper.class);
    private static final String API_ERROR = "IMDb error";
    private static final String HTML_SITE_FULL = "http://www.imdb.com/";
    private static final String HTML_TITLE = "title/";
    private static final String HTML_A_END = "</a>";
    private static final String HTML_A_START = "<a ";
    private static final String HTML_H5_END = ":</h5>";
    private static final String HTML_H5_START = "<h5>";
    private static final String HTML_DIV_END = "</div>";

    @Autowired
    private ConfigService configService;
    @Autowired
    private ImdbApi imdbApi;
    @Autowired
    private PoolingHttpClient httpClient;
    @Autowired
    private LocaleService localeService;

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

    public ImdbMovieDetails getMovieDetails(String imdbId, Locale locale, boolean throwTempError) {
        ImdbMovieDetails movieDetails = null;
        try {
            movieDetails = imdbApi.getFullDetails(imdbId, locale);
        } catch (ImdbException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get movie details using IMDb ID {}: {}", imdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return movieDetails;
    }
        
    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId}")
    public String getMovieDetailsXML(final String imdbId, boolean throwTempError) throws IOException {
        DigestedResponse response;
        try {
            response = httpClient.requestContent(getImdbUrl(imdbId), UTF8);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("IMDb request failed", ex);
        }

        checkTempError(throwTempError, response);
        return response.getContent();
    }
    
    public List<ImdbCredit> getFullCast(String imdbId) {
        List<ImdbCredit> fullCast = null;
        try {
            // use US locale to check for uncredited cast
            fullCast = imdbApi.getFullCast(imdbId, Locale.US);
        } catch (ImdbException ex) {
            LOG.error("Failed to get full cast using IMDb ID {}: {}", imdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return fullCast;
    }

    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId, #locale}", unless="#result==null")
    public ImdbPerson getPerson(String imdbId, Locale locale, boolean throwTempError) {
        ImdbPerson imdbPerson = null;
        try {
            imdbPerson = imdbApi.getActorDetails(imdbId, locale);
        } catch (ImdbException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get movie details using IMDb ID {}: {}", imdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return imdbPerson;
    }

    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName}", unless="#result==null")
    public Map<String,Integer> getTop250(Locale locale, boolean throwTempError) {
        try {
            Map<String,Integer> result = new HashMap<>();
            int rank = 0;
            for (ImdbList imdbList : imdbApi.getTop250(locale)) {
                rank++;
                if (StringUtils.isNotBlank(imdbList.getImdbId())) {
                    result.put(imdbList.getImdbId(), Integer.valueOf(rank));
                }
            }
            return result;
        } catch (ImdbException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get Top250: {}", ex.getMessage());
            LOG.trace(API_ERROR, ex);
            return null;
        }
    }
    
    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId}")
    public List<ImdbImage> getTitlePhotos(String imdbId, Locale locale) {
        List<ImdbImage> titlePhotos = null;
        try {
            titlePhotos = imdbApi.getTitlePhotos(imdbId, locale);
        } catch (ImdbException ex) {
            LOG.error("Failed to get title photos using IMDb ID {}: {}", imdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return (titlePhotos == null ? new ArrayList<ImdbImage>(0) : titlePhotos);
    }

    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId, #locale}", unless="#result==null")
    public Map<Integer,List<ImdbEpisodeDTO>> getTitleEpisodes(String imdbId, Locale locale) {
        List<ImdbSeason> seasons = null;
        try {
            seasons = imdbApi.getTitleEpisodes(imdbId, locale);
        } catch (ImdbException ex) {
            LOG.error("Failed to get title episodes using IMDb ID {}: {}", imdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        
        // if nothing found, then return nothing
        if (seasons == null) {
            return null;
        }

        Map<Integer,List<ImdbEpisodeDTO>> result = new HashMap<>();
        for (ImdbSeason season : seasons) {
            if (StringUtils.isNumeric(season.getToken())) {
                Integer seasonId = Integer.valueOf(season.getToken());
                List<ImdbEpisodeDTO> episodes = new ArrayList<>();
                int episodeCounter = 0;
                for (ImdbMovie movie : season.getEpisodes()) {
                    ImdbEpisodeDTO episode = new ImdbEpisodeDTO();
                    episode.setEpisode(++episodeCounter);
                    episode.setImdbId(movie.getImdbId());
                    episode.setTitle(movie.getTitle());
                    episode.setYear(movie.getYear());
                    episode.setReleaseCountry(locale.getCountry());
                    episode.setReleaseDate(MetadataTools.parseToDate(movie.getReleaseDate()));
                    episodes.add(episode);
                }
                result.put(seasonId, episodes);
            }
        }
        return result;
    }

    public String getReleasInfoXML(final String imdbId) {
        String webpage = null;
        try {
            final DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId, "releaseinfo"), UTF8);
            if (ResponseTools.isOK(response)) {
                webpage = response.getContent();
            } else {
                LOG.warn("Requesting release infos failed with status {}: {}", response.getStatusCode(), imdbId);
            }
        } catch (Exception ex) {
            LOG.error("Requesting release infos failed: " + imdbId, ex);
        }
        return webpage;
    }

    public String getPersonBioXML(final String imdbId, boolean throwTempError) throws IOException {
        DigestedResponse response;
        try {
            response = httpClient.requestContent(HTML_SITE_FULL + "name/" + imdbId + "/bio", UTF8);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("IMDb request failed", ex);
        }

        checkTempError(throwTempError, response);
        return response.getContent();
    }

    public Set<String> getProductionStudios(String imdbId) {
        Set<String> studios = new LinkedHashSet<>();
        try {
            DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId, "companycredits"), UTF8);
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

    public Map<String, String> getCertifications(String imdbId, Locale imdbLocale, ImdbMovieDetails movieDetails) {
        Map<String, String> certifications = new HashMap<>();
        
        // get certificate from IMDb API movie details
        String certificate = movieDetails.getCertificate().get("certificate");
        if (StringUtils.isNotBlank(certificate)) {
            String country = movieDetails.getCertificate().get("country");
            if (StringUtils.isBlank(country)) {
                certifications.put(imdbLocale.getCountry(), certificate);
            }
        }
        
        try {
            DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId, "parentalguide#certification"), UTF8);
            if (ResponseTools.isNotOK(response)) {
                LOG.warn("Requesting certifications failed with status {}: {}", response.getStatusCode(), imdbId);
            } else {
                if (this.configService.getBooleanProperty("yamj3.certification.mpaa", false)) {
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
                                certifications.put("MPAA", mpaa.substring(start, pos));
                            }
                        }
                    }
                }

                List<String> tags = HTMLTools.extractTags(response.getContent(), HTML_H5_START + "Certification" + HTML_H5_END, HTML_DIV_END,
                                "<a href=\"/search/title?certificates=", HTML_A_END);
                Collections.reverse(tags);
                for (String countryCode : localeService.getCertificationCountryCodes(imdbLocale)) {
                    loop: for (String country : localeService.getCountryNames(countryCode)) {
                        certificate = getPreferredValue(tags, country);
                        if (StringUtils.isNotBlank(certificate)) {
                            certifications.put(countryCode, certificate);
                            break loop;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed to retrieve certifications: " + imdbId, ex);
        }
        return certifications;
    }


    private static String getPreferredValue(List<String> tags, String preferredCountry) {
        String value = null;

        for (String text : tags) {
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

    public Set<AwardDTO> getAwards(String imdbId) {
        HashSet<AwardDTO> awards = new HashSet<>();
        
        try {
            DigestedResponse response = httpClient.requestContent(ImdbApiWrapper.getImdbUrl(imdbId, "awards"), UTF8);
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
                        
                        awards.add(new AwardDTO(SOURCE_IMDB, event, category, year).setWon(awardWon).setNominated(!awardWon));
                    }
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed to retrieve awards: " + imdbId, ex);
        }
        return awards;
    }

    private static void checkTempError(boolean throwTempError, DigestedResponse response) {
        if (throwTempError && ResponseTools.isTemporaryError(response)) {
            throw new TemporaryUnavailableException("IMDb service is temporary not available: " + response.getStatusCode());
        } else if (ResponseTools.isNotOK(response)) {
            throw new RuntimeException("IMDb request failed: " + response.getStatusCode());
        }
    }

    private static void checkTempError(boolean throwTempError, ImdbException ex) {
        if (throwTempError && ResponseTools.isTemporaryError(ex)) {
            throw new TemporaryUnavailableException("IMDb service temporary not available: " + ex.getResponseCode(), ex);
        }
    }
}   
