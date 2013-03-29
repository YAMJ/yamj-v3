package com.moviejukebox.common.cmdline;

/**
 * The default validator; doing nothing.
 */
public class OptionValidator {

    /**
     * Validate an option.
     *
     * @see OptionValidator#validate(CmdLineParser, ParsedOption)
     */
    public void validate(final CmdLineParser cmdlineparser, final ParsedOption parsedoption) throws CmdLineException {
        // do nothing
    }
}
