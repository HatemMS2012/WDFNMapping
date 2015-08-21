package hms.alignment;

import hms.alignment.framenet.FrameNetAPI;
import hms.similarity.ValueComparator;
import hms.wikidata.dbimport.JacksonDBAPI;
import hms.wikidata.model.PropertyOfficialCategory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class provides different for identifying FN-WD candidate pairs (based on
 * literature review) we identified several methods.
 * 
 * @author mousselly
 *
 */
public class FNWDCandiateExtractor {

	private static final String EN = "en";

	/**
	 * Get frames that have lexical units matching the label of the input WD
	 * property
	 * 
	 * @param propId
	 *            Property ID
	 * @return
	 */
	public static Map<String, Double> getCandidateByWDLabelAndLUs(String propId) {

		Map<String, Double> candidateFrames = new HashMap<String, Double>();

		String propertyLabel = JacksonDBAPI.getItemLabel(propId, EN);

		// Query framenet for frames with a lexical units matching the label of
		// WD property

		List<String> results = FrameNetAPI.getFramesByLU(propertyLabel);

		for (String frameId : results) {

			Double frameIdCount = candidateFrames.get(frameId);
			if (frameIdCount != null) {
				candidateFrames.put(frameId, frameIdCount + 1);

			} else {
				candidateFrames.put(frameId, 1.0);
			}
		}

		// Sort

		ValueComparator bvc = new ValueComparator(candidateFrames);
		TreeMap<String, Double> sorted_map = new TreeMap<String, Double>(bvc);
		sorted_map.putAll(candidateFrames);

		return sorted_map;

	}

	/**
	 * get frames that have lexical units matching the aliases of the input WD
	 * property The candidates are ranked according to the number of lexical
	 * units correspondences between the frame and the set of aliases
	 * 
	 * @param propId
	 * @return
	 */
	public static Map<String, Double> getCandidateByWDAliasesAndLUs(String propId) {

		Map<String, Double> candidateFrames = new HashMap<String, Double>();

		List<String> aliasList = JacksonDBAPI.getItemAliases(propId, EN);
		// System.out.println(aliasList);

		if (aliasList.size() > 0) {

			for (String alias : aliasList) {
				// Query framenet for frames with a lexical units matching the
				// label of WD property

				List<String> results = FrameNetAPI.getFramesByLU(alias);

				for (String frameId : results) {

					Double frameIdCount = candidateFrames.get(frameId);
					if (frameIdCount != null) {
						candidateFrames.put(frameId, frameIdCount + 1);

					} else {
						candidateFrames.put(frameId, 1.0);
					}
				}

			}

		}

		// Sort
		ValueComparator bvc = new ValueComparator(candidateFrames);
		TreeMap<String, Double> sorted_map = new TreeMap<String, Double>(bvc);
		sorted_map.putAll(candidateFrames);

		return sorted_map;

	}
	
	/**
	 * Get frames containing the label of the given property
	 * @param propId
	 * @return
	 */
	public static Map<String, Double> getCandidateByWDAliasesAndFrameLabel(String propId) {
		Map<String, Double> candidateFrames = new HashMap<String, Double>();
	
		List<String> aliasList = JacksonDBAPI.getItemAliases(propId, EN);

		for(String alias : aliasList){
			List<String> frameIdList = FrameNetAPI.getFrameByLabel(alias, true);
			
			for (String frameId : frameIdList) {

				Double frameIdCount = candidateFrames.get(frameId);
				if (frameIdCount != null) {
					candidateFrames.put(frameId, frameIdCount + 1);

				} else {
					candidateFrames.put(frameId, 1.0);
				}
			}
		}
		
		return candidateFrames;
	}

	/**
	 * Get frames containing the label of the given property
	 * @param propId
	 * @return
	 */
	public static Map<String, Double> getCandidateByWDLabelAndFrameLabel(String propId) {
		
		Map<String, Double> candidateFrames = new HashMap<String, Double>();
		
		String propertyLabel = JacksonDBAPI.getItemLabel(propId, EN);

		List<String> frameIdList = FrameNetAPI.getFrameByLabel(propertyLabel, true);
		
		
		for (String frameId : frameIdList) {

			Double frameIdCount = candidateFrames.get(frameId);
			if (frameIdCount != null) {
				candidateFrames.put(frameId, frameIdCount + 1);

			} else {
				candidateFrames.put(frameId, 1.0);
			}
		}
		
		return candidateFrames;
	}
	
