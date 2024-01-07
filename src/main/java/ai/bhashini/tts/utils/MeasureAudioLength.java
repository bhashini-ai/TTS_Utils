package ai.bhashini.tts.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.cli.ParseException;

public class MeasureAudioLength {
	public static class Arguments extends CommandLineOptions {
		StringOption inputDir = new StringOption("in", "input-dir",
				"Input directory in which the length of each .wav file will be measured");
		BooleanOption recursive = new BooleanOption("r", "recursive",
				"Measure WAV files in each of the <child-dir>/wav directories");
		BooleanOption verbose = new BooleanOption("v", "verbose", "Also print the length of each .wav file");

		public Arguments() {
			super();
			inputDir.setRequired(true);
			options.addOption(inputDir);
			options.addOption(recursive);
			options.addOption(verbose);
		}
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(MeasureAudioLength.class.getCanonicalName());
			return;
		}
		String inputDir = arguments.inputDir.getStringValue();
		boolean recursive = arguments.recursive.getBoolValue();
		boolean verboseOutput = arguments.verbose.getBoolValue();

		if (!recursive) {
			printAudioLength(inputDir, verboseOutput);
		} else {
			File[] subDirs = new File(inputDir).listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.isDirectory();
				}
			});
			Arrays.sort(subDirs);
			for (File subDir : subDirs) {
				File wavDir = new File(subDir, "wav");
				if (wavDir.exists()) {
					printAudioLength(wavDir.getAbsolutePath(), verboseOutput);
				}
			}
		}
	}

	private static void printAudioLength(String inputDir, boolean verboseOutput) {
		SystemTime systemTime = new SystemTime(true);
		HashMap<String, Integer> samplingRates = new HashMap<>();
		File[] wavFiles = FileUtils.getWavFiles(inputDir);
		String padStr = "%0" + (wavFiles.length + "").length() + "d/" + wavFiles.length + ": ";
		double totalLengthInSecs = 0.0;
		for (int i = 0; i < wavFiles.length; i++) {
			File wavFile = wavFiles[i];
			double length = getAudioLengthInSecs(wavFile.getAbsolutePath(), samplingRates);
			if (verboseOutput) {
				System.out.println(String.format(padStr, i + 1) + wavFile.getAbsolutePath() + " = " + length);
			} else {
				// System.out.printf("\r%s", String.format(padStr, i + 1));
			}
			totalLengthInSecs += length;
		}
		System.out.println("\nWAV Dir = " + inputDir);
		System.out.println("Total length = " + SystemTime.toHrsMinsSecs(totalLengthInSecs));
		for (String samplingRateInfo : samplingRates.keySet()) {
			int count = samplingRates.get(samplingRateInfo);
			double percent = 100.0 * count / wavFiles.length;
			System.out.println(samplingRateInfo + " -> " + count + "/" + wavFiles.length + " ("
					+ String.format("%.2f", percent) + "%)");
		}
		systemTime.tock(true);
	}

	public static double getAudioLengthInSecs(String wavFilePath, HashMap<String, Integer> samplingRates) {
		try {
			WavFile readWavFile = WavFile.openWavFile(new File(wavFilePath));
			long sampleRate = readWavFile.getSampleRate();
			long numFrames = readWavFile.getNumFrames();
			int bitsPerSample = readWavFile.getValidBits();
			readWavFile.close();
			String samplingRateInfo = "SampleRate=" + sampleRate + ", BitsPerSample=" + bitsPerSample;
			if (samplingRates != null) {
				int count = 1;
				if (samplingRates.containsKey(samplingRateInfo)) {
					count += samplingRates.get(samplingRateInfo);
				}
				samplingRates.put(samplingRateInfo, count);
			}
			double lengthInSecs = 1.0 * numFrames / sampleRate;
			return lengthInSecs;
		} catch (WavFileException | IOException e) {
			System.err.println("Error while parsing " + wavFilePath);
			e.printStackTrace();
		}
		return 0.0;
	}
}
