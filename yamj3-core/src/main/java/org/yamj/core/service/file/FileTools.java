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

import static org.yamj.plugin.api.Constants.UTF8;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.core.database.model.StageDirectory;
import org.yamj.core.database.model.StageFile;

public final class FileTools {

    private static final Logger LOG = LoggerFactory.getLogger(FileTools.class);
    private static final Lock MKDIRS_LOCK = new ReentrantLock();

    private FileTools() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Copy the source file to the destination
     *
     * @param src
     * @param dst
     * @return
     */
    public static boolean copyFile(String src, String dst) {
        File srcFile = new File(src);
        File dstFile = new File(dst);
        return copyFile(srcFile, dstFile);
    }

    /**
     * Copy the source file to the destination
     *
     * @param src
     * @param dst
     * @return
     */
    public static boolean copyFile(File src, File dst) {
        return copyFile(src, dst, false);
    }
    
    /**
     * Copy the source file to the destination
     *
     * @param src
     * @param dst
     * @return
     */
    public static boolean copyFile(File src, File dst, boolean deleteSource) {
        boolean returnValue = false;

        if (!src.exists()) {
            LOG.error("The file '{}' does not exist", src);
            return returnValue;
        }

        if (dst.isDirectory()) {
            makeDirectories(dst);
            returnValue = copyFile(src, new File(dst + File.separator + src.getName()));
        } else {
            try (FileInputStream inSource = new FileInputStream(src);
                 FileOutputStream outSource = new FileOutputStream(dst);
                 FileChannel inChannel = inSource.getChannel();
                 FileChannel outChannel = outSource.getChannel())
             {
                long p = 0, s = inChannel.size();
                while (p < s) {
                    p += inChannel.transferTo(p, 1024 * 1024, outChannel);
                }
                
                if (deleteSource) {
                    try  {
                        src.delete();
                    } catch (Exception ex)  {
                        LOG.warn("Source file could not be deleted", ex);
                    }
                }
                
                returnValue = true;
            } catch (IOException error) {
                LOG.error("Failed copying file '{}' to '{}'", src, dst);
                LOG.error("File copying error", error);
                returnValue = false;
            }
        }

        return returnValue;
    }

    /**
     * Create all directories up to the level of the file passed
     *
     * @param filename Source directory or file to create the directories
     * directories
     * @return
     */
    public static boolean makeDirectories(String filename) {
        return makeDirectories(new File(filename));
    }

    /**
     * Create all directories up to the level of the file passed
     *
     * @param filename Source directory or file to create the directories
     * @param numOfTries Number of attempts that will be made to create the
     * directories
     * @return
     */
    public static boolean makeDirectories(String filename, int numOfTries) {
        return makeDirectories(new File(filename), numOfTries);
    }

    /**
     * Create all directories up to the level of the file passed
     *
     * @param file Source directory or file to create the directories
     * directories
     * @return
     */
    public static boolean makeDirectories(File file) {
        return makeDirectories(file, 10);
    }

    /**
     * Create all directories up to the level of the file passed
     *
     * @param sourceDirectory Source directory or file to create the directories
     * @param numOfTries Number of attempts that will be made to create the
     * directories
     * @return
     */
    public static boolean makeDirectories(final File sourceDirectory, int numOfTries) {
        File targetDirectory;
        if (sourceDirectory.isDirectory()) {
            targetDirectory = sourceDirectory;
        } else {
            targetDirectory = sourceDirectory.getParentFile();
        }

        if (targetDirectory.exists()) {
            return true;
        }
        LOG.debug("Creating directories for {} ", targetDirectory.getAbsolutePath());

        MKDIRS_LOCK.lock();
        try {
            boolean status = targetDirectory.mkdirs();
            int looper = 1;
            while (!status && looper++ <= numOfTries) {
                status = targetDirectory.mkdirs();
            }
            if (status && looper > 10) {
                LOG.error("Failed creating the directory '{}'. Ensure this directory is read/write!", targetDirectory.getAbsolutePath());
                return false;
            }
            return true;
        } finally {
            MKDIRS_LOCK.unlock();
        }
    }

