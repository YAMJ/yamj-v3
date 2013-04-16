package com.moviejukebox.core.service.mediaimport;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

import com.moviejukebox.common.util.KeywordMap;
import com.moviejukebox.common.util.PatternUtils;
import com.moviejukebox.common.util.TokensPatternMap;
import com.moviejukebox.core.database.model.type.FileType;
import com.moviejukebox.core.tools.LanguageTools;
import com.moviejukebox.core.tools.PropertyTools;
import com.moviejukebox.core.tools.StringTools;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("filenameScanner")
public class FilenameScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilenameScanner.class);

    // Allow the use of [IMDB tt123456] to define the IMDB reference
    private static final Pattern ID_PATTERN = PatternUtils.patt("\\[ID ([^\\[\\]]*)\\]");
    // Search for tt followed by 6 or 7 digits and then a word boundary
    private static final Pattern IMDB_PATTERN = PatternUtils.patt("(?i)(tt\\d{6,7})\\b");
    // Everything in format [SET something] (case insensitive)
    private static final Pattern SET_PATTERN = PatternUtils.ipatt("\\[SET(?:\\s|-)([^\\[\\]]*)\\]");
    // Number at the end of string preceded with '-'
    private static final Pattern SET_INDEX_PATTERN = PatternUtils.patt("-\\s*(\\d+)\\s*$");
    private static final Pattern TV_PATTERN = PatternUtils.ipatt("(?<![0-9])((s[0-9]{1,4})|[0-9]{1,2})(?:(\\s|\\.|x))??((?:(e|x)\\s??[0-9]+)+)");
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

        {
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
    private static final List<Pattern> PARENT_FOLDER_PART_PATTERNS = new ArrayList<Pattern>() {
        private static final long serialVersionUID = 6125546333783004357L;

        {
            for (Pattern p : PART_PATTERNS) {
                add(Pattern.compile("^" + p, CASE_INSENSITIVE));
            }
            add(Pattern.compile("^" + TV_PATTERN, CASE_INSENSITIVE));
        }
    };
    private static final Map<Integer, Pattern> FPS_MAP = new HashMap<Integer, Pattern>() {
        private static final long serialVersionUID = -514057952318403685L;

        {
            for (int i : new int[]{23, 24, 25, 29, 30, 50, 59, 60}) {
                put(i, PatternUtils.iwpatt("p" + i + "|" + i + "p"));
            }
        }
    };
    private static final Map<String, Pattern> AUDIO_CODEC_MAP = new HashMap<String, Pattern>() {
        private static final long serialVersionUID = 8916278631320047158L;

        {
            for (String s : new String[]{"AC3", "DTS", "DD", "AAC", "FLAC"}) {
                put(s, PatternUtils.iwpatt(s));
            }
        }
    };
    private static final Map<String, Pattern> VIDEO_CODEC_MAP = new HashMap<String, Pattern>() {
        private static final long serialVersionUID = 7370884465939448891L;

        {
            put("XviD", PatternUtils.iwpatt("XVID"));
            put("DivX", PatternUtils.iwpatt("DIVX|DIVX6"));
            put("H.264", PatternUtils.iwpatt("H264|H\\.264|X264"));
        }
    };
    private static final Map<String, Pattern> HD_RESOLUTION_MAP = new HashMap<String, Pattern>() {
        private static final long serialVersionUID = 3476960701738952741L;

        {
            for (String s : new String[]{"720p", "1080i", "1080p", "HD", "1280x720", "1920x1080"}) {
                put(s, PatternUtils.iwpatt(s));
            }
        }
    };

    private TokensPatternMap videoSourceMap = new TokensPatternMap() {
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

    private Collection<String> videoExtensions = new HashSet<String>();
    private Collection<String> subtitleExtensions = new HashSet<String>();
    private Collection<String> imageExtensions = new HashSet<String>();
    private Collection<Pattern> skipPatterns = new ArrayList<Pattern>();
    private Collection<Pattern> movieVersionPatterns = new ArrayList<Pattern>();
    private Collection<Pattern> extraPatterns = new ArrayList<Pattern>();
    private boolean languageDetection;
    private boolean skipEpisodeTitle;
    private boolean useParentRegex;
    private Pattern useParentPattern;

    @Autowired
    private LanguageTools languageTools;
    
    public FilenameScanner() {
        // resolve extensions
        videoExtensions = StringTools.tokenize(PropertyTools.getProperty("filename.scanner.video.extensions", "avi,divx,xvid,mkv,wmv,m2ts,ts,rm,qt,iso,vob,mpg,mov,mp4,m1v,m2v,m4v,m2p,top,trp,m2t,mts,asf,rmp4,img,mk3d,rar,001"), ",;|");
        subtitleExtensions = StringTools.tokenize(PropertyTools.getProperty("filename.scanner.subtitle.extensions", "srt,sub,ssa,smi,pgs"), ",;|");
        imageExtensions = StringTools.tokenize(PropertyTools.getProperty("filename.scanner.image.extensions", "jpg,jpeg,gif,bmp,png"), ",;|");

        // other properties
        languageDetection = PropertyTools.getBooleanProperty("filename.scanner.language.detection", Boolean.TRUE);
        skipEpisodeTitle = PropertyTools.getBooleanProperty("filename.scanner.skip.episodeTitle", Boolean.FALSE);
        
        // parent patterns
        useParentRegex = PropertyTools.getBooleanProperty("filename.scanner.useParentRegex", Boolean.FALSE);
        String patternString = PropertyTools.getProperty("filename.scanner.parentRegex", "");
        if (StringUtils.isNotBlank(patternString)) {
            useParentPattern = PatternUtils.ipatt(patternString);
        } else {
            useParentRegex = Boolean.FALSE;
        }

        // build the skip patterns
        boolean caseSensitive = PropertyTools.getBooleanProperty("filename.scanner.skip.caseSensitive", Boolean.TRUE);
        for (String token : tokenizeToStringArray(PropertyTools.getProperty("filename.scanner.skip.keywords", ""), ",;| ")) {
            if (caseSensitive) {
                skipPatterns.add(PatternUtils.wpatt(Pattern.quote(token)));
            } else {
                skipPatterns.add(PatternUtils.iwpatt(Pattern.quote(token)));
            }
        }
        caseSensitive = PropertyTools.getBooleanProperty("filename.scanner.skip.caseSensitive.regex", Boolean.TRUE);
        for (String token : tokenizeToStringArray(PropertyTools.getProperty("filename.scanner.skip.keywords.regex", ""), ",;| ")) {
            if (caseSensitive) {
                skipPatterns.add(PatternUtils.patt(token));
            } else {
                skipPatterns.add(PatternUtils.ipatt(token));
            }
        }
        
        // build version keywords pattern
        for (String token : tokenizeToStringArray(PropertyTools.getProperty("filename.scanner.version.keywords", "remastered,directors cut,extended cut,final cut"), ",;| ")) {
            movieVersionPatterns.add(PatternUtils.iwpatt(token.replace(" ", PatternUtils.WORD_DELIMITERS_MATCH_PATTERN.pattern())));
        }

        // build extra keywords pattern
        for (String token : tokenizeToStringArray(PropertyTools.getProperty("filename.scanner.extra.keywords", "trailer,extra,bonus"), ",;| ")) {
            extraPatterns.add(PatternUtils.pattInSBrackets(Pattern.quote(token)));
        }

        // set source keywords
        KeywordMap sourceKeywords = PropertyTools.getKeywordMap("filename.scanner.source.keywords", "HDTV,PDTV,DVDRip,DVDSCR,DSRip,CAM,R5,LINE,HD2DVD,DVD,DVD5,DVD9,HRHDTV,MVCD,VCD,TS,VHSRip,BluRay,HDDVD,D-THEATER,SDTV");
        videoSourceMap.putAll(sourceKeywords.getKeywords(), sourceKeywords);
    }
    
    public FileType determineFileType(String fileName) {
        try {
            int index = fileName.lastIndexOf(".");
            if (index < 0) {
                return FileType.UNKNOWN;
            }
            
            String extension = fileName.substring(index + 1).toLowerCase();

            if ("nfo".equals(extension)) {
                return FileType.NFO;
            }
            
            if (videoExtensions.contains(extension)) {
                return FileType.VIDEO;
            }

            if (subtitleExtensions.contains(extension)) {
                return FileType.SUBTITLE;
            }

            if (imageExtensions.contains(extension)) {
                return FileType.IMAGE;
            }
        } catch (Exception error) {
            LOGGER.error("Failed to determine file type for: "+fileName, error);
        }
        return FileType.UNKNOWN;
    }

     public void scan(FilenameDTO dto) {
         // CHECK FOR USE_PARENT_PATTERN matches
         if (useParentRegex && useParentPattern.matcher(dto.getName()).find()) {
             // Just go up one parent
             dto.setRest(dto.getParentName());
             LOGGER.debug("UseParentPattern matched for " + dto.getName() + " - Using parent folder name: " + dto.getParentName());
         } else {
             dto.setRest(dto.getName());
         }

         // EXTENSION AND CONTAINER
 
         if (dto.isDirectory()) {
             dto.setContainer("DVD");
             dto.setVideoSource("DVD");
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
                if (parentName == null) {
                    break;
                }
                dto.setRest(cleanUp(parentName) + "./." + dto.getRest());
                break;
            }
        }
         
        // Remove version info
        for (Pattern pattern : movieVersionPatterns) {
            dto.setRest(pattern.matcher(dto.getRest()).replaceAll("./."));
        }

        // EXTRAS (Including Trailers)
        for (Pattern pattern : extraPatterns) {
            Matcher matcher = pattern.matcher(dto.getRest());
            if (matcher.find()) {
                dto.setExtra(Boolean.TRUE);
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
        {
            final Matcher matcher = TV_PATTERN.matcher(dto.getRest());
            if (matcher.find()) {
                // logger.finest("It's a TV Show: " + group0);
                dto.setRest(cutMatch(dto.getRest(), matcher, "./TVSHOW/."));

                final Matcher smatcher = SEASON_PATTERN.matcher(matcher.group(0));
                smatcher.find();
                int season = Integer.parseInt(smatcher.group(1));
                dto.setSeason(season);

                final Matcher ematcher = EPISODE_PATTERN.matcher(matcher.group(0));
                while (ematcher.find()) {
                    dto.getEpisodes().add(Integer.parseInt(ematcher.group(1)));
                }
            }
        }

        // PART
        {
            for (Pattern pattern : PART_PATTERNS) {
                final Matcher matcher = pattern.matcher(dto.getRest());
                if (matcher.find()) {
                    dto.setRest(cutMatch(dto.getRest(), matcher, " /PART/ "));
                    dto.setPart(Integer.parseInt(matcher.group(1)));
                    break;
                }
            }
        }

        // SETS
        {
            for (;;) {
                final Matcher matcher = SET_PATTERN.matcher(dto.getRest());
                if (!matcher.find()) {
                    break;
                }
                dto.setRest(cutMatch(dto.getRest(), matcher, PatternUtils.SPACE_SLASH_SPACE));

                FilenameDTO.SetDTO set = new FilenameDTO.SetDTO();
                dto.getSets().add(set);

                String n = matcher.group(1);
                Matcher nmatcher = SET_INDEX_PATTERN.matcher(n);
                if (nmatcher.find()) {
                    set.setIndex(Integer.parseInt(nmatcher.group(1)));
                    n = cutMatch(n, nmatcher);
                }
                set.setTitle(n.trim());
            }
        }

        // Movie ID detection
        {
            Matcher matcher = ID_PATTERN.matcher(dto.getRest());
            if (matcher.find()) {
                dto.setRest(cutMatch(dto.getRest(), matcher, " /ID/ "));

                String idString[] = matcher.group(1).split("[-\\s+]");
                if (idString.length == 2) {
                    dto.setId(idString[0].toLowerCase(), idString[1]);
                } else {
                    LOGGER.debug("Error decoding ID from filename: " + matcher.group(1));
                }
            } else {
                matcher = IMDB_PATTERN.matcher(dto.getRest());
                if (matcher.find()) {
                    dto.setRest(cutMatch(dto.getRest(), matcher, " /ID/ "));
                    dto.setId("imdb", matcher.group(1));
                }
            }
        }

        // LANGUAGES
        if (languageDetection) {
            for (;;) {
                String language = seekPatternAndUpdateRest(this.languageTools.getStrictLanguageMap(), null, dto);
                if (language == null) {
                    break;
                }
                dto.getLanguages().add(language);
            }
        }

        // TITLE
        {
            String rest = dto.getRest();
            int iextra = dto.isExtra() ? rest.indexOf("/EXTRA/") : rest.length();
            int itvshow = dto.getSeason() >= 0 ? rest.indexOf("/TVSHOW/") : rest.length();
            int ipart = dto.getPart() >= 0 ? rest.indexOf("/PART/") : rest.length();

            {
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

                boolean first = Boolean.TRUE;
                while (t.hasMoreElements()) {
                    String token = t.nextToken();
                    token = cleanUpTitle(token);
                    // Search year (must be next to a non-empty token)
                    if (first) {
                        if (token.length() > 0) {
                            try {
                                int year = Integer.parseInt(token);
                                if (year >= 1800 && year <= 3000) {
                                    dto.setYear(year);
                                }
                            } catch (NumberFormatException error) {
                            }
                        }
                        first = Boolean.FALSE;
                    }

                    if (!languageDetection) {
                        break;
                    }

                    // Loose language search
                    if (token.length() >= 2 && token.indexOf('-') < 0) {
                        for (Map.Entry<String, Pattern> e : this.languageTools.getLooseLanguageMap().entrySet()) {
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
                        int year = Integer.parseInt(ymatcher.group(1));
                        if (year >= 1919 && year <= 2099) {
                            dto.setYear(year);
                            title = cutMatch(title, ymatcher);
                        }
                    }
                }
                dto.setTitle(title);
            }

            // EPISODE TITLE
            if (dto.getSeason() >= 0) {
                itvshow += 8;
                Matcher matcher = SECOND_TITLE_PATTERN.matcher(rest.substring(itvshow));
                while (matcher.find()) {
                    String title = cleanUpTitle(matcher.group(1));
                    if (title.length() > 0) {
                        if (!skipEpisodeTitle) {
                            dto.setEpisodeTitle(title);
                        }
                        break;
                    }
                }
            }

            // PART TITLE
            if (dto.getPart() >= 0) {
                // Just do this for no extra, already named.
                if (!dto.isExtra()) {
                    ipart += 6;
                    Matcher matcher = SECOND_TITLE_PATTERN.matcher(rest.substring(ipart));
                    while (matcher.find()) {
                        String title = cleanUpTitle(matcher.group(1));
                        if (title.length() > 0) {
                            dto.setPartTitle(title);
                            break;
                        }
                    }
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

    private static  <T> T seekPatternAndUpdateRest(Map<T, Pattern> map, T oldValue, FilenameDTO dto, Collection<Pattern> protectPatterns) {
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
