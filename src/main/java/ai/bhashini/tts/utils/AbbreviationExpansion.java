package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.cli.ParseException;

public class AbbreviationExpansion {
	public static final Language[] SUPPORTED_LANGUAGES = { Language.Kannada };

	protected Language language;
	protected Properties patternsAndReplacements = new Properties();
	protected HashMap<String, Pattern> patternsMap = new HashMap<>();

	// Singleton class => private constructor
	private AbbreviationExpansion(Language language) {
		this.language = language;
		loadPatternsAndReplacements(language + "_AbbreviationExpansion.properties");
		createPatternsMap();
	}

	private static HashMap<Language, AbbreviationExpansion> uniqueInstancesMap = new HashMap<>();

	public static AbbreviationExpansion getInstance(Language language) {
		if (!uniqueInstancesMap.containsKey(language)) {
			uniqueInstancesMap.put(language, new AbbreviationExpansion(language));
		}
		return uniqueInstancesMap.get(language);
	}

	protected void loadPatternsAndReplacements(String abbreviationExpansionPropertiesFile) {
		// https://stackoverflow.com/a/30755227 and https://stackoverflow.com/a/17852323
		try {
			InputStream is = getClass().getResourceAsStream("/" + abbreviationExpansionPropertiesFile);
			patternsAndReplacements.load(new InputStreamReader(is, "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void createPatternsMap() {
		for (Object key : patternsAndReplacements.keySet()) {
			Pattern pattern = Pattern.compile((String)key);
			patternsMap.put((String)key, pattern);
		}
	}

	protected String expandAbbreviations(String text) {
		String newText = text;
		for (String key: patternsMap.keySet()) {
			Pattern pattern = patternsMap.get(key);
			String replacement = patternsAndReplacements.getProperty(key);
			newText = pattern.matcher(newText).replaceAll(replacement);
		}
		return newText;
	}

	public void expandAbbreviationsInFile(String inputFilePath, String outputFilePath) {
		try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				String newContent = expandAbbreviations(line);
				bw.write(newContent + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class Arguments extends CommandLineOptions {
		StringOption inputFilePath = new StringOption("in", "input", "Path of input text file.");
		StringOption language = new StringOption("lang", "language", "Language of the input text file");
		StringOption outputFilePath = new StringOption("out", "output",
				"Path of output text file. If this is not specified, the output will be saved in the same directpory as that of the input with '_expanded' added to the filename.");

		public Arguments() {
			super();
			language.setRequired(true);
			options.addOption(inputFilePath);
			options.addOption(language);
			options.addOption(outputFilePath);
		}
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(AbbreviationExpansion.class.getCanonicalName());
			return;
		}
		String inputFilePath = arguments.inputFilePath.getStringValue();
		String languageStr = arguments.language.getStringValue();
		String outputFilePath = arguments.outputFilePath.getStringValue();

		Language language;
		try {
			language = Language.valueOf(languageStr);
		} catch (IllegalArgumentException e) {
			System.out.println("Unrecognized language name. Supported languages are:");
			for (Language l : SUPPORTED_LANGUAGES) {
				System.out.println("\t" + l);
			}
			arguments.printHelp(NumberExpansion.class.getCanonicalName());
			return;
		}

		if (outputFilePath == null) {
			outputFilePath = FileUtils.addSuffixToFilePath(inputFilePath, "_expanded");
		}

		AbbreviationExpansion abbreviationExpansion = AbbreviationExpansion.getInstance(language);
		System.out.println("Expanding abbreviations in " + inputFilePath);
		abbreviationExpansion.expandAbbreviationsInFile(inputFilePath, outputFilePath);
		System.out.println("Output saved to " + outputFilePath);
	}

}
