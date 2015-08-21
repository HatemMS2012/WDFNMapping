package hms.alignment.wikidata;

import hms.SentenceTriple;
import hms.SentenceTriplizer;
import hms.StanfordNLPTools;
import hms.dbpedia.KnowledgeBaseName;
import hms.dbpedia.KnowledgeBaseTypesProvider;
import hms.sentence.triplization.Argument;
import hms.sentence.triplization.MergedSentenceTriple;
import hms.sentence.triplization.MultipleTripleExtractor;
import hms.sentence.triplization.SyntaxTreeUtil;
import hms.sentence.triplization.TripleExtractor;
import hms.sentence.triplization.TripleExtractorStandard;
import hms.similarity.TextSimilarityUtil;
import hms.similarity.ValueComparator;
import hms.wikidata.dbimport.JacksonDBAPI;
import hms.wikidata.model.ClaimRealization;
import hms.wikidata.model.ExperimentalArgTypes;
import hms.wikidata.model.PropertyOfficialCategory;
import hms.wikidata.model.StructuralPropertyMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.Sets;

/**
 * Extract semantic types for the arguments of a given property based on the 
 * structural relationships of the sources property to other properties.
 * TODO: check correct assignments
 * @author mousselly
 *
 */
public class PropertyArgumentTypeExtractor {

	
	private static final String ARG1 = "ARG1";
	private static final String ARG2 = "ARG2";

	private static final String SCHEMA_ORG_ONTOLOGY_PREFIX = "http://schema.org/";
	private static final String DBPEDIA_ONTOLOGY_PREFIX = "http://dbpedia.org/ontology/";
	private final String lang = "en";

	
	/**
	 * Extract ARG1 type directly from "subject item of this property"
	 * @param propId
	 * @return
	 */
	public Collection<String> extractTypeARG2(String propId){
		
		Set<String> types = new HashSet<String>();
		
		List<String> subjectItemIdList = JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.subjectItem);

		for(String subjectItemId : subjectItemIdList){
			
			String label = JacksonDBAPI.getItemLabel(subjectItemId, lang);
			
//			types.addAll(JacksonDBAPI.getItemAliases(subjectItemId,lang));
			
			if(label!=null){
				types.add(label);
			}
		}
		
