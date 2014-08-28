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
package org.yamj.core.service.nfo;

import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.model.StageFile;
import org.yamj.core.service.file.tools.FileTools;
import org.yamj.core.service.plugin.ImdbScanner;
import org.yamj.core.service.plugin.TheMovieDbScanner;
import org.yamj.core.service.plugin.TheTVDbScanner;
import org.yamj.core.tools.StringTools;
import org.yamj.core.tools.xml.DOMHelper;

/**
 * Class to read the NFO files
 */
@Service("infoReader")
public final class InfoReader {

    private static final Logger LOG = LoggerFactory.getLogger(InfoReader.class);
    private static final String XML_START = "<";
    private static final String XML_END = "</";
    private static final String SPLIT_GENRE = "(?<!-)/|,|\\|";  // caters for the case where "-/" is not wanted as part of the split

    @Autowired
    private ConfigService configService;

    /**
     * Try and read a NFO file for information
     *
     * @param nfoFile
     * @param nfoText
     * @param dto
     */
    public void readNfoFile(StageFile stageFile, InfoDTO  dto) {
        String nfoFilename = stageFile.getFileName();

        String nfoContent;
        if (StringUtils.isBlank(stageFile.getContent())) {
            // try to read content from file
            File nfoFile = new File(stageFile.getFullPath());
            nfoContent = FileTools.readFileToString(nfoFile);
        } else {
            // get delivered content
            nfoContent = stageFile.getContent();
        }
        
        if (StringUtils.isBlank(nfoContent)) {
            LOG.warn("NFO could not be read {}", nfoFilename);
            return;
        }
        
        boolean parsedNfo = Boolean.FALSE;   // was the NFO XML parsed correctly or at all
        boolean hasXml = Boolean.FALSE;

        if (StringUtils.containsIgnoreCase(nfoContent, XML_START + DOMHelper.TYPE_MOVIE)
                || StringUtils.containsIgnoreCase(nfoContent, XML_START + DOMHelper.TYPE_TVSHOW)
                || StringUtils.containsIgnoreCase(nfoContent, XML_START + DOMHelper.TYPE_EPISODE)) {
            hasXml = Boolean.TRUE;
        }

        // If the file has XML tags in it, try reading it as a pure XML file
        if (hasXml) {
            parsedNfo = this.readXmlNfo(nfoContent, nfoFilename, dto);
        }

        // If it has XML in it, but didn't parse correctly, try splitting it out
        if (hasXml && !parsedNfo) {
            int posMovie = findPosition(nfoContent, DOMHelper.TYPE_MOVIE);
            int posTv = findPosition(nfoContent, DOMHelper.TYPE_TVSHOW);
            int posEp = findPosition(nfoContent, DOMHelper.TYPE_EPISODE);
            int start = Math.min(posMovie, Math.min(posTv, posEp));

            posMovie = StringUtils.indexOf(nfoContent, XML_END + DOMHelper.TYPE_MOVIE);
            posTv = StringUtils.indexOf(nfoContent, XML_END + DOMHelper.TYPE_TVSHOW);
            posEp = StringUtils.indexOf(nfoContent, XML_END + DOMHelper.TYPE_EPISODE);
            int end = Math.max(posMovie, Math.max(posTv, posEp));

            if ((end > -1) && (end > start)) {
                end = StringUtils.indexOf(nfoContent, '>', end) + 1;

                // Send text to be read
                String nfoTrimmed = StringUtils.substring(nfoContent, start, end);
                parsedNfo = readXmlNfo(nfoTrimmed, nfoFilename, dto);
            }
        }

        // If the XML wasn't found or parsed correctly, then fall back to the old method
        if (parsedNfo) {
            LOG.debug("Successfully scanned {} as XML format", nfoFilename);
        } else {
            throw new RuntimeException("Failed to scan " + nfoFilename + " as XML format");
        }
    }

    /**
     * Find the position of the string or return the maximum
     *
     * @param nfoText
     * @param xmlType
     * @return
     */
    private int findPosition(final String nfoText, final String xmlType) {
        int pos = StringUtils.indexOf(nfoText, XML_START + xmlType);
        return (pos == -1 ? Integer.MAX_VALUE : pos);
    }

