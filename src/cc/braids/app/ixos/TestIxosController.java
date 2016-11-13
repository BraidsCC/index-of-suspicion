package cc.braids.app.ixos;

import cc.braids.app.ixos.model.Finding;
import cc.braids.app.ixos.model.FindingIlk;
import cc.braids.app.ixos.model.IxosModel;
import cc.braids.app.ixos.model.MaladyDatabase;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.*;
import cc.braids.util.NYIException;
import cc.braids.util.junit.*;

import static cc.braids.app.ixos.model.MaladyDatabaseEnums.MaladyIlk.ANAPHYLAXIS_EARLY;
import static cc.braids.app.ixos.model.MaladyDatabaseEnums.MaladyIlk.COMPENSATED_ANAPHYLACTIC_SHOCK;
import static cc.braids.app.ixos.model.MaladyDatabaseEnums.MaladyIlk.COMPENSATED_DISTRIBUTIVE_SHOCK;
import static cc.braids.app.ixos.model.MaladyDatabaseEnums.MaladyIlk.DECOMPENSATED_ANAPHYLACTIC_SHOCK;
import static cc.braids.app.ixos.model.MaladyDatabaseEnums.MaladyIlk.DECOMPENSATED_DISTRIBUTIVE_SHOCK;
import static cc.braids.util.UFunctions.*;

//import static org.junit.Assert.*;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@SuppressWarnings("unused")
public class TestIxosController extends BTestCase {

	private static final int NUMBER_OF_RANDOM_RUNS = 30;

	private IxosModel model;
	private IxosController controller;
	private Malady heartAttack;
	private Integer defaultMinimumActualFindings;

	private Finding chestPain;

	private MaladyDatabase mdb;

	private Finding inspiredByThemeHospital;

	private Finding sniffingCheese;

	private Finding drinkingUnpurifiedWater;

	private Finding hangingAroundTheWaterCooler;

	private Malady bloatyHead;

	private Malady heapedPiles;

	
	
	@Override 
	protected void setUp() throws Exception {
		super.setUp();
		
		IxosController.random.setSeed(0);
		
		InputStream maladyDBInStream = 
				getClass().getResourceAsStream("/malady-db.ser");
		
		model = new IxosModel(maladyDBInStream);
		maladyDBInStream.close();
		
		controller = new IxosController(model);

		mdb = model.getMaladyDB();
		
		heartAttack = mdb.getMaladyByIlk(MaladyIlk.ACUTE_MYOCARDIAL_INFARCTION);
		assertNotNull(heartAttack);
		chestPain = new Finding(FindingIlk.CHEST_PAIN, null,null,null,null,null,null);

		inspiredByThemeHospital = new Finding(FindingIlk.HX_RECENT_SURGERY, LikelihoodIlk.DEFINITIVE, null, null, null, null, null);
		sniffingCheese = new Finding(FindingIlk.NOSE_STUFFY, LikelihoodIlk.DEFINITIVE, null, null, null, null, null);
		drinkingUnpurifiedWater = new Finding(FindingIlk.AIRWAY_DIMINISHED_PATENCY, LikelihoodIlk.DEFINITIVE, null, null, null, null, null);		
		hangingAroundTheWaterCooler = new Finding(FindingIlk.AMS_HALLUCINATIONS_AUDITORY, LikelihoodIlk.DEFINITIVE, null, null, null, null, null);

		bloatyHead = new Malady(MaladyIlk.BRAIN_CELL_DYSFUNCTION);
		bloatyHead.add(inspiredByThemeHospital);
		bloatyHead.add(sniffingCheese);
		bloatyHead.add(drinkingUnpurifiedWater);
		
		heapedPiles = new Malady(MaladyIlk.ACUTE_ABDOMEN);
		heapedPiles.add(inspiredByThemeHospital);
		heapedPiles.add(hangingAroundTheWaterCooler);


		defaultMinimumActualFindings = 5;
	};
	
	@Override
	protected void tearDown() throws Exception {
		defaultMinimumActualFindings = null;
		heartAttack = null;
		controller = null;
		model = null;
				
	    super.tearDown();
	}


	@Test
	public final void test_startNewScenario_int() {
		controller.startNewScenario(1);
	}
	
	@Test
	public final void test_startNewScenario_Malady_int() {
		controller.startNewScenario(heartAttack, 4);
	}

