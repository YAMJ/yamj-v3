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
package org.yamj.core.service.mediaimport;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.tools.StringTools;
import org.yamj.common.util.KeywordMap;
import org.yamj.common.util.PatternUtils;
import org.yamj.common.util.TokensPatternMap;
import org.yamj.core.database.model.type.FileType;

@Service("filenameScanner")
public class FilenameScanner {

    private static final Logger LOG = LoggerFactory.getLogger(FilenameScanner.class);
    // Allow the use of [IMDB tt123456] to define the IMDB reference
    private static final Pattern ID_PATTERN = PatternUtils.patt("\\[ID ([^\\[\\]]*)\\]");
    // Search for tt followed by 6 or 7 digits and then a word boundary
    private static final Pattern IMDB_PATTERN = PatternUtils.patt("(?i)(tt\\d{6,7})\\b");
    // Everything in format [SET something] (case insensitive)
    private static final Pattern SET_PATTERN = PatternUtils.ipatt("\\[SET(?:\\s|-)([^\\[\\]]*)\\]");
    // Number at the end of string preceded with '-'
    private static final Pattern SET_INDEX_PATTERN = PatternUtils.patt("-\\s*(\\d+)\\s*$");
    private static final Pattern TV_PATTERN = PatternUtils.ipatt("(?<![0-9])((s[0-9]{1,4})|[0-9]{1,2})(?:(\\s|\\.|x))??((?:(e|x)\\s??[0-9]+)+)|\\s+((?:e[0-9]+)+)");
    private static final Pattern SEASON_PATTERN = PatternUtils.ipatt("s{0,1}([0-9]+)(\\s|\\.)??[ex-]");
    private static final Pattern EPISODE_PATTERN = PatternUtils.ipatt("[ex]\\s??([0-9]+)");
    
    // Last 4 digits or last 4 digits in parenthesis
    private static final Pattern MOVIE_YEAR_PATTERN = PatternUtils.patt("\\({0,1}(\\d{4})(?:/|\\\\|\\||-){0,1}(I*)\\){0,1}$");
    // One or more '.[]_ '
    private static final Pattern TITLE_CLEANUP_DIV_PATTERN = PatternUtils.patt("([\\. _\\[\\]]+)");
    // '-' or '(' at the end
    private static final Pattern TITLE_CLEANUP_CUT_PATTERN = PatternUtils.patt("-$|\\($");
    // All symbols between '-' and '/' but not after '/TVSHOW/' or '/PART/'
    private static final Pattern SECOND_TITLE_PATTERN = PatternUtils.patt("(?<!/TVSHOW/|/PART/)-([^/]+)");
    
    /**
     * Parts/disks markers.
     *
     * CAUTION: Grouping is used for part number detection/parsing.
     */
    private static final List<Pattern> PART_PATTERNS = new ArrayList<Pattern>() {
        private static final long serialVersionUID = 2534565160759765860L;
        { //NOSONAR
            add(PatternUtils.iwpatt("CD ([0-9]+)"));
            add(PatternUtils.iwpatt("(?:(?:CD)|(?:DISC)|(?:DISK)|(?:PART))([0-9]+)"));
            add(PatternUtils.tpatt("([0-9]{1,2})[ \\.]{0,1}DVD"));
        }
    };
    
    /**
     * Detect if the file/folder name is incomplete and additional info must be
     * taken from parent folder.
     *
     * CAUTION: Grouping is used for part number detection/parsing.
     */
    @SuppressWarnings("synthetic-access")
    private static final List<Pattern> PARENT_FOLDER_PART_PATTERNS = new ArrayList<Pattern>() {
        private static final long serialVersionUID = 6125546333783004357L;
        { //NOSONAR
            for (Pattern p : PART_PATTERNS) {
                add(Pattern.compile("^" + p, CASE_INSENSITIVE));
            }
            add(Pattern.compile("^" + TV_PATTERN, CASE_INSENSITIVE));
        }
    };
    
    private static final Map<Integer, Pattern> FPS_MAP = new HashMap<Integer, Pattern>() {
        private static final long serialVersionUID = -514057952318403685L;
        { //NOSONAR
            for (int i : new int[]{23, 24, 25, 29, 30, 50, 59, 60}) {
                put(i, PatternUtils.iwpatt("p" + i + "|" + i + "p"));
            }
        }
    };
    
