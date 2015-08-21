package hms.alignment.wikidata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import hms.StanfordNLPTools;
import hms.wikidata.dbimport.JacksonDBAPI;
import hms.wikidata.model.StructuralPropertyMapper;

public class WDArgumentMetadataExtractor {

	
	
	private static List<String> STOP_LABEL_PARTS = new ArrayList<String>();
	
	static{


		STOP_LABEL_PARTS.add("Wikidata property to indicate");
		STOP_LABEL_PARTS.add("properties for items about");
		STOP_LABEL_PARTS.add("Wikidata property for");
		STOP_LABEL_PARTS.add("items about");
		STOP_LABEL_PARTS.add("Wikidata property about");
		
		
	}
	private static final String EN ="en";
	private static Set<String> labelList = new HashSet<String>();
	public static int default_depth = 1;
	private static Set<String> defaulStructProperties = StructuralPropertyMapper.structuarlItemRelatedPropertiesMap.keySet();
	
	
	
	public static WDProperty extractPropertyMetaDataEnrichWithInverse(String propId){
		WDProperty property = extractPropertyMetaData(propId);
		
		List<String> inversePropery  = JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.inverseOf);
		
		if(inversePropery.size() > 0){
			
			List<WDProperty> inversePropertyList = new ArrayList<WDProperty>();
			for(String invPropId : inversePropery){
				
				WDProperty invProperty = extractPropertyMetaData(invPropId);
				inversePropertyList.add(invProperty);
			}
			property.setInversePropery(inversePropertyList);
		}

