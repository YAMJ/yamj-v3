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

import javax.annotation.Resource;
import org.yamj.common.cmdline.CmdLineParser;
import org.yamj.common.dto.ImportDTO;
import org.yamj.common.dto.StageDirectoryDTO;
import org.yamj.common.dto.StageFileDTO;
import org.yamj.common.remote.service.FileImportService;
import org.yamj.common.remote.service.SystemInfoService;

public class BatchManagementImpl implements BatchManagement {

    @Resource(name ="systemInfoService")
    private SystemInfoService systemInfoService;

    @Resource(name ="fileImportService")
    private FileImportService fileImportService;

    @Override
    public int runBatch(CmdLineParser parser) {
        // get the batch to call on core server
        String batchName = parser.getParsedOptionValue("b");

        int status = 0;
        try {

            if ("ping".equalsIgnoreCase(batchName)) {
                //
                System.out.println(systemInfoService.ping());

            // JUST FOR TESTING
            } else if ("filetest".equalsIgnoreCase(batchName)) {
                // just a test for file staging
                ImportDTO importDTO = new ImportDTO();
                importDTO.setClient("007");
                importDTO.setPlayerPath("smb://127.0.0.1/test");
                importDTO.setBaseDirectory("C:\\movies\\");

                StageDirectoryDTO stageDirectory = new StageDirectoryDTO();
                stageDirectory.setDate(System.currentTimeMillis());
                stageDirectory.setPath("C:\\movies\\");
                importDTO.setStageDirectory(stageDirectory);

                // import scanned
                fileImportService.importScanned(importDTO);

                stageDirectory = new StageDirectoryDTO();
                stageDirectory.setDate(System.currentTimeMillis());
                stageDirectory.setPath("C:\\movies\\Action");
                importDTO.setStageDirectory(stageDirectory);

                StageFileDTO stageFile = new StageFileDTO();
                stageFile.setFileName("Avatar (2009).bdrip.mkv");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(12344165L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("Avatar (2009) (Extended).sdtv.mkv");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(2344165L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("Avatar (2009).bdrip.nfo");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(10L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("Avatar (2009).bdrip.jpg");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(1451257L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("Avatar (2009).bdrip.FANART.jpg");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(43252L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("Game of Thrones.S03E01.avi");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(1234L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("Game of Thrones.S03E02.avi");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(1235L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("Game of Thrones.S03E02..avi");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(1235L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("Game of Thrones.S03E03E04.avi");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(5000L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("2012 - 1080p BluRay x264.mkv");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(5000L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("The Possession (2012).mkv");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(5000L);
                stageDirectory.addStageFile(stageFile);

                // import scanned
                fileImportService.importScanned(importDTO);

            } else {
                System.err.println("Invalid batch: " + batchName);
                status = 3;
            }

        } catch (Exception error) {
            System.err.println("Failed execution of batch: " + batchName);
            error.printStackTrace(System.err);
            status = 4;
        }
        return status;
    }
}
