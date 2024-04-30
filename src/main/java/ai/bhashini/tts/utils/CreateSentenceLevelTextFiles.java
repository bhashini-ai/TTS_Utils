package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class CreateSentenceLevelTextFiles {

	public static void main(String[] args) {
		String recordingsDir = args[0];
		File scriptsDir = new File(recordingsDir, "script");
		String scriptSuffix = ".txt";
		ArrayList<String> categories = getScriptCategories(scriptsDir, scriptSuffix);
		HashMap<String, String> sentences = new HashMap<>();
		for (String category : categories) {
			sentences.putAll(loadScript(new File(scriptsDir, category + scriptSuffix)));
		}
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
				String sentenceId = FileUtils.getFileNameWithoutExtension(wavFile, "wav");
				String text = sentences.get(sentenceId);
				if (text == null) {
					System.out.println("Couldn't find text for " + wavFile.getAbsolutePath());
					continue;
				}
				String textFilePath = new File(txtDir, sentenceId + ".txt").getAbsolutePath();
				if (new File(textFilePath).exists()) {
					// System.out.println("Skipping already existing file " + textFilePath);
					continue;
				}
				System.out.println("Creating " + textFilePath);
				FileUtils.createFileWithContents(textFilePath, text);
			}
		}
	}

	public static ArrayList<String> getScriptCategories(File scriptsDir, String suffix) {
		File[] scriptFiles = scriptsDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(suffix) && !name.startsWith(".");
			}
		});
		if (scriptFiles == null) {
			scriptFiles = new File[0];
		}
		ArrayList<String> categoriesList = new ArrayList<>();
		for (File scriptFile : scriptFiles) {
			String fileName = scriptFile.getName();
			categoriesList.add(fileName.substring(0, fileName.lastIndexOf(suffix)));
		}
		if (categoriesList.size() > 1) {
			Collections.sort(categoriesList);
		}
		return categoriesList;
	}

	static HashMap<String, String> loadScript(File scriptFile) {
		System.out.println("Loading " + scriptFile.getAbsolutePath() + " ...");
		HashMap<String, String> sentences = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(scriptFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\t");
				if (contents.length < 2) {
					System.out.println("Skipping line: " + line);
					continue;
				}
				sentences.put(contents[0], contents[1]);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sentences;
	}
}
