package hms.alignment;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.netlib.util.doubleW;

import com.google.common.collect.Sets;

import hms.alignment.data.Frame;
import hms.alignment.data.SemanticRole;
import hms.alignment.framenet.FrameNetAPI;
import hms.alignment.wikidata.WDArgumentMetadataExtractor;
import hms.alignment.wikidata.WikidataContextGenerator;
import hms.alignment.wikidata.WikidataStopWords;
import hms.similarity.TermSimilarityMethod;
import hms.similarity.TextSimilarityMethod;
import hms.similarity.TextSimilarityUtil;
import hms.similarity.ValueComparator;
import hms.similarity.WordNetSimilarityMethod;
import hms.wikidata.dbimport.JacksonDBAPI;
import hms.wikidata.graph.WikidataTraverser;
import hms.wikidata.model.StructuralPropertyMapper;

public class MetaDataExtractor {

	

	private static final double WN_SIM_THRESHOLD = 0;
	private static final int MAX_HYPERNYM_LENGTH = 5;
	private static Set<String> labelList = new HashSet<String>();
	
	

	
	public static void main(String[] args) {

		
//		System.out.println(getLabelsSimilarity("killer", "person"));
//		System.out.println(getLabelsSimilarity("killer", "a big killer"));
//		
//		List<String> test = new ArrayList<String>();
//		
//		test.add("killer");
//		test.add("big killer");
//		test.add("car");
//		
//		System.out.println(getLabelsSimilarity("killer", test));
//		
		
//		String frameId = "FN_SemanticPredicate_479"; //"FN_SemanticPredicate_281" ; P1534
//		String propId =  "P69" ; //"P1478" ;   P1534
//		String lang = "en" ;
//		String propLabel =  JacksonDBAPI.getItemLabel(propId, lang);
//		System.out.println("Property: " + propLabel);
//		
//		
//		for (int i = 0; i <5; i++) {
//			
//			String instanceId = WikidataTraverser.getRandomInstance(propId, lang);
//			mapInstance(propId, instanceId, frameId,true);
//			System.out.println(" .............. ");
//			
////			mapInstance(propId, instanceId, frameId,false);
////
////			System.out.println(" ........2222222222222..... ");
//		}
////		
		WDArgumentMetadataExtractor.getEntityClasses("Q183", 1, "en", StructuralPropertyMapper.structuarlItemRelatedPropertiesMap.keySet());
		for(String clazz : labelList){
			
			String id = clazz.split(":")[0];
			System.out.println(clazz + "\t" + JacksonDBAPI.getItemDescription(id, "en") + "\t" + JacksonDBAPI.getItemAliases(id, "en"));
		}
				
		
	}
	
	
	
	
	public static void mapInstance(String propId,String propInstanceId, String frameId,boolean defDescCheck){

		
		List<String> instanceRanges = JacksonDBAPI.getClaimRange(propInstanceId, propId);
		
		Frame frame = FrameNetAPI.getFrameFullData(frameId);
		
		List<SemanticRole> sematicRoles = frame.getRoles();

		String instanceLable = JacksonDBAPI.getItemLabel(propInstanceId, "en");
		
		
		Map<String, Double> instanceRole = identifyRoleNew(null,propInstanceId, sematicRoles,defDescCheck);
		
		System.out.println("Source: " + instanceLable  + ": " + instanceRole);
		
		Map<String, Double> instanceRangeRoleMap = new HashMap<String, Double>();
 		for(String instanceRang: instanceRanges){
			
			instanceRangeRoleMap = identifyRoleNew(propId,instanceRang, sematicRoles,defDescCheck);
		
			String rangeLable = JacksonDBAPI.getItemLabel(instanceRang, "en");

			System.out.println("Target: " + rangeLable + ":" + instanceRangeRoleMap);
			
		}
		
		
			
		
	}


