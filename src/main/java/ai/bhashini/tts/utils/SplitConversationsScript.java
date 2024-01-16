package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SplitConversationsScript {

	public static void main(String[] args) {
		processAndSplitScript(new File("C:/TTS_Speech_Recording_AI4Bharat/Bodo_Conv.tsv"));
	}

	static void processAndSplitScript(File origFile) {
		File parentDir = origFile.getParentFile();
		String fileName = origFile.getName().substring(0, origFile.getName().lastIndexOf('.'));
		File updatedFile = new File(parentDir, fileName + "_Updated.tsv");
		File maleFile = new File(parentDir, fileName + "_Male.tsv");
		File femaleFile = new File(parentDir, fileName + "_Female.tsv");
		System.out.println("Loading " + origFile + " ...");
		try (BufferedReader br = new BufferedReader(new FileReader(origFile));
				BufferedWriter bwUpdated = new BufferedWriter(new FileWriter(updatedFile));
				BufferedWriter bwMale = new BufferedWriter(new FileWriter(maleFile));
				BufferedWriter bwFemale = new BufferedWriter(new FileWriter(femaleFile))) {
			String line = br.readLine(); // ignore header line
			bwUpdated.write("ID\tSentence\tEnglish\n");
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\t");
				if (contents.length != 4) {
					System.out.println("Skipping line: " + line);
					continue;
				}
				String id = contents[0];
				int speakerTurn = Integer.parseInt(contents[1]);
				String sentenceText = contents[2];
				String englishText = contents[3];
				if (speakerTurn == 2) {
					// Male
					id = id.replace("_F_", "_M_");
					sentenceText = sentenceText.replace("सार", "मेडाम");
					englishText = englishText.replace("sir", "madam").replace("Sir", "Madam");
					bwMale.write(id + "\t" + sentenceText + "\t" + englishText + "\n");
				} else {
					// Female
					id = id.replace("_M_", "_F_");
					sentenceText = sentenceText.replace("मेडाम", "सार");
					englishText = englishText.replace("madam", "sir").replace("Madam", "Sir");
					bwFemale.write(id + "\t" + sentenceText + "\t" + englishText + "\n");
				}
				bwUpdated.write(id + "\t" + sentenceText + "\t" + englishText + "\n");
			}
			System.out.println("Created " + updatedFile.getAbsolutePath());
			System.out.println("Created " + femaleFile.getAbsolutePath());
			System.out.println("Created " + maleFile.getAbsolutePath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
