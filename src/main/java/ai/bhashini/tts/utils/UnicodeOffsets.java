package ai.bhashini.tts.utils;

import java.util.HashMap;

public enum UnicodeOffsets {
    SIGN_INVERTED_CANDRABINDU(0x00),
    SIGN_CANDRABINDU(0x01),
    SIGN_ANUSVARA(0x02),
    SIGN_VISARGA(0x03),

    LETTER_SHORT_A(0x04),
    LETTER_A(0x05),
    LETTER_AA(0x06),
	LETTER_I(0x07),
	LETTER_II(0x08),
	LETTER_U(0x09),
	LETTER_UU(0x0A),
	LETTER_VOCALIC_R(0x0B),
	LETTER_VOCALIC_L(0x0C),
	LETTER_CHANDRA_E(0x0D),
	LETTER_E(0x0E),
	LETTER_EE(0x0F),
	LETTER_AI(0x10),
	LETTER_CHANDRA_O(0x11),
	LETTER_O(0x12),
	LETTER_OO(0x13),
	LETTER_AU(0x14),

	LETTER_VOCALIC_RR(0x60),
	LETTER_VOCALIC_LL(0x61),

    SIGN_NUKTA(0x3C),
    SIGN_AVAGRAHA(0x3D),

    VOWEL_SIGN_AA(0x3E),
	VOWEL_SIGN_I(0x3F),
	VOWEL_SIGN_II(0x40),
	VOWEL_SIGN_U(0x41),
	VOWEL_SIGN_UU(0x42),
	VOWEL_SIGN_VOCALIC_R(0x43),
	VOWEL_SIGN_VOCALIC_RR(0x44),
	VOWEL_SIGN_CHANDRA_E(0x45),
	VOWEL_SIGN_E(0x46),
	VOWEL_SIGN_EE(0x47),
	VOWEL_SIGN_AI(0x48),
	VOWEL_SIGN_CHANDRA_O(0x49),
	VOWEL_SIGN_O(0x4A),
	VOWEL_SIGN_OO(0x4B),
	VOWEL_SIGN_AU(0x4C),

	VOWEL_SIGN_VOCALIC_L(0x62),
	VOWEL_SIGN_VOCALIC_LL(0x63),

	HALANT(0X4D),

	LENGTH_MARK(0x55),
    AI_LENGTH_MARK(0x56),
    AU_LENGTH_MARK(0x57);

	public final int offset;

	private UnicodeOffsets(int offset) {
		this.offset = offset;
	}

	// https://www.baeldung.com/java-enum-values
	private static final HashMap<Integer, UnicodeOffsets> BY_OFFSET = new HashMap<>();

	static {
		for (UnicodeOffsets u : values()) {
			BY_OFFSET.put(u.offset, u);
		}
	}

	public static UnicodeOffsets valueOf(Integer offset) {
		return BY_OFFSET.get(offset);
	}

	public static boolean isVowel(int offset) {
		if ((offset >= LETTER_SHORT_A.offset && offset <= LETTER_AU.offset) || offset == LETTER_VOCALIC_RR.offset
				|| offset == LETTER_VOCALIC_RR.offset || offset == SIGN_ANUSVARA.offset) {
			return true;
		}
		return false;
	}

	public static boolean isDependentVowel(int offset) {
		if ((offset >= SIGN_NUKTA.offset && offset <= VOWEL_SIGN_AU.offset) || offset == VOWEL_SIGN_VOCALIC_L.offset
				|| offset == VOWEL_SIGN_VOCALIC_LL.offset || offset == AI_LENGTH_MARK.offset
				|| offset == LENGTH_MARK.offset) {
			return true;
		}
		return false;
	}

	public static UnicodeOffsets convertToVowelSign(UnicodeOffsets vowelLetter) {
		UnicodeOffsets vowelSign;
		switch (vowelLetter) {
		case LETTER_A:
		case LETTER_AA:
			vowelSign = VOWEL_SIGN_AA;
			break;
		case LETTER_I:
			vowelSign = VOWEL_SIGN_I;
			break;
		case LETTER_II:
			vowelSign = VOWEL_SIGN_II;
			break;
		case LETTER_U:
			vowelSign = VOWEL_SIGN_U;
			break;
		case LETTER_UU:
			vowelSign = VOWEL_SIGN_UU;
			break;
		case LETTER_CHANDRA_E:
			vowelSign = VOWEL_SIGN_CHANDRA_E;
			break;
		case LETTER_E:
			vowelSign = VOWEL_SIGN_E;
			break;
		case LETTER_EE:
			vowelSign = VOWEL_SIGN_EE;
			break;
		case LETTER_AI:
			vowelSign = VOWEL_SIGN_AI;
			break;
		case LETTER_CHANDRA_O:
			vowelSign = VOWEL_SIGN_CHANDRA_O;
			break;
		case LETTER_O:
			vowelSign = VOWEL_SIGN_O;
			break;
		case LETTER_OO:
			vowelSign = VOWEL_SIGN_OO;
			break;
		case LETTER_AU:
			vowelSign = VOWEL_SIGN_AU;
			break;
		default:
			vowelSign = vowelLetter;
			break;
		}
		return vowelSign;
	}

}
