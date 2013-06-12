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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.type.ImageFormat;
import org.yamj.core.tools.web.PoolingHttpClient;

@Service("fileStorageService")
public class FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageService.class);
    private String storagePathArtwork;
    @Autowired
    private PoolingHttpClient httpClient;

    @Value("${yamj3.file.storage.artwork}")
    public void setStoragePathArtwork(String storagePathArtwork) {
        this.storagePathArtwork = storagePathArtwork;
        LOG.info("Artwork storage path set to '{}'", storagePathArtwork);
    }

    public boolean exists(StorageType type, String fileName) throws IOException {
        return false;
    }

    public void store(StorageType type, String fileName, URL url) throws IOException {
        LOG.debug("Store file {}; source url: {}", fileName, url.toString());
        String storageFileName = getStorageName(type, fileName);

        HttpEntity entity = httpClient.requestResource(url);
        if (entity == null) {
            throw new RuntimeException("Failed to get content from: " + url);
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
    }

    public void storeArtwork(String fileName, BufferedImage bi, ImageFormat imageFormat, int quality) throws IOException {
        LOG.debug("Store {} image: {}", imageFormat, fileName);
        String storageFileName = getStorageName(StorageType.ARTWORK, fileName);
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
        } catch (Exception error) {
            LOG.debug("Failed saving image: {}", fileName);
            LOG.warn("Graphics error", error);
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

    public boolean delete(StorageType type, String fileName) throws IOException {
        LOG.debug("Delete file {}", fileName);
        File file = getFile(type, fileName);
        return file.delete();
    }

    public File getFile(StorageType type, String fileName) throws IOException {
        String storageName = getStorageName(type, fileName);
        return new File(storageName);
    }

    private String getStorageName(StorageType type, String fileName) {
        if (StorageType.ARTWORK == type) {
            return FilenameUtils.concat(this.storagePathArtwork, fileName);
        }
        throw new RuntimeException("Unknown storage type " + type);
    }

    /**
     * Create a directory hash from the filename
     *
     * @param filename
     * @return
     */
    private static String createDirHash(final String filename) {
        // Skip if the filename is invalid OR has already been hashed
        if (StringUtils.isBlank(filename) || filename.contains(File.separator)) {
            return filename;
        }

        // Remove all the non-word characters from the filename, replacing with an underscore
        String cleanFilename = filename.replaceAll("[^\\p{L}\\p{N}]", "_").toLowerCase().trim();

        StringBuilder dirHash = new StringBuilder();
        dirHash.append(cleanFilename.substring(0, 1)).append(File.separator);
        dirHash.append(cleanFilename.substring(0, cleanFilename.length() > 1 ? 2 : 1)).append(File.separator);
        dirHash.append(filename);

        return dirHash.toString();
    }

    /**
     * Create a directory has from the filename of a file
     *
     * @param file
     * @return
     */
    private static String createDirHash(final File file) {
        return createDirHash(file.getName());
    }
}
