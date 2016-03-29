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
package org.yamj.core.service.file;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.imageio.*;
import javax.imageio.stream.FileImageOutputStream;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.api.model.Skin;
import org.yamj.core.database.model.StageFile;
import org.yamj.core.database.model.type.ImageType;
import org.yamj.core.service.attachment.AttachmentScannerService;

@Service("fileStorageService")
public class FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageService.class);

    // This is the base directory to store the resources in. It should NOT be used in the hash of the filename
    private String storageResourceDir;
    private String storagePathArtwork;
    private String storagePathMediaInfo;
    private String storagePathPhoto;
    private String storagePathSkin;
    private String storagePathTrailer;
    
    @Autowired
    private PoolingHttpClient httpClient;
    @Autowired
    private AttachmentScannerService attachmentScannerService;

    @PostConstruct
    public void init() {
        LOG.trace("Initialize file storage service");

        String value = PropertyTools.getProperty("yamj3.file.storage.resources");
        if (StringUtils.isBlank(value)) {
            LOG.warn("Property 'yamj3.file.storage.resources' not set; using default");
            value = "./resources/";
        }
        this.storageResourceDir = FilenameUtils.normalizeNoEndSeparator(value, true);
        LOG.info("Resource path set to '{}'", this.storageResourceDir);

        value = PropertyTools.getProperty("yamj3.file.storage.artwork");
        if (StringUtils.isBlank(value)) {
            LOG.warn("Property 'yamj3.file.storage.artwork' not set; using default");
            value = "artwork";
        }
        this.storagePathArtwork = FilenameUtils.normalizeNoEndSeparator(FilenameUtils.concat(this.storageResourceDir, value), true).concat("/");
        LOG.info("Artwork storage path set to '{}'", this.storagePathArtwork);

        value = PropertyTools.getProperty("yamj3.file.storage.mediainfo");
        if (StringUtils.isBlank(value)) {
            LOG.warn("Property 'yamj3.file.storage.mediainfo' not set; using default");
            value = "mediainfo";
        }
        this.storagePathMediaInfo = FilenameUtils.normalizeNoEndSeparator(FilenameUtils.concat(this.storageResourceDir, value), true).concat("/");
        LOG.info("MediaInfo storage path set to '{}'", this.storagePathMediaInfo);

        value = PropertyTools.getProperty("yamj3.file.storage.photo");
        if (StringUtils.isBlank(value)) {
            LOG.warn("Property 'yamj3.file.storage.photo' not set; using default");
            value = "photo";
        }
        this.storagePathPhoto = FilenameUtils.normalizeNoEndSeparator(FilenameUtils.concat(this.storageResourceDir, value), true).concat("/");
        LOG.info("Photo storage path set to '{}'", this.storagePathPhoto);

        value = PropertyTools.getProperty("yamj3.file.storage.skins");
        if (StringUtils.isBlank(value)) {
            LOG.warn("Property 'yamj3.file.storage.skins' not set; using default");
            value = "skins";
        }
        this.storagePathSkin = FilenameUtils.normalizeNoEndSeparator(FilenameUtils.concat(this.storageResourceDir, value), true).concat("/");
        LOG.info("Skins storage path set to '{}'", this.storagePathSkin);

        value = PropertyTools.getProperty("yamj3.file.storage.trailer");
        if (StringUtils.isBlank(value)) {
            LOG.warn("Property 'yamj3.file.storage.trailer' not set; using default");
            value = "trailer";
        }
        this.storagePathTrailer = FilenameUtils.normalizeNoEndSeparator(FilenameUtils.concat(this.storageResourceDir, value), true).concat("/");
        LOG.info("Trailer storage path set to '{}'", this.storagePathTrailer);
}

    public boolean store(StorageType type, String filename, URL url) throws IOException {
        LOG.debug("Store file {}; source url: {}", filename, url.toString());
        String storageFileName = getStorageName(type, filename);

        HttpEntity entity = httpClient.requestResource(url);
        if (entity == null) {
            LOG.error("Failed to get content from source url: {}", url);
            return false;
        }
        
        try (OutputStream outputStream = new FileOutputStream(storageFileName)) {
            entity.writeTo(outputStream);
        }

        return true;
    }

    public boolean store(StorageType type, String filename, byte[] bytes) throws IOException {
        LOG.debug("Store file {}; uploaded image", filename);
        String storageFileName = getStorageName(type, filename);
        
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            FileUtils.copyInputStreamToFile(is, new File(storageFileName));
        }

        return true;
    }

    public boolean store(StorageType type, String filename, StageFile stageFile) {
        LOG.debug("Store file {}; source file: {}", filename, stageFile.getFullPath());

        File src = new File(stageFile.getFullPath());
        File dst = getFile(type, filename);
        return FileTools.copyFile(src, dst);
    }

    public boolean store(StorageType type, String filename, StageFile stageFile, int attachmentId) {
        if (attachmentId <= 0) {
            return false;
        }
        
        LOG.debug("Store file {}; attachment {} in source file: {}", filename, attachmentId, stageFile.getFullPath());

        // get destination file
        File dst = getFile(type, filename);
        return attachmentScannerService.extractArtwort(dst, stageFile, attachmentId);
    }

    public boolean store(StorageType type, String filename, File sourceFile) {
        return this.store(type, filename, sourceFile, false);
    }

    public boolean store(StorageType type, String filename, File sourceFile, boolean deleteSource) {
        LOG.debug("Store file {}; source file: {}", filename, sourceFile.getAbsolutePath());
        File dst = getFile(type, filename);
        return FileTools.copyFile(sourceFile, dst, deleteSource);
    }

    public void storeImage(String filename, StorageType type, BufferedImage bi, ImageType imageType, int quality) throws Exception {
        LOG.debug("Store {} {} image: {}", type, imageType, filename);
        String storageFileName = getStorageName(type, filename);
        File outputFile = new File(storageFileName);

        ImageWriter writer = null;
        try {
            if (ImageType.PNG == imageType) {
                ImageIO.write(bi, "png", outputFile);
            } else {
                float jpegQuality = (float) quality / 100;
                BufferedImage bufImage = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
                bufImage.createGraphics().drawImage(bi, 0, 0, null, null);

                writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                ImageWriteParam iwp = writer.getDefaultWriteParam();
                iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwp.setCompressionQuality(jpegQuality);

                try (FileImageOutputStream output = new FileImageOutputStream(outputFile)) {
                    writer.setOutput(output);
                    IIOImage image = new IIOImage(bufImage, null, null);
                    writer.write(null, image, iwp);
                }
            }
        } finally {
            if (writer != null) {
                writer.dispose();
            }
        }
    }

    public String storeSkin(Skin skin) {
        String message = "Skin downloaded OK";
        LOG.debug("Attempting to store skin URL: '{}'", skin.getSourceUrl());
        if (StringUtils.isNotBlank(skin.getSourceUrl())) {
            String filename = FilenameUtils.getName(skin.getSourceUrl()).replaceAll("[^a-zA-Z0-9-_\\.]", "_");
            LOG.debug("Storage filename is '{}'", filename);

            URL skinUrl;
            try {
                skinUrl = new URL(skin.getSourceUrl());
                boolean downloadResult = store(StorageType.SKIN, filename, skinUrl);
                LOG.debug("Skin download {}", downloadResult ? "OK" : "Failed");

                if (downloadResult) {
                    String zipFilename = FilenameUtils.concat(skin.getSkinDir(), filename);
                    LOG.debug("Unzipping skin file '{}'", zipFilename);

                    if (!unzipSkinFile(skin, zipFilename)) {
                        message = "Failed to extract skin from zip file!";
                    }
                } else {
                    message = "Skin download failed. Check log for details.";
                }

            } catch (MalformedURLException ex) {
                LOG.warn("Failed to encode URL '{}': {}", skin.getSourceUrl(), ex.getMessage());
                LOG.trace("Invalid skin URL", ex);
                message = "Failed to decode skin URL, please check and try again";
            } catch (IOException ex) {
                LOG.warn("Failed to download '{}' from URL '{}': {}", filename, skin.getSourceUrl(), ex.getMessage());
                LOG.trace("Skin download error", ex);
                message = "Failed to download skin zip from URL: " + ex.getMessage();
            }
        } else {
            LOG.info("No URL found for skin: {}", skin);
            message = "No URL found for the skin";
        }
        return message;
    }

    private static boolean unzipSkinFile(Skin skin, String zipFilename) {
        boolean unzipResult;
        try {
            ZipFile zf = new ZipFile(zipFilename);

            // Get a list of the files in the ZIP file
            List<FileHeader> fileHeaderList = zf.getFileHeaders();
            // Get the first file
            String tempFilename = fileHeaderList.get(0).getFileName();
            // Get the directory name for the first file
            String tempDir = FilenameUtils.getBaseName(FilenameUtils.getPathNoEndSeparator(tempFilename));

            // If the directory from the ZIP was empty, use the ZIP name to unpack to.
            String zipTargetDir;
            if (StringUtils.isBlank(tempDir)) {
                // There's no folder so add the ZIP filename
                zipTargetDir = FilenameUtils.concat(skin.getSkinDir(), zipFilename);
                skin.setPath(zipFilename);
            } else {
                // Use the skin folder plus what's in the ZIP
                zipTargetDir = skin.getSkinDir();
                // Set the skin path to the one in the zip file
                skin.setPath(tempDir);
            }

            // Unpack the files
            zf.extractAll(zipTargetDir);
            LOG.info("Unzipped zip file '{}' to '{}'", zipFilename, zipTargetDir);

            // Update the skin information
            skin.readSkinInformation();
            
            unzipResult = true;
        } catch (ZipException ex) {
            LOG.warn("Failed to extract zip file '{}': {}", zipFilename, ex.getMessage());
            LOG.trace("ZIP error", ex);
            unzipResult = false;
        }
        
        return unzipResult;
    }
    
    public boolean deleteFile(StorageType type, String filename) {
        LOG.debug("Delete file '{}'", filename);
        File file = getFile(type, filename);
        return file.delete();
    }

    public File getFile(StorageType type, String filename) {
        String storageName = getStorageName(type, filename);
        return new File(storageName);
    }

    public List<String> getDirectoryList(StorageType type, final String dir) {
        File path = new File(getStorageDir(type, StringUtils.trimToEmpty(dir)));
        String[] directories = path.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });

        return Arrays.asList(directories);
    }

    public String getStorageName(StorageType storageType, String filename) {
        return getStorageName(storageType, null, filename);
    }

    public String getStorageDir(StorageType storageType, final String path) {
        String storageDir;
        if (StorageType.ARTWORK == storageType) {
            storageDir = FilenameUtils.concat(this.storagePathArtwork, path);
        } else if (StorageType.PHOTO == storageType) {
            storageDir = FilenameUtils.concat(this.storagePathPhoto, path);
        } else if (StorageType.MEDIAINFO == storageType) {
            storageDir = FilenameUtils.concat(this.storagePathMediaInfo, path);
        } else if (StorageType.SKIN == storageType) {
            storageDir = FilenameUtils.concat(this.storagePathSkin, path);
        } else if (StorageType.TRAILER == storageType) {
            storageDir = FilenameUtils.concat(this.storagePathTrailer, path);
        } else {
            throw new IllegalArgumentException("Unknown storage type " + storageType);
        }
        return storageDir;
    }

    public String getStorageName(StorageType type, final String dir, final String filename) {
        String hashFilename;
        if (type == StorageType.SKIN) {
            // Don't hash the skin filename
            hashFilename = filename;
        } else {
            hashFilename = FileTools.createDirHash(StringUtils.trimToEmpty(filename));
        }

        if (StringUtils.isNotBlank(dir)) {
            hashFilename = FilenameUtils.concat(StringUtils.trimToEmpty(dir), hashFilename);
        }

        hashFilename = getStorageDir(type, hashFilename);
        FileTools.makeDirectories(hashFilename);
        return hashFilename;
    }

    public boolean existsFile(StorageType type, String cacheDir, String cacheFile) {
        try {
            final String filename = FilenameUtils.concat(cacheDir, cacheFile);
            File file = new File(this.getStorageDir(type, filename));
            if (!file.exists()) {
                return false;
            }
            if (file.getCanonicalPath().endsWith(filename)) {
                return true;
            }
            // delete file; false will be returned in any case
            file.delete();
        } catch (Exception any) { //NOSONAR
            // ignore any error
        }
        return false;
    }
    
    public static void deleteStorageFiles(Set<String> filesToDelete) {
        if (filesToDelete.isEmpty()) {
            LOG.trace("No files to delete in storage");
            return;
        }

        // delete files on disk
        for (String filename : filesToDelete) {
            try {
                LOG.debug("Delete file: {}", filename);
                File file = new File(filename);
                if (!file.exists()) {
                    LOG.debug("File '{}' does not exist", filename);
                } else if (!file.delete()) {
                    LOG.warn("File '{}' could not be deleted", filename);
                }
            } catch (Exception ex) {
                LOG.error("Deletion error for file: '" + filename + "'", ex);
            }
        }
    }
    
    public String getStorageResourceDir() {
        return storageResourceDir;
    }

    public String getStoragePathArtwork() {
        return storagePathArtwork;
    }

    public String getStoragePathPhoto() {
        return storagePathPhoto;
    }

    public String getStoragePathMediaInfo() {
        return storagePathMediaInfo;
    }

    public String getStoragePathSkin() {
        return storagePathSkin;
    }

    public String getStoragePathTrailer() {
        return storagePathTrailer;
    }
}
