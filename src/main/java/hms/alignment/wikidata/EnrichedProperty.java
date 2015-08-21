package hms.alignment.wikidata;

import hms.StanfordNLPTools;
import hms.wikidata.dbimport.JacksonDBAPI;
import hms.wikidata.model.StructuralPropertyMapper;
import hms.wn.WordNetManager;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class EnrichedProperty {

	String propId;
	String propLabel ;
	String propDesc ;
	
	List<String> propAliases;
	List<String> instanceOf;
	List<String> subjectItems;
	List<String> inverse;
	List<String> fatherProperty;
	
	/**
	 * Subject types as mined from the property structure
	 */
	Set<String> subjectTypes;
	
	/**
	 * Object types as mined from the property structure
	 */
	Set<String> objectTypes;
	
	
	Set<String> subjectTypesDescriptions;
	Set<String> objectTypesDescriptions;
	
	/**
	 * Subject types as mined from the property label
	 */
	Set<String> subjectTypesByLabel;
	/**
	 * Object types as mined from the property label
	 */
	Set<String> objectTypesByLable;
	
	/**
	 * Types of subject and object as mined from the property description
	 */
	WDDescription descriptionArguments;
	
	
	public EnrichedProperty(String propId){
		this.propId = propId;
		propLabel = JacksonDBAPI.getItemLabel(propId, "en");
		propDesc = JacksonDBAPI.getItemDescription(propId, "en");
		propAliases= JacksonDBAPI.getItemAliases(propId, "en");
		
		fatherProperty= JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.subPropertyOf);
		instanceOf=  JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.instanceOf);
		subjectItems=  JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.subjectItem);
		
		subjectTypes = new HashSet<String>();
		objectTypes = new HashSet<String>();
		subjectTypesDescriptions = new HashSet<String>();
		objectTypesDescriptions = new HashSet<String>();
		subjectTypesByLabel = new HashSet<String>();
		objectTypesByLable= new HashSet<String>();
		inverse=  JacksonDBAPI.getClaimRange(propId,StructuralPropertyMapper.inverseOf);
		
		
	}
	
	
	
	public Set<String> getSubjectTypesByLabel() {
		return subjectTypesByLabel;
	}



	public void setSubjectTypesByLabel(Set<String> subjectTypesByLabel) {
		this.subjectTypesByLabel = subjectTypesByLabel;
	}



	public Set<String> getObjectTypesByLable() {
		return objectTypesByLable;
	}



	public void setObjectTypesByLable(Set<String> objectTypesByLable) {
		this.objectTypesByLable = objectTypesByLable;
	}



	/**
	 * This method instantiate the inverse property of the current property according to 
	 * the procedure defined in the constructor. After that, the subject and object types of 
	 * the inverse property are minded and add to that of the current property using the inverse role.
	 * That means subject types of the inverse property are added to the object types of the current property and
	 * object types of the inverse property are added to the subject types of the current property
	 */
	public void handleInverseProperties() {

		if(inverse.size()>0){
			String inversePropId = inverse.get(0);
			EnrichedProperty ip = new EnrichedProperty(inversePropId);
			ip.mineSubjectTypesFromPropertyStructure();
			ip.mineObjectTypesFromPropertyStructure();
			
			this.getSubjectTypes().addAll(ip.getObjectTypes());
			this.getObjectTypes().addAll(ip.getSubjectTypes());
			this.getSubjectTypesDescriptions().addAll(ip.getObjectTypesDescriptions());
			this.getObjectTypesDescriptions().addAll(ip.getSubjectTypesDescriptions());
			
		}

	}

	/**
	 * The method add the aliases, the father classes and subject items of the 
	 * father property to these of the current property
	 */
	public void handleFatherProperties() {
		
		if(fatherProperty.size()>0){

			String fpId = fatherProperty.get(0);
			EnrichedProperty fp = new EnrichedProperty(fpId);
			this.propAliases.addAll(fp.getPropAliases());
			this.instanceOf.addAll(fp.getInstanceOf());
			this.subjectItems.addAll(fp.subjectItems);
			
		}
	}


	public Set<String> getSubjectTypesDescriptions() {
		return subjectTypesDescriptions;
	}




	public void setSubjectTypesDescriptions(Set<String> subjectTypesDescriptions) {
		this.subjectTypesDescriptions = subjectTypesDescriptions;
	}




	public Set<String> getObjectTypesDescriptions() {
		return objectTypesDescriptions;
	}




	public void setObjectTypesDescriptions(Set<String> objectTypesDescriptions) {
		this.objectTypesDescriptions = objectTypesDescriptions;
	}




	public String getPropId() {
		return propId;
	}


	public void setPropId(String propId) {
		this.propId = propId;
	}


	public String getPropLabel() {
		return propLabel;
	}


	public void setPropLabel(String propLabel) {
		this.propLabel = propLabel;
	}


	public String getPropDesc() {
		return propDesc;
	}


	public void setPropDesc(String propDesc) {
		this.propDesc = propDesc;
	}


	public List<String> getPropAliases() {
		return propAliases;
	}


	public void setPropAliases(List<String> propAliases) {
		this.propAliases = propAliases;
	}


	public List<String> getInstanceOf() {
		return instanceOf;
	}


	public void setInstanceOf(List<String> instanceOf) {
		this.instanceOf = instanceOf;
	}


	public List<String> getSubjectItems() {
		return subjectItems;
	}


	public void setSubjectItems(List<String> subjectItems) {
		this.subjectItems = subjectItems;
	}


	public List<String> getInverse() {
		return inverse;
	}


	public void setInverse(List<String> inverse) {
		this.inverse = inverse;
	}


	public List<String> getFatherProperty() {
		return fatherProperty;
	}


	public void setFatherProperty(List<String> fatherProperty) {
		this.fatherProperty = fatherProperty;
	}


	public Set<String> getSubjectTypes() {
		return subjectTypes;
	}


	public void setSubjectTypes(Set<String> subjectTypes) {
		this.subjectTypes = subjectTypes;
	}


	public Set<String> getObjectTypes() {
		return objectTypes;
	}


	public void setObjectTypes(Set<String> objectTypes) {
		this.objectTypes = objectTypes;
	}


	public void mineSubjectTypesFromPropertyStructure(){
		
		
		for(String p : instanceOf){
			String fatherClass = JacksonDBAPI.getItemLabel(p, "en");
			if(fatherClass!=null){
//				fatherClass = WDArgumentMetadataExtractor.cleanClassLabel(fatherClass);
//				subjectTypes.add(fatherClass);
				
				//Get the facet
				List<String> instanceOfFacet = JacksonDBAPI.getClaimRange(p,StructuralPropertyMapper.facetOf);
				for(String facet: instanceOfFacet){
					subjectTypes.add(JacksonDBAPI.getItemLabel(facet, "en"));
					subjectTypes.addAll(JacksonDBAPI.getItemAliases(facet, "en"));
//					Set<String> facetTypes = WDArgumentMetadataExtractor.getEntityHigherClasses(facet, 1, "en", StructuralPropertyMapper.structuarlItemRelatedPropertiesMap.keySet());	
//					subjectTypes.addAll(facetTypes);
					subjectTypesDescriptions.add(JacksonDBAPI.getItemDescription(facet, "en"));
					
				}
				
			}
		}
		for(String a : propAliases){
			if(a.endsWith("of")){
				Map<String, List<String>> r = StanfordNLPTools.identifyHeadWord(a);
				
				for(Entry<String, List<String>> e: r.entrySet()){
					
					for(String headword: e.getValue()){
						subjectTypes.add(headword);

					}
				}
			}
		}
	}
	
	public void mineObjectTypesFromPropertyStructure(){
		
		for(String p : subjectItems){
			String subjectItem = JacksonDBAPI.getItemLabel(p, "en");
			objectTypes.add(subjectItem);
			objectTypes.addAll(JacksonDBAPI.getItemAliases(p, "en"));
//			Set<String> facetTypes = WDArgumentMetadataExtractor.getEntityHigherClasses(p, 5, "en", StructuralPropertyMapper.structuarlItemRelatedPropertiesMap.keySet());	
//			objectTypes.addAll(facetTypes);
			
			objectTypesDescriptions.add(JacksonDBAPI.getItemDescription(p, "en"));

		}	
		
		for(String a : propAliases){
			if(!a.endsWith("of")){
				Map<String, List<String>> r = StanfordNLPTools.identifyHeadWord(a);
				
				for(Entry<String, List<String>> e: r.entrySet()){
					
					for(String headword: e.getValue()){
						objectTypes.add(headword);

					}
				}
			}
		}
		
	}
	
	public void mineSubjectObjectTypesFromPropertyDescription(){
		
		this.descriptionArguments =  WDDescriptionArgumentTypeExtractor.extractArgumentTypesFromDescription(this.propId);
		
	}
	
	public void mineSubjectTypesFromPropertyLabel(){
		
		List<String> lemmas = StanfordNLPTools.lemmatize(this.propLabel);
		Set<String> acceptableDerivations = new HashSet<String>(); 
	
		for(String lemma : lemmas){
			
			Set<String> derivations = WordNetManager.getDerivations(lemma);
			for(String derivation : derivations){
				
				if(!derivation.endsWith("ion") && !derivation.endsWith("ing") && !derivation.endsWith("gy") && !derivation.endsWith("ity") && !derivation.endsWith("ics")){
					acceptableDerivations.add(derivation);
					

				}
				
			}
		}
		
		if(acceptableDerivations.size() > 0 ){
			if(propLabel.endsWith(" by") || propLabel.endsWith(" at")){
				this.subjectTypesByLabel.addAll(acceptableDerivations);
			}
			else{
				this.objectTypesByLable.addAll(acceptableDerivations);
			}
		}
		else if(acceptableDerivations.size() == 0 && lemmas.size() == 1){
			
			this.objectTypesByLable.add(lemmas.get(0));

		}
		
	}
	
	
	
	public WDDescription getDescriptionArguments() {
		
		return descriptionArguments;
	}

	public void setDescriptionArguments(WDDescription descriptionArguments) {
		this.descriptionArguments = descriptionArguments;
	}

	public void enrich(){
		this.handleFatherProperties();
		this.mineSubjectTypesFromPropertyStructure();
		this.mineObjectTypesFromPropertyStructure();
		this.handleInverseProperties();
		this.mineSubjectObjectTypesFromPropertyDescription();
		this.mineSubjectTypesFromPropertyLabel();
	}
	
	public static void main(String[] args) {
		
		
		String[] testCases = {"P7","P22","P106","P108","P69","P157","P112","P802","P115","P118","P634","P735"};
//		String[] testCases = {"P634"};

		for(String pId : testCases){
			
			EnrichedProperty p = new EnrichedProperty(pId);
			
			p.enrich();
	
			
			System.out.println("Property: " + p.getPropLabel() + ": \t " + p.getPropDesc() );
			System.out.println("Subject Types: " + p.getSubjectTypes());
//			System.out.println("Subject Types Desc: " + p.subjectTypesDescriptions);
			System.out.println("Subject Types Extracted From the Desc: " + p.descriptionArguments.getArg1Types());
			System.out.println("Subject types extracted from the property label: " + p.subjectTypesByLabel);
			
			
			System.out.println("Object Types: "  + p.getObjectTypes());
//			System.out.println("Object Types Desc: "  + p.objectTypesDescriptions);
			System.out.println("Object Types Extracted From the Desc: " + p.descriptionArguments.getArg2Types());
			System.out.println("Object types extracted from the property label: " + p.objectTypesByLable);
			
			System.out.println(".....");
		}

		
	}
	
}