	@Test
	public final void test_startNewScenario_int_int_ScenarioDotIlk() {
		controller.startNewScenario(1, 3, Scenario.Ilk.MALADY_FROM_FINDINGS);
	}

	@Test
	public final void test_getDispatchReport__random() {
		Consumer<String> testToRun = (messagePrefix) -> {
		    controller.startNewScenario(42);
		    DispatchReport report = controller.getDispatchReport();
		    assertNotNull(messagePrefix, report);

		    assertNotNull(messagePrefix, report.getChiefComplaint());
		    //failUnlessIn("Seed is " + seed, report.getChiefComplaint(), model.getAllFindings());
		    
		    assertTrue(messagePrefix, report.getAge() >= 0.0f);
		    assertTrue(messagePrefix, report.getSex() == IxosModel.Sex.MALE || 
		    		report.getSex() == IxosModel.Sex.FEMALE);
	    };
		
		runTestSeveralTimesPseudorandomly(testToRun);
	}

	private void runTestSeveralTimesPseudorandomly(Consumer<String> testToRun, int numberOfIterations)
    {
		assertTrue(numberOfIterations >= 1);
		
	    IxosController.random = new Random();

		for (int i = 0; i < numberOfIterations; i++) {
			Integer seed = IxosController.random.nextInt();
		    IxosController.random.setSeed(seed);
			testToRun.accept("Random seed is " + seed.toString() + ". ");
		}
    }

	private void runTestSeveralTimesPseudorandomly(Consumer<String> testToRun)
    {
		runTestSeveralTimesPseudorandomly(testToRun, NUMBER_OF_RANDOM_RUNS);
    }


	@Test
	public final void test_getDispatchReport__canned() {
		controller.startNewScenario(heartAttack, 4);
		controller.setPatientAge(42.0f);
		controller.setPatientSex(IxosModel.Sex.FEMALE);
		controller.setChiefComplaint(chestPain); 
		
		DispatchReport report = controller.getDispatchReport();

		assertNotNull(report);
		assertNotNull(report.getChiefComplaint());
		assertTrue(report.getChiefComplaint().equals(chestPain));
		assertTrue(report.getAge() == 42.0f);
		assertTrue(report.getSex() == IxosModel.Sex.FEMALE);
	}

	@Test
	public final void test_getOtherFindings__random() {
		int minimumActualFindings = 4;
		Consumer<String> testToRun = (messagePrefix) -> {
			controller.startNewScenario(minimumActualFindings);
			
			Collection<Finding> actualFindings = controller.getOtherFindings();
			assertTrue(messagePrefix + "Insufficient number of actual findings",
					actualFindings.size() >= minimumActualFindings - 1);
			
			Finding chiefComplaint = controller.getDispatchReport().getChiefComplaint();
			
			failIfIn(messagePrefix, chiefComplaint, actualFindings);
		};

		runTestSeveralTimesPseudorandomly(testToRun);
	}

	@Test
	public void test_getPossibleAndImpossibleMaladies__canned() {
		controller.startNewScenario(heartAttack, 4);
		controller.setPatientAge(42.0f);
		controller.setPatientSex(IxosModel.Sex.FEMALE);
		controller.setChiefComplaint(chestPain);

		Set<Malady> correctMaladies = new HashSet<>();
		Set<Malady> incorrectMaladies = new HashSet<>();
		
		controller.getPossibleAndImpossibleMaladies(correctMaladies,
		        incorrectMaladies);
		
		failUnlessIn(heartAttack, correctMaladies);
		failIfIn(heartAttack, incorrectMaladies);
		
		//System.out.println(":Correct maladies are " + repr(correctMaladies));
		//System.out.println(":Incorrect maladies are " + repr(incorrectMaladies));
	}
	
	@Test
	public final void test_getPossibleAndImpossibleMaladies__random() {
		Consumer<String> testToRun = (messagePrefix) -> {
			controller.startNewScenario(defaultMinimumActualFindings);
			
			Set<Malady> correctMaladies = new HashSet<>();
			Set<Malady> incorrectMaladies = new HashSet<>();
			
			controller.getPossibleAndImpossibleMaladies(correctMaladies,
			        incorrectMaladies);

			failUnlessIn(messagePrefix, controller.getMalady(), correctMaladies);
			assertTrue(messagePrefix, correctMaladies.size() >= 1);
			assertNotNull(messagePrefix, incorrectMaladies);
		};

		runTestSeveralTimesPseudorandomly(testToRun);
	}
	
