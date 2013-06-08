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
package org.yamj.core.api.model;

import java.util.EnumMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.yamj.core.api.model.ParameterType.*;

/**
 *
 * @author Stuart
 */
public final class Parameters {

    private static final Logger LOG = LoggerFactory.getLogger(Parameters.class);
    private static final String RESTRICTION_NAME = "name";
    private static final String SORT_DESC = "desc";
    private static final String SORT_ASC = "asc";
    private static final String MATCH_ANY = "any";
    private static final String MATCH_START = "start";
    private static final String MATCH_END = "end";
    private static final String MATCH_EXACT = "exact";
    private Map<ParameterType, String> parameters;

    public Parameters() {
        this.parameters = new EnumMap<ParameterType, String>(ParameterType.class);
    }

    /**
     * Get a map of the parameters and their values
     *
     * @return
     */
    public Map<ParameterType, String> getParameters() {
        return parameters;
    }

    /**
     * Add a parameter and its value
     *
     * If the value is blank or null it will be excluded
     *
     * @param type
     * @param value
     */
    public void add(ParameterType type, String value) {
        if (StringUtils.isNotBlank(value)) {
            parameters.put(type, value.toLowerCase());
        }
    }

    /**
     * Add a parameter and its value (will be converted to string)
     *
     * If the value is less than zero it will be excluded
     *
     * @param type
     * @param value
     */
    public void add(ParameterType type, int value) {
        if (value >= 0) {
            add(type, String.valueOf(value));
        }
    }

    /**
     * Check if the parameter type exists
     *
     * @param type
     * @return
     */
    public boolean has(ParameterType type) {
        return parameters.containsKey(type);
    }

    /**
     * Check and add all the criteria
     *
     * @param criteria
     */
    public void criteriaAddAll(Criteria criteria) {
        criteriaSearch(criteria);
        criteriaSort(criteria);
        criteriaStart(criteria);
        criteriaMax(criteria);
    }

    /**
     * Add the search to the criteria
     *
     * @param criteria
     */
    public void criteriaSearch(Criteria criteria) {
        if (parameters.containsKey(SEARCH)) {
            String search = parameters.get(SEARCH);
            String matchMode;
            String searchField;

            // Set the default match mode if not configured
            if (parameters.containsKey(MATCHMODE)) {
                matchMode = parameters.get(MATCHMODE);
            } else {
                LOG.debug("No match mode provided, defaulting to {}", MATCH_ANY);
                parameters.put(MATCHMODE, MATCH_ANY);
                matchMode = MATCH_ANY;
            }

            // Set the default search field if not configured
            if (parameters.containsKey(SEARCH_FIELD)) {
                searchField = parameters.get(SEARCH_FIELD);
            } else {
                LOG.debug("No search field provided, defaulting to {}", RESTRICTION_NAME);
                parameters.put(SEARCH_FIELD, RESTRICTION_NAME);
                searchField = RESTRICTION_NAME;
            }

            if (MATCH_ANY.equalsIgnoreCase(matchMode)) {
                criteria.add(Restrictions.ilike(searchField, search, MatchMode.ANYWHERE));
            } else if (MATCH_START.equalsIgnoreCase(matchMode)) {
                criteria.add(Restrictions.ilike(searchField, search, MatchMode.START));
            } else if (MATCH_END.equalsIgnoreCase(matchMode)) {
                criteria.add(Restrictions.ilike(searchField, search, MatchMode.START));
            } else if (MATCH_EXACT.equalsIgnoreCase(matchMode)) {
                criteria.add(Restrictions.ilike(searchField, search, MatchMode.EXACT));
            } else {
                LOG.trace("Match mode '{}' not recognised, defaulting to {}", matchMode, MATCH_EXACT);
                criteria.add(Restrictions.ilike(searchField, search, MatchMode.EXACT));
            }
        }
    }

    /**
     * Add the sort to the criteria using the sortField
     *
     * @param criteria
     */
    public void criteriaSort(Criteria criteria) {
        if (parameters.containsKey(SORT) && parameters.containsKey(SORT_FIELD)) {
            if (SORT_ASC.equalsIgnoreCase(parameters.get(SORT))) {
                criteria.addOrder(Order.asc(parameters.get(SORT_FIELD)));
            } else if (SORT_DESC.equalsIgnoreCase(parameters.get(SORT))) {
                criteria.addOrder(Order.desc(parameters.get(SORT_FIELD)));
            } else {
                LOG.warn("Sorting ({}) not implemented for {}", parameters.get(SORT));
            }
        }
    }

    /**
     * Add the max results to the criteria
     *
     * @param criteria
     */
    public void criteriaMax(Criteria criteria) {
        if (parameters.containsKey(MAX)) {
            String max = parameters.get(MAX);
            if (StringUtils.isNumeric(max)) {
                criteria.setMaxResults(Integer.parseInt(max));
            }
        }
    }

    /**
     * Add the first result (start) value to the criteria
     *
     * @param criteria
     */
    public void criteriaStart(Criteria criteria) {
        if (parameters.containsKey(START)) {
            String start = parameters.get(START);
            if (StringUtils.isNumeric(start)) {
                criteria.setFirstResult(Integer.parseInt(start));
            }
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
