package com.moviejukebox.core.tools;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.moviejukebox.common.util.KeywordMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class PropertyTools extends PropertyPlaceholderConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyTools.class);

    private static Properties properties;
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

        properties = new Properties();
        for (Object key : props.keySet()) {
            String keyStr = key.toString();
            String valueStr = resolvePlaceholder(keyStr, props, springSystemPropertiesMode);
            properties.put(keyStr, valueStr);
        }
        
        LOGGER.info("Loaded " + properties.size() + " properties into core");
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Return the key property as a boolean
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String property = properties.getProperty(key);
        if (property != null) {
            return Boolean.parseBoolean(property.trim());
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
        String property = properties.getProperty(key);
        if (property != null) {
            try {
                return Integer.parseInt(property.trim());
            } catch (NumberFormatException nfe) {}
        }
        return defaultValue;
    }

    /**
     * Return the key property as an long
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static long getLongProperty(String key, long defaultValue) {
        String property = properties.getProperty(key);
        if (property != null) {
            try {
                return Long.parseLong(property.trim());
            } catch (NumberFormatException nfe) {}
        }
        return defaultValue;
    }

    /**
     * Return the key property as a float
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static float getFloatProperty(String key, float defaultValue) {
        String property = properties.getProperty(key);
        if (property != null) {
            try {
                return Float.parseFloat(property.trim());
            } catch (NumberFormatException nfe) {}
        }
        return defaultValue;
    }

    public static String getReplacedProperty(String newKey, String oldKey, String defaultValue) {
        String property = properties.getProperty(oldKey);
        if (property == null) {
            property = properties.getProperty(newKey, defaultValue);
        } else {
            LOGGER.warn("Property '" + oldKey + "' has been deprecated and will be removed; please use '" + newKey + "' instead");
        }
        return property;
    }

    public static boolean getReplacedBooleanProperty(String newKey, String oldKey, boolean defaultValue) {
        String property = properties.getProperty(oldKey);
        if (property == null) {
            property = properties.getProperty(newKey);
        } else {
            LOGGER.warn("Property '" + oldKey + "' has been deprecated and will be removed; please use '" + newKey + "' instead");
        }
        if (property != null) {
            return Boolean.parseBoolean(property.trim());
        }
        return defaultValue;
    }

    public static int getReplacedIntProperty(String newKey, String oldKey, int defaultValue) {
        String property = properties.getProperty(oldKey);
        if (property == null) {
            property = properties.getProperty(newKey);
        } else {
            LOGGER.warn("Property '" + oldKey + "' has been deprecated and will be removed; please use '" + newKey + "' instead");
        }
        if (property != null) {
            try {
                return Integer.parseInt(property.trim());
            } catch (NumberFormatException nfe) {}
        }
        return defaultValue;
    }

    public static long getReplacedLongProperty(String newKey, String oldKey, long defaultValue) {
        String property = properties.getProperty(oldKey);
        if (property == null) {
            property = properties.getProperty(newKey);
        } else {
            LOGGER.warn("Property '" + oldKey + "' has been deprecated and will be removed; please use '" + newKey + "' instead");
        }
        if (property != null) {
            try {
                return Long.parseLong(property.trim());
            } catch (NumberFormatException nfe) {}
        }
        return defaultValue;
    }

    public static float getReplacedFloatProperty(String newKey, String oldKey, float defaultValue) {
        String property = properties.getProperty(oldKey);
        if (property == null) {
            property = properties.getProperty(newKey);
        } else {
            LOGGER.warn("Property '" + oldKey + "' has been deprecated and will be removed; please use '" + newKey + "' instead");
        }
        if (property != null) {
            try {
                return Float.parseFloat(property.trim());
            } catch (NumberFormatException nfe) {}
        }
        return defaultValue;
    }

    // Issue 309
    public static Set<Entry<Object, Object>> getEntrySet() {
        // Issue 728
        // Shamelessly adapted from: http://stackoverflow.com/questions/54295/how-to-write-java-util-properties-to-xml-with-sorted-keys
        return new TreeMap<Object, Object>(properties).entrySet();
    }

    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public static void setProperty(String key, boolean value) {
        properties.setProperty(key, Boolean.toString(value));
    }

    public static void setProperty(String key, int value) {
        properties.setProperty(key, Integer.toString(value));
    }

    public static void setProperty(String key, long value) {
        properties.setProperty(key, Long.toString(value));
    }

    /**
     * Collect keywords list and appropriate keyword values. Example:
     * my.languages = EN,FR my.languages.EN = English my.languages.FR = French
     *
     * @param prefix Key for keywords list and prefix for value searching.
     * @return Ordered keyword list and map.
     */
    public static KeywordMap getKeywordMap(String prefix, String defaultValue) {
        KeywordMap keywordMap = new KeywordMap();

        String languages = getProperty(prefix, defaultValue);
        if (!isBlank(languages)) {
            for (String lang : languages.split("[ ,]+")) {
                lang = StringUtils.trimToNull(lang);
                if (lang == null) {
                    continue;
                }
                keywordMap.getKeywords().add(lang);
                String values = getProperty(prefix + "." + lang);
                if (values != null) {
                    keywordMap.put(lang, values);
                }
            }
        }

        return keywordMap;
    }
}