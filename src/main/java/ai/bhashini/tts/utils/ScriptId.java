package ai.bhashini.tts.utils;

public enum ScriptId {
	Deva("Devanagari", 0x0900),
	Beng("Bengali", 0x0980),
	Guru("Gurmukhi", 0x0A00),
	Gujr("Gujarati", 0x0A80),
	Orya("Oriya", 0x0B00),
	Taml("Tamil", 0x0B80),
	Telu("Telugu", 0x0C00),
	Knda("Kannada", 0x0C80),
	Mlym("Malayalam", 0x0D00);

	public final String scriptName;
    public final int unicodeBlockStart;

	private ScriptId(String scriptName, int unicodeBlockStart) {
		this.scriptName = scriptName;
		this.unicodeBlockStart = unicodeBlockStart;
	}

	public boolean isValidCodepoint(int c) {
		return (c >= unicodeBlockStart && c <= unicodeBlockStart + 0x7f);
	}

	@Override
	public String toString() {
		return scriptName;
	}

}
