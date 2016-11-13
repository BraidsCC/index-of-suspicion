package cc.braids.app.ixos.model;

import static cc.braids.util.UFunctions.*;

import java.io.Serializable;
import java.util.*;

import cc.braids.app.ixos.Malady;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.LikelihoodIlk;
import cc.braids.util.*;

@SuppressWarnings("unused")
public class FindingUnion implements Serializable {

    private static final long serialVersionUID = -1678826412537103209L;
	private Malady malady;
	private LikelihoodIlk likelihood;

	public FindingUnion(Malady sourceMalady, LikelihoodIlk likelihoodIlk)
    {
		setMalady(sourceMalady);
		setLikelihood(likelihoodIlk);
    }

	public Malady getMalady() {
	    return malady;
    }

	public void setMalady(Malady malady) {
	    this.malady = malady;
    }

	public LikelihoodIlk getLikelihood() {
	    return likelihood;
    }

	public void setLikelihood(LikelihoodIlk likelihood) {
	    this.likelihood = likelihood;
    }

	public Collection<? extends Finding> getFindings() {
		Set<Finding> result = new TreeSet<>();
		result.addAll(getMalady().getFindingsDeep());
		
		if (getLikelihood() != null) {
			// Swap the result for a baseResult.
			//
			Set<Finding> baseResult = result;
			result = new TreeSet<>();
			
			float myProbability = getLikelihood().getProbability();

			// When a FindingUnion has a likelihood... multiply likelihoods' numeric values together.
			
			for (Finding trooper : baseResult) {
				trooper = (Finding) trooper.clone();

				Float trooperDice = trooper.getProbability();

				if (trooperDice == null) {
					trooperDice = MaladyDatabaseEnums.LikelihoodIlk.DEFAULT_LIKELIHOOD.getProbability();
				}
				
				trooperDice *= myProbability;
				
				// This is why we needed to clone the trooper.
				trooper.setProbability(trooperDice);
				
				result.add(trooper);
			}
		}
		
		return result;
    }

}
