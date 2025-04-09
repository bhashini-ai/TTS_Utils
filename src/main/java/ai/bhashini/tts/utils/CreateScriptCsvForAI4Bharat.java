package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.cli.ParseException;

public class CreateScriptCsvForAI4Bharat {
	public enum FieldOfInterest {
		ID, Sentence, Language, Speaker, Style, Category;

		public int index = -1;

		public static void loadIndices(String[] headerContents) {
			for (int i = 0; i < headerContents.length; i++) {
				for (FieldOfInterest fieldOfInterest : values()) {
					if (headerContents[i].equalsIgnoreCase(fieldOfInterest.toString())) {
						fieldOfInterest.index = i;
						break;
					}
				}
			}
		}

		public static boolean checkValidIndices() {
			boolean missingFields = false;
			for (FieldOfInterest fieldOfInterest : values()) {
				if (fieldOfInterest.index == -1) {
					System.out.println("Missing field: " + fieldOfInterest);
					missingFields = true;
				}
			}
			return !missingFields;
		}
	}

	static class ScriptFields {
		String id;
		String sentence;
		String language;
		String speaker;
		String style;
		String category;

		public static String getHeader() {
			return FieldOfInterest.ID + "," + FieldOfInterest.Sentence + ","
					+ FieldOfInterest.Language.toString().toLowerCase() + ","
					+ FieldOfInterest.Speaker.toString().toString() + ","
					+ FieldOfInterest.Style.toString().toLowerCase() + ","
					+ FieldOfInterest.Category.toString().toLowerCase();
		}

		public ScriptFields(String[] contents) {
			this.id = contents[FieldOfInterest.ID.index];
			this.sentence = contents[FieldOfInterest.Sentence.index];
			this.language = contents[FieldOfInterest.Language.index];
			this.speaker = contents[FieldOfInterest.Speaker.index];
			this.style = contents[FieldOfInterest.Style.index];
			this.category = contents[FieldOfInterest.Category.index];
		}

		@Override
		public String toString() {
			String csvCompatibleSentence = "\"" + sentence.replace("\"", "\"\"") + "\"";
			return id + "," + csvCompatibleSentence + "," + language + "," + speaker + "," + style + "," + category;
		}

	}

	private HashMap<String, ScriptFields> sentencesInTSV = new HashMap<>();
	private HashMap<String, ScriptFields> sentencesInSpecifiedDir = new HashMap<>();

	boolean loadSentencesInTsv(File tsvFile) {
		sentencesInTSV.clear();
		try (BufferedReader br = new BufferedReader(new FileReader(tsvFile))) {
			String line = br.readLine();
			FieldOfInterest.loadIndices(line.split("\t"));
			if (!FieldOfInterest.checkValidIndices()) {
				System.out.println(tsvFile.getAbsolutePath() + " is missing mandatory fields");
				return false;
			}
			while ((line = br.readLine()) != null) {
				try {
					ScriptFields scriptFields = new ScriptFields(line.split("\t"));
					sentencesInTSV.put(scriptFields.id, scriptFields);
				} catch (Exception e) {
					System.out.println("Error in line: " + line);
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	void loadSentencesInSpecifiedDirAndUpdateScript(File recordingsDir) {
		sentencesInSpecifiedDir.clear();
		File txtDir = new File(recordingsDir, "txt");
		if (!txtDir.exists()) {
			return;
		}
		System.out.println("Checking " + txtDir.getAbsolutePath());
		File[] txtFiles = txtDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".txt");
			}
		});
		Arrays.sort(txtFiles);
		for (File txtFile : txtFiles) {
			String id = FileUtils.getFileNameWithoutExtension(txtFile, "txt");
			ScriptFields scriptFieldsInTSV = sentencesInTSV.get(id);
			if (scriptFieldsInTSV != null) {
				String sentenceInTSV = scriptFieldsInTSV.sentence;
				String recordedSentence = FileUtils.getFileContents(txtFile.getAbsolutePath()).split("\n")[0];
				if (!sentenceInTSV.equals(recordedSentence)) {
					scriptFieldsInTSV.sentence = recordedSentence;
				}
				sentencesInSpecifiedDir.put(id, scriptFieldsInTSV);
			}
		}
	}

	void saveUpdatedScript(File csvFile) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {
			bw.write(ScriptFields.getHeader() + "\n");
			ArrayList<String> ids = new ArrayList<>(sentencesInSpecifiedDir.keySet());
			Collections.sort(ids);
			for (String id : ids) {
				ScriptFields scriptFields = sentencesInSpecifiedDir.get(id);
				bw.write(scriptFields.toString() + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class Arguments extends CommandLineOptions {
		StringOption recordingsDir = new StringOption("dir", "recordings-dir",
				"Directory containing recordings (wav/*.wav) and their transcripts (txt/*.txt)");
		StringOption tsvFilePath = new StringOption("tsv", "tsv-filepath",
				"Path of TSV file containing transcripts of interest.");

		public Arguments() {
			super();
			recordingsDir.setRequired(true);
			tsvFilePath.setRequired(true);
			options.addOption(recordingsDir);
			options.addOption(tsvFilePath);
		}
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(CreateScriptCsvForAI4Bharat.class.getCanonicalName());
			return;
		}
		String recordingsDirPath = arguments.recordingsDir.getStringValue();
		String tsvFilePath = arguments.tsvFilePath.getStringValue();
		File recordingsDir = new File(recordingsDirPath);
		File tsvFile = new File(tsvFilePath);
		File csvFile = new File(tsvFilePath.replace(".tsv", ".csv"));

		CreateScriptCsvForAI4Bharat createScriptCsv = new CreateScriptCsvForAI4Bharat();
		System.out.println("\nLoading " + tsvFile.getAbsolutePath() + " ...");
		if (!createScriptCsv.loadSentencesInTsv(tsvFile)) {
			return;
		}
		System.out.println("Loading complete.\n");

		System.out.println("Checking recorded sentences and updating script ...");
		createScriptCsv.loadSentencesInSpecifiedDirAndUpdateScript(recordingsDir);
		System.out.println("Updating complete.\n");

		createScriptCsv.saveUpdatedScript(csvFile);
		System.out.println("Created " + csvFile.getAbsolutePath());
	}

}
