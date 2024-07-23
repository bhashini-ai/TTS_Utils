package ai.bhashini.tts.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.commons.cli.ParseException;

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

	void loadJsonFiles(File testFile, File trainFile) {
		// https://stackoverflow.com/questions/29965764/how-to-parse-json-file-with-gson
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

	void createScriptFiles(File outputDir) {
		outputDir.mkdirs();
		for (String category : categorySpecificSentences.keySet()) {
			ArrayList<SentenceInfo> sentences = categorySpecificSentences.get(category);
			sentences.sort(new Comparator<SentenceInfo>() {
				@Override
				public int compare(SentenceInfo s1, SentenceInfo s2) {
					return s1.filename.compareTo(s2.filename);
				}
			});
			File outputFile = new File(outputDir, category + ".txt");
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

	public static class Arguments extends CommandLineOptions {
		String defaultScriptDirName = "script";
		String defaultTestJsonFilename = "metadata_test.json";
		String defaultTrainJsonFilename = "metadata_train.json";

		StringOption inputDir = new StringOption("in", "input-dir",
				"Input directory containing the transcript JSON files from AI4Bharat");
		StringOption outputDirName = new StringOption("out", "output-dir-name",
				"Name of output directory where the category-wise script files will be saved (default = '"
						+ defaultScriptDirName + "')",
				defaultScriptDirName);
		StringOption testJsonFilename = new StringOption("test", "test-json-filename",
				"Name of test JSON file " + "(default = '" + defaultTestJsonFilename + "')", defaultTestJsonFilename);
		StringOption trainJsonFilename = new StringOption("train", "train-json-filename",
				"Name of train JSON file " + "(default = '" + defaultTrainJsonFilename + "')", defaultTrainJsonFilename);

		public Arguments() {
			super();
			inputDir.setRequired(true);
			options.addOption(inputDir);
			options.addOption(outputDirName);
			options.addOption(testJsonFilename);
			options.addOption(trainJsonFilename);
		}
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(ParseAI4BharatJson.class.getCanonicalName());
			return;
		}

		String inputDirPath = arguments.inputDir.getStringValue();
		String outputDirName = arguments.outputDirName.getStringValue();
		String testJsonFilename = arguments.testJsonFilename.getStringValue();
		String trainJsonFilename = arguments.trainJsonFilename.getStringValue();
		File inputDir = new File(inputDirPath);
		File outputDir = new File(inputDir, outputDirName);
		File testFile = new File(inputDir, testJsonFilename);
		File trainFile = new File(inputDir, trainJsonFilename);

		ParseAI4BharatJson parseAI4BharatJson = new ParseAI4BharatJson();
		parseAI4BharatJson.loadJsonFiles(testFile, trainFile);
		parseAI4BharatJson.categorizeSentences();
		parseAI4BharatJson.createScriptFiles(outputDir);
	}

}
