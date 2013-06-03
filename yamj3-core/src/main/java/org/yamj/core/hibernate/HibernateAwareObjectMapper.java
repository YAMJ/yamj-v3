package org.yamj.core.hibernate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate3.Hibernate3Module;

/**
 * Taken from the Jackson Hibernate module<br>
 * This ensures that the Lazy Instantiated objects are fully instantiated before returning the JSON information to the API
 *
 * https://github.com/FasterXML/jackson-module-hibernate
 *
 * @author Stuart
 */
public class HibernateAwareObjectMapper extends ObjectMapper {

    public HibernateAwareObjectMapper() {
        registerModule(new Hibernate3Module());
    }
}
