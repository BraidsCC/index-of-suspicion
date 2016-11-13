package cc.braids.app.ixos.model;

import static cc.braids.util.UFunctions.*;

import java.io.Serializable;
import java.util.*;

import cc.braids.app.ixos.model.FindingIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.LikelihoodIlk;
import cc.braids.util.*;

@SuppressWarnings("unused")
public class NegatedFinding implements Serializable {

    private static final long serialVersionUID = 6037452281592342575L;
	private FindingIlk finding;
	private LikelihoodIlk likelihood;

	public NegatedFinding(FindingIlk findingIlk, LikelihoodIlk likelihoodIlk) {
		setFinding(findingIlk);
		setLikelihood(likelihoodIlk);
    }

	public FindingIlk getFinding() {
	    return finding;
    }

	public void setFinding(FindingIlk finding) {
	    this.finding = finding;
    }

	public LikelihoodIlk getLikelihood() {
	    return likelihood;
    }

	public void setLikelihood(LikelihoodIlk likelihood) {
	    this.likelihood = likelihood;
    }
	
}
