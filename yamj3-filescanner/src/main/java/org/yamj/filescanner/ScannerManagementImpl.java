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
package org.yamj.filescanner;

import org.yamj.common.cmdline.CmdLineParser;
import org.yamj.common.dto.StageDirectoryDTO;
import org.yamj.common.dto.StageFileDTO;
import org.yamj.common.remote.service.GitHubService;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.DirectoryType;
import org.yamj.common.type.ExitType;
import org.yamj.filescanner.comparator.FileTypeComparator;
import org.yamj.filescanner.model.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.filescanner.model.LibraryCollection;
import org.yamj.filescanner.model.StatType;
import org.yamj.filescanner.service.SystemInfoCore;
import org.yamj.filescanner.tools.DirectoryEnding;
import org.yamj.filescanner.tools.Watcher;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.util.CollectionUtils;
import org.yamj.common.model.YamjInfo;
import org.yamj.common.tools.StringTools;
import org.yamj.common.type.StatusType;
import org.yamj.common.util.KeywordMap;
import org.yamj.filescanner.model.TimeType;

/**
 * Performs an initial scan of the library location and then updates when changes occur.
 *
 * @author Stuart
 */
public class ScannerManagementImpl implements ScannerManagement {

    /*
     * TODO: choose between watcher process and simple re-scan
     * TODO: determine what files have changed between scans
     */
    private static final Logger LOG = LoggerFactory.getLogger(ScannerManagementImpl.class);
    // The default watched status
    private static final Boolean DEFAULT_WATCH_STATE = PropertyTools.getBooleanProperty("filescanner.watch.default", Boolean.FALSE);
    @Autowired
    private LibraryCollection libraryCollection;
    @Autowired
    private SystemInfoCore pingCore;
    @Autowired
    private GitHubService githubService;
    // ImportDTO constants
    private static final String DEFAULT_CLIENT = PropertyTools.getProperty("filescanner.default.client", "FileScanner");
    private static final String DEFAULT_PLAYER_PATH = PropertyTools.getProperty("filescanner.default.playerpath", "");
    private static final String DEFAULT_SPLIT = ",|;";
    private static final String FILE_MJBIGNORE = ".mjbignore";
    // Date check
    private static final int MAX_INSTALL_AGE = PropertyTools.getIntProperty("filescanner.installation.maxdays", 1);
    // Map of filenames & extensions that cause scanning of a directory to stop or a filename to be ignored
    private static final Map<String, List<String>> DIR_EXCLUSIONS = new HashMap<String, List<String>>();
    private static final List<Pattern> DIR_IGNORE_FILES;
    // YAMJ Information
    private static final YamjInfo YAMJ_INFO = new YamjInfo(ScannerManagementImpl.class);

    static {
        // Set up the break scanning list. A "null" for the list means all files.
        // Ensure all filenames and extensions are lowercase
        boolean nmjCompliant = PropertyTools.getBooleanProperty("filescanner.nmjCompliant", Boolean.FALSE);
        KeywordMap fsIgnore = PropertyTools.getKeywordMap("filescanner.ignore", "");

        DIR_EXCLUSIONS.put(FILE_MJBIGNORE, null);
        if (nmjCompliant) {
            DIR_EXCLUSIONS.put(".no_all.nmj", null);
        }

        List<String> keywordList = processKeywords(fsIgnore, "file");
        if (CollectionUtils.isEmpty(keywordList)) {
            DIR_IGNORE_FILES = Collections.emptyList();
        } else {
            DIR_IGNORE_FILES = new ArrayList<Pattern>(keywordList.size());
            for (String keyword : keywordList) {
                try {
                    String regex = keyword.replace("?", ".?").replace("*", ".*?");
                    LOG.debug("Replaced pattern '{}' with regex '{}'", keyword, regex);
                    DIR_IGNORE_FILES.add(Pattern.compile(regex));
                } catch (PatternSyntaxException ex) {
                    LOG.warn("Pattern '{}' not recognised. Error: {}", keyword, ex.getMessage());
                }
            }
        }

        keywordList = processKeywords(fsIgnore, "dir");
        if (!CollectionUtils.isEmpty(keywordList)) {
            for (String keyword : keywordList) {
                DIR_EXCLUSIONS.put(keyword.toLowerCase(), null);
            }
        }

        keywordList = processKeywords(fsIgnore, "video");
        if (!keywordList.isEmpty()) {
            DIR_EXCLUSIONS.put(".no_video.yamj", keywordList);
            if (nmjCompliant) {
                DIR_EXCLUSIONS.put(".no_video.nmj", keywordList);
            }
        }

        keywordList = processKeywords(fsIgnore, "image");
        if (!keywordList.isEmpty()) {
            DIR_EXCLUSIONS.put(".no_image.yamj", keywordList);
            if (nmjCompliant) {
                DIR_EXCLUSIONS.put(".no_photo.nmj", keywordList);
            }
        }

        keywordList = processKeywords(fsIgnore, "other");
        if (!keywordList.isEmpty()) {
            DIR_EXCLUSIONS.put(".no_other.yamj", keywordList);
        }
        LOG.debug("Directory exclusions: {}", DIR_EXCLUSIONS.toString());
        LOG.debug("File exclusions: {}", DIR_IGNORE_FILES);
    }

