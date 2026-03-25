package ai.bhashini.tts.utils;

public enum Language {
	// https://en.wikipedia.org/wiki/List_of_ISO_639_language_codes
	English(Script.English, "eng", "en", ""),
	Hindi(Script.Devanagari, "hin", "hi", "हिंदी"),
	Bengali(Script.Bengali, "ben", "bn", "বাংলা"),
	Marathi(Script.Devanagari, "mar", "mr", "मराठी"),
	Telugu(Script.Telugu, "tel", "te", "తెలుగు"),
	Tamil(Script.Tamil, "tam", "ta", "தமிழ்"),
	Gujarati(Script.Gujarati, "guj", "gu", "ગુજરાતી"),
	Urdu(Script.Urdu, "urd", "ur", "اردو"),
	Bhojpuri(Script.Devanagari, "bho", "bho", "भोजपुरी"), // https://en.wikipedia.org/wiki/Bhojpuri_language
	Kannada(Script.Kannada, "kan", "kn", "ಕನ್ನಡ"),
	Odia(Script.Odia, "ori", "or", "ଓଡିଆ", "ory_Orya"),
	Malayalam(Script.Malayalam, "mal", "ml", "മലയാളം"),
	Punjabi(Script.Gurmukhi, "pan", "pa", "ਪੰਜਾਬੀ"),
	Chhattisgarhi(Script.Devanagari, "hne", "hne", "छत्तीसगढ़ी"), // https://en.wikipedia.org/wiki/Chhattisgarhi_language
	Assamese(Script.Bengali, "asm", "as", "অসমীয়া"),
	Maithili(Script.Devanagari, "mai", "mai", "मैथिली"), // https://en.wikipedia.org/wiki/Maithili_language
	Magahi(Script.Devanagari, "mag", "mag", "मगही"), // https://en.wikipedia.org/wiki/Magahi_language
	Santali(Script.OlChiki, "sat", "sat", "ᱥᱟᱱᱛᱟᱲᱤ"), // https://en.wikipedia.org/wiki/Santali_language
	Kashmiri(Script.Devanagari, "kas", "ksd", "कॉशुर"),
	Kashmiri_Arabic(Script.Urdu, "kas", "ks", "کٲشُر"),
	Nepali(Script.Devanagari, "nep", "ne", "नेपाली", "npi_Deva"),
	Sindhi(Script.Devanagari, "snd", "sdd", "सिन्धी"),
	Sindhi_Arabic(Script.Urdu, "snd", "sd", "سِنڌِي‎"),
	Dogri(Script.Devanagari, "doi", "doi", "डोगरी"), // https://en.wikipedia.org/wiki/Dogri_language
	Konkani(Script.Devanagari, "kok", "kok", "कोंकणी", "gom_Deva"), // https://en.wikipedia.org/wiki/Konkani_language
	Manipuri(Script.Meitei, "mni", "mni", "ꯃꯩꯇꯩꯂꯣꯟ"), // https://en.wikipedia.org/wiki/Meitei_language
	Manipuri_Bengali(Script.Bengali, "mni", "mnib", "মৈতৈলোন্"),
	Bodo(Script.Devanagari, "brx", "brx", "बड़ो"), // https://en.wikipedia.org/wiki/Boro_language_(India)
	Sanskrit(Script.Devanagari, "san", "sa", "संस्कृतम्"),
	Auto(Script.English, "auto", "auto", "Detect"); // Placeholder for auto-detection of language during OCR

	Language(Script script, String threeLetterCode, String twoLetterCode, String nativeText, String bcp47Tag) {
		this.script = script;
		this.threeLetterCode = threeLetterCode;
		this.twoLetterCode = twoLetterCode;
		this.nativeText = nativeText;
		this.bcp47Tag = bcp47Tag;
	}

	Language(Script script, String threeLetterCode, String twoLetterCode, String nativeText) {
		this(script, threeLetterCode, twoLetterCode, nativeText, threeLetterCode + "_" + script.fourLetterCode);
	}

	public final Script script;
	public final String threeLetterCode;
	public final String twoLetterCode;
	public final String nativeText;
	public final String bcp47Tag;

	public String getNameAlongWithNativeText() {
		if (this == English) {
			return name();
		}
		return name() + " (" + nativeText + ")";
	}

	public static Language fromNameAlongWithNativeText(String combinedName) {
		if (combinedName == null || combinedName.isBlank()) {
			return null;
		}
		combinedName = combinedName.trim();
		if (combinedName.equalsIgnoreCase("English")) {
			return English;
		}
		int start = combinedName.indexOf('(');
		int end = combinedName.indexOf(')');
		if (start > -1 && end > -1) {
			return fromName(combinedName.substring(0, start).trim());
		}
		return null;
	}

	public static Language fromName(String name) {
		try {
			return Language.valueOf(name);
		} catch (Exception e) {
			return null;
		}
	}

	public static Language fromThreeLetterCode(String threeLetterCode) {
		for (Language language : Language.values()) {
			if (language.threeLetterCode.equals(threeLetterCode)) {
				return language;
			}
		}
		return null;
	}

	public static Language fromTwoLetterCode(String twoLetterCode) {
		for (Language language : Language.values()) {
			if (language.twoLetterCode.equals(twoLetterCode)) {
				return language;
			}
		}
		return null;
	}

	public static Language fromNativeText(String nativeText) {
		for (Language language : Language.values()) {
			if (language.nativeText.equals(nativeText)) {
				return language;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return name();
	}

	public static Language fromString(String langStr) {
		if (langStr == null || langStr.isBlank()) {
			return null;
		}
		langStr = langStr.trim();
		Language language = fromName(langStr);
		if (language == null) {
			language = fromTwoLetterCode(langStr.toLowerCase());
			if (language == null) {
				language = fromThreeLetterCode(langStr.toLowerCase());
				if (language == null) {
					language = fromNameAlongWithNativeText(langStr);
					if (language == null) {
						language = fromNativeText(langStr);
					}
				}
			}
		}
		return language;
	}

	static void main() {
		System.out.println(fromString("Hindi"));
		System.out.println(fromString("hi"));
		System.out.println(fromString("hin"));
		System.out.println(fromString("Hindi (हिंदी)"));
		System.out.println(fromString("हिंदी"));
		System.out.println(fromString(" Hindi "));
		System.out.println(fromString(" Hi"));
		System.out.println(fromString(" Hin"));
		System.out.println(fromString(" Hindi "));
		System.out.println(fromString(" Hindi1 "));
	}
}