		return property;
	}
	
	public static WDProperty extractPropertyMetaData(String propId){
		
		String label = JacksonDBAPI.getItemLabel(propId, EN);
		String description = JacksonDBAPI.getItemDescription(propId, EN);
		List<String> aliases = JacksonDBAPI.getItemAliases(propId, EN);
		
		//Structural characteristics
		List<String> fatherClass = JacksonDBAPI.getClaimRange(propId, StructuralPropertyMapper.instanceOf);; //Can be used to annotate the subject of the predicate
		List<String> fatherType = JacksonDBAPI.getClaimRange(propId, StructuralPropertyMapper.subclassOf);
		
		List<String> equivalentProperty  = JacksonDBAPI.getClaimRange(propId, StructuralPropertyMapper.equivalentProperty);
		List<String> fatherProperty = JacksonDBAPI.getClaimRange(propId, StructuralPropertyMapper.subPropertyOf);
		List<String> relatedProperties = JacksonDBAPI.getClaimRange(propId, StructuralPropertyMapper.seeAlso);
		List<String> subjectItems  = JacksonDBAPI.getClaimRange(propId, StructuralPropertyMapper.subjectItem); //Can be used to annotate the object of the predicate
		
		
		WDProperty property = new WDProperty();
		property.setLabel(label);
		property.setDescription(description);
		property.setAliases(aliases);
		property.setSubjectType(fatherClass);
		property.setFatherType(fatherType);
		
		property.setEquivalentProperty(equivalentProperty);
		property.setFatherProperty(fatherProperty);
		property.setRelatedProperties(relatedProperties);
		property.setObjectType(subjectItems);
		
		
		
		return property;
		
		
	}
	/**
	 * Extract metadata about an WD item that is involved as argument in a given predicate (property)
	 * @param entityId
	 * @param propertyInvolvedIn
	 * @param isSubject
	 * @return
	 */
	public static WDArgumentMetaData extractItemMetadata(String entityId, String propertyInvolvedIn){
		
		WDArgumentMetaData argMetadata = new WDArgumentMetaData();
			
		String label = JacksonDBAPI.getItemLabel(entityId, EN);
		String description = JacksonDBAPI.getItemDescription(entityId, EN);
		List<String> aliases = JacksonDBAPI.getItemAliases(entityId, EN);
		
		argMetadata.setId(entityId);
		argMetadata.setLabel(label);
		argMetadata.setAliases(aliases);
		argMetadata.setDescription(description);
		
		//Get property inferred type
		if(propertyInvolvedIn!=null){
			
			WDProperty property = extractPropertyMetaDataEnrichWithInverse(propertyInvolvedIn);
			
			argMetadata.setPropertyInvolvedIn(property);
			
		}
		
		
		
		Set<String> fatherClasses = getEntityHigherClasses(entityId, default_depth, EN, defaulStructProperties);

		List<WDArgumentMetaData> fathers = new ArrayList<WDArgumentMetaData>();
		
		
		for(String fatherClassId : fatherClasses){
			
			String fatherId = fatherClassId.split(":")[0];
			String fatherLabel = fatherClassId.split(":")[1];
			String fatherDescription = JacksonDBAPI.getItemDescription(fatherId, EN);
			List<String> fatherAliases = JacksonDBAPI.getItemAliases(fatherId, EN);
			
			
			WDArgumentMetaData fatherMetadata = new WDArgumentMetaData();
			
			fatherMetadata.setId(fatherId);
			fatherMetadata.setLabel(fatherLabel);
			fatherMetadata.setAliases(fatherAliases);
			fatherMetadata.setDescription(fatherDescription);
			fathers.add(fatherMetadata);
		}
		argMetadata.setFather(fathers);
		
		return argMetadata;
	}
	
	
	/**
	 * Generates a tree representation for a given wikidata entity.
	 * The method recursively extract the properties of a given entity and their ranges and apply the 
	 * same procedure on the ranges
	 * @param itemId The item you want to create the tree for
	 * @param depth The recursive depth
	 * @param lang language of the labels
	 * @param targetProp The set of properties you want to consider in the hierarchy
	 */
	public static void getEntityClasses(String itemId, int depth, String lang, Set<String> targetProp){
		
//		System.out.println(tagPropSet.size());
		
		if(depth <= 0)
			
			return;
		
		Map<String, String> claimValueMap = JacksonDBAPI.getEntityClaimsIdsAndValues(itemId);
			
		if(claimValueMap.size() == 0){
			
			return;
		}
	
		depth--;
		
		for(String claimId : claimValueMap.keySet()){
			
			String claimIdWithouthSource = claimId.split("#")[0];
			
			if(targetProp.contains(claimIdWithouthSource)){

				String claimValMain = claimValueMap.get(claimId);
			
				if(lang !=null){
			
					String claimVal = claimValueMap.get(claimId);
									
					if(claimVal.startsWith("Q")||claimVal.startsWith("P")){
						
						 String claimValLab = JacksonDBAPI.getItemLabel(claimVal,lang);
						 if(claimValLab!=null)
							 labelList.add(claimVal + ":" +claimValLab);
					}
					
					
				}
				
				getEntityClasses(claimValMain,depth,lang,targetProp);
			}
		}
		
	}
	
	
	public static Set<String> getEntityHigherClasses(String itemId, int depth, String lang, Set<String> targetProp){
		
		labelList.clear();
		
		getEntityClasses(itemId, depth, lang, targetProp);
		
		return labelList;
	}
	
	
	/**
	 * Extract additional metadata for an item participating in a given property as a subject
	 * The added metadata contains: 
	 * 1- the father class of the predicate (normally describes the subject of the property)  with labels, description and aliases
	 * 2- the description and aliases of the inverse property (if any)
	 * 3- the items defined via "subject item" property of the inverse property with their labels, aliases and descriptions
	 * @param item
	 */
	public static void getItemInferredRoleMetadataForSubject(WDArgumentMetaData item) {

		Map<String, String> subjectTypeMap = new HashMap<String, String>();

		WDProperty mainProperty = item.getPropertyInvolvedIn();

		
		//Extract information as described in (1)
		List<String> subjectTypeIds = mainProperty.getSubjectType();

		for (String subjectTypeId : subjectTypeIds) {

			String subjectLabel = JacksonDBAPI.getItemLabel(subjectTypeId, EN);
			
			if(subjectLabel!=null){
				subjectLabel = cleanClassLabel(subjectLabel);
				subjectTypeMap.put("main-prop-subj-" + subjectTypeId, subjectLabel);
			}
			
			List<String> objectTaypeAliases = JacksonDBAPI.getItemAliases(subjectTypeId, EN);
			int j = 1 ;
			
			for(String a : objectTaypeAliases ){
				a = cleanClassLabel(a);
				subjectTypeMap.put("main-prop-obj-alias-"+j, a);
				j++;
			}
			String objectTypeDesc = JacksonDBAPI.getItemDescription(subjectTypeId, EN);
			if(objectTypeDesc!=null){
				objectTypeDesc = cleanClassLabel(objectTypeDesc);
				subjectTypeMap.put("main-prop-subj-description-"+subjectTypeId, objectTypeDesc);
			}

		}

		//
		List<WDProperty> inverseProperties = mainProperty.getInversePropery();
		
		if (inverseProperties != null) {

			//For each inverse property
			for (WDProperty inverseProperty : inverseProperties) {
				//Get the "subject item" targets
				for (String furtherTypes : inverseProperty.getObjectType()) {
					
					String subjectLabel = JacksonDBAPI.getItemLabel(furtherTypes, EN);
				
					if(subjectLabel!=null){
						subjectLabel = cleanClassLabel(subjectLabel);
						
						subjectTypeMap.put("inverse-prop-subj-" + furtherTypes, subjectLabel);
					}
					String objectDescription = JacksonDBAPI.getItemDescription(furtherTypes, EN);
				
					if(objectDescription!=null){
						objectDescription = cleanClassLabel(objectDescription);
						subjectTypeMap.put("inverse-prop-subj-description-"+ furtherTypes, objectDescription);
					}
					
					List<String> objectAlisases = JacksonDBAPI.getItemAliases(furtherTypes, EN);
					
					int i = 1 ;
					for(String a : objectAlisases){
						a = cleanClassLabel(a);
						subjectTypeMap.put("inverse-prop-subj-alias-"+i, a);
					}

				}
				if(inverseProperty.getDescription()!=null){
					subjectTypeMap.put("inverse-prop-description",cleanClassLabel(inverseProperty.getDescription()));
				}	
				
				int i = 1 ;
				for(String a : inverseProperty.getAliases()){
					a = cleanClassLabel(a);
					subjectTypeMap.put("inverse-prop-alias-"+i, a);
					i++;
				}			
			}
		}

		item.setInferredRoleMetaData(subjectTypeMap);

	}
	
	/**
	 * Extract additional metadata for an item participating in a given property as a subject
	 * The added metadata contains: 
	 * 1- the father class of the predicate (normally describes the subject of the property)  with labels, description and aliases
	 * 2- the description and aliases of the inverse property (if any)
	 * 3- the items defined via "subject item" property of the inverse property with their labels, aliases and descriptions
	 * @param item
	 */
	public static void getItemInferredRoleMetadataForSubjectSimple(WDArgumentMetaData item) {

		Map<String, String> subjectTypeMap = new HashMap<String, String>();

		WDProperty mainProperty = item.getPropertyInvolvedIn();
		
		//Extract information as described in (1)
		List<String> subjectTypeIds = mainProperty.getSubjectType();

		for (String subjectTypeId : subjectTypeIds) {

			String subjectLabel = JacksonDBAPI.getItemLabel(subjectTypeId, EN);
			
			if(subjectLabel!=null){
				subjectLabel = cleanClassLabel(subjectLabel);
				subjectTypeMap.put("main-prop-subj-" + subjectTypeId, subjectLabel);
			}
		}

		//
		List<WDProperty> inverseProperties = mainProperty.getInversePropery();
		
		if (inverseProperties != null) {

			//For each inverse property
			for (WDProperty inverseProperty : inverseProperties) {
				//Get the "subject item" targets
				for (String furtherTypes : inverseProperty.getObjectType()) {
					
					String subjectLabel = JacksonDBAPI.getItemLabel(furtherTypes, EN);
					
					if(subjectLabel!=null){
						subjectLabel = cleanClassLabel(subjectLabel);
						subjectTypeMap.put("inverse-prop-subj-" + furtherTypes, subjectLabel);
					}
				}			
			}
		}

		item.setInferredRoleMetaData(subjectTypeMap);

	}

	public static String cleanClassLabel(String subjectLabel) {
		
//	
		for(String stopLabelPart : STOP_LABEL_PARTS){
			
			subjectLabel = subjectLabel.replace(stopLabelPart,"");
			
		}
		subjectLabel = subjectLabel.trim();
		
		
		return subjectLabel;
	}
	
	/**
	 * Extract additional metadata for an item participating in a given property as an object
	 * The added metadata contains: 
	 * 1- the description and aliases of the property 
	 * 2- the items defined via "subject item" property of the inverse property with their labels, aliases and descriptions
	 * 3- the father class of the inverse property (if any) with labels, description and aliases
	 * @param item
	 */
	public static void getItemInferredRoleMetadataForObject(WDArgumentMetaData item) {

		Map<String, String> objectTypeMap = new HashMap<String, String>();

		WDProperty mainProperty = item.getPropertyInvolvedIn();

		
		//add main property description and aliases
		if(mainProperty.getDescription()!=null){
			objectTypeMap.put("main-prop-description", cleanClassLabel(mainProperty.getDescription()));
		}
		
		int i = 1 ;
		for(String a : mainProperty.getAliases()){
			a = cleanClassLabel(a);
			objectTypeMap.put("main-prop-alias-"+i, a);
			i++;
		}
		
		List<String> objectTypeIds = mainProperty.getObjectType();

		//subject items of the main property
		for (String objectTypeId : objectTypeIds) {

			String objectLabel = JacksonDBAPI.getItemLabel(objectTypeId, EN);
			
			if(objectLabel!=null){
				objectLabel = cleanClassLabel(objectLabel);
	
				objectTypeMap.put("main-prop-obj-"+objectTypeId, objectLabel);
			}
			
			List<String> objectTaypeAliases = JacksonDBAPI.getItemAliases(objectTypeId, EN);
			int j = 1 ;
			for(String a : objectTaypeAliases ){
				a = cleanClassLabel(a);
				
				objectTypeMap.put("main-prop-obj-alias-"+j, a);
				j++;
			}
			
			String objectTypeDesc = JacksonDBAPI.getItemDescription(objectTypeId, EN);
			
			if(objectTypeDesc!=null){
				objectTypeDesc = cleanClassLabel(objectTypeDesc);
				
				objectTypeMap.put("main-prop-obj-description-"+objectTypeId, objectTypeDesc);
			}

		}

		List<WDProperty> inverseProperties = mainProperty.getInversePropery();
	
		if (inverseProperties != null) {

			for (WDProperty inverseProperty : inverseProperties) {

				for (String furtherTypes : inverseProperty.getSubjectType()) {
					String objectLabel = JacksonDBAPI.getItemLabel(furtherTypes, EN);
					
					if(objectLabel!=null){
						objectLabel = cleanClassLabel(objectLabel);
						objectTypeMap.put(furtherTypes, objectLabel);
					}
					
					String objectDescription = JacksonDBAPI.getItemDescription(furtherTypes, EN);
					
					if(objectDescription!=null){
						objectDescription = cleanClassLabel(objectDescription);
						objectTypeMap.put("inverse-prop-obj-description-"+furtherTypes, objectDescription);
					}
					
					List<String> objectTaypeAliases = JacksonDBAPI.getItemAliases(furtherTypes, EN);
					int j = 1 ;
					for(String a : objectTaypeAliases ){
						a = cleanClassLabel(a);
						objectTypeMap.put("inverse-prop-alias-"+j, a);
						j++;
					}

				}
			}
		}

		item.setInferredRoleMetaData(objectTypeMap);

	}
	
	/**
	 * Extract additional metadata for an item participating in a given property as an object
	 * The added metadata contains: 
	 * 1- the description and aliases of the property 
	 * 2- the items defined via "subject item" property of the inverse property with their labels, aliases and descriptions
	 * 3- the father class of the inverse property (if any) with labels, description and aliases
	 * @param item
	 */
	public static void getItemInferredRoleMetadataForObjectSimple(WDArgumentMetaData item) {

		Map<String, String> objectTypeMap = new HashMap<String, String>();

		WDProperty mainProperty = item.getPropertyInvolvedIn();

		
		List<String> objectTypeIds = mainProperty.getObjectType();

		//subject items of the main property
		for (String objectTypeId : objectTypeIds) {

			String objectLabel = JacksonDBAPI.getItemLabel(objectTypeId, EN);
			
			if(objectLabel!=null){
				objectLabel = cleanClassLabel(objectLabel);
	
				objectTypeMap.put("main-prop-obj-"+objectTypeId, objectLabel);
			}
			
		}

		List<WDProperty> inverseProperties = mainProperty.getInversePropery();
	
		if (inverseProperties != null) {

			for (WDProperty inverseProperty : inverseProperties) {

				for (String furtherTypes : inverseProperty.getSubjectType()) {
					String objectLabel = JacksonDBAPI.getItemLabel(furtherTypes, EN);
					
					if(objectLabel!=null){
						objectLabel = cleanClassLabel(objectLabel);
						objectTypeMap.put("inverse-prop-subj-" + furtherTypes, objectLabel);
					}
				
				}
			}
		}

		item.setInferredRoleMetaData(objectTypeMap);

	}
	

	public static void generateRichPropertyRepresentation(String propId){
		
		
		String propLabel = JacksonDBAPI.getItemLabel(propId, "en");
		String propDesc = JacksonDBAPI.getItemDescription(propId, "en");
		List<String> propAliases= JacksonDBAPI.getItemAliases(propId, "en");
		
		System.out.println(propLabel + "\t" + propAliases + "\t" + propDesc);
		
		
	
		
		List<String> inverse=  JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.inverseOf);
		for(String p : inverse){
			String inverseProp = JacksonDBAPI.getItemLabel(p, "en");
			System.out.println("Inverse: " + inverseProp);
		}
		
		List<String> fatherProperty= JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.subPropertyOf);
		for(String p : fatherProperty){
			String fatherProp = JacksonDBAPI.getItemLabel(p, "en");
			System.out.println("Father Property: " + fatherProp);
		}
		
		//Transfer Metadata to Subject
		
		String subjectType = "Subject Metadata"; 
		System.out.println(subjectType);
		List<String> instanceOf=  JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.instanceOf);
		for(String p : instanceOf){
			String fatherClass = JacksonDBAPI.getItemLabel(p, "en");
			System.out.println("Father Class: " + fatherClass);
		}
		//Metadata extracted from aliases
		for(String a : propAliases){
			if(a.endsWith("of")){
				Map<String, List<String>> r = StanfordNLPTools.identifyHeadWord(a);
				System.out.println("Subject is: " + r.values()); 
			}
		}
		
		
		//Object metadata
		System.out.println("Object Metadata");

		List<String> subjectItems=  JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.subjectItem);
		for(String p : subjectItems){
			String subjectItem = JacksonDBAPI.getItemLabel(p, "en");
			System.out.println("Subject Item Class: " + subjectItem);
		}
		for(String a : propAliases){
			if(!a.endsWith("of")){
				Map<String, List<String>> r = StanfordNLPTools.identifyHeadWord(a);
				System.out.println("Object is: " + r.values()); 
			}
		}
		
		
	}
	
	public static void main(String[] args) {
		
		generateRichPropertyRepresentation("P108");
	}
}