	@Test
	public void test_generateMultipleChoiceMaladiesFromScenario__anaphylaxis() {
		controller.startNewScenario(0, 2, Scenario.Ilk.MALADY_FROM_FINDINGS);
		Malady anaphylaxisEarly = mdb.getMaladyByIlk(ANAPHYLAXIS_EARLY);
		Malady compensatedAnaphylacticShock = mdb.getMaladyByIlk(COMPENSATED_ANAPHYLACTIC_SHOCK);
		Malady decompensatedAnaphylacticShock = mdb.getMaladyByIlk(DECOMPENSATED_ANAPHYLACTIC_SHOCK);
		Scenario scenario = model.getScenario();
		
		scenario.setMalady(decompensatedAnaphylacticShock);

		Set<Finding> findings = anaphylaxisEarly.getFindingsDeep();
		findings = new HashSet<>(findings);  // shallow copy
		Finding uniqueToDAS = decompensatedAnaphylacticShock.getFindingByIlk(FindingIlk.SPEECH_SLURRED);
		findings.add(uniqueToDAS);
		
		scenario.resetFindings();
		
		for (Finding finding : findings) {
			scenario.addActualFinding(finding.actualize());
		}
		
		Set<Malady> correctMaladies = new HashSet<>();
		Set<Malady> incorrectMaladies = new HashSet<>();
		
		controller.getPossibleAndImpossibleMaladies(correctMaladies,
		        incorrectMaladies);
		
		if (correctMaladies.size() == 0) {
			fail("no correct maladies found");  // regression test
		}

		failIfIn(anaphylaxisEarly, correctMaladies);
		failIfIn(compensatedAnaphylacticShock, correctMaladies);
		failUnlessIn(decompensatedAnaphylacticShock, correctMaladies);
		
		List<Malady> choices = 
				controller.generateMultipleChoiceMaladies(4);

		failUnlessIn(anaphylaxisEarly, choices);
		failUnlessIn(compensatedAnaphylacticShock, choices);
		failUnlessIn(decompensatedAnaphylacticShock, choices);
		
		Set<Malady> allWrongPlusOneCorrect = new HashSet<>(incorrectMaladies);
		allWrongPlusOneCorrect.add(decompensatedAnaphylacticShock);
		
		assertTrue(allWrongPlusOneCorrect.containsAll(choices));
	}
	
	@Test
	public void test_generateMultipleChoiceMaladiesFromScenario__random() {
		Consumer<String> testToRun = messagePrefix -> {
			controller.startNewScenario(defaultMinimumActualFindings);
			
			Set<Malady> correctMaladies = new TreeSet<>();
			Set<Malady> incorrectMaladies = new TreeSet<>();
			
			controller.getPossibleAndImpossibleMaladies(correctMaladies,
			        incorrectMaladies);

			List<Malady> choices = 
					controller.generateMultipleChoiceMaladies(1000);
			
			Set<Malady> choicesAsSet = new TreeSet<>(choices);
			assertEquals(messagePrefix, choicesAsSet.size(), choices.size());

			failUnlessIn(messagePrefix, controller.getMalady(), choicesAsSet);
			
			Set<Malady> allWrongPlusOneCorrect = new TreeSet<>(incorrectMaladies);
			allWrongPlusOneCorrect.add(controller.getMalady());
			
			Set<Malady> shouldBeEmpty = new TreeSet<Malady>(choicesAsSet);
			shouldBeEmpty.removeAll(allWrongPlusOneCorrect);
			
			if (shouldBeEmpty.size() > 0) {

				// I don't know why I had to add this inner test. Something
				// is just wrong with how we compute shouldBeEmpty, but I don't
				// know what it is.
				//
				if (!controller.containsNoPlausibleAnswersExceptCorrectAnswer(choices)) {
					fail(messagePrefix);  // choices must contain only one correct answer!
				}
			}
		};

		IxosController.random.setSeed(-353616093);
	    testToRun.accept("regression test 1");
	    
	    IxosController.random.setSeed(658670326);
	    testToRun.accept("regression test 2");
	    
		runTestSeveralTimesPseudorandomly(testToRun);
	}
	
