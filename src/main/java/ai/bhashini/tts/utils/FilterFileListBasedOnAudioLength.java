package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.cli.ParseException;

public class FilterFileListBasedOnAudioLength {

	public static class Arguments extends CommandLineOptions {
		StringOption audioTextFileList = new StringOption("filelist", "audio-text-filelist", "Path of audio-text file");
		StringOption datasetPath = new StringOption("dataset", "dataset-path",
				"Path of the directory containing wavs folder (which in turn has audio recordings)");
		IntegerOption minLengthInSecs = new IntegerOption("min", "min-length-in-secs", 1,
				"Minimum audio length in seconds");
		IntegerOption maxLengthInSecs = new IntegerOption("max", "max-length-in-secs", 30,
				"Maximum audio length in seconds");
		BooleanOption replaceOriginalFile = new BooleanOption("replace", "replace-original-file",
				"Replace original audio-text-file with the filtered one");

		public Arguments() {
			super();
			audioTextFileList.setRequired(true);
			datasetPath.setRequired(true);
			options.addOption(audioTextFileList);
			options.addOption(datasetPath);
			options.addOption(minLengthInSecs);
			options.addOption(maxLengthInSecs);
			options.addOption(replaceOriginalFile);
		}
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(FilterFileListBasedOnAudioLength.class.getCanonicalName());
			return;
		}
		String audioTextFileListPath = arguments.audioTextFileList.getStringValue();
		String datasetPath = arguments.datasetPath.getStringValue();
		int minLengthInSecs = arguments.minLengthInSecs.getIntValue();
		int maxLengtInSecs = arguments.maxLengthInSecs.getIntValue();
		boolean replaceOriginalFile = arguments.replaceOriginalFile.getBoolValue();
		if (!new File(audioTextFileListPath).exists()) {
			System.out.println(arguments.audioTextFileList + " does not exist.\nExiting.");
			return;
		}
		if (!new File(datasetPath).exists()) {
			System.out.println(arguments.datasetPath + " does not exist.\nExiting.");
			return;
		}

		String filteredFileListPath = audioTextFileListPath + ".filtered";
		String logFilePath = audioTextFileListPath + ".filtered.log";
		double totalLenShortAudios = 0.0;
		double totalLenLongAudios = 0.0;
		System.out.println("Filtering audios listed in " + audioTextFileListPath);
		try (BufferedReader br = new BufferedReader(new FileReader(audioTextFileListPath));
				BufferedWriter bw = new BufferedWriter(new FileWriter(filteredFileListPath));
				BufferedWriter log = new BufferedWriter(new FileWriter(logFilePath));) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\\|");
				String wavName = contents[0];
				if (!wavName.endsWith(".wav")) {
					wavName = wavName + ".wav";
				}
				if (!wavName.startsWith("wavs/")) {
					wavName = "wavs/" + wavName;
				}
				String wavFilePath = new File(datasetPath, wavName).getAbsolutePath();
				double lenInSecs = MeasureAudioLength.getAudioLengthInSecs(wavFilePath);
				if (lenInSecs >= minLengthInSecs && lenInSecs <= maxLengtInSecs) {
					bw.write(line + "\n");
				} else {
					if (lenInSecs < minLengthInSecs) {
						totalLenShortAudios += lenInSecs;
					} else {
						totalLenLongAudios += lenInSecs;
					}
					String logInfo = "Skipping: " + String.format("%.2f", lenInSecs) + " secs " + wavName + "\n";
					log.write(logInfo);
					System.out.print(logInfo);
				}
			}
			String logInfo = "Total length of shorter audios: " + String.format("%.2f secs", totalLenShortAudios) + "\n"
					+ "Total length of longer audios: " + String.format("%.2f secs", totalLenLongAudios) + "\n";
			log.write(logInfo);
			System.out.print(logInfo);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (replaceOriginalFile) {
			File backupFile = new File(audioTextFileListPath + ".bak");
			try {
				Files.move(new File(audioTextFileListPath).toPath(), backupFile.toPath(),
						StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Original filelist saved to " + backupFile.getAbsolutePath());
				Files.move(new File(filteredFileListPath).toPath(), new File(audioTextFileListPath).toPath(),
						StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Filtered filelist saved to " + audioTextFileListPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Filtered filelist saved to " + filteredFileListPath);
		}
		System.out.println("Log saved to " + logFilePath);
	}

}
