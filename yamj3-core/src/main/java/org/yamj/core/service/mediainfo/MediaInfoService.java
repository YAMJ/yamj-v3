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
package org.yamj.core.service.mediainfo;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.DateTimeTools;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.StatusType;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.MediaStorageService;
import org.yamj.core.service.file.FileTools;
import org.yamj.core.tools.AspectRatioTools;
import org.yamj.core.tools.Constants;

@Service("mediaInfoService")
public class MediaInfoService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaInfoService.class);

    private static final Pattern PATTERN_CHANNELS = Pattern.compile(".*(\\d{1,2}).*");
    // mediaInfo command line, depend on OS
    private static final String MI_FILENAME_WINDOWS = "MediaInfo.exe";
    private static final String MI_RAR_FILENAME_WINDOWS = "MediaInfo-rar.exe";
    private static final String MI_FILENAME_LINUX = "mediainfo";
    private static final String MI_RAR_FILENAME_LINUX = "mediainfo-rar";
    // media info settings
    private static final File MEDIAINFO_PATH = new File(PropertyTools.getProperty("mediainfo.home", "./mediaInfo/"));
    private final List<String> execMediaInfo = new ArrayList<>();
    private boolean isMediaInfoRar = Boolean.FALSE;
    private boolean isActivated = Boolean.TRUE;
    private static final List<String> RAR_DISK_IMAGES = new ArrayList<>();

    @Autowired
    private MediaStorageService mediaStorageService;
    @Autowired
    private AspectRatioTools aspectRatioTools;
    @Autowired
    private LocaleService localeService;
    
    @PostConstruct
    public void init() {
        LOG.info("Initialize MediaInfo service");

        String OS_NAME = System.getProperty("os.name");
        LOG.debug("Operating System Name   : {}", OS_NAME);
        LOG.debug("Operating System Version: {}", System.getProperty("os.version"));
        LOG.debug("Operating System Type   : {}", System.getProperty("os.arch"));
        LOG.debug("Media Info Path         : {}", MEDIAINFO_PATH);

        File mediaInfoFile;
        if (OS_NAME.contains("Windows")) {
            mediaInfoFile = new File(MEDIAINFO_PATH.getAbsolutePath() + File.separator + MI_RAR_FILENAME_WINDOWS);
            if (!mediaInfoFile.exists()) {
                //  fall back to the normal filename
                mediaInfoFile = new File(MEDIAINFO_PATH.getAbsolutePath() + File.separator + MI_FILENAME_WINDOWS);
            } else {
                // enable the extra mediainfo-rar features
                isMediaInfoRar = Boolean.TRUE;
            }
        } else {
            mediaInfoFile = new File(MEDIAINFO_PATH.getAbsolutePath() + File.separator + MI_RAR_FILENAME_LINUX);
            if (!mediaInfoFile.exists()) {
                // Fall back to the normal filename
                mediaInfoFile = new File(MEDIAINFO_PATH.getAbsolutePath() + File.separator + MI_FILENAME_LINUX);
            } else {
                // enable the extra mediainfo-rar features
                isMediaInfoRar = Boolean.TRUE;
            }
        }

        if (!mediaInfoFile.canExecute()) {
            LOG.info("Couldn't find CLI mediaInfo executable tool: Media file data won't be extracted");
            isActivated = Boolean.FALSE;
        } else {
            if (OS_NAME.contains("Windows")) {
                execMediaInfo.add("cmd.exe");
                execMediaInfo.add("/E:1900");
                execMediaInfo.add("/C");
                execMediaInfo.add(mediaInfoFile.getName());
                execMediaInfo.add("-f");
            } else {
                execMediaInfo.add("./" + mediaInfoFile.getName());
                execMediaInfo.add("-f");
            }

            if (isMediaInfoRar) {
                LOG.info("MediaInfo-rar tool found, additional scanning functions enabled.");
            } else {
                LOG.info("MediaInfo tool will be used to extract video data. But not RAR and ISO formats");
            }
            isActivated = Boolean.TRUE;
        }

        // Add a list of supported extensions
        for (String ext : PropertyTools.getProperty("mediainfo.rar.diskExtensions", "iso,img,rar,001").split(",")) {
            RAR_DISK_IMAGES.add(ext.toLowerCase());
        }
    }

    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        mediaStorageService.errorMediaFile(queueElement.getId());
    }

    public void scanMediaInfo(Long id) {
        MediaFile mediaFile = mediaStorageService.getRequiredMediaFile(id);

        StageFile stageFile = mediaFile.getVideoFile();
        if (stageFile == null) {
            LOG.error("No valid video file found for media file: {}", mediaFile.getFileName());
            mediaFile.setStatus(StatusType.INVALID);
            mediaStorageService.update(mediaFile);
            return;
        }

        // check if stage file can be read by MediaInfo
        boolean scannable = FileTools.isFileScannable(stageFile);
        if (scannable && !this.isActivated) {
        	LOG.debug("MediaInfo not activate for scanning video file '{}'", stageFile.getFullPath());
        	mediaFile.setStatus(StatusType.INVALID);
            mediaStorageService.updateMediaFile(mediaFile);
        	// nothing to do anymore
        	return;
        } else if (!scannable && StringUtils.isBlank(stageFile.getContent())) {
            LOG.debug("Video file '{}' is not scannable", stageFile.getFullPath());
        	mediaFile.setStatus(StatusType.INVALID);
            mediaStorageService.updateMediaFile(mediaFile);
            // nothing to do anymore
            return;
        }

        LOG.debug("Scanning media file {}", stageFile.getFullPath());

        boolean scanned = false;
        try (MediaInfoStream stream = (stageFile.getContent() == null ? createStream(stageFile.getFullPath()) : new MediaInfoStream(stageFile.getContent()))) {
            Map<String, String> infosGeneral = new HashMap<>();
            List<Map<String, String>> infosVideo = new ArrayList<>();
            List<Map<String, String>> infosAudio = new ArrayList<>();
            List<Map<String, String>> infosText = new ArrayList<>();

            parseMediaInfo(stream, infosGeneral, infosVideo, infosAudio, infosText);

            updateMediaFile(mediaFile, infosGeneral, infosVideo, infosAudio, infosText);

            scanned = true;
        } catch (Exception error) {
            LOG.error("Failed reading mediainfo output: {}", stageFile);
            LOG.warn("MediaInfo error", error);
        }

        if (scanned) {
            mediaFile.setStatus(StatusType.DONE);
        } else {
            mediaFile.setStatus(StatusType.ERROR);
        }

        mediaStorageService.updateMediaFile(mediaFile);
    }

    private void updateMediaFile(MediaFile mediaFile, Map<String, String> infosGeneral, List<Map<String, String>> infosVideo,
            List<Map<String, String>> infosAudio, List<Map<String, String>> infosText) {

        String infoValue;

        // get container format from general section
        infoValue = infosGeneral.get("Format");
        if (StringUtils.isNotBlank(infoValue)) {
            mediaFile.setContainer(infoValue);
        }

        // get overall bit rate from general section
        infoValue = infosGeneral.get("Overall bit rate");
        mediaFile.setOverallBitrate(getBitRate(infoValue));

        // get runtime either from video info or general section
        String runtime = getRuntime(infosGeneral, infosVideo);
        if (StringUtils.isNotBlank(runtime)) {
            mediaFile.setRuntime(DateTimeTools.processRuntime(runtime));
        } else {
            mediaFile.setRuntime(-1);
        }

        // get Info from first video stream only
        // TODO can evolve to get info from longest video stream
        if (!infosVideo.isEmpty()) {
            Map<String, String> infosMainVideo = infosVideo.get(0);

            // codec
            mediaFile.setCodec(infosMainVideo.get("Codec ID"));
            mediaFile.setCodecFormat(infosMainVideo.get("Format"));
            mediaFile.setCodecProfile(infosMainVideo.get("Format profile"));

            // width
            mediaFile.setWidth(-1);
            try {
                infoValue = infosMainVideo.get("Width");
                if (StringUtils.isNumeric(infoValue)) {
                    mediaFile.setWidth(Integer.parseInt(infoValue));
                }
            } catch (NumberFormatException error) {
                LOG.trace("Failed to parse width: {}", infoValue, error);
            }

            // height
            mediaFile.setHeight(-1);
            try {
                infoValue = infosMainVideo.get("Height");
                if (StringUtils.isNumeric(infoValue)) {
                    mediaFile.setHeight(Integer.parseInt(infoValue));
                }
            } catch (NumberFormatException ex) {
                LOG.trace("Failed to parse height: {}", infoValue, ex);
            }

            // frame rate
            infoValue = infosMainVideo.get("Frame rate");
            if (StringUtils.isBlank(infoValue)) {
                // use original frame rate
                infoValue = infosMainVideo.get("Original frame rate");
            }
            if (StringUtils.isNotBlank(infoValue)) {
                try {
                    int inxDiv = infoValue.indexOf(Constants.SPACE_SLASH_SPACE);
                    if (inxDiv > -1) {
                        infoValue = infoValue.substring(0, inxDiv);
                    }
                    mediaFile.setFps(Float.parseFloat(infoValue));
                } catch (NumberFormatException error) {
                    LOG.debug("Failed to parse frame rate: {}", infoValue, error);
                }
            }

            // aspect ratio
            infoValue = infosMainVideo.get("Display aspect ratio");
            mediaFile.setAspectRatio(aspectRatioTools.cleanAspectRatio(infoValue));

            // bit rate
            mediaFile.setBitrate(getBitRate(infosMainVideo));

            // check 3D video source,
            infoValue = infosMainVideo.get("MultiView_Count");
            if ("2".equals(infoValue)) {
                mediaFile.setVideoSource("3D");
            }
        }

        // cycle through audio streams
        Set<AudioCodec> processedAudioCodecs = new HashSet<>(0);
        for (int numAudio = 0; numAudio < infosAudio.size(); numAudio++) {
            Map<String, String> infosCurrentAudio = infosAudio.get(numAudio);
            AudioCodec codec = mediaFile.getAudioCodec(numAudio + 1);
            if (codec == null) {
                codec = new AudioCodec();
                codec.setCounter(numAudio + 1);
                codec.setMediaFile(mediaFile);
            }
            parseAudioCodec(codec, infosCurrentAudio);
            mediaFile.getAudioCodecs().add(codec);
            processedAudioCodecs.add(codec);
        }

        // remove unprocessed internal audio codecs
        Iterator<AudioCodec> iterAudio = mediaFile.getAudioCodecs().iterator();
        while (iterAudio.hasNext()) {
            if (!processedAudioCodecs.contains(iterAudio.next())) {
                iterAudio.remove();
            }
        }

        // cycle through subtitle streams
        Set<Subtitle> processedSubtitles = new HashSet<>(0);
        for (int numText = 0; numText < infosText.size(); numText++) {
            Map<String, String> infosCurrentText = infosText.get(numText);
            Subtitle subtitle = mediaFile.getSubtitle(numText + 1);
            if (subtitle == null) {
                subtitle = new Subtitle();
                subtitle.setCounter(numText + 1);
            }

            boolean processed = parseSubtitle(subtitle, infosCurrentText);
            if (processed) {
                subtitle.setMediaFile(mediaFile);
                mediaFile.getSubtitles().add(subtitle);
                processedSubtitles.add(subtitle);
            }
        }

        // remove unprocessed internal subtitles
        Iterator<Subtitle> iterSubs = mediaFile.getSubtitles().iterator();
        while (iterSubs.hasNext()) {
            Subtitle subtitle = iterSubs.next();
            if (subtitle.getStageFile() == null && !processedSubtitles.contains(subtitle)) {
                iterSubs.remove();
            }
        }
    }

    private void parseAudioCodec(AudioCodec audioCodec, Map<String, String> infosAudio) {
        // codec
        String infoValue = infosAudio.get("Codec ID");
        if (StringUtils.isBlank(infoValue)) {
            audioCodec.setCodec(Constants.UNDEFINED);
        } else {
            audioCodec.setCodec(infoValue);
        }

        // codec format
        infoValue = infosAudio.get("Format");
        if (StringUtils.isBlank(infoValue)) {
            audioCodec.setCodecFormat(Constants.UNDEFINED);
        } else {
            audioCodec.setCodecFormat(infoValue);
        }

        // bit rate
        audioCodec.setBitRate(getBitRate(infosAudio));

        // number of channels
        audioCodec.setChannels(-1);
        infoValue = infosAudio.get("Channel(s)");
        if (StringUtils.isNotBlank(infoValue)) {
            if (infoValue.contains("/")) {
                infoValue = infoValue.substring(0, infoValue.indexOf('/'));
            }
            try {
                Matcher codecMatch = PATTERN_CHANNELS.matcher(infoValue);
                if (codecMatch.matches()) {
                    audioCodec.setChannels(Integer.parseInt(codecMatch.group(1)));
                }
            } catch (NumberFormatException ex) {
                LOG.trace("Failed to parse channels: {}", infoValue, ex);
            }
        }

        // language
        audioCodec.setLanguageCode(Constants.LANGUAGE_UNTERTERMINED);
        infoValue = infosAudio.get("Language");
        if (StringUtils.isNotBlank(infoValue)) {
            if (infoValue.contains("/")) {
                infoValue = infoValue.substring(0, infoValue.indexOf('/')).trim(); // In this case, language are "doubled", just take the first one.
            }
            // determine language
            String langCode = localeService.findLanguageCode(infoValue);
            if (StringUtils.isNotBlank(langCode)) {
                audioCodec.setLanguageCode(langCode);
            }
        }
    }

    private boolean parseSubtitle(Subtitle subtitle, Map<String, String> infosText) {
        // format
        String infoFormat = infosText.get("Format");
        if (StringUtils.isBlank(infoFormat)) {
            // use codec instead format
            infoFormat = infosText.get("Codec");
        }

        // language
        String infoLanguage = infosText.get("Language");
        if (StringUtils.isNotBlank(infoLanguage)) {
            if (infoLanguage.contains("/")) {
                infoLanguage = infoLanguage.substring(0, infoLanguage.indexOf('/')).trim(); // In this case, language are "doubled", just take the first one.
            }
        }

        // just use defined formats
        if ("SRT".equalsIgnoreCase(infoFormat)
                || "UTF-8".equalsIgnoreCase(infoFormat)
                || "RLE".equalsIgnoreCase(infoFormat)
                || "PGS".equalsIgnoreCase(infoFormat)
                || "ASS".equalsIgnoreCase(infoFormat)
                || "VobSub".equalsIgnoreCase(infoFormat))
        {
            subtitle.setFormat(infoFormat);
            
            String langCode = localeService.findLanguageCode(infoLanguage);
            if (StringUtils.isBlank(infoLanguage)) {
                subtitle.setLanguageCode(Constants.LANGUAGE_UNTERTERMINED);
            } else {
                subtitle.setLanguageCode(langCode);
            }

            subtitle.setDefaultFlag("yes".equalsIgnoreCase(infosText.get("Default")));
            subtitle.setForcedFlag("yes".equalsIgnoreCase(infosText.get("Forced")));
            return Boolean.TRUE;
        }

        LOG.debug("Subtitle format skipped: {}", infoFormat);
        return Boolean.FALSE;
    }

    public boolean isRarDiskImage(String filename) {
        if (isMediaInfoRar && (RAR_DISK_IMAGES.contains(FilenameUtils.getExtension(filename).toLowerCase()))) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private static String getRuntime(Map<String, String> infosGeneral, List<Map<String, String>> infosVideo) {
        String runtimeValue = infosGeneral.get("PlayTime");
        if (runtimeValue == null && !infosVideo.isEmpty()) {
            Map<String, String> infosMainVideo = infosVideo.get(0);
            runtimeValue = infosMainVideo.get("Duration");
        }
        if (runtimeValue == null) {
            runtimeValue = infosGeneral.get("Duration");
        }
        if (runtimeValue != null) {
            if (runtimeValue.indexOf('.') >= 0) {
                runtimeValue = runtimeValue.substring(0, runtimeValue.indexOf('.'));
            }
        }
        return runtimeValue;
    }

    public static int getBitRate(Map<String, String> infos) {
        String bitRateValue = infos.get("Bit rate");
        if (StringUtils.isBlank(bitRateValue)) {
            bitRateValue = infos.get("Nominal bit rate");
        }
        return getBitRate(bitRateValue);
    }

    private static int getBitRate(String bitRateValue) {
        if (StringUtils.isNotBlank(bitRateValue)) {
            String tmp;
            if (bitRateValue.indexOf(Constants.SPACE_SLASH_SPACE) > -1) {
                tmp = bitRateValue.substring(0, bitRateValue.indexOf(Constants.SPACE_SLASH_SPACE));
            } else {
                tmp = bitRateValue;
            }

            tmp = tmp.substring(0, tmp.length() - 3);
            return NumberUtils.toInt(tmp, -1);
        }
        return -1;
    }

    private MediaInfoStream createStream(String movieFilePath) throws IOException {
        // Create the command line
        List<String> commandMedia = new ArrayList<>(execMediaInfo);
        commandMedia.add(movieFilePath);

        ProcessBuilder pb = new ProcessBuilder(commandMedia);

        // set up the working directory.
        pb.directory(MEDIAINFO_PATH);

        return new MediaInfoStream(pb.start());
    }

    /**
     * Read the input skipping any blank lines
     *
     * @param input
     * @return
     * @throws IOException
     */
    private static String localInputReadLine(BufferedReader input) throws IOException {
        String line = input.readLine();
        while ((line != null) && (line.equals(""))) {
            line = input.readLine();
        }
        return line;
    }

    public static void parseMediaInfo(MediaInfoStream stream,
            Map<String, String> infosGeneral,
            List<Map<String, String>> infosVideo,
            List<Map<String, String>> infosAudio,
            List<Map<String, String>> infosText) throws Exception {

        try (InputStreamReader isr = new InputStreamReader(stream.getInputStream());
             BufferedReader bufReader = new BufferedReader(isr))
        {
            // Improvement, less code line, each cat have same code, so use the same for all.
            Map<String, List<Map<String, String>>> matches = new HashMap<>();

            // Create a fake one for General, we got only one, but to use the same algo we must create this one.
            String generalKey[] = {"General", "Géneral", "* Général"};
            matches.put(generalKey[0], new ArrayList<Map<String, String>>());
            matches.put(generalKey[1], matches.get(generalKey[0]));
            matches.put(generalKey[2], matches.get(generalKey[0]));
            matches.put("Video", infosVideo);
            matches.put("Vidéo", matches.get("Video"));
            matches.put("Audio", infosAudio);
            matches.put("Text", infosText);

            String line = localInputReadLine(bufReader);
            String label;

            while (line != null) {
                // In case of new format : Text #1, Audio #1
                if (line.indexOf('#') >= 0) {
                    line = line.substring(0, line.indexOf('#')).trim();
                }

                // Get cat ArrayList from cat name.
                List<Map<String, String>> currentCat = matches.get(line);

                if (currentCat != null) {
                    HashMap<String, String> currentData = new HashMap<>();
                    int indexSeparator = -1;
                    while (((line = localInputReadLine(bufReader)) != null) && ((indexSeparator = line.indexOf(" : ")) != -1)) {
                        label = line.substring(0, indexSeparator).trim();
                        if (currentData.get(label) == null) {
                            currentData.put(label, line.substring(indexSeparator + 3));
                        }
                    }
                    currentCat.add(currentData);
                } else {
                    line = localInputReadLine(bufReader);
                }
            }

            // Setting General Info - Beware of lose data if infosGeneral already have some ...
            try {
                for (String generalKey1 : generalKey) {
                    List<Map<String, String>> arrayList = matches.get(generalKey1);
                    if (!arrayList.isEmpty()) {
                        Map<String, String> datas = arrayList.get(0);
                        if (!datas.isEmpty()) {
                            infosGeneral.putAll(datas);
                            break;
                        }
                    }
                }
            } catch (Exception ignore) {
                // We don't care about this exception
            }
        }
    }
}
