package ai.bhashini.tts.utils;

public enum Script {
	Devanagari(0x0900, "Deva"),
	Bengali(0x0980, "Beng"),
	Gurmukhi(0x0A00, "Guru"),
	Gujarati(0x0A80, "Gujr"),
	Odia(0x0B00, "Orya"),
	Tamil(0x0B80, "Taml"),
	Telugu(0x0C00, "Telu"),
	Kannada(0x0C80, "Knda"),
	Malayalam(0x0D00, "Mlym"),
	English(0x0000, 0x007F, 0x0030, 0x0039, "Latn"),
	Urdu(0x0600, 0x06FF, 0x06F0, 0x06F9, "Arab");

	public final int unicodeBlockStart;
	public final int unicodeBlockEnd;
	public final int digitZero;
	public final int digitNine;
	public final String fourLetterCode;

	private Script(int unicodeBlockStart, String fourLetterCode) {
		this(unicodeBlockStart, unicodeBlockStart + 0x7F, unicodeBlockStart + 0x66, unicodeBlockStart + 0x6F, fourLetterCode);
	}

	private Script(int unicodeBlockStart, int unicodeBlockEnd, int digitZero, int digitNine, String fourLetterCode) {
		this.unicodeBlockStart = unicodeBlockStart;
		this.unicodeBlockEnd = unicodeBlockEnd;
		this.digitZero = digitZero;
		this.digitNine = digitNine;
		this.fourLetterCode = fourLetterCode;
	}

	public boolean isValidCodepoint(int c) {
		return (c >= unicodeBlockStart && c <= unicodeBlockEnd);
	}

}
