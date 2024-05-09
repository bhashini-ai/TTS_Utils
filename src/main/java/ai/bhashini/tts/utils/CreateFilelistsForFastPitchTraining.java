package ai.bhashini.tts.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.cli.ParseException;

public class CreateFilelistsForFastPitchTraining {
	ArrayList<String> wavFileNames = new ArrayList<>();
	HashMap<String, String> wavTextMapping = new HashMap<String, String>();

	public static class Arguments extends CommandLineOptions {
		StringOption audioTextFile = new StringOption("filelist", "audio-text-file", "Path of audio-text file");
		StringOption recordingsDir = new StringOption("dir", "recordings-dir",
				"Path of directory containing date-wise audio recordings");

		public Arguments() {
			super();
			options.addOption(audioTextFile);
			options.addOption(recordingsDir);
		}
	}

	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		try {
			arguments.parse(args);
			arguments.printValues();
		} catch (ParseException e) {
			e.printStackTrace();
			arguments.printHelp(CreateFilelistsForFastPitchTraining.class.getCanonicalName());
			return;
		}
		String audioTextFilePath = arguments.audioTextFile.getStringValue();
		String recordingsDirPath = arguments.recordingsDir.getStringValue();
		if (audioTextFilePath == null && recordingsDirPath == null) {
			arguments.printHelp(CreateFilelistsForFastPitchTraining.class.getCanonicalName());
			System.out.println("Specify one of the options: --" + arguments.audioTextFile.getLongOpt() + " or --"
					+ arguments.recordingsDir.getLongOpt());
			return;
		}

