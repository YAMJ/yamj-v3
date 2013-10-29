/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.common.cmdline;

import java.util.LinkedList;

/**
 * A new command line option to define the option.
 */
public final class CmdLineOption {

    /**
     * Holds the option name
     */
    private final String optionName;
    /**
     * Holds the parameter name for the option
     */
    private final String optionParameterName;
    /**
     * Holds the description
     */
    private final String description;
    /**
     * Flag to indicate if option is required
     */
    private final boolean isRequired;
    /**
     * Flag to indicate if option needs a value
     */
    private final boolean needsValue;
    /**
     * List of required options
     */
    private LinkedList<CmdLineOption> requiredOptions = null;
    /**
     * List of excluded options
     */
    private LinkedList<CmdLineOption> excludedOptions = null;
    /**
     * The validator to use for this option
     */
    private OptionValidator validator = null;

    /**
     * Create a new command line option
     *
     * @param optionName the option name
     * @param description the option description
     * @param isRequired flag to indicate if option is required
     */
    public CmdLineOption(final String optionName, final String description, final boolean isRequired) {
        this(optionName, null, description, isRequired, false);
    }

    /**
     * Create a new command line option
     *
     * @param optionName the option name
     * @param optionParameterName the parameter name of the option
     * @param description the option description
     * @param isRequired flag to indicate if option is required
     * @param needsValue flag to indicate if option needs a value
     */
    public CmdLineOption(final String optionName, final String optionParameterName, final String description,
            final boolean isRequired, final boolean needsValue) {
        this.optionName = optionName;
        this.optionParameterName = optionParameterName;
        this.description = description;
        this.isRequired = isRequired;
        this.needsValue = needsValue;
        this.validator = new OptionValidator();
    }

    /**
     * Get the option name.
     *
     * @return the option name
     */
    public String getName() {
        return this.optionName;
    }

    /**
     * Get the option description.
     *
     * @return the option description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Get the flag if option is required.
     *
     * @return the required flag
     */
    public boolean isRequired() {
        return this.isRequired;
    }

    /**
     * Get the flag if value is needed.
     *
     * @return the needsValue flag
     */
    public boolean needsValue() {
        return this.needsValue;
    }

    /**
     * Set the option validator to use for validating the option.
     *
     * @param validator the option validator
     */
    public void setValidator(final OptionValidator validator) {
        this.validator = validator;
    }

    /**
     * Get the option validator.
     *
     * @return the option validator
     */
    public OptionValidator getValidator() {
        return this.validator;
    }

    /**
     * Test is object equals another object.
     *
     * @param object the object to test for equality
     * @return true if objects are equal, else false
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && (object instanceof CmdLineOption)) {
            return this.getName().equals(((CmdLineOption) object).getName());
        }
        return false;
    }

    /**
     * Ensure hashcode is created for the object.
     *
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + (this.optionName != null ? this.optionName.hashCode() : 0);
        hash = 23 * hash + (this.optionParameterName != null ? this.optionParameterName.hashCode() : 0);
        hash = 23 * hash + (this.description != null ? this.description.hashCode() : 0);
        hash = 23 * hash + (this.isRequired ? 1 : 0);
        hash = 23 * hash + (this.needsValue ? 1 : 0);
        return hash;
    }

    /**
     * Add an excluded option.
     *
     * @param excludedOption the excluded option
     */
    public void addExcludedOption(final CmdLineOption excludedOption) {
        if (this.excludedOptions == null) {
            this.excludedOptions = new LinkedList<CmdLineOption>();
        }
        this.excludedOptions.add(excludedOption);
    }

    /**
     * Add excluded options.
     *
     * @param excludedOptions the excluded options
     */
    public void addExcludedOptions(final CmdLineOption excludedOptions[]) {
        for (int i = 0; excludedOptions != null && i < excludedOptions.length; i++) {
            this.addExcludedOption(excludedOptions[i]);
        }
    }

    /**
     * Get the excluded options.
     *
     * @return the excluded options
     */
    public LinkedList<CmdLineOption> getExcludedOptions() {
        return this.excludedOptions;
    }

