package ai.bhashini.tts.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.cli.ParseException;

public class TrimAndNormalizeAudio {
	static int NEW_SAMPLING_RATE = 22050;
	static int NEW_BITS_PER_SAMPLE = 16;
	static int WINDOW_LENGTH = 1024;
	static int HOP_LENGTH = 256;
	static int CUTOFF_DB = -30;
	static int SILENCE_PADDING = 5;

	public static class Arguments extends CommandLineOptions {
		StringOption inputDir = new StringOption("in", "input-dir",
				"Input directory from which each .wav file will be processed");
		StringOption outputDir = new StringOption("out", "output-dir", "Output directory for processed .wav files");
		IntegerOption newSamplingRate = new IntegerOption("sr", "new-sampling-rate", NEW_SAMPLING_RATE,
				"Sampling rate for output audio");
		IntegerOption newBitsPerSample = new IntegerOption("bps", "new-bits-per-sample", NEW_BITS_PER_SAMPLE,
				"Bits per sample for output audio");
		IntegerOption windowLength = new IntegerOption("win", "window-length", WINDOW_LENGTH,
				"Length of overlapping window");
		IntegerOption hopLength = new IntegerOption("hop", "hop-length", HOP_LENGTH, "Hop length");
		IntegerOption cutoffDB = new IntegerOption("cut", "cutoff-db", CUTOFF_DB,
				"Silences at the beginning and the end below this will be cutoff");
		IntegerOption silencePadding = new IntegerOption("sil", "silence-padding", SILENCE_PADDING,
				"Amount of silence (hop-length * silence-padding) to be added at the beginning and end of audio after trimming");

		public Arguments() {
			super();
			inputDir.setRequired(true);
			outputDir.setRequired(true);
			options.addOption(inputDir);
			options.addOption(outputDir);
			options.addOption(newSamplingRate);
			options.addOption(newBitsPerSample);
			options.addOption(windowLength);
			options.addOption(hopLength);
			options.addOption(cutoffDB);
			options.addOption(silencePadding);
		}
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(TrimAndNormalizeAudio.class.getCanonicalName());
			return;
		}

		SystemTime sysTime = new SystemTime(true);
		File[] inputWavFiles = FileUtils.getWavFiles(arguments.inputDir.getStringValue());
		String inputDirName = new File(arguments.inputDir.getStringValue()).getName() + "/";
		String outputDirName = new File(arguments.outputDir.getStringValue()).getName() + "/";
		String padStr = "%0" + (inputWavFiles.length + "").length() + "d/" + inputWavFiles.length + ": ";
		for (int i = 0; i < inputWavFiles.length; i++) {
			File inputWavFile = inputWavFiles[i];
			File outputWavFile = new File(arguments.outputDir.getStringValue(), inputWavFile.getName());
			System.out.printf("\r%s", String.format(padStr, i + 1) + inputDirName + inputWavFile.getName() + " -> "
					+ outputDirName + outputWavFile.getName());
			trim(inputWavFile.getAbsolutePath(), outputWavFile.getAbsolutePath(),
					arguments.newSamplingRate.getIntValue(), arguments.newBitsPerSample.getIntValue(),
					arguments.windowLength.getIntValue(), arguments.hopLength.getIntValue(),
					arguments.cutoffDB.getIntValue(), arguments.silencePadding.getIntValue());
		}
		System.out.println("");
		sysTime.tock(true);
	}

	public static void convertToMonoUsingSOX(String inputWavFilePath, String outputWavFilePath, String soxInstallDir) {
		try {
			Runtime.getRuntime().exec(
					soxInstallDir + "sox.exe " + inputWavFilePath + " " + outputWavFilePath + " remix 1", null,
					new File(soxInstallDir));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void trim(String inputWavFilePath, String outputWavFilePath) {
		trim(inputWavFilePath, outputWavFilePath, NEW_SAMPLING_RATE, NEW_BITS_PER_SAMPLE, WINDOW_LENGTH, HOP_LENGTH,
				CUTOFF_DB, SILENCE_PADDING);
	}

	public static void trim(String inputWavFilePath, String outputWavFilePath, int newSamplingRate,
			int newBitsPerSample, int windowLength, int hopLength, int cutoffDB, int silencePadding) {
		try {
			double[] wavData = getResampledWavData(inputWavFilePath, newSamplingRate, newBitsPerSample);
			normalize(wavData);
			double[] trimmedData = OverlappingWindow.trimSilences(wavData, windowLength, hopLength, cutoffDB, silencePadding);
			saveWavData(outputWavFilePath, trimmedData, newSamplingRate, newBitsPerSample);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (WavFileException e) {
			e.printStackTrace();
		}
	}

	public static double[] getResampledWavData(String wavFilePath, int newSamplingRate, int newBitsPerSample)
			throws UnsupportedAudioFileException, IOException {
		AudioInputStream srcAudioInputStream = AudioSystem.getAudioInputStream(new File(wavFilePath));
		AudioFormat newFormat = new AudioFormat(newSamplingRate, newBitsPerSample, 1, true, true);
		AudioInputStream newAudioInputStream = AudioSystem.getAudioInputStream(newFormat, srcAudioInputStream);
		ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		while (newAudioInputStream.read(buffer) != -1) {
			dataStream.writeBytes(buffer);
		}
		double[] wavData = WavFile.audioBytesToDouble(dataStream.toByteArray(), newFormat.getFrameSize(),
				newBitsPerSample / 8, 1.0, newFormat.isBigEndian(), 0);
		return wavData;
	}

	public static void normalize(double[] wavData) {
		double max = getAbsMax(wavData);
		double mul = 0.9999 / max;
		for (int i = 0; i < wavData.length; i++) {
			wavData[i] *= mul;
		}
	}

	public static void saveWavData(String wavFilePath, double[] wavData, int samplingRate, int bitsPerSample)
			throws IOException, WavFileException {
		new File(wavFilePath).getParentFile().mkdirs();
		WavFile writeWavFile = WavFile.newWavFile(new File(wavFilePath), 1, wavData.length, bitsPerSample,
				samplingRate);
		writeWavFile.writeFrames(wavData, wavData.length);
		writeWavFile.close();
	}

	public static double getAbsMax(double[] data) {
		double max = 0.0;
		for (double d : data) {
			d = Math.abs(d);
			if (d > max) {
				max = d;
			}
		}
		return max;
	}

}
