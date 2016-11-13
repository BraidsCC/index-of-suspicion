package cc.braids.app.ixos;

import cc.braids.app.ixos.model.Finding;
import cc.braids.app.ixos.model.IxosModel;
import cc.braids.util.NYIException;

@SuppressWarnings("unused")
public class DispatchReport {

	private Scenario situation;

	public DispatchReport(Scenario situation) {
		this.situation = situation;
    }

	public IxosModel.Sex getSex() {
		return situation.getPatientSex();
    }

	public float getAge() {
		return situation.getPatientAgeYears();
    }

	public Finding getChiefComplaint() {
	    return situation.getChiefComplaint();
    }
	
}
