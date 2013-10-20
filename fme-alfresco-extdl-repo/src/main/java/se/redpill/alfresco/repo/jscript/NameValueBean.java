package se.redpill.alfresco.repo.jscript;

import java.io.Serializable;

public class NameValueBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5603557690332051088L;

	String name;
	String value;
	
	public void setName(String name) {
		this.name = name;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
}
