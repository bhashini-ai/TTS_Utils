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
	English(0x0000, 0x007F, 0x0030, 0x0039),
	Urdu(0x0600, 0x06FF, 0x06F0, 0x06F9);

	public final int unicodeBlockStart;
	public final int unicodeBlockEnd;
	public final int digitZero;
	public final int digitNine;

	private Script(int unicodeBlockStart) {
		this(unicodeBlockStart, unicodeBlockStart + 0x7F, unicodeBlockStart + 0x66, unicodeBlockStart + 0x6F);
	}

	private Script(int unicodeBlockStart, int unicodeBlockEnd, int digitZero, int digitNine) {
		this.unicodeBlockStart = unicodeBlockStart;
		this.unicodeBlockEnd = unicodeBlockEnd;
		this.digitZero = digitZero;
		this.digitNine = digitNine;
	}

	public boolean isValidCodepoint(int c) {
		return (c >= unicodeBlockStart && c <= unicodeBlockEnd);
	}

}
