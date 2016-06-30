/*
 *      Copyright (c) 2004-2015 YAMJ Members
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

/**
 * Validator which uses allowed values.
 */
public class EnumValidator extends OptionValidator {

    /**
     * Array with allowed values
     */
    private final String[] allowedValues;

    /**
     * Creates a new enumeration validator.
     *
     * @param allowedValues array of allowed values
     */
    public EnumValidator(final String[] allowedValues) {
        this.allowedValues = allowedValues.clone();
    }

    /**
     * Validate an option.
     *
     * @param cmdlineParser
     * @param parsedOption
     * @throws org.yamj.common.cmdline.CmdLineException
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
                throw new CmdLineException("Parameter " + parsedOption.getValue() + " is not valid for option '" + parsedOption.getParent() + "'");
            }
        } catch (final NullPointerException npe) {
            throw new CmdLineException("NULL value passed", npe);
        }
    }

    /**
     * Get the list of allowed values as string.
     *
     * @return string representation of allowed values
     */
    public String getAllowedValuesList() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; this.allowedValues != null && i < this.allowedValues.length; i++) {
            sb.append(this.allowedValues[i]);
            if (i + 1 < this.allowedValues.length) {
                sb.append("|");
            }
        }
        return sb.toString();
    }
}
