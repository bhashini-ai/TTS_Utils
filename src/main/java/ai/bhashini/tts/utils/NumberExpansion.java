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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.ParseException;

public class NumberExpansion {
	public static final int ENGLISH_DIGIT_ZERO = 0x30;
	public static final int ENGLISH_DIGIT_NINE = 0x39;
	public static final int SYMBOL_COMMA = 0x2C;
	public static final int SYMBOL_DOT = 0x2E;
	public static final String SPACE = " ";
	public static final int DEVANAGARI_VISARGA = 'ः';
	public static final Language[] SUPPORTED_LANGUAGES = { Language.Hindi, Language.Bengali, Language.Marathi,
			Language.Telugu, Language.Tamil, Language.Kannada, Language.Malayalam, Language.Sanskrit,
			Language.English };

	protected Language language;
	protected Properties numberExpansionProperties = new Properties();

	private static final String NUMBERS_REGEX = "[-\\d]?[\\d,.-]*\\d+";
	private static final String COMMA_REGEX1 = "\\d?\\d,\\d\\d\\d";
	private static final String COMMA_REGEX2 = "\\d?\\d,\\d\\d,\\d\\d\\d";
	private static final String COMMA_REGEX3 = "\\d?\\d,\\d\\d,\\d\\d,\\d\\d\\d";
	private static final String COMMA_REGEX4 = "\\d+\\d,\\d\\d,\\d\\d,\\d\\d\\d";
	protected Pattern numbersPattern;
	protected Pattern commaPattern1;
	protected Pattern commaPattern2;
	protected Pattern commaPattern3;
	protected Pattern commaPattern4;

	// Singleton class => private constructor
	private NumberExpansion(Language language) {
		this.language = language;
		this.numbersPattern = Pattern.compile(NUMBERS_REGEX);
		this.commaPattern1 = Pattern.compile(COMMA_REGEX1);
		this.commaPattern2 = Pattern.compile(COMMA_REGEX2);
		this.commaPattern3 = Pattern.compile(COMMA_REGEX3);
		this.commaPattern4 = Pattern.compile(COMMA_REGEX4);
		loadNumberExpansionProperties(language + "_NumberExpansion.properties");
	}

	private static HashMap<Language, NumberExpansion> uniqueInstancesMap = new HashMap<>();

	public static NumberExpansion getInstance(Language language) {
		if (!uniqueInstancesMap.containsKey(language)) {
			uniqueInstancesMap.put(language, new NumberExpansion(language));
		}
		return uniqueInstancesMap.get(language);
	}