    /**
     * Read NFO as XML.
     *
     * @param nfoString
     * @param nfoFilename
     * @param dto
     * @return
     */
    private boolean readXmlNfo(final String nfoContent, final String nfoFilename, InfoDTO dto) {
        Document xmlDoc;

        try {
            xmlDoc = DOMHelper.getDocFromString(nfoContent);
        } catch (Exception ex) {
            LOG.error("Failed parsing NFO file: {}", nfoFilename);
            LOG.error("Error", ex);
            return Boolean.FALSE;
        }

        parseXML(xmlDoc, dto, nfoFilename);
        return Boolean.TRUE;
    }

    /**
     * Parse the XML document for NFO information
     *
     * @param xmlDoc
     * @param dto
     * @param nfoFilename
     * @return
     */
    private void parseXML(final Document xmlDoc, InfoDTO dto, final String nfoFilename) {
        NodeList nlMovies;

        // determine if the NFO file is for a TV Show or Movie so the default ID can be set
        boolean isTV = xmlDoc.getElementsByTagName(DOMHelper.TYPE_TVSHOW).getLength() > 0;
        if (dto.isTvShow() || isTV) {
            nlMovies = xmlDoc.getElementsByTagName(DOMHelper.TYPE_TVSHOW);
            isTV = Boolean.TRUE;
        } else {
            nlMovies = xmlDoc.getElementsByTagName(DOMHelper.TYPE_MOVIE);
            isTV = Boolean.FALSE;
        }
        
        // just one movie per file
        if ((nlMovies == null) || (nlMovies.getLength() == 0)) {
            LOG.warn("NFO {} contains no infos", nfoFilename);
            return;
        }
        // get first info element
        Node nMovie = nlMovies.item(0);
        if (nMovie.getNodeType() != Node.ELEMENT_NODE) {
            LOG.warn("NFO {} contains no infos", nfoFilename);
            return;
        }          
        Element eCommon = (Element) nMovie;
        
        // parse title
        parseTitle(eCommon, dto);

        // parse year
        String value = DOMHelper.getValueFromElement(eCommon, "year");
        dto.setYear(value);

        // get the movie IDs
        parseIds(eCommon.getElementsByTagName("id"), dto, isTV);

        // ID specific to TV Shows
        if (dto.isTvShow()) {
            value = DOMHelper.getValueFromElement(eCommon, "tvdbid");
            if (StringUtils.isNotBlank(value)) {
                dto.addId(TheTVDbScanner.SCANNER_ID, value);
            }
        }

        // TODO parse sets
        //parseSets(eCommon.getElementsByTagName("set"), movie);

        // parse rating
        int rating = parseRating(DOMHelper.getValueFromElement(eCommon, "rating"));
        dto.setRating(rating);

        // parse runtime
        parseRuntime(eCommon, dto);

        // parse certification
        parseCertification(eCommon, dto);

        // parse plot
        value = DOMHelper.getValueFromElement(eCommon, "plot");
        dto.setPlot(value);

        // parse outline
        value = DOMHelper.getValueFromElement(eCommon, "outline");
        dto.setOutline(value);

        // parse tagline
        value = DOMHelper.getValueFromElement(eCommon, "tagline");
        dto.setTagline(value);

        // parse quote
        value = DOMHelper.getValueFromElement(eCommon, "quote");
        dto.setQuote(value);

        // parse company (may be studio)
        value = DOMHelper.getValueFromElement(eCommon, "studio");
        dto.setCompany(value);
        value = DOMHelper.getValueFromElement(eCommon, "company");
        dto.setCompany(value);

        /* TODO
        if (OverrideTools.checkOverwriteGenres(movie, NFO_PLUGIN_ID)) {
            List<String> newGenres = new ArrayList<String>();
            parseGenres(eCommon.getElementsByTagName("genre"), newGenres);
            movie.setGenres(newGenres, NFO_PLUGIN_ID);
        }

        // Premiered & Release Date
        movieDate(movie, DOMHelper.getValueFromElement(eCommon, "premiered"));
        movieDate(movie, DOMHelper.getValueFromElement(eCommon, "releasedate"));


        if (OverrideTools.checkOverwriteCountry(movie, NFO_PLUGIN_ID)) {
            movie.setCountries(DOMHelper.getValueFromElement(eCommon, "country"), NFO_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteTop250(movie, NFO_PLUGIN_ID)) {
            movie.setTop250(DOMHelper.getValueFromElement(eCommon, "top250"), NFO_PLUGIN_ID);
        }

        // Director and Writers
        if (!SKIP_NFO_CREW) {
            parseDirectors(eCommon.getElementsByTagName("director"), movie);
            
            List<Node> writerNodes = new ArrayList<Node>();
            // get writers list
            NodeList nlWriters = eCommon.getElementsByTagName("writer");
            if (nlWriters != null && nlWriters.getLength() > 0) {
                for (int looper = 0; looper < nlWriters.getLength(); looper++) {
                    Node node = nlWriters.item(looper);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        writerNodes.add(node);
                    }
                }
            }
            // get credits list (old style)
            nlWriters = eCommon.getElementsByTagName("credits");
            if (nlWriters != null && nlWriters.getLength() > 0) {
                for (int looper = 0; looper < nlWriters.getLength(); looper++) {
                    Node node = nlWriters.item(looper);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        writerNodes.add(node);
                    }
                }
            }
            // parse writers
            parseWriters(writerNodes, movie);
        }

        // Actors
        if (!SKIP_NFO_CAST) {
            parseActors(eCommon.getElementsByTagName("actor"), movie);
        }

        // FPS
        float tmpFps = NumberUtils.toFloat(DOMHelper.getValueFromElement(eCommon, "fps"), -1F);
        if (tmpFps > -1F) {
            movie.setFps(tmpFps, NFO_PLUGIN_ID);
        }

        // VideoSource: Issue 506 - Even though it's not strictly XBMC standard
        if (OverrideTools.checkOverwriteVideoSource(movie, NFO_PLUGIN_ID)) {
            // Issue 2531: Try the alternative "videoSource"
            movie.setVideoSource(DOMHelper.getValueFromElement(eCommon, "videosource", "videoSource"), NFO_PLUGIN_ID);
        }

        // Video Output
        String tempString = DOMHelper.getValueFromElement(eCommon, "videooutput");
        movie.setVideoOutput(tempString, NFO_PLUGIN_ID);

        // Parse the video info
        parseFileInfo(movie, DOMHelper.getElementByName(eCommon, "fileinfo"));
        
        */

        /* Parse the episode details
        if (movie.isTVShow()) {
            parseAllEpisodeDetails(movie, xmlDoc.getElementsByTagName(TYPE_EPISODE));
        }
        */
    }

