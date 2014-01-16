/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package org.yamj.batch;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.yamj.common.cmdline.CmdLineException;
import org.yamj.common.cmdline.CmdLineOption;
import org.yamj.common.cmdline.CmdLineParser;
import org.yamj.common.model.YamjInfo;

public class YAMJBatch {

    private static final Logger LOG = LoggerFactory.getLogger(YAMJBatch.class);

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        YamjInfo yi = new YamjInfo(YAMJBatch.class);
        yi.printHeader(LOG);

        CmdLineParser parser = getCmdLineParser();

        int status;
        try {
            parser.parse(args);

            YAMJBatch main = new YAMJBatch();
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
            ApplicationContext applicationContext = new ClassPathXmlApplicationContext("yamj3-batch.xml");
            BatchManagement batchManagement = (BatchManagement) applicationContext.getBean("batchManagement");
            status = batchManagement.runBatch(parser);
        } catch (Exception error) {
            LOG.error("Failed to load batch configuration");
            error.printStackTrace(System.err);
            status = 2;
        }
        return status;
    }
}
