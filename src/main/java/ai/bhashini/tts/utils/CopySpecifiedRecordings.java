package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.cli.ParseException;

public class CopySpecifiedRecordings {
	public static ArrayList<String> loadSentenceIds(File sentenceIdsFile) {
		ArrayList<String> sentenceIds = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(sentenceIdsFile))) {
			String line = br.readLine(); // ignore header
			while ((line = br.readLine()) != null) {
				String sentenceId = line.split("\t")[0];
				sentenceIds.add(sentenceId);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sentenceIds;
	}

	public static void copyTextAndWavFiles(ArrayList<String> sentenceIds, HashMap<String, String> txtFilePaths,
			HashMap<String, String> wavFilePaths, File outputTxtDir, File outputWavDir, boolean skipCopyingWavFiles) {
		for (String sentenceId : sentenceIds) {
			if (txtFilePaths.containsKey(sentenceId) && wavFilePaths.containsKey(sentenceId)) {
				try {
					File srcTxtFile = new File(txtFilePaths.get(sentenceId));
					File dstTxtFile = new File(outputTxtDir, sentenceId + ".txt");
					Files.copy(srcTxtFile.toPath(), dstTxtFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					System.out.println("Copied " + srcTxtFile + " -> " + dstTxtFile);

					if (!skipCopyingWavFiles) {
						File srcWavFile = new File(wavFilePaths.get(sentenceId));
						File dstWavFile = new File(outputWavDir, sentenceId + ".wav");
						Files.copy(srcWavFile.toPath(), dstWavFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						System.out.println("Copied " + srcWavFile + " -> " + dstWavFile);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				if (!txtFilePaths.containsKey(sentenceId)) {
					System.out.println("Missing txt file for " + sentenceId);
				}
				if (!wavFilePaths.containsKey(sentenceId)) {
					System.out.println("Missing wav file for " + sentenceId);
				}
			}
		}
	}

	public static class Arguments extends CommandLineOptions {
		String defaultOutputDirName = "temp";

		StringOption dataDir = new StringOption("dir", "data-dir",
				"Directory containing recordings (<child-dir>/wav/*.wav) and their transcripts (<child-dir>/txt/*.txt)");
		StringOption tsvFilePath = new StringOption("tsv", "tsv-filepath",
				"Path of TSV file containing sentence-IDs of interest.");
		StringOption outputDirName = new StringOption("out", "output-dir-name",
				"Name of output directory where the specified recordings will be saved (default = '"
						+ defaultOutputDirName + "')",
				defaultOutputDirName);
		BooleanOption skipCopyingWavFiles = new BooleanOption("sw", "skip-wav", "Skip copying audio files");

		public Arguments() {
			super();
			dataDir.setRequired(true);
			tsvFilePath.setRequired(true);
			options.addOption(dataDir);
			options.addOption(tsvFilePath);
			options.addOption(outputDirName);
			options.addOption(skipCopyingWavFiles);
		}
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(CopySpecifiedRecordings.class.getCanonicalName());
			return;
		}
		String dataDirPath = arguments.dataDir.getStringValue();
		String tsvFilePath = arguments.tsvFilePath.getStringValue();
		String outputDirName = arguments.outputDirName.getStringValue();
		boolean skipCopyingWavFiles = arguments.skipCopyingWavFiles.getBoolValue();

		File dataDir = new File(dataDirPath);
		File sentenceIdsFile = new File(tsvFilePath);
		File outputDir = new File(dataDir, outputDirName);

		System.out.println("Parsing sentenceIds in " + sentenceIdsFile.getAbsolutePath());
		ArrayList<String> sentenceIds = loadSentenceIds(sentenceIdsFile);

		HashMap<String, String> txtFilePaths = new HashMap<String, String>();
		HashMap<String, String> wavFilePaths = new HashMap<String, String>();
		System.out.println("Parsing txt and wav files in " + dataDir.getAbsolutePath());
		FileUtils.loadTxtAndWavFilePaths(dataDir, sentenceIds, "wav", txtFilePaths, wavFilePaths);

		File outputTxtDir = new File(outputDir, "txt");
		File outputWavDir = new File(outputDir, "wav");
		outputTxtDir.mkdirs();
		outputWavDir.mkdirs();
		System.out.println("# of matching txt files found = " + txtFilePaths.size());
		System.out.println("# of matching wav files found = " + wavFilePaths.size() + "\n");
		copyTextAndWavFiles(sentenceIds, txtFilePaths, wavFilePaths, outputTxtDir, outputWavDir, skipCopyingWavFiles);
	}
}
