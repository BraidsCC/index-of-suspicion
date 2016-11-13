package cc.braids.app.ixos.console;

import static cc.braids.app.ixos.console.AndroidEmulation.getResources;
import static cc.braids.util.UFunctions.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import cc.braids.app.ixos.DispatchReport;
import cc.braids.app.ixos.IxosController;
import cc.braids.app.ixos.Malady;
import cc.braids.app.ixos.Scenario;
import cc.braids.app.ixos.Scenario.Ilk;
import cc.braids.app.ixos.model.Finding;
import cc.braids.app.ixos.model.FindingIlk;
import cc.braids.app.ixos.model.IxosModel;
import cc.braids.app.ixos.model.IxosModel.Sex;
import cc.braids.app.ixos.model.MaladyDatabaseEnums.TreatmentIlk;
import cc.braids.util.*;

@SuppressWarnings("unused")
public class TextView {
	
	private static final String DIFFICULTY_RECORD_FILE_NAME = "difficulty-record.txt";
	private static int NUMBER_OF_MULTIPLE_CHOICE_ANSWERS = 4;

	//private static final Pattern restOfLine = Pattern.compile(".*?$");
	public static void main(String[] args) {
		try {
			TextView me = new TextView();
	        System.exit(me.run());
		} catch (Exception e) {
	        throw new RuntimeException(e);
        }
	}

	private IxosModel model;
	private IxosController controller;
	//private Scanner scanner;
	private BufferedReader stdin;
	
	//////////////////////////////////////////////////////////////////
	// Preferences

	// minimum is 3 for now, otherwise we get grammar errors
	private int preferenceMinimumActualFindings = 3;

	private int prefAttractiveDistractors = 1;
	private Writer difficultyRecordWriter;

	// Sets the difficulty level...  need statistics to make this work better.
	//
	// TODO deferred 20150825 private float preferenceDifficultyLevel = .33f  

	//
	//////////////////////////////////////////////////////////////////

	public TextView() throws IOException, ClassNotFoundException {

		String resourcePath = "/malady-db.ser";

		try (InputStream maladyDBInStream = 
				getClass().getResourceAsStream(resourcePath))
		{
		
			model = new IxosModel(maladyDBInStream);
		}
		
		controller = new IxosController(model);
		//scanner = new Scanner(System.in);
		stdin = new BufferedReader(new InputStreamReader(System.in));
    }
	
	private int run() throws IOException {
		while (true) {
			echo("Welcome to Index of Suspicion, the premier EMT simulation game!");
			echo();

			int response = ask(
			  "What would you like to do?  Type your choice and press the Enter key.",
			  new String[] { "Play a round of 10 questions",
					  		 "Change preferences",
					  		 "Exit XYZ"});

			if (response == 0) {
				play(10);
			}
			else if (response == 1) {
				echo("Sorry, that feature is not yet supported.");
				echo();
			}
			else {
				echo("Goodbye!");
				return 0;
			}
		}
	}

	private void play(int numQuestions) throws IOException {
		for (int ix = 1; ix <= numQuestions; ix++) {
			
			difficultyRecordWriter = new FileWriter(DIFFICULTY_RECORD_FILE_NAME, true);

			echo("Question " + ix + ":");
			playMaladyFromFindings();
			
			difficultyRecordWriter.close();
			//playTreatmentFromFindings();  // Uses dispatch report			
		}
    }

	/**
	 * Ask and grade a question where the player must guess the malady from a set of findings.
	 * 
	 * @throws IOException
	 */
	public void playMaladyFromFindings() throws IOException {
		int minActualFindings = randomRangeInclusive(3, 5);
		int numGoodDistractors = randomRangeInclusive(1, 2);
		
	    controller.startNewScenario(minActualFindings, numGoodDistractors, Scenario.Ilk.MALADY_FROM_FINDINGS);

	    Malady correctMalady = controller.getMalady();

	    displayFindings(controller);

		List<Malady> maladyChoices = 
				controller.generateMultipleChoiceMaladies( 
						NUMBER_OF_MULTIPLE_CHOICE_ANSWERS);

		List<String> choices = maladyChoices.stream().
				map(x -> localizedToString(x)).
				collect(Collectors.toList());
		
		// Pop the question!
		//
		int userChoice = ask(//"Of what is your Index of Suspicion the highest?", 
				"Which of the following best matches the scenario?",
				choices);

		Malady selectedAnswer = maladyChoices.get(userChoice);

		// Because the correct answer has been shuffled, we have to find it
		// by its name, not its index.
		//
		boolean userGuessedCorrectly = selectedAnswer == correctMalady;
		
		echo("minActualFindings = " + minActualFindings + "; numGoodDistractors = " + numGoodDistractors);
		recordDifficulty(minActualFindings, numGoodDistractors, userGuessedCorrectly);

		if (userGuessedCorrectly) {
			echo("Correct!");
		}
		else {
			echo("I am sorry, that is not correct.");
			echo("The correct answer is " + repr(localizedToString(correctMalady)) + ".");
		}
		
		echo("Here are the findings in the scenario that match the other choices.");

		for (Malady malady : maladyChoices) {
			if (malady == correctMalady) {
				continue;
			}

			Collection<FindingIlk> findingsThatMatchIncorrectAnswer = 
					controller.getScenarioFindingIlksMatching(malady);
			
			echo(localizedToString(malady) + ": " + repr(findingsThatMatchIncorrectAnswer));
			
			/* Commented out because this was less useful.
			echo("Findings in the scenario that do not match what you picked:");
			echo(repr(findingsThatDoNotMatchIncorrectAnswer));
			echo();
			*/
		}
	
		/* commented out because these are producing too much output.
		List<String> findingsThatOverlap = 
				new ArrayList<>();

		List<String> findingsOnlyInCorrectAnswer = 
				new ArrayList<>();
		
		List<String> findingsOnlyInSelectedAnswer = 
				new ArrayList<>();
		
		controller.differentiate(findingsThatOverlap,
				findingsOnlyInCorrectAnswer,
				findingsOnlyInSelectedAnswer,
				correctAnswer, selectedAnswer);

		echo(String.format("Here is a comparison of %s (incorrect) and %s (correct).", 
				selectedAnswer.getName(), correctAnswer.getName()));
		echo();
		echo("These findings are in both:");
		echo(repr(findingsThatOverlap));
		echo();
		echo("Findings only in " + selectedAnswer.getName() + ":");
		echo(repr(findingsOnlyInSelectedAnswer));
		echo();
		echo("Findings only in " + correctAnswer.getName() + ":");
		echo(repr(findingsOnlyInCorrectAnswer));
		echo();
		*/
		
		echo();
    }

