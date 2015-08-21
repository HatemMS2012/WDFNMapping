package hms.alignment.wikidata;

import java.util.List;

public class WDDescription {

	
	private String id;
	private String label;
	
	private String description;
	private String mainSentence;
	private List<String> arg1Types;
	private List<String> arg2Types;
	private String predicate;
	public String getId() {
		return id;
	}
	
	
	public void setId(String id) {
		this.id = id;
	}
	public String getDescription() {
		return description;
	}
	
	
	public String getLabel() {
		return label;
	}


	public void setLabel(String label) {
		this.label = label;
	}


	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getPredicate() {
		return predicate;
	}
	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}
	public String getMainSentence() {
		return mainSentence;
	}
	public void setMainSentence(String mainSentence) {
		this.mainSentence = mainSentence;
	}
	public List<String> getArg1Types() {
		return arg1Types;
	}
	public void setArg1Types(List<String> arg1Types) {
		this.arg1Types = arg1Types;
	}
	public List<String> getArg2Types() {
		return arg2Types;
	}
	public void setArg2Types(List<String> arg2Types) {
		this.arg2Types = arg2Types;
	}
	
	
	
}
