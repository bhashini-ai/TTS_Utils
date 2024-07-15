package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.commons.cli.ParseException;

public class CheckCharacters {
	ArrayList<String> txtFilePaths = new ArrayList<>();
	HashMap<String, String> replacements = new HashMap<>();

	public CheckCharacters() {
	}

	void loadTextFilePaths(File dataDir, boolean verbose) {
		File[] subDirs = MatchWavAndTextFiles.getSubDirs(dataDir);
		for (File subDir : subDirs) {
			File wavDir = new File(subDir, "wav");
			File txtDir = new File(subDir, "txt");
			if (wavDir.exists() && txtDir.exists()) {
				if (verbose) {
					System.out.println("\t" + subDir.getAbsolutePath());
				}
				File[] wavFiles = wavDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".wav");
					}
				});
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

	void concatenateTxtFiles(File outputFile, boolean verbose) {
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
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

	HashMap<String, String> loadReplacements(File replacementsTSVFile) {
		try (BufferedReader br = new BufferedReader(new FileReader(replacementsTSVFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\t");
				if (contents[1].equalsIgnoreCase("delete")) {
					contents[1] = "";
				}
				replacements.put(contents[0], contents[1]);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return replacements;
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

	void replaceAll(boolean verbose) {
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

		StringOption dataDir = new StringOption("dir", "data-dir",
				"Directory containing recordings (<child-dir>/wav/*.wav) and their transcripts (<child-dir>/txt/*.txt)");
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
		BooleanOption concatenateAgain = new BooleanOption("recreate", "concatenate-again",
				"Force recreation of concatenated-text and unique-characters files");
		BooleanOption inplaceReplacements = new BooleanOption("inplace", "inplace-replacements",
				"Do character replacements in the original transcript files");
		BooleanOption verbose = new BooleanOption("v", "verbose", "Print each file being processed");

		public Arguments() {
			super();
			dataDir.setRequired(true);
			options.addOption(dataDir);
			options.addOption(concatenatedTranscriptsFilePath);
			options.addOption(uniqueCharactersFilePath);
			options.addOption(replacementsTSVFilePath);
			options.addOption(concatenateAgain);
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
		String concatenatedTranscriptsFilePath = arguments.concatenatedTranscriptsFilePath.getStringValue();
		String uniqueCharactersFilePath = arguments.uniqueCharactersFilePath.getStringValue();
		String replacementsTSVFilePath = arguments.replacementsTSVFilePath.getStringValue();
		String prunedTranscriptsFilePath = arguments.prunedTranscriptsFilePath.getStringValue();
		boolean concatenateAgain = arguments.concatenateAgain.getBoolValue();
		boolean inplaceReplacements = arguments.inplaceReplacements.getBoolValue();
		boolean verbose = arguments.verbose.getBoolValue();

		File dataDir = new File(dataDirPath);
		File concatenatedTxtFile = new File(dataDir, concatenatedTranscriptsFilePath);
		File uniqueCharactersFile = new File(dataDir, uniqueCharactersFilePath);
		File replacementsTSVFile = new File(dataDir, replacementsTSVFilePath);
		File prunedTranscriptsFile = new File(dataDir, prunedTranscriptsFilePath);

		CheckCharacters checkCharacters = new CheckCharacters();
		if (!concatenatedTxtFile.exists() || concatenateAgain) {
			System.out.println("Checking .txt files in " + dataDir.getAbsolutePath());
			checkCharacters.loadTextFilePaths(dataDir, verbose);
			System.out.println("Concatenating all the .txt files to " + concatenatedTxtFile.getAbsolutePath());
			checkCharacters.concatenateTxtFiles(concatenatedTxtFile, verbose);
			System.out.println("Successfully completed concatenation.");
		}
		if (!uniqueCharactersFile.exists() || concatenateAgain) {
			System.out.println("Finding unique characters and their unigram count from " + concatenatedTxtFile.getAbsolutePath());
			checkCharacters.createUniqueCharactersFile(concatenatedTxtFile, uniqueCharactersFile);
			System.out.println("Saved unique characters and their count to " + uniqueCharactersFile.getAbsolutePath());
		}
		if (!replacementsTSVFile.exists()) {
			System.out.println("Error: Could not find the TSV file containing replacements "
					+ replacementsTSVFile.getAbsolutePath());
			return;
		}
		checkCharacters.loadReplacements(replacementsTSVFile);
		if (inplaceReplacements) {
			checkCharacters.replaceAll(verbose);
		} else {
			System.out.println("Replacing specified characters in " + concatenatedTxtFile.getAbsolutePath());
			checkCharacters.replace(concatenatedTxtFile, prunedTranscriptsFile);
			System.out.println("Saved pruned transcripts (after replacements) to " + prunedTranscriptsFile.getAbsolutePath());
		}
	}

}
