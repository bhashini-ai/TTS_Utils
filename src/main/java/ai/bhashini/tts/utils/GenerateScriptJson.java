package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GenerateScriptJson {
	public static class Sentence {
		public String id;
		public String text;

		public Sentence(String sentenceId, String text) {
			super();
			this.id = sentenceId;
			this.text = text;
		}
	}

	public static class Script {
		public String projectId;
		public String language;
		public List<Sentence> sentences;

		public Script(String projectId, String language, List<Sentence> prompts) {
			this.projectId = projectId;
			this.language = language;
			this.sentences = prompts;
		}

		String getSentenceId(int index) {
			return sentences.get(index).id;
		}

		String getSentenceText(int index) {
			return sentences.get(index).text;
		}
	}

	public static void generateScriptJson(String projectId, String language, File scriptFile) {
		File jsonFile = new File(scriptFile.getParent(), language + ".json");
		System.out.println("Creating " + jsonFile.getAbsolutePath() + " ...");
		ArrayList<Sentence> sentences = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(scriptFile));
				BufferedWriter bw = new BufferedWriter(new FileWriter(jsonFile))) {
			String line;
			br.readLine(); // ignore first line
			while ((line = br.readLine()) != null) {
				// Sentence sentence = createSentence(line, projectId, language);
				Sentence sentence = createSentence(line);
				if (sentence != null) {
					sentences.add(sentence);
				}
			}
			Script script = new Script(projectId, language, sentences);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String scriptJson = gson.toJson(script);
			bw.write(scriptJson);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static Sentence createSentence(String line, String projectId, String language) {
		String[] contents = line.split("\t");
		if (contents.length != 5) {
			System.out.println("Skipping: " + line);
			return null;
		}
		int lineNum = Integer.parseInt(contents[0].substring(2));
		String style = contents[1];
		String prefix = "";
		if (style.contains("Expressive")) {
			prefix = contents[2] + " ";
		} else if (style.contains("Conversation")) {
			prefix = "Conversation ";
		}
		String sentenceText = prefix + contents[4];
		String sentenceId = projectId + "-" + language + "-" + String.format("%04d", lineNum);
		if (!line.isEmpty()) {
			return new Sentence(sentenceId, sentenceText);
		}
		return null;
	}

	static Sentence createSentence(String line) {
		String[] contents = line.split("\t");
		if (contents.length != 3) {
			System.out.println("Skipping: " + line);
			return null;
		}
		String sentenceId = contents[0];
		String sentenceText = contents[1] + "\n" + contents[2];
		if (!line.isEmpty()) {
			return new Sentence(sentenceId, sentenceText);
		}
		return null;
	}

	public static void main(String[] args) {
		String projectId = "AI4Bharat";
		String language = "Bodo";
		File scriptFile = new File("C:/TTS_Speech_Recording_AI4Bharat/Bodo_Conv.tsv");
		generateScriptJson(projectId, language, scriptFile);
	}

}
