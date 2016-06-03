package org.cytoscape.task.internal.table;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class IdMapBridgeDb implements IdMapper {
    
   
    public static final String ENSEMBL = "En";
    public static final String GO = "T";
    public static final String UNIPROT = "S";
    public static final String MGI = "M";
    public static final String NCBI = "Q";
    
    public static final Map<String, String> LABELS = new HashMap<String, String>();
    static {
        LABELS.put(ENSEMBL, "Ensembl");
        LABELS.put(GO, "GO");
        LABELS.put(UNIPROT, "UniProt");
        LABELS.put(MGI, "MGI");
        LABELS.put(NCBI , "NCBI");
       
    }
        
    
    public static final String DEFAULT_MAP_SERVICE_URL_STR = "http://webservice.bridgedb.org/batch";
    
    
    public static final String Human  = "Human";
    public static final String Mouse  = "Mouse";
    public static final String Rat  = "Rat";
    public static final String Frog  = "Frog";
    public static final String Zebra_fish = "Zebra fish";
    public static final String Fruit_fly = "Fruit fly";
    public static final String Mosquito = "Mosquito";
    public static final String Arabidopsis_thaliana = "Arabidopsis thaliana";
    public static final String Yeast = "Yeast";
    public static final String Escherichia_coli = "Escherichia coli";
    public static final String Tuberculosis = "Tuberculosis";

    public static final boolean DEBUG = true;
    private String _url;
    private Set<String> _unmatched_ids;
    private Set<String> _matched_ids;

    public IdMapBridgeDb(String url) {
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
    public Map<String, IdMapping> map(Collection<String> query_ids,
            String source_type, String target_type, String source_species,
            String target_species) {
        List<String> res = null;
        try {
            res = IdMapBridgeDb
                    .runQuery(query_ids, target_species, "xrefs", source_type, _url);
            
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        for (String l : res) {
            System.out.println(l);
        }
        if (res != null) {
            try {
                Map<String, IdMapping> map = new TreeMap<String, IdMapping>();
                
                parseResponse(res, source_type, target_species, target_type,
                        map);
                return map;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private final void parseResponse(final List<String> res,
            final String in_type, final String target_species,
            final String target_type, final Map<String, IdMapping> map)
            throws IOException {
        
        _matched_ids = new TreeSet<String>();

        for (final String s : res) {
            final String[] s1 = s.split("\t");
            if (s1.length != 3) {
                throw new IOException("illegal format: " + s);
            }
            IdMappingImpl idmap = new IdMappingImpl();
            idmap.setTargetSpecies(target_species);
            idmap.setTargetType(target_type);
            idmap.setSourceType(s1[1]);
            idmap.addSourceId(s1[0]);
            // System.out.println(s1[0]);

            final String[] s2 = s1[2].split(",");

            for (String s2_str : s2) {
                if (s2_str != null && !s2_str.toLowerCase().equals("n/a")) {
                    // System.out.println(s2_str);
                    final String[] s3 = s2_str.split(":", 2);
                    if (s3.length != 2) {
                        throw new IOException("illegal format: " + s);
                    }
                    if (s3[0].equals(target_type)) {
                        // System.out.println(s3[0] + " => " + s3[1]);
                        idmap.addTargetId(s3[1]);

                    }
                }
            }
            System.out.println(idmap);
            map.put(s1[0], idmap);
            _matched_ids.add(s1[0]);

        }

    }

    private static final List<String> post(final String url_str,
            final String species, final String target, final String database,
            final String query) throws IOException {
        final URL url = new URL(url_str + "/" + species + "/" + target + "/"
                + database);
        if (DEBUG) {
            // System.out.println(url.toString());
        }
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        // conn.setRequestProperty("Content-Type", "application/json");

        final OutputStream os = conn.getOutputStream();
        os.write(query.getBytes());
        os.flush();

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {

            throw new IOException("HTTP error code : " + conn.getResponseCode());

        }

        final BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));

        final List<String> res = new ArrayList<String>();
        String line;

        while ((line = br.readLine()) != null) {
            res.add(line);
        }

        br.close();
        conn.disconnect();
        os.close();

        return res;
    }

    private final static List<String> runQuery(final Collection<String> ids,
                                              final String species,
                                              final String target,
                                              final String database,
            final String url_str) throws IOException {
        final String query = makeQuery(ids);
        // System.out.println("url=" + url_str);
        // System.out.println("query=" + query);
        return post(url_str, species, target, database, query);
    }

    private static final String makeQuery(final Collection<String> ids) {
        final StringBuilder sb = new StringBuilder();

        sb.append(listToString(ids));

        return sb.toString();
    }

    private final static StringBuilder listToString(final Collection<String> l) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final String s : l) {
            if (first) {
                first = false;
            }
            else {
                sb.append("\n");
            }
            sb.append(s);
        }
        return sb;
    }

  

    @Override
    public Map<String, IdGuess> guess(Collection<String> query_ids,
            String source_species) {
        return null;
    }
    
    public static void main(final String[] args) throws IOException {
        final Collection<String> ids = new ArrayList<String>();

        ids.add("ENSMUSG00000063455");
        ids.add("ENSMUSG00000073823");
        // for (int i = 0; i < 10000; i++) {
        // ids.add("ENSMUSG00000037031");
        // }
        ids.add("ENSMUSG00000037031");
        
        IdMapBridgeDb map = new IdMapBridgeDb(DEFAULT_MAP_SERVICE_URL_STR);

        String source_type = "En";
        String target_type = "S";
        String source_species = "Mouse";
        String target_species = "Mouse";

        Map<String, IdMapping> x = map.map(ids, source_type, target_type,
                source_species, target_species);
        
        for (Entry<String, IdMapping> entry : x.entrySet())
        {
            System.out.println(entry.getKey() + "=>" + entry.getValue());
        }

        

    }
    
 
}
