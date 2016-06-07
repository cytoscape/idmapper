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

public class BridgeDbIdMapper implements IdMapper {

    public static final String ENSEMBL = "Ensembl";
    public static final String GO = "Gene Ontology";
    public static final String UNIPROT = "UniProt";
    public static final String MGI = "MGI";
    //
    public static final String Gene_ID = "Gene_ID";
    public static final String EMBL = "EMBL";
    public static final String Entrez_Gene = "Entrez Gene";
    public static final String GenBank = "GenBank";
    public static final String Illumina = "Illumina";
    public static final String InterPro = "Uniprot-TrEMBL";
    public static final String UniGene = " UniGene";
    public static final String UCSC_Genome_Browser = "UCSC Genome Browser";
    public static final String RefSeq = "RefSeq";
    public static final String PDB = "PDB";

    public static final Map<String, String> LONG_TO_SHORT = new HashMap<String, String>();
    static {
        LONG_TO_SHORT.put(ENSEMBL, "En");
        LONG_TO_SHORT.put(GO, "T");
        LONG_TO_SHORT.put(UNIPROT, "S");
        LONG_TO_SHORT.put(MGI, "M");
        LONG_TO_SHORT.put(Gene_ID, "Wg");
        LONG_TO_SHORT.put(EMBL, "Em");
        LONG_TO_SHORT.put(Entrez_Gene, "L");
        LONG_TO_SHORT.put(GenBank, "G");
        LONG_TO_SHORT.put(Illumina, "Il");
        LONG_TO_SHORT.put(InterPro, "I");
        LONG_TO_SHORT.put(UniGene, "U");
        LONG_TO_SHORT.put(UCSC_Genome_Browser, "Uc");
        LONG_TO_SHORT.put(RefSeq, "Q");
        LONG_TO_SHORT.put(PDB, "Pd");
    }

    public static final Map<String, String> SHORT_TO_LONG = new HashMap<String, String>();
    static {
        SHORT_TO_LONG.put("En", ENSEMBL);
        SHORT_TO_LONG.put("T", GO);
        SHORT_TO_LONG.put("S", UNIPROT);
        SHORT_TO_LONG.put("M", MGI);
        SHORT_TO_LONG.put("Wg", Gene_ID);
        SHORT_TO_LONG.put("Em", EMBL);
        SHORT_TO_LONG.put("L", Entrez_Gene);
        SHORT_TO_LONG.put("G", GenBank);
        SHORT_TO_LONG.put("Il", Illumina);
        SHORT_TO_LONG.put("I", InterPro);
        SHORT_TO_LONG.put("U", UniGene);
        SHORT_TO_LONG.put("Uc", UCSC_Genome_Browser);
        SHORT_TO_LONG.put("Q", RefSeq);
        SHORT_TO_LONG.put("Pd", PDB);
    }

    public static final String DEFAULT_MAP_SERVICE_URL_STR = "http://webservice.bridgedb.org/batch";

    public static final String Human = "Human";
    public static final String Mouse = "Mouse";
    public static final String Rat = "Rat";
    public static final String Frog = "Frog";
    public static final String Zebra_fish = "Zebra fish";
    public static final String Fruit_fly = "Fruit fly";
    public static final String Mosquito = "Mosquito";
    public static final String Arabidopsis_thaliana = "Arabidopsis thaliana";
    public static final String Yeast = "Yeast";
    public static final String Escherichia_coli = "Escherichia coli";
    public static final String Tuberculosis = "Tuberculosis";
    public static final String Worm = "Worm";

    public static final boolean DEBUG = true;

    private final String _url;
    private Set<String> _unmatched_ids;
    private Set<String> _matched_ids;

    public BridgeDbIdMapper(final String url) {
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
            final String source_type, final String target_type,
            final String source_species, final String target_species) {
        List<String> res = null;
        _matched_ids = new TreeSet<String>();
        _unmatched_ids = new TreeSet<String>();
        try {
            res = BridgeDbIdMapper.runQuery(query_ids, target_species, "xrefs",
                    source_type, _url);
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        if (res != null) {
            for (final String l : res) {
                System.out.println(l);
            }
            if (res != null) {
                try {
                    final Map<String, IdMapping> map = new TreeMap<String, IdMapping>();

                    parseResponse(res, source_type, target_species,
                            target_type, map);
                    return map;

                }
                catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private final void parseResponse(final List<String> res,
            final String in_type, final String target_species,
            final String target_type, final Map<String, IdMapping> map)
                    throws IOException {

        for (final String s : res) {
            final String[] s1 = s.split("\t");
            if (s1.length != 3) {
                throw new IOException("illegal format: " + s);
            }
            final IdMappingImpl idmap = new IdMappingImpl();
            idmap.setTargetSpecies(target_species);
            idmap.setTargetType(target_type);
            idmap.setSourceType(s1[1]);
            idmap.addSourceId(s1[0]);

            final String[] s2 = s1[2].split(",");

            for (final String s2_str : s2) {
                if ((s2_str != null) && !s2_str.toLowerCase().equals("n/a")) {
                    // System.out.println(s2_str);
                    final String[] s3 = s2_str.split(":", 2);
                    if (s3.length != 2) {
                        throw new IOException("illegal format: " + s);
                    }
                    if (s3[0].equals(target_type)) {
                        idmap.addTargetId(s3[1]);

                    }
                }
            }
            System.out.println(idmap);
            if (idmap.getTargetIds().size() > 0) {
                map.put(s1[0], idmap);
                _matched_ids.add(s1[0]);
            }
            else {
                _unmatched_ids.add(s1[0]);
            }
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
            final String species, final String target, final String database,
            final String url_str) throws IOException {
        final String query = makeQuery(ids);
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
    public Map<String, IdGuess> guess(final Collection<String> query_ids,
            final String source_species) {
        return null;
    }

    public static void main(final String[] args) throws IOException {
        final Collection<String> ids = new ArrayList<String>();

        ids.add("ENSMUSG00000063455");
        ids.add("ENSMUSG00000073823");
        ids.add("ENSMUSG00000037031");

        final BridgeDbIdMapper map = new BridgeDbIdMapper(
                DEFAULT_MAP_SERVICE_URL_STR);

        final String source_type = "En";
        final String target_type = "S";
        final String source_species = "Mouse";
        final String target_species = "Mouse";

        final Map<String, IdMapping> x = map.map(ids, source_type, target_type,
                source_species, target_species);

        for (final Entry<String, IdMapping> entry : x.entrySet()) {
            System.out.println(entry.getKey() + "=>" + entry.getValue());
        }

    }

}
