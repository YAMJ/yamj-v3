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
package org.yamj.filescanner;
import org.yamj.filescanner.service.SendToCore;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yamj.common.cmdline.CmdLineParser;
import org.yamj.common.dto.StageDirectoryDTO;
import org.yamj.common.dto.StageFileDTO;
import org.yamj.common.model.YamjInfo;
import org.yamj.common.model.YamjInfoBuild;
import org.yamj.common.remote.service.GitHubService;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.tools.StringTools;
import org.yamj.common.type.DirectoryType;
import org.yamj.common.type.ExitType;
import org.yamj.common.type.StatusType;
import org.yamj.common.util.KeywordMap;
import org.yamj.filescanner.comparator.FileTypeComparator;
import org.yamj.filescanner.model.*;
import org.yamj.filescanner.service.SystemInfoCore;
import org.yamj.filescanner.tools.DirectoryEnding;
import org.yamj.filescanner.tools.Watcher;


import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.yamj.filescanner.ApplicationContextProvider;
import org.yamj.filescanner.model.Library;
import org.yamj.filescanner.model.LibraryCollection;
import org.yamj.filescanner.model.TimeType;

/**
 * Performs an initial scan of the library location and then updates when
 * changes occur.
 *
 * @author Stuart
 */
@Service("scannerManagement")
public class ScannerManagementImpl implements ScannerManagement {

    /*
     * TODO: choose between watcher process and simple re-scan
     * TODO: determine what files have changed between scans
     */
    private static final Logger LOG = LoggerFactory.getLogger(ScannerManagementImpl.class);
    private static final int RETRY_MAX = PropertyTools.getIntProperty("filescanner.send.retry", 5);
    private final AtomicInteger runningCount = new AtomicInteger(0);
    private final AtomicInteger retryCount = new AtomicInteger(0);
	private final AtomicInteger retryWait = new AtomicInteger(0);
	private static String dirToWatch = "";
    // The default watched status
    private static final boolean DEFAULT_WATCH_STATE = PropertyTools.getBooleanProperty("filescanner.watch.default", false);
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
    private static final Map<String, List<String>> DIR_EXCLUSIONS = new HashMap<>();
    private static final List<Pattern> DIR_IGNORE_FILES;
    // YAMJ Information
    private static final YamjInfo YAMJ_INFO = new YamjInfo(YamjInfoBuild.FILESCANNER);
    // Number of seconds to wait between checks
    private static final int WAIT_10_SECONDS = 10;
    // Length of the * line
    private static final int DIVIDER_LINE_LENGTH = 50;

