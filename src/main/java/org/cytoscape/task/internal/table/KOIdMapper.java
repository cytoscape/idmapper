package org.cytoscape.task.internal.table;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KOIdMapper implements IdMapper {

    // https://github.com/cytoscape-ci/service-idmapping
    public static final String  DEFAULT_MAP_SERVICE_URL_STR = "http://ci-dev-serv.ucsd.edu:3000/map";

    // Data sources:
    public static final String  UniProtKB_AC                = "UniProtKB-AC";
    public static final String  UniProtKB_ID                = "UniProtKB-ID";
    public static final String  RefSeq                      = "RefSeq";
    public static final String  GI                          = "GI";
    public static final String  PDB                         = "PDB";
    public static final String  GO                          = "GO";
    public static final String  UniRef100                   = "UniRef100";
    public static final String  UniRef90                    = "UniRef90";
    public static final String  UniRef50                    = "UniRef50";
    public static final String  UniParc                     = "UniParc";
    public static final String  PIR                         = "PIR";
    public static final String  EMBL                        = "EMBL";
    public static final String  GENE_ID                     = "GeneID";
    public static final String  ENSEMBL                     = "Ensembl";
    public static final String  SYMBOL                      = "Symbol";
    public static final String  SYNONYMS                    = "Synonyms";

    // Species:
    public static final String  HUMAN                       = "human";
    public static final String  MOUSE                       = "mouse";
    public static final String  FLY                         = "fly";
    public static final String  YEAST                       = "yeast";

    // Terms used in the json respone:
    private static final String UNMATCHED                   = "unmatched";
    private static final String MATCHED                     = "matched";
    private static final String MATCHES                     = "matches";
    private static final String IN                          = "in";
    private static final String IN_TYPE                     = "inType";
    private static final String SPECIES                     = "species";

    public static final boolean DEBUG                       = true;

    private final String        _url;
    private Set<String>         _unmatched_ids;
    private Set<String>         _matched_ids;

    /**
     * Constructor, takes the URL of the service as parameter
     *
     * @param url
     *            the URL of the service
     */
    public KOIdMapper(final String url) {
        _url = url;
    }

    @Override
    public Set<String> getUnmatchedIds() {
        return _unmatched_ids;
    }

    @Override
    public Set<String> getMatchedIds() {
        return _matched_ids;
    }

    @Override
    public Map<String, IdMapping> map(final Collection<String> query_ids,
                                      final String source_type,
                                      final String target_type,
                                      final String source_species,
                                      final String target_species) {

        String json_str = null;
        _matched_ids = new TreeSet<String>();
        _unmatched_ids = new TreeSet<String>();
        try {
            json_str = runQuery(query_ids,
                                target_type,
                                source_type,
                                _url);
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        if (json_str != null) {

            System.out.println(json_str);

            try {
                final Map<String, IdMapping> map = new TreeMap<String, IdMapping>();
                final SortedSet<String> in_types = new TreeSet<String>();
                in_types.add(KOIdMapper.SYNONYMS);
                in_types.add(source_type);
                parseResponse(json_str,
                              in_types,
                              target_species,
                              target_type,
                              map);

                for (final String id : map.keySet()) {
                    _matched_ids.add(id);
                }

                for (final String query_id : query_ids) {
                    if (!_matched_ids.contains(query_id)) {
                        _matched_ids.add(query_id);
                    }
                }

                return map;

            }
            catch (final IOException e) {
                e.printStackTrace();
            }

        }
        return null;
    }

    @Override
    public Map<String, IdGuess> guess(final Collection<String> query_ids,
                                      final String source_species) {
        return null;
    }

    private final static void parseResponse(final String json_str,
                                            final Set<String> in_types,
                                            final String target_species,
                                            final String target_type,
                                            final Map<String, IdMapping> map) throws IOException, JsonProcessingException {
        if (DEBUG) {
            System.out.println("str =" + json_str);
        }
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode root = mapper.readTree(json_str);
        if (DEBUG) {
            System.out.println("root=" + root);
        }

        if (!root.has(MATCHED)) {
            throw new IOException("no " + MATCHED + " field");
        }

        final JsonNode matched = root.path(MATCHED);

        final Iterator<JsonNode> matched_it = matched.elements();
        while (matched_it.hasNext()) {
            final JsonNode n = matched_it.next();
            if (n.has(SPECIES)) {
                if (target_species.equals(n.get(SPECIES).asText())) {
                    if (in_types.contains(n.get(IN_TYPE).asText())) {
                        final String in = n.get(IN).asText();
                        if (n.has(MATCHES)) {
                            final JsonNode m = n.get(MATCHES);
                            if (m.size() > 0) {
                                if (m.has(target_type)) {
                                    final JsonNode target_ids = m.get(target_type);
                                    if (target_ids.isArray()) {
                                        final Iterator<JsonNode> it = target_ids.iterator();
                                        while (it.hasNext()) {
                                            final JsonNode target_id = it.next();
                                            addMappedId(map,
                                                        in,
                                                        target_id.asText());
                                        }
                                    }
                                    else {
                                        addMappedId(map,
                                                    in,
                                                    target_ids.asText());
                                    }
                                }
                                else {
                                    if (DEBUG) {
                                        System.out.println(json_str);
                                        System.out.println("m=" + m);
                                    }
                                    throw new IOException("no target type: " + target_type);
                                }
                            }
                        }
                        else {
                            throw new IOException("no " + MATCHES + " field");
                        }
                    }
                }
            }
            else {
                throw new IOException("no species: " + target_species);
            }
        }
    }

    private static final String post(final String url_str,
                                     final String source_type,
                                     final String json_query) throws IOException {
        final URL url = new URL(url_str);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",
                                "application/json");
        if (DEBUG) {
            System.out.println(json_query);
        }
        final OutputStream os = conn.getOutputStream();
        os.write(json_query.getBytes());
        os.flush();

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            if (conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new IOException("No mapping available for the data in this column");
            }
            else {
                throw new IOException("HTTP error code : " + conn.getResponseCode());
            }
        }

        final BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        br.close();
        conn.disconnect();
        os.close();

        return sb.toString();
    }

    private final static String runQuery(final Collection<String> ids,
                                         final String target_type,
                                         final String source_type,
                                         final String url) throws IOException {
        final String json_query = makeQuery(ids,
                                            target_type);
        if (DEBUG) {
            System.out.println("url=" + url);
            System.out.println("json_query=" + json_query);
        }
        return post(url,
                    source_type,
                    json_query);
    }

    private final static void addMappedId(final Map<String, IdMapping> map,
                                          final String in,
                                          final String id) {
        if ((id != null) && (id.length() > 0)) {
            if (!map.containsKey(in)) {
                map.put(in,
                        new IdMappingImpl());
            }
            map.get(in).getTargetIds().add(id);
        }
    }

    private final static StringBuilder listToString(final Collection<String> l) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final String s : l) {
            if (first) {
                first = false;
            }
            else {
                sb.append(",");
            }
            sb.append("\"");
            sb.append(s);
            sb.append("\"");
        }
        return sb;
    }

    private static final String makeQuery(final Collection<String> ids,
                                          final String target_type) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"ids\": [");
        sb.append(listToString(ids));
        sb.append("], \"idTypes\":[\"" + target_type + "\"] }");
        return sb.toString();
    }

}