    private static final Map<String, Pattern> AUDIO_CODEC_MAP = new HashMap<String, Pattern>() {
        private static final long serialVersionUID = 8916278631320047158L;
        { //NOSONAR
            for (String s : new String[]{"AC3", "DTS", "DD", "AAC", "FLAC"}) {
                put(s, PatternUtils.iwpatt(s));
            }
        }
    };
    
    private static final Map<String, Pattern> VIDEO_CODEC_MAP = new HashMap<String, Pattern>() {
        private static final long serialVersionUID = 7370884465939448891L;
        { //NOSONAR
            put("XviD", PatternUtils.iwpatt("XVID(?:-[^"+Pattern.quote(PatternUtils.WORD_DELIMITERS_STRING)+"]*)?"));
            // add DIVX-lpdm support
            put("DivX", PatternUtils.iwpatt("(?:DIVX|DIVX6)(?:-[^"+Pattern.quote(PatternUtils.WORD_DELIMITERS_STRING)+"]*)?"));
            put("H.264", PatternUtils.iwpatt("H264|H\\.264|X264"));
        }
    };
    
    private static final Map<String, Pattern> HD_RESOLUTION_MAP = new HashMap<String, Pattern>() {
        private static final long serialVersionUID = 3476960701738952741L;
        { //NOSONAR
            for (String s : new String[]{"720p", "1080i", "1080p", "HD", "1280x720", "1920x1080"}) {
                put(s, PatternUtils.iwpatt(s));
            }
        }
    };

    /**
     * Mapping exact tokens to language.
     *
     * Strict mapping is case sensitive and must be obvious, it must avoid confusing movie name words and language markers.
     *
     * For example the English word "it" and Italian language marker "it", or "French" as part of the title and "french" as language
     * marker.
     *
     * However, described above is important only by file naming with token delimiters (see tokens description constants
     * TOKEN_DELIMITERS*). Language detection in non-token separated titles will be skipped automatically.
     *
     * Language markers, found with this pattern are counted as token delimiters (they will cut movie title)
     */
    private final TokensPatternMap strictLanguageMap = new TokensPatternMap() {
        private static final long serialVersionUID = 3630995345545037071L;

        @Override
        protected void put(String key, Collection<String> tokens) {
            StringBuilder tokenBuilder = new StringBuilder();
            for (String s : tokens) {
                if (tokenBuilder.length() > 0) {
                    tokenBuilder.append('|');
                }
                tokenBuilder.append(Pattern.quote(s));
            }
            put(key, PatternUtils.tpatt(tokenBuilder.toString()));
        }
    };

    /**
     * Mapping loose language markers.
     *
     * The second pass of language detection is being started after movie title detection. Language markers will be scanned with
     * loose pattern in order to find out more languages without chance to confuse with movie title.
     *
     * Markers in this map are case insensitive.
     */
    private final TokensPatternMap looseLanguageMap = new TokensPatternMap() {
        private static final long serialVersionUID = 1383819843117148442L;

        @Override
        protected void put(String key, Collection<String> tokens) {
            StringBuilder tokenBuilder = new StringBuilder();
            for (String token : tokens) {
                // Only add the token if it's not there already
                String quotedToken = Pattern.quote(token.toUpperCase());
                if (tokenBuilder.indexOf(quotedToken) < 0) {
                    if (tokenBuilder.length() > 0) {
                        tokenBuilder.append('|');
                    }
                    tokenBuilder.append(quotedToken);
                }
            }
            put(key, PatternUtils.iwpatt(tokenBuilder.toString()));
        }
    };

    private final TokensPatternMap videoSourceMap = new TokensPatternMap() {
        private static final long serialVersionUID = 4166458100829813911L;

        @Override
        public void put(String key, Collection<String> tokens) {
            StringBuilder patt = new StringBuilder(key);
            for (String token : tokens) {
                patt.append("|");
                patt.append(token);
            }
            put(key, PatternUtils.iwpatt(patt.toString()));
        }
    };
    
    private Collection<String> videoExtensions = new HashSet<>();
    private Collection<String> subtitleExtensions = new HashSet<>();
    private Collection<String> imageExtensions = new HashSet<>();
    private Collection<Pattern> skipPatterns = new ArrayList<>();
    private Collection<Pattern> movieVersionPatterns = new ArrayList<>();
    private Collection<Pattern> extraPatterns = new ArrayList<>();
    private boolean languageDetection;
    private boolean skipEpisodeTitle;
    private boolean useParentRegex;
    private Pattern useParentPattern;