	public static void mapStructure(String propId, String frameId, boolean defDescCheck){

		
		Frame frame = FrameNetAPI.getFrameFullData(frameId);
		
		List<SemanticRole> sematicRoles = frame.getRoles();

		
		Map<String, Double> instanceRole = identifyRoleNew(null,propId, sematicRoles,defDescCheck);
		
		System.out.println("Source: " + propId  + ": " + instanceRole);
		
		List<String> claims = JacksonDBAPI.getEntityClaimsIds(propId);
		
		Map<String, Double> instanceRangeRoleMap = new HashMap<String, Double>();
		for(String instanceRang: claims){
			
			instanceRangeRoleMap = identifyRoleNew(propId,instanceRang, sematicRoles,defDescCheck);
		
			String rangeLable = JacksonDBAPI.getItemLabel(instanceRang, "en");

			System.out.println("Target: " + rangeLable + ":" + instanceRangeRoleMap);
	}
//			
		}

//	private static Map<String, Double> identifyRole(String propId,String propInstanceId,
//			List<SemanticRole> sematicRoles) {
//		
//		//		String propLable =  JacksonDBAPI.getItemLabel(propId, "en"); //The label of the source property is related to the target of the relation
//		
//		Map<String, Double> instanceRole = new HashMap<String,Double>();
//		
//		ValueComparator bvc =  new ValueComparator(instanceRole);
//	
//		for(SemanticRole sr : sematicRoles){
//			
//			String role = sr.getRole().toLowerCase() ;
//		
//			String roleDef = sr.getDefnition();
//			
//			Set<String> instanceClasses = getEntityHigherClasses(propInstanceId,5, "en", StructuralPropertyMapper.structuarlItemRelatedPropertiesMap.keySet());
//			
//			 	
//			//1. calculate the similarity between the role label and the instance label
//			 
//			 
//		
//			 
//			 //WordNet + Exact match similarity
//			 double  similarity1 = calculateWNStringSimilarity(role, instanceClasses);
//			 
//			 
//			 //Description - Definition similarity
//			 double similarity2 = 0;
//			 String desc = JacksonDBAPI.getItemDescription(propInstanceId, "en");
//			 if(desc!=null){
//				 similarity2 = TextSimilarityUtil.calculateTermSimilarityExactMatch(roleDef, desc, true);	 
//				 similarity2 += TextSimilarityUtil.calculateTermSimilarityExactMatch(role, desc, true);	 
//
//			 }
//			 
//			 //alias similarity
//			 List<String> aliases = JacksonDBAPI.getItemAliases(propInstanceId, "en");
//			 double similarity3 = 0;
//			 if(aliases.size()> 0){
//				 similarity3 = TextSimilarityUtil.calculateTermSimilarityExactMatch(roleDef, aliases.toString(), true);	 
//				 similarity3 += TextSimilarityUtil.calculateTermSimilarityExactMatch(role, aliases.toString(), true);	 
//
//			 }
//			 
//			 double totalSimilarity = similarity1 + similarity2 + similarity3 ;
//			 if(totalSimilarity > 0){
//				 
//				 instanceRole.put(role,totalSimilarity);
//
//			 }
//
//		}
//		
//		//Sort
//		 TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
//		 sorted_map.putAll(instanceRole);
//		 
//		return sorted_map;
//	}

	
	private static Map<String, Double> identifyRoleNew(String propId,String propInstanceId, List<SemanticRole> sematicRoles, boolean defDescCheck) {
	
		
		Map<String, Double> instanceRoleMap = new HashMap<String,Double>();
		
		ValueComparator bvc =  new ValueComparator(instanceRoleMap);
		
		String instanceLabel = JacksonDBAPI.getItemLabel(propInstanceId,"en");
		
		
		List<String> aliases = JacksonDBAPI.getItemAliases(propInstanceId, "en");
		
		String desc = JacksonDBAPI.getItemDescription(propInstanceId, "en");
		
		Set<String> instanceHyperonym = getEntityHigherClasses(propInstanceId,MAX_HYPERNYM_LENGTH, "en", StructuralPropertyMapper.structuarlItemRelatedPropertiesMap.keySet());

		for(SemanticRole sr : sematicRoles){
			
			 double totalSimilarity  = 0 ;
			 
			String semanticRoleLabel = sr.getRole() ;
		
//			System.out.println("Similarity between: " + instanceLabel + " and " + semanticRoleLabel);
			
			//1. calculate the similarity between the role label and the instance label
			
			//double labelSimilarity = getLabelsSimilarity(semanticRoleLabel,instanceLabel);
			double labelSimilarity = 0 ;
			totalSimilarity += labelSimilarity;
			
			//2. label-alias similarity
			double labelAliasSimilarity = 0 ;
			if(aliases.size()>0){
//				labelAliasSimilarity = getLabelsSimilarity(semanticRoleLabel, aliases) ;
				labelAliasSimilarity = TextSimilarityUtil.calculateStemOverlap(semanticRoleLabel, aliases.toString()) ;
				
//				System.out.println(semanticRoleLabel + " - Aliases" + ": " + labelAliasSimilarity ); 
				totalSimilarity += labelAliasSimilarity;
			}
			
			
			//3. label-description similarity
			double labelDescSimilarity = 0 ;
			if(desc !=null){
//				labelDescSimilarity =getLabelsSimilarity(semanticRoleLabel, Arrays.asList(desc.split(" "))) ;
				
				labelDescSimilarity = TextSimilarityUtil.calculateStemOverlap(semanticRoleLabel, desc) ;
//				System.out.println(semanticRoleLabel + " - Description" + ": " + labelDescSimilarity ); 

				totalSimilarity += labelDescSimilarity;

			}
			
			//4. label-Hyperonyms list similarity
//			double labelHyperonymSimilarity = getLabelsSimilarity(semanticRoleLabel, instanceHyperonym) ;
			
			double labelHyperonymSimilarity =  TextSimilarityUtil.calculateStemOverlap(semanticRoleLabel, instanceHyperonym.toString()) ;
			
			
			totalSimilarity += labelHyperonymSimilarity;
			
//			System.out.println(semanticRoleLabel + " - Hyperonyms" + ": " + labelHyperonymSimilarity ); 

			String roleDef = sr.getDefnition();
			
			
			
			
			if(defDescCheck){
				//5. Description - Definition similarity
				
				double defDescSimilarity = 0;
			
				if(desc!=null){
					
					defDescSimilarity += TextSimilarityUtil.calculateStemOverlap(roleDef, desc);
					
//					defDescSimilarity = TextSimilarityUtil.calculateTermSimilarityExactMatch(roleDef, desc, true);
					

										
//					defDescSimilarity += getLabelsSimilarity(Arrays.asList(roleDef.split(" ")), Arrays.asList(desc.split(" ")));	
					
//					defDescSimilarity += getLabelsSimilarityCosine(Arrays.asList(roleDef.split(" ")), Arrays.asList(desc.split(" ")));
//					System.out.println( "Role Definition - Description" + ": " + defDescSimilarity ); 

					totalSimilarity += defDescSimilarity;
				}
							 
				//6. definition-aliases similarity 
				
				double defAliaseSimilarity = 0;
				
				if(aliases.size()>0){
					defAliaseSimilarity += TextSimilarityUtil.calculateStemOverlap(roleDef, aliases.toString());
					
//					defAliaseSimilarity = TextSimilarityUtil.calculateTermSimilarityExactMatch(roleDef, aliases.toString(), true);	

//					defAliaseSimilarity += getLabelsSimilarity(Arrays.asList(roleDef.split(" ")), aliases);	
					
//					defAliaseSimilarity += getLabelsSimilarityCosine(Arrays.asList(roleDef.split(" ")), aliases);	
					
//					System.out.println( "Role Definition - Aliases" + ": " + defAliaseSimilarity ); 

					
					totalSimilarity += defAliaseSimilarity;
				}
				
				//7. definition-hypernoyms similairty
				double defHypernoymSimilarity = 0 ;
				
				
				defHypernoymSimilarity += TextSimilarityUtil.calculateStemOverlap(roleDef, instanceHyperonym.toString());

				
//				defHypernoymSimilarity += TextSimilarityUtil.calculateTermSimilarityExactMatch(roleDef, instanceHyperonym.toString(), true);	

//				defHypernoymSimilarity += getLabelsSimilarity(Arrays.asList(roleDef.split(" ")), instanceHyperonym);
				

//				defHypernoymSimilarity += getLabelsSimilarityCosine(Arrays.asList(roleDef.split(" ")), instanceHyperonym);
				
//				System.out.println( "Role Definition - hypernoyms" + ": " + defHypernoymSimilarity ); 

				totalSimilarity += defHypernoymSimilarity;

	//			double defHypernoymSimilarity = TextSimilarityUtil.calculateTermSimilarityExactMatch(roleDef, instanceHyperonym.toString(),true);

//				System.out.println("--------------------------------------------------");

			}
			
		
			 if(totalSimilarity > 0){
				 
				 instanceRoleMap.put(semanticRoleLabel,totalSimilarity);

			 }

		}
		
		//Sort
		 TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
		 sorted_map.putAll(instanceRoleMap);
		 
		return sorted_map;
	}



