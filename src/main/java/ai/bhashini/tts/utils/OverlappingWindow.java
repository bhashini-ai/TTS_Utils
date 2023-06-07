package ai.bhashini.tts.utils;

import java.util.ArrayList;

public class OverlappingWindow {
    double[] data;
    int frameBegin;
    int frameEnd;
    int winBegin;
    int winEnd;
    int windowLength;
    Double meanSquare = null;
    Double powerDB = null;

    public OverlappingWindow(double[] data, int frameBegin, int frameEnd, int windowLength, int hopLength) {
        this.data = data;
        this.frameBegin = frameBegin;
        this.frameEnd = frameEnd;
        this.windowLength = windowLength;
        int offset = (windowLength - hopLength) / 2;
        this.winBegin = frameBegin - offset;
        this.winEnd = frameEnd + offset;
    }

    @Override
    public String toString() {
        String winInterval = "[" + winBegin + ", " + (winEnd - 1) + "]";
        String frameInterval = "[" + frameBegin + ", " + (frameEnd - 1) + "]";
        return winInterval + "\t" + frameInterval;
    }

    public static ArrayList<OverlappingWindow> getOverlappingWindows(double[] wavData, int windowLength,
            int hopLength) {
        ArrayList<OverlappingWindow> overlappingWindows = new ArrayList<>();
        int frameBegin = -hopLength / 2;
        boolean stop = false;
        do {
            int frameEnd = frameBegin + hopLength;
            if (frameEnd >= wavData.length) {
                // last frame
                stop = true;
            }
            overlappingWindows.add(new OverlappingWindow(wavData, frameBegin, frameEnd, windowLength, hopLength));
            frameBegin = frameEnd;
        } while (!stop);
        return overlappingWindows;
    }

    public double computeMeanSquare() {
        double sum = 0.0;
        for (int i = winBegin; i < winEnd; i++) {
            if (i >= 0 && i < data.length) {
                sum += Math.pow(data[i], 2);
            }
        }
        meanSquare = sum / windowLength;
        return meanSquare;
    }

    public double[] getWindowData() {
        double[] windowData = new double[windowLength];
        for (int i = 0; i < windowLength; i++) {
            int index = winBegin + i;
            if (index >= 0 && index < data.length) {
                windowData[i] = data[index];
            }
        }
        return windowData;
    }

    public void copyWindowData(double[] newData, double[] windowData) {
        for (int i = 0; i < windowLength; i++) {
            int index = winBegin + i;
            if (index >= 0 && index < data.length) {
                newData[index] = windowData[i];
            }
        }
    }

    public void computePowerDB() {
        if (meanSquare == null) {
            computeMeanSquare();
        }
        powerDB = convertPowerToDB(meanSquare);
    }

    public void subtractRefDB(double topDB) {
        powerDB -= topDB;
    }

    public static double MIN_POWER = 1e-10;

    public static double convertPowerToDB(double p) {
        return 10 * Math.log10(Math.max(MIN_POWER, p));
    }

    public static double[] trimSilences(double[] wavData, int windowLength, int hopLength, double cutoffDB,
            int silencePadding) {
        // Reference: https://github.com/librosa/librosa/blob/main/librosa/effects.py
        ArrayList<OverlappingWindow> overlappingWindows = OverlappingWindow.getOverlappingWindows(wavData, windowLength,
                hopLength);
        double maxP = 0.0;
        for (OverlappingWindow win : overlappingWindows) {
            double p = win.computeMeanSquare();
            if (p > maxP) {
                maxP = p;
            }
            win.computePowerDB();
        }
        double refDB = OverlappingWindow.convertPowerToDB(maxP);
        for (OverlappingWindow win : overlappingWindows) {
            win.subtractRefDB(refDB);
            // System.out.println(win.powerDB);
        }
        int trimBegin = 0;
        for (int i = 0; i < overlappingWindows.size(); i++) {
            OverlappingWindow win = overlappingWindows.get(i);
            if (win.powerDB > cutoffDB) {
                trimBegin = Math.max(0, win.frameBegin);
                break;
            }
        }
        int trimEnd = wavData.length;
        for (int i = overlappingWindows.size() - 1; i > 0; i--) {
            OverlappingWindow win = overlappingWindows.get(i);
            if (win.powerDB > cutoffDB) {
                trimEnd = Math.min(wavData.length, win.frameEnd);
                break;
            }
        }
        double[] trimmedData;
        if (trimBegin > 0 || trimEnd < wavData.length || silencePadding != 0) {
            int silenceLength = silencePadding * hopLength;
            trimmedData = new double[trimEnd - trimBegin + silenceLength * 2];
            for (int i = trimBegin; i < trimEnd; i++) {
                trimmedData[silenceLength + i - trimBegin] = wavData[i];
            }
        } else {
            trimmedData = wavData;
        }
        return trimmedData;
    }
}