    @PostConstruct
    public void init() { //NOSONAR
        // resolve extensions
        videoExtensions = StringTools.tokenize(PropertyTools.getProperty("filename.scanner.video.extensions", "avi,divx,xvid,mkv,wmv,m2ts,ts,rm,qt,iso,vob,mpg,mov,mp4,m1v,m2v,m4v,m2p,top,trp,m2t,mts,asf,rmp4,img,mk3d,rar,001"), ",;|");
        subtitleExtensions = StringTools.tokenize(PropertyTools.getProperty("filename.scanner.subtitle.extensions", "srt,sub,ssa,smi,pgs"), ",;|");
        imageExtensions = StringTools.tokenize(PropertyTools.getProperty("filename.scanner.image.extensions", "jpg,jpeg,gif,bmp,png"), ",;|");

        // other properties
        languageDetection = PropertyTools.getBooleanProperty("filename.scanner.language.detection", true);
        skipEpisodeTitle = PropertyTools.getBooleanProperty("filename.scanner.skip.episodeTitle", false);

        // parent patterns
        useParentRegex = PropertyTools.getBooleanProperty("filename.scanner.useParentRegex", false);
        String patternString = PropertyTools.getProperty("filename.scanner.parentRegex", "");
        if (StringUtils.isNotBlank(patternString)) {
            useParentPattern = PatternUtils.ipatt(patternString);
        } else {
            useParentRegex = false;
        }

        // build the skip patterns
        boolean caseSensitive = PropertyTools.getBooleanProperty("filename.scanner.skip.caseSensitive", true);
        for (String token : tokenizeToStringArray(PropertyTools.getProperty("filename.scanner.skip.keywords", ""), ",;| ")) {
            if (caseSensitive) {
                skipPatterns.add(PatternUtils.wpatt(Pattern.quote(token)));
            } else {
                skipPatterns.add(PatternUtils.iwpatt(Pattern.quote(token)));
            }
        }
        caseSensitive = PropertyTools.getBooleanProperty("filename.scanner.skip.caseSensitive.regex", true);
        for (String token : tokenizeToStringArray(PropertyTools.getProperty("filename.scanner.skip.keywords.regex", ""), ",;| ")) {
            if (caseSensitive) {
                skipPatterns.add(PatternUtils.patt(token));
            } else {
                skipPatterns.add(PatternUtils.ipatt(token));
            }
        }

        // build version keywords pattern
        for (String token : tokenizeToStringArray(PropertyTools.getProperty("filename.scanner.version.keywords", "director's cut,directors cut,extended cut,final cut,remastered,extended version,special edition"), ",;|")) {
            movieVersionPatterns.add(PatternUtils.iwpatt(token.replace(" ", PatternUtils.WORD_DELIMITERS_MATCH_PATTERN.pattern())));
        }

        // build extra keywords pattern
        for (String token : tokenizeToStringArray(PropertyTools.getProperty("filename.scanner.extra.keywords", "trailer,extra,bonus"), ",;|")) {
            extraPatterns.add(PatternUtils.pattInSBrackets(Pattern.quote(token)));
        }

        // set source keywords
        KeywordMap sourceKeywords = PropertyTools.getKeywordMap("filename.scanner.source.keywords", "HDTV,PDTV,DVDRip,DVDSCR,DSRip,CAM,R5,LINE,HD2DVD,DVD,DVD5,DVD9,HRHDTV,MVCD,VCD,TS,VHSRip,BluRay,BDRip,HDDVD,D-THEATER,SDTV");
        videoSourceMap.putAll(sourceKeywords.getKeywords(), sourceKeywords);
        
        KeywordMap languages = PropertyTools.getKeywordMap("language.detection.keywords", null);
        if (!languages.isEmpty()) {
            for (String lang : languages.getKeywords()) {
                String values = languages.get(lang);
                if (values != null) {
                    strictLanguageMap.put(lang, values);
                    looseLanguageMap.put(lang, values);
                } else {
                    LOG.info("No values found for language code '{}'", lang);
                }
            }
        }
    }

