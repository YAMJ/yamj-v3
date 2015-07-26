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
package org.yamj.core.service.metadata.nfo;

import java.io.File;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.yamj.common.type.StatusType;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.StageFile;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.service.file.FileTools;
import org.yamj.core.service.metadata.online.*;
import org.yamj.core.service.staging.StagingService;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.xml.DOMHelper;

/**
 * Service to read the NFO files
 */
@Service("infoReader")
public final class InfoReader {

    private static final Logger LOG = LoggerFactory.getLogger(InfoReader.class);
    private static final String XML_START = "<";
    private static final String XML_END = "</";
    private static final String SPLIT_GENRE = "(?<!-)/|,|\\|";  // caters for the case where "-/" is not wanted as part of the split
    
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private StagingService stagingService;
    @Autowired
    private OnlineScannerService onlineScannerService;

    /**
     * Try and read a NFO file for information
     *
     * @param nfoFile
     * @param nfoText
     * @param dto
     */
    public void readNfoFile(StageFile stageFile, InfoDTO dto) {
        String nfoFilename = stageFile.getFileName();

        File nfoFile = new File(stageFile.getFullPath());
        String nfoContent = null;
        try {
            nfoContent = FileUtils.readFileToString(nfoFile, FileTools.DEFAULT_CHARSET);
        } catch (Exception e) {
            LOG.error("Unable to read NFO file: " + stageFile.getFullPath(), e);
            
            nfoFile = null;
            nfoContent = stageFile.getContent();
            
            if (StringUtils.isBlank(nfoContent)) {
                LOG.warn("NFO file '{}' is not readable", nfoFilename);
                
                try {
                    stageFile.setStatus(StatusType.INVALID);
                    this.stagingService.updateStageFile(stageFile);
                } catch (Exception ignore) {
                    // error can be ignored
                }
                
                // nothing to do for this stage file
                return;
            }
            
            LOG.warn("NFO file '{}' is not readable; try stage file content", nfoFilename);
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
            parsedNfo = this.readXmlNfo(nfoFile, nfoContent, nfoFilename, dto);
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
                parsedNfo = readXmlNfo(null, nfoTrimmed, nfoFilename, dto);

                nfoTrimmed = StringUtils.remove(nfoContent, nfoTrimmed);
                if (parsedNfo && StringUtils.isNotBlank(nfoTrimmed)) {
                    // we have some text left, so scan that with the text scanner
                    readTextNfo(nfoTrimmed, dto);
                }
            }
        }

