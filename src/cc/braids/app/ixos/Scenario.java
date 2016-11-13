package cc.braids.app.ixos;

import cc.braids.app.ixos.model.Finding;
import cc.braids.app.ixos.model.IxosModel;
import cc.braids.app.ixos.model.IxosModel.Sex;
import cc.braids.util.*;

import java.util.*;

public class Scenario {
	
	private Set<Finding> actualFindings = null;
	private Finding chiefComplaint = null;
	private Malady malady;
	private int minimumActualFindings;
	private Float patientAgeYears = null;
	private Sex patientSex = null;
	private Integer numAttractiveDistractors;
	private int numMultipleChoiceSelections = IxosModel.DEFAULT_NUM_MULTIPLE_CHOICE_SELECTIONS;
	
	public Scenario() {
		resetFindings();
	}
	
	public int getNumMultipleChoiceSelections() {
		return numMultipleChoiceSelections;
	}

	public void setNumMultipleChoiceSelections(int numMultipleChoiceSelections) {
		this.numMultipleChoiceSelections = numMultipleChoiceSelections;
	}

	public Integer getNumAttractiveDistractors() {
		return numAttractiveDistractors;
	}

	public enum Ilk {
		MALADY_FROM_FINDINGS,
		MALADY_FROM_TREATMENTS,
	}
	
	public void addActualFinding(Finding actualFinding) {
		assert Finding.getIlks(malady.getFindingsDeep()).contains(actualFinding.getIlk());
		
		actualFindings.add(actualFinding);
	}
	
	public Set<Finding> getActualFindings() {
		return actualFindings;
	}
	
	public Finding getChiefComplaint() {
		return chiefComplaint;
	}
	
	public Malady getMalady() {
		return malady;
	}
	
	public int getMinimumActualFindings() {
		return minimumActualFindings;
	}

	public Float getPatientAgeYears() {
		return patientAgeYears;
	}
	
	public Sex getPatientSex() {
		return patientSex;
	}

	public void setChiefComplaint(Finding finding) {
		// The chiefComplaint may have nothing to do with the malady at all.
		// This allows for chief complaints that are red herrings.

		chiefComplaint = finding;
	}
	
	public void setMalady(Malady malady) {
		this.malady = malady;
	}
	
	public void setMinimumActualFindings(int mininumActualFindings) {
		if (mininumActualFindings < 0) {
			throw new IllegalArgumentException("mininumActualFindings");
		}
		
		minimumActualFindings = mininumActualFindings;
	}
	
	public void setNumAttractiveDistractors(Integer numAttractiveDistractors) {
		if (numAttractiveDistractors != null && numAttractiveDistractors < 0) {
			throw new IllegalArgumentException("numAttractiveDistractors");
		}
		
		this.numAttractiveDistractors = numAttractiveDistractors;
	}
	
	public void setPatientAgeYears(float patientAgeYears) {
		this.patientAgeYears = patientAgeYears;
	}
	
	public void setPatientSex(Sex patientSex) {
		this.patientSex = patientSex;
	}

	public void setMultipleChoiceSelections(List<Malady> maladies) {
		// TODO Auto-generated method stub
		// 
		
		throw new NYIException();
	}

	public void resetFindings() {
		actualFindings = new HashSet<>();
	}
	
}
