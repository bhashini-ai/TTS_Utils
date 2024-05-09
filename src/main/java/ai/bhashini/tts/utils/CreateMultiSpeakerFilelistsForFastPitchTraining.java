package ai.bhashini.tts.utils;

import java.io.File;

import org.apache.commons.cli.ParseException;

public class CreateMultiSpeakerFilelistsForFastPitchTraining {

	public static class Arguments extends CommandLineOptions {
		StringOption inputDir = new StringOption("dir", "filelists-directory",
				"Path of directory containing audio-text filelists for each speaker");
		IntegerOption numSpeakers = new IntegerOption("ns", "num-speakers", 1, "Total number of speakers");

		public Arguments() {
			super();
			inputDir.setRequired(true);
			numSpeakers.setRequired(true);
			options.addOption(inputDir);
			options.addOption(numSpeakers);
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
		int numSpeakers = arguments.numSpeakers.getIntValue();

		CreateFilelistsForFastPitchTraining createFilelistsForTraining = new CreateFilelistsForFastPitchTraining();
		for (int s = 0; s < numSpeakers; s++) {
			String filelistPath = inputDir + "/" + s + ".txt";
			System.out.println("Processing speaker " + s + " filelist: " + filelistPath);
			createFilelistsForTraining.loadAudioTextFile(filelistPath, s);
		}
		createFilelistsForTraining.createFilelists(new File(inputDir, "filelists"));
	}
}
