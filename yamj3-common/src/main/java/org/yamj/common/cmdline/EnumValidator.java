package org.yamj.common.cmdline;

/**
 * Validator which uses allowed values.
 */
public class EnumValidator extends OptionValidator {

    /**
     * Array with allowed values
     */
    private final String allowedValues[];

    /**
     * Creates a new enumeration validator.
     *
     * @param allowedValues array of allowed values
     */
    public EnumValidator(final String allowedValues[]) {
        this.allowedValues = allowedValues.clone();
    }

    /**
     * Validate an option.
     *
     * @see OptionValidator#validate(CmdLineParser, ParsedOption)
     */
    @Override
    public void validate(final CmdLineParser cmdlineParser, final ParsedOption parsedOption) throws CmdLineException {
        try {
            boolean result = false;
            for (int i = 0; this.allowedValues != null && i < this.allowedValues.length; i++) {
                if (parsedOption.getValue().equals(this.allowedValues[i])) {
                    result = true;
                    break;
                }
            }

            if (!result) {
                throw new CmdLineException("Parameter " + parsedOption.getValue() + " is not valid for option '"
                        + parsedOption.getParent().toString(cmdlineParser) + "'");
            }
        } catch (final NullPointerException npe) {
            throw new CmdLineException("NULL value passed");
        }
    }

    /**
     * Get the list of allowed values as string.
     *
     * @return string representation of allowed values
     */
    public String getAllowedValuesList() {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; this.allowedValues != null && i < this.allowedValues.length; i++) {
            sb.append(this.allowedValues[i]);
            if (i + 1 < this.allowedValues.length) {
                sb.append("|");
            }
        }
        return sb.toString();
    }
}
