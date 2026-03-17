package ai.bhashini.tts.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.ParseException;

public class RepairAudio {
	static double MIN_SPIKE_PERCENT = 0.85;
	static double CLIP_PERCENT = 90.0;

	public static class Arguments extends CommandLineOptions {
		StringOption inputDir = new StringOption("in", "input-dir",
				"Input directory from which each .wav file will be repaired");
		StringOption outputDir = new StringOption("out", "output-dir", "Output directory for repaired .wav files");
		DoubleOption minSpikePercent = new DoubleOption("mp", "min-spike-percent", MIN_SPIKE_PERCENT,
				"Minimum absolute spike amplitude as a fraction of quantization max (range 0..1)");
		DoubleOption clipPercent = new DoubleOption("cp", "clip-percent", CLIP_PERCENT,
				"Normalize so peak amplitude is this percentage of full-scale range");

		public Arguments() {
			super();
			inputDir.setRequired(true);
			outputDir.setRequired(true);
			options.addOption(inputDir);
			options.addOption(outputDir);
			options.addOption(minSpikePercent);
			options.addOption(clipPercent);
		}
	}

	private static class AudioData {
		long[][] samples;
		int numChannels;
		int numFrames;
		int validBits;
		long sampleRate;
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(RepairAudio.class.getCanonicalName());
			return;
		}

		SystemTime sysTime = new SystemTime(true);
		File inputDir = new File(arguments.inputDir.getStringValue());
		File outputDir = new File(arguments.outputDir.getStringValue());
		File[] inputWavFiles = FileUtils.getWavFiles(inputDir);

		int totalArtifacts = 0;
		for (int i = 0; i < inputWavFiles.length; i++) {
			File inputWavFile = inputWavFiles[i];
			File outputWavFile = new File(outputDir, inputWavFile.getName());
			int repairedCount = repair(inputWavFile.getAbsolutePath(), outputWavFile.getAbsolutePath(),
					arguments.minSpikePercent.getDoubleValue(), arguments.clipPercent.getDoubleValue());
			totalArtifacts += repairedCount;
			System.out.println(inputWavFile.getName() + ": " + repairedCount);
		}

		System.out.println("");
		System.out.println("Total WAV files = " + inputWavFiles.length);
		System.out.println("Total repaired artifacts = " + totalArtifacts);
		sysTime.tock(true);
	}

	public static int repair(String inputWavFilePath, String outputWavFilePath, double minSpikePercent, double clipPercent) {
		try {
			AudioData audioData = readWavAudio(inputWavFilePath);
			double quantizationMax = WavFile.getQuantizationMax(audioData.validBits);
			long minSpikeAmplitude = Math.max(1L, Math.round(Math.abs(minSpikePercent) * quantizationMax));

			int repairedCount = 0;
			for (int c = 0; c < audioData.numChannels; c++) {
				repairedCount += repairNegativeClicks(audioData.samples[c], audioData.numFrames, minSpikeAmplitude);
			}

			double[][] normalizedAudio = normalizeAndClip(audioData.samples, audioData.numChannels, audioData.numFrames,
					audioData.validBits, clipPercent);
			saveWavData(outputWavFilePath, normalizedAudio, audioData.numChannels, audioData.numFrames, audioData.validBits,
					audioData.sampleRate);
			return repairedCount;
		} catch (IOException | WavFileException e) {
			e.printStackTrace();
		}
		return 0;
	}

	private static AudioData readWavAudio(String wavFilePath) throws IOException, WavFileException {
		WavFile wavFile = WavFile.openWavFile(new File(wavFilePath));
		try {
			AudioData audioData = new AudioData();
			audioData.numChannels = wavFile.getNumChannels();
			audioData.validBits = wavFile.getValidBits();
			audioData.sampleRate = wavFile.getSampleRate();

			long numFramesLong = wavFile.getNumFrames();
			if (numFramesLong > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Audio is too large for in-memory repair: " + wavFilePath);
			}
			int numFrames = (int) numFramesLong;
			long[][] samples = new long[audioData.numChannels][numFrames];

			int framesReadTotal = 0;
			while (framesReadTotal < numFrames) {
				int framesRead = wavFile.readFrames(samples, framesReadTotal, numFrames - framesReadTotal);
				if (framesRead == 0) {
					break;
				}
				framesReadTotal += framesRead;
			}

			audioData.samples = samples;
			audioData.numFrames = framesReadTotal;
			return audioData;
		} finally {
			wavFile.close();
		}
	}

	/**
	 * A click artifact is detected when a single sample polarity flips sharply
	 * against both neighboring samples (negative spike in positive context or
	 * positive spike in negative context).
	 */
	public static int repairNegativeClicks(long[] audio, int numFrames, long minSpikeAmplitude) {
		if (audio == null || numFrames < 3) {
			return 0;
		}
		int repairedCount = 0;
		for (int i = 1; i < numFrames - 1; i++) {
			long prev = audio[i - 1];
			long curr = audio[i];
			long next = audio[i + 1];

			boolean neighborsPositive = prev > 0 && next > 0 && curr < 0;
			boolean neighborsNegative = prev < 0 && next < 0 && curr > 0;
			if (!neighborsPositive && !neighborsNegative) {
				continue;
			}

			double absCurr = Math.abs((double) curr);
			if (absCurr < minSpikeAmplitude) {
				continue;
			}

			double neighborAverage = (Math.abs((double) prev) + Math.abs((double) next)) / 2.0;
			if (absCurr > neighborAverage) {
				audio[i] = Math.round((prev + next) / 2.0);
				repairedCount++;
			}
		}
		return repairedCount;
	}

	public static double[][] normalizeAndClip(long[][] audio, int numChannels, int numFrames, int bitsPerSample,
			double clipPercent) {
		double clipRatio = clipPercent / 100.0;
		if (clipRatio <= 0.0) {
			clipRatio = 0.9;
		}
		if (clipRatio > 1.0) {
			clipRatio = 1.0;
		}

		long absMax = getAbsMax(audio, numChannels, numFrames);
		double[][] normalized = new double[numChannels][numFrames];
		if (absMax <= 0) {
			return normalized;
		}

		double quantizationMax = WavFile.getQuantizationMax(bitsPerSample);
		double targetPeak = quantizationMax * clipRatio;
		double scale = absMax / targetPeak;

		for (int c = 0; c < numChannels; c++) {
			for (int i = 0; i < numFrames; i++) {
				normalized[c][i] = audio[c][i] / scale / quantizationMax;
			}
		}
		return normalized;
	}

	public static void saveWavData(String wavFilePath, double[][] wavData, int numChannels, int numFrames,
			int bitsPerSample, long samplingRate) throws IOException, WavFileException {
		new File(wavFilePath).getParentFile().mkdirs();
		WavFile writeWavFile = WavFile.newWavFile(new File(wavFilePath), numChannels, numFrames, bitsPerSample,
				samplingRate);
		writeWavFile.writeFrames(wavData, numFrames);
		writeWavFile.close();
	}

	public static long getAbsMax(long[][] data, int numChannels, int numFrames) {
		long max = 0;
		for (int c = 0; c < numChannels; c++) {
			for (int i = 0; i < numFrames; i++) {
				long value = Math.abs(data[c][i]);
				if (value > max) {
					max = value;
				}
			}
		}
		return Math.max(0, max - 1); // keep headroom to prevent clipping
	}
}
