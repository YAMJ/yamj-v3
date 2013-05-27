package com.yamj.filescanner;

import com.yamj.common.cmdline.CmdLineException;
import com.yamj.common.cmdline.CmdLineOption;
import com.yamj.common.cmdline.CmdLineParser;
import com.yamj.common.type.ExitType;
import static com.yamj.common.type.ExitType.*;
import javax.naming.OperationNotSupportedException;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public final class FileScanner {

    private static final Logger LOG = LoggerFactory.getLogger(FileScanner.class);
    private static final String LOG_FILENAME = "yamj-filescanner";

    private FileScanner() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void main(String[] args) {
        System.setProperty("file.name", LOG_FILENAME);
        PropertyConfigurator.configure("config/log4j.properties");

        CmdLineParser parser = getCmdLineParser();

        ExitType status;
        try {
            parser.parse(args);

            if (parser.userWantsHelp()) {
                help(parser);
                status = SUCCESS;
            } else {
                FileScanner main = new FileScanner();
                status = main.execute(parser);
            }
        } catch (CmdLineException ex) {
            LOG.error("Failed to parse command line options: {}", ex.getMessage());
            help(parser);
            status = CMDLINE_ERROR;
        }
        System.exit(status.getReturn());
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
        parser.addOption(new CmdLineOption("l", "library", "The library file to read", false, true));
        // Nothing is done with these at the moment
        parser.addOption(new CmdLineOption("h", "host", "The IP Address of the core server", false, true));
        parser.addOption(new CmdLineOption("p", "port", "The port for the core server", false, true));
        return parser;
    }

    private ExitType execute(CmdLineParser parser) {
        ExitType status;
        try {
            ApplicationContext applicationContext = new ClassPathXmlApplicationContext("yamj3-filescanner.xml");
            ScannerManagement scannerManagement = (ScannerManagement) applicationContext.getBean("scannerManagement");
            status = scannerManagement.runScanner(parser);
        } catch (BeansException ex) {
            LOG.error("Failed to load scanner configuration");
            ex.printStackTrace(System.err);
            status = CONFIG_ERROR;
        }
        return status;
    }
}
