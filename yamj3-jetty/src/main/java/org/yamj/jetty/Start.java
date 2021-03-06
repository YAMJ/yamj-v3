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
package org.yamj.jetty;

import static org.yamj.common.type.ExitType.CMDLINE_ERROR;
import static org.yamj.common.type.ExitType.STARTUP_FAILURE;
import static org.yamj.common.type.ExitType.SUCCESS;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.common.cmdline.CmdLineException;
import org.yamj.common.cmdline.CmdLineOption;
import org.yamj.common.cmdline.CmdLineParser;
import org.yamj.common.model.YamjInfo;
import org.yamj.common.model.YamjInfoBuild;
import org.yamj.common.tools.SystemTools;
import org.yamj.common.type.ExitType;

public final class Start {

    private static final Logger LOG = LoggerFactory.getLogger(Start.class);
    private static final String WAR_DIR = "lib/";
    private static final String WAR_FILE_RELEASE = "yamj3-core-3.0.war";
    private static final String WAR_FILE_SNAPSHOT = "yamj3-core-3.0-SNAPSHOT.war";
    
    private static final String SKINS_DIR = "skins/";
    private static final String[] DEFAULT_WELCOME_PAGES = {"yamj.html", "yamj3.html", "index.html"};
    private static final String SERVER_ERROR = "Server error";

    private static String yamjHome = ".";
	private static String RESOURCES_DIR = yamjHome + "/resources/";	
    private static int yamjPort = 8888;
    private static int yamjShutdownTimeout = 5000;
    private static boolean yamjStopAtShutdown = true;

    private Start() {
        // empty private constructor
    }

    public static void main(String[] args) {
        PropertyConfigurator.configure("config/log4j-core.properties");

        YamjInfo yi = new YamjInfo(YamjInfoBuild.JETTY);
        yi.printHeader();

        CmdLineParser parser = getCmdLineParser();
        ExitType status;
        try {
            parser.parse(args);

            if (parser.userWantsHelp()) {
                help(parser);
                status = SUCCESS;
            } else {
                status = startUp(parser);
            }
        } catch (CmdLineException ex) {
            LOG.error("Failed to parse command line options: {}", ex.getMessage());
            LOG.trace("Command line parser error", ex);
            help(parser);
            status = CMDLINE_ERROR;
        }

        LOG.info("Exiting with status '{}', return code {}", status.toString(), status.getReturn());
        System.exit(status.getReturn());
    }

