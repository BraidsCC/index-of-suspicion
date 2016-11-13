package cc.braids.app.ixos;

import cc.braids.app.ixos.model.Choice;
import cc.braids.app.ixos.model.Finding;
import cc.braids.app.ixos.model.FindingIlk;
import cc.braids.app.ixos.model.FindingUnion;
import cc.braids.app.ixos.model.NegatedFinding;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.CategoryIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.LikelihoodIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.MaladyIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.OnsetIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.PalliProvocationIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.QualityIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.RadiationIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.SeverityIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.TreatmentIlk;
import cc.braids.util.*;
import cc.braids.util.python.port.KeyError;

import static cc.braids.util.UFunctions.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class Malady implements Choice, Comparable<Malady>, Serializable {
    private static final long serialVersionUID = 490149227236913872L;

	/**
	 * The STARTED state means the entry is in the middle of being crystallized;
	 * this helps us detect cyclic dependencies.
	 */
	private enum CrystalState { FINISHED, NOT_STARTED, STARTED }
	
	private static boolean debugFlag = false;;
	
	private CrystalState crystalState = CrystalState.NOT_STARTED;
	private Set<FindingIlk> findingIlkSet = EnumSet.noneOf(FindingIlk.class);
	private Set<FindingIlk> findingSubtractions = EnumSet.noneOf(FindingIlk.class);
	/** @deprecated */ private Set<Malady> oldFindingUnions = new HashSet<>();
	private MaladyIlk ilk;
	private Set<TreatmentIlk> treatments = EnumSet.noneOf(TreatmentIlk.class);

	private Set<Malady> treatmentUnions = new HashSet<>();

	private Set<Finding> findings = new HashSet<>();
	private Set<FindingUnion> findingUnions = new HashSet<>();

	private Set<NegatedFinding> negatedFindings = new HashSet<>();

	private CategoryIlk category;

	private Set<Finding> deepFindingsCache = null;

	private Set<TreatmentIlk> deepTreatmentsCache = null;
	
	public Malady(MaladyIlk ilk) {
		this.ilk = ilk;
		debug("Creating " + repr(this));
	}
	
	public void add(Finding finding) {
		findings.add(finding);
	}
	
	public void add(FindingUnion funion) {
		findingUnions.add(funion);
	}

	public void add(NegatedFinding neg) {
		negatedFindings .add(neg);
	}
	
	public void add(TreatmentIlk treatmentIlk) {
		assert crystalState == CrystalState.NOT_STARTED;

		treatments.add(treatmentIlk);
		
		debug("Added treatment " + repr(treatmentIlk) + " to "
				+ repr(this));
	}
	
	/**
	 * @deprecated
	 */
	public void addFinding(FindingIlk findingIlk) {
		assert crystalState == CrystalState.NOT_STARTED;

		findingIlkSet.add(findingIlk);
		
		debug("Added finding " + repr(findingIlk) + " to " + repr(this));
	}
	
	/** @deprecated */
	public void addFinding(FindingIlk findingIlk, LikelihoodIlk likelihoodIlk, OnsetIlk onsetIlk,
			PalliProvocationIlk provocationPalliationIlk, QualityIlk qualityIlk, RadiationIlk radiationIlk,
			SeverityIlk severityIlk)
	{
		// This simple version only uses findings.
		addFinding(findingIlk);
	}

	/** @deprecated */
	public void addFinding(String themeHospitalCommonFinding) {
		throw new AssertionError("outmoded");
	}

	/** @deprecated */
	public void addFinding(String finding, String likelihood, String onset,
			String provocationOrPalliation, String quality, String radiation,
			String severity)
	{
		throw new AssertionError("outmoded");
	}

	/** @deprecated */
	public void addFindingUnion(Malady subset,
			LikelihoodIlk likelihoodIlk, OnsetIlk onsetIlk)
	{
		assert crystalState == CrystalState.NOT_STARTED;

		oldFindingUnions.add(subset);
		
		debug("Added finding-union of " + repr(subset)
				+ " to " + repr(this));
	}

	
	/** @deprecated */
	public void addFindingUnion(Malady subset, String likelihood, String onset,
			String provocationOrPalliation, String quality, String radiation,
			String severity)
	{
		throw new AssertionError("outmoded");
	}
	
	/** @deprecated */
	public void addNegatedFinding(FindingIlk findingIlk,
			LikelihoodIlk likelihoodIlk)
	{
		assert crystalState == CrystalState.NOT_STARTED;

		findingSubtractions.add(findingIlk);
		
		debug("Added negative finding " + repr(findingIlk) + " to "
				+ repr(this));
	}

	/** @deprecated */
	public void addNegatedFinding(String negativeFinding, String likelihood) {
		throw new AssertionError("outmoded");
	}

	public void addTreatmentUnion(Malady subset) {
		assert crystalState == CrystalState.NOT_STARTED;

		treatmentUnions.add(subset);
		
		debug("Added treatment-union of " + repr(subset)
				+ " to " + repr(this));
	}

	public void crystallize() {
		
		// If the following assertion fails, there is a cyclic dependence in one
		// of the unions.  Fix the input data (spreadsheet).
		assert crystalState != CrystalState.STARTED;

		if (crystalState == CrystalState.FINISHED) {
			debug(repr(this) + " has already been crystallized.");
			return;
		}
		
		crystalState = CrystalState.STARTED;
		// This object may no longer be modified outside this method.
		
		// Crystallize the maladies in the unions, then add their
		// technological and cultural distinctiveness to our o-- er, assimilate
		// them.
		//
		for (Malady malady : oldFindingUnions) {
			debug(repr(this) + " is crystallizing " + repr(malady) + ".");
			
			malady.crystallize();
			findingIlkSet.addAll(malady.findingIlkSet);
		}
		
		for (Malady malady : treatmentUnions) {
			debug(repr(this) + " is crystallizing " + repr(malady) + ".");

			malady.crystallize();
			treatments.addAll(malady.treatments);
		}
		
		findingIlkSet.removeAll(findingSubtractions);

		if (debugFlag) {
			debug(repr(this) + " finished crystallizing.  Findings are "
					+ repr(findingIlkSet) + "; treatments are " + repr(treatments)
					+ ".");
		}

		crystalState = CrystalState.FINISHED;
	}

	private void debug(String string) {
		if (debugFlag) {
			System.out.println(string);
		}
	}

	/** @deprecated */
	@Deprecated
    public Set<String> getFindingNames() {
		return findingIlkSet.stream().
				map(f -> f.toString()).
				collect(Collectors.toSet());
	}

	/** @deprecated */
	public Set<FindingIlk> oldGetFindings() {
		return findingIlkSet;
	}
	
	public Set<Finding> getFindings() {
		return findings;
	}
	
	/**
	 * Gets the direct findings for this malady and all findings for all of its findings-unions.
	 */
	public Set<Finding> getFindingsDeep() {
		if (deepFindingsCache == null) {

			deepFindingsCache = new HashSet<>();

			deepFindingsCache.addAll(getFindings());
	
			for (FindingUnion funion : getFindingUnions()) {
				deepFindingsCache.addAll(funion.getFindings());
			}
		}
		
		return deepFindingsCache;
    }
	
	public Set<FindingUnion> getFindingUnions() {
		return findingUnions;
	}
	
	public Set<NegatedFinding> getNegatedFindings() {
		// TODO deferred 20150825 implement getNegatedFindings
		throw new NYIException();
	}
	
	public MaladyIlk getIlk() {
		return ilk;
	}
	
	public Set<TreatmentIlk> getTreatments() {
		return treatments;
	}

	public String getName() {
		return ilk.toString();
	}
	
	/**
	 * @deprecated Use {@link #getName()} instead
	 */
	@Deprecated
	@Override
	public String toString() {
		return getName();
	}

	public CategoryIlk getCategory() {
		return this.category;
    }
	
	public void setCategory(CategoryIlk catilk) {
		this.category = catilk;
	}

	public Set<TreatmentIlk> getTreatmentsDeep() {
		if (deepTreatmentsCache == null) {

			deepTreatmentsCache = EnumSet.noneOf(TreatmentIlk.class);

			deepTreatmentsCache.addAll(getTreatments());
	
			for (Malady funion : getTreatmentUnions()) {
				deepTreatmentsCache.addAll(funion.getTreatmentsDeep());
			}
		}
	    
	    return deepTreatmentsCache;
    }

	private Set<Malady> getTreatmentUnions() {
		return treatmentUnions;
    }

	/** 
	 * Mostly useful for unit testing.
	 * 
	 * @param findingIlk  an ilk of the desired finding
	 * @return  one finding having that ilk, chosen arbitrarily if more than one such finding has that ilk
	 * @throws KeyError  if this malady has no finding of that ilk
	 */
	public Finding getFindingByIlk(FindingIlk findingIlk) {
		for (Finding finding : getFindingsDeep()) {
			if (finding.getIlk() == findingIlk) {
				return finding;
			}
		}
		
		throw new KeyError(this + " has no finding of the ilk " + findingIlk);
	}

	@Override
	public int compareTo(Malady that) {
		if (that == null)  return +1;
		
		int enumComp = this.getIlk().compareTo(that.getIlk());
		
		if (enumComp < 0)  return -1;
		if (enumComp > 0)  return +1;
		else {

			if (this.hashCode() < that.hashCode())  return -1;
			if (this.hashCode() > that.hashCode())  return +1;
			else {

				assert that == this;
				return 0;
			}
		}
	}
}
