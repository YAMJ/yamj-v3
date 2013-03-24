package com.moviejukebox.batch;

import com.moviejukebox.batch.cmdline.CmdLineParser;
import com.moviejukebox.common.dto.FileImportDTO;
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
            } if ("filetest".equalsIgnoreCase(batchName)) {
                // just a test for file staging
                FileImportDTO dto = new FileImportDTO();
                dto.setScanPath("smb://127.0.0.1/test");
                dto.setFilePath("/test/movies/Avatar 2009.mkv");
                dto.setFileDate(System.currentTimeMillis());
                dto.setFileSize(2000000l);
                fileImportService.importFile(dto);
                
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
