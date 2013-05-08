package com.yamj.core.remote.service;

import com.yamj.common.remote.service.PingService;
import org.springframework.stereotype.Service;

@Service("pingService")
public class PingServiceImpl implements PingService {

    @Override
    public String ping() {
        return "YAMJ3 Core is running";
    }
}
