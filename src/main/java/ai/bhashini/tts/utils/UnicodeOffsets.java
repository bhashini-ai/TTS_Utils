package ai.bhashini.tts.utils;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

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
	HALANT(0X4D),
	VOWEL_SIGN_PRISHTHAMATRA_E(0x4E),
	LETTER_KHANDA_T(0x4E),

	VOWEL_SIGN_VOCALIC_L(0x62),
	VOWEL_SIGN_VOCALIC_LL(0x63),

	LETTER_K(0x15),
	LETTER_KH(0x16),
	LETTER_G(0x17),
	LETTER_GH(0x18),
	LETTER_NG(0x19),

	LETTER_C(0x1A),
	LETTER_CH(0x1B),
	LETTER_J(0x1C),
	LETTER_JH(0x1D),
	LETTER_NY(0x1E),

	LETTER_TT(0x1F),
	LETTER_TTH(0x20),
	LETTER_DD(0x21),
	LETTER_DDH(0x22),
	LETTER_NN(0x23),

	LETTER_T(0x24),
	LETTER_TH(0x25),
	LETTER_D(0x26),
	LETTER_DH(0x27),
	LETTER_N(0x28),
	LETTER_NNN(0x29),

	LETTER_P(0x2A),
	LETTER_PH(0x2B),
	LETTER_B(0x2C),
	LETTER_BH(0x2D),
	LETTER_M(0x2E),

	LETTER_Y(0x2F),
	LETTER_R(0x30),
	LETTER_RR(0x31),
	LETTER_L(0x32),
	LETTER_LL(0x33),
	LETTER_LLL(0x34),

	LETTER_V(0x35),
	LETTER_SH(0x36),
	LETTER_SS(0x37),
	LETTER_S(0x38),
	LETTER_H(0x39),

	LENGTH_MARK(0x55),
    AI_LENGTH_MARK(0x56),
    AU_LENGTH_MARK(0x57),

	LETTER_Q(0x58),
	LETTER_KHH(0x59),
	LETTER_GHH(0x5A),
	LETTER_Z(0x5B),
	LETTER_DDDH(0x5C),
	LETTER_RH(0x5D),
	LETTER_F(0x5E),
	LETTER_YY(0x5F),

	DIGIT_ZERO(0x66),
	DIGIT_ONE(0x67),
	DIGIT_TWO(0x68),
	DIGIT_THREE(0x69),
	DIGIT_FOUR(0x6A),
	DIGIT_FIVE(0x6B),
	DIGIT_SIX(0x6C),
	DIGIT_SEVEN(0x6D),
	DIGIT_EIGHT(0x6E),
	DIGIT_NINE(0x6F);
	
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
				|| offset == LENGTH_MARK.offset || offset == AU_LENGTH_MARK.offset) {
			return true;
		}
		return false;
	}

	public static boolean isConsonant(int offset) {
		if (offset >= LETTER_K.offset && offset <= LETTER_H.offset) {
			return true;
		}
		// Devanagari
		if ((offset >= LETTER_Q.offset && offset <= LETTER_YY.offset) || (offset >= 0x78 && offset <= 0x7F)) {
			return true;
		}
		// Bengali
		if (offset == LETTER_KHANDA_T.offset) {
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

	public static int convertToVowelSign(int offset) {
		return convertToVowelSign(valueOf(offset)).offset;
	}

	public String getUnicode(Script script) {
		return script.getUnicode(offset);
	}

	public static class UnicodeNormalization {
		private Script script;

		// Enforce similar usage as that of NumberExpansion and AbbreviationExpansion
		private UnicodeNormalization(Script script) {
			this.script = script;
		}

		// Thread-safe map
		private static ConcurrentHashMap<Script, UnicodeNormalization> uniqueInstancesMap = new ConcurrentHashMap<>();

		public static UnicodeNormalization getInstance(Script script) {
			return uniqueInstancesMap.computeIfAbsent(script, UnicodeNormalization::new);
		}

		public String mergeVowelSigns(String text) {
			switch (script) {
			case Devanagari:
				text = mergeDevanagariVowelSigns(text);
				text = mergeNuktas(text);
				text = mergePrishthamatra(text);
				break;
			case Bengali:
				text = mergeBengaliVowelSigns(text);
				break;
			case Gurmukhi:
				break;
			case Gujarati:
				break;
			case Odia:
				break;
			case Tamil:
				text = mergeTamilVowelSigns(text);
				break;
			case Telugu:
				text = mergeTeluguVowelSigns(text);
				break;
			case Kannada:
				text = mergeKannadaVowelSigns(text);
				break;
			case Malayalam:
				text = mergeMalayalamVowelSigns(text);
				break;
			default:
				break;
			}
			return text;
		}

		private String mergeUnicode(UnicodeOffsets first, UnicodeOffsets second, UnicodeOffsets replace, String text) {
			String target = first.getUnicode(script) + second.getUnicode(script);
			String replacement = replace.getUnicode(script);
			return text.replaceAll(target, replacement);
		}

		private String replaceUnicode(UnicodeOffsets old, UnicodeOffsets replace, String text) {
			String target = old.getUnicode(script);
			String replacement = replace.getUnicode(script);
			return text.replaceAll(target, replacement);
		}

		public String mergeTwoPartVowels(UnicodeOffsets left, UnicodeOffsets right, UnicodeOffsets replace,
				String text) {
			StringBuilder newText = new StringBuilder();
			for (int i = 0; i < text.length();) {
				int ch = text.codePointAt(i);
				int offset = script.getUnicodeOffset(ch);

				// Try pattern: left + consonant + right
				if (offset == left.offset && i + 2 < text.length()) {
					int mid = text.codePointAt(i + 1);
					int last = text.codePointAt(i + 2);
					int lastOffset = script.getUnicodeOffset(last);

					if (isConsonant(mid) && lastOffset == right.offset) {
						// Append consonant + replacement vowel
						newText.append(Character.toChars(mid));
						newText.append(replace.getUnicode(script));
						i += 3;
						continue;
					}
				}

				// Default case: append original char
				newText.append(Character.toChars(ch));
				i += Character.charCount(ch);
			}
			return newText.toString();
		}

		public String mergeDevanagariVowelSigns(String text) {
			text = mergeUnicode(VOWEL_SIGN_AA, VOWEL_SIGN_CHANDRA_E, VOWEL_SIGN_CHANDRA_O, text);
			text = mergeUnicode(VOWEL_SIGN_AA, VOWEL_SIGN_E, VOWEL_SIGN_O, text);
			text = mergeUnicode(VOWEL_SIGN_AA, VOWEL_SIGN_EE, VOWEL_SIGN_OO, text);
			text = mergeUnicode(VOWEL_SIGN_AA, VOWEL_SIGN_AI, VOWEL_SIGN_AU, text);
			text = mergeUnicode(LETTER_A, VOWEL_SIGN_AA, LETTER_AA, text);
			return text;
		}

		public String mergeNuktas(String text) {
			text = mergeUnicode(LETTER_K, SIGN_NUKTA, LETTER_Q, text);
			text = mergeUnicode(LETTER_KH, SIGN_NUKTA, LETTER_KHH, text);
			text = mergeUnicode(LETTER_G, SIGN_NUKTA, LETTER_GHH, text);
			text = mergeUnicode(LETTER_J, SIGN_NUKTA, LETTER_Z, text);
			text = mergeUnicode(LETTER_DD, SIGN_NUKTA, LETTER_DDDH, text);
			text = mergeUnicode(LETTER_DDH, SIGN_NUKTA, LETTER_RH, text);
			text = mergeUnicode(LETTER_PH, SIGN_NUKTA, LETTER_F, text);
			text = mergeUnicode(LETTER_Y, SIGN_NUKTA, LETTER_YY, text);
			text = mergeUnicode(LETTER_N, SIGN_NUKTA, LETTER_NNN, text);
			text = mergeUnicode(LETTER_R, SIGN_NUKTA, LETTER_RR, text);
			text = mergeUnicode(LETTER_LL, SIGN_NUKTA, LETTER_LLL, text);
			return text;
		}

		public String mergePrishthamatra(String text) {
			text = mergeUnicode(VOWEL_SIGN_PRISHTHAMATRA_E, VOWEL_SIGN_E, VOWEL_SIGN_AI, text);
			text = mergeUnicode(VOWEL_SIGN_PRISHTHAMATRA_E, VOWEL_SIGN_AA, VOWEL_SIGN_O, text);
			text = mergeUnicode(VOWEL_SIGN_PRISHTHAMATRA_E, VOWEL_SIGN_O, VOWEL_SIGN_AU, text);
			return text;
		}

		public String mergeBengaliVowelSigns(String text) {
			text = mergeUnicode(LETTER_DD, SIGN_NUKTA, LETTER_DDDH, text);
			text = mergeUnicode(LETTER_DDH, SIGN_NUKTA, LETTER_RH, text);
			text = mergeUnicode(LETTER_Y, SIGN_NUKTA, LETTER_YY, text);

			text = mergeTwoPartVowels(VOWEL_SIGN_E, VOWEL_SIGN_AA, VOWEL_SIGN_O, text);
			text = mergeTwoPartVowels(VOWEL_SIGN_E, AU_LENGTH_MARK, VOWEL_SIGN_AU, text);

			return text;
		}

		public String mergeTamilVowelSigns(String text) {
			text = mergeUnicode(VOWEL_SIGN_E, VOWEL_SIGN_AA, VOWEL_SIGN_O, text);
			text = mergeUnicode(VOWEL_SIGN_EE, VOWEL_SIGN_AA, VOWEL_SIGN_OO, text);
			text = mergeUnicode(VOWEL_SIGN_E, AU_LENGTH_MARK, VOWEL_SIGN_AU, text);
			text = mergeUnicode(LETTER_O, AU_LENGTH_MARK, LETTER_AU, text);
	        return text;
	    }

	    public String mergeTeluguVowelSigns(String text) {
			text = mergeUnicode(VOWEL_SIGN_I, LENGTH_MARK, VOWEL_SIGN_II, text);
			text = mergeUnicode(VOWEL_SIGN_E, LENGTH_MARK, VOWEL_SIGN_EE, text);
			text = mergeUnicode(VOWEL_SIGN_O, LENGTH_MARK, VOWEL_SIGN_OO, text);
			text = mergeUnicode(VOWEL_SIGN_E, AI_LENGTH_MARK, VOWEL_SIGN_AI, text);
			text = mergeUnicode(VOWEL_SIGN_E, VOWEL_SIGN_U, VOWEL_SIGN_O, text);
	        return text;
	    }

	    public String mergeKannadaVowelSigns(String text) {
			text = mergeUnicode(VOWEL_SIGN_I, LENGTH_MARK, VOWEL_SIGN_II, text);
			text = mergeUnicode(VOWEL_SIGN_E, LENGTH_MARK, VOWEL_SIGN_EE, text);
			text = mergeUnicode(VOWEL_SIGN_O, LENGTH_MARK, VOWEL_SIGN_OO, text);
			text = mergeUnicode(VOWEL_SIGN_E, AI_LENGTH_MARK, VOWEL_SIGN_AI, text);
			text = mergeUnicode(VOWEL_SIGN_E, VOWEL_SIGN_UU, VOWEL_SIGN_O, text);
		    return text;
		}

		public String mergeMalayalamVowelSigns(String text) {
			text = mergeTwoPartVowels(VOWEL_SIGN_E, VOWEL_SIGN_AA, VOWEL_SIGN_O, text);
			text = mergeTwoPartVowels(VOWEL_SIGN_E, AU_LENGTH_MARK, AU_LENGTH_MARK, text);
			text = mergeTwoPartVowels(VOWEL_SIGN_EE, VOWEL_SIGN_AA, VOWEL_SIGN_OO, text);

			// MALAYALAM AU LENGTH MARK: used alone to write the /au/ dependent vowel in modern texts
			// MALAYALAM VOWEL SIGN AU: archaic form of the /au/ dependent vowel
			text = replaceUnicode(VOWEL_SIGN_AU, AU_LENGTH_MARK, text);

			return text;
		}

	}

}
