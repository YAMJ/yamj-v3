/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.service.file;

import org.yamj.core.database.model.StageFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.type.ImageFormat;
import org.yamj.core.service.file.tools.FileTools;
import org.yamj.core.tools.web.PoolingHttpClient;

@Service("fileStorageService")
public class FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageService.class);
    // This is the base directory to store the resources in. It should NOT be used in the hash of the filename
    private String storageResourceDir;
    private String storagePathArtwork;
    private String storagePathMediaInfo;
    @Autowired
    private PoolingHttpClient httpClient;

    @Value("${yamj3.file.storage.resources}")
    public void setStorageResourceDir(String storageResourceDir) {
        this.storageResourceDir = FilenameUtils.normalize(storageResourceDir, Boolean.TRUE);
        LOG.info("Resource path set to '{}'", this.storageResourceDir);
    }

    @Value("${yamj3.file.storage.artwork}")
    public void setStoragePathArtwork(String storagePathArtwork) {
        this.storagePathArtwork = FilenameUtils.normalize(FilenameUtils.concat(storageResourceDir, storagePathArtwork), Boolean.TRUE);
        LOG.info("Artwork storage path set to '{}'", this.storagePathArtwork);
    }

    @Value("${yamj3.file.storage.mediainfo}")
    public void setStoragePathMediaInfo(String storagePathMediaInfo) {
        this.storagePathMediaInfo = FilenameUtils.normalize(FilenameUtils.concat(storageResourceDir, storagePathMediaInfo), Boolean.TRUE);
        LOG.info("MediaInfo storage path set to '{}'", this.storagePathMediaInfo);
    }

    public boolean exists(StorageType type, String filename) throws IOException {
        return false;
    }

    public boolean store(StorageType type, String filename, URL url) throws IOException {
        LOG.debug("Store file {}; source url: {}", filename, url.toString());
        String storageFileName = getStorageName(type, filename);

        HttpEntity entity = httpClient.requestResource(url);
        if (entity == null) {
            LOG.error("Failed to get content from source url: {}", url);
            return Boolean.FALSE;
        }

        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(storageFileName);
            entity.writeTo(outputStream);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ignore) {
                    // ignore this error
                }
            }
        }

        return Boolean.TRUE;
    }

    public boolean store(StorageType type, String filename, StageFile stageFile) throws IOException {
        LOG.debug("Store file {}; source file: {}", filename, stageFile.getFullPath());

        File src = new File(stageFile.getFullPath());
        File dst = getFile(type, filename);
        return FileTools.copyFile(src, dst);
    }

    public void storeArtwork(String filename, BufferedImage bi, ImageFormat imageFormat, int quality) throws Exception {
        LOG.debug("Store {} image: {}", imageFormat, filename);
        String storageFileName = getStorageName(StorageType.ARTWORK, filename);
        File outputFile = new File(storageFileName);

        ImageWriter writer = null;
        FileImageOutputStream output = null;
        try {
            if (ImageFormat.PNG == imageFormat) {
                ImageIO.write(bi, "png", outputFile);
            } else {
                float jpegQuality = (float) quality / 100;
                BufferedImage bufImage = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
                bufImage.createGraphics().drawImage(bi, 0, 0, null, null);

                @SuppressWarnings("rawtypes")
                Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
                writer = (ImageWriter) iter.next();

                ImageWriteParam iwp = writer.getDefaultWriteParam();
                iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwp.setCompressionQuality(jpegQuality);

                output = new FileImageOutputStream(outputFile);
                writer.setOutput(output);
                IIOImage image = new IIOImage(bufImage, null, null);
                writer.write(null, image, iwp);
            }
        } finally {
            if (writer != null) {
                writer.dispose();
            }
            if (output != null) {
                try {
                    output.close();
                } catch (Exception ignore) {
                    // ignore this error
                }
            }
        }
    }

    public boolean delete(StorageType type, String filename) throws IOException {
        LOG.debug("Delete file {}", filename);
        File file = getFile(type, filename);
        return file.delete();
    }

    public File getFile(StorageType type, String filename) throws IOException {
        String storageName = getStorageName(type, filename);
        return new File(storageName);
    }

    public String getStorageName(StorageType type, String filename) {
        String hashFilename;
        if (StorageType.ARTWORK == type) {
            hashFilename = FilenameUtils.concat(this.storagePathArtwork, FileTools.createDirHash(filename));
            FileTools.makeDirectories(hashFilename);
        } else if (StorageType.MEDIAINFO == type) {
            hashFilename = FilenameUtils.concat(this.storagePathMediaInfo, FileTools.createDirHash(filename));
            FileTools.makeDirectories(hashFilename);
        } else {
            throw new IllegalArgumentException("Unknown storage type " + type);
        }
        return hashFilename;
    }

    public String getStorageName(StorageType type, String dir, String filename) {
        String hashFilename = FilenameUtils.concat(dir, filename);
        if (StorageType.ARTWORK == type) {
            hashFilename = FilenameUtils.concat(this.storagePathArtwork, hashFilename);
            FileTools.makeDirectories(hashFilename);
        } else if (StorageType.MEDIAINFO == type) {
            hashFilename = FilenameUtils.concat(this.storagePathMediaInfo, hashFilename);
            FileTools.makeDirectories(hashFilename);
        } else {
            throw new IllegalArgumentException("Unknown storage type " + type);
        }

        return hashFilename;
    }
}
