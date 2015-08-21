package hms.alignment;


import hms.StanfordNLPTools;
import hms.alignment.data.AlignmentResult;
import hms.alignment.data.Frame;
import hms.alignment.data.SemanticRole;
import hms.alignment.framenet.FrameNetAPI;
import hms.alignment.wikidata.EnrichedProperty;
import hms.alignment.wikidata.PropertyArgumentTypeExtractor;
import hms.alignment.wikidata.PropertyArgumentTypeSources;
import hms.alignment.wikidata.WDArgumentMetaData;
import hms.alignment.wikidata.WDArgumentMetadataExtractor;
import hms.alignment.wikidata.WDProperty;
import hms.similarity.RoleSimilarityMethods;
import hms.similarity.TextSimilarityUtil;
import hms.similarity.ValueComparator;
import hms.similarity.WordNetSimilarityCalculator;
import hms.similarity.WordNetSimilarityMethod;
import hms.wikidata.dbimport.JacksonDBAPI;
import hms.wikidata.graph.WikidataTraverser;
import hms.wikidata.model.ExperimentalArgTypes;
import hms.wikidata.model.PropertyOfficialCategory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;


public class RoleMapper {
	
	
	
//	private static final boolean STEM = false;
//	private static WordNetSimilarityMethod WN_SIM_METHOD =null;

	
	
//	private static final int MAX_NUMBER_OF_MATCHING_ROLES = 2;

	private static final WordNetSimilarityMethod WN_DEFAULT_SIM_METHOD = WordNetSimilarityMethod.Resnik;


	/**
	 * Identify the semantic roles for an instance of a given property according to a given candidate frame
	 * @param propId
	 * @param propInstanceId
	 * @param frameId
	 * @return 
	 */
	public static Map<String, Map<String, Double>> mapInstance(String propId,String propInstanceId, String frameId,RoleSimilarityMethods simMethod,
			WordNetSimilarityMethod wnMethod){

		Map<String,Map<String, Double>> finalResult = new HashMap<String, Map<String, Double>>();
		
		Frame frame = FrameNetAPI.getFrameFullData(frameId); //candidate frame
		
		List<SemanticRole> sematicRoles = frame.getRoles(); //the semantic roles of the candidate frame

		//identify the role of the subject of the property
		Map<String, Double> instanceRole = identifyRole(propInstanceId,propId,true,frame.getLabel(), sematicRoles,simMethod,wnMethod);
		
		
		
		finalResult.put("Sub: " + JacksonDBAPI.getItemLabel(propInstanceId, "en"),instanceRole);
		
		//Get the object of the property
		
		List<String> instanceRanges = JacksonDBAPI.getClaimRange(propInstanceId, propId);
		
 		
		//Identify the roles of the objects
		for(String instanceRang: instanceRanges){
			
			Map<String, Double> objectRole = identifyRole(instanceRang, propId, false, frame.getLabel(),sematicRoles,simMethod,wnMethod);
			finalResult.put("Obj: " + JacksonDBAPI.getItemLabel(instanceRang, "en") ,objectRole );
		}
		
		return finalResult;
	}

	/**
	 * Identify the semantic role of a given wikidata argument from a list of possible role
	 * @param entityId
	 * @param sematicRoles
	 * @return
	 */
	private static Map<String, Double> identifyRole(String entityId, String propertyInvolvedIn, boolean isSubject, String frameLabel,
			List<SemanticRole> sematicRoles,RoleSimilarityMethods simMethod, 
			WordNetSimilarityMethod wnMethod
			) {
	
		Map<String, Double> entityRoleMap = new HashMap<String,Double>();

		//Get metadata about the entity
		
		WDArgumentMetaData entityMetaData = WDArgumentMetadataExtractor.extractItemMetadata(entityId,propertyInvolvedIn);
		
		//Select the best matching semantic role
		
		for(SemanticRole semanticRole : sematicRoles){
			
			double entityRoleSimilarity = 0 ;
			if(simMethod.equals(RoleSimilarityMethods.stemOverlap)) {
				entityRoleSimilarity = calculateEntityRoleSimilarityStemOverlap(entityMetaData, frameLabel,semanticRole,isSubject);
			}
			else if(simMethod.equals(RoleSimilarityMethods.WN)){
				entityRoleSimilarity = calculateEntityRoleSimilarityWN(entityMetaData, frameLabel,semanticRole,wnMethod,isSubject);
			}
			
			if(entityRoleSimilarity > 0) {
				
			
				
				entityRoleMap.put(semanticRole.getRole(), entityRoleSimilarity);
			}
		}
		
		ValueComparator bvc =  new ValueComparator(entityRoleMap);

		//Sort
		TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
		sorted_map.putAll(entityRoleMap);
		
				
		return sorted_map;
	}

