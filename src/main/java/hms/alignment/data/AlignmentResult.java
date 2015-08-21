package hms.alignment.data;

import java.util.Map;

public class AlignmentResult {

	private String propId;
	private String frameId;
	
	private Map<String, Map<String, Double>> mappings;

	public String getPropId() {
		return propId;
	}

	public void setPropId(String propId) {
		this.propId = propId;
	}

	public String getFrameId() {
		return frameId;
	}

	public void setFrameId(String frameId) {
		this.frameId = frameId;
	}

	public Map<String, Map<String, Double>> getMappings() {
		return mappings;
	}

	public void setMappings(Map<String, Map<String, Double>> mappings) {
		this.mappings = mappings;
	}
	
	
}
