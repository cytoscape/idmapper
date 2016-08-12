package org.nrnb.idmapper;

import java.util.HashMap;
import java.util.Map;


// see http://webservice.bridgedb.org/contents for full list supported in BridgeDB
public enum Species {
	   Human("Human", "Homo sapiens") ,
	   Mouse("Mouse", "Mus musculus") ,
	   Rat("Rat", "Rattus norvegicus") ,
	   Frog("Frog", "Xenopus tropicalis") ,
	   Zebra_fish("Zebra fish", "Danio rerio") ,
	   Fruit_fly("Fruit fly", "Drosophila melanogaster") ,
	   Mosquito("Mosquito", "Anopheles gambiae") ,
	   Arabidopsis_thaliana("Arabidopsis thaliana", "Arabidopsis thaliana") ,
	   Yeast("Yeast", "Saccharomyces cerevisiae") ,
	   Escherichia_coli("E. coli", "Escherichia coli") ,
	   Tuberculosis("Tuberculosis", "Mycobacterium tuberculosis") ,
	   Worm("Worm", "Caenorhabditis elegans");
 
	private String name;
	private String latin;

	static Map<String, String> mapToCommon;
	static Map<String, String> mapToLatin;
	//--------------------------------------------------------------------
	
	private Species(String commonName, String latinName)
	{
		name = commonName;
		latin = latinName;
	}
	
	public static void buildMaps()
	{
		if (mapToCommon == null)
		{
			mapToCommon = new HashMap<String, String>();
			mapToLatin = new HashMap<String, String>();
			for (Species spec : values())
			{
				mapToCommon.put(spec.latin, spec.name);
				mapToLatin.put(spec.name, spec.latin);
			}
		}
	}
	
	String common()		{ return name;		}
	String latin()		{ return latin;		}
	//--------------------------------------------------------------------
	public static Species lookup(String input)
	{
		int idx = input.indexOf(" (");
		if (idx > 0) input = input.substring(0,idx);
		for (Species s : values())
			if (s.name.equals(input) || s.latin.equals(input))
				return s;
		return null;
	}
	//--------------------------------------------------------------------
	public static String[] commonNames()
	{
		String[] names = new String[Species.values().length];
		int i = 0;
		for (Species spec : values())
			names[i++] = spec.name;
		return names;		
	}
	public static String[] latinNames()
	{
		String[] latinnames = new String[Species.values().length];
		int i = 0;
		for (Species spec : values())
			latinnames[i++] = spec.latin;
		return latinnames;		
	}
	public static String[] fullNames()
	{
		String[] fullNames = new String[Species.values().length];
		int i = 0;
		for (Species spec : values())
			fullNames[i++] = spec.name + " (" + spec.latin + ")";
		return fullNames;		
	}
}
