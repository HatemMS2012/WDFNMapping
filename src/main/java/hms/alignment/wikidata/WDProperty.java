package hms.alignment.wikidata;

import java.util.List;

public class WDProperty {

	String label ;
	String description ;
	List<String> aliases ;
	
	//Structural characteristics
	List<String> subjectType ; 
	List<String> fatherType ;

	List<String> equivalentProperty;
	List<String> fatherProperty ;
	List<String> relatedProperties ;
	List<String> objectType  ;

	List<WDProperty> inversePropery ;
	
	

	
	public List<WDProperty> getInversePropery() {
		return inversePropery;
	}
	public void setInversePropery(List<WDProperty> inversePropery) {
		this.inversePropery = inversePropery;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<String> getAliases() {
		return aliases;
	}
	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}
	public List<String> getSubjectType() {
		return subjectType;
	}
	public void setSubjectType(List<String> fatherClass) {
		this.subjectType = fatherClass;
	}
	public List<String> getFatherType() {
		return fatherType;
	}
	public void setFatherType(List<String> fatherType) {
		this.fatherType = fatherType;
	}
	
	public List<String> getEquivalentProperty() {
		return equivalentProperty;
	}
	public void setEquivalentProperty(List<String> equivalentProperty) {
		this.equivalentProperty = equivalentProperty;
	}
	public List<String> getFatherProperty() {
		return fatherProperty;
	}
	public void setFatherProperty(List<String> fatherProperty) {
		this.fatherProperty = fatherProperty;
	}
	public List<String> getRelatedProperties() {
		return relatedProperties;
	}
	public void setRelatedProperties(List<String> relatedProperties) {
		this.relatedProperties = relatedProperties;
	}
	public List<String> getObjectType() {
		return objectType;
	}
	public void setObjectType(List<String> subjectItems) {
		this.objectType = subjectItems;
	}
	
	@Override
	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append("Label:").append(label).append("\n").
		append("Description:").append(description).append("\n").
		append("Aliases:").append(aliases).append("\n").
		append("Father Class:").append(subjectType).append("\n").
		append("Father Type:").append(fatherType).append("\n").
		append("Inverse Propery:").append(inversePropery).append("\n").
		append("Equivalent Property:").append(equivalentProperty).append("\n").
		append("Father Property:").append(fatherProperty).append("\n").
		append("Related Properties:").append(relatedProperties).append("\n").
		append("Subject Items:").append(objectType).append("\n");
		
		return str.toString();
	}
	
	
}
