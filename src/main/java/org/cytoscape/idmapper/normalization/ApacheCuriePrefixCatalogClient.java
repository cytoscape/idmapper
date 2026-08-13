package org.cytoscape.idmapper.normalization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ApacheCuriePrefixCatalogClient implements CuriePrefixCatalogClient {

    private static final String GET_CURIE_PREFIXES_PATH = "/get_curie_prefixes";

    private final String prefixesUrl;
    private final int connectTimeoutMs;
    private final int requestTimeoutMs;

    public ApacheCuriePrefixCatalogClient(final String prefixesUrl, final int connectTimeoutMs,
            final int requestTimeoutMs) {
        this.prefixesUrl = prefixesUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.requestTimeoutMs = requestTimeoutMs;
    }

    @Override
    public List<CuriePrefixSuggestion> fetchPrefixSuggestions() throws NodeNormalizationException {
        if (prefixesUrl == null || prefixesUrl.trim().isEmpty())
            return Collections.emptyList();

        final HttpGet get = new HttpGet(prefixesUrl.trim() + GET_CURIE_PREFIXES_PATH);
        get.setHeader("Accept", "application/json");
        get.setConfig(RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMs)
                .setConnectionRequestTimeout(requestTimeoutMs)
                .setSocketTimeout(requestTimeoutMs)
                .build());

        try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(get)) {
            final int statusCode = response.getStatusLine().getStatusCode();
            final HttpEntity entity = response.getEntity();
            final String body = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
            if (statusCode < 200 || statusCode >= 300)
                throw new NodeNormalizationException("CURIE prefix catalog HTTP " + statusCode + ": " + body);
            return parsePrefixSuggestions(body);
        } catch (IOException e) {
            throw new NodeNormalizationException("Unable to fetch CURIE prefix catalog", e);
        }
    }

    public static List<CuriePrefixSuggestion> parsePrefixSuggestions(final String body)
            throws NodeNormalizationException {
        try {
            final Object parsed = new JSONParser().parse(body);
            if (!(parsed instanceof JSONObject))
                throw new NodeNormalizationException("CURIE prefix catalog response was not a JSON object");

            final Map<String, Long> countsByPrefix = new LinkedHashMap<String, Long>();
            final JSONObject categories = (JSONObject) parsed;
            for (final Object rawCategoryValue : categories.values()) {
                if (!(rawCategoryValue instanceof JSONObject))
                    continue;

                final Object rawCuriePrefix = ((JSONObject) rawCategoryValue).get("curie_prefix");
                if (!(rawCuriePrefix instanceof JSONObject))
                    continue;

                final JSONObject curiePrefix = (JSONObject) rawCuriePrefix;
                for (final Object rawEntry : curiePrefix.entrySet()) {
                    final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) rawEntry;
                    if (entry.getKey() == null || entry.getValue() == null)
                        continue;

                    final String prefix = entry.getKey().toString();
                    final long count = Long.parseLong(entry.getValue().toString());
                    final Long previous = countsByPrefix.get(prefix);
                    if (previous == null || count > previous.longValue())
                        countsByPrefix.put(prefix, Long.valueOf(count));
                }
            }

            final List<CuriePrefixSuggestion> suggestions = new ArrayList<CuriePrefixSuggestion>();
            for (final Map.Entry<String, Long> entry : countsByPrefix.entrySet())
                suggestions.add(new CuriePrefixSuggestion(entry.getKey(), entry.getValue().longValue()));
            Collections.sort(suggestions);
            return suggestions;
        } catch (ParseException e) {
            throw new NodeNormalizationException("Unable to parse CURIE prefix catalog response", e);
        } catch (NumberFormatException e) {
            throw new NodeNormalizationException("CURIE prefix catalog contained a non-numeric count", e);
        }
    }
}