		return types;
	}
	
	/**
	 * Extract ARG2 type from instance of -> fact of
	 * @param propId
	 * @return
	 */
	public Collection<String> extractTypeARG1(String propId){
		
		Set<String> types = new HashSet<String>();
		
		List<String> instanceOfList = JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.instanceOf);

		for(String instanceOfId : instanceOfList){
			
			List<String> facetOfIdList = JacksonDBAPI.getClaimRange(instanceOfId,StructuralPropertyMapper.facetOf);
			
			for(String facetId : facetOfIdList){
				
				
				String label = JacksonDBAPI.getItemLabel(facetId, lang);

//				types.addAll(JacksonDBAPI.getItemAliases(facetId,lang));
				
				if(label!=null){
					types.add(label);
				}
				
			}
		}
		
		return types;
		
	}
	
	
	/**
	 *  Infer the semantic types of property arguments (ARG1, ARG1) from inverse properties "inverse of"
	 * @param propId
	 * @return
	 */
	public Map<String, Collection<String>> extractArgumentTypesFromInverseProperty(String propId){
		
		Map<String, Collection<String>> argTypeMap = new HashMap<String, Collection<String>>();
	
		Collection<String> typeArg1 = new HashSet<String>() ;
		Collection<String> typeArg2 = new HashSet<String>();
		
		List<String> inversePropIdList = JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.inverseOf);

		for(String inversePropId: inversePropIdList){
			
			typeArg1.addAll(extractTypeARG2(inversePropId));
			
			typeArg2.addAll(extractTypeARG1(inversePropId));
			
		}
		argTypeMap.put(ARG1, typeArg1);
		argTypeMap.put(ARG2, typeArg2);
		
		return argTypeMap;
	}
	

	/**
	 * Infer the semantic types of property arguments (ARG1, ARG1) from the father property "subproperty of"
	 * @return
	 */
	public Map<String, Collection<String>> extractArgumentTypesFromSeeAlsoProperty(String propId){
		
		Map<String, Collection<String>> argTypeMap = new HashMap<String, Collection<String>>();
	
		Collection<String> typeArg1 = new HashSet<String>() ;
		Collection<String> typeArg2 = new HashSet<String>();
		
		List<String> seeAlsoIdList = JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.seeAlso);

		for(String seeAlsoId: seeAlsoIdList){
			
			typeArg1.addAll(extractTypeARG1(seeAlsoId));
			
			typeArg2.addAll(extractTypeARG2(seeAlsoId));
			
		}
		argTypeMap.put(ARG1, typeArg1);
		argTypeMap.put(ARG2, typeArg2);
		
		return argTypeMap;
	}
	
	/**
	 * Infer the semantic types of property arguments from related properties via "see also" property
	 * @param propId
	 * @return
	 */
	public Map<String, Collection<String>> extractArgumentTypesFromSubProperty(String propId){
		
		Map<String, Collection<String>> argTypeMap = new HashMap<String, Collection<String>>();
		Collection<String> typeArg1 = new HashSet<String>() ;
		Collection<String> typeArg2 = new HashSet<String>();
		List<String> subpropIdList = JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.subPropertyOf);

		for(String subPropId: subpropIdList){
			
			typeArg1.addAll(extractTypeARG1(subPropId));
			
			typeArg2.addAll(extractTypeARG2(subPropId));
			
		}
		argTypeMap.put(ARG1, typeArg1);
		argTypeMap.put(ARG2, typeArg2);
		
		return argTypeMap;
	}
	
	/**
	 * Infer the semantic types of property arguments (ARG1, ARG1) from the equivalent property "equivalent property"
	 * @return
	 */
	public Map<String, Collection<String>> extractArgumentTypesFromEquivalentProperty(String propId){
		
		Map<String, Collection<String>> argTypeMap = new HashMap<String, Collection<String>>();
	
		Collection<String> typeArg1 = new HashSet<String>() ;
		Collection<String> typeArg2 = new HashSet<String>();
		
		List<String> equivalentPropertyIdList = JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.equivalentProperty);

		for(String equivalentPropId: equivalentPropertyIdList){
			String domainRange = null;
			
			if(equivalentPropId.startsWith(DBPEDIA_ONTOLOGY_PREFIX)){
				
				domainRange = KnowledgeBaseTypesProvider.getProeprtyDomainAndRange(equivalentPropId,KnowledgeBaseName.DBpedia);

			}
			else if(equivalentPropId.startsWith(SCHEMA_ORG_ONTOLOGY_PREFIX)){
				domainRange = KnowledgeBaseTypesProvider.getProeprtyDomainAndRange(equivalentPropId.replace(SCHEMA_ORG_ONTOLOGY_PREFIX, ""),KnowledgeBaseName.SchemaOrg);
			}
			
			if(domainRange!=null){
				typeArg1.add(domainRange.split(KnowledgeBaseTypesProvider.DOMAIN_RANGE_SEP)[0]);
				typeArg2.add(domainRange.split(KnowledgeBaseTypesProvider.DOMAIN_RANGE_SEP)[1]);
			}
			
			
			
		}
		argTypeMap.put(ARG1, typeArg1);
		argTypeMap.put(ARG2, typeArg2);
		
		return argTypeMap;
	}
	
	
	/**
	 * Extract argument types from the property label itself
	 * In cases where the property ends with "of" link in the case of "student of" reverse 
	 * the identified argument types.
	 * @param propId
	 * @return
	 */
	public  Map<String, Collection<String>> extractArgumentTypesFromPropertyLabel(String propId){
		
		Map<String, Collection<String>> argTypeMap = new HashMap<String, Collection<String>>();		
		
		String label = JacksonDBAPI.getItemLabel(propId, lang);
		
		if(label!=null){
			Map<String, Collection<String>> temp = getArgumentTypesFromSentence(label,true);
			
			Collection<String> arg1Types = temp.get(ARG1);

			Collection<String> arg1TypesCurrent = argTypeMap.get(ARG1);
			
			if(arg1TypesCurrent!=null){
				
				arg1TypesCurrent.addAll(arg1Types);
				argTypeMap.put(ARG1, arg1TypesCurrent);
			}
			else{
				argTypeMap.put(ARG1, arg1Types);
			}
			
			Collection<String> arg2Types = temp.get(ARG2);
			
			Collection<String> arg2TypesCurrent = argTypeMap.get(ARG2);
		
			if(arg2TypesCurrent!=null){
				
				arg2TypesCurrent.addAll(arg2Types);
				argTypeMap.put(ARG2, arg2TypesCurrent);

				
			}
			else{
				argTypeMap.put(ARG2, arg2Types);
			}
			
			//If the property end with of "e.g. student of" reverse the roles
			
			if(label.endsWith(" of")){
				Collection<String> arg1 = argTypeMap.get(ARG1);
				argTypeMap.put(ARG1, argTypeMap.get(ARG2));
				argTypeMap.put(ARG2, arg1);

				
			}
		}
		
		return argTypeMap;
	}
	
	/**
	 * 
	 * @param propId
	 * @return
	 */
	public  Map<String, Collection<String>> extractArgumentTypesFromPropertyAliases(String propId){
		
		Map<String, Collection<String>> argTypeMap = new HashMap<String, Collection<String>>();		

		
		List<String> aliases = JacksonDBAPI.getItemAliases(propId, lang); 
		
		for(String alias : aliases){
			
			Map<String, Collection<String>> temp = getArgumentTypesFromSentence(alias,true);
			
			Collection<String> arg1Types = temp.get(ARG1);

			Collection<String> arg1TypesCurrent = argTypeMap.get(ARG1);
			if(arg1TypesCurrent!=null){
				
				arg1TypesCurrent.addAll(arg1Types);
				argTypeMap.put(ARG1, arg1TypesCurrent);
			}
			else{
				argTypeMap.put(ARG1, arg1Types);
			}
			
			Collection<String> arg2Types = temp.get(ARG2);
			
			Collection<String> arg2TypesCurrent = argTypeMap.get(ARG2);
			if(arg2TypesCurrent!=null){
				
				arg2TypesCurrent.addAll(arg2Types);
				argTypeMap.put(ARG2, arg2TypesCurrent);

				
			}
			else{
				argTypeMap.put(ARG2, arg2Types);
			}
			

		
		}
		
		
		return argTypeMap;
	}
	
	

