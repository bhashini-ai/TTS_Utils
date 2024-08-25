package ai.bhashini.tts.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CombineWAVs {

    private static void copyWavData(WavFile readWavFile, WavFile writeWavFile, int numChannels)
            throws IOException, WavFileException {
        final int BUF_SIZE = 5001;
        int[] buffer = new int[BUF_SIZE * numChannels];
//		long[] buffer = new long[BUF_SIZE * numChannels];
//      double[] buffer = new double[BUF_SIZE * numChannels];
        int framesRead = 0;
        do {
            framesRead = readWavFile.readFrames(buffer, BUF_SIZE);
            writeWavFile.writeFrames(buffer, framesRead);
        } while (framesRead != 0);
    }

    private static void addGap(WavFile writeWavFile, int gapToBeInsertedInMilliSecs)
            throws IOException, WavFileException {
        int numFrames = (int) writeWavFile.getSampleRate() * gapToBeInsertedInMilliSecs / 1000;
        int[] buffer = new int[numFrames * writeWavFile.getNumChannels()];
        writeWavFile.writeFrames(buffer, numFrames);
    }

    public static File combine(ArrayList<File> files, int gapToBeInsertedInMilliSecs) {
		if (files.size() == 0) {
			return null;
		}
        if (files.size() == 1) {
            return files.get(0);
        }
        try {
            ArrayList<WavFile> wavFiles = new ArrayList<>();
            long sampleRate = 0;
            long numFrames = 0;
            int numChannels = 0;
            int validBits = 0;
            File combinedFile = null;
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                WavFile wavFile = WavFile.openWavFile(file);
                wavFiles.add(wavFile);
                if (i == 0) {
                    sampleRate = wavFile.getSampleRate();
                    numFrames = wavFile.getNumFrames();
                    numChannels = wavFile.getNumChannels();
                    validBits = wavFile.getValidBits();
                    combinedFile = new File(file.getParent(), file.getName().replace(".wav", "_combined.wav"));
                } else {
                    numFrames += wavFile.getNumFrames();
                }
                if (i < files.size() - 1) {
                    numFrames += sampleRate * gapToBeInsertedInMilliSecs / 1000;
                }
            }
            WavFile writeWavFile = WavFile.newWavFile(combinedFile, numChannels, numFrames, validBits, sampleRate);

            for (int i = 0; i < wavFiles.size(); i++) {
                WavFile readWavFile = wavFiles.get(i);
                copyWavData(readWavFile, writeWavFile, numChannels);
                readWavFile.close();
                if (i < files.size() - 1) {
                    addGap(writeWavFile, gapToBeInsertedInMilliSecs);
                }
            }
            writeWavFile.close();
            return combinedFile;
        } catch (IOException | WavFileException e) {
            e.printStackTrace();
        }
        return files.get(0);
    }

	public static File padSilenceAtTheEnd(File file, int gapToBeInsertedInMilliSecs) {
		try {
			WavFile wavFile = WavFile.openWavFile(file);
			long sampleRate = wavFile.getSampleRate();
			long numFrames = wavFile.getNumFrames();
			int validBits = wavFile.getValidBits();
			int numChannels = wavFile.getNumChannels();
			numFrames += sampleRate * gapToBeInsertedInMilliSecs / 1000;

			File newFile = new File(file.getParent(), file.getName().replace(".wav", "_padded.wav"));
			WavFile writeWavFile = WavFile.newWavFile(newFile, numChannels, numFrames, validBits, sampleRate);
			copyWavData(wavFile, writeWavFile, numChannels);
			wavFile.close();
			addGap(writeWavFile, gapToBeInsertedInMilliSecs);
			writeWavFile.close();
			return newFile;
		} catch (IOException | WavFileException e) {
			e.printStackTrace();
			return file;
		}
	}

    public static void main(String[] args) {
        ArrayList<File> wavFiles = new ArrayList<>();
        wavFiles.add(new File("D:\\Temp\\SynthesizedSpeech\\hi_5a.wav"));
        wavFiles.add(new File("D:\\Temp\\SynthesizedSpeech\\hi_5b.wav"));
        wavFiles.add(new File("D:\\Temp\\SynthesizedSpeech\\hi_5c.wav"));
        combine(wavFiles, 330);
    }

}
