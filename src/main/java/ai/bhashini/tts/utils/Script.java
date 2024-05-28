package ai.bhashini.tts.utils;

public enum Script {
	Devanagari(0x0900),
	Bengali(0x0980),
	Gurmukhi(0x0A00),
	Gujarati(0x0A80),
	Oriya(0x0B00),
	Tamil(0x0B80),
	Telugu(0x0C00),
	Kannada(0x0C80),
	Malayalam(0x0D00),
	English(0x0000),
	Urdu(0x0600, 0x06ff);

	public final int unicodeBlockStart;
	public final int unicodeBlockEnd;

	private Script(int unicodeBlockStart) {
		this(unicodeBlockStart, unicodeBlockStart + 0x7f);
	}

	private Script(int unicodeBlockStart, int unicodeBlockEnd) {
		this.unicodeBlockStart = unicodeBlockStart;
		this.unicodeBlockEnd = unicodeBlockEnd;
	}

	public boolean isValidCodepoint(int c) {
		return (c >= unicodeBlockStart && c <= unicodeBlockEnd);
	}

}
