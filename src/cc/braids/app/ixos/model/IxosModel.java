package cc.braids.app.ixos.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.*;

import cc.braids.app.ixos.Malady;
import cc.braids.app.ixos.Scenario;
import cc.braids.app.ixos.model.FindingIlk;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.MaladyIlk;
import cc.braids.util.NYIException;

@SuppressWarnings("unused")
public class IxosModel {

	public static enum Sex {
		FEMALE, MALE
	}

	public static final float MAXIMUM_HUMAN_LIFESPAN = 125.0f;
	public static final float MINIMUM_PATIENT_AGE = 4.0f;
	public static final float MAXIMUM_PATIENT_AGE = MAXIMUM_HUMAN_LIFESPAN;
	public static final int DEFAULT_NUM_MULTIPLE_CHOICE_SELECTIONS = 4;


	// replaced by enum FindingIlk: private Set<String> allFindings;
	
	private MaladyDatabase maladyDB;

	private Scenario scenario;
	
	public MaladyDatabase getMaladyDB() {
		return maladyDB;
	}


	public IxosModel(InputStream containsMaladyDB) throws IOException, ClassNotFoundException {
		ObjectInputStream objIn = new ObjectInputStream(containsMaladyDB);
		
		try {
			maladyDB = (MaladyDatabase) objIn.readObject();
		}
		catch (InvalidObjectException exn) {
			throw new RuntimeException("Using incompatible and/or old version of malady database", exn);
		}
		finally {
			objIn.close();
		}
		
		maladyDB.validate(null);
	}
	
	
	/**
	 * @deprecated
	 */
	@Deprecated
	public void addMaladyByName(MaladyIlk name, Malady malady) {
		throw new AssertionError("succeeded by MaladyDatabase");
	}

	/**
	 * @deprecated Use {@link cc.braids.app.ixos.model.MaladyDatabase#categorize(cc.braids.app.ixos.model.IxosModel,String,Malady)} instead
	 */
	@Deprecated
	public void categorize(String category, Malady malady) {
		throw new AssertionError("succeeded by MaladyDatabase");
	}
	
	
	
	public void clearCurrentScenario() {
		scenario = new Scenario();
	}
	
	/**
	 * @deprecated
	 */
	@Deprecated
	public Set<String> getAllFindings() {
		HashSet<String> result = new HashSet<>();
		for (FindingIlk ilk : FindingIlk.values())
		{
			result.add(ilk.name());
		}

		return result;
	}
	
	/**
	 * @deprecated Use {@link cc.braids.app.ixos.model.MaladyDatabase#getMaladyByName(cc.braids.app.ixos.model.IxosModel,String)} instead
	 */
	@Deprecated
	public Malady getMaladyByName(String string) {
		throw new AssertionError("outmoded");
	}

	public Scenario getScenario() {
		return scenario;
	}
	
	
	public List<Malady> getTopLevelMaladies() {
		return maladyDB.getTopLevelMaladies();
	}
	
}
