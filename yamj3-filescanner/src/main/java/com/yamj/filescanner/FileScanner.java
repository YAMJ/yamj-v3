package com.yamj.filescanner;

import com.yamj.common.cmdline.CmdLineException;
import com.yamj.common.cmdline.CmdLineOption;
import com.yamj.common.cmdline.CmdLineParser;
import com.yamj.common.type.ExitType;
import static com.yamj.common.type.ExitType.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public final class FileScanner {

    private static final Logger LOG = LoggerFactory.getLogger(FileScanner.class);
    private static final String LOG_FILENAME = "yamj-filescanner";

    public static void main(String[] args) {
        System.setProperty("file.name", LOG_FILENAME);
        PropertyConfigurator.configure("config/log4j.properties");

        printHeader();
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

    /**
     * Print the title
     */
    private static void printHeader() {
        String projectName = FileScanner.class.getPackage().getImplementationVendor();   // "YAMJ"
        String projectVersion = FileScanner.class.getPackage().getImplementationVersion();  // Project version
        String moduleName = FileScanner.class.getPackage().getImplementationTitle();    // Module Name
//        String moduleDesc = FileScanner.class.getPackage().getSpecificationTitle();     // Module description
        String buildTimestamp = FileScanner.class.getPackage().getSpecificationVendor();    // Build timestamp
        String buildNumber = FileScanner.class.getPackage().getSpecificationVersion();   // Build number

        LOG.info("{} {}", projectName, projectVersion);
        LOG.info(StringUtils.repeat("~", (projectName.length() + projectVersion.length() + 1)));
        LOG.info("{}", moduleName);
        LOG.info("");
        LOG.info("  Revision: {}", buildNumber);
        LOG.info("Build Time: {}", buildTimestamp);
        LOG.info("");
    }

    /**
     * Print the parse descriptions
     *
     * @param parser
     */
    private static void help(CmdLineParser parser) {
        LOG.info(parser.getDescriptions());
    }

    private static CmdLineParser getCmdLineParser() {
        CmdLineParser parser = new CmdLineParser();
        parser.addOption(new CmdLineOption("d", "direcctory", "The directory to process", true, true));
        parser.addOption(new CmdLineOption("w", "watcher", "Keep watching the directories for changes", false, true));
        parser.addOption(new CmdLineOption("l", "library", "The library file to read", false, true));
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