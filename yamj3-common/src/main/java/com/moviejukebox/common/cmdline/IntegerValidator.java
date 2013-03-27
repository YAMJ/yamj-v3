package com.moviejukebox.common.cmdline;

/**
 * Validator for integer values.
 */
public class IntegerValidator extends OptionValidator {

	/**
	 * Validate an integer option.
	 *
	 * @see OptionValidator#validate(CmdLineParser, ParsedOption)
	 */
    @Override
	public void validate(final CmdLineParser cmdlineParser, final ParsedOption parsedOption) throws CmdLineException {
		try {
			Integer.parseInt(parsedOption.getValue());
		}
		catch (final NullPointerException npe) {
			throw new CmdLineException("NULL value passed");
		}
		catch (final NumberFormatException nfe) {
			throw new CmdLineException("Parameter " + parsedOption.getValue() + " should be an integer number");
		}
	}
}
