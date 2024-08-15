package ai.bhashini.tts.utils;

public enum Language {
	// https://en.wikipedia.org/wiki/List_of_ISO_639_language_codes
	English(Script.English, "eng", "en"),
	Hindi(Script.Devanagari, "hin", "hi"),
	Bengali(Script.Bengali, "ben", "bn"),
	Marathi(Script.Devanagari, "mar", "mr"),
	Telugu(Script.Telugu, "tel", "te"),
	Tamil(Script.Tamil, "tam", "ta"),
	Gujarati(Script.Gujarati, "guj", "gu"),
	Urdu(Script.Urdu, "urd", "ur"),
	Bhojpuri(Script.Devanagari, "bho", "bho"), // https://en.wikipedia.org/wiki/Bhojpuri_language
	Kannada(Script.Kannada, "kan", "kn"),
	Odia(Script.Odia, "ori", "or", "ory_Orya"),
	Malayalam(Script.Malayalam, "mal", "ml"),
	Punjabi(Script.Gurmukhi, "pan", "pa"),
	Chhattisgarhi(Script.Devanagari, "hne", "hne"), // https://en.wikipedia.org/wiki/Chhattisgarhi_language
	Assamese(Script.Bengali, "asm", "as"),
	Maithili(Script.Devanagari, "mai", "mai"), // https://en.wikipedia.org/wiki/Maithili_language
	Magahi(Script.Devanagari, "mag", "mag"), // https://en.wikipedia.org/wiki/Magahi_language
	Santali(Script.Devanagari, "sat", "sat"), // https://en.wikipedia.org/wiki/Santali_language
	Kashmiri(Script.Devanagari, "kas", "ks"),
	Nepali(Script.Devanagari, "nep", "ne", "npi_Deva"),
	Sindhi(Script.Devanagari, "snd", "sd"),
	Dogri(Script.Devanagari, "doi", "doi"), // https://en.wikipedia.org/wiki/Dogri_language
	Konkani(Script.Devanagari, "kok", "kok", "gom_Deva"), // https://en.wikipedia.org/wiki/Konkani_language
	Manipuri(Script.Bengali, "mni", "mni"), // https://en.wikipedia.org/wiki/Meitei_language
	Bodo(Script.Devanagari, "brx", "brx"), // https://en.wikipedia.org/wiki/Boro_language_(India)
	Sanskrit(Script.Devanagari, "san", "sa");

	private Language(Script script, String threeLetterCode, String twoLetterCode, String bcp47Tag) {
		this.script = script;
		this.threeLetterCode = threeLetterCode;
		this.twoLetterCode = twoLetterCode;
		this.bcp47Tag = bcp47Tag;
	}

	private Language(Script script, String threeLetterCode, String twoLetterCode) {
		this(script, threeLetterCode, twoLetterCode, threeLetterCode + "_" + script.fourLetterCode);
	}

	public final Script script;
	public final String threeLetterCode;
	public final String twoLetterCode;
	public final String bcp47Tag;

}