    /**
     * Parse all the title information from the XML NFO file
     *
     * @param eCommon
     * @param dto
     */
    private  void parseTitle(Element eCommon, InfoDTO dto) {
        // determine title elements
        String titleMain = DOMHelper.getValueFromElement(eCommon, "title");
        String titleOrig = DOMHelper.getValueFromElement(eCommon, "originaltitle", "originalTitle");
        String titleSort = DOMHelper.getValueFromElement(eCommon, "sorttitle", "sortTitle");

        dto.setTitle(titleMain);
        dto.setTitleOriginal(titleOrig);
        dto.setTitleSort(titleSort);
    }

    /**
     * Parse all the IDs associated with the movie from the XML NFO file
     *
     * @param nlElements
     * @param dto
     * @param isTV
     */
    private void parseIds(NodeList nlElements, InfoDTO dto, boolean isTV) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eId = (Element) nElements;

                String movieId = eId.getTextContent();
                if (StringUtils.isNotBlank(movieId)) {
                    String movieDb = eId.getAttribute("moviedb");
                    if (StringUtils.isBlank(movieDb)) {
                        if ("-1".equals(movieId)) {
                            // skip all scans
                            dto.setSkipAllOnlineScans();
                        } else {
                            // choose default scanner id
                            if (isTV) {
                                movieDb = TheTVDbScanner.SCANNER_ID;
                            } else {
                                movieDb = ImdbScanner.SCANNER_ID;
                            }
                            dto.addId(movieDb, movieId);
                            LOG.debug("Found {} ID: {}, ", movieDb, movieId);
                        }
                    } else {
                        dto.addId(movieDb, movieId);
                        LOG.debug("Found {} ID: {}, ", movieDb, movieId);
                    }
                }
                