    public FileType determineFileType(final String extension) {
        if (extension == null) {
            return FileType.UNKNOWN;
        }

        final String ext = extension.toLowerCase();
        final FileType result;
        if ("nfo".equals(ext)) {
            return FileType.NFO;
        } else if ("watched".equals(extension)) {
            result = FileType.WATCHED;
        } else if ("bluray".equals(extension)) {
        	result = FileType.BLURAY;
        } else if ("dvd".equals(extension)) {
        	result = FileType.DVD;
        } else if (videoExtensions.contains(ext)) {
            result = FileType.VIDEO;
        } else if (subtitleExtensions.contains(ext)) {
            result = FileType.SUBTITLE;
        } else if (imageExtensions.contains(ext)) {
            result = FileType.IMAGE;
        } else {
            result = FileType.UNKNOWN;
        }
        return result;
    }

    public void scan(FilenameDTO dto) {
        // CHECK FOR USE_PARENT_PATTERN matches
        if (useParentRegex && useParentPattern.matcher(dto.getName()).find()) {
            // Just go up one parent
            dto.setRest(dto.getParentName());
            LOG.debug("UseParentPattern matched for {} - Using parent folder name: {}", dto.getName(), dto.getParentName());
        } else {
            dto.setRest(dto.getName());
        }

        // EXTENSION AND CONTAINER
        if (dto.isBluray()) {
            dto.setContainer(FileType.BLURAY.name());
            dto.setVideoSource("BluRay");
        } else if (dto.isDvd()) {
            dto.setContainer(FileType.DVD.name());
            dto.setVideoSource(FileType.DVD.name());
        } else {
            // Extract and strip extension
            String ext = FilenameUtils.getExtension(dto.getRest());
            if (ext.length() > 0) {
                dto.setRest(FilenameUtils.removeExtension(dto.getRest()));
                dto.setContainer(ext.toUpperCase());
            }
        }

        dto.setRest(cleanUp(dto.getRest()));

        // Detect incomplete filenames and add parent folder name to parser
        for (Pattern pattern : PARENT_FOLDER_PART_PATTERNS) {
            final Matcher matcher = pattern.matcher(dto.getRest());
            if (matcher.find()) {
                final String parentName = dto.getParentName();
                if (parentName != null) {
                    dto.setRest(cleanUp(parentName) + "./." + dto.getRest());
                }
                break;
            }
        }

        // Remove version info
        for (Pattern pattern : movieVersionPatterns) {
            Matcher matcher = pattern.matcher(dto.getRest());
            if (matcher.find()) {
                dto.setMovieVersion(matcher.group(0));
            }
            dto.setRest(pattern.matcher(dto.getRest()).replaceAll("./."));
        }

        // EXTRAS (Including Trailers)
        for (Pattern pattern : extraPatterns) {
            Matcher matcher = pattern.matcher(dto.getRest());
            if (matcher.find()) {
                dto.setExtra(true);
                dto.setPartTitle(matcher.group(1));
                dto.setRest(cutMatch(dto.getRest(), matcher, "./EXTRA/."));
                break;
            }
        }

        dto.setFps(seekPatternAndUpdateRest(FPS_MAP, dto.getFps(), dto));
        dto.setAudioCodec(seekPatternAndUpdateRest(AUDIO_CODEC_MAP, dto.getAudioCodec(), dto));
        dto.setVideoCodec(seekPatternAndUpdateRest(VIDEO_CODEC_MAP, dto.getVideoCodec(), dto));
        dto.setHdResolution(seekPatternAndUpdateRest(HD_RESOLUTION_MAP, dto.getHdResolution(), dto));
        dto.setVideoSource(seekPatternAndUpdateRest(videoSourceMap, dto.getVideoSource(), dto, PART_PATTERNS));

        // SEASON + EPISODES
        processSeasonEpisode(dto);
        processPart(dto);
        processSets(dto);
        procesIdDetection(dto);
        processLanguages(dto);
        processTitle(dto);
    }

