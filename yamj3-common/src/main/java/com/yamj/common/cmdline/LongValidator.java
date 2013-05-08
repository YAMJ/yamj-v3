package com.yamj.common.cmdline;

/**
 * Validator for long values.
 */
public class LongValidator extends OptionValidator {

    /**
     * Validate a long option.
     *
     * @see OptionValidator#validate(CmdLineParser, ParsedOption)
     */
    @Override
    public void validate(final CmdLineParser cmdlineParser, final ParsedOption parsedOption) throws CmdLineException {
        try {
            Long.parseLong(parsedOption.getValue());
        } catch (final NullPointerException npe) {
            throw new CmdLineException("NULL value passed");
        } catch (final NumberFormatException nfe) {
            throw new CmdLineException("Parameter " + parsedOption.getValue() + " should be a long number");
        }
    }
}
