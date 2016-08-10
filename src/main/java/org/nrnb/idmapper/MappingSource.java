package org.nrnb.idmapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public enum MappingSource {

	ENSEMBL ("Ensembl", "En", "", "^ENS[A-Z]*[FPTG]\\d{11}$", "ENSG00000139618"),
	Entrez_Gene ("Entrez Gene", "L", "", "^\\d+$", "11234"),
	FlyBase ("FlyBase", "F", "Drosophila melanogaster", "^FB\\w{2}\\d{7}$", "FBgn0011293"),
	HGNC ("HGNC", "H", "Homo sapiens", "^[A-Za-z0-9]+", "DAPK1" ),
	KEGG_Genes ("KEGG Genes", "Kg", "", "^\\w+:[\\w\\d\\.-]*$", "syn:ssr3451" ),
	MGI ("MGI", "M", "Mus musculus", "^MGI:\\d+$", "MGI:2442292" ),
	miRBase ("miRBase", "Mbm", "", "MIMAT\\d{7}", "MIMAT0000001" ),
	RGD ("RGD", "R", "Rattus norvegicus", "^\\d{4,7}$", "2018" ),
	SGD ("SGD", "D", "Saccharomyces cerevisiae", "^S\\d+$", "S000028457" ),
	TAIR ("TAIR", "A", "Arabidopsis thaliana", "^AT[1-5]G\\d{5}$", "AT1G01030" ),
	UniGene ("UniGene", "U", "", "[A-Z][a-z][a-z]?\\.\\d+", "Hs.553708" ),
	Uniprot_TrEMBL("Uniprot-TrEMBL", "S", "", "^([A-N,R-Z][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9])|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])(\\.\\d+)?|([A-N,R-Z][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9])$", "P62158"),
	WormBase ("WormBase", "W", "Caenorhabditis elegans", "^WBGene\\d{8}$", "WBGene00000001" ),
	ZFIN ("ZFIN", "Z", "ZDB\\-GENE\\-\\d+\\-\\d+", "Danio rerio", "ZDB-GENE-041118-11");

	private String descriptor;
	private String system;
	private String species;
	private Pattern pattern;
	private String example;
	
	MappingSource(String s, String sy, String spec, String pat, String sample)
	{
		descriptor = s;
		system = sy;
		species = spec;
		pattern = Pattern.compile(pat);
		example = sample;
	}
	public String descriptor()	{ return descriptor;	}
	public String system()		{ return system;	}
	public String species()		{ return species;	}
	public Pattern pattern()	{ return pattern;	}
	
	public static MappingSource systemLookup(String sys)
	{
		for (MappingSource src : MappingSource.values())
			if (src.system.equals(sys)) 
				return src;
		return null;		
	}

	public static MappingSource nameLookup(String str)
	{
		for (MappingSource src : values())
			if (str.contains(src.descriptor)) 
				return src;
		return null;		
	}
	public static String[] allStrings() {
		int n = values().length;
		int i = 0;
		String[] strs = new String[n];
		for (MappingSource src : values())
			strs[i++] = src.descriptor + " (e.g., " + src.example + ")";
		return strs;
	}
	private static boolean VERBOSE = false;
	public static MappingSource guessSource(List<String> names) {

		Map<MappingSource, Integer> counter = new HashMap<MappingSource, Integer>();
		for (MappingSource src : values())
			counter.put(src, 0);
//		for (MappingSource src : values())
//			System.out.println(src.descriptor +  " matches " + counter.get(src));
	
		int sampleSize = Math.min(names.size(),  10);
		for (int i=0; i<sampleSize; i++)
		{
			String id = names.get(i);
			for (MappingSource src : values())
				if (src.patternMatch(id))
					counter.put(src, counter.get(src)+1);
		}
		MappingSource maxSrc = null;
		int maxCount = 0;
		
		for (MappingSource src : values())
		{
			int count = counter.get(src);
			if (count >= maxCount)  { 	maxCount = count; maxSrc = src;	}		// this favors the latter in the list
			if (VERBOSE) 
				System.out.println(src.descriptor +  " matches " + counter.get(src));
		}
		if (VERBOSE) 
			System.out.println(maxSrc.descriptor +  " is maximum with  " + counter.get(maxSrc) + " matches");
		return maxSrc;
	}
	private boolean patternMatch(String id) {		return pattern.matcher(id).matches();	}

}