    /**
     * Process the Season and Episodes
     *
     * @param dto
     */
    private static void processSeasonEpisode(FilenameDTO dto) {
        Matcher matcher = TV_PATTERN.matcher(dto.getRest());
        if (matcher.find()) {
            dto.setRest(cutMatch(dto.getRest(), matcher, "./TVSHOW/."));

            final Matcher smatcher = SEASON_PATTERN.matcher(matcher.group(0));
            // Default season for tv show like "my tv E05 - title"
            String sseason = "01";
            if (smatcher.find()) {
                sseason = smatcher.group(1);
            }
       
            int season = Integer.parseInt(sseason);
            dto.setSeason(season);

            final Matcher ematcher = EPISODE_PATTERN.matcher(matcher.group(0));
            while (ematcher.find()) {
                dto.getEpisodes().add(Integer.valueOf(ematcher.group(1)));
            }
        }
    }

    /**
     * Process the "Part" portion
     *
     * @param dto
     */
    private static void processPart(FilenameDTO dto) {
        Matcher matcher;
        for (Pattern pattern : PART_PATTERNS) {
            matcher = pattern.matcher(dto.getRest());
            if (matcher.find()) {
                dto.setRest(cutMatch(dto.getRest(), matcher, " /PART/ "));
                dto.setPart(NumberUtils.toInt(matcher.group(1)));
                break;
            }
        }
    }

    /**
     * Process sets from the filename
     *
     * @param dto
     */
    private static void processSets(FilenameDTO dto) {
        Matcher matcher = SET_PATTERN.matcher(dto.getRest());
        while (matcher.find()) {
            dto.setRest(cutMatch(dto.getRest(), matcher, PatternUtils.SPACE_SLASH_SPACE));

            Integer order = null;
            String n = matcher.group(1);
            Matcher nmatcher = SET_INDEX_PATTERN.matcher(n);
            if (nmatcher.find()) {
                String sIndex = StringUtils.trim(nmatcher.group(1));
                if (StringUtils.isNumeric(sIndex)) {
                    order = Integer.valueOf(sIndex);
                }
                n = cutMatch(n, nmatcher);
            }
            String setName = n.trim();
            if (StringUtils.isNotBlank(setName)) {
                dto.getSetMap().put(setName, order);
            }

            // Check for the next occurence
            matcher = SET_PATTERN.matcher(dto.getRest());
        }
    }

    /**
     * Movie ID detection
     *
     * @param dto
     */
    private static void procesIdDetection(FilenameDTO dto) {
        Matcher matcher = ID_PATTERN.matcher(dto.getRest());
        if (matcher.find()) {
            dto.setRest(cutMatch(dto.getRest(), matcher, " /ID/ "));

            String[] idString = matcher.group(1).split("[-\\s+]");
            if (idString.length == 2) {
                dto.setId(idString[0].toLowerCase(), idString[1]);
            } else {
                LOG.debug("Error decoding ID from filename: {}", matcher.group(1));
            }
        } else {
            matcher = IMDB_PATTERN.matcher(dto.getRest());
            if (matcher.find()) {
                dto.setRest(cutMatch(dto.getRest(), matcher, " /ID/ "));
                dto.setId("imdb", matcher.group(1));
            }
        }
    }

    /**
     * Process languages
     *
     * @param dto
     */
    private void processLanguages(FilenameDTO dto) {
        if (!languageDetection) {
            return;
        }

        String language = seekPatternAndUpdateRest(strictLanguageMap, null, dto);
        while (StringUtils.isNotBlank(language)) {
            dto.getLanguages().add(StringUtils.trim(language));
            language = seekPatternAndUpdateRest(strictLanguageMap, null, dto);
        }
    }

