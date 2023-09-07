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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

public class FileUtils {
	public static String getFileNameWithoutExtension(File file, String extension) {
        String fileName = file.getName();
        return fileName.substring(0, fileName.lastIndexOf("." + extension));
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

	public static File[] getWavFiles(String inputDir) {
		File[] wavFiles = new File(inputDir).listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.getName().endsWith(".wav") && !file.getName().startsWith("._");
			}
		});
		Arrays.sort(wavFiles, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return wavFiles;
	}
}