    /**
     * Create a directory hash from the filename
     *
     * @param filename
     * @return
     */
    public static String createDirHash(final String filename) {
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
    public static String createDirHash(final File file) {
        return createDirHash(file.getName());
    }

    /**
     * Read a file and return it as a string using default encoding
     *
     * @param file
     * @return the file content
     */
    public static String readFileToString(File file) {
        return readFileToString(file, UTF8);
    }

    /**
     * Read a file and return it as a string
     *
     * @param file
     * @param encoding
     * @return the file content
     */
    public static String readFileToString(File file, Charset charset) {
        String data = "";
        if (file == null) {
            LOG.error("Failed reading file, file is null");
        } else {
            try {
                data = FileUtils.readFileToString(file, charset);
            } catch (Exception ex) {
                LOG.error("Failed reading file: {}", file.getName());
                LOG.error("Error", ex);
            }
        }
        return data;
    }

    public static boolean isFileScannable(StageFile stageFile) {
        boolean scannable;
        if (StringUtils.isBlank(stageFile.getContent())) {
            scannable = isFileReadable(stageFile);
        } else {
            scannable = true;
        }
        return scannable;
    }

    public static boolean isFileReadable(StageFile stageFile) {
        boolean readable = false;
        try {
            File file = new File(stageFile.getFullPath());
            final boolean exists = file.exists();
            final boolean canRead = file.canRead();
            readable = (exists && canRead);
            LOG.trace("File '{}' exists: {}", stageFile.getFullPath(), exists);
            LOG.trace("File '{}' readable: {}", stageFile.getFullPath(), canRead);
        } catch (Exception e) {
            LOG.trace("Could not determine if file '" + stageFile.getFullPath() + "' is readable", e);
        }
        return readable;
    }

    public static boolean isFileReadable(StageDirectory stageDirectory) {
        boolean readable = false;
        try {
            File file = new File(stageDirectory.getDirectoryPath());
            final boolean exists = file.exists();
            final boolean canRead = file.canRead();
            readable = (exists && canRead);
            LOG.trace("Directory '{}' exists: {}", stageDirectory.getDirectoryPath(), exists);
            LOG.trace("Directory '{}' readable: {}", stageDirectory.getDirectoryPath(), canRead);
        } catch (Exception e) {
            LOG.trace("Could not determine if directory '" + stageDirectory.getDirectoryPath() + "' is readable", e);
        }
        return readable;
    }

    public static boolean isWithinSpecialFolder(StageFile stageFile, String folderName) {
        if (StringUtils.isBlank(folderName) || stageFile == null) {
            return false;
        }
        StageDirectory directory = stageFile.getStageDirectory();
        if (directory == null) {
            return false;
        }
        if (directory.getDirectoryName().equalsIgnoreCase(folderName)) {
            return true;
        }
        return StringUtils.containsIgnoreCase(directory.getDirectoryPath(), getPathFragment(folderName));
    }

    public static String getPathFragment(String folderName) {
        return FilenameUtils.separatorsToUnix("/" + folderName + "/");
    }
    
    public static Set<StageDirectory> getParentDirectories(Collection<StageDirectory> directories) {
        Set<StageDirectory> parentDirectories = new HashSet<>();
        for (StageDirectory directory : directories) {
            if (directory.getParentDirectory() != null) {
                parentDirectories.add(directory.getParentDirectory());
            }
        }
        return parentDirectories;
    }

    /**
     * Read the input skipping any blank lines
     *
     * @param input
     * @return
     * @throws IOException
     */
    public static String readLine(BufferedReader input) {
        String line = null;
        try {
            line = input.readLine();
            while (StringUtils.EMPTY.equals(line)) {
                line = input.readLine();
            }
        } catch (IOException ignore) { //NOSONAR
            // ignore this error
        }
        return line;
    }
}