    private static ExitType startUp(CmdLineParser parser) { //NOSONAR
        if (StringUtils.isNotBlank(parser.getParsedOptionValue("h"))) {
            yamjHome = parser.getParsedOptionValue("h");
			
        }
		// set the resources_dir according with the home_dir if required
		String resources_dir = yamjHome +  "/resources/"; 
		RESOURCES_DIR = resources_dir;
        yamjPort = convertToInt(parser.getParsedOptionValue("p"), yamjPort);
        yamjShutdownTimeout = convertToInt(parser.getParsedOptionValue("t"), yamjShutdownTimeout);
        yamjStopAtShutdown = convertToBoolean(parser.getParsedOptionValue("s"), yamjStopAtShutdown);

        // first release WAR, then snapshot WAR
        String warFilename = FilenameUtils.concat(yamjHome, WAR_DIR + WAR_FILE_RELEASE);
		
        File warFile = new File(warFilename);
        if (!warFile.exists()) {
            warFilename = FilenameUtils.concat(yamjHome, WAR_DIR + WAR_FILE_SNAPSHOT);
            warFile = new File(warFilename);
        }        

        if (warFile.exists()) {
            try {
                // This is a temporary fix until the yamj3.home can be read from the servlet
                SystemTools.checkSystemProperty("yamj3.home", new File(yamjHome).getCanonicalPath());
            } catch (IOException ex) { //NOSONAR
                SystemTools.checkSystemProperty("yamj3.home", yamjHome);
            }
            
            LOG.info("YAMJ Home: '{}'", yamjHome);
			LOG.info("Yamj resources_dir: '{}'", RESOURCES_DIR);
            LOG.info("YAMJ Port: {}", yamjPort);
            LOG.info("YAMJ Shudown Timeout: {}ms", yamjShutdownTimeout);
            LOG.info("YAMJ {} stop at Shutdown", yamjStopAtShutdown ? "will" : "will not");
            LOG.info("Using war file: {}", warFilename);
            LOG.info("");
        } else {
            help(parser);
            LOG.info("");
            LOG.error("Initialisation error!");
            LOG.error("Please ensure that the WAR file is in the '{}' directory", warFile.getParent());
            return STARTUP_FAILURE;
        }

        LOG.info("Starting server...");
        System.setProperty("yamj3.core.port", String.valueOf(yamjPort));
        Server server = new Server(yamjPort);
        server.setGracefulShutdown(yamjShutdownTimeout);
        server.setStopAtShutdown(yamjStopAtShutdown);

        try {
            WebAppContext webapp = new WebAppContext();
            webapp.setContextPath("/yamj3");
            webapp.setWar(warFile.getCanonicalPath());

            // Ensure the 'RESOURCES_DIR' directory is created
            FileUtils.forceMkdir(new File(RESOURCES_DIR));
            // Allow the jetty server to serve the artwork files (and any others) from the 'RESOURCES_DIR' directory
            ResourceHandler resourceDirHandler = new ResourceHandler();
            resourceDirHandler.setResourceBase(RESOURCES_DIR);
            resourceDirHandler.setWelcomeFiles(DEFAULT_WELCOME_PAGES);
            resourceDirHandler.setDirectoriesListed(true);
            LOG.info("Resource base: {}", resourceDirHandler.getResourceBase());

            // Ensure the 'SKIN_DIR' directory is created
            String skinDir = FilenameUtils.concat(RESOURCES_DIR, SKINS_DIR);
            FileUtils.forceMkdir(new File(skinDir));
            LOG.info("Skins directory: {}", skinDir);

            HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[]{webapp, resourceDirHandler, new DefaultHandler()});
            server.setHandler(handlers);

            if (server.getThreadPool() instanceof QueuedThreadPool) {
                ((QueuedThreadPool) server.getThreadPool()).setMaxIdleTimeMs(2000);
            }

            server.start();
            server.join();

            LOG.info("Server run completed.");
            LOG.info("Exiting.");
            return SUCCESS;
        } catch (IOException ex) {
            LOG.error("Failed to start server, error: ", ex.getMessage());
            LOG.trace(SERVER_ERROR, ex);
            return STARTUP_FAILURE;
        } catch (InterruptedException ex) {
            LOG.error("Server interrupted, error: ", ex.getMessage());
            LOG.trace(SERVER_ERROR, ex);
            return STARTUP_FAILURE;
        } catch (Exception ex) {
            LOG.error("General server eror, message: ", ex.getMessage());
            LOG.trace(SERVER_ERROR, ex);
            return STARTUP_FAILURE;
        }
    }

    /**
     * Print the parse descriptions
     *
     * @param parser
     */
    private static void help(CmdLineParser parser) {
        LOG.info(parser.getDescriptions());
    }

    /**
     * Create the command line parser
     *
     * @return
     */
    private static CmdLineParser getCmdLineParser() {
        CmdLineParser parser = new CmdLineParser();
        parser.addOption(new CmdLineOption("h", "home", "the home directory for jetty, default: '" + yamjHome + "'", false, true));
        parser.addOption(new CmdLineOption("p", "port", "The port for the core server, default: " + yamjPort, false, true));
        parser.addOption(new CmdLineOption("t", "shutdown timeout", "The time allowed for the server to gracefully stop, default: " + yamjShutdownTimeout + "ms", false, true));
        parser.addOption(new CmdLineOption("s", "stop shutdown", "Shutdown the server when exiting, default: " + yamjStopAtShutdown, false, false));

        return parser;
    }

    private static int convertToInt(String toConvert, int defaultValue) {
        if (StringUtils.isNumeric(toConvert)) {
            return Integer.parseInt(toConvert);
        }
        return defaultValue;
    }

    private static boolean convertToBoolean(String toConvert, boolean defaultValue) {
        if (StringUtils.isNotBlank(toConvert)) {
            return Boolean.parseBoolean(toConvert);
        }
        return defaultValue;
    }
}