	protected void loadNumberExpansionProperties(String numberExpansionPropertiesFile) {
		// https://stackoverflow.com/a/30755227 and https://stackoverflow.com/a/17852323
		try {
			InputStream is = getClass().getResourceAsStream("/" + numberExpansionPropertiesFile);
			numberExpansionProperties.load(new InputStreamReader(is, "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected String expandBasedOnPattern(long number, int divisor, String xString, String zerosString,
			boolean doSandhi) {
		long quotient = number / divisor;
		long remainder = number % divisor;
		String output = numberExpansionProperties.getProperty(quotient + xString);
		if (output != null) {
			return sandhi(output, expandNumber(remainder), doSandhi) + SPACE;
		}
		if (remainder == 0 && (output = numberExpansionProperties.getProperty("x" + zerosString)) != null) {
			return sandhi(expandNumber(quotient), output, true) + SPACE;
		}
		output = numberExpansionProperties.getProperty("x" + xString);
		if (output != null) {
			return sandhi(sandhi(expandNumber(quotient), output, true), expandNumber(remainder), doSandhi) + SPACE;
		}
		return "";
	}

	public String expandNumber(long number) {
		if (number == 0) {
			return "";
		}
		String output = numberExpansionProperties.getProperty(number + "");
		if (output != null) {
			return output + SPACE;
		}
		if (language == Language.Sanskrit) {
			return expandSanskritNumber(number);
		}
		if (number <= 99) {
			return expandBasedOnPattern(number, 10, "x", "0", true);
		}
		if (number <= 999) {
			return expandBasedOnPattern(number, 100, "xx", "00", false);
		}
		if (language == Language.Hindi || language == Language.Bengali || language == Language.Telugu) {
			if (number >= 1001 && number <= 1999) {
				return expandBasedOnPattern(number, 100, "xx", "00", false);
			}
		}
		if (language == Language.Marathi) {
			if (thousandsExpandableAsHundreds(number)) {
				return expandBasedOnPattern(number, 100, "xx", "00", false);
			}
		}
		if (number <= 99999) {
			return expandBasedOnPattern(number, 1000, "xxx", "000", false);
		}
		if (number <= 9999999) {
			return expandBasedOnPattern(number, 100000, "xxxxx", "00000", false);
		}
		return expandBasedOnPattern(number, 10000000, "xxxxxxx", "0000000", false);
	}

	public boolean thousandsExpandableAsHundreds(long number) {
		return (number >= 1100 && number <= 1999) || (number >= 2100 && number <= 2999)
				|| (number >= 3100 && number <= 3999) || (number >= 4100 && number <= 4999)
				|| (number >= 5100 && number <= 5999) || (number >= 6100 && number <= 6999)
				|| (number >= 7100 && number <= 7999) || (number >= 8100 && number <= 8999)
				|| (number >= 9100 && number <= 9999);
	}

	public String expandSanskritNumber(long number) {
		String s = expandSanskritTens(number);
		if (number >= 100) {
			int t1 = thousandsExpandableAsHundreds(number) ? 10000 : 1000;
			s += expandSanskritHundreds(number, t1);
		}
		if (number >= 1000 && !thousandsExpandableAsHundreds(number)) {
			s += expandSanskritThousands(number);
		}
		if (number >= 100000) {
			s += expandSanskritLakhs(number);
		}
		if (number >= 10000000) {
			s += expandSanskritCrores(number);
		}
		if (number >= 1000000000) {
			s += expandSanskritArbhudham(number);
		}
		return s;
	}

	public String expandSanskritTens(long number) {
		long n = number % 100;
		if (n > 0) {
			String pattern = number >= 100 ? "x" + n : "" + n;
			return numberExpansionProperties.getProperty(pattern) + SPACE;
		}
		return "";
	}

	public String expandSanskritHundreds(long number, long t1) {
		return expandBasedOnPattern2(number, t1, 100, "xx", "00");
	}

	public String expandSanskritThousands(long number) {
		return expandBasedOnPattern2(number, 100000, 1000, "xxx", "000");
	}

	public String expandSanskritLakhs(long number) {
		return expandBasedOnPattern2(number, 10000000, 100000, "xxxxx", "00000");
	}

	public String expandSanskritCrores(long number) {
		return expandBasedOnPattern2(number, 1000000000, 10000000, "xxxxxxx", "0000000");
	}

	public String expandSanskritArbhudham(long number) {
		long n = number / 1000000000;
		String pattern = "000000000";
		String s = numberExpansionProperties.getProperty(n + pattern);
		if (s != null) {
			return s + SPACE;
		}
		String s1 = expandSanskritNumber(n);
		String s2 = numberExpansionProperties.getProperty("x" + pattern);
		return s1 + SPACE + s2 + SPACE;
	}

	public String expandBasedOnPattern2(long number, long t1, long t2, String p1, String p2) {
		long n = (number % t1) / t2;
		if (n > 0) {
			String pattern = number >= t1 ? p1 : p2;
			String s = numberExpansionProperties.getProperty(n + pattern);
			if (s != null) {
				return s + SPACE;
			}
			String s1 = numberExpansionProperties.getProperty(n + "").trim();
			if (s1.charAt(s1.length() - 1) == DEVANAGARI_VISARGA) {
				s1 = s1.substring(0, s1.length() - 1);
			}
			String s2 = numberExpansionProperties.getProperty("x" + pattern);
			return s1 + s2 + SPACE;
		}
		return "";
	}

	protected String replaceLanguageDigitsWithEnglishDigits(String text) {
		char[] chars = text.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			int c = chars[i];
			if (c >= getLanguageDigitZero() && c <= getLanguageDigitNine()) {
				chars[i] = getEnglishEquivalentOfLanguageDigit(c);
			}
		}
		return new String(chars);
	}

	public String expandNumbers(String input, boolean retainNumbersForValidation) {
		String replacedInput = replaceLanguageDigitsWithEnglishDigits(input);
		Matcher matcher = numbersPattern.matcher(replacedInput);
		int prevMatchEnd = 0;
		StringBuffer output = new StringBuffer();
		while (matcher.find()) {
			if (matcher.start() > prevMatchEnd) {
				String nonMatchingSubStr = input.substring(prevMatchEnd, matcher.start());
				output.append(nonMatchingSubStr);
			}
			String numberStr = replacedInput.substring(matcher.start(), matcher.end());
			String expandedStr = expandByHandlingDashes(numberStr);
			if (matcher.end() < input.length()) {
				String restOfSentence = input.substring(matcher.end());
				expandedStr = handleSandhiWithNextWord(expandedStr, restOfSentence);
			}
			if (retainNumbersForValidation) {
				String numberStrOrig = input.substring(matcher.start(), matcher.end());
				output.append("{" + numberStrOrig + "}{" + expandedStr + "}");
			} else {
				output.append(expandedStr);
			}
			prevMatchEnd = matcher.end();
		}
		if (prevMatchEnd < input.length()) {
			String nonMatchingSubStr = input.substring(prevMatchEnd, input.length());
			output.append(nonMatchingSubStr);
		}
		return output.toString();
	}

	protected String expandByHandlingDashes(String numberStr) {
		StringBuffer output = new StringBuffer();
		if (numberStr.charAt(0) == '-') {
			output.append(numberExpansionProperties.getProperty("-") + SPACE);
			numberStr = numberStr.substring(1);
		}
		String[] parts = numberStr.split("-");
		for (String part: parts) {
			output.append(expandByHandlingDots(part) + SPACE);
		}
		return output.toString().trim().replaceAll("\\s+", " ");
	}

	protected String expandByHandlingDots(String numberStr) {
		StringBuffer output = new StringBuffer();
		String[] parts = numberStr.split("\\.");
		if (parts.length == 2 && !parts[1].contains(",")) {
			output.append(expandByHandlingCommas(parts[0]) + SPACE);
			output.append(numberExpansionProperties.getProperty(".") + SPACE);
			output.append(expandDigitsIndividually(parts[1]) + SPACE);
		} else {
			for (String part : parts) {
				output.append(expandByHandlingCommas(part));
			}
		}
		return output.toString();
	}

	protected String expandByHandlingCommas(String numberStr) {
		StringBuffer output = new StringBuffer();
		if (numberStr.contains(",")) {
			if (validCommas(numberStr)) {
				String numberStrWithoutCommas = numberStr.replace(",", "");
				output.append(expandNumber(numberStrWithoutCommas));
			} else {
				String[] parts = numberStr.split(",");
				for (String part : parts) {
					output.append(expandNumber(part));
				}
			}
		} else if (numberStr.length() <= 7) {
			output.append(expandNumber(numberStr) + SPACE);
		} else {
			output.append(expandDigitsIndividually(numberStr) + SPACE);
		}
		return output.toString();
	}

	protected boolean validCommas(String numberStr) {
		int numDigits = numberStr.replace(",", "").length();
		if (numDigits <= 3) {
			return false;
		}
		if (numDigits <= 5) {
			return commaPattern1.matcher(numberStr).matches();
		}
		if (numDigits <= 7) {
			return commaPattern2.matcher(numberStr).matches();
		}
		if (numDigits <= 9) {
			return commaPattern3.matcher(numberStr).matches();
		}
		return commaPattern4.matcher(numberStr).matches();
	}

	protected String expandNumber(String numberString) {
		long numberToExpand = 0;
		try {
			numberToExpand = Long.parseLong(numberString);
		} catch (NumberFormatException e) {
			// ignore
		}
		if (numberToExpand == 0) {
			return expandDigitsIndividually(numberString);
		} else {
			return expandNumber(numberToExpand);
		}
	}

	protected String expandDigitsIndividually(String numberString) {
		StringBuffer strBuf = new StringBuffer();
		for (char c : numberString.toCharArray()) {
			strBuf.append(numberExpansionProperties.getProperty(c + "") + SPACE);
		}
		return strBuf.toString();
	}

	public int getLanguageDigitZero() {
		return language.script.digitZero;
	}

	public int getLanguageDigitNine() {
		return language.script.digitNine;
	}

	char getEnglishEquivalentOfLanguageDigit(int scriptDigit) {
		return (char) (scriptDigit - getLanguageDigitZero() + ENGLISH_DIGIT_ZERO);
	}

	protected String handleSandhiWithNextWord(String expandedStr, String restOfSentence) {
		for (int i = 1;; i++) {
			String suffix = numberExpansionProperties.getProperty("s" + i);
			String prefix = numberExpansionProperties.getProperty("p" + i);
			String replace = numberExpansionProperties.getProperty("r" + i);
			if (suffix != null && prefix != null && replace != null) {
				if (expandedStr.endsWith(suffix) && restOfSentence.trim().startsWith(prefix)) {
					if (replace.equalsIgnoreCase("delete") || replace.equalsIgnoreCase("remove")) {
						replace = "";
					}
					expandedStr = expandedStr.substring(0, expandedStr.length() - suffix.length()) + replace;
					break;
				}
			} else {
				break;
			}
		}
		return expandedStr;
	}

	protected String sandhi(String word1, String word2, boolean doSandhi) {
		word1 = word1.trim();
		word2 = word2.trim();
		if (doSandhi && word1.length() > 0 && word2.length() > 0) {
			int prevChar = word1.codePointAt(word1.length() - 1);
			int nextChar = word2.codePointAt(0);
			if (isDependentVowel(prevChar) && isVowel(nextChar)) {
				String dependentVowel = Character.toString(convertToDependentVowel(nextChar));
				return word1.substring(0, word1.length() - 1) + dependentVowel + word2.substring(1);
			}
		}
		return word1 + SPACE + word2;
	}

	public boolean isVowel(int codePoint) {
		return UnicodeOffsets.isVowel(codePoint - language.script.unicodeBlockStart);
	}

	public boolean isDependentVowel(int codePoint) {
		return UnicodeOffsets.isDependentVowel(codePoint - language.script.unicodeBlockStart);
	}

	public int convertToDependentVowel(int vowel) {
		UnicodeOffsets vowelLetter = UnicodeOffsets.valueOf(vowel - language.script.unicodeBlockStart);
		UnicodeOffsets vowelSign = UnicodeOffsets.convertToVowelSign(vowelLetter);
		return vowelSign.offset + language.script.unicodeBlockStart;
	}

	public String removeNumbersAndCurlyBrackets(String text) {
		char langZero = (char) language.script.digitZero;
		char langNine = (char) language.script.digitNine;
		text = text.replaceAll("\\{[" + langZero + "-" + langNine + ",.\\-]+\\}", "");
		text = text.replaceAll("\\{[\\d,.\\-]+\\}", "");
		text = text.replaceAll("\\{", "");
		text = text.replaceAll("\\}", "");
		return text;
	}

	public static class Arguments extends CommandLineOptions {
		StringOption inputFilePath = new StringOption("in", "input", "Path of input text file.");
		StringOption language = new StringOption("lang", "language", "Language of the input text file");
		StringOption outputFilePath = new StringOption("out", "output",
				"Path of output text file. If this is not specified, the output will be saved in the same directpory as that of the input with '_expanded' "
						+ "(or '_cleaned' in case of 'remove-numbers-brackets') added to the filename.");
		BooleanOption retainNumbersForValidation = new BooleanOption("retain", "retain-numbers",
				"Retain original numbers for validation. Original numbers and their expansion will be enclosed in curly brackets.");
		BooleanOption removeNumbersAndCurlyBrackets = new BooleanOption("remove", "remove-numbers-brackets",
				"Remove the original numbers and the curly brackets around their expansion that was previoulsy inserted using the 'retain-numbers' option.");
		IntegerOption printStartNumber = new IntegerOption("from", "show-number-expansion-from", 0,
				"Prints number expansion from this number");
		IntegerOption printEndNumber = new IntegerOption("to", "show-number-expansion-to", 0,
				"Prints number expansion upto this number");

		public Arguments() {
			super();
			language.setRequired(true);
			options.addOption(inputFilePath);
			options.addOption(language);
			options.addOption(outputFilePath);
			options.addOption(retainNumbersForValidation);
			options.addOption(removeNumbersAndCurlyBrackets);
			options.addOption(printStartNumber);
			options.addOption(printEndNumber);
		}
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(NumberExpansion.class.getCanonicalName());
			return;
		}
		String inputFilePath = arguments.inputFilePath.getStringValue();
		String languageStr = arguments.language.getStringValue();
		String outputFilePath = arguments.outputFilePath.getStringValue();
		boolean retainNumbersForValidation = arguments.retainNumbersForValidation.getBoolValue();
		boolean removeNumbersAndCurlyBrackets = arguments.removeNumbersAndCurlyBrackets.getBoolValue();
		int printtStartNumber = arguments.printStartNumber.getIntValue();
		int printtEndNumber = arguments.printEndNumber.getIntValue();

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
		NumberExpansion numberExpansion = NumberExpansion.getInstance(language);
		if (printtEndNumber > printtStartNumber) {
			for (int n = printtStartNumber; n <= printtEndNumber; n++) {
				System.out.println(n + ": " + numberExpansion.expandNumber(n));
			}
		}
		if (inputFilePath == null) {
			return;
		}
		if (outputFilePath == null) {
			int indx = inputFilePath.lastIndexOf('.');
			String suffix = removeNumbersAndCurlyBrackets ? "_cleaned" : "_expanded";
			outputFilePath = inputFilePath.substring(0, indx) + suffix + inputFilePath.substring(indx);
		}
		try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				String newContent;
				if (removeNumbersAndCurlyBrackets) {
					newContent = numberExpansion.removeNumbersAndCurlyBrackets(line);
				} else {
					newContent = numberExpansion.expandNumbers(line, retainNumbersForValidation);
				}
				bw.write(newContent + "\n");
			}
			System.out.println("Output saved to " + outputFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
