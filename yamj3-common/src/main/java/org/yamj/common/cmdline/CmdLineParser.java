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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * The command line parser.
 */
public final class CmdLineParser {

    /**
     * The help option
     */
    public static final CmdLineOption HELP_OPTION = new CmdLineOption("help", "Print usage information", false);
    /**
     * Holds the command line options
     */
    private final LinkedList<CmdLineOption> options = new LinkedList<CmdLineOption>();
    /**
     * Holds the parsed options
     */
    private final HashMap<String, ParsedOption> parsedOptions = new HashMap<String, ParsedOption>();

    /**
     * Create a new command line parser.
     */
    public CmdLineParser() {
        this.addOption(HELP_OPTION);
    }

    /**
     * Add a command line option to parser.
     *
     * @param option the command line option to add
     */
    public void addOption(final CmdLineOption option) {
        this.options.add(option);
    }

    /**
     * Add command line options to parser.
     *
     * @param options the command line options to add
     */
    public void addOptions(final CmdLineOption options[]) {
        this.options.addAll(Arrays.asList(options));
    }

    /**
     * Get the parsed option.
     *
     * @param optionName the option name
     * @return the parsed option
     */
    public ParsedOption getParsedOption(final String optionName) {
        return this.parsedOptions.get(optionName);
    }

    /**
     * Get the parsed option.
     *
     * @param option the option
     * @return the parsed option
     */
    public ParsedOption getParsedOption(final CmdLineOption option) {
        return this.getParsedOption(option.getName());
    }

    /**
     * Get the parsed option value.
     *
     * @param optionName the option name
     * @return the parsed option value
     */
    public String getParsedOptionValue(final String optionName) {
        ParsedOption option = this.getParsedOption(optionName);
        return option == null ? null : option.getValue();
    }

    /**
     * Get the parsed option value.
     *
     * @param option the option
     * @return the parsed option value
     */
    public String getParsedOptionValue(final CmdLineOption option) {
        return this.getParsedOptionValue(option.getName());
    }

    /**
     * Check if an option is contained in the parsed options.
     *
     * @param optionName the option name
     * @return true if option is contained in parsed options
     */
    public boolean containsOption(final String optionName) {
        return this.getParsedOption(optionName) != null;
    }

    /**
     * Check if an option is contained in the parsed options.
     *
     * @param option the option
     * @return true if option is contained in parsed options
     */
    public boolean containsOption(final CmdLineOption option) {
        return this.containsOption(option.getName());
    }

    /**
     * Check if user wants help.
     *
     * @return true if help option contained in parsed options
     */
    public boolean userWantsHelp() {
        return this.getParsedOption(HELP_OPTION.getName()) != null;
    }

    /**
     * Parse the input.
     *
     * @param args the arguments to parse
     * @throws CmdLineException if an option is invalid
     */
    public void parse(final String args[]) throws CmdLineException {
        for (CmdLineOption option : this.options) {
            int index = this.findOption(args, option);
            if (index == -1 && option.isRequired()) {
                throw new CmdLineException("Missing required option: " + option);
            }

            if (index != -1) {
                CmdLineOption excludedOption = this.isExcluded(option.getName());
                if (excludedOption != null) {
                    throw new CmdLineException("Option -" + option.getName() + " may not be used with option -"
                            + excludedOption.getName());
                }

                String value = index + 1 >= args.length ? null : args[index + 1];
                if (option.needsValue() && value == null) {
                    throw new CmdLineException("Missing option value: " + option);
                }

                if (value != null && value.startsWith("-")) {
                    throw new CmdLineException(
                            "Option value "
                            + value
                            + " starts with a '-' character. Quote this character with \\ if this is really what you meant.");
                }

                if (value != null && value.startsWith("\\-")) {
                    value = value.substring(1);
                }
                ParsedOption newOption = new ParsedOption(option, value, (value != null));
                option.getValidator().validate(this, newOption);
                this.parsedOptions.put(option.getName(), newOption);
            }
        }

        for (ParsedOption parsed : this.parsedOptions.values()) {
            CmdLineOption option = parsed.getParent();
            if (option.hasRequiredOptions()) {
                for (CmdLineOption required : option.getRequiredOptions()) {
                    if (this.getParsedOption(required) == null) {
                        throw new CmdLineException("Option -" + option.getName() + " requires option -"
                                + required.getName());
                    }
                }
            }
        }

    }

    /**
     * Check if option is excluded.
     *
     * @param optionName the option name to check
     * @return the excluded command line option; may be null
     */
    private CmdLineOption isExcluded(final String optionName) {
        for (ParsedOption parsed : this.parsedOptions.values()) {
            CmdLineOption option = parsed.getParent();
            if (option.hasExcludedOptions()) {
                for (CmdLineOption excluded : option.getExcludedOptions()) {
                    if (excluded.getName().equals(optionName)) {
                        return option;
                    }
                }

            }
        }

        return null;
    }

    /**
     * Find an option within the arguments.
     *
     * @param args the parser arguments
     * @param option the option to find
     * @return the arguments index of the found option
     */
    private int findOption(final String args[], final CmdLineOption option) {
        int result = -1;
        for (int i = 0; i < args.length && result == -1; i++) {
            if (args[i].equals("-" + option.getName())) {
                result = i;
                break;
            }
        }
        return result;
    }

    /**
     * Get the option descriptions.
     *
     * @return the option descriptions
     */
    public String getDescriptions() {
        final StringBuffer sb = new StringBuffer("\n\nAvailable Options: \n\n");
        for (CmdLineOption option : this.options) {
            sb.append(option.toString(this));
            sb.append(" => ");
            sb.append(option.getDescription());
            sb.append("\n");
        }
        return sb.toString();
    }
}
