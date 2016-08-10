package org.nrnb.idmapper;

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

/**
 * An client for the Id Mapping service BridgeDB.
 *
 * See
 *
 * http://developers.bridgedb.org/wiki/BridgeWebservice
 *
 * http://www.bridgedb.org/swagger/#!/
 *
 * @author cmzmasek
 *
 */

public class BridgeDbIdMapper implements IdMapper {

    public static final String              DEFAULT_MAP_SERVICE_URL_STR = "http://webservice.bridgedb.org:8185";
     // Sources and target types:
//    	public static final String              ENSEMBL                     = "Ensembl";
//    	public static final String              Uniprot_TrEMBL              = "Uniprot-TrEMBL";
//        public static final String              Entrez_Gene                 = "Entrez Gene";
//        public static final String              KEGG_Genes					 = "KEGG Gene";
//        public static final String              GO                          = "Gene Ontology";
//        public static final String              MGI                         = "MGI";
// 
//        public static final String              miRBase                     = "miRBase";
//        public static final String              RGD                     	= "RGD";
//        public static final String              SGD                     	= "SGD";
//        public static final String              TAIR                    	 = "TAIR";
//        public static final String              UniGene                     = "UniGene";
//        public static final String              WormBase                    = "WormBase";
//        public static final String              ZFIN                        = "ZFIN";
       
//    public static final String              GenBank                     = "GenBank";
//    public static final String              Illumina                    = "Illumina";
//    public static final String              InterPro              		= "InterPro";
//    public static final String              UCSC_Genome_Browser         = "UCSC Genome Browser";
//    public static final String              RefSeq                      = "RefSeq";
//    public static final String              PDB                         = "PDB";

//    public static final String              UNIPROT                     = "UniProt";
    // To go between full and short names for types:
//    public static final Map<String, String> LONG_TO_SHORT               = new HashMap<String, String>();
//	static {
//		LONG_TO_SHORT.put(ENSEMBL, "En");
//		LONG_TO_SHORT.put(GO, "T");
//		LONG_TO_SHORT.put(Uniprot_TrEMBL, "S");
//		LONG_TO_SHORT.put(MGI, "M");
//		LONG_TO_SHORT.put(Gene_ID, "Wg");
//		LONG_TO_SHORT.put(EMBL, "Em");
//		LONG_TO_SHORT.put(Entrez_Gene, "L");
//		LONG_TO_SHORT.put(GenBank, "G");
//		LONG_TO_SHORT.put(Illumina, "Il");
//		LONG_TO_SHORT.put(InterPro, "I");
//		LONG_TO_SHORT.put(UniGene, "U");
//		LONG_TO_SHORT.put(UCSC_Genome_Browser, "Uc");
//		LONG_TO_SHORT.put(RefSeq, "Q");
//		LONG_TO_SHORT.put(PDB, "Pd");
//	}
//
//    // To go between full and short names for types:
//    public static final Map<String, String> SHORT_TO_LONG               = new HashMap<String, String>();
//    static {
//		SHORT_TO_LONG.put("En", ENSEMBL);
//		SHORT_TO_LONG.put("T", GO);
//		SHORT_TO_LONG.put("S", Uniprot_TrEMBL);
//		SHORT_TO_LONG.put("M", MGI);
//		SHORT_TO_LONG.put("Wg", Gene_ID);
//		SHORT_TO_LONG.put("Em", EMBL);
//		SHORT_TO_LONG.put("L", Entrez_Gene);
//		SHORT_TO_LONG.put("G", GenBank);
//		SHORT_TO_LONG.put("Il", Illumina);
//		SHORT_TO_LONG.put("I", InterPro);
//		SHORT_TO_LONG.put("U", UniGene);
//		SHORT_TO_LONG.put("Uc", UCSC_Genome_Browser);
//		SHORT_TO_LONG.put("Q", RefSeq);
//		SHORT_TO_LONG.put("Pd", PDB);
//	}

    // Select species:
//    public static final String              Human                       = "Human";
//    public static final String              Mouse                       = "Mouse";
//    public static final String              Rat                         = "Rat";
//    public static final String              Frog                        = "Frog";
//    public static final String              Zebra_fish                  = "Zebra fish";
//    public static final String              Fruit_fly                   = "Fruit fly";
//    public static final String              Mosquito                    = "Mosquito";
//    public static final String              Arabidopsis_thaliana        = "Arabidopsis thaliana";
//    public static final String              Yeast                       = "Yeast";
//    public static final String              Escherichia_coli            = "Escherichia coli";
//    public static final String              Tuberculosis                = "Tuberculosis";
//    public static final String              Worm                        = "Worm";

    public static final boolean             DEBUG                       = true;

    private final String                    _url;
    private Set<String>                     _unmatched_ids;
    private Set<String>                     _matched_ids;

    /**
     * Constructor, takes the URL of the service as parameter
     *
     * @param url
     *            the URL of the service
     */
    public BridgeDbIdMapper(final String url) {
        _url = url;
    }

