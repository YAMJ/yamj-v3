package com.moviejukebox.filescanner;

import com.moviejukebox.common.cmdline.CmdLineException;
import com.moviejukebox.common.cmdline.CmdLineOption;
import com.moviejukebox.common.cmdline.CmdLineParser;
import org.apache.log4j.BasicConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class FileScanner {

    private static final int EXIT_CMDLINE_ERROR = 1;
    private static final int EXIT_CONFIG_ERROR = 2;

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        
        CmdLineParser parser = getCmdLineParser();

        int status;
        try {
            parser.parse(args);

            FileScanner main = new FileScanner();
            status = main.execute(parser);
        } catch (CmdLineException cle) {
            System.err.println(cle.getMessage());
            status = EXIT_CMDLINE_ERROR;
        }
        System.exit(status);
    }

    private static CmdLineParser getCmdLineParser() {
        CmdLineParser parser = new CmdLineParser();
        parser.addOption(new CmdLineOption("d", "direcctory", "The directory to process", true, true));
        return parser;
    }

    @SuppressWarnings("resource")
    private int execute(CmdLineParser parser) {
        int status;
        try {
            ApplicationContext applicationContext = new ClassPathXmlApplicationContext("yamj3-filescanner.xml");
            ScannerManagement batchManagement = (ScannerManagement) applicationContext.getBean("scannerManagement");
            status = batchManagement.runScanner(parser);
        } catch (Exception error) {
            System.err.println("Failed to load scanner configuration");
            error.printStackTrace(System.err);
            status = EXIT_CONFIG_ERROR;
        }
        return status;
    }
}