    static {
        // Set up the break scanning list. A "null" for the list means all files.
        // Ensure all filenames and extensions are lowercase
        boolean nmjCompliant = PropertyTools.getBooleanProperty("filescanner.nmjCompliant", false);
        KeywordMap fsIgnore = PropertyTools.getKeywordMap("filescanner.ignore", "");

        DIR_EXCLUSIONS.put(FILE_MJBIGNORE, null);
        if (nmjCompliant) {
            DIR_EXCLUSIONS.put(".no_all.nmj", null);
        }

        List<String> keywordList = processKeywords(fsIgnore, "file");
        if (CollectionUtils.isEmpty(keywordList)) {
            DIR_IGNORE_FILES = Collections.emptyList();
        } else {
            DIR_IGNORE_FILES = new ArrayList<>(keywordList.size());
            for (String keyword : keywordList) {
                try {
                    String regex = keyword.replace("?", ".?").replace("*", ".*?");
                    LOG.debug("Replaced pattern '{}' with regex '{}'", keyword, regex);
                    DIR_IGNORE_FILES.add(Pattern.compile(regex));
                } catch (PatternSyntaxException ex) {
                    LOG.warn("Pattern '{}' not recognised: {}", keyword, ex.getMessage());
                    LOG.trace("Pattern error", ex);
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
        }
        return Collections.emptyList();
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
        // Do a quick check of the status of the connection
        pingCore.check(0, 0);

        String directoryProperty = parser.getParsedOptionValue("d");
        boolean watchEnabled = parseWatchStatus(parser.getParsedOptionValue("w"));
        String libraryFilename = parser.getParsedOptionValue("l");
		
		LOG.info("watchEnabled :{}", watchEnabled);
		
		
        if (StringUtils.isNotBlank(libraryFilename)) {
            List<String> libraryList = Arrays.asList(libraryFilename.split(DEFAULT_SPLIT));
            libraryCollection.processLibraryList(libraryList, watchEnabled);
        }

        if (StringUtils.isNotBlank(directoryProperty)) {
            LOG.info("Adding directory from command line: {}", directoryProperty);

            File temp = new File(directoryProperty);
            directoryProperty = FilenameUtils.normalizeNoEndSeparator(temp.getAbsolutePath());
            LOG.info("Corrected path: {}", directoryProperty);

            libraryCollection.addLibraryDirectory(directoryProperty, watchEnabled);
        }

        LOG.info("Found {} libraries to process.", libraryCollection.size());
        if (libraryCollection.isEmpty()) {
            return ExitType.NO_DIRECTORY;
        }

        // Send all libraries to be scanned
        ExitType status = ExitType.SUCCESS;
        for (Library library : libraryCollection.getLibraries()) {
            library.getStatistics().setTime(TimeType.START);
            status = scan(library);
            library.getStatistics().setTime(TimeType.END);
            library.setScanningComplete(true);
            LOG.info("Scanning completed.");
        }

        // Wait for the libraries to be sent
        boolean allDone;
		retryWait.getAndSet(0);
        do {
            allDone = true;
            for (Library library : libraryCollection.getLibraries()) {
                LOG.info("Library '{}' sending status: {}", library.getImportDTO().getBaseDirectory(), library.isSendingComplete() ? "Done" : "Not Done");
                allDone = allDone && library.isSendingComplete();
            }

            if (!allDone) {
                try {
				// for some reason ?? the sendLibraries action doesn't send, don't wait too much time 
				// try to send one Time
					for (Library library : libraryCollection.getLibraries()) {
						if ((retryWait.get() > RETRY_MAX) && library.isScanningComplete()) 
						{
							LOG.info("Maximum number of wait-time ({}) exceeded. Try to send .", Integer.valueOf(RETRY_MAX));
							sendLibrariesOneTime();
							// if done resst all value 
							retryWait.getAndSet(0);
							allDone = true;
						}
					}
                    LOG.info("Waiting for library sending to complete...");
                    TimeUnit.SECONDS.sleep(WAIT_10_SECONDS);
					retryWait.incrementAndGet();
                } catch (InterruptedException ex) { //NOSONAR
                    LOG.trace("Interrupted whilst waiting for threads to complete");
                }
            }
        } while (!allDone);
		// exit loop, reset de wait count to 0
		retryWait.getAndSet(0);
		
        if (LOG.isInfoEnabled()) {
            LOG.info(StringUtils.repeat("*", DIVIDER_LINE_LENGTH));
            LOG.info("Completed initial sending of all libraries ({} total)", libraryCollection.size());
            LOG.info("");
            LOG.info("Library statistics:");
            for (Library library : libraryCollection.getLibraries()) {
                LOG.info("Description: '{}'", library.getDescription());
                LOG.info("{}", library.getStatistics().generateStatistics(true));
            }
        }
        // when watched is asked loop to the specified directory until the watcher detect something   
		// set status = ExitType.LOOP not SUCCESS to loop,
		// if SUCCESS is setted filescanner shutdown     
		// when the watcher detect a change now the following sequence is performed 
		// stop watching (reset keys and break watcher)
		// rescan the library
		// send the new scanning to the core (one time)
		// restart the watcher
		
        do  {
            Watcher wd;
            try {
                wd = new Watcher();
			//	LOG.debug("ScannerManagementImpl watchEnabled wd");
            } catch (UnsatisfiedLinkError ule) { //NOSONAR
                LOG.warn("Watching is not possible on this system; therefore watch service will not be used");
                wd = null;
            }

            if (wd != null) {
                boolean directoriesToWatch = false;
                for (Library library : libraryCollection.getLibraries()) {
                    dirToWatch = library.getImportDTO().getBaseDirectory();
                    if (library.isWatch()) {
                        LOG.info("Watching directory '{}' for changes...", dirToWatch);
                        wd.addDirectory(dirToWatch);
                        directoriesToWatch = true;
                    } else {
                        LOG.info("Watching skipped for directory '{}'", dirToWatch);
                    }
                }

                if (directoriesToWatch) {
				//	LOG.debug("ScannerManagementImpl start wd.processEvents() with  '{}' ", dirToWatch);
					status = ExitType.LOOP;
                    wd.processEvents();
					 for (Library library : libraryCollection.getLibraries()) {
								library.getStatistics().setTime(TimeType.START);
								status = scan(library);
								library.getStatistics().setTime(TimeType.END);
								library.setScanningComplete(true);
								LOG.info("Scanning completed.");
								library.setSendingComplete(false);
								status = ExitType.LOOP;
								sendLibrariesOneTime ();
							}
                    LOG.info("Watching directory '{}' completed", directoryProperty);
                } else {
                    LOG.info("No directories marked for watching");
                }
            }
			if (status.equals(ExitType.LOOP)) 
			{
					LOG.info("do watch with status {}", status);
					continue;
			}
			else {
				LOG.info("Exiting watch with status {}", status);
				break;
			}
        } while (watchEnabled);
		if (!watchEnabled)
			{
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
            LOG.info("Skipping directory '{}' as its in the exclusion list.", directory.getAbsolutePath());
            stageDir = null;
        } else {
            try {
                if (FileUtils.directoryContains(directory, new File(directory, FILE_MJBIGNORE))) {
                    LOG.debug("Exclusion file '{}' found, skipping scanning of directory {}.", FILE_MJBIGNORE, directory.getName());
                    return null;
                }
            } catch (IOException ex) {
                LOG.trace("Failed to seach for '{}' in the directory {}", FILE_MJBIGNORE, directory.getName());
                LOG.trace("IO error", ex);
            }

            stageDir = new StageDirectoryDTO();
            stageDir.setPath(directory.getAbsolutePath());
            stageDir.setDate(directory.lastModified());

            library.getStatistics().increment(StatType.DIRECTORY);

            File[] files = directory.listFiles();
            if (files == null) {
                return stageDir;
            }
            
            final List<File> currentFileList = Arrays.asList(files);
            final FileTypeComparator comp = new FileTypeComparator(false);
            Collections.sort(currentFileList, comp);

            /*
             * We need to scan the directory and look for any of the exclusion filenames.
             *
             * We then build a list of those excluded extensions, so that when we scan the filename list we can exclude the unwanted files.
             */
            List<String> exclusions = new ArrayList<>();
            for (File file : currentFileList) {
                if (!file.isFile()) {
                    // First directory we find, we can stop (because we sorted the files first)
                    break;
                }
                
                final String lcFilename = file.getName().toLowerCase();
                if (DIR_EXCLUSIONS.containsKey(lcFilename)) {
                    if (CollectionUtils.isEmpty(DIR_EXCLUSIONS.get(lcFilename))) {
                        // Because the value is null or empty we exclude the whole directory, so quit now.
                        LOG.debug("Exclusion file '{}' found, skipping scanning of directory {}.", lcFilename, file.getParent());
                        // All files to be excluded, so quit
                        return null;
                    }

                    // We found a match, so add it to our local copy
                    LOG.debug("Exclusion file '{}' found, will exclude all {} file types", lcFilename, DIR_EXCLUSIONS.get(lcFilename).toString());
                    exclusions.addAll(DIR_EXCLUSIONS.get(lcFilename));
                    // Skip to the next file, theres no need of further processing
                }
            }

            // Create a precompiled Matcher for use later (Doesn't matter what the values are)
            Matcher matcher = Pattern.compile(FILE_MJBIGNORE).matcher(FILE_MJBIGNORE);

            // Scan the directory properly
            for (File file : currentFileList) {
                if (!file.isFile()) {
                    // First directory we find, we can stop (because we sorted the files first)
                    break;
                }

                boolean excluded = false;
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
                        excluded = true;
                        break;
                    }
                }

                if (!excluded) {
                    stageDir.addStageFile(scanFile(file));
                    library.getStatistics().increment(StatType.FILE);
                }
            }

            library.addDirectory(stageDir);
            queueForSending(library, stageDir);

            // Resort the files with directories first
            comp.setDirectoriesFirst(true);
            Collections.sort(currentFileList, comp);

            // Now scan the directories
            for (File scanDir : currentFileList) {
                if (!scanDir.isDirectory()) {
                    // First file we find, we can stop (because we are sorted directories first)
                    break;
                }
                
                if (scanDir(library, scanDir) == null) {
                    LOG.info("Not adding directory '{}', no files found or all excluded", scanDir.getAbsolutePath());
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
    private static StageFileDTO scanFile(File file) {
        LOG.info("Scanning file '{}'", file.getName());
        return new StageFileDTO(file);
    }

    /**
     * Get the watched status from the command line property or return the
     * default value.
     *
     * @param parsedOptionValue the property from the command line
     * @return
     */
    private static boolean parseWatchStatus(String parsedOptionValue) {
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
            LOG.trace("Exception:", ex);
        } catch (RemoteAccessException ex) {
            LOG.warn("Failed to get GitHub status, error: {}", ex.getMessage());
            LOG.trace("Exception:", ex);
        }
    }

    /**
     * Add the file to the library for sending to the core
     *
     * @param library
     * @param stageDir
     */
    private static void queueForSending(Library library, StageDirectoryDTO stageDir) {
        library.addDirectoryStatus(stageDir.getPath(), ConcurrentUtils.constantFuture(StatusType.NEW));
    }
	
	/**
	* sequence added to start sending new scanned libraries once to the core
	* @param there is no param 
	* add a new task 
	* start send library
	* check status 
	* send to the core
	* @return 
	*/
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor yamjExecutor;

	private void sendLibrariesOneTime() { //NOSONAR
	
    
        if (retryCount.get() > RETRY_MAX) {
            LOG.info("Maximum number of retries ({}) exceeded. No further processing attempted.", Integer.valueOf(RETRY_MAX));
            for (Library library : libraryCollection.getLibraries()) {
                library.setSendingComplete(true);
            }
            return;
        }

        LOG.info("There are {} libraries to process, there have been {} consecutive failed attempts to send.", libraryCollection.size(), retryCount.get());
        LOG.info("There are {} items currently queued to be sent to core.", runningCount.get());

        for (Library library : libraryCollection.getLibraries()) {
            library.getStatistics().setTime(TimeType.SENDING_START);
            LOG.info("  {} has {} directories and the file scanner has {} scanning.",
                    library.getImportDTO().getBaseDirectory(),
                    library.getDirectories().size(),
                    library.isScanningComplete() ? "finished" : "not finished");

            try {
                for (Map.Entry<String, Future<StatusType>> entry : library.getDirectoryStatus().entrySet()) {
                    LOG.info("    {}: {}", entry.getKey(), entry.getValue().isDone() ? entry.getValue().get() : "Being processed");

                    if (checkStatus(library, entry.getValue(), entry.getKey())) {
                        if (retryCount.get() > 0) {
                            LOG.debug("Successfully sent file to server, resetting retry count to 0 from {}.", retryCount.getAndSet(0));
                        } else {
                            LOG.debug("Successfully sent file to server.");
                            retryCount.set(0);
                        }
                    } else {
                        // Make sure this is set to false
                        library.setSendingComplete(false);
                        LOG.warn("Failed to send a file, this was failed attempt #{}. Waiting until next run...", retryCount.incrementAndGet());
                        return;
                    }
                }

                // Don't stop sending until the scanning is completed and there are no running tasks
                if (library.isScanningComplete() && runningCount.get() <= 0) {
                    // When we reach this point we should have completed the library sending
                    LOG.info("Sending complete for {}", library.getImportDTO().getBaseDirectory());
                    library.setSendingComplete(true);
                    library.getStatistics().setTime(TimeType.SENDING_END);
                } else {
                    LOG.info("  {}: Scanning and/or sending ({} left) is not complete. Waiting for more files to send.", library.getImportDTO().getBaseDirectory(), runningCount.get());
                }
            } catch (InterruptedException ex) { //NOSONAR
                LOG.info("Interrupted error: {}", ex.getMessage());
            } catch (ExecutionException ex) {
                LOG.warn("Execution error", ex);
            }
        }
    }
  private boolean checkStatus(Library library, Future<StatusType> statusType, String directory) throws InterruptedException, ExecutionException {
        boolean sendStatus;

        
        if (statusType.isDone()) {
            StatusType processingStatus = statusType.get();
            
            if (processingStatus == StatusType.NEW) {
                LOG.info("    Sending '{}' to core for processing.", directory);
                sendStatus = sendToCore(library, directory);
            } else if (processingStatus == StatusType.UPDATED) {
                LOG.info("    Sending updated '{}' to core for processing.", directory);
                sendStatus = sendToCore(library, directory);
            } else if (processingStatus == StatusType.ERROR) {
                LOG.info("    Resending '{}' to core for processing (was in error status).", directory);
                sendStatus = sendToCore(library, directory);
            } else if (processingStatus == StatusType.DONE) {
                LOG.info("    Completed: '{}'", directory);
                sendStatus = true;
            } else {
                LOG.warn("    Unknown processing status {} for {}", processingStatus, directory);
                // Assume this is correct, so we don't get stuck
                sendStatus = true;
            }
        } else {
            LOG.warn("    Still being procesed {}", directory);
            sendStatus = false;
        }
        return sendStatus;
    }
   /**
     * Send the directory to the core.
     *
     * Will get the StageDirectoryDTO from the library for sending.
     *
     * @param library
     * @param sendDir
     */
    private boolean sendToCore(Library library, String sendDir) {
        StageDirectoryDTO stageDto = library.getDirectory(sendDir);
        boolean sentOk = false;

        if (stageDto == null) {
            LOG.warn("StageDirectoryDTO for '{}' is null!", sendDir);
            // We do not want to send this again.
            library.addDirectoryStatus(sendDir, ConcurrentUtils.constantFuture(StatusType.INVALID));
            return true;
        }

        LOG.info("Sending #{}: {}", runningCount.incrementAndGet(), sendDir);

        ApplicationContext appContext = ApplicationContextProvider.getApplicationContext();
        SendToCore stc = (SendToCore) appContext.getBean("sendToCore");
        stc.setImportDto(library.getImportDTO(stageDto));
        stc.setCounter(runningCount);
        FutureTask<StatusType> task = new FutureTask<>(stc);

        try {
            yamjExecutor.submit(task);
            library.addDirectoryStatus(stageDto.getPath(), task);
            sentOk = true;
        } catch (TaskRejectedException ex) {
            LOG.warn("Send queue full. '{}' will be sent later.", stageDto.getPath());
            LOG.trace("Exception: ", ex);
            library.addDirectoryStatus(stageDto.getPath(), ConcurrentUtils.constantFuture(StatusType.NEW));
        }
        
        return sentOk;
    }
  
}
