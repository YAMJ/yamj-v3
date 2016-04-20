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

import static org.yamj.plugin.api.common.Constants.SOURCE_IMDB;

import com.omertron.imdbapi.model.*;
import java.io.IOException;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.web.apis.ImdbApiWrapper;
import org.yamj.core.web.apis.ImdbEpisodeDTO;
import org.yamj.core.web.apis.ImdbSearchEngine;
import org.yamj.plugin.api.metadata.tools.MetadataTools;
import org.yamj.plugin.api.metadata.tools.PersonName;
import org.yamj.plugin.api.type.JobType;
import org.yamj.plugin.api.web.HTMLTools;

@Service("imdbScanner")
public class ImdbScanner implements IMovieScanner, ISeriesScanner, IPersonScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ImdbScanner.class);
    private static final String HTML_DIV_END = "</div>";
    private static final String HTML_A_END = "</a>";
    private static final String HTML_H4_END = ":</h4>";
    private static final String HTML_TABLE_END = "</table>";
    private static final String HTML_TD_END = "</td>";
    private static final String LITERAL_NORMAL = "normal";
    
    @Autowired
    private ImdbSearchEngine imdbSearchEngine;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private ImdbApiWrapper imdbApiWrapper;
    @Autowired
    private IdentifierService identifierService;

    @Override
    public String getScannerName() {
        return SOURCE_IMDB;
    }

    @Override
    public String getMovieId(VideoData videoData) {
        return getMovieId(videoData, false);
    }

    private String getMovieId(VideoData videoData, boolean throwTempError) {
        String imdbId = videoData.getSourceDbId(SOURCE_IMDB);
        // search by title
        if (StringUtils.isBlank(imdbId)) {
            imdbId = imdbSearchEngine.getImdbId(videoData.getTitle(), videoData.getPublicationYear(), false, throwTempError);
            videoData.setSourceDbId(SOURCE_IMDB, imdbId);
        }
        // search by original title
        if (StringUtils.isBlank(imdbId) && MetadataTools.isOriginalTitleScannable(videoData.getTitle(), videoData.getTitleOriginal())) {
            imdbId = imdbSearchEngine.getImdbId(videoData.getTitleOriginal(), videoData.getPublicationYear(), false, throwTempError);
            videoData.setSourceDbId(SOURCE_IMDB, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getSeriesId(Series series) {
        return getSeriesId(series, false);
    }

    private String getSeriesId(Series series, boolean throwTempError) {
        String imdbId = series.getSourceDbId(SOURCE_IMDB);
        if (StringUtils.isBlank(imdbId)) {
            imdbId = imdbSearchEngine.getImdbId(series.getTitle(), series.getStartYear(), true, throwTempError);
            series.setSourceDbId(SOURCE_IMDB, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getPersonId(Person person) {
        return getPersonId(person, false);
    }

    private String getPersonId(Person person, boolean throwTempError) {
        String imdbId = person.getSourceDbId(SOURCE_IMDB);
        if (StringUtils.isNotBlank(imdbId)) {
            return imdbId;
        }
        if (StringUtils.isNotBlank(person.getName())) {
            imdbId = this.imdbSearchEngine.getImdbPersonId(person.getName(), throwTempError);
            person.setSourceDbId(SOURCE_IMDB, imdbId);
        }
        return imdbId;
    }
    
    @Override
    public ScanResult scanMovie(VideoData videoData, boolean throwTempError) {
        try {
            // get movie id
            String imdbId = getMovieId(videoData, throwTempError);
            if (StringUtils.isBlank(imdbId)) {
                LOG.debug("IMDb id not available : {}", videoData.getTitle());
                return ScanResult.MISSING_ID;
            }

            LOG.debug("IMDb id available ({}), updating movie", imdbId);
            return updateMovie(videoData, imdbId, throwTempError);
            
        } catch (IOException ioe) {
            LOG.error("IMDb service error: '" + videoData.getTitle() + "'", ioe);
            return ScanResult.ERROR;
        }
    }

    private ScanResult updateMovie(VideoData videoData, String imdbId, boolean throwTempError) throws IOException {
        Locale imdbLocale = localeService.getLocaleForConfig(SOURCE_IMDB);
        ImdbMovieDetails movieDetails = imdbApiWrapper.getMovieDetails(imdbId, imdbLocale, throwTempError);
        Map<String,Integer> top250 = imdbApiWrapper.getTop250(imdbLocale, throwTempError);
        
        if (movieDetails == null || StringUtils.isBlank(movieDetails.getImdbId()) || top250 == null) {
            return ScanResult.NO_RESULT;
        }

        // check type change
        if (!"feature".equals(movieDetails.getType())) {
            return ScanResult.TYPE_CHANGE;
        }
        
        // movie details XML is still needed for some parts
        final String xml = imdbApiWrapper.getMovieDetailsXML(imdbId, throwTempError);
        
        // update common values for movie and episodes
        updateCommonMovieEpisode(videoData, movieDetails, imdbId, imdbLocale);
        
        // ORIGINAL TITLE
        if (OverrideTools.checkOverwriteOriginalTitle(videoData, SOURCE_IMDB)) {
            // get header tag
            String headerXml = HTMLTools.extractTag(xml, "<h1 class=\"header\">", "</h1>");
            videoData.setTitleOriginal(parseOriginalTitle(headerXml), SOURCE_IMDB);
        }

        // YEAR
        if (OverrideTools.checkOverwriteYear(videoData, SOURCE_IMDB)) {
            videoData.setPublicationYear(movieDetails.getYear(), SOURCE_IMDB);
        }

        // TOP250
        Integer rank = top250.get(imdbId);
        if (rank != null) {
            videoData.setTopRank(rank.intValue());
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(videoData, SOURCE_IMDB)) {
            videoData.setGenreNames(movieDetails.getGenres(), SOURCE_IMDB);
        }

        // COUNTRIES
        if (OverrideTools.checkOverwriteCountries(videoData, SOURCE_IMDB)) {
            videoData.setCountryCodes(parseCountryCodes(xml), SOURCE_IMDB);
        }

        // STUDIOS
        if (OverrideTools.checkOverwriteStudios(videoData, SOURCE_IMDB)) {
            videoData.setStudioNames(imdbApiWrapper.getProductionStudios(imdbId), SOURCE_IMDB);
        }

        // RELEASE INFO
        parseReleasedTitles(videoData, imdbId, imdbLocale);

        // AWARDS
        if (configServiceWrapper.getBooleanProperty("imdb.movie.awards", false)) {
            videoData.addAwardDTOS(imdbApiWrapper.getAwards(imdbId));
        }
        
        return ScanResult.OK;
    }


    private void updateCommonMovieEpisode(VideoData videoData, ImdbMovieDetails movieDetails, String imdbId, Locale imdbLocale) {
        // TITLE
        if (OverrideTools.checkOverwriteTitle(videoData, SOURCE_IMDB)) {
            videoData.setTitle(movieDetails.getTitle(), SOURCE_IMDB);
        }

        // RELEASE DATE
        if (MapUtils.isNotEmpty(movieDetails.getReleaseDate()) && OverrideTools.checkOverwriteReleaseDate(videoData, SOURCE_IMDB)) {
            final Date releaseDate = MetadataTools.parseToDate(movieDetails.getReleaseDate().get(LITERAL_NORMAL));
            videoData.setRelease(releaseDate, SOURCE_IMDB);
        }

        // PLOT
        if (movieDetails.getBestPlot() != null && OverrideTools.checkOverwritePlot(videoData, SOURCE_IMDB)) {
            videoData.setPlot(MetadataTools.cleanPlot(movieDetails.getBestPlot().getSummary()), SOURCE_IMDB);
        }

        // OUTLINE
        if (movieDetails.getPlot() != null && OverrideTools.checkOverwriteOutline(videoData, SOURCE_IMDB)) {
            videoData.setOutline(MetadataTools.cleanPlot(movieDetails.getPlot().getOutline()), SOURCE_IMDB);
        }

        // TAGLINE
        if (OverrideTools.checkOverwriteTagline(videoData, SOURCE_IMDB)) {
            videoData.setTagline(movieDetails.getTagline(), SOURCE_IMDB);
        }

        // QUOTE
        if (movieDetails.getQuote() != null &&
            CollectionUtils.isNotEmpty(movieDetails.getQuote().getLines()) &&
            OverrideTools.checkOverwriteQuote(videoData, SOURCE_IMDB))
        {
            videoData.setQuote(MetadataTools.cleanPlot(movieDetails.getQuote().getLines().get(0).getQuote()), SOURCE_IMDB);
        }
        
        // RATING
        videoData.addRating(SOURCE_IMDB, MetadataTools.parseRating(movieDetails.getRating()));

        // CERTIFICATIONS
        videoData.setCertificationInfos(imdbApiWrapper.getCertifications(imdbId, imdbLocale, movieDetails));

        // CAST/CREW
        parseCastCrew(videoData, imdbId);
    }
    
    @Override
    public ScanResult scanSeries(Series series, boolean throwTempError) {
        try {
            // get series id
            String imdbId = getSeriesId(series, throwTempError);
            if (StringUtils.isBlank(imdbId)) {
                LOG.debug("IMDb id not available: {}", series.getIdentifier());
                return ScanResult.MISSING_ID;
            }

            LOG.debug("IMDb id available ({}), updating series", imdbId);
            return updateSeries(series, imdbId, throwTempError);
            
        } catch (IOException ioe) {
            LOG.error("IMDb service error: '" + series.getIdentifier() + "'", ioe);
            return ScanResult.ERROR;
        }
    }

    private ScanResult updateSeries(Series series, String imdbId, boolean throwTempError) throws IOException {
        Locale imdbLocale = localeService.getLocaleForConfig(SOURCE_IMDB);
        ImdbMovieDetails movieDetails = imdbApiWrapper.getMovieDetails(imdbId, imdbLocale, throwTempError);
        if (movieDetails == null || StringUtils.isBlank(movieDetails.getImdbId())) {
            return ScanResult.NO_RESULT;
        }
        
        // check type change
        if (!"tv_series".equals(movieDetails.getType())) {
            return ScanResult.TYPE_CHANGE;
        }

        // movie details XML is still needed for some parts
        final String xml = imdbApiWrapper.getMovieDetailsXML(imdbId, throwTempError);
        // get header tag
        final String headerXml = HTMLTools.extractTag(xml, "<h1 class=\"header\">", "</h1>");

        // TITLE
        final String title = movieDetails.getTitle(); 
        if (OverrideTools.checkOverwriteTitle(series, SOURCE_IMDB)) {
            series.setTitle(title, SOURCE_IMDB);
        }

        // START YEAR and END YEAR
        if (OverrideTools.checkOverwriteYear(series, SOURCE_IMDB)) {
            series.setStartYear(movieDetails.getYear(), SOURCE_IMDB);
            series.setEndYear(NumberUtils.toInt(movieDetails.getYearEnd(), -1), SOURCE_IMDB);
        }

        // PLOT
        final String plot = (movieDetails.getBestPlot() == null) ? null : MetadataTools.cleanPlot(movieDetails.getBestPlot().getSummary());
        if (OverrideTools.checkOverwritePlot(series, SOURCE_IMDB)) {
            series.setPlot(plot, SOURCE_IMDB);
        }

        // OUTLINE
        final String outline = (movieDetails.getPlot() == null) ? null : MetadataTools.cleanPlot(movieDetails.getPlot().getOutline());
        if (OverrideTools.checkOverwriteOutline(series, SOURCE_IMDB)) {
            series.setOutline(outline, SOURCE_IMDB);
        }
        
        // ORIGINAL TITLE
        final String titleOriginal = parseOriginalTitle(headerXml);
        if (OverrideTools.checkOverwriteOriginalTitle(series, SOURCE_IMDB)) {
            series.setTitleOriginal(titleOriginal, SOURCE_IMDB);
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(series, SOURCE_IMDB)) {
            series.setGenreNames(movieDetails.getGenres(), SOURCE_IMDB);
        }

        // STUDIOS
        if (OverrideTools.checkOverwriteStudios(series, SOURCE_IMDB)) {
            series.setStudioNames(imdbApiWrapper.getProductionStudios(imdbId), SOURCE_IMDB);
        }

        // COUNTRIES
        if (OverrideTools.checkOverwriteCountries(series, SOURCE_IMDB)) {
            series.setCountryCodes(parseCountryCodes(xml), SOURCE_IMDB);
        }

        // CERTIFICATIONS
        series.setCertificationInfos(imdbApiWrapper.getCertifications(imdbId, imdbLocale, movieDetails));

        // RELEASE INFO
        parseReleasedTitles(series, imdbId, imdbLocale);

        // AWARDS
        if (configServiceWrapper.getBooleanProperty("imdb.tvshow.awards", false)) {
            series.addAwardDTOS(imdbApiWrapper.getAwards(imdbId));
        }

        // scan seasons
        this.scanSeasons(series, imdbId, title, titleOriginal, plot, outline, imdbLocale);

        return ScanResult.OK;
    }

    private void scanSeasons(Series series, String imdbId, String title, String titleOriginal, String plot, String outline, Locale imdbLocale) {
        for (Season season : series.getSeasons()) {

            // get the episodes
            Map<Integer, ImdbEpisodeDTO> episodes = getEpisodes(imdbId, season.getSeason(), imdbLocale);

            if (!season.isTvSeasonDone(SOURCE_IMDB)) {

                // use values from series
                if (OverrideTools.checkOverwriteTitle(season, SOURCE_IMDB)) {
                    season.setTitle(title, SOURCE_IMDB);
                }
                if (OverrideTools.checkOverwriteOriginalTitle(season, SOURCE_IMDB)) {
                    season.setTitleOriginal(titleOriginal, SOURCE_IMDB);
                }
                if (OverrideTools.checkOverwritePlot(season, SOURCE_IMDB)) {
                    season.setPlot(plot, SOURCE_IMDB);
                }
                if (OverrideTools.checkOverwriteOutline(season, SOURCE_IMDB)) {
                    season.setOutline(outline, SOURCE_IMDB);
                }

                if (OverrideTools.checkOverwriteYear(season, SOURCE_IMDB)) {
                    Date publicationYear = null;
                    for (ImdbEpisodeDTO episode : episodes.values()) {
                        if (publicationYear == null) {
                            publicationYear = episode.getReleaseDate();
                        } else if (episode.getReleaseDate() != null && publicationYear.after(episode.getReleaseDate())) {
                            // previous episode
                            publicationYear = episode.getReleaseDate();
                        }
                    }
                    season.setPublicationYear(MetadataTools.extractYearAsInt(publicationYear), SOURCE_IMDB);
                }

                // mark season as done
                season.setTvSeasonDone();

                // scan episodes
                for (VideoData videoData : season.getVideoDatas()) {
                    this.scanEpisode(videoData, episodes, imdbLocale);
                }
            }
        }
    }

    private void scanEpisode(VideoData videoData, Map<Integer, ImdbEpisodeDTO> episodes, Locale imdbLocale) {
        if (videoData.isTvEpisodeDone(SOURCE_IMDB)) {
            // episode already done
            return;
        }
        
        ImdbEpisodeDTO dto = episodes.get(Integer.valueOf(videoData.getEpisode()));
        if (dto == null) {
            // mark episode as not found
            videoData.removeOverrideSource(SOURCE_IMDB);
            videoData.removeSourceDbId(SOURCE_IMDB);
            videoData.setTvEpisodeNotFound();
            return;
        }

        videoData.setSourceDbId(SOURCE_IMDB, dto.getImdbId());

        // set other values
        if (OverrideTools.checkOverwriteTitle(videoData, SOURCE_IMDB)) {
            videoData.setTitle(dto.getTitle(), SOURCE_IMDB);
        }
        if (OverrideTools.checkOverwriteReleaseDate(videoData, SOURCE_IMDB)) {
            videoData.setRelease(dto.getReleaseCountry(), dto.getReleaseDate(), SOURCE_IMDB);
        }

        // get movie details from IMDB
        ImdbMovieDetails movieDetails = imdbApiWrapper.getMovieDetails(dto.getImdbId(), imdbLocale, false);
        if (movieDetails == null || StringUtils.isBlank(movieDetails.getImdbId())) {
            videoData.setTvEpisodeNotFound();
            return;
        }
        
        // update common values for movie and episodes
        updateCommonMovieEpisode(videoData, movieDetails, dto.getImdbId(), imdbLocale);
        
        // ORIGINAL TITLE
        if (OverrideTools.checkOverwriteOriginalTitle(videoData, SOURCE_IMDB)) {
            // no original title present; so always get the title
            videoData.setTitleOriginal(movieDetails.getTitle(), SOURCE_IMDB);
        }
    }

    private Map<Integer, ImdbEpisodeDTO> getEpisodes(String imdbId, int season, Locale imdbLocale) {
        Map<Integer, ImdbEpisodeDTO> episodes = new HashMap<>();
        
        List<ImdbEpisodeDTO> episodeList = imdbApiWrapper.getTitleEpisodes(imdbId, imdbLocale).get(Integer.valueOf(season));
        if (episodeList != null) {
            for (ImdbEpisodeDTO episode : episodeList) {
                episodes.put(Integer.valueOf(episode.getEpisode()), episode);
            }
        }
        return episodes;
    }

    private void parseReleasedTitles(AbstractMetadata metadata, String imdbId, Locale locale) {
        
        // get the AKS
        Map<String, String> akas = getAkaMap(imdbId);
        if (MapUtils.isEmpty(akas)) {
            return;
        }
        
        // ORIGINAL TITLE
        if (OverrideTools.checkOverwriteOriginalTitle(metadata, SOURCE_IMDB)) {
            // get the AKAs from release info XML
            for (Map.Entry<String, String> aka : akas.entrySet()) {
                if (StringUtils.indexOfIgnoreCase(aka.getKey(), "original title") > 0) {
                    metadata.setTitleOriginal(aka.getValue().trim(), SOURCE_IMDB);
                    break;
                }
            }
        }

        // TITLE for preferred country from AKAS
        boolean akaScrapeTitle = configServiceWrapper.getBooleanProperty("imdb.aka.scrape.title", false);
        if (!akaScrapeTitle || !OverrideTools.checkOverwriteTitle(metadata, SOURCE_IMDB)) {
            return;
        }
        
        List<String> akaIgnoreVersions = configServiceWrapper.getPropertyAsList("imdb.aka.ignore.versions", "");

        // build countries to search for within AKA list
        Set<String> akaMatchingCountries = new TreeSet<>(localeService.getCountryNames(locale.getCountry()));
        for (String fallback : configServiceWrapper.getPropertyAsList("imdb.aka.fallback.countries", "")) {
            String countryCode = localeService.findCountryCode(fallback);
            akaMatchingCountries.addAll(localeService.getCountryNames(countryCode));
        }

        String foundValue = null;
        // NOTE: First matching country is the preferred country
        outerLoop: for (String matchCountry : akaMatchingCountries) {
            innerLoop: for (Map.Entry<String, String> aka : akas.entrySet()) {
                int startIndex = aka.getKey().indexOf(matchCountry);
                if (startIndex < 0) {
                    continue innerLoop;
                }

                String extracted = aka.getKey().substring(startIndex);
                int endIndex = extracted.indexOf('/');
                if (endIndex > -1) {
                    extracted = extracted.substring(0, endIndex);
                }

                if (isNotIgnored(extracted, akaIgnoreVersions)) {
                    foundValue = aka.getValue().trim();
                    break outerLoop;
                }
            }
        }
        metadata.setTitle(foundValue, SOURCE_IMDB);
    }
    
    private static final boolean isNotIgnored(String value, List<String> ignoreVersions) {
        for (String ignore : ignoreVersions) {
            if (StringUtils.isNotBlank(ignore) && StringUtils.containsIgnoreCase(value, ignore.trim())) {
                return false;
            }
        }
        return true;
    }
    
    private Map<String, String> getAkaMap(String imdbId) {
        String releaseInfoXML = imdbApiWrapper.getReleasInfoXML(imdbId);
        if (releaseInfoXML != null) {
            // Just extract the AKA section from the page
            List<String> akaList = HTMLTools.extractTags(releaseInfoXML, "<a id=\"akas\" name=\"akas\">", HTML_TABLE_END, "<td>", HTML_TD_END, false);
            return buildAkaMap(akaList);
        }
        return null;
    }

    private static String parseOriginalTitle(String xml) {
       return HTMLTools.extractTag(xml, "<span class=\"title-extra\">", "</span>")
                       .replaceFirst("<i>(original title)</i>", StringUtils.EMPTY)
                       .replace("\"", StringUtils.EMPTY)
                       .trim();
    }

    private Set<String> parseCountryCodes(String xml) {
        Set<String> countryCodes = new HashSet<>();
        for (String country : HTMLTools.extractTags(xml, "Country" + HTML_H4_END, HTML_DIV_END, "<a href=\"", HTML_A_END)) {
            final String countryCode = localeService.findCountryCode(HTMLTools.removeHtmlTags(country));
            if (countryCode != null) {
                countryCodes.add(countryCode);
            }
        }
        return countryCodes;
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
            } catch (Exception ignore) { //NOSONAR
                i = -1;
            }
        } while (i != -1);
        return map;
    }

    private void parseCastCrew(VideoData videoData, String imdbId) {
        List<ImdbCredit> fullCast = imdbApiWrapper.getFullCast(imdbId);
        
        if (CollectionUtils.isEmpty(fullCast)) {
            LOG.info("No cast for imdb ID: {}", imdbId);
            return;
        }

        // build jobs map
        EnumMap<JobType,List<ImdbCast>> jobs = getJobs(fullCast);
        // get configuration parameters
        boolean skipFaceless = configServiceWrapper.getBooleanProperty("yamj3.castcrew.skip.faceless", false);
        boolean skipUncredited = configServiceWrapper.getBooleanProperty("yamj3.castcrew.skip.uncredited", true);
        
        // add credits
        addCredits(videoData, JobType.DIRECTOR, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.WRITER, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.ACTOR, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.PRODUCER, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.CAMERA, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.EDITING, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.ART, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.SOUND, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.EFFECTS, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.LIGHTING, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.COSTUME_MAKEUP, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.CREW, jobs, skipUncredited, skipFaceless);
        addCredits(videoData, JobType.UNKNOWN, jobs, skipUncredited, skipFaceless);
    }

    private static EnumMap<JobType,List<ImdbCast>>  getJobs(List<ImdbCredit> credits) {
        EnumMap<JobType,List<ImdbCast>> result = new EnumMap<>(JobType.class);
        
        for (ImdbCredit credit : credits) {
            if (CollectionUtils.isEmpty(credit.getCredits())) {
                continue;
            }
            
            switch (credit.getToken()) {
                case "cast":
                    result.put(JobType.ACTOR, credit.getCredits());
                    break;
                case "writers":
                    result.put(JobType.WRITER, credit.getCredits());
                    break;
                case "directors":
                    result.put(JobType.DIRECTOR, credit.getCredits());
                    break;
                case "cinematographers":
                    result.put(JobType.CAMERA, credit.getCredits());
                    break;
                case "editors":
                    result.put(JobType.EDITING, credit.getCredits());
                    break;
                case "producers":
                case "casting_directors":
                    if (result.containsKey(JobType.PRODUCER)) {
                        result.get(JobType.PRODUCER).addAll(credit.getCredits());
                    } else {
                        result.put(JobType.PRODUCER, credit.getCredits());
                    }
                    break;
                case "music_original":
                    result.put(JobType.SOUND, credit.getCredits());
                    break;
                case "production_designers":
                case "art_directors":
                case "set_decorators":
                    if (result.containsKey(JobType.ART)) {
                        result.get(JobType.ART).addAll(credit.getCredits());
                    } else {
                        result.put(JobType.ART, credit.getCredits());
                    }
                    break;
                case "costume_designers":
                    if (result.containsKey(JobType.COSTUME_MAKEUP)) {
                        result.get(JobType.COSTUME_MAKEUP).addAll(credit.getCredits());
                    } else {
                        result.put(JobType.COSTUME_MAKEUP, credit.getCredits());
                    }
                    break;
                case "assistant_directors":
                case "production_managers":
                case "art_department":
                case "sound_department":
                case "special_effects_department":
                case "visual_effects_department":
                case "stunts":
                case "camera_department":
                case "animation_department":
                case "casting_department":
                case "costume_department":
                case "editorial_department":
                case "music_department":
                case "transportation_department":
                case "make_up_department":
                case "miscellaneous":
                    if (result.containsKey(JobType.CREW)) {
                        result.get(JobType.CREW).addAll(credit.getCredits());
                    } else {
                        result.put(JobType.CREW, credit.getCredits());
                    }
                    break;
                default:
                    if (result.containsKey(JobType.UNKNOWN)) {
                        result.get(JobType.UNKNOWN).addAll(credit.getCredits());
                    } else {
                        result.put(JobType.UNKNOWN, credit.getCredits());
                    }
                    break;
            }
        }
        
        return result;
    }
    
    private void addCredits(VideoData videoData, JobType jobType, EnumMap<JobType,List<ImdbCast>> jobs, boolean skipUncredited, boolean skipFaceless) {
        if (CollectionUtils.isEmpty(jobs.get(jobType))) {
            return;
        }
        if (!this.configServiceWrapper.isCastScanEnabled(jobType)) {
            return;
        }
            
        for (ImdbCast cast : jobs.get(jobType)) {
            final ImdbPerson person = cast.getPerson();
            if (person == null || StringUtils.isBlank(person.getName())) {
                continue; //NOSONAR
            }
            
            if (skipUncredited && StringUtils.contains(cast.getAttr(), "(uncredited")) {
                continue; //NOSONAR
            }

            final String photoURL = (person.getImage() == null) ? null : person.getImage().getUrl();
            if (skipFaceless && JobType.ACTOR.equals(jobType) && StringUtils.isEmpty(photoURL)) {
                // skip faceless actors only
                continue; //NOSONAR
            }

            CreditDTO creditDTO = this.identifierService.createCredit(SOURCE_IMDB, person.getActorId(), jobType,  person.getName(), cast.getCharacter());
            videoData.addCreditDTO(creditDTO);
        }
    }

    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        return scanImdbID(nfoContent, dto, ignorePresentId);
    }

    public static boolean scanImdbID(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(SOURCE_IMDB))) {
            return true;
        }

        LOG.trace("Scanning NFO for IMDb ID");

        try {
            int beginIndex = nfoContent.indexOf("/tt");
            if (beginIndex != -1) {
                String imdbId =  new StringTokenizer(nfoContent.substring(beginIndex + 1), "/ \n,:!&Ã©\"'(--Ã¨_Ã§Ã )=$").nextToken();
                LOG.debug("IMDb ID found in NFO: {}", imdbId);
                dto.addId(SOURCE_IMDB, imdbId);
                return true;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        LOG.debug("No IMDb ID found in NFO");
        return false;
    }

    @Override
    public ScanResult scanPerson(Person person, boolean throwTempError) {
        try {
            // get person id
            String imdbId = getPersonId(person, throwTempError);
            if (StringUtils.isBlank(imdbId)) {
                LOG.debug("IMDb id not available: {}", person.getName());
                return ScanResult.MISSING_ID;
            }

            LOG.debug("IMDb id available ({}), updating person", imdbId);
            return updatePerson(person, imdbId, throwTempError);
            
        } catch (IOException ioe) {
            LOG.error("IMDb service error: '" + person.getName() + "'", ioe);
            return ScanResult.ERROR;
        }
    }

    private ScanResult updatePerson(Person person, String imdbId, boolean throwTempError) throws IOException {
        Locale imdbLocale = localeService.getLocaleForConfig(SOURCE_IMDB);
        ImdbPerson imdbPerson = imdbApiWrapper.getPerson(imdbId, imdbLocale, throwTempError);
        if (imdbPerson == null || StringUtils.isBlank(imdbPerson.getActorId())) {
            return ScanResult.NO_RESULT;
        }
        
        // split person names
        PersonName personName = MetadataTools.splitFullName(imdbPerson.getName());
        if (OverrideTools.checkOverwriteName(person, SOURCE_IMDB)) {
            person.setName(personName.getName(), SOURCE_IMDB);
        }
        if (OverrideTools.checkOverwriteFirstName(person, SOURCE_IMDB)) {
            person.setFirstName(personName.getFirstName(), SOURCE_IMDB);
        }
        if (OverrideTools.checkOverwriteLastName(person, SOURCE_IMDB)) {
            person.setLastName(personName.getLastName(), SOURCE_IMDB);
        }
        if (OverrideTools.checkOverwriteBirthName(person, SOURCE_IMDB)) {
            person.setBirthName(imdbPerson.getRealName(), SOURCE_IMDB);
        }
        
        if (OverrideTools.checkOverwriteBiography(person, SOURCE_IMDB)) {
            final String apiBio = MetadataTools.cleanBiography(imdbPerson.getBiography());
            if (StringUtils.isNotBlank(apiBio)) {
                person.setBiography(apiBio, SOURCE_IMDB);
            } else {
                // try biography from web site
                final String bio = imdbApiWrapper.getPersonBioXML(imdbId, throwTempError);
                if (bio.contains(">Mini Bio (1)</h4>")) {
                    String biography = HTMLTools.extractTag(bio, ">Mini Bio (1)</h4>", "<em>- IMDb Mini Biography");
                    if (StringUtils.isBlank(biography) && bio.contains("<a name=\"trivia\">")) {
                        biography = HTMLTools.extractTag(bio, ">Mini Bio (1)</h4>", "<a name=\"trivia\">");
                    }
                    person.setBiography(HTMLTools.removeHtmlTags(biography), SOURCE_IMDB);
                }
            }
        }
        
        if (imdbPerson.getBirth() != null) {
            if (imdbPerson.getBirth().getDate() != null && OverrideTools.checkOverwriteBirthDay(person, SOURCE_IMDB)) {
                final String birthDay = imdbPerson.getBirth().getDate().get(LITERAL_NORMAL);
                person.setBirthDay(MetadataTools.parseToDate(birthDay), SOURCE_IMDB);
            }

            if (OverrideTools.checkOverwriteBirthPlace(person, SOURCE_IMDB)) {
                person.setBirthPlace(imdbPerson.getBirth().getPlace(), SOURCE_IMDB);
            }
        }

        if (imdbPerson.getDeath() != null) {
            if (imdbPerson.getDeath().getDate() != null && OverrideTools.checkOverwriteDeathDay(person, SOURCE_IMDB)) {
                final String deathDay = imdbPerson.getDeath().getDate().get(LITERAL_NORMAL);
                person.setDeathDay(MetadataTools.parseToDate(deathDay), SOURCE_IMDB);
            }

            if (OverrideTools.checkOverwriteDeathPlace(person, SOURCE_IMDB)) {
                person.setDeathPlace(imdbPerson.getDeath().getPlace(), SOURCE_IMDB);
            }
        }

        return ScanResult.OK;
    }
}
