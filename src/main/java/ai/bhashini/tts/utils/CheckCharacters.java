package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.cli.ParseException;

public class CheckCharacters {
	ArrayList<String> txtFilePaths = new ArrayList<>();
	HashMap<String, String> replacements = new HashMap<>();
	HashSet<Character> validCharacters = new HashSet<>();
	boolean verbose = false;

	public CheckCharacters(boolean verbose) {
		this.verbose = verbose;
	}

	void loadTextFilePaths(File dataDir) {
		txtFilePaths.clear();
		File[] subDirs = FileUtils.getSubDirs(dataDir);
		for (File subDir : subDirs) {
			File wavDir = new File(subDir, "wav");
			File txtDir = new File(subDir, "txt");
			if (wavDir.exists() && txtDir.exists()) {
				if (verbose) {
					System.out.println("\t" + subDir.getAbsolutePath());
				}
				File[] wavFiles = FileUtils.getWavFiles(wavDir);
				for (File wavFile : wavFiles) {
					String wavFileName = wavFile.getName();
					File txtFile = new File(txtDir, wavFileName.replace(".wav", ".txt"));
					if (txtFile.exists()) {
						txtFilePaths.add(txtFile.getAbsolutePath());
					} else {
						System.out.println("No matching txt file found for " + wavFile.getAbsolutePath());
					}
				}
			}
		}
		Collections.sort(txtFilePaths, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return new File(o1).getName().compareTo(new File(o2).getName());
			}
		});
		if (verbose) {
			System.out.println("Total # of files = " + txtFilePaths.size());
		}
	}

	void concatenateTxtFiles(File outputFile) {
		outputFile.getParentFile().mkdirs();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
			for (int i = 0; i < txtFilePaths.size(); i++) {
				String txtFilePath = txtFilePaths.get(i);
				if (verbose) {
					System.out.println((i + 1) + ") " + txtFilePath);
				}
				String txt = FileUtils.getFileContents(txtFilePath).replaceAll("\n", " ").trim();
				String promptId = FileUtils.getFileNameWithoutExtension(new File(txtFilePath).getName());
				bw.write(promptId + "\t" + txt + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	HashMap<Character, Integer> getUniqueCharactersWithCount(File txtFile) {
		HashMap<Character, Integer> symbolsCount = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(txtFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\t");
				if (contents.length < 2) {
					System.out.println("Skipping line " + line);
					continue;
				}
				String unicodeText = contents[1];
				for (int i = 0; i < unicodeText.length(); i++) {
					char symbol = unicodeText.charAt(i);
					Integer priorCount = symbolsCount.get(symbol);
					if (priorCount == null) {
						priorCount = 0;
					}
					symbolsCount.put(symbol, priorCount + 1);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return symbolsCount;
	}

	void createUniqueCharactersFile(File inFile, File outFile) {
		HashMap<Character, Integer> symbolsCount = getUniqueCharactersWithCount(inFile);
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
			ArrayList<Character> chars = new ArrayList<Character>(symbolsCount.keySet());
			Collections.sort(chars);
			for (Character c : chars) {
				bw.write(c.toString() + "\t" + symbolsCount.get(c) + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void loadReplacements(File replacementsTSVFile) {
		replacements.clear();
		try (BufferedReader br = new BufferedReader(new FileReader(replacementsTSVFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\t");
				if (contents[1].equalsIgnoreCase("delete")) {
					contents[1] = "";
				}
				replacements.put(contents[0], contents[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void loadValidCharacters(File validCharactersFile) {
		validCharacters.clear();
		validCharacters.add(' '); // space
		try (BufferedReader br = new BufferedReader(new FileReader(validCharactersFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				validCharacters.add(line.trim().charAt(0));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void saveTranscriptsWithInvalidCharacters(Language language, File concatenatedTranscriptsFile,
			File transcriptsWithNumbersFile, File transcriptsWithEnglishLettersFile,
			File transcriptsWithInvalidCharactersFile) {
		int zero = language.script.digitZero;
		int nine = language.script.digitNine;
		NumberExpansion numberExpansion = NumberExpansion.getInstance(language);
		try (BufferedReader br = new BufferedReader(new FileReader(concatenatedTranscriptsFile));
				BufferedWriter bwNumbers = new BufferedWriter(new FileWriter(transcriptsWithNumbersFile));
				BufferedWriter bwEnglishLetters = new BufferedWriter(new FileWriter(transcriptsWithEnglishLettersFile));
				BufferedWriter bwInvalidCharacters = new BufferedWriter(new FileWriter(transcriptsWithInvalidCharactersFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\t");
				String transcript = contents[1];
				if (hasNumbers(transcript, zero, nine)) {
					bwNumbers.write(line + "\n");
				}
				if (hasEnglishLetters(transcript)) {
					bwEnglishLetters.write(line + "\n");
				} else {
					transcript = numberExpansion.removeNumbersAndCurlyBrackets(transcript);
					ArrayList<Character> invalidCharacters = getInvalidCharacters(transcript, validCharacters);
					if (invalidCharacters.size() > 0) {
						bwInvalidCharacters.write(line + "\t" + invalidCharacters.toString() + "\n");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean hasEnglishLetters(String transcript) {
		for (int i = 0; i < transcript.length(); i++) {
			char c = transcript.charAt(i);
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasNumbers(String transcript, int zero, int nine) {
		for (int i = 0; i < transcript.length(); i++) {
			char c = transcript.charAt(i);
			if ((c >= '0' && c <= '9') || (c >= zero && c <= nine)) {
				return true;
			}
		}
		return false;
	}

	public static ArrayList<Character> getInvalidCharacters(String transcript, HashSet<Character> validCharacters) {
		ArrayList<Character> invalidCharacters = new ArrayList<>();
		for (int i = 0; i < transcript.length(); i++) {
			char c = transcript.charAt(i);
			if (!validCharacters.contains(c)) {
				invalidCharacters.add(c);
			}
		}
		return invalidCharacters;
	}

	String replaceText(String txt) {
		String newTxt = txt;
		for (String oldStr : replacements.keySet()) {
			String newStr = replacements.get(oldStr);
			newTxt = newTxt.replaceAll(oldStr, newStr);
		}
		// Replace pipe symbol with Devanagari danda
		newTxt = newTxt.replaceAll("[|]", "।");
		return newTxt;
	}

	void removeExtraSpaces() {
		for (String txtFilePath : txtFilePaths) {
			String txt = FileUtils.getFileContents(txtFilePath).replaceAll("\n", " ").trim();
			String newTxt = txt.replaceAll("\\s+", " ");
			if (!newTxt.contentEquals(txt)) {
				if (verbose) {
					System.out.println(txtFilePath + "\n\t" + txt + "\n\t" + newTxt);
				}
				FileUtils.createFileWithContents(txtFilePath, newTxt);
			}
		}
	}

	void expandAbbreviationsInTranscriptFiles(Language language) {
		AbbreviationExpansion abbreviationExpansion = AbbreviationExpansion.getInstance(language);
		for (String txtFilePath : txtFilePaths) {
			String txt = FileUtils.getFileContents(txtFilePath).replaceAll("\n", " ").trim();
			String newTxt = abbreviationExpansion.expandAbbreviations(txt);
			if (!newTxt.contentEquals(txt)) {
				if (verbose) {
					System.out.println(txtFilePath + "\n\t" + txt + "\n\t" + newTxt);
				}
				FileUtils.createFileWithContents(txtFilePath, newTxt);
			}
		}
	}

	void expandNumbersInTranscriptFiles(Language language, boolean retainNumbersForValidation) {
		NumberExpansion numberExpansion = NumberExpansion.getInstance(language);
		for (String txtFilePath : txtFilePaths) {
			String txt = FileUtils.getFileContents(txtFilePath).replaceAll("\n", " ").trim();
			String newTxt = numberExpansion.expandNumbers(txt, retainNumbersForValidation);
			if (!newTxt.contentEquals(txt)) {
				if (verbose) {
					System.out.println(txtFilePath + "\n\t" + txt + "\n\t" + newTxt);
				}
				FileUtils.createFileWithContents(txtFilePath, newTxt);
			}
		}
	}

	void replaceAll() {
		for (String txtFilePath : txtFilePaths) {
			String txt = FileUtils.getFileContents(txtFilePath).replaceAll("\n", " ").trim();
			String newTxt = replaceText(txt);
			if (!newTxt.contentEquals(txt)) {
				if (verbose) {
					System.out.println(new File(txtFilePath).getName() + " -> " + txt + " -> " + newTxt);
				}
				FileUtils.createFileWithContents(txtFilePath, newTxt);
			}
		}
	}

	void replace(File inFile, File outFile) {
		try (BufferedReader br = new BufferedReader(new FileReader(inFile));
				BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\t");
				String id = contents[0];
				String transcript = contents[1];
				String newTranscript = replaceText(transcript);
				bw.write(id + "\t" + newTranscript + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class Arguments extends CommandLineOptions {
		String defaultConcatenatedTranscriptsFilePath = "filelists/concatenatedTranscripts.txt";
		String defaultUniqueCharactersFilePath = "filelists/uniqueCharacters.txt";
		String defaultReplacementsTSVFilePath = "filelists/replacements.tsv";
		String defaultPrunedTranscriptsFilePath = "filelists/prunedTranscripts.txt";
		String defaultValidCharactersFilePath = "filelists/validCharacters.txt";
		String defaultTranscriptsWithNumbersFilePath = "script/withNumbers.txt";
		String defaultTranscriptsWithEnglishLettersFilePath = "script/withEnglishLetters.txt";
		String defaultTranscriptsWithInvalidCharactersFilePath = "script/withInvalidCharacters.txt";

		StringOption dataDir = new StringOption("dir", "data-dir",
				"Directory containing recordings (<child-dir>/wav/*.wav) and their transcripts (<child-dir>/txt/*.txt)");
		StringOption language = new StringOption("lang", "language", "Language of the input text file");
		StringOption concatenatedTranscriptsFilePath = new StringOption("ctfp", "concatenated-transcripts-filepath",
				"Relative path of text file containing concatenated transcripts " + "(default = '"
						+ defaultConcatenatedTranscriptsFilePath + "')",
				defaultConcatenatedTranscriptsFilePath);
		StringOption uniqueCharactersFilePath = new StringOption("ucfp", "unique-characters-filepath",
				"Relative path of text file containing unique characters along with their unigram count "
						+ "(default = '" + defaultUniqueCharactersFilePath + "')",
				defaultUniqueCharactersFilePath);
		StringOption replacementsTSVFilePath = new StringOption("rfp", "replacements-filepath",
				"Relative path of text file containing the character replacements to be done (in TSV format) "
						+ "(default = '" + defaultReplacementsTSVFilePath + "')",
				defaultReplacementsTSVFilePath);
		StringOption prunedTranscriptsFilePath = new StringOption("out", "pruned-transcripts-filepath",
				"Relative path of file to which pruned transcripts (i.e. after replacements) have to be saved "
						+ "(default = '" + defaultPrunedTranscriptsFilePath + "')",
				defaultPrunedTranscriptsFilePath);
		StringOption validCharactersFilePath = new StringOption("vcfp", "valid-characters-filepath",
				"Relative path of text file containing valid characters" + "(default = '"
						+ defaultValidCharactersFilePath + "')",
				defaultValidCharactersFilePath);
		StringOption transcriptsWithNumbersFilePath = new StringOption("twnfp", "transcripts-with-numbers-filepath",
				"Relative path of text file to which transcripts with numbers have to be saved" + "(default = '"
						+ defaultTranscriptsWithNumbersFilePath + "')",
				defaultTranscriptsWithNumbersFilePath);
		StringOption transcriptsWithEnglishLettersFilePath = new StringOption("twelfp",
				"transcripts-with-english-letters-filepath",
				"Relative path of text file to which transcripts with English letters have to be saved" + "(default = '"
						+ defaultTranscriptsWithEnglishLettersFilePath + "')",
				defaultTranscriptsWithEnglishLettersFilePath);
		StringOption transcriptsWithInvalidCharactersFilePath = new StringOption("twicfp",
				"transcripts-with-invalid-characters-filepath",
				"Relative path of text file to which transcripts with invalid characters have to be saved"
						+ "(default = '" + defaultTranscriptsWithInvalidCharactersFilePath + "')",
				defaultTranscriptsWithInvalidCharactersFilePath);
		BooleanOption concatenateAgain = new BooleanOption("recreate", "concatenate-again",
				"Force recreation of concatenated-text and unique-characters files");
		BooleanOption expandAbbreviations = new BooleanOption("xabbr", "expand-abbreviations",
				"Expand various abbreviations in the transcript files");
		BooleanOption expandNumbers = new BooleanOption("xnums", "expand-numbers",
				"Expand numbers in the transcript files");
		BooleanOption retainNumbersForValidation = new BooleanOption("retain", "retain-numbers",
				"Retain original numbers for validation. Original numbers and their expansion will be enclosed in curly brackets.");
		BooleanOption removeExtraSpaces = new BooleanOption("spaces", "remove-extra-spaces",
				"Replace multiple spaces with a single space in the original transcript files");
		BooleanOption inplaceReplacements = new BooleanOption("inplace", "inplace-replacements",
				"Do character replacements in the original transcript files");
		BooleanOption verbose = new BooleanOption("v", "verbose", "Print each file being processed");

		public Arguments() {
			super();
			dataDir.setRequired(true);
			language.setRequired(true);
			options.addOption(dataDir);
			options.addOption(language);
			options.addOption(concatenatedTranscriptsFilePath);
			options.addOption(uniqueCharactersFilePath);
			options.addOption(replacementsTSVFilePath);
			options.addOption(validCharactersFilePath);
			options.addOption(transcriptsWithNumbersFilePath);
			options.addOption(transcriptsWithEnglishLettersFilePath);
			options.addOption(transcriptsWithInvalidCharactersFilePath);
			options.addOption(concatenateAgain);
			options.addOption(expandAbbreviations);
			options.addOption(expandNumbers);
			options.addOption(retainNumbersForValidation);
			options.addOption(removeExtraSpaces);
			options.addOption(inplaceReplacements);
			options.addOption(verbose);
		}
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(CheckCharacters.class.getCanonicalName());
			return;
		}
		String dataDirPath = arguments.dataDir.getStringValue();
		String languageStr = arguments.language.getStringValue();
		String concatenatedTranscriptsFilePath = arguments.concatenatedTranscriptsFilePath.getStringValue();
		String uniqueCharactersFilePath = arguments.uniqueCharactersFilePath.getStringValue();
		String replacementsTSVFilePath = arguments.replacementsTSVFilePath.getStringValue();
		String prunedTranscriptsFilePath = arguments.prunedTranscriptsFilePath.getStringValue();
		String validCharactersFilePath = arguments.validCharactersFilePath.getStringValue();
		String transcriptsWithNumbersFilePath = arguments.transcriptsWithNumbersFilePath.getStringValue();
		String transcriptsWithEnglishLettersFilePath = arguments.transcriptsWithEnglishLettersFilePath.getStringValue();
		String transcriptsWithInvalidCharactersFilePath = arguments.transcriptsWithInvalidCharactersFilePath.getStringValue();
		boolean concatenateAgain = arguments.concatenateAgain.getBoolValue();
		boolean expandAbbreviations = arguments.expandAbbreviations.getBoolValue();
		boolean expandNumbers = arguments.expandNumbers.getBoolValue();
		boolean retainNumbersForValidation = arguments.retainNumbersForValidation.getBoolValue();
		boolean removeExtraSpaces = arguments.removeExtraSpaces.getBoolValue();
		boolean inplaceReplacements = arguments.inplaceReplacements.getBoolValue();
		boolean verbose = arguments.verbose.getBoolValue();

		File dataDir = new File(dataDirPath);
		Language language;
		try {
			language = Language.valueOf(languageStr);
		} catch (IllegalArgumentException e) {
			System.out.println("Unrecognized language name.");
			return;
		}
		File concatenatedTranscriptsFile = new File(dataDir, concatenatedTranscriptsFilePath);
		File uniqueCharactersFile = new File(dataDir, uniqueCharactersFilePath);
		File replacementsTSVFile = new File(dataDir, replacementsTSVFilePath);
		File prunedTranscriptsFile = new File(dataDir, prunedTranscriptsFilePath);
		File validCharactersFile = new File(dataDir, validCharactersFilePath);
		File transcriptsWithNumbersFile = new File(dataDir, transcriptsWithNumbersFilePath);
		File transcriptsWithEnglishLettersFile = new File(dataDir, transcriptsWithEnglishLettersFilePath);
		File transcriptsWithInvalidCharactersFile = new File(dataDir, transcriptsWithInvalidCharactersFilePath);

		CheckCharacters checkCharacters = new CheckCharacters(verbose);
		if (!concatenatedTranscriptsFile.exists() || concatenateAgain || inplaceReplacements || removeExtraSpaces) {
			System.out.println("Checking .txt files in " + dataDir.getAbsolutePath());
			checkCharacters.loadTextFilePaths(dataDir);
		}
		if (!concatenatedTranscriptsFile.exists() || concatenateAgain) {
			System.out.println("Concatenating all the .txt files to " + concatenatedTranscriptsFile.getAbsolutePath());
			checkCharacters.concatenateTxtFiles(concatenatedTranscriptsFile);
			System.out.println("Successfully completed concatenation.");
		}
		if (!uniqueCharactersFile.exists() || concatenateAgain) {
			System.out.println("Finding unique characters and their unigram count from " + concatenatedTranscriptsFile.getAbsolutePath());
			checkCharacters.createUniqueCharactersFile(concatenatedTranscriptsFile, uniqueCharactersFile);
			System.out.println("Saved unique characters and their count to " + uniqueCharactersFile.getAbsolutePath());
		}
		if (validCharactersFile.exists()) {
			System.out.println("Loading list of valid characters from " + validCharactersFile.getAbsolutePath());
			checkCharacters.loadValidCharacters(validCharactersFile);
			checkCharacters.saveTranscriptsWithInvalidCharacters(language, concatenatedTranscriptsFile,
					transcriptsWithNumbersFile, transcriptsWithEnglishLettersFile,
					transcriptsWithInvalidCharactersFile);
			System.out.println("Saved transcripts with numbers to " + transcriptsWithNumbersFile.getAbsolutePath());
			System.out.println("Saved transcripts with English letters to " + transcriptsWithEnglishLettersFile.getAbsolutePath());
			System.out.println("Saved transcripts containing other invalid characters to "
					+ transcriptsWithInvalidCharactersFile.getAbsolutePath());
		}
		if (replacementsTSVFile.exists()) {
			checkCharacters.loadReplacements(replacementsTSVFile);
			if (inplaceReplacements) {
				checkCharacters.replaceAll();
			} else {
				System.out.println("Replacing specified characters in " + concatenatedTranscriptsFile.getAbsolutePath());
				checkCharacters.replace(concatenatedTranscriptsFile, prunedTranscriptsFile);
				System.out.println("Saved pruned transcripts (after replacements) to " + prunedTranscriptsFile.getAbsolutePath());
			}
		}
		if (removeExtraSpaces) {
			checkCharacters.removeExtraSpaces();
		}
		if (expandAbbreviations) {
			if (inplaceReplacements) {
				checkCharacters.expandAbbreviationsInTranscriptFiles(language);
			} else {
				AbbreviationExpansion abbreviationExpansion = AbbreviationExpansion.getInstance(language);
				System.out.println("Expanding abbreviations in " + concatenatedTranscriptsFile.getAbsolutePath());
				abbreviationExpansion.expandAbbreviationsInFile(concatenatedTranscriptsFile.getAbsolutePath(),
						prunedTranscriptsFile.getAbsolutePath(), verbose);
				System.out.println("Saved transcripts with expansions to " + prunedTranscriptsFile.getAbsolutePath());
			}
		}
		if (expandNumbers) {
			if (inplaceReplacements) {
				checkCharacters.expandNumbersInTranscriptFiles(language, retainNumbersForValidation);
			} else {
				NumberExpansion numberExpansion = NumberExpansion.getInstance(language);
				System.out.println("Expanding numbers in " + concatenatedTranscriptsFile.getAbsolutePath());
				numberExpansion.expandNumbersInFile(concatenatedTranscriptsFile.getAbsolutePath(),
						prunedTranscriptsFile.getAbsolutePath(), retainNumbersForValidation, false);
				System.out.println("Saved transcripts with expansions to " + prunedTranscriptsFile.getAbsolutePath());
			}
		}
	}

}
