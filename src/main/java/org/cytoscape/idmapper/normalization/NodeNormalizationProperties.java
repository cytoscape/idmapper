package org.cytoscape.idmapper.normalization;

import java.util.Properties;

/**
 * 
 * Provides default properties and utility methods for node normalization.
 */
public final class NodeNormalizationProperties {

    public static final String PROPERTY_NAME = "idmapper.nodeNormalization";
    public static final String BASE_URL = "idmapper.nodeNormalization.baseUrl";
    public static final String BATCH_SIZE = "idmapper.nodeNormalization.batchSize";
    public static final String CONNECT_TIMEOUT = "idmapper.nodeNormalization.connectTimeout";
    public static final String REQUEST_TIMEOUT = "idmapper.nodeNormalization.requestTimeout";

    public static final String DEFAULT_BASE_URL = "https://nodenormalization-sri.renci.org";
    public static final int DEFAULT_BATCH_SIZE = 500;
    public static final int DEFAULT_CONNECT_TIMEOUT = 10000;
    public static final int DEFAULT_REQUEST_TIMEOUT = 30000;

    private NodeNormalizationProperties() {
    }

    /**
     * Returns a Properties object containing the default node normalization
     * properties.
     * 
     * @return
     */
    public static Properties defaults() {
        final Properties props = new Properties();
        props.setProperty(BASE_URL, DEFAULT_BASE_URL);
        props.setProperty(BATCH_SIZE, Integer.toString(DEFAULT_BATCH_SIZE));
        props.setProperty(CONNECT_TIMEOUT, Integer.toString(DEFAULT_CONNECT_TIMEOUT));
        props.setProperty(REQUEST_TIMEOUT, Integer.toString(DEFAULT_REQUEST_TIMEOUT));
        return props;
    }

    /**
     * Returns the base URL for node normalization from the given properties, or the
     * default if not set.
     * 
     * @param props
     * @return
     */
    public static String getBaseUrl(final Properties props) {
        return getString(props, BASE_URL, DEFAULT_BASE_URL);
    }

    /**
     * Returns the batch size for node normalization from the given properties, or
     * the
     * default if not set.
     * 
     * @param props
     * @return
     */
    public static int getBatchSize(final Properties props) {
        return getPositiveInt(props, BATCH_SIZE, DEFAULT_BATCH_SIZE);
    }

    /**
     * Returns the connect timeout for node normalization from the given properties,
     * or the default if not set.
     * 
     * @param props
     * @return
     */
    public static int getConnectTimeout(final Properties props) {
        return getPositiveInt(props, CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
    }

    /**
     * Returns the request timeout for node normalization from the given properties,
     * or the default if not set.
     * 
     * @param props
     * @return
     */
    public static int getRequestTimeout(final Properties props) {
        return getPositiveInt(props, REQUEST_TIMEOUT, DEFAULT_REQUEST_TIMEOUT);
    }

    /**
     * Returns the value of the given key from the properties, or the default value
     * if
     * the key is not set or the value is empty.
     * 
     * @param props
     * @param key
     * @param defaultValue
     * @return
     */
    private static String getString(final Properties props, final String key, final String defaultValue) {
        if (props == null)
            return defaultValue;
        final String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty())
            return defaultValue;
        return value.trim();
    }

    /**
     * Returns the value of the given key from the properties as a positive integer,
     * or the default value if the key is not set, the value is empty, or the value
     * is not a positive integer.
     * 
     * @param props
     * @param key
     * @param defaultValue
     * @return
     */
    private static int getPositiveInt(final Properties props, final String key, final int defaultValue) {
        if (props == null)
            return defaultValue;
        final String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty())
            return defaultValue;
        try {
            final int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
