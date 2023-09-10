package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.ParseException;

public class RemoveRepeatedSentences {
	public static class Arguments extends CommandLineOptions {
		StringOption file1 = new StringOption("f1", "file-1", "Path of 1st text file");
		BooleanOption prune1 = new BooleanOption("p1", "prune-file-1", "Prune lines in file-1");
		StringOption file2 = new StringOption("f2", "file-2", "Path of 2nd text file");
		BooleanOption prune2 = new BooleanOption("p2", "prune-file-2", "Prune lines in file2");
		BooleanOption merge = new BooleanOption("m", "merge-files", "Merge unique lines in file1 and file2");

		public Arguments() {
			super();
			file1.setRequired(true);
			options.addOption(file1);
			options.addOption(prune1);
			options.addOption(file2);
			options.addOption(prune2);
			options.addOption(merge);
		}
	}

	public static void main1(String[] args) {
		String baseDir = "D:/TTS_Recording_IISc/Chattisgarhi_Script/";
		String[] categories = new String[] { "agri", "books", "finance", "food", "healthcare", "indic", "politics",
				"social", "sports", "technology" };
		for (String category : categories) {
			System.out.println("\nProcessing " + baseDir + category + ".txt ...");
			main(new String[] { "-f1", baseDir + category + "_richSentences_validated.txt", "-p1" });
		}
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(RemoveRepeatedSentences.class.getCanonicalName());
			return;
		}
		String file1 = arguments.file1.getStringValue();
		boolean prune1 = arguments.prune1.getBoolValue();
		String file2 = arguments.file2.getStringValue();
		boolean prune2 = arguments.prune2.getBoolValue();
		boolean merge = arguments.merge.getBoolValue();

		double werThreshold = 0.25;
		List<String> lines1 = getLines(file1);
		if (prune1) {
			lines1 = removeCloseMatches(lines1, werThreshold);
			saveLines(file1 + ".unique", lines1);
		}
		if (file2 != null) {
			List<String> lines2 = getLines(file2);
			if (prune2) {
				lines2 = removeCloseMatches(lines2, werThreshold);
				saveLines(file2 + ".unique", lines2);
			}
			if (merge) {
				List<String> uniqueLines = mergeUniqueLines(lines1, lines2, werThreshold);
				File f1 = new File(file1);
				File f2 = new File(file2);
				File f3 = new File(f1.getParentFile(), f1.getName() + "_" + f2.getName() + ".unique");
				saveLines(f3.getAbsolutePath(), uniqueLines);
			}
		}
	}

	public static List<String> getLines(String inputFilePath) {
		ArrayList<String> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.split("\t").length == 2) {
					lines.add(line);
				} else {
					System.out.println("Skipping line: " + line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	public static void saveLines(String outputFilePath, List<String> lines) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath))) {
			for (String line : lines) {
				bw.write(line + System.lineSeparator());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static List<String> removeCloseMatches(List<String> lines, double werThreshold) {
		if (lines.size() <= 1) {
			return lines;
		}
		int mid = lines.size() / 2;
		List<String> firstHalf = lines.subList(0, mid);
		List<String> secondHalf = lines.subList(mid, lines.size());
		List<String> uniqueInFirstHalf = removeCloseMatches(firstHalf, werThreshold);
		List<String> uniqueInSecondHalf = removeCloseMatches(secondHalf, werThreshold);
		return mergeUniqueLines(uniqueInFirstHalf, uniqueInSecondHalf, werThreshold);
	}

	public static List<String> mergeUniqueLines(List<String> uniqueLines1, List<String> uniqueLines2,
			double werThreshold) {
		ArrayList<String> uniqueLines = new ArrayList<>(uniqueLines1);
		for (String line2 : uniqueLines2) {
			List<String> wordsInLine2 = getWords(line2.split("\t")[1]);
			double wer = 1.0;
			for (String line1 : uniqueLines1) {
				List<String> wordsInLine1 = getWords(line1.split("\t")[1]);
				wer = getWER(wordsInLine1, wordsInLine2);
				if (wer <= werThreshold) {
					System.out.println(
							"Close match (wer = " + wer + ") between following sentences\n\t" + line1 + "\n\t" + line2);
					break;
				}
			}
			if (wer > werThreshold) {
				uniqueLines.add(line2);
			}
		}
		return uniqueLines;
	}

	public static List<String> getWords(String line) {
		return Arrays.asList(line.split("\\s+"));
	}

	public static double getWER(List<String> wordsInLine1, List<String> wordsInLine2) {
		LevenshteinDistance levenshteinDistance = new LevenshteinDistance(wordsInLine1, wordsInLine2);
		return levenshteinDistance.getErrorRate();
	}
}
