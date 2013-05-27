package com.yamj.common.cmdline;

/**
 * Class for parsed option which holds the value read from the command line.
 */
public final class ParsedOption {

    /**
     * The command line option this option belongs to
     */
    private final CmdLineOption parent;
    /**
     * The parsed value
     */
    private final String value;
    /**
     * Flag to indicate if option has a value
     */
    private final boolean hasValue;

    /**
     * Create a new parsed option.
     *
     * @param parent the command line option
     * @param value the parsed valued
     * @param hasValue indicator if parsed option has a value
     */
    public ParsedOption(final CmdLineOption parent, final String value, final boolean hasValue) {
        this.parent = parent;
        this.value = value;
        this.hasValue = hasValue;
    }

    /**
     * Get the string representation of this object.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("value=");
        sb.append(this.value);
        sb.append(", hasValue=");
        sb.append(this.hasValue);
        sb.append(", parent=");
        sb.append(this.parent.getName());
        return sb.toString();
    }

    /**
     * Get the command line option of this parsed option.
     *
     * @return the command line option
     */
    public CmdLineOption getParent() {
        return this.parent;
    }

    /**
     * Get the parsed value.
     *
     * @return the parsed value
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Indicates if parsed option has a value.
     *
     * @return the boolean flag
     */
    public boolean hasValue() {
        return this.hasValue;
    }
}
