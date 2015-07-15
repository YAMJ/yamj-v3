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
package org.yamj.core.remote.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.yamj.common.remote.service.FileImportService;
import org.yamj.common.remote.service.GitHubService;
import org.yamj.common.remote.service.SystemInfoService;

@Configuration
public class RemoteServiceConfiguration  {

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
