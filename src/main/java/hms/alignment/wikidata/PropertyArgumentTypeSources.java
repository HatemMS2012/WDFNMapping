package hms.alignment.wikidata;

import java.util.HashSet;
import java.util.Set;

public class PropertyArgumentTypeSources {

	

	public static final String SRC_PROP_DESC = "SRC-PROP-DESC";
	public static final String SRC_PROP_LABEL = "SRC-PROP-LABEL";
	public static final String EQUI_PROP = "EQUI-PROP";
	public static final String SEE_ALSO = "SEE-ALSO";
	public static final String SUB_PROP = "SUB-PROP";
	public static final String INVS_PROP = "INVS-PROP";

	public static final String SUB_ITEM = "SUB-ITEM";
	public static final String INST_FACET = "INST-FACET";
	public static final String REALIZATION = "REAL"; 
	
	public static Set<String> allResourceNames = new HashSet<String>();
	
	static {
		allResourceNames.add(SRC_PROP_DESC);
		allResourceNames.add(SRC_PROP_LABEL);
		allResourceNames.add(EQUI_PROP);
		allResourceNames.add(REALIZATION);
		allResourceNames.add("EQUI-PROP2"); //TODO remove it

		allResourceNames.add(SEE_ALSO);
		allResourceNames.add(SUB_PROP);
		allResourceNames.add(INVS_PROP);
		allResourceNames.add(SUB_ITEM);
		allResourceNames.add(INST_FACET);	
	}
	
}