    /**
     * Constructor
     *
     */
    public BridgeDbIdMapper() {
        _url = DEFAULT_MAP_SERVICE_URL_STR;
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
        List<String> res_list = null;
        _matched_ids = new TreeSet<String>();
        _unmatched_ids = new TreeSet<String>();
        try {
          System.out.println(target_species + ", " + source_type);
            res_list = BridgeDbIdMapper.runQuery(query_ids,
                                                 target_species,
                                                 "xrefsBatch",
                                                 source_type,
                                                 _url);
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        if (res_list != null) {
//            for (final String l : res_list) {
//                System.out.println(l);
//            }

            try {
                final Map<String, IdMapping> res = parseResponse(res_list,
                                                                 source_species,
                                                                 source_type,
                                                                 target_species,
                                                                 target_type);
                return res;

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

    /**
     * This parses the response (List of String).
     *
     *
     * @param res_list
     *            to response to be parsed
     * @param source_species
     *            the source species
     * @param source_type
     *            the source type
     * @param target_species
     *            the target species
     * @param target_type
     *            the target type
     * @return the result of the parsing as Map of String to IdMapping
     * @throws IOException
     */
    private final Map<String, IdMapping> parseResponse(final List<String> res_list,
                                                       final String source_species,
                                                       final String source_type,
                                                       final String target_species,
                                                       final String target_type) throws IOException {

        final Map<String, IdMapping> res = new TreeMap<String, IdMapping>();
        for (final String s : res_list) {
            final String[] s1 = s.split("\t");
            if (s1.length != 3) {
                throw new IOException("illegal format: " + s);
            }
            final IdMappingImpl idmap = new IdMappingImpl();
//            idmap.setTargetSpecies(target_species);
            idmap.setSourceSpecies(source_species);
            idmap.setTargetType(MappingSource.systemLookup(target_type));
            idmap.setSourceType(MappingSource.systemLookup(s1[1]));
            idmap.addSourceId(s1[0]);

            final String[] s2 = s1[2].split(",");

            for (final String s2_str : s2) {
                if ((s2_str != null) && !s2_str.toLowerCase().equals("n/a")) {
                    // System.out.println(s2_str);
                    final String[] s3 = s2_str.split(":", 2);
                    if (s3.length != 2) 
                        throw new IOException("illegal format: " + s);
                    
                    if (s3[0].equals(target_type)) 
                        idmap.addTargetId(s3[1]);
                }
            }
//            System.out.println(idmap);
            if (idmap.getTargetIds().size() > 0) {
                res.put(s1[0],
                        idmap);
                _matched_ids.add(s1[0]);
            }
            else {
                _unmatched_ids.add(s1[0]);
            }
        }
        return res;
    }

    /**
     * This posts a query to a URL
     *
     * @param url_str
     *            the URL to post to
     * @param species
     *            the species
     * @param target
     *            the target type
     * @param database
     *            the database
     * @param query
     *            the query
     * @return the response as List of String
     * @throws IOException
     */
    private static final List<String> post(final String url_str,
                                           final String species,
                                           final String target,
                                           final String database,
                                           final String query) throws IOException {
        final URL url = new URL(url_str + "/" + species + "/" + target + "/" + database);

//System.out.println("POSTING:  " + url.toString());
//System.out.println(query + "\n\n\n");
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        final OutputStream os = conn.getOutputStream();
        os.write(query.getBytes());
        os.flush();

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {

            throw new IOException("HTTP error code : " + conn.getResponseCode());

        }

        final BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

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

    /**
     * Runs to query against a URL.
     *
     * @param ids
     * @param species
     * @param target
     * @param database
     * @param url_str
     * @return
     * @throws IOException
     */
    private final static List<String> runQuery(final Collection<String> ids,
                                               final String species,
                                               final String target,
                                               final String database,
                                               final String url_str) throws IOException {
        final String query = makeQuery(ids);
        return post(url_str,
                    species,
                    target,
                    database,
                    query);
    }

    /**
     * To make the query String.
     *
     *
     * @param ids
     * @return
     */
    private final static String makeQuery(final Collection<String> ids) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final String id : ids) {
            if (first) {
                first = false;
            }
            else {
                sb.append("\n");
            }
            sb.append(id);
        }
        return sb.toString();
    }

    public static void main(final String[] args) throws IOException {
        final Collection<String> ids = new ArrayList<String>();

        ids.add("ENSMUSG00000063455");
        ids.add("ENSMUSG00000073823");
        ids.add("ENSMUSG00000037031");

        final BridgeDbIdMapper map = new BridgeDbIdMapper();

        final String source_type = "En";
        final String target_type = "S";
        final String source_species = "Mouse";
        final String target_species = "Mouse";

        final Map<String, IdMapping> x = map.map(ids,
                                                 source_type,
                                                 target_type,
                                                 source_species,
                                                 target_species);

//        for (final Entry<String, IdMapping> entry : x.entrySet()) {
//            System.out.println(entry.getKey() + "=>" + entry.getValue());
//        }

    }

}
