package com.yamj.common.cmdline;

/**
 * Validator for float values.
 */
public class FloatValidator extends OptionValidator {

    /**
     * Validate a float option.
     *
     * @see OptionValidator#validate(CmdLineParser, ParsedOption)
     */
    @Override
    public void validate(final CmdLineParser cmdlineParser, final ParsedOption parsedOption) throws CmdLineException {
        try {
            Float.parseFloat(parsedOption.getValue());
        } catch (final NullPointerException npe) {
            throw new CmdLineException("NULL value passed");
        } catch (final NumberFormatException nfe) {
            throw new CmdLineException("Parameter " + parsedOption.getValue() + " should be a float number");
        }
    }
}
