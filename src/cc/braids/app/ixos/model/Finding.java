package cc.braids.app.ixos.model;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static cc.braids.app.ixos.model.FindingIlk.*;
import static cc.braids.util.UFunctions.*;

import cc.braids.app.ixos.model.FindingIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.LikelihoodIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.OnsetIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.PalliProvocationIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.QualityIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.RadiationIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.SeverityIlk;
import cc.braids.util.*;

@SuppressWarnings("unused")
public class Finding implements Cloneable, Comparable<Object>, Fact, Serializable, HasReprMethod {

    private static final DecimalFormat DECIMAL_FORMAT_2_DECIMAL_PLACES = new DecimalFormat("#.##");
	private static final long serialVersionUID = 4397049590196157297L;
	public static final Finding INDETERMINATE_SYMPTOM_FINDING = new Finding(INDETERMINATE_SYMPTOM, null, null, null, null, null, null);
	private FindingIlk ilk;
	private Float probability;
	private OnsetIlk onset;
	private PalliProvocationIlk provocation;
	private QualityIlk quality;
	private RadiationIlk radiation;
	private SeverityIlk severity;

	@Override
	public int compareTo(Object that) {
		int thisHC = this.hashCode();
		int thatHC = that.hashCode();

		if (thisHC < thatHC) {
			return -1;
		}

		if (thisHC == thatHC) {
			return 0;
		}

		return +1;
	}


	public Finding(FindingIlk findingIlk, LikelihoodIlk likelihoodIlk,
            OnsetIlk onsetIlk,
            PalliProvocationIlk provocationPalliationIlk,
            QualityIlk qualityIlk, RadiationIlk radiationIlk,
            SeverityIlk severityIlk)
    {
		setIlk(findingIlk);
		setLikelihood(likelihoodIlk);
		setOnset(onsetIlk);
		setProvocation(provocationPalliationIlk);
		setQuality(qualityIlk);
		setRadiation(radiationIlk);
		setSeverity(severityIlk);

    }

	public FindingIlk getIlk() {
		return ilk;
    }

	public void setIlk(FindingIlk ilk) {
	    this.ilk = ilk;
    }

	public void setLikelihood(LikelihoodIlk likelihood) {
		if (likelihood == null) {
			setProbability(null);
		}
		else {
			setProbability(likelihood.getProbability());
		}
    }

	public OnsetIlk getOnset() {
	    return onset;
    }

	public void setOnset(OnsetIlk onset) {
	    this.onset = onset;
    }

	public PalliProvocationIlk getProvocation() {
	    return provocation;
    }

	public void setProvocation(PalliProvocationIlk provocation) {
	    this.provocation = provocation;
    }

	public QualityIlk getQuality() {
	    return quality;
    }

	public void setQuality(QualityIlk quality) {
	    this.quality = quality;
    }

	public RadiationIlk getRadiation() {
	    return radiation;
    }

	public void setRadiation(RadiationIlk radiation) {
	    this.radiation = radiation;
    }

	public SeverityIlk getSeverity() {
	    return severity;
    }

	public void setSeverity(SeverityIlk severity) {
	    this.severity = severity;
    }

	public Float getProbability() {
	    return probability;
    }

	public void setProbability(Float probability) {
	    this.probability = probability;
    }
	
	@Override
	public Object clone() {
		Object result;
		try {
	        result = super.clone();    // a shallow copy is fine.
        } catch (CloneNotSupportedException e) {
	        throw new RuntimeException(e);
        }
		
		return result;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Finding(");
		
		List<Object> members = Arrays.asList(getIlk(), getOnset(), getProvocation(), getQuality(), getRadiation(), getSeverity());
		
		for (Object obj : members) {
			if (obj != null) {
				buf.append(obj);
				buf.append(';');
			}
		}

		if (getProbability() != null) {
			buf.append(DECIMAL_FORMAT_2_DECIMAL_PLACES.format(getProbability()));
		}

		buf.append(')');
		return buf.toString();
	}


	@Override
    public String __repr__() {
		return toString();
    }


	/**
	 * Responsible for simplifying a theoretical finding into a more concrete,
	 * actual one.  For instance, actual findings do not have a probability.
	 * 
	 * @return a Finding very similar to this one, but with some unknown variables removed
	 */
	public Finding actualize() {
		Finding result = (Finding) this.clone();
		
		result.setProbability(null);
	    
	    return result;
    }

	/**
	 * Responsible for determining whether the first set of findings (the
	 * superset) contain items that are approximately plausible from the second
	 * set (a loose subset).
	 * 
	 * TODO This is a bit weak; it only compares FindingIlk of the args.
	 */
	public static boolean leftContainsRight(Set<Finding> specificSuperset,
            Set<Finding> looseSubset)
    {
		Set<FindingIlk> expIlk = getIlks(specificSuperset);
		
		Set<FindingIlk> actIlk = getIlks(looseSubset);
		
		return (expIlk.containsAll(actIlk));
    }


	public static Set<FindingIlk> getIlks(Collection<Finding> findings) {
		return findings
				.stream()
				.map(f -> f.getIlk())
		        .collect(Collectors.toSet());
    }
}
