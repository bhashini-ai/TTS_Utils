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

	// Singleton class => private constructor
	private NumberExpansion(Language language) {
		this.language = language;
		loadNumberExpansionProperties(language + "_NumberExpansion.properties");
	}

	private static HashMap<Language, NumberExpansion> uniqueInstancesMap = new HashMap<>();

	public static NumberExpansion getInstance(Language language) {
		if (!uniqueInstancesMap.containsKey(language)) {
			uniqueInstancesMap.put(language, new NumberExpansion(language));
		}
		return uniqueInstancesMap.get(language);
	}

	protected void loadNumberExpansionProperties(String numberExpansionConfigurationXml) {
		// https://stackoverflow.com/a/30755227 and https://stackoverflow.com/a/17852323
		try {
			InputStream is = getClass().getResourceAsStream("/" + numberExpansionConfigurationXml);
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

	protected int replaceLanguageDigitsWithEnglishDigits(char[] inputChars) {
		int numbersStartIndex = -1;
		for (int i = 0; i < inputChars.length; i++) {
			int currentCharacter = inputChars[i];
			if (currentCharacter >= ENGLISH_DIGIT_ZERO && currentCharacter <= ENGLISH_DIGIT_NINE) {
				if (numbersStartIndex == -1) {
					numbersStartIndex = i;
				}
			} else if (currentCharacter >= getLanguageDigitZero() && currentCharacter <= getLanguageDigitNine()) {
				if (numbersStartIndex == -1) {
					numbersStartIndex = i;
				}
				inputChars[i] = getEnglishEquivalentOfLanguageDigit(currentCharacter);
			}
		}
		return numbersStartIndex;
	}

	public String expandNumbers(String input) {
		return expandNumbers(input, true, false);
	}

	public String expandNumbers(String input, boolean retainNumbersForValidation) {
		return expandNumbers(input, true, retainNumbersForValidation);
	}

	public String expandNumbers(String input, boolean readDecimalDigitsIndividually,
			boolean retainNumbersForValidation) {
		char[] inputChars = input.toCharArray();
		int numbersStartIndex = replaceLanguageDigitsWithEnglishDigits(inputChars);
		if (numbersStartIndex == -1) {
			return input;
		}
		StringBuffer outputBuffer = new StringBuffer();
		if (numbersStartIndex > 0) {
			if (input.charAt(numbersStartIndex - 1) == '-' && numbersStartIndex > 1
					&& (input.charAt(numbersStartIndex - 2) == ' ' || input.charAt(numbersStartIndex - 2) == '\n')) {
				numbersStartIndex--;
			}
			// copy the original non-numeral text into outputBuffer
			outputBuffer.append(input, 0, numbersStartIndex);
		}
		StringBuffer numbersBeforeDecimalPoint = new StringBuffer();
		int decimalStartIndex = inputChars.length;
		int numbersEndIndex = inputChars.length;
		boolean hasComma = false;
		boolean hasDot = false;
		boolean isNegative = false;
		for (int i = numbersStartIndex; i < inputChars.length; i++) {
			char currentCharacter = inputChars[i];
			if (currentCharacter == SYMBOL_COMMA && i < inputChars.length - 1 && inputChars[i + 1] >= ENGLISH_DIGIT_ZERO
					&& inputChars[i + 1] <= ENGLISH_DIGIT_NINE) {
				// Skip comma as in 80,55,255
				hasComma = true;
				continue;
			}
			if (currentCharacter == '-') {
				if (i == numbersStartIndex) {
					isNegative = true;
				} else {
					// currentCharacter is dash and is ignored
				}
				continue;
			}
			if (currentCharacter == SYMBOL_DOT && i < inputChars.length - 1) {
				// If a number with decimals are encountered, then
				// the fractional part must be read out individually.
				hasDot = true;
				decimalStartIndex = i + 1;
				break;
			}
			if (currentCharacter < ENGLISH_DIGIT_ZERO || currentCharacter > ENGLISH_DIGIT_NINE) {
				numbersEndIndex = i;
				break;
			}
			numbersBeforeDecimalPoint.append(currentCharacter);
		}
		StringBuffer strBuf = new StringBuffer();
		if (isNegative) {
			strBuf.append(numberExpansionProperties.getProperty("-") + SPACE);
		}
		int lengthOfNumber = numbersBeforeDecimalPoint.length();
		if (hasComma || hasDot || isNegative || lengthOfNumber <= 4) {
			// numbers that contain comma, dot or minus are expanded as well as numbers in
			// date format
			strBuf.append(expandNumber(numbersBeforeDecimalPoint.toString()));
		} else {
			// all other numbers are read out individually
			strBuf.append(expandDigitsIndividually(numbersBeforeDecimalPoint.toString()));
		}
		if (decimalStartIndex < inputChars.length) {
			StringBuffer numbersAfterDecimalPoint = new StringBuffer();
			for (int i = decimalStartIndex; i < inputChars.length; i++) {
				char currentCharacter = inputChars[i];
				if (currentCharacter < ENGLISH_DIGIT_ZERO || currentCharacter > ENGLISH_DIGIT_NINE) {
					numbersEndIndex = i;
					break;
				}
				numbersAfterDecimalPoint.append(currentCharacter);
			}
			if (numbersAfterDecimalPoint.length() > 0) {
				strBuf.append(numberExpansionProperties.getProperty(".") + SPACE);
				if (readDecimalDigitsIndividually) {
					strBuf.append(expandDigitsIndividually(numbersAfterDecimalPoint.toString()));
				} else {
					strBuf.append(expandNumber(numbersAfterDecimalPoint.toString()));
				}
			} else {
				strBuf.insert(strBuf.length() - 1, ".");
			}
		}
		if (retainNumbersForValidation) {
			outputBuffer.append("{");
			outputBuffer.append(input, numbersStartIndex, numbersEndIndex);
			outputBuffer.append("}{");
			outputBuffer.append(strBuf.toString().trim());
			outputBuffer.append("}");
		} else {
			outputBuffer.append(strBuf);
		}
		if (numbersEndIndex < inputChars.length) {
			// recursive call to expand any numbers further down the input string
			String remainingOutput = expandNumbers(input.substring(numbersEndIndex), readDecimalDigitsIndividually,
					retainNumbersForValidation);
			outputBuffer.append(remainingOutput);
		}
		return outputBuffer.toString();
	}

	private String expandNumber(String numberString) {
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

	private String expandDigitsIndividually(String numberString) {
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
