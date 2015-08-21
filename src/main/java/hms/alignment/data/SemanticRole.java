package hms.alignment.data;

public class SemanticRole {

	private String role;
	private String type;
	private String defnition;
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getDefnition() {
		return defnition;
	}
	public void setDefnition(String defnition) {
		this.defnition = defnition;
	}
	
	@Override
	public String toString() {
		return role + "\t" + type + "\t" + defnition;
		
	}
	
}
