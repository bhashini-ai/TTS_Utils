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

public class UpdateScriptCsv {
	static class ScriptFields {
		String id;
		String sentence;
		String language;
		String speaker;
		String style;
		String category;

		private static String[] fieldsOfInterest = new String[] { "ID", "Sentence", "language", "speaker", "style", "category" };

		public static String getHeader() {
			return "ID,Sentence,language,speaker,style,category";
		}

		public ScriptFields(String[] contents, HashMap<String, Integer> fieldIndices) {
			this.id = contents[fieldIndices.get("ID")];
			this.sentence = contents[fieldIndices.get("Sentence")];
			this.language = contents[fieldIndices.get("language")];
			this.speaker = contents[fieldIndices.get("speaker")];
			this.style = contents[fieldIndices.get("style")];
			this.category = contents[fieldIndices.get("category")];
		}

		@Override
		public String toString() {
			String csvCompatibleSentence = "\"" + sentence.replace("\"", "\"\"") + "\"";
			return id + "," + csvCompatibleSentence + "," + language + "," + speaker + "," + style + "," + category;
		}

		public static HashMap<String, Integer> getFieldIndices(String[] headerContents) {
			HashMap<String, Integer> indices = new HashMap<>();
			for (int i = 0; i < headerContents.length; i++) {
				for (String fieldOfInterest : fieldsOfInterest) {
					if (headerContents[i].equalsIgnoreCase(fieldOfInterest)) {
						indices.put(fieldOfInterest, i);
					}
				}
			}
			return indices;
		}

		public static boolean checkFieldIndices(HashMap<String, Integer> indices) {
			boolean missingFields = false;
			for (String fieldOfInterest : fieldsOfInterest) {
				if (!indices.containsKey(fieldOfInterest)) {
					System.out.println("Missing field: " + fieldOfInterest);
					missingFields = true;
				}
			}
			return !missingFields;
		}
	}

	private HashMap<String, ScriptFields> sentences = new HashMap<>();
	private HashMap<String, ScriptFields> relevantSentences = new HashMap<>();

	void loadScriptTsv(File tsvFile) {
		sentences.clear();
		try (BufferedReader br = new BufferedReader(new FileReader(tsvFile))) {
			String line = br.readLine();
			HashMap<String, Integer> fieldIndices = ScriptFields.getFieldIndices(line.split("\t"));
			if (!ScriptFields.checkFieldIndices(fieldIndices)) {
				System.out.println(tsvFile.getAbsolutePath() + " is missing mandatory fields");
				return;
			}
			while ((line = br.readLine()) != null) {
				try {
					ScriptFields scriptFields = new ScriptFields(line.split("\t"), fieldIndices);
					sentences.put(scriptFields.id, scriptFields);
				} catch (Exception e) {
					System.out.println("Error in line: " + line);
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void saveScriptCsv(File csvFile) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {
			bw.write(ScriptFields.getHeader() + "\n");
			ArrayList<String> ids = new ArrayList<>(relevantSentences.keySet());
			Collections.sort(ids);
			for (String id : ids) {
				ScriptFields scriptFields = relevantSentences.get(id);
				bw.write(scriptFields.toString() + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void updateScript(File recordingsDir) {
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
			ScriptFields scriptFields = sentences.get(id);
			if (scriptFields != null) {
				String scriptSentence = scriptFields.sentence;
				String recordedSentence = FileUtils.getFileContents(txtFile.getAbsolutePath()).split("\n")[0];
				if (!scriptSentence.equals(recordedSentence)) {
					scriptFields.sentence = recordedSentence;
				}
				relevantSentences.put(id, scriptFields);
			}
		}
	}

	public static class Arguments extends CommandLineOptions {
		StringOption dataDir = new StringOption("dir", "data-dir",
				"Directory containing recordings (<child-dir>/wav/*.wav) and their transcripts (<child-dir>/txt/*.txt)");
		StringOption tsvFilePath = new StringOption("tsv", "tsv-filepath",
				"Relative path of TSV file containing transcripts of interest.");

		public Arguments() {
			super();
			dataDir.setRequired(true);
			tsvFilePath.setRequired(true);
			options.addOption(dataDir);
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
			arguments.printHelp(UpdateScriptCsv.class.getCanonicalName());
			return;
		}
		String dataDirPath = arguments.dataDir.getStringValue();
		String tsvFilePath = arguments.tsvFilePath.getStringValue();

		File dataDir = new File(dataDirPath);
		File tsvFile = new File(dataDir, tsvFilePath);
		File csvFile = new File(dataDir, tsvFilePath.replace(".tsv", ".csv"));

		UpdateScriptCsv updateScriptCsv = new UpdateScriptCsv();

		System.out.println("Loading " + tsvFile.getAbsolutePath() + " ...");
		updateScriptCsv.loadScriptTsv(tsvFile);
		System.out.println("Loading complete.\n");

		System.out.println("Checking recorded sentences and updating script ...");
		updateScriptCsv.updateScript(dataDir);
		System.out.println("Updating complete.\n");

		updateScriptCsv.saveScriptCsv(csvFile);
		System.out.println("Created " + csvFile.getAbsolutePath());
	}

}
