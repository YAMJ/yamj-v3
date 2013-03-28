package com.moviejukebox.filescanner;

import com.moviejukebox.common.cmdline.CmdLineException;
import com.moviejukebox.common.cmdline.CmdLineOption;
import com.moviejukebox.common.cmdline.CmdLineParser;
import java.io.IOException;
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
    // return status codes
    private static final int EXIT_CMDLINE_ERROR = 1;
    private static final int EXIT_CONFIG_ERROR = 2;

    public static void main(String[] args) throws Exception {
        System.setProperty("file.name", logFilename);
        PropertyConfigurator.configure("config/log4j.properties");

        CmdLineParser parser = getCmdLineParser();

        int status;
        try {
            parser.parse(args);

            FileScanner main = new FileScanner();
            status = main.execute(parser);
        } catch (CmdLineException cle) {
            LOG.error("{}Failed to parse command line options: {}", LOG_MESSAGE, cle.getMessage());
            status = EXIT_CMDLINE_ERROR;
        }
        System.exit(status);
    }

    private static CmdLineParser getCmdLineParser() {
        CmdLineParser parser = new CmdLineParser();
        parser.addOption(new CmdLineOption("d", "direcctory", "The directory to process", true, true));
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
            status = EXIT_CONFIG_ERROR;
        }
        return status;
    }
}
