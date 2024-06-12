package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class CopySpecifiedRecordings {

	public static void main(String[] args) {
		File recordingsDir = new File(args[0]);
		File outputDir = new File(recordingsDir, "temp");
		File outputTxtDir = new File(outputDir, "txt");
		File outputWavDir = new File(outputDir, "wav");
		outputTxtDir.mkdirs();
		outputWavDir.mkdirs();
		File sentenceIdsFile = new File(args[1]);
		System.out.println("Parsing sentenceIds in " + sentenceIdsFile.getAbsolutePath());
		ArrayList<String> sentenceIds = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(sentenceIdsFile))) {
			String line = br.readLine(); // ignore header
			while ((line = br.readLine()) != null) {
				String sentenceId = line.split(",")[0];
				sentenceIds.add(sentenceId);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		HashMap<String, String> txtFilePaths = new HashMap<String, String>();
		HashMap<String, String> wavFilePaths = new HashMap<String, String>();
		System.out.println("Parsing txt and wav files in " + recordingsDir.getAbsolutePath());
		loadTxtAndWavFilePaths(recordingsDir, sentenceIds, "wav", txtFilePaths, wavFilePaths);
		System.out.println("# of matching txt files found = " + txtFilePaths.size());
		System.out.println("# of matching wav files found = " + wavFilePaths.size() + "\n");
		for (String sentenceId: sentenceIds) {
			if (txtFilePaths.containsKey(sentenceId) && wavFilePaths.containsKey(sentenceId)) {
				try {
					File srcTxtFile = new File(txtFilePaths.get(sentenceId));
					File dstTxtFile = new File(outputTxtDir, sentenceId + ".txt");
					Files.copy(srcTxtFile.toPath(), dstTxtFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					System.out.println("Copied " + srcTxtFile + " -> " + dstTxtFile);

					File srcWavFile = new File(wavFilePaths.get(sentenceId));
					File dstWavFile = new File(outputWavDir, sentenceId + ".wav");
					Files.copy(srcWavFile.toPath(), dstWavFile.toPath(),  StandardCopyOption.REPLACE_EXISTING);
					System.out.println("Copied " + srcWavFile + " -> " + dstWavFile);
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

	public static void loadTxtAndWavFilePaths(File dataDir, ArrayList<String> sentenceIds, String wavDirName,
			HashMap<String, String> txtFilePaths, HashMap<String, String> wavFilePaths) {
		HashSet<String> sentenceIdsMap = new HashSet<String>(sentenceIds);
		File[] subDirs = MatchWavAndTextFiles.getSubDirs(dataDir);
		for (File subDir : subDirs) {
			File txtDir = new File(subDir, "txt");
			if (txtDir.exists()) {
				loadFilePaths(txtDir, sentenceIdsMap, txtFilePaths, "txt");
			}
			File wavDir = new File(subDir, wavDirName);
			if (wavDir.exists()) {
				loadFilePaths(wavDir, sentenceIdsMap, wavFilePaths, wavDirName);
			}
		}
	}

	public static void loadFilePaths(File baseDir, HashSet<String> sentenceIdsMap, HashMap<String, String> filePaths,
			String extension) {
		File[] files = baseDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				String nameWithoutExtension = FileUtils.getFileNameWithoutExtension(name);
				return sentenceIdsMap.contains(nameWithoutExtension);
			}
		});
		for (File file : files) {
			String sentenceId = FileUtils.getFileNameWithoutExtension(file.getName());
			filePaths.put(sentenceId, file.getAbsolutePath());
		}
	}

}