	/**
	 * Extract candidate frames for properties of a given category and store the results in a file
	 * @param outputDir
	 * @param type
	 */
	public static void getCadidateFrames(String outputDir,
			PropertyOfficialCategory type) {

		String fileName = outputDir + "candidates2_" + type + ".txt";

		List<String> propIdList = JacksonDBAPI.getOfficialProperties(type);

		System.out.println("Dealing the category: " + type);

		try {

			PrintWriter resultWriter = new PrintWriter(new File(fileName));
			resultWriter.print("Property ID \t Property Label \t Frame ID \t Frame Label \t Score \t Matching Source");

			for (String propId : propIdList) {

				String propertyLabel = JacksonDBAPI.getItemLabel(propId, EN);

				Map<String, Double> matchingFramesByWDLabel = getCandidateByWDLabelAndLUs(propId);

			

				for (Entry<String, Double> e : matchingFramesByWDLabel
						.entrySet()) {

					resultWriter.println(propId + "\t" + propertyLabel + "\t"
							+ e.getKey() + "\t"
							+ FrameNetAPI.getFrameLabel(e.getKey()) + "\t"
							+ e.getValue() + "\t label-lus");
				}
				
				Map<String, Double> matchingFramesByWDLabelAndFLabel = getCandidateByWDLabelAndFrameLabel(propId);

				

				for (Entry<String, Double> e : matchingFramesByWDLabelAndFLabel
						.entrySet()) {

					resultWriter.println(propId + "\t" + propertyLabel + "\t"
							+ e.getKey() + "\t"
							+ FrameNetAPI.getFrameLabel(e.getKey()) + "\t"
							+ e.getValue() + "\t label-FrameLabel");
				}
				

				Map<String, Double> matchingFramesByAliases = getCandidateByWDAliasesAndLUs(propId);

				for (Entry<String, Double> e : matchingFramesByAliases
						.entrySet()) {

					resultWriter.println(propId + "\t" + propertyLabel + "\t"
							+ e.getKey() + "\t"
							+ FrameNetAPI.getFrameLabel(e.getKey()) + "\t"
							+ e.getValue() + "\t ALIAS-lus");

				}
				
				
				Map<String, Double> matchingFramesByAliasesAndFLabel = getCandidateByWDAliasesAndFrameLabel(propId);

				for (Entry<String, Double> e : matchingFramesByAliasesAndFLabel
						.entrySet()) {

					resultWriter.println(propId + "\t" + propertyLabel + "\t"
							+ e.getKey() + "\t"
							+ FrameNetAPI.getFrameLabel(e.getKey()) + "\t"
							+ e.getValue() + "\t ALIAS-FrameLabel");

					
				}

			}
			resultWriter.close();

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

	}

	
	public static Map<String, Double> getCandidateAll(String propId){
		
		Map<String, Double> finalResult = new HashMap<String, Double>();
		Map<String, Double> res1 = getCandidateByWDAliasesAndFrameLabel(propId);
		
		updateCandidateList(finalResult, res1);

		
		Map<String, Double> res2 = getCandidateByWDAliasesAndLUs(propId);

		updateCandidateList(finalResult, res2);
		
		
		Map<String, Double> res3 = getCandidateByWDLabelAndFrameLabel(propId);
		updateCandidateList(finalResult, res3);
		
		Map<String, Double> res4 = getCandidateByWDLabelAndLUs(propId);
		updateCandidateList(finalResult, res4);
		
		
		//Sort the map
		ValueComparator bvc =  new ValueComparator(finalResult);

		//Sort
		TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
		sorted_map.putAll(finalResult);
		
		
		return sorted_map;
	}

	private static void updateCandidateList(Map<String, Double> finalResult,
			Map<String, Double> res2) {
		for(Entry<String, Double> e : res2.entrySet()){
			
			Double count = finalResult.get(e.getKey());
			
			if(count !=null){
				
				finalResult.put(e.getKey(),finalResult.get(e.getKey())+1);
			}
			else{
				finalResult.put(e.getKey(),e.getValue());
			}
		}
	}
	public static void main(String[] args) {
		
		
		
		String propId = "P710";
		Map<String, Double>  res = getCandidateByWDAliasesAndFrameLabel(propId);
		System.out.println(res);
		Map<String, Double> res3 = getCandidateByWDAliasesAndLUs(propId);
		System.out.println(res3);
		Map<String, Double> res4 = getCandidateByWDLabelAndFrameLabel(propId);
		System.out.println(res4);
		Map<String, Double> res5 = getCandidateByWDLabelAndLUs(propId);
		System.out.println(res5);
		
		
		System.out.println("all");
		System.out.println(getCandidateAll(propId));
		
//		String outputDir = "output/LabelAlias/";
//
//		for (PropertyOfficialCategory type : PropertyOfficialCategory.values()) {
//
//			System.out.println("Dealing the category: " + type);
//			getCadidateFrames(outputDir, type);
//
//		}
	}

}
