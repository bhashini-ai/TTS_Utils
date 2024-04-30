package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class CreateSentenceLevelTextFiles {

	public static void main(String[] args) {
		String scriptsDir = args[0];
		HashMap<String, HashMap<Integer, String>> categoryWiseSentences = new HashMap<String, HashMap<Integer, String>>();
		String[] categories = new String[] { "agri", "education", "finance", "general", "health", "other", "politics",
				"weather", "books", "iitm" };
		for (String category : categories) {
			HashMap<Integer, String> sentences = loadScript(
					new File(scriptsDir, category + "_richSentences_validated.txt"));
			categoryWiseSentences.put(category, sentences);
		}
		String recordingsDir = args[1];
		File[] subDirs = MatchWavAndTextFiles.getSubDirs(new File(recordingsDir));
		for (File subDir : subDirs) {
			File wavDir = new File(subDir, "wav");
			if (!wavDir.exists()) {
				continue;
			}
			File txtDir = new File(subDir, "txt");
			txtDir.mkdirs();
			
			File[] wavFiles = FileUtils.getWavFiles(wavDir);
			for (File wavFile : wavFiles) {
				String fileName = FileUtils.getFileNameWithoutExtension(wavFile, "wav");
				int index2 = fileName.lastIndexOf('_');
				try {
					int sentenceNumber = Integer.parseInt(fileName.substring(index2 + 1));
					String subFileName = fileName.substring(0, index2);
					String category = subFileName.substring(subFileName.lastIndexOf('_') + 1).toLowerCase();
					HashMap<Integer, String> sentences = categoryWiseSentences.get(category);
					if (sentences == null) {
						System.out.println("Couldn't find text for category: " + category);
						continue;
					}
					String text = sentences.get(sentenceNumber);
					if (text == null) {
						System.out.println("Couldn't find text for " + wavFile.getAbsolutePath());
						continue;
					}
					String textFilePath = new File(txtDir, wavFile.getName().replace(".wav", ".txt")).getAbsolutePath();
					if (new File(textFilePath).exists()) {
						// System.out.println("Skipping already existing file " + textFilePath);
						continue;
					}
					System.out.println("Creating " + textFilePath);
					FileUtils.createFileWithContents(textFilePath, text);
				} catch (NumberFormatException e) {
					System.out.println("Error parsing filename of " + wavFile.getAbsolutePath());
				}
			}
		}
	}

	static HashMap<Integer, String> loadScript(File scriptFile) {
		System.out.println("Loading " + scriptFile.getAbsolutePath() + " ...");
		HashMap<Integer, String> sentences = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(scriptFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\t");
				if (contents.length != 2) {
					System.out.println("Skipping line: " + line);
					continue;
				}
				int lineNumber = Integer.parseInt(contents[0]);
				sentences.put(lineNumber, contents[1]);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sentences;
	}
}
