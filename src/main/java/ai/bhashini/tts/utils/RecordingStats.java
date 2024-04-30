package ai.bhashini.tts.utils;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;

public class RecordingStats {
	Duration total = Duration.ofSeconds(0);
	HashMap<String, Duration> wavFilesAndDurations = new HashMap<String, Duration>();

	public void add(String wavPath, boolean modified) {
		Duration duration = getAudioDuration(wavPath, modified);
		if (wavFilesAndDurations.containsKey(wavPath)) {
			total = total.minus(wavFilesAndDurations.get(wavPath));
		}
		wavFilesAndDurations.put(wavPath, duration);
		total = total.plus(duration);
	}

	public void remove(String wavPath) {
		if (wavFilesAndDurations.containsKey(wavPath)) {
			total = total.minus(wavFilesAndDurations.get(wavPath));
			wavFilesAndDurations.remove(wavPath);
		}
	}

	public int getNumRecordings() {
		return wavFilesAndDurations.keySet().size();
	}

	private Duration getAudioDuration(String wavPath, boolean modified) {
		File wavFile = new File(wavPath);
		File normalizedWavFile = new File(wavFile.getParent() + "_normalized", wavFile.getName());
		if (!normalizedWavFile.exists() || modified) {
			TrimAndNormalizeAudio.trim(wavPath, normalizedWavFile.getAbsolutePath());
		}
		double secs = MeasureAudioLength.getAudioLengthInSecs(normalizedWavFile.getAbsolutePath());
		return Duration.ofMillis(Math.round(secs * 1000));
	}

	@Override
	public String toString() {
		return "#Sentences=" + wavFilesAndDurations.keySet().size() + "\tDuration=" + totalDurationAsString();
	}

	public String totalDurationAsString() {
		long s = total.toSeconds();
		return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
	}

	public void loadStats(String wavDir) {
		total = Duration.ofSeconds(0);
		wavFilesAndDurations.clear();
		File[] wavFiles = FileUtils.getWavFiles(wavDir);
		if (wavFiles != null) {
			for (File wavFile : wavFiles) {
				add(wavFile.getAbsolutePath(), false);
			}
		}
	}

	public void add(RecordingStats recordingStats) {
		for (String wavPath : recordingStats.wavFilesAndDurations.keySet()) {
			Duration duration = recordingStats.wavFilesAndDurations.get(wavPath);
			if (wavFilesAndDurations.containsKey(wavPath)) {
				total = total.minus(wavFilesAndDurations.get(wavPath));
			}
			wavFilesAndDurations.put(wavPath, duration);
			total = total.plus(duration);
		}
	}

	public static void main(String[] args) {
		File dataDir = new File(args[0]);
		RecordingStats totalRecordingStats = new RecordingStats();
		File[] subDirs = MatchWavAndTextFiles.getSubDirs(dataDir);
		System.out.println("WavDir:\t#Sentences\tDuration");
		int offset = dataDir.getAbsolutePath().length() + 1;
		for (File subDir : subDirs) {
			File wavDir = new File(subDir, "wav");
			if (wavDir.exists()) {
				RecordingStats recordingStats = new RecordingStats();
				recordingStats.loadStats(wavDir.getAbsolutePath());
				System.out.println(wavDir.getAbsolutePath().substring(offset) + ":\t"
						+ recordingStats.getNumRecordings() + "\t" + recordingStats.totalDurationAsString());
				totalRecordingStats.add(recordingStats);
			}
		}
		System.out.println("\nTotal: " + totalRecordingStats.toString());
	}
}