    /**
     * Check if excluded options exist.
     *
     * @return true if excluded options exist
     */
    public boolean hasExcludedOptions() {
        try {
            return this.excludedOptions.size() != 0;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    /**
     * Test if command line option is excluded.
     *
     * @param cmdLineOption the command line option to check
     * @return true if option is excluded, else false
     */
    public boolean excludesOption(final CmdLineOption cmdLineOption) {
        if (this.excludedOptions == null) {
            return false;
        }

        boolean result = false;
        for (int i = 0; i < this.excludedOptions.size(); i++) {
            CmdLineOption o = this.excludedOptions.get(i);
            if (o.equals(cmdLineOption)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Test if parsed option is excluded.
     *
     * @param parsedOption the parse option
     * @return true if parsed option is excluded, else false
     */
    public boolean isExcluded(final ParsedOption parsedOption) {
        if (this.excludedOptions != null) {
            CmdLineOption option = parsedOption.getParent();
            for (CmdLineOption excluded : this.excludedOptions) {
                if (option.equals(excluded)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add a required option.
     *
     * @param requiredOption the required option
     */
    public void addRequiredOption(final CmdLineOption requiredOption) {
        if (this.requiredOptions == null) {
            this.requiredOptions = new LinkedList<CmdLineOption>();
        }
        this.requiredOptions.add(requiredOption);
    }

    /**
     * Add a required options.
     *
     * @param requiredOptions the required option
     */
    public void addRequiredOptions(final CmdLineOption requiredOptions[]) {
        for (int i = 0; requiredOptions != null && i < requiredOptions.length; i++) {
            this.addRequiredOption(requiredOptions[i]);
        }
    }

    /**
     * Get the required options.
     *
     * @return the required options
     */
    public LinkedList<CmdLineOption> getRequiredOptions() {
        return this.requiredOptions;
    }

    /**
     * Check if required options exist.
     *
     * @return true if required options exist
     */
    public boolean hasRequiredOptions() {
        try {
            return this.requiredOptions.size() != 0;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    /**
     * Test if command line option is required.
     *
     * @param cmdLineOption the command line option to check
     * @return true if option is required, else false
     */
    public boolean requiresOption(final CmdLineOption cmdLineOption) {
        if (this.requiredOptions == null) {
            return false;
        }

        boolean result = false;
        for (int i = 0; i < this.requiredOptions.size(); i++) {
            CmdLineOption o = this.requiredOptions.get(i);
            if (o.equals(cmdLineOption)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Get the string representation.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("-");
        sb.append(this.optionName);
        sb.append(" ");
        if (this.needsValue) {
            sb.append("<");
            sb.append(this.optionParameterName);
            sb.append("> ");
        } else if (this.optionParameterName != null) {
            sb.append("[");
            sb.append(this.optionParameterName);
            sb.append("]");
        }
        return sb.toString();
    }

    /**
     * Get the string representation.
     *
     * @param caller the command line parser calling this option
     * @return the string representation
     */
    public String toString(final CmdLineParser caller) {
        StringBuffer sb = new StringBuffer();
        sb.append(this.isRequired ? "<" : "[");
        sb.append("-");
        sb.append(this.optionName);
        sb.append(" ");
        if (this.needsValue) {
            if (!(this.getValidator() instanceof EnumValidator)) {
                sb.append("<");
                sb.append(this.optionParameterName);
                sb.append("> ");
            } else {
                EnumValidator val = (EnumValidator) this.getValidator();
                sb.append("<");
                sb.append(val.getAllowedValuesList());
                sb.append("> ");
            }
        } else if (this.optionParameterName != null) {
            sb.append("[");
            sb.append(this.optionParameterName);
            sb.append("]");
        }
        if (this.hasRequiredOptions()) {
            for (int i = 0; i < this.requiredOptions.size(); i++) {
                sb.append(this.requiredOptions.get(i));
            }
        }
        sb.append(this.isRequired ? ">" : "]");
        return sb.toString();
    }
}
