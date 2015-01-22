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

import static org.yamj.common.type.ExitType.CMDLINE_ERROR;
import static org.yamj.common.type.ExitType.CONFIG_ERROR;
import static org.yamj.common.type.ExitType.SUCCESS;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.yamj.common.cmdline.CmdLineException;
import org.yamj.common.cmdline.CmdLineOption;
import org.yamj.common.cmdline.CmdLineParser;
import org.yamj.common.model.YamjInfo;
import org.yamj.common.tools.ClassTools;
import org.yamj.common.type.ExitType;

public final class FileScanner {

    private static final Logger LOG = LoggerFactory.getLogger(FileScanner.class);
    private static final String YAMJ3_HOME = "yamj3.home";

    public static void main(String[] args) throws IOException {
        PropertyConfigurator.configure("config/log4j-filescanner.properties");

        // Get the current directory
        String yamjHome = ClassTools.checkSystemProperty(YAMJ3_HOME, (new File(".")).getCanonicalPath());

        try {
            // This is a temporary fix until the yamj3.home can be read from the servlet
            ClassTools.checkSystemProperty(YAMJ3_HOME, (new File(yamjHome)).getCanonicalPath());
        } catch (IOException ex) {
            ClassTools.checkSystemProperty(YAMJ3_HOME, yamjHome);
            LOG.trace("Exception:", ex);
        }

        YamjInfo yi = new YamjInfo(FileScanner.class);
        yi.printHeader(LOG);
        CmdLineParser parser = getCmdLineParser();

        ExitType status;
        try {
            parser.parse(args);

            if (parser.userWantsHelp()) {
                help(parser);
                status = SUCCESS;
            } else {
                status = execute(parser);
            }
        } catch (CmdLineException ex) {
            LOG.error("Failed to parse command line options: {}", ex.getMessage());
            LOG.trace("Exception:", ex);
            help(parser);
            status = CMDLINE_ERROR;
        }
        System.exit(status.getReturn());    //NOSONAR
    }

    /**
     * Print the parse descriptions
     *
     * @param parser
     */
    private static void help(CmdLineParser parser) {
        // Print the command line options
        LOG.info(parser.getDescriptions());

        // Print the exit status
        LOG.info(ExitType.getDescriptions());
    }

    private static CmdLineParser getCmdLineParser() {
        CmdLineParser parser = new CmdLineParser();
        parser.addOption(new CmdLineOption("d", "direcctory", "The directory to process", false, true));
        parser.addOption(new CmdLineOption("w", "watcher", "Keep watching the directories for changes", false, true));
        parser.addOption(new CmdLineOption("l", "library", "The library file to read", false, true));
        return parser;
    }

    private static ExitType execute(CmdLineParser parser) {
        ExitType status;

        try (ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("yamj3-filescanner.xml")) {
            ScannerManagement scannerManagement = (ScannerManagement) applicationContext.getBean("scannerManagement");
            status = scannerManagement.runScanner(parser);
        } catch (BeansException ex) {
            LOG.error("Failed to load scanner configuration", ex);
            status = CONFIG_ERROR;
        }
        return status;
    }
}
