package org.distropia.client;

public enum Gender {
	ORGANIZATION, FEMALE, MALE, BOTH, NOT_SPECIFIED;

	public String toDisplayString() {
		if (this.equals(ORGANIZATION)) return "Keine natürliche Person.";
		if (this.equals(FEMALE)) return "Weiblich";
		if (this.equals(MALE)) return "Männlich";
		if (this.equals(BOTH)) return "Transexuell";
		return "Nicht angegeben";
	}
	
	
}
