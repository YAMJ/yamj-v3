package com.moviejukebox.core.remote.service;

import org.springframework.stereotype.Service;

import com.moviejukebox.common.remote.service.PingService;

@Service("pingService")
public class PingServiceImpl implements PingService {

    @Override
    public String ping() {
        return "YAMJ3 Core is running";
    }
}
