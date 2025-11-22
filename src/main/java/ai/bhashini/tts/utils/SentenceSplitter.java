package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.cli.ParseException;

import ai.bhashini.tts.utils.UnicodeOffsets.UnicodeNormalization;

public class SentenceSplitter {
	public static final Script[] SUPPORTED_SCRIPTS = { Script.Devanagari, Script.Bengali, Script.Tamil, Script.Telugu,
			Script.Kannada, Script.Malayalam };
	public static int MAX_UNICODES_IN_SENTENCE = 250;

	public static final int ZWNJ = 0x200C;

	private Script script;
	protected HashSet<String> initialsAndAcronyms;
	private Pattern numberedListsPattern;

	// Singleton class => private constructor
	private SentenceSplitter(Script script) {
		this.script = script;
		loadInitialsAndAcronyms(script.name() + "_InitialsAcronyms.tsv");
		compileNumberedListsRegexPattern();
	}

	private static ConcurrentHashMap<Script, SentenceSplitter> uniqueInstancesMap = new ConcurrentHashMap<>();

	public static SentenceSplitter getInstance(Script script) {
		return uniqueInstancesMap.computeIfAbsent(script, SentenceSplitter::new);
	}

	protected void loadInitialsAndAcronyms(String initialsAcronymsFile) {
		initialsAndAcronyms = new HashSet<>();
		InputStream is = getClass().getResourceAsStream("/" + initialsAcronymsFile);
		if (is == null) {
			System.out.println("Couldn't find " + initialsAcronymsFile);
			return;
		}
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\t");
				for (String content : contents) {
					initialsAndAcronyms.add(content);
				}
			}
		} catch (Exception e) {
			System.out.println("Error loading " + initialsAcronymsFile);
			e.printStackTrace();
		}
	}

	void compileNumberedListsRegexPattern() {
		String firstNativeChar = script.getUnicode(UnicodeOffsets.LETTER_A.offset);
		String lastNativeChar = script.getUnicode(UnicodeOffsets.LETTER_H.offset);
		String nativeCharacterRegexPattern = "[" + firstNativeChar + "-" + lastNativeChar + "]";
		numberedListsPattern = Pattern.compile("\\s+(\\d+)(\\.)(" + nativeCharacterRegexPattern + ")");
	}

	public String processNumberedLists(String text) {
		return numberedListsPattern.matcher(text).replaceAll("\n$1$2 $3");
	}

	public boolean isNumber(int c) {
		return (c >= '0' && c <= '9') || script.isNumber(c);
	}

	public static class Paragraph {
		public ArrayList<String> sentences;

		public Paragraph(ArrayList<String> sentences) {
			this.sentences = sentences;
		}
	}

	public static ArrayList<Paragraph> normalizeAndSplit(Language language, String text) {
		return normalizeAndSplit(language, text, MAX_UNICODES_IN_SENTENCE);
	}

	public static ArrayList<Paragraph> normalizeAndSplit(Language language, String text, int maxUnicodesInSentence) {
		String expandedText = text = SentenceSplitter.getInstance(language.script).processNumberedLists(text);
		expandedText = UnicodeNormalization.getInstance(language.script).mergeVowelSigns(expandedText);
		expandedText = AbbreviationExpansion.getInstance(language).expandAbbreviations(expandedText);
		expandedText = NumberExpansion.getInstance(language).expandNumbers(expandedText, false);
		return SentenceSplitter.getInstance(language.script).splitText(expandedText, maxUnicodesInSentence);
	}

	public ArrayList<Paragraph> splitText(String normalizedExpandedText, int maxUnicodesInSentence) {
		ArrayList<Paragraph> paragraphs = new ArrayList<>();
		for (String paragraphText : normalizedExpandedText.split("\\R")) {
			if (paragraphText.isBlank()) {
				continue;
			}
			ArrayList<String> candidateSentences = splitIntoSentences(paragraphText);
			ArrayList<String> sentences = splitLengthySentences(candidateSentences, maxUnicodesInSentence);
			// ArrayList<String> regroupedSentences = groupSentences(sentences, maxUnicodesInSentence);
			paragraphs.add(new Paragraph(sentences));
		}
		return paragraphs;
	}

	/**
	 * Based on
	 * https://indic-nlp-library.readthedocs.io/en/latest/_modules/indicnlp/tokenize/sentence_tokenize.html
	 * 
	 * @param inputText to be split into sentences
	 * @return list of sentences
	 */
	public ArrayList<String> splitIntoSentences(String text) {
		ArrayList<String> candidateSentences = new ArrayList<String>();
		int begin = 0;
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			text.codePointAt(i);
			if (ch == '.' || ch == '?' || ch == '!' || ch == '।' || ch == '॥') {
				if (ch == '.' && i > 0 && isNumber(text.charAt(i - 1))) {
					continue;
				}
				if (ch == '.' && i + 1 < text.length() && text.charAt(i + 1) == '.') {
					continue;
				}
				if (i + 1 < text.length() && (text.charAt(i + 1) == '"' || text.charAt(i + 1) == '”'
						|| text.charAt(i + 1) == '’' || text.charAt(i + 1) == 'ʼ' || text.charAt(i + 1) == '\'')) {
					continue;
				}
				String sentence = text.substring(begin, i + 1).trim();
				if (sentence.length() > 1) {
					candidateSentences.add(sentence);
				}
				begin = i + 1;
			}
		}
		String sentence = text.substring(begin).trim();
		if (sentence.length() > 1) {
			candidateSentences.add(sentence);
		}
		return mergeInitials(candidateSentences);
	}

	private ArrayList<String> mergeInitials(ArrayList<String> candidateSentences) {
		ArrayList<String> sentences = new ArrayList<>();
		for (int i = 0; i < candidateSentences.size(); i++) {
			String sentence = candidateSentences.get(i);
			String[] words = sentence.split("\\s+");
			if (words.length == 0) {
				continue;
			}
			String lastWord = words[words.length - 1];
			if (lastWord.length() <= 1) {
				sentences.add(sentence);
				continue;
			}
			String lastWordWithoutDelimiter = lastWord.substring(0, lastWord.length() - 1);
			if (lastWordWithoutDelimiter.length() > 0
					&& lastWordWithoutDelimiter.charAt(lastWordWithoutDelimiter.length() - 1) == ZWNJ) {
				lastWordWithoutDelimiter = lastWordWithoutDelimiter.substring(0, lastWordWithoutDelimiter.length() - 1);
			}
			if (lastWordWithoutDelimiter.length() > 0 && lastWordWithoutDelimiter.charAt(0) == '(') {
				lastWordWithoutDelimiter = lastWordWithoutDelimiter.substring(1);
			}
			if (lastWord.charAt(lastWord.length() - 1) == '.' && initialsAndAcronyms.contains(lastWordWithoutDelimiter)
					&& i + 1 < candidateSentences.size()) {
				// merge current sentence with next one
				candidateSentences.set(i + 1, sentence + " " + candidateSentences.get(i + 1));
			} else if (words.length == 1 && sentences.size() > 0) {
				// merge current sentence with previous one
				sentences.set(sentences.size() - 1, sentences.get(sentences.size() - 1) + " " + sentence);
			} else {
				sentences.add(sentence);
			}
		}
		return sentences;
	}

	public ArrayList<String> groupSentences(ArrayList<String> sentences, int maxUnicodesInGroup) {
		ArrayList<String> groupedSentences = new ArrayList<>();
		String previousSentence = null;
		for (String sentenceText : sentences) {
			if (previousSentence == null) {
				previousSentence = sentenceText;
			} else {
				if (previousSentence.length() + sentenceText.length() < maxUnicodesInGroup) {
					previousSentence += " " + sentenceText;
				} else {
					groupedSentences.add(previousSentence);
					previousSentence = sentenceText;
				}
			}
		}
		if (previousSentence != null) {
			groupedSentences.add(previousSentence);
		}
		return groupedSentences;
	}

	public ArrayList<String> splitLengthySentences(ArrayList<String> candidateSentences, int maxSentenceLength) {
		ArrayList<String> croppedSentences = new ArrayList<String>();
		for (String candidateSentence : candidateSentences) {
			if (candidateSentence.length() > maxSentenceLength) {
				int begin = 0;
				while (begin < candidateSentence.length()) {
					int end = findPrevWordBoundary(candidateSentence, begin, begin + maxSentenceLength);
					croppedSentences.add(candidateSentence.substring(begin, end));
					begin = end + 1;
				}
			} else {
				croppedSentences.add(candidateSentence);
			}
		}
		return croppedSentences;
	}

	private int findPrevWordBoundary(String sentence, int begin, int end) {
		if (end >= sentence.length()) {
			return sentence.length();
		}
		for (int i = end; i >= begin; i--) {
			if (sentence.charAt(i) == ' ') {
				return i;
			}
		}
		return end;
	}

	public void splitTextInFile(String inputFilePath, String outputFilePath) {
		String fileContents = FileUtils.getFileContents(inputFilePath);
		ArrayList<Paragraph> splitText = splitText(fileContents, MAX_UNICODES_IN_SENTENCE);
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath))) {
			for (Paragraph p : splitText) {
				for (String s : p.sentences) {
					bw.write(s + "\n");
				}
				bw.write("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class Arguments extends CommandLineOptions {
		StringOption inputFilePath = new StringOption("in", "input", "Path of input text file.");
		StringOption outputFilePath = new StringOption("out", "output",
				"Path of output text file. If this is not specified, the output will be saved in the same directpory as that of the input with '_split' added to the filename.");
		StringOption script = new StringOption("scr", "script", "Script of the input text file");

		public Arguments() {
			super();
			inputFilePath.setRequired(true);
			script.setRequired(true);
			options.addOption(inputFilePath);
			options.addOption(outputFilePath);
			options.addOption(script);
		}
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(SentenceSplitter.class.getCanonicalName());
			return;
		}
		String inputFilePath = arguments.inputFilePath.getStringValue();
		String scriptStr = arguments.script.getStringValue();
		String outputFilePath = arguments.outputFilePath.getStringValue();

		Script script;
		try {
			script = Script.valueOf(scriptStr);
		} catch (IllegalArgumentException e) {
			System.out.println("Unrecognized script name. Supported scripts are:");
			for (Script s : SUPPORTED_SCRIPTS) {
				System.out.println("\t" + s);
			}
			arguments.printHelp(SentenceSplitter.class.getCanonicalName());
			return;
		}
		SentenceSplitter sentenceSplitter = SentenceSplitter.getInstance(script);
		if (inputFilePath == null) {
			return;
		}
		if (outputFilePath == null) {
			outputFilePath = FileUtils.addSuffixToFilePath(inputFilePath, "_split");
		}
		System.out.println("Splitting text in " + inputFilePath);
		sentenceSplitter.splitTextInFile(inputFilePath, outputFilePath);
		System.out.println("Output saved to " + outputFilePath);
	}
}
