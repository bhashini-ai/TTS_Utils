package ai.bhashini.tts.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;

import org.apache.commons.cli.ParseException;

public class MatchWavAndTextFiles {
	public static class Arguments extends CommandLineOptions {
		StringOption inputDir = new StringOption("in", "input-dir",
				"Input directory in which each of <child-dir>/wav/*.wav and <child-dir>/txt/*.txt files will be matched");
		StringOption filenamePrefix = new StringOption("pre", "filename-prefix",
				"Only those txt/wav files containing this filename-prefix will be matched");
		BooleanOption removeUnmatched = new BooleanOption("rm", "remove-unmatched",
				"Each of the unmatched txt or wav files will be moved to <child-dir>/skipped directory");
		BooleanOption verbose = new BooleanOption("v", "verbose", "Print each matching pair");
		StringOption wavDirName = new StringOption("w", "wav-dir",
				"Name of the sub-directory within <child-dirs> for finding WAV files (default = wav)", "wav");

		public Arguments() {
			super();
			inputDir.setRequired(true);
			filenamePrefix.setRequired(true);
			options.addOption(inputDir);
			options.addOption(filenamePrefix);
			options.addOption(removeUnmatched);
			options.addOption(verbose);
			options.addOption(wavDirName);
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
		String filenamePrefix = arguments.filenamePrefix.getStringValue();
		boolean removeUnmatched = arguments.removeUnmatched.getBoolValue();
		boolean verboseOutput = arguments.verbose.getBoolValue();
		String wavDirName = arguments.wavDirName.getStringValue();

		File[] subDirs = getSubDirs(new File(inputDir));
		for (File subDir : subDirs) {
			System.out.println(subDir);
			File txtDir = new File(subDir, "txt");
			File wavDir = new File(subDir, wavDirName);
			File extrasDir = new File(subDir, "skipped");
			if (txtDir.exists() && wavDir.exists()) {
				match(wavDir, txtDir, extrasDir, filenamePrefix, ".wav", ".txt", removeUnmatched, verboseOutput);
				match(txtDir, wavDir, extrasDir, filenamePrefix, ".txt", ".wav", removeUnmatched, verboseOutput);
			}
		}
	}

	public static File[] getSubDirs(File inputDir) {
		File[] subDirs = inputDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isDirectory() && !file.getName().equalsIgnoreCase("filelists")
						&& !file.getName().equalsIgnoreCase("wavs") && !file.getName().equalsIgnoreCase("evaluation")
						&& !file.getName().equalsIgnoreCase("script");
			}
		});
		if (subDirs == null) {
			subDirs = new File[0];
		}
		if (subDirs.length > 1) {
			Arrays.sort(subDirs);
		}
		return subDirs;
	}

	static void match(File srcDir, File dstDir, File extrasDir, final String srcPrefix, final String srcExtn,
			String dstExtn, boolean removeUnmatched, boolean verboseOutput) {
		File[] srcFiles = srcDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(srcPrefix) && name.endsWith(srcExtn);
			}
		});
		Arrays.sort(srcFiles);
		for (File srcFile : srcFiles) {
			File dstFile = new File(dstDir, srcFile.getName().replace(srcExtn, dstExtn));
			if (!dstFile.exists()) {
				System.out.println("No " + dstExtn + " file for " + srcFile.getAbsolutePath());
				if (removeUnmatched) {
					if (!extrasDir.exists()) {
						extrasDir.mkdir();
					}
					srcFile.renameTo(new File(extrasDir, srcFile.getName()));
				}
			} else {
				if (verboseOutput) {
					System.out.println("\t" + srcFile.getName() + "->" + dstFile.getName());
				}
			}
		}
	}
}
