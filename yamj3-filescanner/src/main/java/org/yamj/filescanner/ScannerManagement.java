package org.yamj.filescanner;

import org.yamj.common.cmdline.CmdLineParser;
import org.yamj.common.type.ExitType;

public interface ScannerManagement {

    ExitType runScanner(CmdLineParser parser);
}