    /**
     * Process the title from the filename
     *
     * @param dto
     */
    private void processTitle(FilenameDTO dto) {
        String rest = dto.getRest();
        int iextra = dto.isExtra() ? rest.indexOf("/EXTRA/") : rest.length();
        int itvshow = dto.getSeason() >= 0 ? rest.indexOf("/TVSHOW/") : rest.length();
        int ipart = dto.getPart() >= 0 ? rest.indexOf("/PART/") : rest.length();

        int min = iextra < itvshow ? iextra : itvshow;
        min = min < ipart ? min : ipart;

        // Find first token before trailer, TV show and part
        // Name should not start with '-' (exclude wrongly marked part/episode titles)
        String title = "";
        StringTokenizer t = new StringTokenizer(rest.substring(0, min), "/[]");
        while (t.hasMoreElements()) {
            String token = t.nextToken();
            token = cleanUpTitle(token);
            if (token.length() >= 1 && token.charAt(0) != '-') {
                title = token;
                break;
            }
        }

        boolean first = true;
        while (t.hasMoreElements()) {
            String token = t.nextToken();
            token = cleanUpTitle(token);
            // Search year (must be next to a non-empty token)
            if (first) {
                int year = NumberUtils.toInt(token, -1);
                if (year >= 1800 && year <= 3000) {
                    dto.setYear(year);
                }
                first = false;
            }

            if (!languageDetection) {
                break;
            }

            // Loose language search
            if (token.length() >= 2 && token.indexOf('-') < 0) {
                for (Map.Entry<String, Pattern> e : looseLanguageMap.entrySet()) {
                    Matcher matcher = e.getValue().matcher(token);
                    if (matcher.find()) {
                        dto.getLanguages().add(e.getKey());
                    }
                }
            }
        }

        // Search year within title (last 4 digits or 4 digits in parenthesis)
        if (dto.getYear() < 0) {
            Matcher ymatcher = MOVIE_YEAR_PATTERN.matcher(title);
            if (ymatcher.find()) {
                int year = NumberUtils.toInt(ymatcher.group(1), -1);
                if (year >= 1800 && year <= 3000) {
                    dto.setYear(year);
                    title = cutMatch(title, ymatcher);
                }
            }
        }
        dto.setTitle(title);

        // EPISODE TITLE
        if (dto.getSeason() >= 0) {
            itvshow += 8;
            Matcher matcher = SECOND_TITLE_PATTERN.matcher(rest.substring(itvshow));
            while (matcher.find()) {
                title = cleanUpTitle(matcher.group(1));
                if (title.length() > 0) {
                    if (!skipEpisodeTitle) {
                        dto.setEpisodeTitle(title);
                    }
                    break;
                }
            }
        }

        // PART TITLE
        // Just do this for no extra, already named.
        if ((dto.getPart() >= 0) && !dto.isExtra()) {
            ipart += 6;
            Matcher matcher = SECOND_TITLE_PATTERN.matcher(rest.substring(ipart));
            while (matcher.find()) {
                title = cleanUpTitle(matcher.group(1));
                if (title.length() > 0) {
                    dto.setPartTitle(title);
                    break;
                }
            }
        }
    }

    private String cleanUp(final String filename) {
        String rFilename = filename;
        for (Pattern p : skipPatterns) {
            rFilename = p.matcher(rFilename).replaceAll("./.");
        }
        return rFilename;
    }

    /**
     * Replace all dividers with spaces and trim trailing spaces and redundant
     * braces/minuses at the end.
     *
     * @param token String to clean up.
     * @return Prepared title.
     */
    private static String cleanUpTitle(String token) {
        String title = TITLE_CLEANUP_DIV_PATTERN.matcher(token).replaceAll(" ").trim();
        return TITLE_CLEANUP_CUT_PATTERN.matcher(title).replaceAll("").trim();
    }

    private static <T> T seekPatternAndUpdateRest(Map<T, Pattern> map, T oldValue, FilenameDTO dto) {
        for (Map.Entry<T, Pattern> e : map.entrySet()) {
            Matcher matcher = e.getValue().matcher(dto.getRest());
            if (matcher.find()) {
                dto.setRest(cutMatch(dto.getRest(), matcher, "./."));
                return e.getKey();
            }
        }
        return oldValue;
    }

    private static <T> T seekPatternAndUpdateRest(Map<T, Pattern> map, T oldValue, FilenameDTO dto, Collection<Pattern> protectPatterns) {
        for (Map.Entry<T, Pattern> e : map.entrySet()) {
            Matcher matcher = e.getValue().matcher(dto.getRest());
            if (matcher.find()) {
                String restCut = cutMatch(dto.getRest(), matcher, "./.");
                for (Pattern protectPattern : protectPatterns) {
                    if (protectPattern.matcher(dto.getRest()).find() && !protectPattern.matcher(restCut).find()) {
                        return e.getKey();
                    }
                }
                dto.setRest(restCut);
                return e.getKey();
            }
        }
        return oldValue;
    }

    private static String cutMatch(String rest, Matcher matcher) {
        return (rest.substring(0, matcher.start()) + rest.substring(matcher.end())).trim();
    }

    private static String cutMatch(String rest, Matcher matcher, String divider) {
        return rest.substring(0, matcher.start()) + divider + rest.substring(matcher.end());
    }
}