	private static String localizedToString(Malady malady) {
		return getResources().getString(malady.getName());
	}
	
	private static int randomRangeInclusive(int minPossible, int maxPossible) {
		return minPossible + IxosController.random.nextInt(maxPossible - minPossible + 1);
	}

	private void recordDifficulty(Integer minActualFindings, Integer numGoodDistractors, boolean userGuessedCorrectly)
		throws IOException
	{
		Writer dr = difficultyRecordWriter;
		dr.write(minActualFindings.toString());
		dr.write('\t');
		dr.write(numGoodDistractors.toString());
		dr.write('\t');
		if (userGuessedCorrectly) {
			dr.write('1');
		}
		else {
			dr.write('0');
		}
		dr.write('\n');

		dr.flush();
	}

	public void playTreatmentFromFindings() throws IOException {
	    controller.startNewScenario(preferenceMinimumActualFindings);

	    Set<TreatmentIlk> correctTreatmentSet = controller.getMalady().getTreatments();

	    displayFindings(controller);

		List<TreatmentIlk> treatmentChoices =
				new ArrayList<>(NUMBER_OF_MULTIPLE_CHOICE_ANSWERS);
		
		TreatmentIlk correctTreatment = 
				controller.generateMultipleChoiceTreatments( 
						treatmentChoices, NUMBER_OF_MULTIPLE_CHOICE_ANSWERS);

		List<String> choices = treatmentChoices
				.stream()
				.map(x -> x.toString())
				.collect(Collectors.toList());
		
		// Pop the question!
		//
		int userChoice = ask(//"Of what is your Index of Suspicion the highest?", 
				"Which of the following best matches the scenario?",
				choices);

		TreatmentIlk selectedAnswer = treatmentChoices.get(userChoice);
		
		// Because the correct answer has been shuffled, we have to find it
		// by its name, not its index.
		//
		if (selectedAnswer == correctTreatment) {
			echo("Correct!");
		}
		else {
			echo("I am sorry, that is not correct.");
			echo("The correct answer is " + repr(correctTreatment + "."));
		}
		echo_n("The patient is likely suffering from ");
		echo_n(localizedToString(controller.getMalady()));
		echo(".");
		
		echo();
    }

	private static void displayFindings(IxosController controller) {
	    DispatchReport report = controller.getDispatchReport();
		
		float ptAge = report.getAge();
		
		String ageAsString;
		if (ptAge <= 2.0) {
			ageAsString = (int) ptAge * 12.0 + "-month-old child"; 
		}
		else {
			ageAsString = (int) ptAge + "-year-old";
		}
		
		Sex ptSex = report.getSex();
		String sexAsString;
		if (ptSex == Sex.FEMALE) {
			sexAsString = "female";
		} else {
			sexAsString = "male";
		}

		String childOrPerson;
		if (ptAge < 18.0f) {
			childOrPerson = "child";
		}
		else {
			childOrPerson = "person";
		}

		echo_n(String.format(
				"You have been dispatched to a(n) %s %s %s complaining of %s.  ",
				ageAsString, sexAsString, childOrPerson, 
				report.getChiefComplaint()));
		
		echo_n("Other findings include ");
		
		Collection<Finding> findings = controller.getOtherFindings();
		int totalFindings = findings.size();

		int ix = -1;
		for (Finding finding : findings) {
			ix++;

			if (ix == 0) {
				echo_n(finding.toString());
			}
			else if (ix < totalFindings - 1) {
				echo_n(", " + finding);
			}
			else {
				echo(", and " + finding + ".");
			}
		}
    }

	private int ask(String question, String[] choices) throws IOException {
		return ask(question, Arrays.asList(choices));
	}

	private int ask(String question, List<String> choices) throws IOException {
		while (true) {
			echo(question);
			echo();
			
			int ix = 0;
			for (String choiceText : choices) {
				echo(ix + ".  " + choiceText);
				ix++;
			}
			echo_n("? ");

			int result;
			String line = stdin.readLine();
			try {
				result = Integer.parseInt(line);
			}
			catch (NumberFormatException exn) {
				echo("I didn't understand that.  Please try again.");
				echo();
				continue;
			}
			
			if (result < 0 || result >= choices.size()) {
				echo("That is not one of the choices.  Please try again.");
				echo();
				continue;
			}

			echo();
			return result;
		}
    }	
}