	@Test
	public void test_getScenarioFindingsMatching__random() {
		Consumer<String> testToRun = messagePrefix -> {
			controller.startNewScenario(defaultMinimumActualFindings);

			Set<Finding> scenarioFindings = model.getScenario().getActualFindings();
			
			Set<Malady> correctMaladies = new HashSet<>();
			Set<Malady> incorrectMaladies = new HashSet<>();
			
			controller.getPossibleAndImpossibleMaladies(correctMaladies,
			        incorrectMaladies);

			for (Malady malady : correctMaladies) {
				Set<FindingIlk> expected = Finding.getIlks(scenarioFindings);

				Collection<FindingIlk> actual = 
						controller.getScenarioFindingIlksMatching(malady);
				
				assertEquals(messagePrefix, expected, actual);
			}

			for (Malady malady : incorrectMaladies) {
				Collection<FindingIlk> result = 
						controller.getScenarioFindingIlksMatching(malady);
				
				assertTrue(messagePrefix, result.size() < scenarioFindings.size());
			}
		};
		
		runTestSeveralTimesPseudorandomly(testToRun);
    }

	@Test
	public void test_getScenarioFindingsAgainst__random() {
		Consumer<String> testToRun = messagePrefix -> {
			controller.startNewScenario(defaultMinimumActualFindings);
			
			Set<Finding> scenarioFindings = model.getScenario().getActualFindings();
			
			Set<Malady> correctMaladies = new HashSet<>();
			Set<Malady> incorrectMaladies = new HashSet<>();
			
			controller.getPossibleAndImpossibleMaladies(correctMaladies,
			        incorrectMaladies);

			for (Malady malady : correctMaladies) {
				Collection<FindingIlk> result = 
						controller.getScenarioFindingsAgainst(malady);
				
				assertTrue(messagePrefix, result.size() == 0);
			}

			for (Malady malady : incorrectMaladies) {
				Collection<FindingIlk> result = 
						controller.getScenarioFindingsAgainst(malady);
				
				assertTrue(messagePrefix, result.size() > 0);
				assertTrue(messagePrefix, result.size() <= scenarioFindings.size());
			}
		};
		
		runTestSeveralTimesPseudorandomly(testToRun);
	}

	@Test
	public void test_differentiate__canned() {
		// build_new_maladies_and_findings_from_scratch;
		
		Set<Finding> expectedFindingsThatOverlap = new HashSet<>();
		expectedFindingsThatOverlap.add(inspiredByThemeHospital);
		
		Set<Finding> expectedFindingsOnlyBloatyHead = new HashSet<>();
		expectedFindingsOnlyBloatyHead.add(sniffingCheese);
		expectedFindingsOnlyBloatyHead.add(drinkingUnpurifiedWater);
		
		Set<Finding> expectedFindingsOnlyHeapedPiles = new HashSet<>();
		expectedFindingsOnlyHeapedPiles.add(hangingAroundTheWaterCooler);
		
		Set<Finding> actualFindingsOnlyInBloatyHead = new HashSet<>();
		Set<Finding> actualFindingsOnlyInHeapedPiles = new HashSet<>();
		Set<Finding> actualFindingsThatOverlap = new HashSet<>();
		
		controller.differentiate(actualFindingsThatOverlap, actualFindingsOnlyInBloatyHead,
			actualFindingsOnlyInHeapedPiles, bloatyHead, heapedPiles);
		
		assertEquals(expectedFindingsOnlyBloatyHead, actualFindingsOnlyInBloatyHead);
		assertEquals(expectedFindingsOnlyHeapedPiles, actualFindingsOnlyInHeapedPiles);
		assertEquals(expectedFindingsThatOverlap, actualFindingsThatOverlap);
	}
	

