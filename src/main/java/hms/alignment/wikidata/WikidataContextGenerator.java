package hms.alignment.wikidata;

import hms.alignment.framenet.FrameNetContextGenerator;
import hms.similarity.TextSimilarityMethod;
import hms.similarity.TextSimilarityUtil;
import hms.wikidata.dbimport.JacksonDBAPI;
import hms.wikidata.model.StructuralPropertyMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

public class WikidataContextGenerator {

	
	/**
	 * Generate context for a given wikidata entity. The context consist of a collection found in the label, description alises of the item
	 * and that of other entities that connect to the source entity via a structural attribute
	 * @param propId
	 * @param applyStemming
	 * @return
	 */
	
	public static List<String> generateContext(String propId, boolean applyStemming){
		
		List<String> finalTokes = new ArrayList<String>();
		
		String propLabel = JacksonDBAPI.getItemLabel(propId, "en");
		
		String propDesc = JacksonDBAPI.getItemDescription(propId, "en");
		
		List<String> propAlisases = JacksonDBAPI.getItemAliases(propId, "en");
		
		Set<String> claims = new HashSet<String>(JacksonDBAPI.getEntityClaimsIds(propId));
		
		

//		System.out.println("Main Property Metadata");
//		System.out.println(propLabel);
//		System.out.println(propDesc);
//		System.out.println(propAlisases);
		
		for(String alias: propAlisases){
			finalTokes.addAll(TextSimilarityUtil.tokenize(alias,applyStemming));

		}
		if(propLabel!=null){
			finalTokes.addAll(TextSimilarityUtil.tokenize(propLabel,applyStemming));
		}
		if(propDesc!=null){
			finalTokes.addAll(TextSimilarityUtil.tokenize(propDesc,applyStemming));
		}
		
//		System.out.println("Main Property Claims");

		for(String claim : claims){
			
			if(StructuralPropertyMapper.structuarlPropertiesMap.keySet().contains(claim)){
			
				String claimLabel = JacksonDBAPI.getItemLabel(claim, "en");
				
				String claimDesc = JacksonDBAPI.getItemDescription(claim, "en");
				
				List<String> claimAlisases = JacksonDBAPI.getItemAliases(claim, "en");
				
				List<String> claimTargets = JacksonDBAPI.getClaimRange(propId, claim);
				
				for(String claimTarget : claimTargets){
				
					
					String claimTargetLabel = JacksonDBAPI.getItemLabel(claimTarget, "en");		
					List<String> claimTargetAlisases = JacksonDBAPI.getItemAliases(claimTarget, "en");
					String claimTargetDesc = JacksonDBAPI.getItemDescription(claimTarget, "en");
					
					
//					System.out.println(claimLabel + ": " + claimTargetLabel + " , "  + claimTargetDesc + " , " + claimTargetAlisases);
					if(claimTargetLabel!=null){
						finalTokes.addAll(TextSimilarityUtil.tokenize(claimTargetLabel,applyStemming));
					}
					if(claimTargetDesc!=null){
						finalTokes.addAll(TextSimilarityUtil.tokenize(claimTargetDesc,applyStemming));
						
					}
				
					for(String alias: claimTargetAlisases){
						finalTokes.addAll(TextSimilarityUtil.tokenize(alias,applyStemming));

					}
				}
				
				
				
				//System.out.println(claimDesc);
				//System.out.println(claimAlisases);
		

			}
		}
		
		
		
//		System.out.println("final tokens");
//		System.out.println(finalTokes);
		
		return finalTokes;
	}
	
	/**
	 * Generate a vector representation from a collection of terms. The vector is map where the keys are
	 * tokes and the values the number of times appear in the token list
	 * @param tokenList
	 * @return
	 */
	public static Map<String, Integer> generateVectorRepresentation(List<String> tokenList){
		
		Map<String, Integer> tokenCountMap = new HashMap<String, Integer>();
		
		
		
		
		for(String token:tokenList){
			
			
			Integer count = tokenCountMap.get(token);
			if(count == null){
				tokenCountMap.put(token, 1);
			}
			else{
				tokenCountMap.put(token,count+1);
			}
			
		}
		
		return tokenCountMap;
		
	}
	
	
	public static void main(String[] args) {
		String p1 = "P69" ;
		boolean useStemming = true;
		
		List<String> tokenList1 = generateContext(p1,useStemming);

		Map<String, Integer> tokenVector1 = generateVectorRepresentation(tokenList1);
		
		List<String> result = FrameNetContextGenerator.generateContext("FN_SemanticPredicate_600", true);
		tokenVector1  = WikidataContextGenerator.generateVectorRepresentation(result);
	
		
		
		for(String term : tokenVector1.keySet()){
			System.out.println(term + "\t" + tokenVector1.get(term));
		}

		System.out.println(tokenVector1);
		
	

	}
}
