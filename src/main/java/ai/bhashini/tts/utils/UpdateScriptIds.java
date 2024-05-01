package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class UpdateScriptIds {

	public static void main(String[] args) {
		File inputScriptFilePath = new File(args[0]);
		System.out.println("Parsing script file " + inputScriptFilePath.getAbsolutePath());
		File outputScriptFilePath = new File(inputScriptFilePath.getParent(),
				inputScriptFilePath.getName().replace(".txt", "_new.txt"));
		String prefix = args[1];
		try (BufferedReader br = new BufferedReader(new FileReader(inputScriptFilePath));
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputScriptFilePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\t");
				if (contents.length < 2) {
					System.out.println("Skipping: " + line);
					continue;
				}
				String sentenceId = contents[0];
				if (sentenceId.matches("\\d+")) {
					sentenceId = String.format(prefix + "%05d", Integer.parseInt(sentenceId));
				}
				String sentenceText = contents[1];
				if (contents.length > 2) {
					sentenceText += "\n" + contents[2];
				}
				bw.write(sentenceId + "\t" + sentenceText + "\n");
			}
			System.out.println("Created new script file " + outputScriptFilePath.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
