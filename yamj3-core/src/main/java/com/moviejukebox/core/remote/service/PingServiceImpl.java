package com.moviejukebox.core.remote.service;

import com.moviejukebox.common.remote.service.PingService;
import org.springframework.stereotype.Service;

@Service("pingService")
public class PingServiceImpl implements PingService {

    @Override
    public String ping() {
        return "YAMJ3 Core is running";
    }
}