	private static double calculateEntityRoleSimilarityStemOverlap3(WDArgumentMetaData entityMetaData, SemanticRole semanticRole,boolean isSubject) {
		
		double finalSimilarity = 0 ;
		
		//1. calculate entity label - semantic role label similarity
		//2. calculate entity aliases - semantic role label similarity
		//3. calculate entity description - semantic role definition similarity
		
		String entityLabel = entityMetaData.getLabel();
		String roleLabel = semanticRole.getRole();
		finalSimilarity += TextSimilarityUtil.calculateStemOverlap(entityLabel, roleLabel);
		List<String> entityAliases = entityMetaData.getAliases();
		finalSimilarity += TextSimilarityUtil.calculateStemOverlap(entityAliases.toString(), roleLabel);
		String entityDescription = entityMetaData.getDescription();
		String roleRefnition = semanticRole.getDefnition();
		finalSimilarity += TextSimilarityUtil.calculateStemOverlap(entityDescription, roleRefnition);

		
		if(isSubject){
			
			WDArgumentMetadataExtractor.getItemInferredRoleMetadataForSubject(entityMetaData);
			
			
		}
		else{
			 WDArgumentMetadataExtractor.getItemInferredRoleMetadataForObject(entityMetaData);
		
		}
		
		for(Entry<String, String> e: entityMetaData.getInferredRoleMetaData().entrySet()){
			if(e.getKey().contains("description")){
				finalSimilarity += TextSimilarityUtil.calculateStemOverlap(e.getValue(), entityDescription);
			}
			else{
				finalSimilarity += TextSimilarityUtil.calculateStemOverlap(e.getValue(), entityLabel);
			}
			
		}
		
		//4. repeat the same for each father node
		
		for(WDArgumentMetaData fatherMetaData : entityMetaData.getFather()){
			
			finalSimilarity += TextSimilarityUtil.calculateStemOverlap(fatherMetaData.getLabel(), roleLabel);
			finalSimilarity += TextSimilarityUtil.calculateStemOverlap(fatherMetaData.getAliases().toString(), roleLabel);
			finalSimilarity += TextSimilarityUtil.calculateStemOverlap(fatherMetaData.getDescription(), roleRefnition);


		}
		return finalSimilarity;
	}
	
	
	private static Set<WDArgumentMetaData> cachedArguments = new HashSet<WDArgumentMetaData>();
	private static Set<WDArgumentMetaData> cachedFE = new HashSet<WDArgumentMetaData>();

	
	private static double calculateEntityRoleSimilarityWN(WDArgumentMetaData entityMetaData, String frameLabel,
			SemanticRole semanticRole,WordNetSimilarityMethod WN_SIM_METHOD, boolean isSubject) {
	
		
		double finalSimilarity = 0 ;
		
		
		//WD Argument Metadata
		String entityLabel = entityMetaData.getLabel();
		List<String> entityAliases = entityMetaData.getAliases();
		String entityDescription = entityMetaData.getDescription();
		List<WDArgumentMetaData> fatherClasses = entityMetaData.getFather();
		 
		if(isSubject){
			WDArgumentMetadataExtractor.getItemInferredRoleMetadataForSubjectSimple(entityMetaData);

		}
		else{
			 WDArgumentMetadataExtractor.getItemInferredRoleMetadataForObjectSimple(entityMetaData);
		
		}
		Map<String, String> inferredMetadata = entityMetaData.getInferredRoleMetaData();
		
		//Role Metadata
		String roleLabel = semanticRole.getRole();
		String roleDefnition = semanticRole.getDefnition();

		
		
		//****** Check similarity with FE label
//		finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityLabel,roleLabel,WN_SIM_METHOD);
//		finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityAliases.toString(),roleLabel,WN_SIM_METHOD);
		
		//Calculate similarity with father classes
		for(WDArgumentMetaData fatherClass:fatherClasses){
			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(fatherClass.getLabel().toString(),roleLabel,WN_SIM_METHOD);
		}
		//Calculate similarity with inferred metadata
		for(String m :inferredMetadata.values()){
			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(m,roleLabel,WN_SIM_METHOD);
		}
		
		//************ Check similarity with FE definition
//		finalSimilarity += WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityDescription, roleDefnition, WN_SIM_METHOD);
		for(WDArgumentMetaData fatherClass:fatherClasses){
			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(fatherClass.getLabel().toString(),roleDefnition,WN_SIM_METHOD);
		}
				
		
		//********* Check the similarity with FE fillers
		
		Set<String> fillers = FrameNetAPI.getFEFillerHeadWords(frameLabel, roleLabel).keySet();

		finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityLabel,fillers.toString(),WN_SIM_METHOD);
		finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityAliases.toString(),fillers.toString(),WN_SIM_METHOD);
		
		//Calculate similarity with father classes
		for (WDArgumentMetaData fatherClass : fatherClasses) {
			finalSimilarity += WordNetSimilarityCalculator.calculateSimilarityLemmatize(fatherClass.getLabel().toString(), fillers.toString(), WN_SIM_METHOD);
		}
		// Calculate similarity with inferred metadata
		for (String m : inferredMetadata.values()) {
			finalSimilarity += WordNetSimilarityCalculator.calculateSimilarityLemmatize(m, fillers.toString(), WN_SIM_METHOD);
		}
		
		finalSimilarity += WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityDescription, fillers.toString(), WN_SIM_METHOD);

		
		
		return finalSimilarity;
	}
	
	private static double calculateEntityRoleSimilarityStemOverlap(WDArgumentMetaData entityMetaData, String frameLabel, SemanticRole semanticRole,boolean isSubject) {
		 
	
		
		double finalSimilarity = 0 ;
		
		
		//WD Argument Metadata
		String entityLabel = entityMetaData.getLabel();
		List<String> entityAliases = entityMetaData.getAliases();
		String entityDescription = entityMetaData.getDescription();
		List<WDArgumentMetaData> fatherClasses = entityMetaData.getFather();
		 
		if(isSubject){
			WDArgumentMetadataExtractor.getItemInferredRoleMetadataForSubjectSimple(entityMetaData);

		}
		else{
			 WDArgumentMetadataExtractor.getItemInferredRoleMetadataForObjectSimple(entityMetaData);
		
		}
		Map<String, String> inferredMetadata = entityMetaData.getInferredRoleMetaData();
		
		//Role Metadata
		String roleLabel = semanticRole.getRole();
		String roleDefnition = semanticRole.getDefnition();

		
		
		//****** Check similarity with FE label
//		finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityLabel,roleLabel,WN_SIM_METHOD);
//		finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityAliases.toString(),roleLabel,WN_SIM_METHOD);
		
		//Calculate similarity with father classes
		for(WDArgumentMetaData fatherClass:fatherClasses){
			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(fatherClass.getLabel().toString(),roleLabel);
		}
		//Calculate similarity with inferred metadata
		for(String m :inferredMetadata.values()){
			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(m,roleLabel);
		}
		
		//************ Check similarity with FE definition
//		finalSimilarity += WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityDescription, roleDefnition, WN_SIM_METHOD);
		for(WDArgumentMetaData fatherClass:fatherClasses){
			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(fatherClass.getLabel().toString(),roleDefnition);
		}
				
		
		//********* Check the similarity with FE fillers
		
		Set<String> fillers = FrameNetAPI.getFEFillerHeadWords(frameLabel, roleLabel).keySet();

		finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(entityLabel,fillers.toString());
		finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(entityAliases.toString(),fillers.toString());
		
		//Calculate similarity with father classes
		for (WDArgumentMetaData fatherClass : fatherClasses) {
			finalSimilarity +=TextSimilarityUtil.calculateStemOverlap(fatherClass.getLabel().toString(), fillers.toString());
		}
		// Calculate similarity with inferred metadata
		for (String m : inferredMetadata.values()) {
			finalSimilarity += TextSimilarityUtil.calculateStemOverlap(m, fillers.toString());
		}
		
		finalSimilarity += TextSimilarityUtil.calculateStemOverlap(entityDescription, fillers.toString());

		
		
		return finalSimilarity;
	}
	
	
	
	
	
	
	
	

	private static double calculateEntityRoleSimilarityWN2(WDArgumentMetaData entityMetaData, String frameLabel,
			SemanticRole semanticRole,WordNetSimilarityMethod WN_SIM_METHOD, boolean isSubject) {
		
		double finalSimilarity = 0 ;
		
		//1. calculate entity label - semantic role label similarity
		//2. calculate entity aliases - semantic role label similarity
		//3. calculate entity description - semantic role definition similarity
		//4. calculate entity aliases - semantic role definition similarity
		//5. calculate entity inferred role - role label similarity
		//6. calculate entity inferred role - role definition similarity
		String entityLabel = entityMetaData.getLabel();
		String roleLabel = semanticRole.getRole();
		
	

		
		
		if(entityLabel !=null)
			finalSimilarity += WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityLabel, roleLabel,WN_SIM_METHOD);
		
		List<String> entityAliases = entityMetaData.getAliases();
		
		finalSimilarity += WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityAliases.toString(), roleLabel,WN_SIM_METHOD);
	
		String roleDefnition = semanticRole.getDefnition();
		
		//***************************//
		
		if(isSubject){
			
			WDArgumentMetadataExtractor.getItemInferredRoleMetadataForSubjectSimple(entityMetaData);
			
			
		}
		else{
			 WDArgumentMetadataExtractor.getItemInferredRoleMetadataForObjectSimple(entityMetaData);
		
		}
		
		for(Entry<String, String> e: entityMetaData.getInferredRoleMetaData().entrySet()){
			if(e.getKey().contains("description")){
				finalSimilarity += WordNetSimilarityCalculator.calculateSimilarityLemmatize(e.getValue(), roleDefnition,WN_SIM_METHOD);
			}
			else{
				finalSimilarity += WordNetSimilarityCalculator.calculateSimilarityLemmatize(e.getValue(), roleLabel,WN_SIM_METHOD);
			}
			
		}
		
		

		//+++++++++++++++++++++++++
		
		
		//************ Calcuate similarity with role fillers
		Map<String, Double> headWords = FrameNetAPI.getFEFillerHeadWords(frameLabel, roleLabel);
		
		finalSimilarity += WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityLabel, headWords.keySet().toString(),WN_SIM_METHOD);
		finalSimilarity += WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityAliases.toString(), headWords.keySet().toString(),WN_SIM_METHOD);
		
		//*************
		
		String entityDescription = entityMetaData.getDescription();
		
		if(entityDescription != null && roleDefnition!=null){
					
			finalSimilarity +=  WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityDescription,roleDefnition,WN_SIM_METHOD );
		}
		
		finalSimilarity +=  WordNetSimilarityCalculator.calculateSimilarityLemmatize(entityAliases.toString(),roleDefnition,WN_SIM_METHOD );

		
		
		//5. repeat the same for each father node
		
		for(WDArgumentMetaData fatherMetaData : entityMetaData.getFather()){
			
			String fatherLabel = fatherMetaData.getLabel();
			finalSimilarity +=  WordNetSimilarityCalculator.calculateSimilarityLemmatize(fatherLabel, roleLabel,WN_SIM_METHOD);
			List<String> fatherAliases = fatherMetaData.getAliases();
			
			finalSimilarity +=  WordNetSimilarityCalculator.calculateSimilarityLemmatize(fatherAliases.toString(), roleLabel,WN_SIM_METHOD);
			
			String fatherDescription = fatherMetaData.getDescription();
		
			if(fatherDescription != null && roleDefnition!=null){
				finalSimilarity +=  WordNetSimilarityCalculator.calculateSimilarityLemmatize(fatherDescription,roleDefnition,WN_SIM_METHOD);
			}
			
			finalSimilarity +=  WordNetSimilarityCalculator.calculateSimilarityLemmatize(fatherAliases.toString(),roleDefnition,WN_SIM_METHOD );


		}
		return finalSimilarity;
	}
	
	
	public static  Map<String, Map<String, Double>>  mapProperty(String propId,String frameId, RoleSimilarityMethods roleSimilarityMethod, boolean loadFromDB){

		Map<String, Map<String, Double>>  results = new HashMap<String, Map<String,Double>>();
		
		PropertyArgumentTypeExtractor ext = new PropertyArgumentTypeExtractor();
		
		Map<String, Map<String, Collection<String>>> argTypeMap = null;
	
		

		
		Collection<String> arg1Types = new HashSet<String>();
		Collection<String> arg2Types = new HashSet<String>();
		
		if(!loadFromDB){
			 argTypeMap = ext.extractFullArgumentTypes(propId,100,20);
			 
			 for(Collection<String> x : argTypeMap.get("ARG1").values()){
					arg1Types.addAll(x);
			  }
			 
			 for(Collection<String> x : argTypeMap.get("ARG2").values()){
						arg2Types.addAll(x);
			 }	
			 System.out.println(arg1Types);
			 System.out.println(arg2Types);

		}
		else{ //Load from the DB
			
			String arg1FromDB = null;
			String arg2FromDB = null;
			
			ExperimentalArgTypes argTypes = JacksonDBAPI.getExperimentalArgTypes(propId);
			
			arg1FromDB = argTypes.getTypeArg1();
			arg2FromDB = argTypes.getTypeArg2();
			
			for(String resName: PropertyArgumentTypeSources.allResourceNames){
				arg1FromDB = arg1FromDB.replace(resName+"=", "").replace("{", "").replace("}", "").replace("[", "").replace("]", "");
				arg2FromDB = arg2FromDB.replace(resName+"=", "").replace("{", "").replace("}", "").replace("[", "").replace("]", "");
				
				
			}	
			
			arg1Types.addAll(Arrays.asList(arg1FromDB.split(",")));
			System.out.println(arg1Types);
			arg2Types.addAll(Arrays.asList(arg2FromDB.split(",")));
			System.out.println(arg2Types);
			
		}
		
		Frame frame = FrameNetAPI.getFrameFullData(frameId); //candidate frame
		
		List<SemanticRole> sematicRoles = frame.getRoles(); //the semantic roles of the candidate frame
		
		//Identify the best role for the subject
		
		Map<String, Double> subjectRoleMap = new HashMap<String,Double>();
		Map<String, Double> objectRoleMap = new HashMap<String,Double>();

		
		for(SemanticRole sr : sematicRoles){
			
			Set<String> fillers = FrameNetAPI.getFEFillerHeadWords(frame.getLabel(),  sr.getRole()).keySet();
		
			
			
			double simRoleSubject = identifyBestFEForPropertyArgument(arg1Types, fillers, sr, roleSimilarityMethod);
			
			if(simRoleSubject > 0){
				subjectRoleMap.put(sr.getRole(), simRoleSubject);
			}
		
			
			
			double simRoleObject = identifyBestFEForPropertyArgument(arg2Types, fillers, sr, roleSimilarityMethod);
			if(simRoleObject > 0){
				objectRoleMap.put(sr.getRole(), simRoleObject);
			}

		}
		
		//Sort the maps
		
		
		ValueComparator bvc =  new ValueComparator(subjectRoleMap);
		TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
		sorted_map.putAll(subjectRoleMap);
		results.put("Subject", sorted_map);
		
		
		ValueComparator bvc2 =  new ValueComparator(objectRoleMap);
		TreeMap<String,Double> sorted_map2 = new TreeMap<String,Double>(bvc2);
		sorted_map2.putAll(objectRoleMap);
		results.put("Object", sorted_map2);
		
		
		return results;
	}
	private static double identifyBestFEForPropertyArgument(
			Collection<String> mindedTypes, Set<String> fillers,
			SemanticRole semanticRole, RoleSimilarityMethods roleSimilarityMethod) {


		
		double finalSimilarity  = 0 ;
		
		
		//Role Metadata
		String roleLabel = semanticRole.getRole();
		String roleDefnition = semanticRole.getDefnition();
		
		if(roleSimilarityMethod.equals(RoleSimilarityMethods.stemOverlap)) {
			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(mindedTypes,roleLabel);
			
			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(mindedTypes,fillers);
			

			
//			List<String> rolDefArgsList = StanfordNLPTools.lemmatize(roleDefnition);
//			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(mindedTypes,rolDefArgsList);//rolDefArgs.get("ARG1"));

			PropertyArgumentTypeExtractor argExtractor = new PropertyArgumentTypeExtractor();
			Map<String, Collection<String>> rolDefArgs = argExtractor.getArgumentTypesFromSentence(roleDefnition,false);
			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(mindedTypes,rolDefArgs.get("ARG1"));

			//Get ARG1 which is normally the defining item of the role
			
		}
		else if(roleSimilarityMethod.equals(RoleSimilarityMethods.WN)) {
			
			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityNoLemmatization(mindedTypes,roleLabel,WN_DEFAULT_SIM_METHOD);
			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityNoLemmatization(mindedTypes,fillers,WN_DEFAULT_SIM_METHOD);
//			List<String> rolDefArgsList = StanfordNLPTools.lemmatize(roleDefnition);
//			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityNoLemmatization(mindedTypes,rolDefArgsList,WN_DEFAULT_SIM_METHOD);


			PropertyArgumentTypeExtractor argExtractor = new PropertyArgumentTypeExtractor();
			Map<String, Collection<String>> rolDefArgs = argExtractor.getArgumentTypesFromSentence(roleDefnition,false);
			//Get ARG1 which is normally the defining item of the role
			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityNoLemmatization(mindedTypes,rolDefArgs.get("ARG1"),WN_DEFAULT_SIM_METHOD);

		
			
		}
		
		return finalSimilarity;
		
	}

	public static  Map<String, Map<String, Double>>  mapPropertyOld(String propId,String frameId, RoleSimilarityMethods roleSimilarityMethod){

		Map<String, Map<String, Double>>  results = new HashMap<String, Map<String,Double>>();
		EnrichedProperty p = new EnrichedProperty(propId);
		p.enrich();
		
		
		Frame frame = FrameNetAPI.getFrameFullData(frameId); //candidate frame
		
		List<SemanticRole> sematicRoles = frame.getRoles(); //the semantic roles of the candidate frame
		
		//Identify the best role for the subject
		
		Map<String, Double> subjectRoleMap = new HashMap<String,Double>();
		Map<String, Double> objectRoleMap = new HashMap<String,Double>();

		
		for(SemanticRole sr : sematicRoles){
			
			Set<String> fillers = FrameNetAPI.getFEFillerHeadWords(frame.getLabel(),  sr.getRole()).keySet();

			double simRoleSubject = identifyBestFEForPropertyArgument(p.getSubjectTypesByLabel(), p.getSubjectTypes(), p.getSubjectTypesDescriptions(),p.getDescriptionArguments().getArg1Types(), fillers, sr, roleSimilarityMethod);
			if(simRoleSubject > 0){
				subjectRoleMap.put(sr.getRole(), simRoleSubject);
			}
		
			//Identify the bset role for the object
		
			double simRoleObject = identifyBestFEForPropertyArgument(p.getObjectTypesByLable(),p.getObjectTypes(), p.getObjectTypesDescriptions(), p.getDescriptionArguments().getArg2Types(),fillers, sr,roleSimilarityMethod );
			if(simRoleObject > 0){
				objectRoleMap.put(sr.getRole(), simRoleObject);
			}

		}
		
		//Sort the maps
		
		
		ValueComparator bvc =  new ValueComparator(subjectRoleMap);
		TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
		sorted_map.putAll(subjectRoleMap);
		results.put("Subject", sorted_map);
		
		
		ValueComparator bvc2 =  new ValueComparator(objectRoleMap);
		TreeMap<String,Double> sorted_map2 = new TreeMap<String,Double>(bvc2);
		sorted_map2.putAll(objectRoleMap);
		results.put("Object", sorted_map2);
		
		
		return results;
	}
	
	private static double identifyBestFEForPropertyArgument(Set<String> typesByLabel,
			Set<String> mindedTypes, Set<String> minedTypesDescriptions, List<String> descMinedTypes,
			Set<String> fillers, SemanticRole semanticRole, RoleSimilarityMethods roleSimilarityMethod) {
		
		double finalSimilarity  = 0 ;
		
		
		//Role Metadata
		String roleLabel = semanticRole.getRole();
		String roleDefnition = semanticRole.getDefnition();
		
		if(roleSimilarityMethod.equals(RoleSimilarityMethods.stemOverlap)) {
			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(mindedTypes.toString(),roleLabel);
			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(mindedTypes.toString(),fillers.toString());
			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(mindedTypes.toString(),roleDefnition);
			
//			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(minedTypesDescriptions.toString(),roleLabel);
//			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(minedTypesDescriptions.toString(),fillers.toString());
//			finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(minedTypesDescriptions.toString(),roleDefnition);
			
			//Check types extracted from the property description
			if(descMinedTypes!=null){
				finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(descMinedTypes.toString(),roleLabel);
				finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(descMinedTypes.toString(),roleDefnition);
				finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(descMinedTypes.toString(),fillers.toString());
			}
			
			if(typesByLabel.size() > 0){
				
				finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(typesByLabel.toString(),roleLabel);
				finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(typesByLabel.toString(),roleDefnition);
				finalSimilarity+= TextSimilarityUtil.calculateStemOverlap(typesByLabel.toString(),fillers.toString());
			}
		}
		else if(roleSimilarityMethod.equals(RoleSimilarityMethods.WN)) {
			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(mindedTypes.toString(),roleLabel,WN_DEFAULT_SIM_METHOD);
//			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(mindedTypes.toString(),fillers.toString(),WN_DEFAULT_SIM_METHOD);
			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(mindedTypes.toString(),roleDefnition,WN_DEFAULT_SIM_METHOD);
			
//			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(minedTypesDescriptions.toString(),roleLabel,WordNetSimilarityMethod.Path);
//			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(minedTypesDescriptions.toString(),fillers.toString(),WordNetSimilarityMethod.Path);
//			finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(minedTypesDescriptions.toString(),roleDefnition,WordNetSimilarityMethod.Path);
			
			//Check types extracted from the property description
			if(descMinedTypes!=null){
				finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(descMinedTypes.toString(),roleLabel,WN_DEFAULT_SIM_METHOD);
				finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(descMinedTypes.toString(),roleDefnition,WN_DEFAULT_SIM_METHOD);
//				finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(descMinedTypes.toString(),fillers.toString(),WN_DEFAULT_SIM_METHOD);
			}
			
			if(typesByLabel.size() > 0){
				finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(typesByLabel.toString(),roleLabel,WN_DEFAULT_SIM_METHOD);
				finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(typesByLabel.toString(),roleDefnition,WN_DEFAULT_SIM_METHOD);
//				finalSimilarity+= WordNetSimilarityCalculator.calculateSimilarityLemmatize(typesByLabel.toString(),fillers.toString(),WN_DEFAULT_SIM_METHOD);
			}
		}
		
		return finalSimilarity;

	}

	
	/**
	 * Find a matching frame for a given WD property
	 * @param propId
	 */
	public static List<AlignmentResult> findMatchingFrames(String propId,RoleSimilarityMethods simMethod,int maxCandidates, boolean loadFromDB){
		
		List<AlignmentResult> result = new ArrayList<AlignmentResult>() ;
		
		
		Map<String, Double> candFrames = FNWDCandiateExtractor.getCandidateAll(propId);
		
		//Deal with the best candidate
		int count = 0 ;
		
		for(String frameId : candFrames.keySet()){
			
			if(count >= maxCandidates){
				
				break;
			}
			
			Map<String, Map<String, Double>> rs = mapProperty(propId, frameId,simMethod,loadFromDB);
			
			
			AlignmentResult r = new AlignmentResult();
			r.setPropId(propId);
			r.setFrameId(frameId);
			r.setMappings(rs);
			result.add(count, r);
			
			count ++ ;
		}
		
		return result;
	}
	
	
	/**
	 * Find alignments between all WD properties and framenet frames and roles
	 * @param output
	 * @param simMethod
	 * @param maxCandidates
	 * @param maxRoles
	 * @throws FileNotFoundException
	 */
	public static void findMatchingFrames(String output,RoleSimilarityMethods simMethod,int maxCandidates, int maxRoles,boolean loadFromDB ) throws FileNotFoundException{
		
		PrintWriter out = new PrintWriter(new File(output));
		out.println("Prop ID \t Frame ID \t ARG1 Role \t ARG2 Role \t simMethod \t rank");
		System.out.println("Prop ID \t Frame ID \t ARG1 Role \t ARG2 Role");
	
		for(PropertyOfficialCategory cat : PropertyOfficialCategory.values()) {
		
			List<String> propIdList = JacksonDBAPI.getOfficialProperties(cat);
			
			for(String propId : propIdList){
				
				List<AlignmentResult> r = findMatchingFrames(propId, simMethod,maxCandidates,loadFromDB);
				
				//Get the highest ranked frame
				int rank = 1;
				for(AlignmentResult ar : r){
//				if(r.size() > 0){
//					AlignmentResult ar = r.get(0);
					
					List<String> subTopRoles = getTopRoles(ar.getMappings().get("Subject"),maxRoles);
					List<String> objTopRoles = getTopRoles(ar.getMappings().get("Object"),maxRoles);
					System.out.println(ar.getPropId() + "\t" + ar.getFrameId() + "\t" + subTopRoles+ "\t" +objTopRoles +"\t" + simMethod + "\t " + rank);
					out.println(ar.getPropId() + "\t" + ar.getFrameId() + "\t" + subTopRoles+ "\t" +objTopRoles +"\t" + simMethod + "\t " + rank);
					
					rank++;
				}

			}
		}
		out.close();
	}
	
	

	public static void main2(String[] args) throws FileNotFoundException {
		
		String frameId = "FN_SemanticPredicate_376"; //"FN_SemanticPredicate_281" ; P1534
		String propId =  "P108" ;
		String lang = "en" ;
	
		
		String instanceId = WikidataTraverser.getRandomInstance(propId, lang);
		PrintWriter out = new PrintWriter(new File("output/annotation_"+propId+"_"+"all"+"_d="+WDArgumentMetadataExtractor.default_depth+".txt"));
	
		
		for (int i = 1; i <= 1; i++) {
			
			
			WDArgumentMetadataExtractor.default_depth= i ;
			
			out.println("**************** DEPTH = " + i + " *************");
			for (WordNetSimilarityMethod val : WordNetSimilarityMethod.values()) {
				
				

				if(val.equals(WordNetSimilarityMethod.Lesk) || val.equals(WordNetSimilarityMethod.HirstStOnge) )
					continue;
						
		
				
					out.println("-----------" + val + " --------------");
				
			
					
					Map<String, Map<String, Double>> res = mapInstance(propId, instanceId, frameId,RoleSimilarityMethods.WN, val);
					
					
					
					for(Entry<String, Map<String, Double>> e : res.entrySet()){
						
						
						out.println(e.getKey() + "\t" + e.getValue());
						
					}
				
			
			}
			out.println("********************************************");
		}
		out.close();
	
	
	
//		Map<String, Map<String, Double>> res1 = mapInstance(propId, instanceId, frameId,RoleSimilarityMethods.WN, WordNetSimilarityMethod.Path);
//		
//		for(Entry<String, Map<String, Double>> e : res1.entrySet()){
//			
//			out.println(e.getKey() + "\t" + e.getValue());
//			
//		}
//		
//		out.println("...........");
//	
//		Map<String, Map<String, Double>> res2 = mapInstance(propId, instanceId, frameId,RoleSimilarityMethods.WN, WordNetSimilarityMethod.Resnik);
//			
//			for(Entry<String, Map<String, Double>> e : res2.entrySet()){
//				
//				out.println(e.getKey() + "\t" + e.getValue());
//				
//			}
//			
//			out.close();
	}
	
	public static void test(String propId,String frameId, int nrInstance, int depth,WordNetSimilarityMethod wnMethod,String resutlDir) throws FileNotFoundException{

		String lang = "en" ;
		WDArgumentMetadataExtractor.default_depth = depth;
		
		
		PrintWriter out = new PrintWriter(new File(resutlDir +"annotation_"+propId+"_"+frameId+"-"+wnMethod+"_d="+depth+".txt"));

		for (int i = 0; i < nrInstance; i++) {
		
			String instanceId = WikidataTraverser.getRandomInstance(propId, lang);
			Map<String, Map<String, Double>> res = mapInstance(propId, instanceId, frameId,RoleSimilarityMethods.WN,wnMethod);
			
			for(Entry<String, Map<String, Double>> e : res.entrySet()){
				out.println(e.getKey() + "\t" + e.getValue());
			}
			
			out.println("...................");
		}
		out.close();
	}
	
	public static void testStemOverlap(String propId,String frameId, int nrInstance, int depth,String resutlDir) throws FileNotFoundException{

		String lang = "en" ;
		WDArgumentMetadataExtractor.default_depth = depth;
		
		
		PrintWriter out = new PrintWriter(new File(resutlDir +"annotation_"+propId+"_"+frameId+"-"+"stem"+"_d="+depth+".txt"));

		for (int i = 0; i < nrInstance; i++) {
		
			String instanceId = WikidataTraverser.getRandomInstance(propId, lang);
			Map<String, Map<String, Double>> res = mapInstance(propId, instanceId, frameId,RoleSimilarityMethods.stemOverlap,null);
			
			for(Entry<String, Map<String, Double>> e : res.entrySet()){
				out.println(e.getKey() + "\t" + e.getValue());
			}
			
			out.println("...................");
		}
		out.close();
	}
	
	
	public static void test2(String propId,String frameId, int nrInstance, int depth,String resutlDir) throws FileNotFoundException{

		String lang = "en" ;
		WDArgumentMetadataExtractor.default_depth = depth;
		
		
		PrintWriter out = new PrintWriter(new File(resutlDir +"annotation_"+propId+"_"+frameId+"-"+"stemOverlap"+"_d="+depth+".txt"));

		for (int i = 0; i < nrInstance; i++) {
		
			String instanceId = WikidataTraverser.getRandomInstance(propId, lang);
			Map<String, Map<String, Double>> res = mapInstance(propId, instanceId, frameId,RoleSimilarityMethods.stemOverlap,null);
			
			for(Entry<String, Map<String, Double>> e : res.entrySet()){
				out.println(e.getKey() + "\t" + e.getValue());
			}
			
			out.println("...................");
		}
		out.close();
	}
	public static void main(String[] args) throws IOException {
//		P69	educated at	FN_SemanticPredicate_479	Education_teaching	1.0	 ALIAS
//P1066	student of	FN_SemanticPredicate_479	Education_teaching	2.0	 ALIAS
//		P22	father	FN_SemanticPredicate_689	Birth	1.0	 ALIAS
		
//		Map<String, Map<String, Double>> rs = mapProperty("P69", "FN_SemanticPredicate_479", RoleSimilarityMethods.stemOverlap,true);
////		
//		System.out.println(rs.get("Subject"));
//		System.out.println(rs.get("Object"));
//		
//		
//		rs = mapProperty("P69", "FN_SemanticPredicate_479", RoleSimilarityMethods.stemOverlap,false);
//////	
//		System.out.println(rs.get("Subject"));
//		System.out.println(rs.get("Object"));


//		testCasePropertyOnly(RoleSimilarityMethods.WN,true);
//		
//		List<AlignmentResult> r = findMatchingFrames("P7", RoleSimilarityMethods.stemOverlap,10);
//		for(AlignmentResult ar : r){
////		
//			System.out.println("Prop ID \t Frame ID \t ARG1 Role \t ARG2 Role");
//			List<String> subTopRoles = getTopRoles(ar.getMappings().get("Subject"),5);
//			List<String> objTopRoles = getTopRoles(ar.getMappings().get("Object"),5);
//			System.out.println(ar.getPropId() + "\t" + ar.getFrameId() + "\t" + subTopRoles+ "\t" +objTopRoles);
//		}
//		
		findMatchingFrames("test/output/all/wd_fn_wn_v4.txt",RoleSimilarityMethods.WN,10,3,true);
		
	}

	private static List<String> getTopRoles(Map<String, Double> map, int max) {
		List<String> topRoles = new ArrayList<String>();
		int index = 0 ;
		int count = 0 ;
		
		for(Entry<String, Double> e: map.entrySet()){
			
			if(count >= max){
				
				break;
			}
			
			topRoles.add(index++,e.getKey());
			count++;
		}
		
		return topRoles;
	}

	private static void testCase1() throws FileNotFoundException, IOException {
		int numberOfInstances= 5;
		int depth = 1;
		String testCaseFile = "test/input/test_cases.txt";
		String resultDir = "test/output/stemOverlap/";
		//Load test cases
		// Open the file
		FileInputStream fstream = new FileInputStream(testCaseFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		String strLine;
		//Read File Line By Line
		
		int i = 0;
		while ((strLine = br.readLine()) != null)   {
			
			i++;
			//ignore the first line
			if(i==1)
				continue;
//			System.out.println("Case " + (i-1));
				 
			String[] lineArr = strLine.split(",");
			String propertyID = lineArr[0];
			String frameID =  lineArr[2];
			
//			test(propertyID, frameID, numberOfInstances,depth,WordNetSimilarityMethod.JiangConrath,resultDir);
			
//			testStemOverlap(propertyID, frameID, numberOfInstances,depth,resultDir);
			
//			System.out.println(" WN");
//			Map<String, Map<String, Double>> rs = mapProperty(propertyID, frameID, RoleSimilarityMethods.WN);
//			System.out.println(rs.get("Subject"));
//			System.out.println(rs.get("Object"));
			
//			System.out.println(" Stem Overlap");
			Map<String, Map<String, Double>> rs = mapProperty(propertyID, frameID, RoleSimilarityMethods.stemOverlap,false);
			
			System.out.println("Sub:  \t" + rs.get("Subject"));
			System.out.println("Obj:  \t" + rs.get("Object"));
			
			System.out.println("......");
		}
		
		br.close();
	}
	
	
	private static void testCasePropertyOnly(RoleSimilarityMethods simMethod, boolean loadFromDB) throws FileNotFoundException, IOException {
		
		String testCaseFile = "test/input/test_cases_new.txt";
		String resultDir = null;

		String resultFileName = null;

		if(simMethod.equals(RoleSimilarityMethods.WN)){
			
			resultFileName = "test_case_wn."  + System.currentTimeMillis()+ ".txt";
			resultDir = "test/output/WN/";

		}
		else{
			resultFileName = "test_case_stemOverlap" +  System.currentTimeMillis() + ".txt";
			resultDir = "test/output/stemOverlap/";

		}

		String finalFileName = resultDir+resultFileName;
		
		if(new File(finalFileName).exists()){
			
//			int version = Integer.valueOf(finalFileName.substring(finalFileName.indexOf("_v")+2,finalFileName.indexOf(".txt")));
			
//			finalFileName = finalFileName.replace("v_" + version + ".txt","v_" + (version+1) + ".txt");
			
			finalFileName = finalFileName.replace(".txt","_v.txt");

		}
		
		PrintWriter out = new PrintWriter(new File(finalFileName));
		//Load test cases
		
		FileInputStream fstream = new FileInputStream(testCaseFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		String strLine;
		int i = 0;
		while ((strLine = br.readLine()) != null)   {
			
			i++;
			//ignore the first line
			if(i==1)
				continue;
			System.out.println("Case " + (i-1));
				 
			String[] lineArr = strLine.split(",");
			String propertyID = lineArr[0];
			String frameID =  lineArr[2];

			Map<String, Map<String, Double>> rs = mapProperty(propertyID, frameID, simMethod,loadFromDB);
			

			System.out.println("FrameID:  \t" + frameID);
			System.out.println("PropertyID:  \t" + propertyID);
			System.out.println("Sub:  \t" + rs.get("Subject"));
			System.out.println("Obj:  \t" + rs.get("Object"));
			System.out.println("......");
			
			out.println(frameID  + "\t" + propertyID + "\t" + "Sub:" + rs.get("Subject") + "\t Obj:" + rs.get("Object"));
			
			
		}
		
		br.close();
		out.close();
	}
	

	private static void testCaseSimpleStemOverlap(String propId, String frameId, int depth, int nrInstances) throws FileNotFoundException, IOException {
		
		for (int i = 0; i < nrInstances; i++) {
			
			String instanceId = WikidataTraverser.getRandomInstance(propId, "en");
			
			Map<String, Map<String, Double>> res = mapInstance(propId, instanceId, frameId,RoleSimilarityMethods.stemOverlap,null);
			
			for(Entry<String, Map<String, Double>> e : res.entrySet()){
				System.out.println(e.getKey() + "\t" + e.getValue());
			}
		
		}
		
		
	}
	private static void testCaseSimpleWN(String propId, String frameId, int depth, int nrInstances) throws FileNotFoundException, IOException {
		
		for (int i = 0; i < nrInstances; i++) {
			
			String instanceId = WikidataTraverser.getRandomInstance(propId, "en");
			
			Map<String, Map<String, Double>> res = mapInstance(propId, instanceId, frameId,RoleSimilarityMethods.WN,WordNetSimilarityMethod.Resnik);
			
			for(Entry<String, Map<String, Double>> e : res.entrySet()){
				System.out.println(e.getKey() + "\t" + e.getValue());
			}
		
		}
		
		
	}
}
