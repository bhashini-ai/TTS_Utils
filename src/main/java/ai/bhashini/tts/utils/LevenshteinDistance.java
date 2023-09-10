package ai.bhashini.tts.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * See 'edit distance' algorithm in 'Pattern Classification' book by Duda, Hart & Stork
 * (section 8.5.2, page 418 in Second Edition).
 * 
 */
public class LevenshteinDistance {
    int outputSize;
    int groundTruthSize;
    int editDistance;
	ArrayList<Integer> substitutionPositionsInOutput = new ArrayList<Integer>();
	ArrayList<Integer> substitutionPositionsInGroundTruth = new ArrayList<Integer>();
	ArrayList<Integer> extraPositionsInOutput = new ArrayList<Integer>();
	ArrayList<Integer> extraPositionsInGroundTruth = new ArrayList<Integer>();

	public <T> LevenshteinDistance(List<T> outputText, List<T> groundTruthText) {
		int m = outputText.size();
		int n = groundTruthText.size();
		int distance[][] = new int[m + 1][n + 1];
		for (int i = 1; i <= m; i++) {
			distance[i][0] = i;
		}
		for (int j = 1; j <= n; j++) {
			distance[0][j] = j;
		}
		for (int j = 1; j <= n; j++) {
			for (int i = 1; i <= m; i++) {
				if (outputText.get(i - 1).equals(groundTruthText.get(j - 1))) {
					distance[i][j] = distance[i - 1][j - 1];
				} else {
					distance[i][j] = MinValueType.min(distance[i - 1][j - 1], distance[i][j - 1],
							distance[i - 1][j], distance[i][j], outputText.get(i - 1), groundTruthText.get(j - 1)).minValue + 1;
				}
			}
		}

		this.outputSize = outputText.size();
		this.groundTruthSize = groundTruthText.size();

		for (int j = distance[0].length - 1, i = distance.length - 1; j > 0 || i > 0;) {
			MinValueType minValueType;
			if (i > 0 && j > 0) {
				minValueType = MinValueType.min(distance[i - 1][j - 1], distance[i][j - 1],
						distance[i - 1][j], distance[i][j], 0, 0);
			} else if (i > 0) { // first column
				minValueType = new MinValueType(distance[i - 1][j], MinValueType.TOP);
			} else { // first row
				minValueType = new MinValueType(distance[i][j - 1], MinValueType.LEFT);
			}
			switch (minValueType.minType) {
			case MinValueType.DIAG_PREV:
				if (minValueType.minValue != distance[i][j]) {
					substitutionPositionsInOutput.add(i - 1);
					substitutionPositionsInGroundTruth.add(j - 1);
				}
				i--;
				j--;
				break;
			case MinValueType.TOP:
				extraPositionsInOutput.add(i - 1);
				i--;
				break;
			default:
				extraPositionsInGroundTruth.add(j - 1);
				j--;
			}
		}

		Collections.reverse(substitutionPositionsInOutput);
		Collections.reverse(substitutionPositionsInGroundTruth);
		Collections.reverse(extraPositionsInOutput);
		Collections.reverse(extraPositionsInGroundTruth);

		this.editDistance = distance[m][n];
	}

	static class MinValueType {
		int minValue;
		int minType;

		static final int DIAG_PREV = 0;
		static final int LEFT = 1;
		static final int TOP = 2;

		MinValueType(int _minValue, int _minType) {
			minValue = _minValue;
			minType = _minType;
		}

		static <T> MinValueType min(int diagPrev, int left, int top, int current, T s, T t) {
			int minValue;
			int minType;
			if (diagPrev < top) {
				if (diagPrev < left) {
					if (s instanceof Character && ((Character) s == '\n' || (Character) t == '\n')) {
						minValue = top < left ? top : left;
						minType = top < left ? TOP : LEFT;
					} else {
						minValue = diagPrev;
						minType = DIAG_PREV;
					}
				} else if (diagPrev == left && diagPrev == current) {
					minValue = diagPrev;
					minType = DIAG_PREV;
				} else {
					minValue = left;
					minType = LEFT;
				}
			} else if (diagPrev == top && diagPrev == current) {
				if (diagPrev <= left) {
					minValue = diagPrev;
					minType = DIAG_PREV;
				} else {
					minValue = left;
					minType = LEFT;
				}
			} else {
				minValue = top < left ? top : left;
				minType = top < left ? TOP : LEFT;
			}
			return new MinValueType(minValue, minType);
		}
	}

	public int numSubstitutions() {
		return substitutionPositionsInOutput.size();
	}

	public int numExtrasInOutput() {
		return extraPositionsInOutput.size();
	}

	public int numExtrasInGroundTruth() {
		return extraPositionsInGroundTruth.size();
	}

	public double getErrorRate() {
		return 1.0 * (numSubstitutions() + numExtrasInGroundTruth() + numExtrasInOutput()) / groundTruthSize;
	}

	public static String getHeader() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("Accuracy, ");
		stringBuffer.append("Ground truth size" + ", ");
		stringBuffer.append("Output size" + ", ");
		stringBuffer.append("# of substitutions, ");
		stringBuffer.append("# of insertions, ");
		stringBuffer.append("# of deletions\n");
		return stringBuffer.toString();
	 }

	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		double accuracy = (groundTruthSize - numSubstitutions() - numExtrasInGroundTruth() - numExtrasInOutput()) * 100.0
				/ groundTruthSize;
		stringBuffer.append(new DecimalFormat("#.##").format(accuracy) + ", ");
		stringBuffer.append(groundTruthSize + ", ");
		stringBuffer.append(outputSize + ", ");
		stringBuffer.append(numSubstitutions() + ", ");
		stringBuffer.append(numExtrasInOutput() + ", ");
		stringBuffer.append(numExtrasInGroundTruth() + "\n");
		return stringBuffer.toString();
	}

}
