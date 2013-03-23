package com.moviejukebox.batch;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.moviejukebox.batch.cmdline.CmdLineException;
import com.moviejukebox.batch.cmdline.CmdLineOption;
import com.moviejukebox.batch.cmdline.CmdLineParser;

public class MovieJukeboxBatch {
    
    public static void main(String[] args) throws Exception {
        CmdLineParser parser = getCmdLineParser();
        
        int status;
        try {
            parser.parse(args);
            
            MovieJukeboxBatch main = new MovieJukeboxBatch();
            status = main.execute(parser);
        } catch (CmdLineException cle) {
            System.err.println(cle.getMessage());
            status = 1;
        }
        System.exit(status);
    }
    
    private static CmdLineParser getCmdLineParser() {
        CmdLineParser parser = new CmdLineParser();
        parser.addOption(new CmdLineOption("b", "batch", "The batch parameter", true, true));
        return parser;
    }

    @SuppressWarnings("resource")
    private int execute(CmdLineParser parser) {
        int status;
        try {
            ApplicationContext  applicationContext = new ClassPathXmlApplicationContext("yamj3-batch.xml");
            BatchManagement batchManagement = (BatchManagement) applicationContext.getBean("batchManagement");
            status = batchManagement.runBatch(parser);
        } catch (Exception error) {
            System.err.println("Failed to load batch configuration");
            error.printStackTrace(System.err);
            status = 2;
        }
        return status;
    }
}
