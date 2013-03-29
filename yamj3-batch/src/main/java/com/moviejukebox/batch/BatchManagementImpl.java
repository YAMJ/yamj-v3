package com.moviejukebox.batch;

import com.moviejukebox.common.dto.StageFileDTO;

import com.moviejukebox.common.cmdline.CmdLineParser;
import com.moviejukebox.common.dto.FileImportDTO;
import com.moviejukebox.common.dto.LibraryDTO;
import com.moviejukebox.common.dto.StageDirectoryDTO;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.common.remote.service.PingService;
import javax.annotation.Resource;

public class BatchManagementImpl implements BatchManagement {

    @Resource(name ="pingService")
    private PingService pingService;

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
                System.out.println(pingService.ping());

            // JUST FOR TESTING
            } else if ("filetest".equalsIgnoreCase(batchName)) {
                // just a test for file staging
                FileImportDTO dto = new FileImportDTO();
                dto.setScanPath("smb://127.0.0.1/test");
                dto.setFilePath("/test/movies/Avatar 2009.mkv");
                dto.setFileDate(System.currentTimeMillis());
                dto.setFileSize(2000000l);
                fileImportService.importFile(dto);
            } else if ("library".equalsIgnoreCase(batchName)) {
                // just a test for file staging
                LibraryDTO library = new LibraryDTO();
                library.setClient("007");
                library.setPlayerPath("smb://127.0.0.1/test");
                library.setBaseDirectory("D:/test");

                StageDirectoryDTO stageDirectory = new StageDirectoryDTO();
                stageDirectory.setDate(System.currentTimeMillis());
                stageDirectory.setPath("D:/test/movies");
                library.addStageDirectory(stageDirectory);
                
                StageFileDTO stageFile = new StageFileDTO();
                stageFile.setFileName("Avatar (2009).mkv");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(12344165L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("Avatar (2009).nfo");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(10L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("Avatar (2009).jpg");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(1451257L);
                stageDirectory.addStageFile(stageFile);

                stageFile = new StageFileDTO();
                stageFile.setFileName("Avatar (2009).FANART.jpg");
                stageFile.setFileDate(System.currentTimeMillis());
                stageFile.setFileSize(43252L);
                stageDirectory.addStageFile(stageFile);

                fileImportService.importLibrary(library);
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
