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
package org.yamj.core.database.model.type;

/**
 * List of known job types for people
 */
public enum JobType {

    /**
     * The person is a director
     */
    DIRECTOR,
    /**
     * The person is an actor
     */
    ACTOR,
    /**
     * The person is a guest star (kind of actor)
     */
    GUEST_STAR,
    /**
     * The person is a writer
     */
    WRITER,
    /**
     * The person is a producer
     */
    PRODUCER,
    /**
     * Camera and film related jobs
     */
    CAMERA,
    /**
     * Editing jobs, e.g. Editor, Dialog editor, Color Timer
     */
    EDITING,
    /**
     * All art related jobs
     */
    ART,
    /**
     * Costume and Make-up jobs
     */
    COSTUME_MAKEUP,
    /**
     * Sound jobs
     */
    SOUND,
    /**
     * Special effects
     */
    EFFECTS,
    /**
     * Misc Crew
     */
    CREW,
    /**
     * Lighting jobs
     */
    LIGHTING,
    /**
     * The job type is not known
     */
    UNKNOWN;

    /**
     * Determine the job type from a string
     *
     * @param type
     * @return
     */
    public static JobType fromString(String type) {
        try {
            return JobType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
