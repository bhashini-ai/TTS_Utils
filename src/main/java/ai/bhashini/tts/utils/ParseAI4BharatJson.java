package ai.bhashini.tts.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class ParseAI4BharatJson {
	MetaDataTest metaDataTest;
	MetaDataTrain metaDataTrain;
	HashMap<String, ArrayList<SentenceInfo>> categorySpecificSentences = new HashMap<>();

	public class SentenceInfo {
		String filename;
		String text;
		String language;
		String gender;
		String style;
		String duration;
	}

	public class MetaDataTest {
		SentenceInfo[] test;
	}

	public class MetaDataTrain {
		SentenceInfo[] train;
	}

	void loadJsonFiles(File inputFolder) {
		// https://stackoverflow.com/questions/29965764/how-to-parse-json-file-with-gson
		File testFile = new File(inputFolder, "metadata_test.json");
		File trainFile = new File(inputFolder, "metadata_train.json");
		Gson gson = new Gson();
		System.out.println("Loading " + testFile.getAbsolutePath() + " ...");
		System.out.println("Loading " + trainFile.getAbsolutePath() + " ...");
		try (JsonReader metaDataTestReader = new JsonReader(new FileReader(testFile.getAbsolutePath()));
				JsonReader metaDataTrainReader = new JsonReader(new FileReader(trainFile.getAbsolutePath()))) {
			metaDataTest = gson.fromJson(metaDataTestReader, MetaDataTest.class);
			metaDataTrain = gson.fromJson(metaDataTrainReader, MetaDataTrain.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void categorize(SentenceInfo sentenceInfo) {
		String category = sentenceInfo.style;
		if (!categorySpecificSentences.containsKey(category)) {
			categorySpecificSentences.put(category, new ArrayList<>());
		}
		categorySpecificSentences.get(category).add(sentenceInfo);
	}

	void categorizeSentences() {
		for (SentenceInfo sentenceInfo : metaDataTest.test) {
			categorize(sentenceInfo);
		}
		for (SentenceInfo sentenceInfo : metaDataTrain.train) {
			categorize(sentenceInfo);
		}
	}

	void createScriptFiles(File outputFolder) {
		outputFolder.mkdirs();
		for (String category : categorySpecificSentences.keySet()) {
			ArrayList<SentenceInfo> sentences = categorySpecificSentences.get(category);
			sentences.sort(new Comparator<SentenceInfo>() {
				@Override
				public int compare(SentenceInfo s1, SentenceInfo s2) {
					return s1.filename.compareTo(s2.filename);
				}
			});
			File outputFile = new File(outputFolder, category + ".txt");
			System.out.println("Creating " + outputFile.getAbsolutePath());
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
				for (SentenceInfo s : sentences) {
					bw.write(s.filename + "\t" + s.text + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		File inputFolder = new File(args[0]);
		ParseAI4BharatJson parseAI4BharatJson = new ParseAI4BharatJson();
		parseAI4BharatJson.loadJsonFiles(inputFolder);
		parseAI4BharatJson.categorizeSentences();
		parseAI4BharatJson.createScriptFiles(new File(inputFolder, "script"));
	}

}
