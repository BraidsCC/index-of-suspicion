package cc.braids.app.ixos;

import cc.braids.app.ixos.Scenario.Ilk;
import cc.braids.app.ixos.model.Choice;
import cc.braids.app.ixos.model.Fact;
import cc.braids.app.ixos.model.Finding;
import cc.braids.app.ixos.model.FindingIlk;
import cc.braids.app.ixos.model.IxosModel;
import cc.braids.util.NYIException;
import cc.braids.util.python.port.ListFrom;

import static cc.braids.app.ixos.model.IxosModel.Sex.*;
import static cc.braids.app.ixos.model.MaladyDatabaseEnums.*;
import static cc.braids.util.UFunctions.*;
import static cc.braids.util.python.port.PyFunctions.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class IxosController {

	/**
	 * Only unit tests may access this field directly.
	 */
	public static Random random = new Random();
	
	private IxosModel model;

	public IxosController(IxosModel model) {
		this.model = model;
	}

	private int computeGap(Malady distractor, int numFactsMatchingCorrectAnswer, Collection<Finding> factsInUse) {
		int numFactsMatchingDistractor = countMatchingFacts(distractor, factsInUse);
		
		return numFactsMatchingCorrectAnswer - numFactsMatchingDistractor;
	}

	boolean containsNoPlausibleAnswersExceptCorrectAnswer(Collection<Malady> maladies) {
		Set<Malady> plausibleAnswers = new HashSet<>();
		List<Malady> ignored = new ArrayList<>();
		getPossibleAndImpossibleMaladies(plausibleAnswers, ignored);

		Set<Malady> trueIfEmpty = new HashSet<>(maladies);
		trueIfEmpty.retainAll(plausibleAnswers);  //intersection of maladies and plausibleAnswers
		trueIfEmpty.remove(model.getScenario().getMalady());

		if (trueIfEmpty.size() == 0) {
			return true;
		}

		return false;  // good place for a breakpoint
	}

	private int countMatchingFacts(Malady choice, Collection<Finding> factsInUse) {
		// This only takes the FindingIlk into account

		Set<FindingIlk> distractorIlks = Finding.getIlks(choice.getFindingsDeep());
		Set<FindingIlk> factIlks = Finding.getIlks(factsInUse);
		
		Set<FindingIlk> intersection = EnumSet.copyOf(distractorIlks);
		intersection.retainAll(factIlks);
		
		return intersection.size();
	}
	
	private void cullDistractorsThatCannotBeStrengthened(List<Malady> goodDistractors, List<List<Finding>> findingSets,
		Set<Finding> actualFindings, Malady correctAnswer)
	{
		// TODO Auto-generated method stub
		//
		
		throw new NYIException();
	}

	public void differentiate(
		Collection<Finding> findingsThatOverlap,
		Collection<Finding> findingsOnlyInMalady1,
		Collection<Finding> findingsOnlyInMalady2,
		Malady malady1, Malady malady2)
	{
		Set<Finding> malady1Findings = malady1.getFindingsDeep();
		Set<Finding> malady2Findings = malady2.getFindingsDeep();

		findingsThatOverlap.addAll(intersection(malady1Findings, malady2Findings));

		findingsOnlyInMalady1.addAll(subtract(malady1Findings, malady2Findings));
		
		findingsOnlyInMalady2.addAll(subtract(malady2Findings, malady1Findings));
	}

	void fillInRestOfScenario(Scenario scenario) {
		fillInRestOfScenario(scenario, Scenario.Ilk.MALADY_FROM_FINDINGS);
	}
	
	void fillInRestOfScenario(Scenario scenario, Scenario.Ilk scenIlk) {
		if (scenario.getMalady() == null) {
			List<Malady> topLevelMalades = model.getTopLevelMaladies();
			int totalTLMaladies = topLevelMalades.size();
			int randomIx = random.nextInt(totalTLMaladies);
			
			scenario.setMalady(topLevelMalades.get(randomIx));
		}
		
		Malady malady = scenario.getMalady();
		Set<Finding> allFindingsForMalady = malady.getFindingsDeep();

		assert allFindingsForMalady.size() > 0
		:
			"malady not sufficiently initialized";
		
		int numMaladyFindingsUsed = 0;
		List<Finding> findingsNotYetUsed = new LinkedList<>(allFindingsForMalady);
		Collections.shuffle(findingsNotYetUsed);
		
		// Add all (definitive) findings with probability >= 1.00f.
		//
		Iterator<Finding> iter = findingsNotYetUsed.iterator();
		while (iter.hasNext()) {
			Finding finding = iter.next();

			Float probability = finding.getProbability();

			if (probability != null && probability >= 1.00f) {
				Finding actualFinding = finding.actualize();
				scenario.addActualFinding(actualFinding);
				numMaladyFindingsUsed++;
				iter.remove();
				
			}
		}

		
		if (scenIlk == Scenario.Ilk.MALADY_FROM_FINDINGS) {
			// Add random, actual findings from the malady until minimum actual
			// findings have been met.
			//
			while (numMaladyFindingsUsed < scenario.getMinimumActualFindings())
			{
				Finding randomFinding;
				try {
					// We could allow the finding's probability to play a part here,
					// but the game is more interesting if we don't.
					
					randomFinding = findingsNotYetUsed.remove(0);
					
				} catch (IndexOutOfBoundsException exn) {
					break;  // We've added all the findings we can.
				}
				
				Finding actualFinding = randomFinding.actualize();
				
				scenario.addActualFinding(actualFinding);
				numMaladyFindingsUsed++;
			}
			
			setupAttractiveDistractors(scenario, scenIlk);
		}
		
		// Now set the age, sex, and chief complaint based on the actual findings.
		
		List<Finding> actualFindings = new ListFrom<Finding>(
			scenario.getActualFindings());

		// Set the age if not already set.
		if (scenario.getPatientAgeYears() == null) {
			setPatientAgeAccordingTo(actualFindings);
		}
		
		// Set the sex if not already set.
		if (scenario.getPatientSex() == null) {
			setPatientSexAccordingTo(actualFindings, scenario);
		}
		
		int numberOfActualFindings = actualFindings.size();


		// Set the chief complaint.
		setRandomChiefComplaint(actualFindings);
	}
	
	/**
	 * Using the current scenario, generate a fully randomized list of maladies
	 * where only one is the correct answer.
	 *
	 * @param maxMultipleChoiceAnswers
	 *            maximum number of choices to return
	 * @return a list of Malady instances
	 */
	public List<Malady> generateMultipleChoiceMaladies(int maxMultipleChoiceAnswers) {
		Scenario scenario = model.getScenario();

		scenario.setNumMultipleChoiceSelections(maxMultipleChoiceAnswers);
		
		List<Malady> result = generateMultipleChoiceMaladies(scenario);
		//assert containsNoPlausibleAnswersExceptCorrectAnswer(result);
		return result;
	}

	
 	/**
	 * Generate a fully randomized list of maladies where only one is the correct answer.
	 *
	 * This takes Scenario.getNumAttractiveDistractors() into account.
	 *
	 * @param scenario  valuable source and destination of much data
	 * @return  a list of maladies, one of which is correct
	 */
	private List<Malady> generateMultipleChoiceMaladies(Scenario scenario) {
		List<Malady> result = null;
		int maxSelections = scenario.getNumMultipleChoiceSelections();
		Set<Malady> resultSet = new HashSet<>(maxSelections * 2);
		Integer numDesiredGoodDistractors = scenario.getNumAttractiveDistractors();

		if (numDesiredGoodDistractors == null || numDesiredGoodDistractors >= maxSelections) {
			numDesiredGoodDistractors = maxSelections - 1;
		}

		Malady correctAnswer = getMalady();
		Collection<Finding> factsInUse = scenario.getActualFindings();
		
		List<Malady> goodDistractors = improveDistractorsByAddingFacts(correctAnswer, factsInUse,
			numDesiredGoodDistractors, scenario);
		
		//assert containsNoPlausibleAnswersExceptCorrectAnswer(goodDistractors);

		// Add at most numDesiredGoodDistractors to the resultSet.
		//
		for (int i = 0; i < numDesiredGoodDistractors; i++) {
			try {
				Malady distractor = goodDistractors.remove(0);
				resultSet.add(distractor);

			} catch (IndexOutOfBoundsException exn) {
				// We ran out of good distractors.  Oh, well.
				break;
			}
		}
		
		//assert containsNoPlausibleAnswersExceptCorrectAnswer(resultSet);

		if (resultSet.size() < maxSelections - 1) {
			// We need to add some random choices, either because we didn't
			// want all the choices to be good distractors, or we couldn't
			// find/make enough good distractors.
			
			List<Malady> ignored = new ArrayList<>();
			List<Malady> incorrectMaladies = new ArrayList<>();
			getPossibleAndImpossibleMaladies(ignored, incorrectMaladies);

			Collections.shuffle(incorrectMaladies);

			while (resultSet.size() < maxSelections - 1 && incorrectMaladies.size() > 0) {
				Malady randomBogusChoice = incorrectMaladies.remove(0);

				if (!resultSet.contains(randomBogusChoice)) {
					resultSet.add(randomBogusChoice);
					//assert containsNoPlausibleAnswersExceptCorrectAnswer(resultSet);
				}
			}

			
		}
		
		resultSet.add(correctAnswer);
		
		result = new ArrayList<>(resultSet);
		assert result.size() <= maxSelections;
		assert result.size() >= 1;		
		Collections.shuffle(result);
		assert containsNoPlausibleAnswersExceptCorrectAnswer(result);
		assert result.contains(correctAnswer);
		return result;
	}


	/**
	 * Using the current scenario, generate a fully randomized list of treatments
	 * where only one is the correct answer.
	 *
	 * @param resultMultipleChoices  output variable in which to store the choices
	 * @param maxMultipleChoiceAnswers  maximum number of choices to return
	 * @return  the correct answer (not its index)
	 */
	public TreatmentIlk generateMultipleChoiceTreatments(
		List<TreatmentIlk> resultMultipleChoices,
		int maxMultipleChoiceAnswers)
	{
		if (resultMultipleChoices == null || resultMultipleChoices.size() > 0) {
			throw new java.lang.IllegalArgumentException("resultMultipleChoices must be non-null and empty");
		}

		EnumSet<TreatmentIlk> correctTreatmentSet = EnumSet.noneOf(TreatmentIlk.class);
		EnumSet<TreatmentIlk> incorrectTreatmentSet = EnumSet.noneOf(TreatmentIlk.class);
		getPossibleAndImpossibleTreatments(correctTreatmentSet, incorrectTreatmentSet);
		
		List<TreatmentIlk> correctTreatmentShuffledList = shuffledListFrom(correctTreatmentSet);
		List<TreatmentIlk> incorrectTreatmentShuffledList = shuffledListFrom(incorrectTreatmentSet);

		// Build the multiple choice answers and shuffle them.
		//
		
		int choicesIx;
		for (choicesIx = 0;
			choicesIx < maxMultipleChoiceAnswers - 1 &&
			choicesIx < incorrectTreatmentShuffledList.size();
			choicesIx++)
		{
			resultMultipleChoices.add(incorrectTreatmentShuffledList.get(choicesIx));
		}
		

		TreatmentIlk result = correctTreatmentShuffledList.get(0);
		resultMultipleChoices.add(result);
		
		assert resultMultipleChoices.size() <= maxMultipleChoiceAnswers;
		
		Collections.shuffle(resultMultipleChoices);
		return result;
	}

	public DispatchReport getDispatchReport() {
		Scenario scenario = model.getScenario();
		DispatchReport result = new DispatchReport(scenario);
		return result;
	}

	/**
	 * @deprecated
	 * 
	 * @param goodDistractors
	 * @param factsInUse
	 * @param correctAnswer
	 * @return applicable findings for first distractor that could be
	 *         strengthened, or null if no such finding exists
	 */
	private List<Finding> getFactsForFirstDistractorThatCouldBeStrengthened(List<Malady> goodDistractors,
		Collection<Finding> factsInUse, Malady correctAnswer)
	{
		List<Finding> result = null;

		Set<FindingIlk> correctAnswerFindingIlks = Finding.getIlks(correctAnswer.getFindingsDeep());
		Set<FindingIlk> ilksOfFactsInUse = Finding.getIlks(factsInUse);

		Set<FindingIlk> correctAnswerFindingIlksNotInUse = EnumSet.copyOf(correctAnswerFindingIlks);
		correctAnswerFindingIlksNotInUse.removeAll(ilksOfFactsInUse);
		
		// For each distractor, determine which facts it has that "match" the
		// facts that could be used (but are not yet in use) to support the
		// correct answer.
		//
		for (Malady distractor : goodDistractors) {
			Set<FindingIlk> distractorFindingIlks = Finding.getIlks(distractor.getFindingsDeep());

			Set<FindingIlk> matchingDistractorFindingIlks = EnumSet.copyOf(distractorFindingIlks);
			matchingDistractorFindingIlks.retainAll(correctAnswerFindingIlksNotInUse);

			if (matchingDistractorFindingIlks.size() > 0) {
				result = new ArrayList<>(matchingDistractorFindingIlks.size());
				
				for (FindingIlk filk : matchingDistractorFindingIlks) {
					result.add(correctAnswer.getFindingByIlk(filk));
				}
				
				Collections.shuffle(result);
				
				break;
			}
		}

		return result;
	}

	/**
	 * Fetch the malady for the current scenario.
	 */
	public Malady getMalady() {
		return model.getScenario().getMalady();
	}
	
	/**
	 * @return  the actual findings in this scenario other than the chief complaint
	 */
	public Collection<Finding> getOtherFindings() {
		Scenario scenario = model.getScenario();
		Set<Finding> originalActualFindings = scenario.getActualFindings();
		
		Set<Finding> result = new HashSet<>(originalActualFindings.size() * 2);
		result.addAll(originalActualFindings);
		result.remove(scenario.getChiefComplaint());
		
		return result;
	}

	/**
	 * For the current scenario, determine which maladies match the current,
	 * actual findings and which ones do not.
	 *
	 * @param correctMaladies  output variable; we add to this
	 * @param incorrectMaladies  output variable; we add to this
	 */
	public void getPossibleAndImpossibleMaladies(Collection<Malady> correctMaladies,
		Collection<Malady> incorrectMaladies)
	{
		if (correctMaladies == null || correctMaladies.size() > 0) {
			throw new IllegalArgumentException("correctMaladies must be non-null and empty");
		}

		if (incorrectMaladies == null || incorrectMaladies.size() > 0) {
			throw new IllegalArgumentException("incorrectMaladies must be non-null and empty");
		}

		Scenario scenario = model.getScenario();
		Set<Finding> actualFindings = scenario.getActualFindings();
		
		for (Malady mal : model.getMaladyDB().getTopLevelMaladies()) {
			if (Finding.leftContainsRight(mal.getFindingsDeep(), actualFindings)) {
				correctMaladies.add(mal);
			}
			else {
				incorrectMaladies.add(mal);
			}
		}
	}

	
	/* Commented out BECAUSE
	 * some top-level maladies have other top-level maladies' findings as unions.
	 * They will always look similar until we implement likelihoods (like
	 * DEFINITIVE) or other quantitative analyses.
	 *
	 * Old doc...
	 * Check if the set of actual findings is a subset of any other top-level
	 * malady. (We know a malady is a top-level malady when it has a category.)
	 *
	 * @param scenario
	 *            the scenario to check
	 *
	 * @return true if there are other maladies that the scenario's actual
	 *         findings could match
	 private boolean looksLikeAnotherTopLevelMalady(Scenario scenario) {
		Set<String> actualFindings = scenario.getActualFindings();
		Malady scenariosMalady = scenario.getMalady();

		for (Malady mal : model.getTopLevelMaladies()) {
			if (mal == scenariosMalady) {
				// Skip if this is the malady we're already using.
				// Otherwise, it's not "another" malady.

				continue;
			}

			if (mal.getFindings().containsAll(actualFindings)) {
				if (debugFlag ) {
					System.out.println(
							"Selected actualFindings " +
							repr(actualFindings) +
							" of " +
							repr(scenariosMalady) +
							" are a subset of " +
							repr(mal));
				}
				return true;
			}
		}

		// We didn't find a matching top-level malady. That's usually a good
		// thing.

		return false;
    }
	 */


	public void getPossibleAndImpossibleTreatments(
		EnumSet<TreatmentIlk> correctTreatments,
		Set<TreatmentIlk> incorrectTreatments)
	{
		if (correctTreatments == null || correctTreatments.size() > 0) {
			throw new IllegalArgumentException("correctTreatments must be non-null and empty");
		}

		if (incorrectTreatments == null || incorrectTreatments.size() > 0) {
			throw new IllegalArgumentException("incorrectTreatments must be non-null and empty");
		}

		Scenario scenario = model.getScenario();
		Malady malady = scenario.getMalady();
		
		correctTreatments.addAll(malady.getTreatments());
		EnumSet<TreatmentIlk> incorrectSet = EnumSet.complementOf(correctTreatments);
		
		
		incorrectTreatments.addAll(incorrectSet);
	}

	/**
	 * Determines what scenario finding ilks match a given malady.
	 *
	 * Comparing the ilk of each finding instead of the actual Finding instances
	 * is only slightly inaccurate.
	 */
	public Collection<FindingIlk> getScenarioFindingIlksMatching(Malady selectedAnswer) {
		
		Set<FindingIlk> scenarioFindings = Finding.getIlks(model.getScenario()
			.getActualFindings());

		Set<FindingIlk> findingsFromSelected = Finding.getIlks(selectedAnswer
			.getFindingsDeep());

		return intersection(scenarioFindings, findingsFromSelected);
	}

	/**
	 * Return the completed set of findings in the scenario that match the given
	 * malady.
	 */
	public Collection<FindingIlk> getScenarioFindingsAgainst(Malady selectedAnswer)
	{
		Set<FindingIlk> actualFindingIlks = EnumSet.copyOf(model
			.getScenario()
			.getActualFindings()
			.stream()
			.map(x -> x.getIlk())
			.collect(Collectors.toList())
			);
		
		Set<FindingIlk> findingIlksForSelectedMalady = EnumSet.copyOf(selectedAnswer
			.getFindingsDeep()
			.stream()
			.map(x -> x.getIlk())
			.collect(Collectors.toList())
			);
		
		actualFindingIlks.removeAll(findingIlksForSelectedMalady);
		
		return actualFindingIlks;
	}

	private List<Malady> improveDistractorsByAddingFacts(Malady correctAnswer, Collection<Finding> factsInUse,
		int numDesiredGoodDistractors, Scenario scenario)
	{
		List<Malady> result = null;
		List<Malady> implausibleChoices = new ArrayList<>();
		List<Malady> plausibleChoices = new ArrayList<>();
		
		// This while-condition never changes. It's just a clever way avoiding a lot of
		// code that only cares if we want at least one attractive distractor.
		//
		while (numDesiredGoodDistractors >= 1)
		{

			// Determine which choice are plausible.
			plausibleChoices.clear();
			implausibleChoices.clear();
			getPossibleAndImpossibleMaladies(plausibleChoices, implausibleChoices);


			int numberOfPerfectDistractors = sortByAttractiveness(implausibleChoices, correctAnswer,
				factsInUse, false);

			// implausibleChoices is now sorted.
			
			if (numberOfPerfectDistractors >= numDesiredGoodDistractors) {
				break;
			}
			
			sortByAttractiveness(plausibleChoices, correctAnswer,
				factsInUse, true);

			// This returns a list of findings that do not apply to one of the
			// plausible choices, but to apply to the correct answer.
			//
			List<Finding> contrastingFindingsForFirstPlausibleChoice = getFactsForFirstPlausibleChoiceThatCouldBeWeakened(
				plausibleChoices, factsInUse, correctAnswer);

			if (contrastingFindingsForFirstPlausibleChoice == null
				|| contrastingFindingsForFirstPlausibleChoice.size() == 0)
			{
				// We couldn't improve any of the choices. The sorted list of
				// implausibleChoices is the best we have.
				
				break;
			}

			// Narrow the gap of the first plausible choice by one.
			
			scenario.addActualFinding(contrastingFindingsForFirstPlausibleChoice.get(0).actualize());

			// Keep doing this until we have enough distractors (or we run out
			// of plausible choices that can be strengthened).
		}
		
		result = implausibleChoices;  // TODO verify
		return result;
	}

	/**
	 * 
	 * @param plausibleChoices
	 * @param factsInUse
	 * @param correctAnswer
	 * @return applicable findings for first choice that could be weakened, or
	 *         null if no such finding exists
	 */
	private List<Finding> getFactsForFirstPlausibleChoiceThatCouldBeWeakened(List<Malady> plausibleChoices,
		Collection<Finding> factsInUse, Malady correctAnswer)
	{
		List<Finding> result = null;

		Set<FindingIlk> correctAnswerFindingIlks = Finding.getIlks(correctAnswer.getFindingsDeep());
		Set<FindingIlk> ilksOfFactsInUse = Finding.getIlks(factsInUse);

		Set<FindingIlk> correctAnswerFindingIlksNotInUse = EnumSet.copyOf(correctAnswerFindingIlks);
		correctAnswerFindingIlksNotInUse.removeAll(ilksOfFactsInUse);
		
		// For each plausible choice, determine which facts it has that don't
		// "match" the facts that could be used (BUT are not yet in use) to
		// support the correct answer.
		//
		for (Malady choice : plausibleChoices) {
			Set<FindingIlk> choiceFindingIlks = Finding.getIlks(choice.getFindingsDeep());

			Set<FindingIlk> contrastingFindingIlks = EnumSet.copyOf(correctAnswerFindingIlksNotInUse);
			contrastingFindingIlks.removeAll(choiceFindingIlks);

			if (contrastingFindingIlks.size() > 0) {
				result = new ArrayList<>(contrastingFindingIlks.size());
				
				for (FindingIlk filk : contrastingFindingIlks) {
					result.add(correctAnswer.getFindingByIlk(filk));
				}
				
				Collections.shuffle(result);
				
				break;
			}
		}

		return result;
	}

	/** @deprecated */
	private List<Malady> OLDimproveDistractorsByAddingFacts(Malady correctAnswer, Collection<Finding> factsInUse,
		int numDesiredGoodDistractors, Scenario scenario)
	{
		List<Malady> result = new ArrayList<>();
		
		// This while-condition never changes. It's just a clever way avoiding a lot of
		// code that only cares if we want at least one attractive distractor.
		//
		while (numDesiredGoodDistractors >= 1)
		{

			// Gather the incorrect choices.
			//

			List<Malady> ignored = new ArrayList<>();
			result.clear();
			getPossibleAndImpossibleMaladies(ignored, result);


			int numberOfPerfectDistractors = OLDsortInDecreasingOrderOfAttractiveness(result, correctAnswer,
				factsInUse);

			// result is now sorted.
			
			if (numberOfPerfectDistractors >= numDesiredGoodDistractors) {
				break;
			}
			
			List<Finding> applicableFindingsForFirstDistractor = getFactsForFirstDistractorThatCouldBeStrengthened(
				result, factsInUse, correctAnswer);

			if (applicableFindingsForFirstDistractor == null || applicableFindingsForFirstDistractor.size() == 0) {
				// We couldn't improve any of the distractors.  What we have is good enough.
				break;
			}

			// Close the gap of the first item by one.
			
			scenario.addActualFinding(applicableFindingsForFirstDistractor.get(0).actualize());

			// Keep doing this until we have enough distractors (or we run out
			// of distractors that can be strengthened).
		}
		
		return result;
	}

	public void setChiefComplaint(Finding actualFinding) {
		model.getScenario().setChiefComplaint(actualFinding);
	}

	public void setPatientAge(float ageYears) {
		model.getScenario().setPatientAgeYears(ageYears);
	}

	void setPatientAgeAccordingTo(List<Finding> actualFindings) {
		Scenario scenario = model.getScenario();
		boolean ageWasDetermined = false;
		
		for (Finding finding : actualFindings) {
			FindingIlk findingIlk = finding.getIlk();

			if (findingIlk.getSpeciesSet().contains(FindingIlkSpecies.AGE)) {
				switch (findingIlk) {
				
				case AGE_BETWEEN_20_AND_40:
					setPatientAgeRandomRange(scenario, 20.0f, 40.1f);
					ageWasDetermined = true;
					break;
					
				case AGE_BETWEEN_30_AND_50:
					setPatientAgeRandomRange(scenario, 30.0f, 50.1f);
					ageWasDetermined = true;
					break;

				case AGE_GERIATRIC:
					setPatientAgeRandomRange(scenario, 65.0f,
						IxosModel.MAXIMUM_PATIENT_AGE + 0.1f);
					
					ageWasDetermined = true;
					break;

				case AGE_LESS_THAN_18:
					setPatientAgeRandomRange(scenario,
						IxosModel.MINIMUM_PATIENT_AGE, 18.0f);
					
					ageWasDetermined = true;
					break;

				case HX_VENOUS_POOLING_ASSOCIATED_WITH_PREGNANCY:
					// Child-bearing age
					setPatientAgeRandomRange(scenario, 12.0f, 50.1f);
					ageWasDetermined = true;
					break;
					
				default:
					throw new AssertionError("unrecognized FindingIlk "
						+ findingIlk + " that has " + FindingIlkSpecies.AGE
						+ " as one of its species");
				}
			}
			
			if (ageWasDetermined) break;
		}

		if (!ageWasDetermined) {
			setPatientAgeRandomRange(scenario, IxosModel.MINIMUM_PATIENT_AGE, IxosModel.MAXIMUM_PATIENT_AGE + 0.1f);
		}
	}

	
	private void setPatientAgeRandomRange(Scenario scenario, float minAgeInclusive,
		float maxAgeExclusive)
	{
		scenario.setPatientAgeYears(random.nextFloat() * (maxAgeExclusive - minAgeInclusive) + minAgeInclusive);
	}

	public void setPatientSex(IxosModel.Sex sex) {
		model.getScenario().setPatientSex(sex);
	}

	void setPatientSexAccordingTo(List<Finding> actualFindings, Scenario scenario)
	{
		boolean sexWasDetermined = false;
		
		for (Finding finding : actualFindings) {
			FindingIlk findingIlk = finding.getIlk();
			if (findingIlk.getSpeciesSet().contains(FindingIlkSpecies.SEX)) {
				switch (findingIlk) {

				case SEX_FEMALE:
				case UTERUS_PAIN:
				case HX_VENOUS_POOLING_ASSOCIATED_WITH_PREGNANCY:
					scenario.setPatientSex(FEMALE);
					sexWasDetermined = true;
					break;

				case SEX_MALE:
					scenario.setPatientSex(MALE);
					sexWasDetermined = true;
					break;

				default:
					throw new AssertionError("unrecognized FindingIlk "
						+ findingIlk + " that has " + FindingIlkSpecies.SEX
						+ " as one of its species");
				}
			}

			if (sexWasDetermined) break;
		}
		
		if (!sexWasDetermined) {
			// Make the sex random.
			boolean isFemale = random.nextBoolean();
			
			if (isFemale) {
				scenario.setPatientSex(FEMALE);
			}
			else {
				scenario.setPatientSex(MALE);
			}
		}
		
	}

	private void setRandomChiefComplaint(Collection<Finding> actualFindingsDoNotTouch) {
		List<Finding> actualFindings = new ListFrom<>(actualFindingsDoNotTouch);
		int numberOfActualFindings = actualFindings.size();
		Scenario scenario = model.getScenario();
		
		if (scenario.getChiefComplaint() == null) {
			Collections.shuffle(actualFindings);
			//assert numberOfActualFindings > 0;

			boolean foundACC = false;
			// keep going through the findings until we find a symptom (as opposed to a sign)
			for (Finding finding : actualFindings) {
				if (finding.getIlk().canBeChiefComplaint()) {
					foundACC = true;
					scenario.setChiefComplaint(finding);
					break;
				}
			}

			if (!foundACC) {
				scenario.setChiefComplaint(Finding.INDETERMINATE_SYMPTOM_FINDING);
			}
		}
	}
	
	void setupAttractiveDistractors(Scenario scenario, Scenario.Ilk scenIlk) {
		/*
		 * In this game, we define the best distractor as one that does not
		 * match all of the facts available, but it matches all but one. Such a
		 * distractor is not guaranteed to exist for all possible subsets of
		 * findings for all maladies. For ease of analysis, let's assume we are
		 * only looking to maximize the effectiveness of one distractor.
		 *
		 * Let's define some variables: numAnswerMatches are the number of facts
		 * available that match the correct answer. Similarly, the number of
		 * facts that match the distractor is numDistractorMatches.
		 *
		 * Let's define the distractor's gap as numAnswerMatches -
		 * numDistractorMatches. The best possible distractor has gap == 1. A
		 * distractor with gap < 1 is unfair, because the distractor is just as
		 * strong a choice as (or a stronger choice than) the answer. As the gap
		 * widens (i.e., gets larger), the distractor loses attractiveness.
		 *
		 * Adding a good fact that matches the answer, but not the distractor,
		 * increases the gap by one. This is very helpful if numAnswerMatches <=
		 * numDistractorMatches, which happens whenever the distractor is a
		 * plausible answer:
		 *
		 * numAnswerMatches' = numAnswerMatches + 1,
		 *
		 * => gap' = (numAnswerMatches + 1) - numDistractorMatches
		 *
		 * => gap' = gap + 1.
		 *
		 *
		 * Adding a fact that matches both the correct answer and the distractor
		 * does not change the gap for that distractor. It increases both
		 * numAnswerMatches and numDistractorMatches by one, so the gap does not
		 * change. This is only useful when trying to keep the gap the
		 * same for one distractor, but to increase it for another distractor
		 * (assuming the fact does NOT match the other distractor).
		 *
		 * Adding a red herring -- a fact that matches the distractor, but not
		 * the answer -- reduces the gap by one:
		 *
		 * numDistractorMatches' = numDistractorMatches + 1 => gap' =
		 * numAnswerMatches - (numDistractorMatches + 1) => gap' = gap - 1.
		 *
		 * However, such a practice may legitimately distract the player from a
		 * less serious condition to a more serious one. For example, if there
		 * are 3 strong indicators for an acute MI, but 4 for acute pulmonary
		 * edema, the player might be much more concerned about the former. I
		 * suppose this might work for red herrings that aren't alarming, such
		 * as a history, event, medication, or minor sign/symptom (for some
		 * unknown definition of minor). However, I just cannot see this as a
		 * good idea.
		 *
		 * The gap is a delicate balance, especially when attempting to maximize
		 * more than one distractor. Because the only way to safely alter the
		 * gaps of the various distractors is by adding facts that do match the
		 * correct answer ...
		 *
		 * It is best to start with a minimal number of answer matches (i.e.,
		 * 2)!
		 *
		 * In MALADY_FROM_FINDINGS, attractive (good) distractors are easier to
		 * handle: select the set of maladies that match as many of the findings
		 * as possible, but not all of them. There is a many-to-many
		 * relationship between finding and maladies. While some findings are
		 * only related to one malady, all top-level maladies have more than one
		 * finding.
		 *
		 * In TREATMENT_FROM_MALADIES, it's more difficult. We still have a
		 * many-to-many relationship between maladies and treatments. However,
		 * some treatments only correspond to one malady. Because finding
		 * attractive distractors tends to increase the number of presented
		 * facts and then adjusting the choices from them, it may be best to use
		 * MALADY_FROM_TREATMENTS instead. Otherwise, on higher difficulty
		 * settings, the player may never see unique treatments such as
		 * PROTECT_PARALYZED_EXTREMITIES.
		 *
		 * Therefore, if there is a many-to-many relationship from set A to set
		 * B, but some items in set A only relate to one element of set B, it is
		 * better to present the items in set A as facts, not choices.
		 * Otherwise, they are quickly eliminated as poor distractors.
		 */
		
		if (scenIlk == Scenario.Ilk.MALADY_FROM_FINDINGS) {
			generateMultipleChoiceMaladies(scenario);
		}
		else {
			throw new NYIException();
		}
	}
	
	/**
	 * @deprecated
	 * 
	 * @param distractorsToSort
	 *            input/output variable for the distractors. After this method
	 *            returns, this list is sorted in decreasing order of
	 *            approximate goodness, with the best first.
	 *
	 * @return the count of distractors where gap == 1
	 */
	private int OLDsortInDecreasingOrderOfAttractiveness(List<Malady> distractorsToSort, Malady correctAnswer, Collection<Finding> factsInUse)
	{
		TreeMap<Integer,List<Malady>> gapToDistractors = new TreeMap<>();
		
		int numFactsMatchingCorrectAnswer = countMatchingFacts(correctAnswer, factsInUse);
		
		for (Malady distractor : distractorsToSort) {
			int gap = computeGap(distractor, numFactsMatchingCorrectAnswer, factsInUse);

			List<Malady> distractorsHavingThatGap = gapToDistractors.get(gap);
			if (distractorsHavingThatGap == null) {
				distractorsHavingThatGap = new ArrayList<>();
			}

			distractorsHavingThatGap.add(distractor);

			gapToDistractors.put(gap, distractorsHavingThatGap);
		}

		// We now know the gap for each distractor.

		// Add them in increasing order of gap size (with a little shuffling for good measure).
		// Take advantage of the TreeMap's "keys are always sorted" property.

		distractorsToSort.clear();
		for (Entry<Integer, List<Malady>> entry : gapToDistractors.entrySet()) {
			// "The set's iterator returns the entries in ascending key order." --javadoc for TreeMap#entrySet

			List<Malady> listOfDistractorsHavingThisGap = entry.getValue();
			
			Collections.shuffle(listOfDistractorsHavingThisGap);
			distractorsToSort.addAll(listOfDistractorsHavingThisGap);
		}
		
		int result = 0;
		List<Malady> distractorsHavingGap1 = gapToDistractors.get(1);
		if (distractorsHavingGap1 != null) {
			// Distractors with gap == 1 are interesting.

			result = distractorsHavingGap1.size();
		}

		return result;
	}
	
	/**
	 * @param distractorsToSort
	 *            input/output variable for the distractors. After this method
	 *            returns, this list is sorted according to the increasing
	 *            parameter.
	 *
	 * @param increasing
	 *            if true, sort from least to most attractive; otherwise, sort
	 *            from most to least attractive.
	 * 
	 * @return the count of distractors where gap == 1
	 */
	private int sortByAttractiveness(List<Malady> distractorsToSort, Malady correctAnswer, Collection<Finding> factsInUse,
		boolean increasing)
	{
		TreeMap<Integer,List<Malady>> gapToDistractors = new TreeMap<>();
		
		int numFactsMatchingCorrectAnswer = countMatchingFacts(correctAnswer, factsInUse);

		// Populate gapToDistractors.
		//
		for (Malady distractor : distractorsToSort) {
			int gap = computeGap(distractor, numFactsMatchingCorrectAnswer, factsInUse);

			List<Malady> distractorsHavingThatGap = gapToDistractors.get(gap);
			if (distractorsHavingThatGap == null) {
				distractorsHavingThatGap = new ArrayList<>();
			}

			distractorsHavingThatGap.add(distractor);

			gapToDistractors.put(gap, distractorsHavingThatGap);
		}

		// We now know the gap for each distractor.

		// Add them in increasing order of gap size (with a little shuffling for good measure).
		// Take advantage of the TreeMap's "keys are always sorted" property.

		distractorsToSort.clear();
		for (Entry<Integer, List<Malady>> entry : gapToDistractors.entrySet()) {
			// "The set's iterator returns the entries in ascending key order." --javadoc for TreeMap#entrySet

			List<Malady> listOfDistractorsHavingThisGap = entry.getValue();
			
			Collections.shuffle(listOfDistractorsHavingThisGap);
			if (increasing) {
				distractorsToSort.addAll(0, listOfDistractorsHavingThisGap);
			}
			else {
				distractorsToSort.addAll(listOfDistractorsHavingThisGap);
			}
		}
		
		int result = 0;
		List<Malady> distractorsHavingGap1 = gapToDistractors.get(1);
		if (distractorsHavingGap1 != null) {
			// Distractors with gap == 1 are interesting.

			result = distractorsHavingGap1.size();
		}

		return result;
	}
	
	public void startNewScenario(int minimumActualFindings) {
		startNewScenario(minimumActualFindings, null, Scenario.Ilk.MALADY_FROM_FINDINGS);
	}
	
	/**
	 * Set up a scenario and its malady choices with specific difficulty
	 * settings.
	 *
	 * @param mininumActualFindings
	 *            the minimum number of findings to place in the scenario
	 *
	 * @param numAttractiveDistractors
	 *            the exact number of attractively distracting maladies to place
	 *            in the scenario; null means "don't care"
	 */
	public void startNewScenario(int mininumActualFindings,
		Integer numAttractiveDistractors, Scenario.Ilk scenIlk)
	{
		model.clearCurrentScenario();
		Scenario scenario = model.getScenario();
		
		scenario.setMinimumActualFindings(mininumActualFindings);
		scenario.setNumAttractiveDistractors(numAttractiveDistractors);
		
		fillInRestOfScenario(scenario, scenIlk);
	}
	
	public void startNewScenario(Malady malady, int minimumActualFindings) {
		model.clearCurrentScenario();
		Scenario scenario = model.getScenario();
		
		scenario.setMalady(malady);
		scenario.setMinimumActualFindings(minimumActualFindings);
		
		fillInRestOfScenario(scenario, Scenario.Ilk.MALADY_FROM_FINDINGS);
	}
	
	/** @deprecated */
	@Deprecated
	public List<Malady> ZZOLDgenerateMultipleChoiceMaladies(int maxMultipleChoiceAnswers) {
		Malady correctAnswer = getMalady();

		// Gather the incorrect answers and shuffle them.
		//
		
		List<Malady> ignored = new ArrayList<>();
		List<Malady> incorrectMaladies = new ArrayList<>();

		getPossibleAndImpossibleMaladies(ignored, incorrectMaladies);
		
		Collections.shuffle(incorrectMaladies);


		// Build the multiple choice answers and shuffle them.
		//
		
		ArrayList<Malady> choices = new ArrayList<>(maxMultipleChoiceAnswers);

		int choicesIx;
		for (choicesIx = 0;
			choicesIx < maxMultipleChoiceAnswers - 1 &&
			choicesIx < incorrectMaladies.size();
			choicesIx++)
		{
			choices.add(incorrectMaladies.get(choicesIx));
		}

		choices.add(correctAnswer);
		
		assert choices.size() <= maxMultipleChoiceAnswers;
		
		Collections.shuffle(choices);
		return choices;
	}
	
}
