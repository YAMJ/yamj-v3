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

import com.omertron.imdbapi.ImdbApi;
import com.omertron.imdbapi.model.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.core.CachingNames;
import org.yamj.core.config.ConfigService;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.dto.AwardDTO;
import org.yamj.core.service.metadata.online.OnlineScannerException;
import org.yamj.core.service.metadata.online.TemporaryUnavailableException;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.web.HTMLTools;
import org.yamj.core.web.PoolingHttpClient;
import org.yamj.core.web.ResponseTools;

@Service
public class ImdbApiWrapper {
    
    private static final Logger LOG = LoggerFactory.getLogger(ImdbApiWrapper.class);
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final String HTML_SITE_FULL = "http://www.imdb.com/";
    private static final String HTML_TITLE = "title/";
    private static final String HTML_A_END = "</a>";
    private static final String HTML_A_START = "<a ";
    private static final String HTML_H5_END = ":</h5>";
    private static final String HTML_H5_START = "<h5>";
    private static final String HTML_DIV_END = "</div>";

    private final Lock imdbApiLock = new ReentrantLock(true);

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

    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId, #locale}")
    public ImdbMovieDetails getMovieDetails(String imdbId, Locale locale) {
        ImdbMovieDetails imdbMovieDetails;
        imdbApiLock.lock();
        try {
            imdbApi.setLocale(locale);
            imdbMovieDetails = imdbApi.getFullDetails(imdbId);
        } finally {
            imdbApiLock.unlock();
        }
        return (imdbMovieDetails == null ? new ImdbMovieDetails() : imdbMovieDetails);
    }
        
    public String getMovieDetailsXML(final String imdbId, boolean throwTempError) throws IOException {
        DigestedResponse response;
        try {
            response = httpClient.requestContent(getImdbUrl(imdbId), CHARSET);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OnlineScannerException("IMDb request failed", ex);
        }

        if (throwTempError && ResponseTools.isTemporaryError(response)) {
            throw new TemporaryUnavailableException("IMDb service is temporary not available: " + response.getStatusCode());
        } else if (ResponseTools.isNotOK(response)) {
            throw new OnlineScannerException("IMDb request failed: " + response.getStatusCode());
        }
        return response.getContent();
    }
    
    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId}")
    public List<ImdbCredit> getFullCast(String imdbId) {
        List<ImdbCredit> fullCast;
        imdbApiLock.lock();
        try {
            // use US locale to check for uncredited cast
            imdbApi.setLocale(Locale.US);
            fullCast = imdbApi.getFullCast(imdbId);
        } finally {
            imdbApiLock.unlock();
        }
        return (fullCast == null ? new ArrayList<ImdbCredit>() : fullCast);
    }

    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId, #locale}")
    public ImdbPerson getPerson(String imdbId, Locale locale) {
        ImdbPerson imdbPerson;
        imdbApiLock.lock();
        try {
            imdbApi.setLocale(locale);
            imdbPerson = imdbApi.getActorDetails(imdbId);
        } finally {
            imdbApiLock.unlock();
        }
        return (imdbPerson == null ? new ImdbPerson() : imdbPerson);
    }

    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId}")
    public List<ImdbImage> getTitlePhotos(String imdbId) {
        List<ImdbImage> titlePhotos = imdbApi.getTitlePhotos(imdbId);
        return (titlePhotos == null ? new ArrayList<ImdbImage>() : titlePhotos);
    }

    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId, #locale}")
    public Map<Integer,List<ImdbEpisodeDTO>> getTitleEpisodes(String imdbId, Locale locale) {
        List<ImdbSeason> seasons;
        imdbApiLock.lock();
        try {
            imdbApi.setLocale(locale);
            seasons = imdbApi.getTitleEpisodes(imdbId);
        } finally {
            imdbApiLock.unlock();
        }

        Map<Integer,List<ImdbEpisodeDTO>> result = new HashMap<>();
        for (ImdbSeason season : seasons) {
            if (StringUtils.isNumeric(season.getToken())) {
                Integer seasonId = Integer.valueOf(season.getToken());
                List<ImdbEpisodeDTO> episodes = new ArrayList<>();
                int episodeCounter = 0;
                for (ImdbMovie movie : season.getEpisodes()) {
                    if (!"tv_episode".equals(movie.getType())) {
                        continue;
                    }
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

    @Cacheable(value=CachingNames.API_IMDB, key="{#root.methodName, #imdbId}", unless="#result==null")
    public String getReleasInfoXML(final String imdbId) {
        String webpage = null;
        try {
            final DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId, "releaseinfo"), CHARSET);
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
            response = httpClient.requestContent(HTML_SITE_FULL + "name/" + imdbId + "/bio", CHARSET);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OnlineScannerException("IMDb request failed", ex);
        }

        if (throwTempError && ResponseTools.isTemporaryError(response)) {
            throw new TemporaryUnavailableException("IMDb service is temporary not available: " + response.getStatusCode());
        } else if (ResponseTools.isNotOK(response)) {
            throw new OnlineScannerException("IMDb request failed: " + response.getStatusCode());
        }
        return response.getContent();
    }

    public Set<String> getProductionStudios(String imdbId) {
        Set<String> studios = new LinkedHashSet<>();
        try {
            DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId, "companycredits"), CHARSET);
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

    public Map<String, String> getCertifications(String imdbId, Locale imdbLocale) {
        Map<String, String> certifications = new HashMap<>();
        
        try {
            DigestedResponse response = httpClient.requestContent(getImdbUrl(imdbId, "parentalguide#certification"), CHARSET);
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
                        String certificate = getPreferredValue(tags, country);
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
            DigestedResponse response = httpClient.requestContent(ImdbApiWrapper.getImdbUrl(imdbId, "awards"), CHARSET);
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
                        
                        awards.add(new AwardDTO(event, category, "imdb", year).setWon(awardWon).setNominated(!awardWon));
                    }
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed to retrieve awards: " + imdbId, ex);
        }
        return awards;
    }

}   
