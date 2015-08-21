package hms.alignment.wikidata;

import java.util.List;
import java.util.Map;

public class WDArgumentMetaData {

	
	

	private String id;
	private String label;
	private String description;
	private List<String> aliases;
	
	private List<WDArgumentMetaData> father ;
	private WDProperty propertyInvolvedIn;
	
	private Map<String,String> inferredRoleMetaData;
	
	
	
	

	public Map<String, String> getInferredRoleMetaData() {
		return inferredRoleMetaData;
	}

	public void setInferredRoleMetaData(Map<String, String> inferredRoleMetaData) {
		this.inferredRoleMetaData = inferredRoleMetaData;
	}

	public WDProperty getPropertyInvolvedIn() {
		return propertyInvolvedIn;
	}

	public void setPropertyInvolvedIn(WDProperty propertyInvolvedIn) {
		this.propertyInvolvedIn = propertyInvolvedIn;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public List<String> getAliases() {
		return aliases;
	}

	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}

	public List<WDArgumentMetaData> getFather() {
		return father;
	}

	public void setFather(List<WDArgumentMetaData> father) {
		this.father = father;
	}
	
	
	
}
