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
    public static final String CURIE_PREFIXES_CACHE_TTL = "idmapper.nodeNormalization.curiePrefixesCacheTtl";
    public static final String MAX_PREFIX_GUESSES = "idmapper.nodeNormalization.maxPrefixGuesses";
    public static final String USE_IDENTIFIER_FORMAT_FILTERS =
            "idmapper.nodeNormalization.useIdentifierFormatFilters";
    public static final String DEFAULT_BASE_URL = "https://nodenormalization-sri.renci.org";
    public static final int DEFAULT_BATCH_SIZE = 500;
    public static final int DEFAULT_CONNECT_TIMEOUT = 10000;
    public static final int DEFAULT_REQUEST_TIMEOUT = 30000;
    public static final int DEFAULT_MAX_PREFIX_GUESSES = 25;
    public static final boolean DEFAULT_USE_IDENTIFIER_FORMAT_FILTERS = true;
    public static final long DEFAULT_CURIE_PREFIXES_CACHE_TTL = 86400000L;

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
        props.setProperty(CURIE_PREFIXES_CACHE_TTL, Long.toString(DEFAULT_CURIE_PREFIXES_CACHE_TTL));
        props.setProperty(MAX_PREFIX_GUESSES, Integer.toString(DEFAULT_MAX_PREFIX_GUESSES));
        props.setProperty(USE_IDENTIFIER_FORMAT_FILTERS, Boolean.toString(DEFAULT_USE_IDENTIFIER_FORMAT_FILTERS));
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

    public static long getCuriePrefixesCacheTtl(final Properties props) {
        return getPositiveLong(props, CURIE_PREFIXES_CACHE_TTL, DEFAULT_CURIE_PREFIXES_CACHE_TTL);
    }

    public static int getMaxPrefixGuesses(final Properties props) {
        return getPositiveInt(props, MAX_PREFIX_GUESSES, DEFAULT_MAX_PREFIX_GUESSES);
    }

    public static boolean getUseIdentifierFormatFilters(final Properties props) {
        return getBoolean(props, USE_IDENTIFIER_FORMAT_FILTERS, DEFAULT_USE_IDENTIFIER_FORMAT_FILTERS);
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

    public static String defaultCuriePrefixesUrl() {
        return appendPath(DEFAULT_BASE_URL, "get_curie_prefixes");
    }

    private static String appendPath(final String baseUrl, final String path) {
        final String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        final String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        return cleanBaseUrl + "/" + cleanPath;
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

    private static long getPositiveLong(final Properties props, final String key, final long defaultValue) {
        if (props == null)
            return defaultValue;
        final String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty())
            return defaultValue;
        try {
            final long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean getBoolean(final Properties props, final String key, final boolean defaultValue) {
        if (props == null)
            return defaultValue;
        final String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty())
            return defaultValue;
        return Boolean.parseBoolean(value.trim());
    }
}
