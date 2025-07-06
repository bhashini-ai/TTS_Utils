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
	protected HashMap<String, String> arpabetDevanagariMappings = new HashMap<>();

	private static Transliterate uniqueInstance = null;

	public static synchronized Transliterate getInstance() {
		if (uniqueInstance == null) {
			uniqueInstance = new Transliterate();
		}
		return uniqueInstance;
	}

	protected static String TRANSLITERATION_MAPPINGS_FILE = "TransliterationMappings.tsv";
	protected static String ARPABET_DEVANAGARI_MAPPINGS_FILE = "ArpabetDevanagariMappings.tsv";

	private Transliterate() {
		loadTransliterationMappings(TRANSLITERATION_MAPPINGS_FILE);
		loadArpabetDevanagariMappings(ARPABET_DEVANAGARI_MAPPINGS_FILE);
	}

	protected void loadTransliterationMappings(String transliterationMappingsFile) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				getClass().getResourceAsStream("/" + transliterationMappingsFile), StandardCharsets.UTF_8))) {
			// Read header
			String line = br.readLine();
			ArrayList<Script> scripts = new ArrayList<>();
			for (String script : line.split("\t")) {
				scripts.add(Script.valueOf(script));
			}
			// Read mappings
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\t");
				if (contents.length != scripts.size()) {
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
						Script outputScript = scripts.get(j);
						String outputText = contents[j];
						putTransliterationMapping(inputText, outputScript, outputText);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void loadArpabetDevanagariMappings(String arpabetDevanagariMappingsFile) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				getClass().getResourceAsStream("/" + arpabetDevanagariMappingsFile), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					continue; // Skip comments
				}
				String[] parts = line.split("\t");
				if (parts.length >= 2) {
					arpabetDevanagariMappings.put(parts[0], parts[1]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected String getKey(String inputText, Script outputScript) {
		return inputText + ":" + outputScript.name();
	}

	protected String getTransliterationMapping(String inputText, Script outputScript) {
		return transliterationMappings.get(getKey(inputText, outputScript));
	}

	protected String putTransliterationMapping(String inputText, Script outputScript, String outputText) {
		return transliterationMappings.put(getKey(inputText, outputScript), outputText);
	}

	public String transliterate(String inputText, Script inputScript, Script outputScript) {
		int srcUnicodeStart = inputScript.unicodeBlockStart;
		int dstUnicodeStart = outputScript.unicodeBlockStart;
		String outputText = "";
		for (int i = 0; i < inputText.length(); i++) {
			int inputC = inputText.codePointAt(i);
			String inputS = Character.toString(inputC);
			String outputS;
			if (inputScript.isValidCodepoint(inputC)) {
				outputS = getTransliterationMapping(inputS, outputScript);
				if (outputS == null) {
					int outputC = inputC - srcUnicodeStart + dstUnicodeStart;
					if (Character.isDefined(outputC)) {
						outputS = Character.toString(outputC);
					} else {
						outputS = inputS;
					}
				}
			} else {
				outputS = inputS;
			}
			outputText += "" + outputS;
		}
		return outputText;
	}

	public void transliterate(String inputFilePath, String outputFilePath, Script srcScript, Script dstScript) {
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

	public String getIndicMapping(String[] arpabets) {
		StringBuilder strBuilder = new StringBuilder();
		boolean wordStart = true;
		boolean isPrevSymbolConsonant = false;
		for (String arpabet : arpabets) {
			if (arpabet.isBlank()) {
				strBuilder.append(arpabet);
				wordStart = true;
				isPrevSymbolConsonant = false;
			} else if (arpabet.matches("[\\p{Punct}]+")) {
				strBuilder.append(arpabet);
				isPrevSymbolConsonant = false;
			} else {
				if (Character.isDigit(arpabet.codePointAt(arpabet.length() - 1))) {
					arpabet = arpabet.substring(0, arpabet.length() - 1);
				}
				String indicMapping = arpabetDevanagariMappings.get(arpabet);
				if (indicMapping == null || indicMapping.isBlank()) {
					continue;
				}
				int unicodeOffset = Script.Devanagari.getUnicodeOffset(indicMapping.codePointAt(indicMapping.length() - 1));
				if (unicodeOffset == UnicodeOffsets.SIGN_NUKTA.offset) {
					unicodeOffset = Script.Devanagari.getUnicodeOffset(indicMapping.codePointAt(0));
				}
				if (UnicodeOffsets.isVowel(unicodeOffset)) {
					if (!wordStart && isPrevSymbolConsonant) {
						indicMapping = Script.Devanagari.getUnicode(UnicodeOffsets.convertToVowelSign(unicodeOffset));
					}
					isPrevSymbolConsonant = false;
				} else {
					if (isPrevSymbolConsonant) {
						indicMapping = Script.Devanagari.getUnicode(UnicodeOffsets.HALANT.offset) + indicMapping;
					}
					isPrevSymbolConsonant = true;
				}
				strBuilder.append(indicMapping);
				wordStart = false;
			}
		}
		if (isPrevSymbolConsonant) {
			strBuilder.append(Script.Devanagari.getUnicode(UnicodeOffsets.HALANT.offset));
		}
		return strBuilder.toString();
	}

	public static void main(String[] args) {
		String inputText = "கடல்களுக்கான உரோமானியக்கடவுள் நெப்டியூன் என்பவரின் பெயரே ஞாயிற்றுத் தொகுதியில் உள்ள கோளான நெப்டியூன் கோளிற்கு சூட்டப்பட்டது.";
		String outputText = Transliterate.getInstance().transliterate(inputText, Script.Tamil, Script.Kannada);
		System.out.println(inputText + "\n->\n" + outputText);

		inputText = "अभिषिषेणयिषुं भुवनानि यः स्मरमिवाख्यत लोध्ररजश्चयः।\n" + "क्षुभितसैन्यपरागविपाण्डुर- द्युतिरयं तिरयन्नुदभूद्दिशः॥";
		outputText = Transliterate.getInstance().transliterate(inputText, Script.Devanagari, Script.Kannada);
		System.out.println("\n" + inputText + "\n->\n" + outputText);

		// Transliterate.getInstance().transliterate(args[0], args[1], ScriptId.Devanagari, ScriptId.Kannada);
	}
}
