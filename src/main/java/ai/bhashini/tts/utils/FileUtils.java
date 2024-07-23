package ai.bhashini.tts.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class FileUtils {
	public static String getFileNameWithoutExtension(File file, String extension) {
        String fileName = file.getName();
        return fileName.substring(0, fileName.lastIndexOf("." + extension));
    }

	public static String getFileNameWithoutExtension(String fileName) {
		int end = fileName.lastIndexOf('.');
		if (end == -1) {
			return fileName;
		}
		return fileName.substring(0, end);
    }

    public static String getFileContents(String filePath) {
        StringBuffer strBuffer = new StringBuffer();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                strBuffer.append(line + "\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strBuffer.toString().trim();
    }

    public static void createFileWithContents(String filePath, String fileContents) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(fileContents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyContents(InputStream in, OutputStream out) throws IOException {
		byte[] buf = new byte[4096];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
	}

	public static void copyFile(String sourceFilePath, String destinationFilePath) {
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(sourceFilePath));
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destinationFilePath))) {
			copyContents(in, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static File[] getSubDirs(File dataDir) {
		File[] subDirs = dataDir.listFiles(new FileFilter() {
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

	public static void loadTxtAndWavFilePaths(File dataDir, ArrayList<String> sentenceIds, String wavDirName,
			HashMap<String, String> txtFilePaths, HashMap<String, String> wavFilePaths) {
		HashSet<String> sentenceIdsMap = new HashSet<String>(sentenceIds);
		File[] subDirs = getSubDirs(dataDir);
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
				String nameWithoutExtension = getFileNameWithoutExtension(name);
				return sentenceIdsMap.contains(nameWithoutExtension);
			}
		});
		for (File file : files) {
			String sentenceId = getFileNameWithoutExtension(file.getName());
			filePaths.put(sentenceId, file.getAbsolutePath());
		}
	}

	public static File[] getWavFiles(String wavDirPath) {
		return getWavFiles(new File(wavDirPath));
	}

	public static File[] getWavFiles(File wavDir) {
		File[] wavFiles = wavDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.getName().endsWith(".wav") && !file.getName().startsWith("._");
			}
		});
		if (wavFiles != null) {
			Arrays.sort(wavFiles, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
		}
		return wavFiles;
	}
}