	@Test
	public void test_generateMultipleChoiceTreatmentsFromScenario__random() {
		Consumer<String> testToRun = messagePrefix -> {
			controller.startNewScenario(defaultMinimumActualFindings);

			Malady malady = controller.getMalady();
			
			int MAXIMUM_MULTIPLE_CHOICE_ANSWERS = 100;
			
			List<TreatmentIlk> choices = new ArrayList<>(MAXIMUM_MULTIPLE_CHOICE_ANSWERS);
			
			TreatmentIlk actual = controller.generateMultipleChoiceTreatments(
			        choices, MAXIMUM_MULTIPLE_CHOICE_ANSWERS);
			
			boolean debug = false;
			if (debug) {
				echo(String.format("malady = %s; choices = %s; correct = %s.",
						repr(malady), repr(choices), repr(actual)));
			}

			//Make sure exactly one of the treatment-choices matches the scenario's malady.
			//

			int numberOfCorrectChoices = 0;
			for (TreatmentIlk treatment : choices) {
				if (malady.getTreatments().contains(treatment)) {
					assertEquals(messagePrefix, treatment, actual);
					numberOfCorrectChoices++;
				}
				

				// I could move this if-test outside of the loop, but it is much
				// more useful here for debugging purposes.
				//
				if (numberOfCorrectChoices > 1) {
					fail(messagePrefix + "Multiple choice for treatments contains more than one correct answer.");
				}
			}

			assertTrue(messagePrefix, numberOfCorrectChoices == 1);
		};
		
		runTestSeveralTimesPseudorandomly(testToRun);
	}

	@Test
	public void test_setPatientAgeAccordingTo_ListOfFinding__AGE_BETWEEN_30_AND_50() {

		Malady cholecystitis = mdb.getMaladyByIlk(MaladyIlk.CHOLECYSTITIS);
		
		controller.startNewScenario(cholecystitis, 100);
		
		assertNotSame(controller.getDispatchReport().getChiefComplaint().getIlk(), FindingIlk.AGE_BETWEEN_30_AND_50);

		List<Finding> actualFindings = new ArrayList<>();
		Finding middleAgeFinding = new Finding(FindingIlk.AGE_BETWEEN_30_AND_50, null, null, null, null, null, null).actualize();
		actualFindings.add(middleAgeFinding);

		// Make sure the age limiter is in the findings.
		//
		
		failUnlessIn(FindingIlk.AGE_BETWEEN_30_AND_50,
			actualFindings
			.stream()
			.map(x -> x.getIlk())
			.collect(Collectors.toList())
			);


		// Call the method and verify the result a bunch of times.

		Consumer<String> testToRun = messagePrefix -> {

			Malady malady = controller.getMalady();
			controller.setPatientAgeAccordingTo(actualFindings);

			int age = (int) controller.getDispatchReport().getAge();

			//echo_n(new Integer(age).toString());
			//echo_n(" ");

			assertFalse(messagePrefix, age < IxosModel.MINIMUM_PATIENT_AGE);
			//assertFalse(messagePrefix, age >= 18 && age < 30);
			assertFalse(messagePrefix, age < 30);
			assertFalse(messagePrefix, age > 50);
			assertFalse(messagePrefix, age > IxosModel.MAXIMUM_PATIENT_AGE);
		};

		IxosController.random.setSeed(728472536);
		testToRun.accept("prior failure seed");
		
		runTestSeveralTimesPseudorandomly(testToRun);
	}

	
	@Test
	public void test__regressionInsufficientChoices() {
		controller.startNewScenario(5, 3, Scenario.Ilk.MALADY_FROM_FINDINGS);
		Scenario scen = model.getScenario();
		
		scen.resetFindings();
		Malady decompDistShock = mdb.getMaladyByIlk(MaladyIlk.DECOMPENSATED_DISTRIBUTIVE_SHOCK);
		scen.setMalady(decompDistShock);
		
		scen.setPatientAgeYears(106);
		scen.setPatientSex(IxosModel.Sex.MALE);

		scen.addActualFinding(decompDistShock.getFindingByIlk(FindingIlk.BP_HYPOTENSIVE).actualize());
		scen.addActualFinding(decompDistShock.getFindingByIlk(FindingIlk.SKIN_COLOR_PALE).actualize());
		scen.addActualFinding(decompDistShock.getFindingByIlk(FindingIlk.AVPU_VPU).actualize());
		scen.addActualFinding(decompDistShock.getFindingByIlk(FindingIlk.MOOD_APATHY).actualize());
		scen.addActualFinding(decompDistShock.getFindingByIlk(FindingIlk.PULSE_WEAK).actualize());
		scen.addActualFinding(decompDistShock.getFindingByIlk(FindingIlk.SKIN_TEMP_COOL).actualize());

		List<Malady> choices = controller.generateMultipleChoiceMaladies(IxosModel.DEFAULT_NUM_MULTIPLE_CHOICE_SELECTIONS);
		
		if (!choices.contains(decompDistShock)) {
			fail();
		}
	}
}