    private static List<String> processKeywords(KeywordMap fsIgnore, String keyName) {
        if (fsIgnore.containsKey(keyName) && StringUtils.isNotBlank(fsIgnore.get(keyName))) {
            return StringTools.splitList(fsIgnore.get(keyName), DEFAULT_SPLIT);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Start the scanner and process the command line properties.
     *
     * @param parser
     * @return
     */
    @Override
    public ExitType runScanner(CmdLineParser parser) {
        checkGitHubStatus();
        libraryCollection.setDefaultClient(DEFAULT_CLIENT);
        libraryCollection.setDefaultPlayerPath(DEFAULT_PLAYER_PATH);
        pingCore.check(0, 0);   // Do a quick check of the status of the connection

        String directoryProperty = parser.getParsedOptionValue("d");
        boolean watchEnabled = parseWatchStatus(parser.getParsedOptionValue("w"));
        String libraryFilename = parser.getParsedOptionValue("l");

        if (StringUtils.isNotBlank(libraryFilename)) {
            List<String> libraryList = Arrays.asList(libraryFilename.split(DEFAULT_SPLIT));
            libraryCollection.processLibraryList(libraryList, watchEnabled);
        }

        if (StringUtils.isNotBlank(directoryProperty)) {
            LOG.info("Adding directory from command line: {}", directoryProperty);
            libraryCollection.addLibraryDirectory(directoryProperty, watchEnabled);
        }

        LOG.info("Found {} libraries to process.", libraryCollection.size());
        if (libraryCollection.size() == 0) {
            return ExitType.NO_DIRECTORY;
        }

//        String saveFilename="myLibrary.xml";
//        LOG.info("Saving library to: {}",saveFilename);
//        libraryCollection.saveLibraryFile(saveFilename);

        // Send all libraries to be scanned
        ExitType status = ExitType.SUCCESS;
        for (Library library : libraryCollection.getLibraries()) {
            library.getStatistics().setTime(TimeType.START);
            status = scan(library);
            library.getStatistics().setTime(TimeType.END);
            library.setScanningComplete(Boolean.TRUE);
            LOG.info("Scanning completed.");
        }

        // Wait for the libraries to be sent
        boolean allDone;
        do {
            allDone = Boolean.TRUE;
            for (Library library : libraryCollection.getLibraries()) {
                LOG.info("Library '{}' sending status: {}", library.getImportDTO().getBaseDirectory(), library.isSendingComplete() ? "Done" : "Not Done");
                allDone = allDone && library.isSendingComplete();
            }

            if (!allDone) {
                try {
                    LOG.info("Waiting for library sending to complete...");
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ex) {
                    LOG.trace("Interrupted whilst waiting for threads to complete.");
                }
            }
        } while (!allDone);

        LOG.info(StringUtils.repeat("*", 50));
        LOG.info("Completed initial sending of all libraries ({} total).",libraryCollection.size());
        LOG.info("");
        LOG.info("Library statistics:");
        for (Library library : libraryCollection.getLibraries()) {
            LOG.info("Description: '{}'", library.getDescription());
            LOG.info("{}", library.getStatistics().generateStatistics(Boolean.TRUE));
        }

        if (watchEnabled) {
            Watcher wd = new Watcher();
            Boolean directoriesToWatch = Boolean.TRUE;

            for (Library library : libraryCollection.getLibraries()) {
                String dirToWatch = library.getImportDTO().getBaseDirectory();
                if (library.isWatch()) {
                    LOG.info("Watching directory '{}' for changes...", dirToWatch);
                    wd.addDirectory(dirToWatch);
                    directoriesToWatch = Boolean.TRUE;
                } else {
                    LOG.info("Watching skipped for directory '{}'", dirToWatch);
                }
            }

            if (directoriesToWatch) {
                wd.processEvents();
                LOG.info("Watching directory '{}' completed", directoryProperty);
            } else {
                LOG.info("No directories marked for watching.");
            }
        } else {
            LOG.info("Watching not enabled.");
        }

        LOG.info("Exiting with status {}", status);

        return status;
    }

    /**
     * Start scanning a library.
     *
     * @param library
     * @return
     */
    private ExitType scan(Library library) {
        library.getStatistics().setTime(TimeType.SCANNING_START);

        ExitType status = ExitType.SUCCESS;
        File baseDirectory = new File(library.getImportDTO().getBaseDirectory());
        LOG.info("Scanning library '{}'...", baseDirectory.getAbsolutePath());

        if (!baseDirectory.exists()) {
            LOG.info("Failed to read directory '{}'", baseDirectory.getAbsolutePath());
            return ExitType.NO_DIRECTORY;
        }

        scanDir(library, baseDirectory);

        library.getStatistics().setTime(TimeType.SCANNING_END);
        return status;
    }

    /**
     * Scan a directory (and recursively any other directories contained
     *
     * @param library
     * @param parentDto
     * @param directory
     */
    private StageDirectoryDTO scanDir(Library library, File directory) {
        DirectoryType dirType = DirectoryEnding.check(directory);
        StageDirectoryDTO stageDir;

        LOG.info("Scanning directory '{}', detected type - {}", library.getRelativeDir(directory), dirType);

        if (dirType == DirectoryType.BLURAY || dirType == DirectoryType.DVD) {
            // Don't scan BLURAY or DVD structures
            LOG.info("Skipping directory '{}' as its a {} type", directory.getAbsolutePath(), dirType);
            library.getStatistics().increment(dirType == DirectoryType.BLURAY ? StatType.BLURAY : StatType.DVD);
            stageDir = null;
        } else if (DIR_EXCLUSIONS.containsKey(directory.getName().toLowerCase())) {
            LOG.info("Skipping directory '{}' as its in the exclusion list.",directory.getAbsolutePath());
            stageDir = null;
        } else {
            try {
                if (FileUtils.directoryContains(directory, new File(directory, FILE_MJBIGNORE))) {
                    LOG.debug("Exclusion file '{}' found, skipping scanning of directory {}.", FILE_MJBIGNORE, directory.getName());
                    return null;
                }
            } catch (IOException ex) {
                LOG.trace("Failed to seach for '{}' in the directory {}", FILE_MJBIGNORE, directory.getName());
            }

            stageDir = new StageDirectoryDTO();
            stageDir.setPath(directory.getAbsolutePath());
            stageDir.setDate(directory.lastModified());

            library.getStatistics().increment(StatType.DIRECTORY);

            List<File> currentFileList = Arrays.asList(directory.listFiles());
            FileTypeComparator comp = new FileTypeComparator(Boolean.FALSE);
            Collections.sort(currentFileList, comp);

            /*
             * We need to scan the directory and look for any of the exclusion filenames.
             *
             * We then build a list of those excluded extensions, so that when we scan the filename list we can exclude the unwanted files.
             */
            List<String> exclusions = new ArrayList<String>();
            for (File file : currentFileList) {
                if (file.isFile()) {
                    String lcFilename = file.getName().toLowerCase();
                    if (DIR_EXCLUSIONS.containsKey(lcFilename)) {
                        if (CollectionUtils.isEmpty(DIR_EXCLUSIONS.get(lcFilename))) {
                            // Because the value is null or empty we exclude the whole directory, so quit now.
                            LOG.debug("Exclusion file '{}' found, skipping scanning of directory {}.", lcFilename, file.getParent());
                            // All files to be excluded, so quit
                            return null;
                        } else {
                            // We found a match, so add it to our local copy
                            LOG.debug("Exclusion file '{}' found, will exclude all {} file types", lcFilename, DIR_EXCLUSIONS.get(lcFilename).toString());
                            exclusions.addAll(DIR_EXCLUSIONS.get(lcFilename));
                            // Skip to the next file, theres no need of further processing
                            continue;
                        }
                    }
                } else {
                    // First directory we find, we can stop (because we sorted the files first)
                    break;
                }
            }

            // Create a precompiled Matcher for use later (Doesn't matter what the values are)
            Matcher matcher = Pattern.compile(FILE_MJBIGNORE).matcher(FILE_MJBIGNORE);

            // Scan the directory properly
            for (File file : currentFileList) {
                boolean excluded = Boolean.FALSE;
                if (file.isFile()) {
                    String lcFilename = file.getName().toLowerCase();
                    if (exclusions.contains(FilenameUtils.getExtension(lcFilename)) || DIR_EXCLUSIONS.containsKey(lcFilename)) {
                        LOG.debug("File name '{}' excluded because it's listed in the exlusion list for this directory", file.getName());
                        continue;
                    }

                    // Process the DIR_IGNORE_FILES
                    for (Pattern pattern : DIR_IGNORE_FILES) {
                        matcher.reset(lcFilename).usePattern(pattern);
                        if (matcher.matches()) {
                            // Found the file pattern, so skip the file
                            LOG.debug("File name '{}' excluded because it matches exlusion pattern '{}'", file.getName(), pattern.pattern());
                            excluded = Boolean.TRUE;
                            break;
                        }
                    }

                    if (!excluded) {
                        stageDir.addStageFile(scanFile(file));
                        library.getStatistics().increment(StatType.FILE);
                    }
                } else {
                    // First directory we find, we can stop (because we sorted the files first)
                    break;
                }
            }

            library.addDirectory(stageDir);
            queueForSending(library, stageDir);

            // Resort the files with directories first
            comp.setDirectoriesFirst(Boolean.TRUE);
            Collections.sort(currentFileList, comp);

            // Now scan the directories
            for (File scanDir : currentFileList) {
                if (scanDir.isDirectory()) {
                    if (scanDir(library, scanDir) == null) {
                        LOG.info("Not adding directory '{}', no files found or all excluded", scanDir.getAbsolutePath());
                    }
                } else {
                    // First file we find, we can stop (because we are sorted directories first)
                    break;
                }
            }
        }
        return stageDir;
    }

    /**
     * Scan an individual file
     *
     * @param library
     * @param parentDto
     * @param file
     */
    private StageFileDTO scanFile(File file) {
        LOG.info("Scanning file '{}'", file.getName());
        return new StageFileDTO(file);
    }

    /**
     * Get the watched status from the command line property or return the default value.
     *
     * @param parsedOptionValue the property from the command line
     * @return
     */
    private boolean parseWatchStatus(String parsedOptionValue) {
        if (StringUtils.isBlank(parsedOptionValue)) {
            return DEFAULT_WATCH_STATE;
        }
        return Boolean.parseBoolean(parsedOptionValue);
    }

    private void checkGitHubStatus() {
        try {
            DateTime fsDate = YAMJ_INFO.getBuildDateTime();
            boolean installationOk = githubService.checkInstallationDate(fsDate, MAX_INSTALL_AGE);

            if (installationOk) {
                LOG.info("Installation is less than {} days old.", MAX_INSTALL_AGE);
            } else {
                LOG.error("***** Your installation is more than {} days old. You should consider updating! *****", MAX_INSTALL_AGE);
            }
        } catch (RemoteConnectFailureException ex) {
            LOG.warn("Failed to get GitHub status, error: {}", ex.getMessage());
        } catch (RemoteAccessException ex) {
            LOG.warn("Failed to get GitHub status, error: {}", ex.getMessage());
        }
    }

    /**
     * Add the file to the library for sending to the core
     *
     * @param library
     * @param stageDir
     */
    private void queueForSending(Library library, StageDirectoryDTO stageDir) {
        library.addDirectoryStatus(stageDir.getPath(), ConcurrentUtils.constantFuture(StatusType.NEW));
    }
}
