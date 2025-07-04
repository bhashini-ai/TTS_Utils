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
		for (char c : inputText.toCharArray()) {
			String mappedString = c + "";
			if (inputScript.isValidCodepoint(c)) {
				mappedString = getTransliterationMapping(c + "", outputScript);
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

	public static int getUnicodeOffset(char c) {
		return c - Script.Devanagari.unicodeBlockStart;
	}

	public static String getUnicode(int offset) {
		return Character.toString(offset + Script.Devanagari.unicodeBlockStart);
	}

	public static String convertToVowelSign(int offset) {
		return getUnicode(UnicodeOffsets.convertToVowelSign(UnicodeOffsets.valueOf(offset)).offset);
	}

	public String getIndicMapping(String[] arpabets) {
		StringBuilder strBuilder = new StringBuilder();
		boolean isPrevSymbolConsonant = false;
		boolean wordStart = true;
		for (String arpabet : arpabets) {
			String indicMapping = arpabetDevanagariMappings.get(arpabet);
			if (indicMapping == null) {
				indicMapping = arpabetDevanagariMappings.get(arpabet.substring(0, arpabet.length() - 1));
			}
			int offset = getUnicodeOffset(indicMapping.charAt(indicMapping.length() - 1));
			if (offset == UnicodeOffsets.SIGN_NUKTA.offset) {
				offset = getUnicodeOffset(indicMapping.charAt(0));
			}
			if (UnicodeOffsets.isVowel(offset)) {
				if (!wordStart) {
					if (isPrevSymbolConsonant) {
						indicMapping = convertToVowelSign(offset);
					}
				}
				isPrevSymbolConsonant = false;
			} else {
				if (isPrevSymbolConsonant) {
					indicMapping = getUnicode(UnicodeOffsets.HALANT.offset) + indicMapping;
				}
				isPrevSymbolConsonant = true;
			}
			strBuilder.append(indicMapping);
			wordStart = false;
		}
		if (isPrevSymbolConsonant) {
			strBuilder.append(getUnicode(UnicodeOffsets.HALANT.offset));
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
