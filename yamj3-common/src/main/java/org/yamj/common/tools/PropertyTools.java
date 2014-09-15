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
package org.yamj.common.tools;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.yamj.common.util.KeywordMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public final class PropertyTools extends PropertyPlaceholderConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyTools.class);
    private static final Properties PROPERTIES = new Properties();
    // Default as in PropertyPlaceholderConfigurer
    private int springSystemPropertiesMode = SYSTEM_PROPERTIES_MODE_FALLBACK;

    @Override
    public void setSystemPropertiesMode(int systemPropertiesMode) {
        super.setSystemPropertiesMode(systemPropertiesMode);
        springSystemPropertiesMode = systemPropertiesMode;
    }

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) throws BeansException {
        super.processProperties(beanFactory, props);

        PROPERTIES.clear();
        for (Object key : props.keySet()) {
            String keyStr = key.toString();
            String valueStr = resolvePlaceholder(keyStr, props, springSystemPropertiesMode);
            PROPERTIES.put(keyStr, valueStr);
        }

        LOG.info("Loaded {} properties", PROPERTIES.size());
    }

    public static String getProperty(String key) {
        return StringUtils.trimToNull(PROPERTIES.getProperty(key));
    }

    public static String getProperty(String key, String defaultValue) {
        return StringUtils.trimToEmpty(PROPERTIES.getProperty(key, defaultValue));
    }

    /**
     * Return the key property as a boolean
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String property = StringUtils.trimToEmpty(PROPERTIES.getProperty(key));

        if (StringUtils.isNotBlank(property)) {
            return Boolean.parseBoolean(property);
        }
        return defaultValue;
    }

    /**
     * Return the key property as integer
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static int getIntProperty(String key, int defaultValue) {
        String property = StringUtils.trimToEmpty(PROPERTIES.getProperty(key));
        return NumberUtils.toInt(property, defaultValue);
    }

    /**
     * Return the key property as an long
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static long getLongProperty(String key, long defaultValue) {
        String property = StringUtils.trimToEmpty(PROPERTIES.getProperty(key));
        return NumberUtils.toLong(property, defaultValue);
    }

    /**
     * Return the key property as a float
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static float getFloatProperty(String key, float defaultValue) {
        String property = StringUtils.trimToEmpty(PROPERTIES.getProperty(key));
        return NumberUtils.toFloat(property, defaultValue);
    }

    public static String getReplacedProperty(String newKey, String oldKey, String defaultValue) {
        String property = StringUtils.trimToEmpty(PROPERTIES.getProperty(oldKey));
        if (StringUtils.isBlank(property)) {
            property = PROPERTIES.getProperty(newKey, defaultValue);
        } else {
            LOG.warn("Property '{}' has been deprecated and will be removed; please use '{}' instead", oldKey, newKey);
        }
        return property;
    }

    public static boolean getReplacedBooleanProperty(String newKey, String oldKey, boolean defaultValue) {
        String property = StringUtils.trimToEmpty(PROPERTIES.getProperty(oldKey));
        if (StringUtils.isBlank(property)) {
            property = StringUtils.trimToEmpty(PROPERTIES.getProperty(newKey));
        } else {
            LOG.warn("Property '{}' has been deprecated and will be removed; please use '{}' instead", oldKey, newKey);
        }

        if (StringUtils.isNotBlank(property)) {
            return Boolean.parseBoolean(property.trim());
        }
        return defaultValue;
    }

    public static int getReplacedIntProperty(String newKey, String oldKey, int defaultValue) {
        String property = StringUtils.trimToEmpty(PROPERTIES.getProperty(oldKey));
        if (StringUtils.isBlank(property)) {
            property = PROPERTIES.getProperty(newKey);
        } else {
            LOG.warn("Property '{}' has been deprecated and will be removed; please use '{}' instead", oldKey, newKey);
        }

        return NumberUtils.toInt(PROPERTIES.getProperty(property), defaultValue);
    }

    public static long getReplacedLongProperty(String newKey, String oldKey, long defaultValue) {
        String property = StringUtils.trimToEmpty(PROPERTIES.getProperty(oldKey));
        if (StringUtils.isBlank(property)) {
            property = PROPERTIES.getProperty(newKey);
        } else {
            LOG.warn("Property '{}' has been deprecated and will be removed; please use '{}' instead", oldKey, newKey);
        }

        return NumberUtils.toLong(PROPERTIES.getProperty(property), defaultValue);
    }

    public static float getReplacedFloatProperty(String newKey, String oldKey, float defaultValue) {
        String property = StringUtils.trimToEmpty(PROPERTIES.getProperty(oldKey));
        if (StringUtils.isBlank(property)) {
            property = PROPERTIES.getProperty(newKey);
        } else {
            LOG.warn("Property '{}' has been deprecated and will be removed; please use '{}' instead", oldKey, newKey);
        }

        return NumberUtils.toFloat(PROPERTIES.getProperty(property), defaultValue);
    }

    public static Set<Entry<Object, Object>> getEntrySet() {
        // Shamelessly adapted from: http://stackoverflow.com/questions/54295/how-to-write-java-util-properties-to-xml-with-sorted-keys
        return new TreeMap<Object, Object>(PROPERTIES).entrySet();
    }

    public static void setProperty(String key, String value) {
        PROPERTIES.setProperty(key, value);
    }

    public static void setProperty(String key, boolean value) {
        PROPERTIES.setProperty(key, Boolean.toString(value));
    }

    public static void setProperty(String key, int value) {
        PROPERTIES.setProperty(key, Integer.toString(value));
    }

    public static void setProperty(String key, long value) {
        PROPERTIES.setProperty(key, Long.toString(value));
    }

    /**
     * Collect keywords list and appropriate keyword values. <br>
     * Example: <br>
     * my.languages=EN,FR<br>
     * my.languages.EN=English<br>
     * my.languages.FR=French
     *
     * @param prefix Key for keywords list and prefix for value searching.
     * @param defaultValue
     * @return Ordered keyword list and map.
     */
    public static KeywordMap getKeywordMap(String prefix, String defaultValue) {
        KeywordMap keywordMap = new KeywordMap();

        String propertyString = getProperty(prefix, defaultValue);
        if (!isBlank(propertyString)) {
            for (String singleProperty : propertyString.split("[ ,]+")) {
                singleProperty = StringUtils.trimToNull(singleProperty);
                if (singleProperty == null) {
                    continue;
                }
                keywordMap.getKeywords().add(singleProperty);
                String values = getProperty(prefix + "." + singleProperty);
                if (values != null) {
                    keywordMap.put(singleProperty, values);
                }
            }
        }

        return keywordMap;
    }
}
