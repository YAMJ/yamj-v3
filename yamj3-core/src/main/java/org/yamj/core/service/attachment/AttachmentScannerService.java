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
package org.yamj.core.service.attachment;

import java.io.*;
import java.util.*;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.StageFile;
import org.yamj.core.service.file.FileTools;

/**
 * Scans and extracts attachments within a file i.e. matroska files.
 */
@Service("attachmentScannerService")
public class AttachmentScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(AttachmentScannerService.class);
    
    // mkvToolnix command line, depend on OS
    private static final File MT_PATH = new File(PropertyTools.getProperty("mkvtoolnix.home", "./mkvToolnix/"));
    private static final List<String> MT_INFO_EXE = new ArrayList<>();
    private static final List<String> MT_EXTRACT_EXE = new ArrayList<>();
    private static final String MT_INFO_FILENAME_WINDOWS = "mkvinfo.exe";
    private static final String MT_INFO_FILENAME_LINUX = "mkvinfo";
    private static final String MT_EXTRACT_FILENAME_WINDOWS = "mkvextract.exe";
    private static final String MT_EXTRACT_FILENAME_LINUX = "mkvextract";

    // tokens
    private static final String[] POSTER_TOKEN = new String[]{"poster", "cover"};
    private static final String[] FANART_TOKEN = new String[]{"fanart", "backdrop", "background"};
    private static final String[] BANNER_TOKEN = new String[]{"banner"};
    private static final String[] VIDEOIMAGE_TOKEN = new String[]{"videoimage"};

    // flag to indicate if scanner is activated
    private boolean isActivated = Boolean.FALSE;
    // valid MIME types
    private Set<String> validMimeTypesText;
    private Map<String, String> validMimeTypesImage;

    @Autowired
    private Cache attachmentCache;
    
    @PostConstruct
    public void init() {
        LOG.info("Initialize attachment scanner service");

        String OS_NAME = System.getProperty("os.name");
        LOG.debug("MKV Toolnix Path : {}", MT_PATH);

        File mkvInfoFile;
        File mkvExtractFile;
        if (OS_NAME.contains("Windows")) {
            mkvInfoFile = new File(MT_PATH.getAbsolutePath() + File.separator + MT_INFO_FILENAME_WINDOWS);
            mkvExtractFile = new File(MT_PATH.getAbsolutePath() + File.separator + MT_EXTRACT_FILENAME_WINDOWS);
        } else {
            mkvInfoFile = new File(MT_PATH.getAbsolutePath() + File.separator + MT_INFO_FILENAME_LINUX);
            mkvExtractFile = new File(MT_PATH.getAbsolutePath() + File.separator + MT_EXTRACT_FILENAME_LINUX);
        }
        
        if (!mkvInfoFile.canExecute()) {
            LOG.info( "Couldn't find MKV toolnix executable tool 'mkvinfo'");
            isActivated = Boolean.FALSE;
        } else if (!mkvExtractFile.canExecute()) {
            LOG.info( "Couldn't find MKV toolnix executable tool 'mkvextract'");
            isActivated = Boolean.FALSE;
        } else {
            isActivated = Boolean.TRUE;
            
            // activate tools
            if (OS_NAME.contains("Windows")) {
                MT_INFO_EXE.clear();
                MT_INFO_EXE.add("cmd.exe");
                MT_INFO_EXE.add("/E:1900");
                MT_INFO_EXE.add("/C");
                MT_INFO_EXE.add(mkvInfoFile.getName());
                MT_INFO_EXE.add("--ui-language");
                MT_INFO_EXE.add("en");
                
                MT_EXTRACT_EXE.clear();
                MT_EXTRACT_EXE.add("cmd.exe");
                MT_EXTRACT_EXE.add("/E:1900");
                MT_EXTRACT_EXE.add("/C");
                MT_EXTRACT_EXE.add(mkvExtractFile.getName());
            } else {
                MT_INFO_EXE.clear();
                MT_INFO_EXE.add("./" + mkvInfoFile.getName());
                MT_INFO_EXE.add("--ui-language");
                MT_INFO_EXE.add("en_US");
    
                MT_EXTRACT_EXE.clear();
                MT_EXTRACT_EXE.add("./" + mkvExtractFile.getName());
            }
            
            // add valid mime types (text)
            validMimeTypesText = new HashSet<>(3);
            validMimeTypesText.add("text/xml");
            validMimeTypesText.add("application/xml");
            validMimeTypesText.add("text/html");

            // add valid mime types (image)
            validMimeTypesImage = new HashMap<>(4);
            validMimeTypesImage.put("image/jpeg", ".jpg");
            validMimeTypesImage.put("image/png", ".png");
            validMimeTypesImage.put("image/gif", ".gif");
            validMimeTypesImage.put("image/x-ms-bmp", ".bmp");
        }
    }

    /**
     * Checks if a file is scanable for attachments. Therefore the file must exist and the extension must be equal to MKV.
     *
     * @param stageFile the file to scan
     * @return true, if file is scanable, else false
     */
    private static boolean isFileScanable(StageFile stageFile) {
        if (!"mkv".equalsIgnoreCase(stageFile.getExtension())) {
            // no MATROSKA file
            return Boolean.FALSE;
        }
        return FileTools.isFileReadable(stageFile);
    }

    /**
     * Scan artwork attachments
     *
     * @param movie
     */
    public List<Attachment> scan(Artwork artwork) {
        if (!isActivated) {
            return null;
        }

        // TODO find scanable stage files
        List<StageFile> stageFiles = Collections.emptyList();
        
        // create attachments
        List<Attachment> artworkAttachments = new ArrayList<>();
        for (StageFile stageFile : stageFiles) {
            List<Attachment> attachments = scanAttachments(stageFile);
            if (CollectionUtils.isNotEmpty(attachments)) {
                artworkAttachments.addAll(attachments);
            }
        }
        
        if (CollectionUtils.isEmpty(artworkAttachments)) {
            // nothing to do anymore cause no attachments found
            return null;
        }
        
        // filter attachments
        Iterator<Attachment> iter = artworkAttachments.iterator();
        while (iter.hasNext()) {
            Attachment attachment = iter.next();
            if (!artwork.getArtworkType().name().equals(attachment.getType().name())) {
                // remove non matching types
                iter.remove();
            }
        }

        // return attachments for artwork
        return artworkAttachments;
    }

    /**
     * Scans a matroska movie file for attachments.
     *
     * @param movieFile the movie file to scan
     */
    private List<Attachment> scanAttachments(StageFile stageFile) {
        if (!isFileScanable(stageFile)) return null;
        
        final String cacheKey = String.valueOf(stageFile.getId());
        List<Attachment> attachments = attachmentCache.get(cacheKey, List.class);
        if (attachments != null) {
            // attachments stored so just return them
            return attachments;
        }
        
        // create new attachments
        attachments = new ArrayList<>();

        LOG.debug("Scanning file {}",  stageFile.getFileName());
        int attachmentId = 0;
        try {
            // create the command line
            List<String> commandMkvInfo = new ArrayList<>(MT_INFO_EXE);
            commandMkvInfo.add(stageFile.getFullPath());

            ProcessBuilder pb = new ProcessBuilder(commandMkvInfo);
            pb.directory(MT_PATH);

            Process p = pb.start();
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = localInputReadLine(input);
            while (line != null) {
                if (line.contains("+ Attached")) {
                    // increase the attachment id
                    attachmentId++;
                    // next line contains file name
                    String fileNameLine = localInputReadLine(input);
                    // next line contains MIME type
                    String mimeTypeLine = localInputReadLine(input);

                    Attachment attachment = createAttachment(attachmentId, fileNameLine, mimeTypeLine);
                    if (attachment != null) {
                        attachment.setStageFile(stageFile);
                        attachments.add(attachment);
                    }
                }

                line = localInputReadLine(input);
            }

            if (p.waitFor() != 0) {
                LOG.error("Error during attachment retrieval - ErrorCode={}",  p.exitValue());
            }
        } catch (IOException | InterruptedException ex) {
            LOG.error("Attachment scanner error", ex);
        }
        
        // put into cache
        this.attachmentCache.putIfAbsent(cacheKey, attachments);
        return attachments;
    }

    private static String localInputReadLine(BufferedReader input) {
        String line = null;
        try {
            line = input.readLine();
            while ((line != null) && StringUtils.isBlank(line)) {
                line = input.readLine();
            }
        } catch (IOException ignore) {
            // ignore this error
        }
        return line;
    }

    /**
     * Creates an attachment.
     *
     * @param id
     * @param filename
     * @param mimetype
     * @return Attachment or null
     */
    private Attachment createAttachment(int id, String filename, String mimetype) {
        String fixedFileName = null;
        if (filename.contains("File name:")) {
            fixedFileName = filename.substring(filename.indexOf("File name:") + 10).trim();
        }
        String fixedMimeType = null;
        if (mimetype.contains("Mime type:")) {
            fixedMimeType = mimetype.substring(mimetype.indexOf("Mime type:") + 10).trim();
        }

        AttachmentContent content = determineContent(fixedFileName, fixedMimeType);

        Attachment attachment = null;
        if (content == null) {
            LOG.debug("Failed to dertermine attachment type for '{}' ({})",fixedFileName,  fixedMimeType );
        } else {
            attachment = new Attachment();
            attachment.setType(AttachmentType.MATROSKA); // one and only type at the moment
            attachment.setAttachmentId(id);
            attachment.setContentType(content.getContentType());
            attachment.setMimeType(fixedMimeType == null ? null : fixedMimeType.toLowerCase());
            attachment.setPart(content.getPart());
            LOG.debug("Found attachment {}",  attachment);
        }
        return attachment;
    }

    /**
     * Determines the content of the attachment by file name and mime type.
     *
     * @param inFileName
     * @param inMimeType
     * @return the content, may be null if determination failed
     */
    private AttachmentContent determineContent(String inFileName, String inMimeType) {
        if (inFileName == null) {
            return null;
        }
        if (inMimeType == null) {
            return null;
        }
        String fileName = inFileName.toLowerCase();
        String mimeType = inMimeType.toLowerCase();

        if (validMimeTypesText.contains(mimeType)) {
            // NFO
            if ("nfo".equalsIgnoreCase(FilenameUtils.getExtension(fileName))) {
                return new AttachmentContent(ContentType.NFO);
            }
        } else if (validMimeTypesImage.containsKey(mimeType)) {
            String check = FilenameUtils.removeExtension(fileName);
            // check for SET image
            boolean isSetImage = Boolean.FALSE;
            if (check.endsWith(".set")) {
                isSetImage = Boolean.TRUE;
                // fix check to look for image type
                // just removing extension which is ".set" in this moment
                check = FilenameUtils.removeExtension(check);
            }
            for (String posterToken : POSTER_TOKEN) {
                if (check.endsWith("."+posterToken) || check.equals(posterToken)) {
                    if (isSetImage) {
                        // fileName = <any>.<posterToken>.set.<extension>
                        return new AttachmentContent(ContentType.SET_POSTER);
                    }
                    // fileName = <any>.<posterToken>.<extension>
                    return new AttachmentContent(ContentType.POSTER);
                }
            }
            for (String fanartToken : FANART_TOKEN) {
                if (check.endsWith("."+fanartToken) || check.equals(fanartToken)) {
                    if (isSetImage) {
                        // fileName = <any>.<fanartToken>.set.<extension>
                        return new AttachmentContent(ContentType.SET_FANART);
                    }
                    // fileName = <any>.<fanartToken>.<extension>
                    return new AttachmentContent(ContentType.FANART);
                }
            }
            for (String bannerToken : BANNER_TOKEN) {
                if (check.endsWith("."+bannerToken) || check.equals(bannerToken)) {
                    if (isSetImage) {
                        // fileName = <any>.<bannerToken>.set.<extension>
                        return new AttachmentContent(ContentType.SET_BANNER);
                    }
                    // fileName = <any>.<bannerToken>.<extension>
                    return new AttachmentContent(ContentType.BANNER);
                }
            }
            for (String videoimageToken : VIDEOIMAGE_TOKEN) {
                if (check.endsWith("."+videoimageToken) || check.equals(videoimageToken)) {
                    // fileName = <any>.<videoimageToken>.<extension>
                    return new AttachmentContent(ContentType.VIDEOIMAGE);
                }
                // TODO determination of episode/part
            }
        }

        // no content type determined
        return null;
    }

    public boolean extractArtwort(File dst, StageFile stageFile, int attachmentId) {
        if (!FileTools.isFileReadable(stageFile)) return false;

        LOG.trace("Extract attachement {} from stage file {}",  attachmentId, stageFile.getFullPath());
        
        boolean stored = true;
        try {
            // Create the command line
            List<String> commandMedia = new ArrayList<>(MT_EXTRACT_EXE);
            commandMedia.add("attachments");
            commandMedia.add(stageFile.getFullPath());
            commandMedia.add(attachmentId + ":" + dst.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(commandMedia);
            pb.directory(MT_PATH);
            Process p = pb.start();

            if (p.waitFor() != 0) {
                LOG.error("Error during extraction - ErrorCode={}",  p.exitValue());
                stored = false;
            }
        } catch (IOException | InterruptedException ex) {
            LOG.error("Attachment extraction error", ex);
            stored = false;
        }

        if (!stored) {
            // delete destination file in error case
            try {
                dst.delete();
            } catch (Exception e) {
                // ignore any error;
            }
        }
        
        return stored;
    }
}
