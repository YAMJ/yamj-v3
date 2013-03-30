package com.moviejukebox.filescanner;

import com.moviejukebox.common.cmdline.CmdLineException;
import com.moviejukebox.common.cmdline.CmdLineOption;
import com.moviejukebox.common.cmdline.CmdLineParser;
import static com.moviejukebox.common.type.ExitType.*;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class FileScanner {

    private static final Logger LOG = LoggerFactory.getLogger(FileScanner.class);
    private static final String LOG_MESSAGE = "FileScanner: ";
    private static final String logFilename = "yamj-filescanner";

    public static void main(String[] args) throws Exception {
        System.setProperty("file.name", logFilename);
        PropertyConfigurator.configure("config/log4j.properties");

        CmdLineParser parser = getCmdLineParser();

        int status;
        try {
            parser.parse(args);

            if (parser.userWantsHelp()) {
                help(parser);
                status = SUCCESS.getReturn();
            } else {
                FileScanner main = new FileScanner();
                status = main.execute(parser);
            }
        } catch (CmdLineException ex) {
            LOG.error("{}Failed to parse command line options: {}", LOG_MESSAGE, ex.getMessage());
            help(parser);
            status = CMDLINE_ERROR.getReturn();
        }
        System.exit(status);
    }

    private static void help(CmdLineParser parser) {
        LOG.error("YAMJ v3 File Scanner");
        LOG.error("~~~~ ~~ ~~~~ ~~~~~~~");
        LOG.error("Scans the specified directory for media files.");
        LOG.error(parser.getDescriptions());
    }

    private static CmdLineParser getCmdLineParser() {
        CmdLineParser parser = new CmdLineParser();
        parser.addOption(new CmdLineOption("d", "direcctory", "The directory to process", true, true));
        parser.addOption(new CmdLineOption("w", "watcher", "Keep watching the directories for changes", false, true));
        // Nothing is done with this at the moment
        parser.addOption(new CmdLineOption("h", "host", "The IP Address of the core server", false, true));
        parser.addOption(new CmdLineOption("p", "port", "The port for the core server", false, true));
        return parser;
    }

    @SuppressWarnings("resource")
    private int execute(CmdLineParser parser) {
        int status;
        try {
            ApplicationContext applicationContext = new ClassPathXmlApplicationContext("yamj3-filescanner.xml");
            ScannerManagement batchManagement = (ScannerManagement) applicationContext.getBean("scannerManagement");
            status = batchManagement.runScanner(parser);
        } catch (BeansException ex) {
            LOG.error("{}Failed to load scanner configuration", LOG_MESSAGE);
            ex.printStackTrace(System.err);
            status = CONFIG_ERROR.getReturn();

        }
        return status;
    }
}