                // process the TMDB id
                movieId = eId.getAttribute("TMDB");
                if (StringUtils.isNotBlank(movieId)) {
                    LOG.debug("Found TheMovieDb ID: {}", movieId);
                    dto.addId(TheMovieDbScanner.SCANNER_ID, movieId);
                }
            }
        }
    }

    /**
     * Parse the rating from the passed string and normalise it
     *
     * @param ratingString
     * @param movie
     * @return true if the rating was successfully parsed.
     */
    private int parseRating(String ratingString) {
        if (StringUtils.isBlank(ratingString)) {
            // Rating isn't valid, so skip it
            return -1;
        } else {
            try {
                float rating = Float.parseFloat(ratingString);
                if (rating > 0.0f) {
                    if (rating <= 10.0f) {
                        return Math.round(rating * 10f);
                    } else {
                        return Math.round(rating * 1f);
                    }
                } else {
                    // Negative or zero, so return zero
                    return 0;
                }
            } catch (NumberFormatException ex) {
                LOG.trace("Failed to transform rating ", ratingString);
                return -1;
            }
        }
    }
    
    /**
     * Parse Runtime from the XML NFO file
     *
     * @param eCommon
     * @param dto
     */
    private void parseRuntime(Element eCommon, InfoDTO dto) {
        String runtime = DOMHelper.getValueFromElement(eCommon, "runtime");

        // Save the first runtime to use if no preferred one is found
        String prefRuntime = null;
        // Split the runtime into individual parts
        for (String rtSingle : runtime.split("\\|")) {
            // IF we don't have a current preferred runtime, set it now.
            if (StringUtils.isBlank(prefRuntime)) {
                prefRuntime = rtSingle;
            }

            String preferredCountry = this.configService.getProperty("yamj3.scan.preferredCountry", "USA");
            // Check to see if we have our preferred country in the string
            if (StringUtils.containsIgnoreCase(rtSingle, preferredCountry)) {
                // Lets get the country runtime
                prefRuntime = rtSingle.substring(rtSingle.indexOf(preferredCountry) + preferredCountry.length() + 1);
            }
        }
        dto.setRuntime(prefRuntime);
    }

    /**
     * Parse Certification from the XML NFO file
     *
     * @param eCommon
     * @param movie
     */
    private void parseCertification(Element eCommon, InfoDTO dto) {
        boolean certFromMPAA = this.configService.getBooleanProperty("yamj3.scan.certificationFromMPAA", Boolean.TRUE);
        
        String tempCert;
        if (certFromMPAA) {
            tempCert = DOMHelper.getValueFromElement(eCommon, "mpaa");
            if (StringUtils.isNotBlank(tempCert)) {
                String mpaa = StringTools.processMpaaCertification(tempCert);
                dto.setCertification(mpaa);
            }
        } else {
            tempCert = DOMHelper.getValueFromElement(eCommon, "certification");
            if (StringUtils.isNotBlank(tempCert)) {
                String preferredCountry = this.configService.getProperty("yamj3.scan.preferredCountry", "USA");
                int countryPos = tempCert.lastIndexOf(preferredCountry);
                if (countryPos > 0) {
                    // We've found the country, so extract just that tag
                    tempCert = tempCert.substring(countryPos);
                    int pos = tempCert.indexOf(':');
                    if (pos > 0) {
                        int endPos = tempCert.indexOf(" /");
                        if (endPos > 0) {
                            // This is in the middle of the string
                            tempCert = tempCert.substring(pos + 1, endPos);
                        } else {
                            // This is at the end of the string
                            tempCert = tempCert.substring(pos + 1);
                        }
                    }
                } else if (StringUtils.containsIgnoreCase(tempCert, "Rated")) {
                    // Extract the MPAA rating from the certification
                    tempCert = StringTools.processMpaaCertification(tempCert);
                } else {
                    // The country wasn't found in the value, so grab the last one
                    int pos = tempCert.lastIndexOf(':');
                    if (pos > 0) {
                        // Strip the country code from the rating for certification like "UK:PG-12"
                        tempCert = tempCert.substring(pos + 1);
                    }
                }

                dto.setCertification(tempCert.trim());
            }
        }
    }

    // still TODO
    
    /**
     * Parse the FileInfo section
     *
     * @param movie
     * @param eFileInfo
    private static void parseFileInfo(Movie movie, Element eFileInfo) {
        if (eFileInfo == null) {
            return;
        }

        if (OverrideTools.checkOverwriteContainer(movie, NFO_PLUGIN_ID)) {
            String container = DOMHelper.getValueFromElement(eFileInfo, "container");
            movie.setContainer(container, NFO_PLUGIN_ID);
        }

        Element eStreamDetails = DOMHelper.getElementByName(eFileInfo, "streamdetails");

        if (eStreamDetails == null) {
            return;
        }

        // Video
        NodeList nlStreams = eStreamDetails.getElementsByTagName("video");
        Node nStreams;
        for (int looper = 0; looper < nlStreams.getLength(); looper++) {
            nStreams = nlStreams.item(looper);
            if (nStreams.getNodeType() == Node.ELEMENT_NODE) {
                Element eStreams = (Element) nStreams;

                String temp = DOMHelper.getValueFromElement(eStreams, "codec");
                if (isValidString(temp)) {
                    Codec videoCodec = new Codec(CodecType.VIDEO);
                    videoCodec.setCodecSource(CodecSource.NFO);
                    videoCodec.setCodec(temp);
                    movie.addCodec(videoCodec);
                }

                if (OverrideTools.checkOverwriteAspectRatio(movie, NFO_PLUGIN_ID)) {
                    temp = DOMHelper.getValueFromElement(eStreams, "aspect");
                    movie.setAspectRatio(ASPECT_TOOLS.cleanAspectRatio(temp), NFO_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteResolution(movie, NFO_PLUGIN_ID)) {
                    movie.setResolution(DOMHelper.getValueFromElement(eStreams, "width"), DOMHelper.getValueFromElement(eStreams, "height"), NFO_PLUGIN_ID);
                }
            }
        } // End of VIDEO

        // Audio
        nlStreams = eStreamDetails.getElementsByTagName("audio");

        for (int looper = 0; looper < nlStreams.getLength(); looper++) {
            nStreams = nlStreams.item(looper);
            if (nStreams.getNodeType() == Node.ELEMENT_NODE) {
                Element eStreams = (Element) nStreams;

                String aCodec = DOMHelper.getValueFromElement(eStreams, "codec").trim();
                String aLanguage = DOMHelper.getValueFromElement(eStreams, "language");
                String aChannels = DOMHelper.getValueFromElement(eStreams, "channels");

                // If the codec is lowercase, covert it to uppercase, otherwise leave it alone
                if (StringUtils.isAllLowerCase(aCodec)) {
                    aCodec = aCodec.toUpperCase();
                }

                if (StringTools.isValidString(aLanguage)) {
                    aLanguage = MovieFilenameScanner.determineLanguage(aLanguage);
                }

                Codec audioCodec = new Codec(CodecType.AUDIO, aCodec);
                audioCodec.setCodecSource(CodecSource.NFO);
                audioCodec.setCodecLanguage(aLanguage);
                audioCodec.setCodecChannels(aChannels);
                movie.addCodec(audioCodec);
            }
        } // End of AUDIO

        // Update the language
        if (OverrideTools.checkOverwriteLanguage(movie, NFO_PLUGIN_ID)) {
            Set<String> langs = new HashSet<String>();
            // Process the languages and remove any duplicates
            for (Codec codec : movie.getCodecs()) {
                if (codec.getCodecType() == CodecType.AUDIO) {
                    langs.add(codec.getCodecLanguage());
                }
            }

            // Remove UNKNOWN if it is NOT the only entry
            if (langs.contains(Movie.UNKNOWN) && langs.size() > 1) {
                langs.remove(Movie.UNKNOWN);
            } else if (langs.isEmpty()) {
                // Add the language as UNKNOWN by default.
                langs.add(Movie.UNKNOWN);
            }

            // Build the language string
            StringBuilder movieLanguage = new StringBuilder();
            for (String lang : langs) {
                if (movieLanguage.length() > 0) {
                    movieLanguage.append(LANGUAGE_DELIMITER);
                }
                movieLanguage.append(lang);
            }
            movie.setLanguage(movieLanguage.toString(), NFO_PLUGIN_ID);
        }

        // Subtitles
        List<String> subtitles = new ArrayList<String>();
        nlStreams = eStreamDetails.getElementsByTagName("subtitle");
        for (int looper = 0; looper < nlStreams.getLength(); looper++) {
            nStreams = nlStreams.item(looper);
            if (nStreams.getNodeType() == Node.ELEMENT_NODE) {
                Element eStreams = (Element) nStreams;
                subtitles.add(DOMHelper.getValueFromElement(eStreams, "language"));
            }
        }
        SubtitleTools.setMovieSubtitles(movie, subtitles);
    }
    */

    /**
     * Process all the Episode Details
     *
     * @param movie
     * @param nlEpisodeDetails
    private static void parseAllEpisodeDetails(Movie movie, NodeList nlEpisodeDetails) {
        Node nEpisodeDetails;
        for (int looper = 0; looper < nlEpisodeDetails.getLength(); looper++) {
            nEpisodeDetails = nlEpisodeDetails.item(looper);
            if (nEpisodeDetails.getNodeType() == Node.ELEMENT_NODE) {
                Element eEpisodeDetail = (Element) nEpisodeDetails;
                parseSingleEpisodeDetail(eEpisodeDetail).updateMovie(movie);
            }
        }
    }
    */

    /**
     * Parse a single episode detail element
     *
     * @param movie
     * @param eEpisodeDetails
     * @return
    private static EpisodeDetail parseSingleEpisodeDetail(Element eEpisodeDetails) {
        EpisodeDetail epDetail = new EpisodeDetail();
        if (eEpisodeDetails == null) {
            return epDetail;
        }

        epDetail.setTitle(DOMHelper.getValueFromElement(eEpisodeDetails, "title"));

        String tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "season");
        if (StringUtils.isNumeric(tempValue)) {
            epDetail.setSeason(Integer.parseInt(tempValue));
        }

        tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "episode");
        if (StringUtils.isNumeric(tempValue)) {
            epDetail.setEpisode(Integer.parseInt(tempValue));
        }

        epDetail.setPlot(DOMHelper.getValueFromElement(eEpisodeDetails, "plot"));

        tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "rating");
        int rating = parseRating(tempValue);
        if (rating > -1) {
            // Looks like a valid rating
            epDetail.setRating(String.valueOf(rating));
        }

        tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "aired");
        if (isValidString(tempValue)) {
            try {
                epDetail.setFirstAired(DateTimeTools.convertDateToString(new DateTime(tempValue)));
            } catch (Exception ignore) {
                // Set the aired date if there is an exception
                epDetail.setFirstAired(tempValue);
            }
        }

        epDetail.setAirsAfterSeason(DOMHelper.getValueFromElement(eEpisodeDetails, "airsafterseason", "airsAfterSeason"));
        epDetail.setAirsBeforeSeason(DOMHelper.getValueFromElement(eEpisodeDetails, "airsbeforeseason", "airsBeforeSeason"));
        epDetail.setAirsBeforeEpisode(DOMHelper.getValueFromElement(eEpisodeDetails, "airsbeforeepisode", "airsBeforeEpisode"));

        return epDetail;
    }
    */

    /**
     * Convert the date string to a date and update the movie object
     *
     * @param movie
     * @param dateString
     * @param parseDate
    public static void movieDate(Movie movie, final String dateString) {
        DateTimeConfigBuilder.newInstance().setDmyOrder(false);

        String parseDate = StringUtils.normalizeSpace(dateString);
        if (StringTools.isValidString(parseDate)) {
            try {
                DateTime dateTime;
                if (parseDate.length() == 4 && StringUtils.isNumeric(parseDate)) {
                    // Warn the user
                    LOG.debug(LOG_MESSAGE + "Partial date detected in premiered field of NFO for " + movie.getBaseFilename());
                    // Assume just the year an append "-01-01" to the end
                    dateTime = new DateTime(parseDate + "-01-01");
                } else {
                    dateTime = new DateTime(parseDate);
                }

                if (OverrideTools.checkOverwriteReleaseDate(movie, NFO_PLUGIN_ID)) {
                    movie.setReleaseDate(DateTimeTools.convertDateToString(dateTime), NFO_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteYear(movie, NFO_PLUGIN_ID)) {
                    movie.setYear(dateTime.toString("yyyy"), NFO_PLUGIN_ID);
                }
            } catch (Exception ex) {
                LOG.warn(LOG_MESSAGE + "Failed parsing NFO file for movie: " + movie.getBaseFilename() + ". Please fix or remove it.");
                LOG.warn(LOG_MESSAGE + "premiered or releasedate does not contain a valid date: " + parseDate);
                LOG.warn(LOG_MESSAGE + SystemTools.getStackTrace(ex));

                if (OverrideTools.checkOverwriteReleaseDate(movie, NFO_PLUGIN_ID)) {
                    movie.setReleaseDate(parseDate, NFO_PLUGIN_ID);
                }
            }
        }
    }
     */

    /**
     * Parse Genres from the XML NFO file
     *
     * Caters for multiple genres on the same line and multiple lines.
     *
     * @param nlElements
     * @param movie
    private static void parseGenres(NodeList nlElements, List<String> newGenres) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eGenre = (Element) nElements;
                NodeList nlNames = eGenre.getElementsByTagName("name");
                if ((nlNames != null) && (nlNames.getLength() > 0)) {
                    parseGenres(nlNames, newGenres);
                } else {
                    newGenres.addAll(StringTools.splitList(eGenre.getTextContent(), SPLIT_GENRE));
                }
            }
        }
    }
    */

    /**
     * Parse Actors from the XML NFO file.
     *
     * @param nlElements
     * @param movie
    private static void parseActors(NodeList nlElements, Movie movie) {
        // check if we have a node
        if (nlElements == null || nlElements.getLength() == 0) {
            return;
        }

        // check if we should override
        boolean overrideActors = OverrideTools.checkOverwriteActors(movie, NFO_PLUGIN_ID);
        boolean overridePeopleActors = OverrideTools.checkOverwritePeopleActors(movie, NFO_PLUGIN_ID);
        if (!overrideActors && !overridePeopleActors) {
            // nothing to do if nothing should be overridden
            return;
        }

        // count for already set actors
        int count = 0;
        // flag to indicate if cast must be cleared
        boolean clearCast = Boolean.TRUE;
        boolean clearPeopleCast = Boolean.TRUE;

        for (int actorLoop = 0; actorLoop < nlElements.getLength(); actorLoop++) {
            // Get all the name/role/thumb nodes
            Node nActors = nlElements.item(actorLoop);
            NodeList nlCast = nActors.getChildNodes();
            Node nElement;

            String aName = Movie.UNKNOWN;
            String aRole = Movie.UNKNOWN;
            String aThumb = Movie.UNKNOWN;
            Boolean firstActor = Boolean.TRUE;

            if (nlCast.getLength() > 1) {
                for (int looper = 0; looper < nlCast.getLength(); looper++) {
                    nElement = nlCast.item(looper);
                    if (nElement.getNodeType() == Node.ELEMENT_NODE) {
                        Element eCast = (Element) nElement;
                        if (eCast.getNodeName().equalsIgnoreCase("name")) {
                            if (firstActor) {
                                firstActor = Boolean.FALSE;
                            } else {

                                if (overrideActors) {
                                    // clear cast if not already done
                                    if (clearCast) {
                                        movie.clearCast();
                                        clearCast = Boolean.FALSE;
                                    }
                                    // add actor
                                    movie.addActor(aName, NFO_PLUGIN_ID);
                                }

                                if (overridePeopleActors && (count < MAX_COUNT_ACTOR)) {
                                    // clear people cast if not already done
                                    if (clearPeopleCast) {
                                        movie.clearPeopleCast();
                                        clearPeopleCast = Boolean.FALSE;
                                    }
                                    // add actor
                                    if (movie.addActor(Movie.UNKNOWN, aName, aRole, aThumb, Movie.UNKNOWN, NFO_PLUGIN_ID)) {
                                        count++;
                                    }
                                }
                            }
                            aName = eCast.getTextContent();
                            aRole = Movie.UNKNOWN;
                            aThumb = Movie.UNKNOWN;
                        } else if (eCast.getNodeName().equalsIgnoreCase("role") && StringUtils.isNotBlank(eCast.getTextContent())) {
                            aRole = eCast.getTextContent();
                        } else if (eCast.getNodeName().equalsIgnoreCase("thumb") && StringUtils.isNotBlank(eCast.getTextContent())) {
                            // thumb will be skipped if there's nothing in there
                            aThumb = eCast.getTextContent();
                        }
                        // There's a case where there might be a different node here that isn't name, role or thumb, but that will be ignored
                    }
                }
            } else {
                // This looks like a Mede8er node in the "<actor>Actor Name</actor>" format, so just get the text element
                aName = nActors.getTextContent();
            }

            if (overrideActors) {
                // clear cast if not already done
                if (clearCast) {
                    movie.clearCast();
                    clearCast = Boolean.FALSE;
                }
                // add actor
                movie.addActor(aName, NFO_PLUGIN_ID);
            }

            if (overridePeopleActors && (count < MAX_COUNT_ACTOR)) {
                // clear people cast if not already done
                if (clearPeopleCast) {
                    movie.clearPeopleCast();
                    clearPeopleCast = Boolean.FALSE;
                }
                // add actor
                if (movie.addActor(Movie.UNKNOWN, aName, aRole, aThumb, Movie.UNKNOWN, NFO_PLUGIN_ID)) {
                    count++;
                }
            }
        }
    }
    */

    /**
     * Parse Writers from the XML NFO file
     *
     * @param nlElements
     * @param movie
    private static void parseWriters(List<Node> nlWriters, Movie movie) {
        // check if we have nodes
        if (nlWriters == null || nlWriters.isEmpty()) {
            return;
        }

        // check if we should override
        boolean overrideWriters = OverrideTools.checkOverwriteWriters(movie, NFO_PLUGIN_ID);
        boolean overridePeopleWriters = OverrideTools.checkOverwritePeopleWriters(movie, NFO_PLUGIN_ID);
        if (!overrideWriters && !overridePeopleWriters) {
            // nothing to do if nothing should be overridden
            return;
        }

        Set<String> newWriters = new LinkedHashSet<String>();
        for (Node nWriter : nlWriters) {
            NodeList nlChilds = ((Element)nWriter).getChildNodes();
            Node nChilds;
            for (int looper = 0; looper < nlChilds.getLength(); looper++) {
                nChilds = nlChilds.item(looper);
                if (nChilds.getNodeType() == Node.TEXT_NODE) {
                    newWriters.add(nChilds.getNodeValue());
                }
            }
        }

        if (overrideWriters) {
            movie.setWriters(newWriters, NFO_PLUGIN_ID);
        }
        if (overridePeopleWriters) {
            movie.setPeopleWriters(newWriters, NFO_PLUGIN_ID);
        }
    }
    */

    /**
     * Parse Directors from the XML NFO file
     *
     * @param nlElements
     * @param movie
    private static void parseDirectors(NodeList nlElements, Movie movie) {
        // check if we have a node
        if (nlElements == null || nlElements.getLength() == 0) {
            return;
        }

        // check if we should override
        boolean overrideDirectors = OverrideTools.checkOverwriteDirectors(movie, NFO_PLUGIN_ID);
        boolean overridePeopleDirectors = OverrideTools.checkOverwritePeopleDirectors(movie, NFO_PLUGIN_ID);
        if (!overrideDirectors && !overridePeopleDirectors) {
            // nothing to do if nothing should be overridden
            return;
        }

        List<String> newDirectors = new ArrayList<String>();
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eDirector = (Element) nElements;
                newDirectors.add(eDirector.getTextContent());
            }
        }

        if (overrideDirectors) {
            movie.setDirectors(newDirectors, NFO_PLUGIN_ID);
        }

        if (overridePeopleDirectors) {
            movie.setPeopleDirectors(newDirectors, NFO_PLUGIN_ID);
        }
    }
     */

    /**
     * Parse Trailers from the XML NFO file
     *
     * @param nlElements
     * @param movie
    private static void parseTrailers(NodeList nlElements, Movie movie) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eTrailer = (Element) nElements;

                String trailer = eTrailer.getTextContent().trim();
                if (!trailer.isEmpty()) {
                    ExtraFile ef = new ExtraFile();
                    ef.setNewFile(Boolean.FALSE);
                    ef.setFilename(trailer);
                    movie.addExtraFile(ef);
                }
            }
        }
    }
    */
    
    /**
     * Parse Sets from the XML NFO file
     *
     * @param nlElements
     * @param movie
    private static void parseSets(NodeList nlElements, Movie movie) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eId = (Element) nElements;

                String setOrder = eId.getAttribute("order");
                if (StringUtils.isNumeric(setOrder)) {
                    movie.addSet(eId.getTextContent(), Integer.parseInt(setOrder));
                } else {
                    movie.addSet(eId.getTextContent());
                }
            }
        }
    }
    */
}
