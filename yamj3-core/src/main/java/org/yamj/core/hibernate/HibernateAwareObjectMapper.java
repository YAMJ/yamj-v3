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
package org.yamj.core.hibernate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * Taken from the Jackson Hibernate module<br>
 * This ensures that the Lazy Instantiated objects are fully instantiated before returning the JSON information to the API
 *
 * https://github.com/FasterXML/jackson-module-hibernate
 *
 * @author Stuart
 */
public class HibernateAwareObjectMapper extends ObjectMapper {
    private static final long serialVersionUID = 34607231867726798L;

    public HibernateAwareObjectMapper() {
        // https://github.com/FasterXML/jackson-module-hibernate
        Hibernate4Module hm = new Hibernate4Module();
        registerModule(hm);
        hm.configure(Hibernate4Module.Feature.FORCE_LAZY_LOADING, true);

        // https://github.com/FasterXML/jackson-datatype-joda
        registerModule(new JodaModule());
    }
}