		if (audioTextFilePath != null) {
			File audioTextFile = new File(audioTextFilePath);
			if (!audioTextFile.exists()) {
				System.out.println("Error: " + arguments.audioTextFile + " does not exist." + "\nExiting.");
				return;
			}
			System.out.println("Processing " + arguments.audioTextFile);
			CreateFilelistsForFastPitchTraining createFilelistsForTraining = new CreateFilelistsForFastPitchTraining();
			createFilelistsForTraining.loadAudioTextFile(audioTextFilePath, null);
			createFilelistsForTraining.createFilelists(new File(audioTextFile.getParentFile(), "filelists"));
		} else {
			File recordingsDir = new File(recordingsDirPath);
			if (!recordingsDir.exists()) {
				System.out.println("Error: " + arguments.recordingsDir + " does not exist." + "\nExiting.");
				return;
			}
			System.out.println("Processing " + arguments.recordingsDir);
			CreateFilelistsForFastPitchTraining createFilelistsForTraining = new CreateFilelistsForFastPitchTraining();
			createFilelistsForTraining.loadWavTextMapping(recordingsDir);
			System.out.println("Found " + createFilelistsForTraining.wavFileNames.size()
					+ " audio recordings and corresponding transcripts");
			createFilelistsForTraining.createFilelists(new File(recordingsDir, "filelists"));
		}
	}

	public void loadAudioTextFile(String filePath, Integer speakerId) {
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] contents = line.split("\\|");
				if (contents.length != 2) {
					System.out.println("Incorrect line: " + line);
					continue;
				}
				String wavName = contents[0].replaceAll("wavs/", "");
				String textAndSpeakerId = contents[1];
				if (speakerId != null) {
					textAndSpeakerId += "|" + speakerId;
				}
				wavFileNames.add(wavName);
				wavTextMapping.put(wavName, textAndSpeakerId);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadWavTextMapping(File dataDir) {
		File[] subDirs = MatchWavAndTextFiles.getSubDirs(dataDir);
		for (File subDir : subDirs) {
			File wavDir = new File(subDir, "wav");
			File txtDir = new File(subDir, "txt");
			if (wavDir.exists() && txtDir.exists()) {
				File[] wavFiles = wavDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".wav") && !name.startsWith("._");
					}
				});
				for (File wavFile : wavFiles) {
					String wavFileName = wavFile.getName();
					File txtFile = new File(txtDir, wavFileName.replace(".wav", ".txt"));
					if (txtFile.exists()) {
						String txt = FileUtils.getFileContents(txtFile.getAbsolutePath()).replaceAll("\n", " ").trim();
						if (txt.trim().isEmpty()) {
							System.out.println("Skipping empty file: " + txtFile.getAbsolutePath());
							continue;
						}
						if (wavTextMapping.containsKey(wavFileName)) {
							System.out.println("Duplicate entry found for: " + wavFile.getAbsolutePath());
						} else {
							wavFileNames.add(wavFileName);
							wavTextMapping.put(wavFileName, txt);
						}
					} else {
						System.out.println("No matching txt file found for " + wavFile.getAbsolutePath());
					}
				}
			}
		}
	}

	public void createFilelists(File outputDir) {
		outputDir.mkdir();

		Collections.sort(wavFileNames);
		createAudioPitchFilelists(new File(outputDir, "audio_text_full.txt"),
				new File(outputDir, "audio_pitch_text_full.txt"), 0, wavFileNames.size());

		Collections.shuffle(wavFileNames);
		int trainEnd = (int) Math.floor(wavFileNames.size() * 0.95);
		createFilelists(new File(outputDir, "audio_text_train.txt"), new File(outputDir, "mel_text_train.txt"),
				new File(outputDir, "audio_pitch_text_train.txt"), new File(outputDir, "hifigan_audio_text_train.txt"),
				0, trainEnd);
		int valEnd = (int) Math.floor(wavFileNames.size() * 0.99);
		createFilelists(new File(outputDir, "audio_text_val.txt"), new File(outputDir, "mel_text_val.txt"),
				new File(outputDir, "audio_pitch_text_val.txt"), new File(outputDir, "hifigan_audio_text_val.txt"),
				trainEnd, valEnd);
		createAudioMelFilelists(new File(outputDir, "audio_text_test.txt"), new File(outputDir, "mel_text_test.txt"),
				valEnd, wavFileNames.size());
	}

	private void createAudioPitchFilelists(File audioTextFilelist, File audioPitchTextFilelist, int start, int end) {
		try (BufferedWriter bwAudioText = new BufferedWriter(new FileWriter(audioTextFilelist));
				BufferedWriter bwAudioPitchText = new BufferedWriter(new FileWriter(audioPitchTextFilelist));) {
			createFilelists(bwAudioText, null, bwAudioPitchText, null, start, end);
			System.out.println("Created " + audioTextFilelist.getAbsolutePath());
			System.out.println("Created " + audioPitchTextFilelist.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createAudioMelFilelists(File audioTextFilelist, File melTextFilelist, int start, int end) {
		try (BufferedWriter bwAudioText = new BufferedWriter(new FileWriter(audioTextFilelist));
				BufferedWriter bwMelText = new BufferedWriter(new FileWriter(melTextFilelist));) {
			createFilelists(bwAudioText, bwMelText, null, null, start, end);
			System.out.println("Created " + audioTextFilelist.getAbsolutePath());
			System.out.println("Created " + melTextFilelist.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createFilelists(File audioTextFilelist, File melTextFilelist, File audioPitchTextFilelist,
			File hifiganAudioTextFilelist, int start, int end) {
		try (BufferedWriter bwAudioText = new BufferedWriter(new FileWriter(audioTextFilelist));
				BufferedWriter bwMelText = new BufferedWriter(new FileWriter(melTextFilelist));
				BufferedWriter bwAudioPitchText = new BufferedWriter(new FileWriter(audioPitchTextFilelist));
				BufferedWriter bwHiFiGANAudioText = new BufferedWriter(new FileWriter(hifiganAudioTextFilelist));) {
			createFilelists(bwAudioText, bwMelText, bwAudioPitchText, bwHiFiGANAudioText, start, end);
			System.out.println("Created " + audioTextFilelist.getAbsolutePath());
			System.out.println("Created " + melTextFilelist.getAbsolutePath());
			System.out.println("Created " + audioPitchTextFilelist.getAbsolutePath());
			System.out.println("Created " + hifiganAudioTextFilelist.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createFilelists(BufferedWriter bwAudioText, BufferedWriter bwMelText, BufferedWriter bwAudioPitchText,
			BufferedWriter bwHiFiGANAudioText, int start, int end) throws IOException {
		for (int i = start; i < end; i++) {
			String wavFileName = wavFileNames.get(i);
			String text = wavTextMapping.get(wavFileName);
			String ptFileName = wavFileName.replace(".wav", ".pt");
			bwAudioText.write("wavs/" + wavFileName + "|" + text + "\n");
			if (bwMelText != null) {
				bwMelText.write("mels/" + ptFileName + "|" + text + "\n");
			}
			if (bwAudioPitchText != null) {
				bwAudioPitchText.write("wavs/" + wavFileName + "|" + "pitch/" + ptFileName + "|" + text + "\n");
			}
			if (bwHiFiGANAudioText != null) {
				bwHiFiGANAudioText.write(wavFileName.substring(0, wavFileName.indexOf(".wav")) + "|" + text + "\n");
			}
		}
	}
}