//	public  Map<String, Collection<String>> extractArgumentTypesFromPropertyLabel1(String propId){
//		
//		Map<String, Collection<String>> argTypeMap = new HashMap<String, Collection<String>>();		
//		
//		String label = JacksonDBAPI.getItemLabel(propId, lang);
//		
//		if(label!=null){
//			String sentence = WDDescriptionArgumentTypeExtractor.extractMainSentenceFromDescription(label);
//			
//			Map<Integer, SentenceTriple> tripleList = SentenceTriplizer.extractTriples(sentence);
//			
//			if(tripleList.size() > 0){
//				
//				String subject = tripleList.get(0).getSubject();
//				String object = tripleList.get(0).getObject();
//				
//				List<String> typeArg1 = null;
//				List<String> typeArg2 = null;
//				if(subject!=null){
//					typeArg1 = WDDescriptionArgumentTypeExtractor.getWordPosPairs(subject, WDDescriptionArgumentTypeExtractor.filterPos);
//				}
//				
//				if(object!=null){
//					typeArg2 = WDDescriptionArgumentTypeExtractor.getWordPosPairs(object, WDDescriptionArgumentTypeExtractor.filterPos);
//				}
//				
//				argTypeMap.put("ARG1", typeArg2);
//				argTypeMap.put("ARG2", typeArg1);
//				
//			}
//		}
//		
//		return argTypeMap;
//	}
	/**
	 * Apply triple extraction on the description of a property and use the subject/object as types for the 
	 * arguments of the property
	 * @param propId
	 * @return
	 */
	public  Map<String, Collection<String>> extractArgumentTypesFromPropertyDescription(String propId){

		
		Map<String, Collection<String>> argTypeMap = new HashMap<String, Collection<String>>();		
		
		String desc = JacksonDBAPI.getItemDescription(propId, lang);
		
		if(desc!=null){
		
			argTypeMap = getArgumentTypesFromSentence(desc,false);

		}
		
		return argTypeMap;
			
	}

	public Map<String, Collection<String>>  getArgumentTypesFromSentence(String desc, boolean labelOnly) {
		
		Map<String, Collection<String>> argTypeMap = new HashMap<String, Collection<String>>();
		String sentence = WDDescriptionArgumentTypeExtractor.extractMainSentenceFromDescription(desc);

		TripleExtractor tripleExtractor = new TripleExtractorStandard();

		MultipleTripleExtractor multiTripleExtractor = new MultipleTripleExtractor(
				tripleExtractor);

		MergedSentenceTriple finalTriple = multiTripleExtractor.extractMergedTriple(sentence);
 
		if (finalTriple != null) {

			List<List<Argument>> subjectTypes = finalTriple.getSubject();
			List<List<Argument>> objectTypes = finalTriple.getObject();

			List<String> arg1TypeList = getTypesFromDescriptionTriple(subjectTypes);
			List<String> arg2TypeList = getTypesFromDescriptionTriple(objectTypes);

			Argument predicate = finalTriple.getPredicate();

			
			if(!labelOnly){
				
				if(SyntaxTreeUtil.isFirstChildStartingWith(arg2TypeList,finalTriple)) {
					
					argTypeMap.put(ARG1, arg1TypeList);
					argTypeMap.put(ARG2, arg2TypeList);
				}
				else{
					argTypeMap.put(ARG2, arg1TypeList);
					argTypeMap.put(ARG1, arg2TypeList);

				}

			}
			else{
				
				if (predicate != null && !SyntaxTreeUtil.isFirstChildStartingWith(arg1TypeList,finalTriple)) {
					
					argTypeMap.put(ARG1, arg1TypeList);
					argTypeMap.put(ARG2, arg2TypeList);
				
				}
				
				else{
					argTypeMap.put(ARG2, arg1TypeList);
					argTypeMap.put(ARG1, arg2TypeList);
					
				}
			}

		}
		 return argTypeMap;
	}


	private List<String> getTypesFromDescriptionTriple(	List<List<Argument>> subjectTypes) {
		
		List<String> arg1TypeList = new ArrayList<String>();
		 
		if(subjectTypes!= null && subjectTypes.size() > 0){
			for(List<Argument> args : subjectTypes){
				if(args!=null){
					for(Argument arg : args){
						if(arg!=null && !arg.getPartOfSpeech().equals("DT") && !arg.getLemma().equalsIgnoreCase("Subject") && !arg.getLemma().equalsIgnoreCase("Object") ){
							arg1TypeList.add(arg.getLemma());
						}
					}
				}
			}
		}
		
		return arg1TypeList;
	}
	
	

	
	
	
	/**
	 * Extract argument types for wikidata properties and store the results to a file
	 * @param output
	 * @param full
	 * @throws FileNotFoundException
	 */
	public static void extractPropertyArgumentTypes(String output, boolean full, int maxReal, int maxRealTypes) throws FileNotFoundException{
		
		PrintWriter out = new PrintWriter(new File(output));
		out.println("ID \t arg1 \t arg2");
		PropertyArgumentTypeExtractor ext = new PropertyArgumentTypeExtractor();
		
		
		List<String> propIdList = JacksonDBAPI.getEntityIdList("property");

		
			for(String propId : propIdList){
				
				if(full){
					Map<String, Map<String,Collection<String>>> propDescMap = null;

					 propDescMap = ext.extractFullArgumentTypes(propId,maxReal,maxRealTypes);
					 String propLabel =  JacksonDBAPI.getItemLabel(propId, "en");
						
						String 	propDesc = JacksonDBAPI.getItemDescription(propId, "en");
						
						System.out.println(propId + "\t" + propLabel + "\t" + propDesc + "\t" +  propDescMap.get(ARG1) + "\t" +  propDescMap.get(ARG2));
//						Map<String, Collection<String>> arg1 = propDescMap.get("ARG1");
//						Map<String, Collection<String>> arg2 = propDescMap.get("ARG2");
						out.println(propId + "\t"  +  propDescMap.get(ARG1) + "\t" +  propDescMap.get(ARG2));
					
				}
				
				else{
					 Map<String, Collection<String>> propDescMap = ext.extractArgumentTypesFromPropertyDescription(propId);
					 String propLabel =  JacksonDBAPI.getItemLabel(propId, "en");
						
						
					 String 	propDesc = JacksonDBAPI.getItemDescription(propId, "en");
						
						
					 System.out.println( propId + "\t" + propLabel + "\t" + propDesc + "\t" +  propDescMap.get(ARG1) + "\t" +  propDescMap.get(ARG2));
						
						
					 out.println( propId + "\t" + propDescMap.get(ARG1) + "\t" +  propDescMap.get(ARG2));
					
				}
				
				
			}
			
		
		out.close();
	}
	
	/**
	 * Extract arguments types for a given property using the DSTP and the ESPT links as well as property's label and description
	 * @param propId
	 * @return
	 */
	public 	Map<String, Map<String,Collection<String>>>  extractFullArgumentTypes(String propId,int maxReal, int maxRealTypes){
	
		Map<String, Map<String,Collection<String>>> finalMap = new HashMap<String, Map<String,Collection<String>>>();
		
		
		 Map<String,Collection<String>> finalArg1Types = new HashMap<String,Collection<String>> ();
		 Map<String,Collection<String>> finalArg2Types = new HashMap<String,Collection<String>> ();
		
		//DSTP
		Collection<String> arg1Type = extractTypeARG1(propId); 
		Collection<String> arg2Type = extractTypeARG2(propId);
	
		if(arg1Type!=null && arg1Type.size() > 0){
			finalArg1Types.put(PropertyArgumentTypeSources.INST_FACET, arg1Type);
		}
		if(arg2Type!=null && arg2Type.size() > 0){
			finalArg2Types.put(PropertyArgumentTypeSources.SUB_ITEM, arg2Type);
		}
		
		//ESTP
		Map<String, Collection<String>> inverseMap = extractArgumentTypesFromInverseProperty(propId);
		if(inverseMap.get(ARG1)!=null && inverseMap.get(ARG1).size() > 0){
			finalArg1Types.put(PropertyArgumentTypeSources.INVS_PROP,inverseMap.get(ARG1));
		}
		if(inverseMap.get(ARG2)!=null &&  inverseMap.get(ARG2).size() > 0){
			finalArg2Types.put(PropertyArgumentTypeSources.INVS_PROP,inverseMap.get(ARG2));
		}
		
			
		Map<String, Collection<String>> subPropMap = extractArgumentTypesFromSubProperty(propId);
		
		if(subPropMap.get(ARG1)!=null && subPropMap.get(ARG1).size() > 0){
			finalArg1Types.put(PropertyArgumentTypeSources.SUB_PROP, subPropMap.get(ARG1));
		}
		if(subPropMap.get(ARG2)!=null && subPropMap.get(ARG2).size() > 0){
			finalArg2Types.put(PropertyArgumentTypeSources.SUB_PROP,subPropMap.get(ARG2));
		}
		
		
		Map<String, Collection<String>> seeAlsoMap = extractArgumentTypesFromSeeAlsoProperty(propId);
		if(seeAlsoMap.get(ARG1)!=null && seeAlsoMap.get(ARG1).size() > 0){
			finalArg1Types.put(PropertyArgumentTypeSources.SEE_ALSO,seeAlsoMap.get(ARG1));
		}
		if(seeAlsoMap.get(ARG2)!=null && seeAlsoMap.get(ARG2).size() > 0){
			finalArg2Types.put(PropertyArgumentTypeSources.SEE_ALSO,seeAlsoMap.get(ARG2));
		}
		
		Map<String, Collection<String>> equiMap = extractArgumentTypesFromEquivalentProperty(propId);
		
		if(equiMap.get(ARG1)!=null && equiMap.get(ARG1).size() > 0){
			finalArg1Types.put(PropertyArgumentTypeSources.EQUI_PROP,equiMap.get(ARG1));
		}
		if(equiMap.get(ARG2)!=null && equiMap.get(ARG2).size() > 0){
			finalArg2Types.put(PropertyArgumentTypeSources.EQUI_PROP,equiMap.get(ARG2));
		}
		
		
		// Labels

		Map<String, Collection<String>> propLabelMap = extractArgumentTypesFromPropertyLabel(propId);

		if (propLabelMap.get(ARG1) != null && propLabelMap.get(ARG1).size() > 0) {
			finalArg1Types.put(PropertyArgumentTypeSources.SRC_PROP_LABEL,propLabelMap.get(ARG1));
		}
		if (propLabelMap.get(ARG2) != null && propLabelMap.get(ARG2).size() > 0) {
			finalArg2Types.put(PropertyArgumentTypeSources.SRC_PROP_LABEL,propLabelMap.get(ARG2));
		}

		// Aliases: they are two noisy to use
//		Map<String, Collection<String>> propAliasMap = extractArgumentTypesFromPropertyAliases(propId);
//
//		if (propAliasMap.get("ARG1") != null) {
//			finalArg1Types.addAll(propAliasMap.get("ARG1"));
//		}
//		if (propAliasMap.get("ARG2") != null) {
//			finalArg2Types.addAll(propAliasMap.get("ARG2"));
//		}
		
		//Description
		Map<String, Collection<String>> propDescMap = extractArgumentTypesFromPropertyDescription(propId);

		if(propDescMap.get(ARG1)!=null && propDescMap.get(ARG1).size() > 0){
			finalArg1Types.put(PropertyArgumentTypeSources.SRC_PROP_DESC,propDescMap.get(ARG1));
		}
		if(propDescMap.get(ARG2)!=null && propDescMap.get(ARG2).size() > 0){
			finalArg2Types.put(PropertyArgumentTypeSources.SRC_PROP_DESC,propDescMap.get(ARG2));
		}
		
		//Realizations
		
		Map<String, Collection<String>> argTypesMap = extractPropertyArgumentRealizations(propId, maxReal,maxRealTypes);
		
		if(argTypesMap.get("ARG1")!=null && argTypesMap.get("ARG1").size() > 0){
			finalArg1Types.put("REALIZATION",argTypesMap.get("ARG1"));
		}
		if(argTypesMap.get("ARG2")!=null && argTypesMap.get("ARG2").size() > 0){
			finalArg2Types.put("REALIZATION",argTypesMap.get("ARG2"));
		}
//		
		
		
		finalMap.put(ARG1, finalArg1Types);
		finalMap.put(ARG2, finalArg2Types);
		
		return finalMap;
	}
	
	
	public Map<String, Collection<String>> extractPropertyArgumentRealizations(String propId,int maxRealization, int maxTypes){
		
		Map<String, Collection<String>> argTypesMap = new HashMap<String, Collection<String>>();
		 
		Collection<String> finalArg1Types = new ArrayList<String>();
		Collection<String> arg1StructureTypes = new HashSet<String>();

		
		Collection<String> finalArg2Types = new ArrayList<String>();
		Collection<String> arg2StructureTypes = new HashSet<String>();
		
		
		
		List<ClaimRealization> cRealizations = JacksonDBAPI.getClaimRelatization(propId, maxRealization);
		
//		String propLabel = JacksonDBAPI.getItemLabel(propId, "en"); 
		
		for(ClaimRealization r : cRealizations){
		
			String domainId = r.getDomain();
			Collection<String> arg1Types = getArgTypesFromPropRealization(domainId);
			Collection<String> domainTypes = getItemTypes(domainId);

			String rangeId = r.getRange();
			Collection<String> arg2Types = getArgTypesFromPropRealization(rangeId);
			Collection<String> rangeTypes = getItemTypes(rangeId);

			 
			Set<String> intersection = Sets.newHashSet(arg1Types);
	        intersection.retainAll(arg2Types);
	          
			arg1Types.removeAll(intersection);
			arg2Types.removeAll(intersection);
			
			finalArg1Types.addAll(arg1Types);
			finalArg2Types.addAll(arg2Types);
			
			arg1StructureTypes.addAll(domainTypes);
			arg2StructureTypes.addAll(rangeTypes);
			

		}
		
		
		Set<String> intersection = Sets.newHashSet(finalArg1Types);
        intersection.retainAll(finalArg2Types);
        finalArg1Types.removeAll(intersection);
//		System.out.println("Arg1: "  + finalArg1Types);
//		System.out.println("Arg1: "  + convertWordListToFreqMap(finalArg1Types));
		
		finalArg2Types.removeAll(intersection);
//		System.out.println("Arg2: " + finalArg2Types);
//		System.out.println("Arg2: "  + convertWordListToFreqMap(finalArg2Types));

//		System.out.println("........");
		finalArg1Types =  getTopWordsFromWordMap(convertWordListToFreqMap(finalArg1Types),maxTypes);
        finalArg1Types.addAll(arg1StructureTypes);
		argTypesMap.put(ARG1, finalArg1Types);

		
		finalArg2Types =  getTopWordsFromWordMap(convertWordListToFreqMap(finalArg2Types),maxTypes);
		finalArg2Types.addAll(arg2StructureTypes);

		argTypesMap.put(ARG2,finalArg2Types );
		
		

		return argTypesMap;
	}
	
	
	/**
	 * Take a list of words and count the number of occurrences of each word in the list
	 * The methods return a sorted map of words and their number of occurrences
	 * @param wordList
	 * @return
	 */
	public Map<String, Double> convertWordListToFreqMap(Collection<String> wordList){
		Map<String, Double> wordCountMap = new HashMap<String, Double>(); 
		
		for(String w : wordList){
			
			Double count = wordCountMap.get(w);
			
			if(count != null){
				
				wordCountMap.put(w, count+1);
			}
			else{
				wordCountMap.put(w, 1.0);
			}
			
		}
		
		//Sort
		ValueComparator bvc =  new ValueComparator(wordCountMap);

		TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
		
		sorted_map.putAll(wordCountMap);
				
				
		return sorted_map;
		
	}
	
	/**
	 * Get the top frequent element from an ordered word-count map
	 * @param orderedWordMap
	 * @param max
	 * @return
	 */
	public Collection<String> getTopWordsFromWordMap(Map<String, Double> orderedWordMap, int max){
		
		int counter = 0 ;
		List<String> result = new ArrayList<String>();
		
		for(Entry<String, Double> e: orderedWordMap.entrySet()){
			
			if(counter > max){
				return result;
			}
			else{
				
				result.add(counter++, e.getKey());
			}
			
		}
		return result;
	}

	/**
	 * Get the type of a wikidata item based on instance of relationship
	 * @param r
	 * @return
	 */
	private Collection<String> getItemTypes(String itemId) {
		
		Collection<String> domainTypes = new ArrayList<String>();

		List<String> instanceOfList = JacksonDBAPI.getClaimRange(itemId,StructuralPropertyMapper.instanceOf);

		for(String instanceOfId : instanceOfList){
			
			String typeLabel = JacksonDBAPI.getItemLabel(instanceOfId, "en");
			if(typeLabel!=null){
				domainTypes.add(typeLabel);
			}
			
		}
		return domainTypes;
	}
	
	public Collection<String> getArgTypesFromPropRealization(String realId){
		
		Collection<String> argTypes = new ArrayList<String>();

		
		String label = JacksonDBAPI.getItemLabel(realId, "en");
		
		List<String> labelLemmas = StanfordNLPTools.lemmatize(label);
		
		String desc = JacksonDBAPI.getItemDescription(realId, "en");
		List<String> descLemmas = StanfordNLPTools.lemmatize(desc);

		
		argTypes.addAll(labelLemmas);
		argTypes.addAll(descLemmas);
		
		return argTypes;
	}
	
	
	public  void extractPropertyArgumentTypesFromRealization(String output, int maxRealization, int maxTypes) throws FileNotFoundException{
		PrintWriter out = new PrintWriter(new File(output));
		
		System.out.println("propId \t arg1 \t arg2");
		out.println("propId \t arg1 \t arg2");

		List<String> propertyIdList = JacksonDBAPI.getEntityIdList("property");
		for(String propId:propertyIdList){
			
			Map<String, Collection<String>> argTypesMap = extractPropertyArgumentRealizations(propId, maxRealization,maxTypes);
		
			System.out.println(propId + "\t" + argTypesMap.get(ARG1) + "\t" + argTypesMap.get(ARG2));
			
			out.println(propId + "\t" + argTypesMap.get(ARG1) + "\t" + argTypesMap.get(ARG2));

		}
		out.close();

	}
	
	public static void main(String[] args) throws FileNotFoundException {
		
//		PropertyArgumentTypeExtractor ext = new PropertyArgumentTypeExtractor();

//		ext.extractPropertyArgumentTypesFromRealization("test/output/argTypes/arg_types_realization_100_20.txt", 100, 20);
		
		extractPropertyArgumentTypes("output/argTypes/arg_types_full_all_props.txt",true,100, 20);

//		PropertyArgumentTypeExtractor ext = new PropertyArgumentTypeExtractor();
//		String propId = "P1000" ; //"P1478";
//		Map<String, Collection<String>> argTypesMap = ext.extractArgumentTypesFromPropertyLabel(propId);
//		System.out.println("ARG1:" + argTypesMap.get("ARG1") + "\nARG2:" + argTypesMap.get("ARG2")  );

		
//		
//		Map<String, Map<String, Collection<String>>> argTypesMap2 = ext.extractFullArgumentTypes(propId);
//		System.out.println("Sub property\n ARG1:" + argTypesMap2.get("ARG1") + "\n ARG2:" + argTypesMap2.get("ARG2")  );

	}

}
