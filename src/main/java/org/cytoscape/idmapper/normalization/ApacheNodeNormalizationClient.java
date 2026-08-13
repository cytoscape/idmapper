package org.cytoscape.idmapper.normalization;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ApacheNodeNormalizationClient implements NodeNormalizationClient {

    private static final String NORMALIZED_NODES_PATH = "/get_normalized_nodes";

    private final String baseUrl;
    private final int connectTimeoutMs;
    private final int requestTimeoutMs;

    public ApacheNodeNormalizationClient(final String baseUrl, final int connectTimeoutMs, final int requestTimeoutMs) {
        this.baseUrl = baseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.requestTimeoutMs = requestTimeoutMs;
    }

    @Override
    public Map<String, NodeNormalizationResult> normalize(final List<String> curies) throws NodeNormalizationException {
        final HttpPost post = new HttpPost(buildEndpoint(baseUrl));
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");
        post.setConfig(RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMs)
                .setConnectionRequestTimeout(requestTimeoutMs)
                .setSocketTimeout(requestTimeoutMs)
                .build());
        post.setEntity(new StringEntity(buildRequestBody(curies), ContentType.APPLICATION_JSON));

        try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(post)) {
            final int statusCode = response.getStatusLine().getStatusCode();
            final HttpEntity entity = response.getEntity();
            final String body = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
            if (statusCode < 200 || statusCode >= 300)
                throw new NodeNormalizationException("Node Normalization HTTP " + statusCode + ": " + body);
            return parseResponse(curies, body);
        } catch (IOException e) {
            throw new NodeNormalizationException("Unable to call Node Normalization service", e);
        }
    }

    public static URI buildEndpoint(final String baseUrl) throws NodeNormalizationException {
        if (baseUrl == null || baseUrl.trim().isEmpty())
            throw new NodeNormalizationException("Node Normalization base URL is empty");

        final String trimmed = baseUrl.trim();
        final String joined = trimmed.endsWith("/")
                ? trimmed.substring(0, trimmed.length() - 1) + NORMALIZED_NODES_PATH
                : trimmed + NORMALIZED_NODES_PATH;
        try {
            return new URI(joined);
        } catch (URISyntaxException e) {
            throw new NodeNormalizationException("Invalid Node Normalization URL: " + joined, e);
        }
    }

    @SuppressWarnings("unchecked")
    static String buildRequestBody(final List<String> curies) {
        final JSONObject request = new JSONObject();
        final JSONArray identifiers = new JSONArray();
        if (curies != null)
            identifiers.addAll(curies);
        request.put("curies", identifiers);
        return request.toJSONString();
    }

    public static Map<String, NodeNormalizationResult> parseResponse(final List<String> requestedCuries, final String body)
            throws NodeNormalizationException {
        final Map<String, NodeNormalizationResult> results = new LinkedHashMap<String, NodeNormalizationResult>();
        try {
            final Object parsed = new JSONParser().parse(body);
            if (!(parsed instanceof JSONObject))
                throw new NodeNormalizationException("Node Normalization response was not a JSON object");

            final JSONObject object = (JSONObject) parsed;
            for (final String curie : requestedCuries) {
                final Object rawNode = object.get(curie);
                if (!(rawNode instanceof JSONObject)) {
                    results.put(curie, new NodeNormalizationResult(curie, null));
                    continue;
                }

                final Object rawId = ((JSONObject) rawNode).get("id");
                String canonical = null;
                if (rawId instanceof JSONObject) {
                    final Object identifier = ((JSONObject) rawId).get("identifier");
                    if (identifier != null)
                        canonical = identifier.toString();
                }
                results.put(curie, new NodeNormalizationResult(curie, canonical));
            }
            return results;
        } catch (ParseException e) {
            throw new NodeNormalizationException("Unable to parse Node Normalization response", e);
        }
    }
}
