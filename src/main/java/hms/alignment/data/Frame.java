package hms.alignment.data;

import java.util.List;
import java.util.Map;

public class Frame {

	
	private String frameId;
	private String label;
	private String definition;
	private List<SemanticRole> roles;
	private List<String> lexicalEntries;
	private Map<String, String> relatedFrames;
	
	
	
	public String getFrameId() {
		return frameId;
	}
	
	
	public void setFrameId(String frameId) {
		this.frameId = frameId;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getDefinition() {
		return definition;
	}
	public void setDefinition(String definition) {
		this.definition = definition;
	}
	public List<SemanticRole> getRoles() {
		return roles;
	}
	public void setRoles(List<SemanticRole> roles) {
		this.roles = roles;
	}
	
	
	
	public Map<String, String> getRelatedFrames() {
		return relatedFrames;
	}


	public void setRelatedFrames(Map<String, String> relatedFrames) {
		this.relatedFrames = relatedFrames;
	}


	public List<String> getLexicalEntries() {
		return lexicalEntries;
	}


	public void setLexicalEntries(List<String> lexicalEntries) {
		this.lexicalEntries = lexicalEntries;
	}


	@Override
	public String toString() {

		return frameId + "\t" + label + "\t" + definition + "\t" + roles;
		
	}
	
}
