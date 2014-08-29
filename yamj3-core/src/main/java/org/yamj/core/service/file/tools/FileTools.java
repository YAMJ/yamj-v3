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
package org.yamj.core.service.file.tools;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTools {

    private static final Logger LOG = LoggerFactory.getLogger(FileTools.class);
    private static final int BUFF_SIZE = 16 * 1024;
    public static final String DEFAULT_CHARSET = "UTF-8";
    private static Lock mkdirsLock = new ReentrantLock();
    
    /**
     * One buffer for each thread to allow threaded copies
     */
    private static final ThreadLocal<byte[]> THREAD_BUFFER = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[BUFF_SIZE];
        }
    };

    public static int copy(InputStream is, OutputStream os) throws IOException {
        int bytesCopied = 0;
        byte[] buffer = THREAD_BUFFER.get();
        try {
            while (Boolean.TRUE) {
                int amountRead = is.read(buffer);
                if (amountRead == -1) {
                    break;
                } else {
                    bytesCopied += amountRead;
                }
                os.write(buffer, 0, amountRead);
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException error) {
                // ignore
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException error) {
                // ignore
            }
        }
        return bytesCopied;
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
        boolean returnValue = Boolean.FALSE;

        if (!src.exists()) {
            LOG.error("The file '{}' does not exist", src);
            return returnValue;
        }

        if (dst.isDirectory()) {
            makeDirectories(dst);
            returnValue = copyFile(src, new File(dst + File.separator + src.getName()));
        } else {
            FileInputStream inSource = null;
            FileOutputStream outSource = null;
            FileChannel inChannel = null;
            FileChannel outChannel = null;
            try {
                // gc: copy using file channels, potentially much faster
                inSource = new FileInputStream(src);
                outSource = new FileOutputStream(dst);
                inChannel = inSource.getChannel();
                outChannel = outSource.getChannel();

                long p = 0, s = inChannel.size();
                while (p < s) {
                    p += inChannel.transferTo(p, 1024 * 1024, outChannel);
                }
                return Boolean.TRUE;
            } catch (IOException error) {
                LOG.error("Failed copying file '{}' to '{}'", src, dst);
                LOG.error("File copying error", error);
                returnValue = Boolean.FALSE;
            } finally {
                if (inChannel != null) {
                    try {
                        inChannel.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }
                if (inSource != null) {
                    try {
                        inSource.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }

                if (outChannel != null) {
                    try {
                        outChannel.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }
                if (outSource != null) {
                    try {
                        outSource.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }
            }
        }

        return returnValue;
    }

    /**
     * Create all directories up to the level of the file passed
     *
     * @param sourceDirectory Source directory or file to create the directories directories
     * @return
     */
    public static boolean makeDirectories(String filename) {
        return makeDirectories(new File(filename));
    }

    /**
     * Create all directories up to the level of the file passed
     *
     * @param sourceDirectory Source directory or file to create the directories
     * @param numOfTries Number of attempts that will be made to create the directories
     * @return
     */
    public static boolean makeDirectories(String filename, int numOfTries) {
        return makeDirectories(new File(filename), numOfTries);
    }

    /**
     * Create all directories up to the level of the file passed
     *
     * @param sourceDirectory Source directory or file to create the directories directories
     * @return
     */
    public static boolean makeDirectories(File file) {
        return makeDirectories(file, 10);
    }

    /**
     * Create all directories up to the level of the file passed
     *
     * @param sourceDirectory Source directory or file to create the directories
     * @param numOfTries Number of attempts that will be made to create the directories
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
            return Boolean.TRUE;
        }
        LOG.debug("Creating directories for {} ", targetDirectory.getAbsolutePath());

        mkdirsLock.lock();
        try {
            boolean status = targetDirectory.mkdirs();
            int looper = 1;
            while (!status && looper++ <= numOfTries) {
                status = targetDirectory.mkdirs();
            }
            if (status && looper > 10) {
                LOG.error("Failed creating the directory '{}'. Ensure this directory is read/write!", targetDirectory.getAbsolutePath());
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        } finally {
            mkdirsLock.unlock();
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
        return readFileToString(file, DEFAULT_CHARSET);
    }

    /**
     * Read a file and return it as a string
     *
     * @param file
     * @param encoding
     * @return the file content
     */
    public static String readFileToString(File file, String encoding) {
        String data = "";
        if (file == null) {
            LOG.error("Failed reading file, file is null");
        } else {
            try {
                data = FileUtils.readFileToString(file, encoding);
            } catch (Exception ex) {
                LOG.error("Failed reading file {}", file.getName());
                LOG.error("Error", ex);
            }
        }
        return data;
    }
}