        // If the XML wasn't found or parsed correctly, then fall back to the old method
        if (parsedNfo) {
            LOG.debug("Successfully scanned {} as XML format", nfoFilename);
        } else {
            // If the XML wasn't found or parsed correctly, then fall back to the old method
            parsedNfo = readTextNfo(nfoContent, dto);
            if (parsedNfo) {
                LOG.debug("Successfully scanned {} as text format", nfoFilename);
            } else {
                LOG.warn("Failed to find any information in {}", nfoFilename);

                try {
                    stageFile.setStatus(StatusType.INVALID);
                    this.stagingService.updateStageFile(stageFile);
                } catch (Exception ignore) {
                    // error can be ignored cause will be retried later
                }
            }
        }
    }

    /**
     * Find the position of the string or return the maximum
     *
     * @param nfoText
     * @param xmlType
     * @return
     */
    private static int findPosition(final String nfoText, final String xmlType) {
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
    private boolean readXmlNfo(final File nfoFile, final String nfoContent, final String nfoFilename, InfoDTO dto) {
        Document xmlDoc;

        try {
            if (nfoFile == null) {
                // Assume we're using the string
                xmlDoc = DOMHelper.getDocFromString(nfoContent);
            } else {
                xmlDoc = DOMHelper.getDocFromFile(nfoFile);
            }
        } catch (Exception ex) {
            LOG.error("Failed parsing NFO file: " + nfoFilename, ex);
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
            dto.setTvShow(true);
        } else {
            nlMovies = xmlDoc.getElementsByTagName(DOMHelper.TYPE_MOVIE);
            dto.setTvShow(false);
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

        // parsed watched
        value = DOMHelper.getValueFromElement(eCommon, "watched");
        boolean watched = Boolean.parseBoolean(value);
        
        if (dto.isTvShow()) {
            // TV show specific

            // specific TVDB id
            value = DOMHelper.getValueFromElement(eCommon, "tvdbid");
            if (StringUtils.isNotBlank(value)) {
                dto.addId(TheTVDbScanner.SCANNER_ID, value);
            }
        } else {
            // movie specific
        
            // movie watched status
            dto.setWatched(watched);
        }
        
        // parse sets
        parseSets(eCommon.getElementsByTagName("set"), dto);

        // parse rating
        int rating = MetadataTools.parseRating(DOMHelper.getValueFromElement(eCommon, "rating"));
        dto.setRating(rating);

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
        value = DOMHelper.getValueFromElement(eCommon, "studio", "company");
        dto.setCompany(value);

        // parse genres
        parseGenres(eCommon.getElementsByTagName("genre"), dto);

        // premiered / release date
        movieDate(DOMHelper.getValueFromElement(eCommon, "premiered", "releasedate"), dto);


        /* TODO
        if (OverrideTools.checkOverwriteCountry(movie, NFO_PLUGIN_ID)) {
            movie.setCountries(DOMHelper.getValueFromElement(eCommon, "country"), NFO_PLUGIN_ID);
        }
        */
        
        // parse Top250
        value = DOMHelper.getValueFromElement(eCommon, "top250");
        if (StringUtils.isNumeric(value)) {
            try {
                dto.setTop250(Integer.parseInt(value));
            } catch (Exception e) {
                // ignore this error
            }
        }
        
        // director and writers
        if (!this.configServiceWrapper.getBooleanProperty("nfo.skip.crew", false)) {
            if (this.configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
                parseDirectors(eCommon.getElementsByTagName("director"), dto);
            }
            
            if (this.configServiceWrapper.isCastScanEnabled(JobType.WRITER)) {
                List<Node> writerNodes = new ArrayList<>();
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
                parseWriters(writerNodes, dto);
            }
        }

        // parse actors
        if (!this.configServiceWrapper.getBooleanProperty("nfo.skip.cast", false)
            && this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) 
        {
            parseActors(eCommon.getElementsByTagName("actor"), dto);
        }
        
        // parse artwork URLs
        if (!this.configServiceWrapper.getBooleanProperty("nfo.skip.posterURL", true)) {
            dto.addPosterURL(DOMHelper.getValueFromElement(eCommon, "thumb"));
        }
        if (!this.configServiceWrapper.getBooleanProperty("nfo.skip.fanartURL", true)) {
            dto.addFanartURL(DOMHelper.getValueFromElement(eCommon, "fanart"));
        }

        // parse trailer
        if (!this.configServiceWrapper.getBooleanProperty("nfo.skip.trailerURL", false)) {
            parseTrailers(eCommon.getElementsByTagName("trailer"), dto);
        }
        
        // parse all episodes
        if (dto.isTvShow()) {
            parseAllEpisodeDetails(dto, xmlDoc.getElementsByTagName(DOMHelper.TYPE_EPISODE), watched);
        }
    }

    /**
     * Parse all the title information from the XML NFO file
     *
     * @param eCommon
     * @param dto
     */
    private static void parseTitle(Element eCommon, InfoDTO dto) {
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
    private static void parseIds(NodeList nlElements, InfoDTO dto, boolean isTV) {
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
                            dto.setSkipAllScans();
                        } else {
                            // choose default scanner id
                            if (isTV) {
                                movieDb = TheTVDbScanner.SCANNER_ID;
                            } else {
                                movieDb = ImdbScanner.SCANNER_ID;
                            }
                            dto.addId(movieDb, movieId);
                            LOG.debug("Found {} ID: {}", movieDb, movieId);
                        }
                    } else {
                        dto.addId(movieDb, movieId);
                        LOG.debug("Found {} ID: {}", movieDb, movieId);
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
     * Parse Certification from the XML NFO file
     *
     * @param eCommon
     * @param movie
     */
    private void parseCertification(Element eCommon, InfoDTO dto) {
        boolean certificationMPAA = this.configServiceWrapper.getBooleanProperty("yamj3.certification.mpaa", false);
        String tempCert;
        
        if (certificationMPAA) {
            tempCert = DOMHelper.getValueFromElement(eCommon, "mpaa");
            if (StringUtils.isNotBlank(tempCert)) {
                String mpaa = MetadataTools.processMpaaCertification(tempCert);
                dto.addCertificatioInfo("MPAA", StringUtils.trimToNull(mpaa));
            }
        }

        tempCert = DOMHelper.getValueFromElement(eCommon, "certification");
        if (StringUtils.isNotBlank(tempCert)) {
            // scan for given countries
            for (Locale locale : this.localeService.getCertificationLocales()) {
                for (String country : this.localeService.getCountryNames(locale)) {
                    int countryPos = StringUtils.lastIndexOfIgnoreCase(tempCert, country);
                    if (countryPos >= 0) {
                        // We've found the country, so extract just that tag
                        String certification = tempCert.substring(countryPos);
                        int pos = certification.indexOf(':');
                        if (pos > 0) {
                            int endPos = certification.indexOf("/");
                            if (endPos > 0) {
                                // this is in the middle of the string
                                certification = certification.substring(pos + 1, endPos);
                            } else {
                                // this is at the end of the string
                                certification = certification.substring(pos + 1);
                            }
                        }
                        dto.addCertificatioInfo(locale.getCountry(), StringUtils.trimToNull(certification));
                    }
                }
                
                if (certificationMPAA && StringUtils.containsIgnoreCase(tempCert, "Rated")) {
                    // extract the MPAA rating from the certification
                    String mpaa = MetadataTools.processMpaaCertification(tempCert);
                    dto.addCertificatioInfo("MPAA", StringUtils.trimToNull(mpaa));
                }
            }
        }
    }

    /**
     * Parse Genres from the XML NFO file
     * Caters for multiple genres on the same line and multiple lines.
     *
     * @param nlElements
     * @param dto
    */
    private void parseGenres(NodeList nlElements, InfoDTO dto) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eGenre = (Element) nElements;
                NodeList nlNames = eGenre.getElementsByTagName("name");
                if ((nlNames != null) && (nlNames.getLength() > 0)) {
                    parseGenres(nlNames, dto);
                } else if (eGenre.getTextContent() != null) {
                    for (String genre : eGenre.getTextContent().split(SPLIT_GENRE)) {
                        dto.adGenre(genre);
                    }
                }
            }
        }
    }

    /**
     * Convert the date string to a date and update the movie object
     *
     * @param dateString
     * @param dto
     */
    private static void movieDate(final String dateString, InfoDTO dto) {
        Date releaseDate = MetadataTools.parseToDate(dateString);
        if (releaseDate != null) {
            dto.setReleaseDate(releaseDate);
            dto.setYear(MetadataTools.extractYearAsString(releaseDate));
        }
    }

    /**
     * Parse Sets from the XML NFO file
     *
     * @param nlElements
     * @param dto
     */
    private static void parseSets(NodeList nlElements, InfoDTO dto) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eId = (Element) nElements;

                String setOrder = eId.getAttribute("order");
                if (StringUtils.isNumeric(setOrder)) {
                    dto.addSetInfo(eId.getTextContent(), Integer.parseInt(setOrder));
                } else {
                    dto.addSetInfo(eId.getTextContent());
                }
            }
        }
    }

    /**
     * Parse directors from the XML NFO file
     *
     * @param nlElements
     * @param dto
     */
    private static void parseDirectors(NodeList nlElements, InfoDTO dto) {
        // check if we have a node
        if (nlElements == null || nlElements.getLength() == 0) {
            return;
        }

        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eDirector = (Element) nElements;
                dto.addDirector(eDirector.getTextContent());
            }
        }
    }

    /**
     * Parse writers from the XML NFO file
     *
     * @param nlElements
     * @param dto
     */
    private static void parseWriters(List<Node> nlWriters, InfoDTO dto) {
        // check if we have nodes
        if (nlWriters == null || nlWriters.isEmpty()) {
            return;
        }

        for (Node nWriter : nlWriters) {
            NodeList nlChilds = ((Element)nWriter).getChildNodes();
            Node nChilds;
            for (int looper = 0; looper < nlChilds.getLength(); looper++) {
                nChilds = nlChilds.item(looper);
                if (nChilds.getNodeType() == Node.TEXT_NODE) {
                    dto.addWriter(nChilds.getNodeValue());
                }
            }
        }
    }

    /**
     * Parse Actors from the XML NFO file.
     *
     * @param nlElements
     * @param dto
     */
    private static void parseActors(NodeList nlElements, InfoDTO dto) {
        // check if we have a node
        if (nlElements == null || nlElements.getLength() == 0) {
            return;
        }

        for (int actorLoop = 0; actorLoop < nlElements.getLength(); actorLoop++) {
            // Get all the name/role/thumb nodes
            Node nActors = nlElements.item(actorLoop);
            NodeList nlCast = nActors.getChildNodes();
            Node nElement;

            String aName = null;
            String aRole = null;
            String aThumb = null;
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
                                dto.addActor(aName, aRole, aThumb);
                            }
                            aName = eCast.getTextContent();
                            aRole = null;
                            aThumb = null;
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

            // after all add the last scraped actor
            dto.addActor(aName, aRole, aThumb);
        }
    }

    /**
     * Parse Trailers from the XML NFO file
     *
     * @param nlElements
     * @param dto
     */
    private static void parseTrailers(NodeList nlElements, InfoDTO dto) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eTrailer = (Element) nElements;
                dto.addTrailerURL(eTrailer.getTextContent());
            }
        }
    }

    /**
     * Process all the Episode Details
     *
     * @param dto
     * @param nlEpisodeDetails
     */
    private static void parseAllEpisodeDetails(InfoDTO dto, NodeList nlEpisodeDetails, boolean watched) {
        Node nEpisodeDetails;
        for (int looper = 0; looper < nlEpisodeDetails.getLength(); looper++) {
            nEpisodeDetails = nlEpisodeDetails.item(looper);
            if (nEpisodeDetails.getNodeType() == Node.ELEMENT_NODE) {
                Element eEpisodeDetail = (Element) nEpisodeDetails;
                InfoEpisodeDTO episodeDTO = parseSingleEpisodeDetail(eEpisodeDetail);
                episodeDTO.setWatched(watched);
                dto.addEpisode(episodeDTO);
            }
        }
    }

    /**
     * Parse a single episode detail element
     *
     * @param eEpisodeDetails
     * @return
     */
    private static InfoEpisodeDTO parseSingleEpisodeDetail(Element eEpisodeDetails) {
        if (eEpisodeDetails == null) {
            return null;
        }
        InfoEpisodeDTO episodeDTO = new InfoEpisodeDTO();
        
        episodeDTO.setTitle(DOMHelper.getValueFromElement(eEpisodeDetails, "title"));

        String tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "season");
        if (StringUtils.isNumeric(tempValue)) {
            episodeDTO.setSeason(Integer.parseInt(tempValue));
        }

        tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "episode");
        if (StringUtils.isNumeric(tempValue)) {
            episodeDTO.setEpisode(Integer.parseInt(tempValue));
        }

        episodeDTO.setPlot(DOMHelper.getValueFromElement(eEpisodeDetails, "plot"));

        tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "rating");
        episodeDTO.setRating(MetadataTools.parseRating(tempValue));

        tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "aired");
        if (StringUtils.isNotBlank(tempValue)) {
            try {
                episodeDTO.setFirstAired(MetadataTools.parseToDate(tempValue.trim()));
            } catch (Exception ignore) {
                // ignore error if date has invalid format
            }
        }

        episodeDTO.setAirsAfterSeason(DOMHelper.getValueFromElement(eEpisodeDetails, "airsafterseason", "airsAfterSeason"));
        episodeDTO.setAirsBeforeSeason(DOMHelper.getValueFromElement(eEpisodeDetails, "airsbeforeseason", "airsBeforeSeason"));
        episodeDTO.setAirsBeforeEpisode(DOMHelper.getValueFromElement(eEpisodeDetails, "airsbeforeepisode", "airsBeforeEpisode"));

        return episodeDTO;
    }

    /**
     * Scan a text file for information
     *
     * @param nfoContent
     * @param dto
     * @return
     */
    private boolean readTextNfo(String nfoContent, InfoDTO dto) {
        boolean foundInfo = onlineScannerService.scanNFO(nfoContent, dto);
        
        LOG.trace("Scanning NFO for Poster URL");
        int urlStartIndex = 0;
        while (urlStartIndex >= 0 && urlStartIndex < nfoContent.length()) {
            int currentUrlStartIndex = StringUtils.indexOfIgnoreCase(nfoContent, "http://", urlStartIndex);
            if (currentUrlStartIndex >= 0) {
                int currentUrlEndIndex = StringUtils.indexOfIgnoreCase(nfoContent, "jpg", currentUrlStartIndex);
                if (currentUrlEndIndex >= 0) {
                    int nextUrlStartIndex = StringUtils.indexOfIgnoreCase(nfoContent, "http://", currentUrlStartIndex); 
                    // look for shortest http://
                    while ((nextUrlStartIndex != -1) && (nextUrlStartIndex < currentUrlEndIndex + 3)) {
                        currentUrlStartIndex = nextUrlStartIndex;
                        nextUrlStartIndex = StringUtils.indexOfIgnoreCase(nfoContent, "http://", currentUrlStartIndex + 1); 
                    }

                    // Check to see if the URL has <fanart> at the beginning and ignore it if it does (Issue 706)
                    if ((currentUrlStartIndex < 8)
                            || (nfoContent.substring(currentUrlStartIndex - 8, currentUrlStartIndex).compareToIgnoreCase("<fanart>") != 0)) {
                        String foundUrl = nfoContent.substring(currentUrlStartIndex, currentUrlEndIndex + 3);

                        // Check for some invalid characters to see if the URL is valid
                        if (foundUrl.contains(" ") || foundUrl.contains("*")) {
                            urlStartIndex = currentUrlStartIndex + 3;
                        } else {
                            LOG.debug("Poster URL found in NFO: {} ", foundUrl);
                            dto.addPosterURL(foundUrl);
                            urlStartIndex = -1;
                            foundInfo = Boolean.TRUE;
                        }
                    } else {
                        LOG.debug("Poster URL ignored in NFO because it's a fanart URL");
                        // Search for the URL again
                        urlStartIndex = currentUrlStartIndex + 3;
                    }
                } else {
                    urlStartIndex = currentUrlStartIndex + 3;
                }
            } else {
                urlStartIndex = -1;
            }
        }
        
        return foundInfo;
    }
}
