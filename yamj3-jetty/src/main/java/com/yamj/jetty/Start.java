package com.yamj.jetty;

import com.yamj.common.cmdline.CmdLineException;
import com.yamj.common.cmdline.CmdLineOption;
import com.yamj.common.cmdline.CmdLineParser;
import com.yamj.common.tools.ClassTools;
import com.yamj.common.type.ExitType;
import static com.yamj.common.type.ExitType.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Start {

    private static final Logger LOG = LoggerFactory.getLogger(Start.class);
    private static final String LOG_FILENAME = "yamj-jetty";
    private static final String WAR_DIR = "lib/";
    private static final String WAR_FILE = "yamj3-core-3.0-SNAPSHOT.war";
    private static String yamjHome = ".";
    private static int yamjPort = 8888;
    private static int yamjShutdownPort = 3000;
    private static boolean yamjStopAtShutdown = Boolean.TRUE;

    public static void main(String[] args) {
        // Create the log file name here, so we can change it later (because it's locked
        System.setProperty("file.name", LOG_FILENAME);
        PropertyConfigurator.configure("config/log4j.properties");

        ClassTools.printHeader(Start.class, LOG);

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
            help(parser);
            status = CMDLINE_ERROR;
        }

        LOG.info("Exiting with status '{}', return code {}", status.toString(), status.getReturn());
        System.exit(status.getReturn());
    }

    private static ExitType startUp(CmdLineParser parser) {
        if (StringUtils.isNotBlank(parser.getParsedOptionValue("h"))) {
            yamjHome = parser.getParsedOptionValue("h");
        }
        yamjPort = convertToInt(parser.getParsedOptionValue("p"), yamjPort);
        yamjShutdownPort = convertToInt(parser.getParsedOptionValue("sp"), yamjShutdownPort);
        yamjStopAtShutdown = convertToBoolean(parser.getParsedOptionValue("ss"), yamjStopAtShutdown);

        String warFilename = FilenameUtils.concat(yamjHome, WAR_DIR + WAR_FILE);
        File warFile = new File(warFilename);

        if (warFile.exists()) {
            try {
                // This is a temporary fix until the yamj3.home can be read from the servlet
                System.setProperty("yamj3.home", (new File(yamjHome)).getCanonicalPath());
            } catch (IOException ex) {
                System.setProperty("yamj3.home", yamjHome);
            }
            LOG.info("YAMJ Home: '{}'", yamjHome);
            LOG.info("YAMJ Port: {}", yamjPort);
            LOG.info("YAMJ Shudown Port: {}", yamjShutdownPort);
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
        Server server = new Server(yamjPort);
        server.setGracefulShutdown(yamjShutdownPort);
        server.setStopAtShutdown(yamjStopAtShutdown);

        try {
            WebAppContext webapp = new WebAppContext();
            webapp.setContextPath("/");
            webapp.setWar(warFile.getCanonicalPath());
            server.setHandler(webapp);

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
            return STARTUP_FAILURE;
        } catch (InterruptedException ex) {
            LOG.error("Server interrupted, error: ", ex.getMessage());
            return STARTUP_FAILURE;
        } catch (Exception ex) {
            LOG.error("General server eror, message: ", ex.getMessage());
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
        parser.addOption(new CmdLineOption("sp", "shutdown port", "The port to shutdown the server, default: " + yamjShutdownPort, false, true));
        parser.addOption(new CmdLineOption("ss", "stop shutdown", "Shutdown the server when exiting, default: " + yamjStopAtShutdown, false, false));

        return parser;
    }

    private static int convertToInt(String toConvert, int defaultValue) {
        if (StringUtils.isNumeric(toConvert)) {
            return Integer.parseInt(toConvert);
        } else {
            return defaultValue;
        }
    }

    private static boolean convertToBoolean(String toConvert, boolean defaultValue) {
        if (StringUtils.isNotBlank(toConvert)) {
            return Boolean.parseBoolean(toConvert);
        } else {
            return defaultValue;
        }
    }
}
