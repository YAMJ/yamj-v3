package org.yamj.batch;

import org.yamj.common.cmdline.CmdLineParser;
import org.yamj.common.dto.ImportDTO;
import org.yamj.common.dto.StageDirectoryDTO;
import org.yamj.common.dto.StageFileDTO;
import org.yamj.common.remote.service.FileImportService;
import javax.annotation.Resource;
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
                importDTO.setBaseDirectory("D:\\test\\");

                StageDirectoryDTO stageDirectory = new StageDirectoryDTO();
                stageDirectory.setDate(System.currentTimeMillis());
                stageDirectory.setPath("D:\\test\\movies\\");
                importDTO.setStageDirectory(stageDirectory);

                // import scanned
                fileImportService.importScanned(importDTO);

                stageDirectory = new StageDirectoryDTO();
                stageDirectory.setDate(System.currentTimeMillis());
                stageDirectory.setPath("D:\\test\\movies\\Action");
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
