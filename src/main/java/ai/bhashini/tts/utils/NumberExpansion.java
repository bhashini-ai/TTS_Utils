package ai.bhashini.tts.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Properties;

public class NumberExpansion {
	public static final int ENGLISH_DIGIT_ZERO = 0x30;
	public static final int ENGLISH_DIGIT_NINE = 0x39;
	public static final int SYMBOL_COMMA = 0x2C;
	public static final int SYMBOL_DOT = 0x2E;
	public static final String SPACE = " ";

	protected Language language;
	protected Properties numberExpansionProperties = new Properties();

	// Singleton class => private constructor
	private NumberExpansion(Language language) {
		this.language = language;
		loadNumberExpansionProperties(language + "_NumberExpansion.properties");
	}

	private HashMap<Language, NumberExpansion> uniqueInstancesMap = new HashMap<>();

	public NumberExpansion getInstance(Language language) {
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
		if (number <= 99) {
			return expandBasedOnPattern(number, 10, "x", "0", true);
		}
		if (number <= 999) {
			return expandBasedOnPattern(number, 100, "xx", "00", false);
		}
		if (language == Language.Telugu) {
			if (number >= 1001 && number <= 1999) {
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

	public static void main(String[] args) {
		NumberExpansion numberExpansion = new NumberExpansion(Language.Telugu);
		for (int i = 1; i <= 10000; i++) {
			System.out.println(i + ": " + numberExpansion.expandNumber(i));
		}
	}

}
