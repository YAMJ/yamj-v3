package com.moviejukebox.filescanner;

import com.moviejukebox.common.cmdline.CmdLineParser;
import com.moviejukebox.common.type.ExitType;

public interface ScannerManagement {

    public ExitType runScanner(CmdLineParser parser);
}
