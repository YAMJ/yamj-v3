/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
