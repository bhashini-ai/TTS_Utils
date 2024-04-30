package ai.bhashini.tts.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MergeRecordedScript {

	public static void main(String[] args) {
		String inputDirPath = args[0];
		String outputDirPath = args[1];
		String prefix = args[2];
		new File(outputDirPath).mkdirs();
		File scriptsDir = new File(inputDirPath, "script");
		ArrayList<String> categories = CreateSentenceLevelTextFiles.getScriptCategories(scriptsDir, ".txt");
		for (String category : categories) {
			String outputFilePath = new File(outputDirPath, category + ".txt").getAbsolutePath();
			System.out.println("Creating " + outputFilePath);
			createScript(inputDirPath, outputFilePath, prefix + "_" + category + "_");
		}
	}

	static void createScript(String inputDirPath, String outputFilePath, String prefix) {
		File[] subDirs = MatchWavAndTextFiles.getSubDirs(new File(inputDirPath));
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath))) {
			for (File subDir : subDirs) {
				File txtDir = new File(subDir, "txt");
				if (!txtDir.exists()) {
					continue;
				}
				File[] txtFiles = txtDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".txt");
					}
				});
				Arrays.sort(txtFiles);
				for (File txtFile : txtFiles) {
					String sentenceId = FileUtils.getFileNameWithoutExtension(txtFile, "txt");
					if (sentenceId.startsWith(prefix)) {
						bw.write(sentenceId + "\t" + FileUtils.getFileContents(txtFile.getAbsolutePath()) + "\n");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
