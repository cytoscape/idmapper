package org.cytoscape.idmapper.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public enum MappingSource {

//	Unspecified ("Unspecified", "", "", "^[A-Za-z0-9]+", "" ),
	HGNC ("HGNC", "H", "Homo sapiens", "^[A-Za-z0-9]+", "DAPK1" ),
	ENSEMBL ("Ensembl", "En", "", "^ENS[A-Z]*[FPTG]\\d{11}$", "ENSG00000139618"),			//|^[YFW]*$		also accept anything that starts with Y, F, W ??
	Entrez_Gene ("Entrez Gene", "L", "", "^\\d+$", "11234"),
	FlyBase ("FlyBase", "F", "Drosophila melanogaster", "^FB\\w{2}\\d{7}$", "FBgn0011293"),
	KEGG_Genes ("KEGG Genes", "Kg", "", "^\\w+:[\\w\\d\\.-]*$", "syn:ssr3451" ),
	MGI ("MGI", "M", "Mus musculus", "^MGI:\\d+$", "MGI:2442292" ),
	miRBase ("miRBase", "Mbm", "", "MIMAT\\d{7}", "MIMAT0000001" ),
	RGD ("RGD", "R", "Rattus norvegicus", "^\\d{4,7}$", "2018" ),
	SGD ("SGD", "D", "Saccharomyces cerevisiae", "^S\\d+$", "S000028457" ),
	TAIR ("TAIR", "A", "Arabidopsis thaliana", "^AT[1-5]G\\d{5}$", "AT1G01030" ),
	UniGene ("UniGene", "U", "", "[A-Z][a-z][a-z]?\\.\\d+", "Hs.553708" ),
	Uniprot_TrEMBL("Uniprot-TrEMBL", "S", "", "^([A-N,R-Z][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9])|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])(\\.\\d+)?|([A-N,R-Z][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9])$", "P62158"),
	WormBase ("WormBase", "W", "Caenorhabditis elegans", "^WBGene\\d{8}$", "WBGene00000001" ),
	ZFIN ("ZFIN", "Z", "Danio rerio", "ZDB\\-GENE\\-\\d+\\-\\d+", "ZDB-GENE-041118-11");

	private final String descriptor;
	private final String system;
	private final Species species;
	private final Pattern pattern;
	private final String example;
	
	//----------------------------------------------------------------------------
	MappingSource(String s, String sy, String spec, String pat, String sample)
	{
		descriptor = s;
		system = sy;
		species = Species.lookup(spec);
		pattern = Pattern.compile(pat);
		example = sample;
	}
	//----------------------------------------------------------------------------
	public String descriptor()	{ return descriptor;	}
	public String system()		{ return system;	}
	public String species()		{ return species.common();	}
	public Pattern pattern()	{ return pattern;	}
	//----------------------------------------------------------------------------
	public static MappingSource systemLookup(String sys)
	{
		for (MappingSource src : MappingSource.values())
			if (src.system.equals(sys)) 
				return src;
		return null;		
	}

	public static MappingSource nameLookup(String str)
	{
		if (str != null)
			for (MappingSource src : values())
				if (str.contains(src.descriptor) || str.contains(src.getMenuString())) 
					return src;
//		System.out.println("couldn't match: " + str);
		return null;		
	}

	public boolean matchSpecies(Species inSpecies)	
	{
		return inSpecies == null || inSpecies.match(species);	
	}
	//----------------------------------------------------------------------------
//	public static String[] allStrings() {
//		int n = values().length;
//		int i = 0;
//		String[] strs = new String[n];
//		for (MappingSource src : values())
//			strs[i++] = src.getMenuString();
//		return strs;
//	}
//	public static String[] allStringsExcept(String exclude) {
//		int n = values().length;
//		int i = 0;
//		String[] strs = new String[n];
//		for (MappingSource src : values())
//			if (!src.descriptor.equals(exclude))
//				strs[i++] = src.getMenuString();
//		return strs;
//	}
	
	// get the list of sources that are available for this species
	public static List<String> filteredStrings(Species inSpecies, MappingSource inSource) {
		int n = values().length;
		List<String> matchingSources = new ArrayList<String>();
		String srcName = inSource == null ? "" : inSource.name();
		for (MappingSource src : values())
		{
			if (src == inSource) continue;
			if (src.matchSpecies(inSpecies))
			{
				if (!src.name().equals(srcName))
					matchingSources.add(src.getMenuString());
			}
		}
		if (VERBOSE) System.out.println(inSpecies.common() + " " + srcName);
		if (inSpecies==Species.Yeast )
			matchingSources.add(MappingSource.ENSEMBL.getMenuString());
			
	return matchingSources;	
	}
	
	public String getMenuString() {
		String ex = ((example.trim().length() > 0) ? " (e.g., " + example + ")" : "");
		return descriptor + ex;
	}
	//----------------------------------------------------------------------------
	private boolean patternMatch(String id) 
	{		
		if (pattern == null)				return false;
		if (pattern.matcher(id) == null)	return false;
		return pattern.matcher(id).matches();	
	}

	private static boolean VERBOSE = false;

	public static MappingSource guessSource(Species inSpecies, List<String> names) {

		Map<MappingSource, Integer> counter = new HashMap<MappingSource, Integer>();
		for (MappingSource src : values())
			counter.put(src, 0);
//		for (MappingSource src : values())
//			System.out.println(src.descriptor +  " matches " + counter.get(src));
	
		int sampleSize = Math.min(names.size(),  10);		// don't look at more than 10 lines to guess
		for (int i=0; i<sampleSize; i++)
		{
			String id = names.get(i);
			for (MappingSource src : values())
			{
				if (src.matchSpecies(inSpecies) && src.patternMatch(id))
					counter.put(src, counter.get(src)+1);
			}
			int ensemblCt = counter.get(MappingSource.ENSEMBL) + 1;
			if ((inSpecies == Species.Yeast && id.startsWith("Y")) ||
					(inSpecies == Species.Fruit_fly && id.startsWith("F")) ||
						(inSpecies == Species.Worm && id.startsWith("W")))
				counter.put(MappingSource.ENSEMBL, ensemblCt);
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

}


