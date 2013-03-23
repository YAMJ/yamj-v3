package com.moviejukebox.batch;

import javax.annotation.Resource;

import com.moviejukebox.batch.cmdline.CmdLineParser;
import com.moviejukebox.common.remote.service.PingService;

public class BatchManagementImpl implements BatchManagement {

    @Resource(name ="pingService")
    private PingService pingService;
    
    @Override
    public int runBatch(CmdLineParser parser) {
        // get the batch to call on core server
        String batchName = parser.getParsedOptionValue("b");
        
        int status = 0;
        try {
        
            if ("ping".equalsIgnoreCase(batchName)) {
                // 
                System.out.println(pingService.ping());
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
