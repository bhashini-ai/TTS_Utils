package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class Transliterate {
	protected HashMap<String, String> transliterationMappings = new HashMap<>();

	private static Transliterate uniqueInstance = null;

	public static synchronized Transliterate getInstance() {
		if (uniqueInstance == null) {
			uniqueInstance = new Transliterate();
		}
		return uniqueInstance;
	}

	protected static String TRANSLITERATION_MAPPINGS_FILE = "TransliterationMappings.tsv";

	private Transliterate() {
		loadTransliterationMappings(TRANSLITERATION_MAPPINGS_FILE);
	}

	protected void loadTransliterationMappings(String transliterationMappingsFile) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				getClass().getResourceAsStream("/" + transliterationMappingsFile), StandardCharsets.UTF_8))) {
			// Read header
			String line = br.readLine();
			ArrayList<ScriptId> scriptIds = new ArrayList<>();
			for (String script : line.split("\t")) {
				scriptIds.add(ScriptId.valueOf(script));
			}
			// Read mappings
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\t");
				if (contents.length != scriptIds.size()) {
					System.err.println(
							"Error while loading " + transliterationMappingsFile + "\n" + "\tSkipping line: " + line);
					continue;
				}
				for (int i = 0; i < contents.length; i++) {
					String inputText = contents[i];
					for (int j = 0; j < contents.length; j++) {
						if (i == j) {
							continue;
						}
						ScriptId outputScriptId = scriptIds.get(j);
						String outputText = contents[j];
						putTransliterationMapping(inputText, outputScriptId, outputText);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected String getKey(String inputText, ScriptId outputScriptId) {
		return inputText + ":" + outputScriptId.name();
	}

	protected String getTransliterationMapping(String inputText, ScriptId outputScriptId) {
		return transliterationMappings.get(getKey(inputText, outputScriptId));
	}

	protected String putTransliterationMapping(String inputText, ScriptId outputScriptId, String outputText) {
		return transliterationMappings.put(getKey(inputText, outputScriptId), outputText);
	}

	public String transliterate(String inputText, ScriptId inputScriptId, ScriptId outputScriptId) {
		int srcUnicodeStart = inputScriptId.unicodeBlockStart;
		int dstUnicodeStart = outputScriptId.unicodeBlockStart;
		String outputText = "";
		for (char c : inputText.toCharArray()) {
			String mappedString = c + "";
			if (inputScriptId.isValidCodepoint(c)) {
				mappedString = getTransliterationMapping(c + "", outputScriptId);
				if (mappedString == null) {
					char targetC = (char) ((int) c - srcUnicodeStart + dstUnicodeStart);
					if (!Character.isDefined(targetC)) {
						targetC = c;
					}
					mappedString = targetC + "";
				}
			}
			outputText += "" + mappedString;
		}
		return outputText;
	}

	public void transliterate(String inputFilePath, String outputFilePath, ScriptId srcScript, ScriptId dstScript) {
		try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				String transliteratedString = transliterate(line, srcScript, dstScript);
				bw.write(transliteratedString + "\n");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String inputText = "கடல்களுக்கான உரோமானியக்கடவுள் நெப்டியூன் என்பவரின் பெயரே ஞாயிற்றுத் தொகுதியில் உள்ள கோளான நெப்டியூன் கோளிற்கு சூட்டப்பட்டது.";
		// String inputText = "अभिषिषेणयिषुं भुवनानि यः स्मरमिवाख्यत लोध्ररजश्चयः।\n" + "क्षुभितसैन्यपरागविपाण्डुर- द्युतिरयं तिरयन्नुदभूद्दिशः॥";
		String outputText = Transliterate.getInstance().transliterate(inputText, ScriptId.Taml, ScriptId.Knda);
		System.out.println(inputText + "\n->\n" + outputText);
		// Transliterate.getInstance().transliterate(args[0], args[1], ScriptId.Deva, ScriptId.Knda);
	}
}
