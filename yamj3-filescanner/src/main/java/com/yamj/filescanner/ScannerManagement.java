package com.yamj.filescanner;

import com.yamj.common.cmdline.CmdLineParser;
import com.yamj.common.type.ExitType;

public interface ScannerManagement {

    public ExitType runScanner(CmdLineParser parser);
}
