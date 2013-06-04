/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.common.remote.service;

public interface GitHubService {

    /**
     * Get the push date for the default repository
     *
     * @return
     */
    String pushDate();

    /**
     * Get the push date for the given owner/repository combination
     *
     * @param owner
     * @param repository
     * @return
     */
    String pushDate(String owner, String repository);

    /**
     * Check the installation date of the default owner/repository
     *
     * @param buildDate
     * @param maxAge
     * @return
     */
    boolean checkInstallationDate(String buildDate, int maxAgeDays);

    /**
     * Check the installation date of the owner/repository
     *
     * @param owner
     * @param repository
     * @param buildDate
     * @param maxAge
     * @return
     */
    boolean checkInstallationDate(String owner, String repository, String buildDate, int maxAgeDays);
}
