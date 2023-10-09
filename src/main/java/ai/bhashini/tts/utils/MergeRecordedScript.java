package ai.bhashini.tts.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

public class MergeRecordedScript {

	public static void main(String[] args) {
		String inputDirPath = args[0];
		String outputDirPath = args[1];
		String prefix = args[2];
		new File(outputDirPath).mkdirs();
		String[] categories = { "agri", "education", "finance", "general", "health", "other", "politics", "weather", "iitm" };
		for (String category : categories) {
			String outputFilePath = new File(outputDirPath, category + ".txt").getAbsolutePath();
			System.out.println("Creating " + outputFilePath);
			createScript(inputDirPath, outputFilePath, prefix + "_" + category + "_");
		}
	}

	static void createScript(String inputDirPath, String outputFilePath, String prefix) {
		File[] subDirs = new File(inputDirPath).listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isDirectory() && !file.getName().equalsIgnoreCase("filelists")
						&& !file.getName().equalsIgnoreCase("wavs") && !file.getName().equalsIgnoreCase("evaluation");
			}
		});
		if (subDirs == null) {
			return;
		}
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
						int sentenceNumber = Integer.parseInt(sentenceId.substring(prefix.length()));
						bw.write(String.format("%04d", sentenceNumber) + "\t"
								+ FileUtils.getFileContents(txtFile.getAbsolutePath()) + "\n");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