	private static double calculateWNStringSimilarity(String role,Set<String> instanceClasses) {
		 
		
		double similarity = 0;
		
		for(String instanceClass : instanceClasses){

				 
			 if(instanceClass.toLowerCase().contains(role)|| role.contains(instanceClass)){
				 similarity =  1;

			 }
			 
			 //WordNet Similarity
			 String[] instanceCompArr = instanceClass.split(" ");
			
			 for(String comp : instanceCompArr){
				
				 double temp = TextSimilarityUtil.calculateWNSimilarity(role, comp,WordNetSimilarityMethod.Path,false);
				
				 if(temp >= 0.25)
				
					 similarity += temp;
			 }
			 
			
			 
		 }
		return similarity;
	}
	
	
	/**
	 * Calculate the similarity between two words. If the two words are identical or one of them contains
	 * the other the method return 1 otherwise the similarity between the two words is determined according to wordnet
	 * @param label1
	 * @param label2
	 * @return
	 */
	public static double getLabelsSimilarity(String label1, String label2){
		
		if(WikidataStopWords.isStopWord(label1) || WikidataStopWords.isStopWord(label2))
			return 0;
		
		
		double similarity = 0;
		
		label1 = label1.toLowerCase() ;
		label2 = label2.toLowerCase() ;
		if(label1.contains(label2)|| label2.contains(label1)){
				 similarity =  1.0/Math.max(label1.split(" ").length, label2.split(" ").length);
		}
		else {
			 
			 //WordNet Similarity
			 String[] instanceCompArr = label2.split(" ");
			
			 for(String comp : instanceCompArr){
				 if(WikidataStopWords.isStopWord(comp)) 
						 continue;
				 double temp = TextSimilarityUtil.calculateWNSimilarity(label1, comp,WordNetSimilarityMethod.Path,false);
				 
				
				 if(temp >= WN_SIM_THRESHOLD)
				
					 similarity += temp;
			 }
		}
			
		return similarity;
		
	}
	
	
	/**
	 * Compare a word to a list of words and return the maximum similarity.
	 * The similarity is calculated according to @see  getLabelsSimilarity(String label1, String label2)
	 * @param label1
	 * @param label2
	 * @return
	 */
	public static double getLabelsSimilarity(String label1, Collection<String> label2){
		
		double maxSimilarity = 0 ;
		
		for(String label : label2){
			
			double sim = getLabelsSimilarity(label1, label);
			if(sim> maxSimilarity){
				maxSimilarity = sim;
			}
		}
//		maxSimilarity /=label2.size();
			
		return maxSimilarity;
	}
	
