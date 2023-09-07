package ai.bhashini.tts.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

public class MatchWavAndTextFiles {

	public static void main(String[] args) {
		String baseDir = args[0];
		String prefix = args[1];
		File[] subDirs = new File(baseDir).listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isDirectory();
			}
		});
		for (File subDir : subDirs) {
			System.out.println(subDir);
			File txtDir = new File(subDir, "txt");
			File wavDir = new File(subDir, "wav");
			File extrasDir = new File(subDir, "skipped");
			if (txtDir.exists() && wavDir.exists()) {
				match(wavDir, txtDir, extrasDir, prefix, ".wav", ".txt");
				match(txtDir, wavDir, extrasDir, prefix, ".txt", ".wav");
			}
		}
	}

	static void match(File srcDir, File dstDir, File extrasDir, final String srcPrefix, final String srcExtn,
			String dstExtn) {
		File[] srcFiles = srcDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(srcPrefix) && name.endsWith(srcExtn);
			}
		});
		for (File srcFile : srcFiles) {
			File dstFile = new File(dstDir, srcFile.getName().replace(srcExtn, dstExtn));
			if (!dstFile.exists()) {
				System.out.println("No " + dstExtn + " file for " + srcFile.getAbsolutePath());
				if (!extrasDir.exists()) {
					extrasDir.mkdir();
				}
				srcFile.renameTo(new File(extrasDir, srcFile.getName()));
			} else {
				System.out.println("\t" + srcFile.getName() + "->" + dstFile.getName());
			}
		}
	}
}
