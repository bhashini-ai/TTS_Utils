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

	void loadScriptTsv(String tsvFilePath) {
		sentences.clear();
		try (BufferedReader br = new BufferedReader(new FileReader(tsvFilePath))) {
			String line = br.readLine();
			HashMap<String, Integer> fieldIndices = ScriptFields.getFieldIndices(line.split("\t"));
			if (!ScriptFields.checkFieldIndices(fieldIndices)) {
				System.out.println(tsvFilePath + " is missing mandatory fields");
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

	void saveScriptCsv(String tsvFilePath) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(tsvFilePath))) {
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

	void updateScript(String recordingsDir) {
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

	public static void main(String[] args) {
		String tsvFilePath = args[0];
		String recordingsDir = args[1];

		UpdateScriptCsv updateScriptCsv = new UpdateScriptCsv();

		System.out.println("Loading " + tsvFilePath + " ...");
		updateScriptCsv.loadScriptTsv(tsvFilePath);
		System.out.println("Loading complete.\n");

		System.out.println("Checking recorded sentences and updating script ...");
		updateScriptCsv.updateScript(recordingsDir);
		System.out.println("Updating complete.\n");

		String csvFilePath = tsvFilePath.replace(".tsv", ".csv");
		updateScriptCsv.saveScriptCsv(csvFilePath);
		System.out.println("Created " + csvFilePath);
	}

}