	public static double getLabelsSimilarity(Collection<String> label1, Collection<String> label2){
		
		double maxSimilarity = 0 ;
		
		for(String label : label1){
			
			double sim = getLabelsSimilarity(label, label2);
		
			if(sim> maxSimilarity){
				maxSimilarity = sim;
			}
			
		}
			
		return maxSimilarity;
	}
	
	public static double getLabelsSimilarityCosine(Collection<String> label1, Collection<String> label2){
	
		
		label1 = TextSimilarityUtil.tokenize(label1.toString(),true);
		label2 = TextSimilarityUtil.tokenize(label2.toString(),true);
		
		Map<String, Integer> label1Map = WikidataContextGenerator.generateVectorRepresentation((List<String>) label1);
		Map<String, Integer> label2Map = WikidataContextGenerator.generateVectorRepresentation((List<String>) label2);
		
		return TextSimilarityUtil.cosine(label1Map, label2Map);

	}
	
	public static double getLabelDescriptionSimilarity(){
		
		return 0;
		
	}

	
	public static Set<String> getEntityHigherClasses(String itemId, int depth, String lang, Set<String> targetProp){
		labelList.clear();
		
		WDArgumentMetadataExtractor.getEntityClasses(itemId, depth, lang, targetProp);
		
		return labelList;
	}
	

	
//	public static void identifyClass(String itemId){
//		
//		
//		String res = WikidataTraverser. geneateTreeJSON(itemId, 5, "en",StructuralPropertyMapper.structuarlItemRelatedPropertiesMap.keySet());
//		System.out.println(res);
//	}
	
}
