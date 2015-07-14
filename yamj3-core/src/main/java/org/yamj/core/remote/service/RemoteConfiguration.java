package org.yamj.core.remote.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.yamj.common.remote.service.FileImportService;
import org.yamj.common.remote.service.GitHubService;
import org.yamj.common.remote.service.SystemInfoService;

@Configuration
public class RemoteConfiguration  {

    @Autowired
    private SystemInfoService systemInfoService;
    @Autowired
    private FileImportService fileImportService;
    @Autowired
    private GitHubService gitHubService;
    
    @Bean(name="/SystemInfoService")
    public HttpInvokerServiceExporter systemInfoServiceExporter() {
        HttpInvokerServiceExporter httpInvokerServiceExporter = new HttpInvokerServiceExporter();
        httpInvokerServiceExporter.setService(systemInfoService);
        httpInvokerServiceExporter.setServiceInterface(SystemInfoService.class);
        return httpInvokerServiceExporter;
    }

    @Bean(name="/FileImportService")
    public HttpInvokerServiceExporter fileImportServiceExporter() {
        HttpInvokerServiceExporter httpInvokerServiceExporter = new HttpInvokerServiceExporter();
        httpInvokerServiceExporter.setService(fileImportService);
        httpInvokerServiceExporter.setServiceInterface(FileImportService.class);
        return httpInvokerServiceExporter;
    }
    
    @Bean(name="/GitHubService")
    public HttpInvokerServiceExporter gitHubServiceExporter() {
        HttpInvokerServiceExporter httpInvokerServiceExporter = new HttpInvokerServiceExporter();
        httpInvokerServiceExporter.setService(gitHubService);
        httpInvokerServiceExporter.setServiceInterface(GitHubService.class);
        return httpInvokerServiceExporter;
    }
}
