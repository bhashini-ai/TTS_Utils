package ai.bhashini.tts.utils;

public class SystemTime {
	public static final double TEN_POWER_NINE = 1e+9;

	long cBegin;
	long cEnd;
	double timeInSecs;

	public SystemTime(boolean tick) {
		if (tick) {
			tick();
		}
	}

	public void tick() {
		cBegin = System.nanoTime();
	}

	public double tock(boolean print) {
		cEnd = System.nanoTime();
		timeInSecs = (cEnd - cBegin) / TEN_POWER_NINE;
		if (print) {
			System.out.println("Total compute time = " + timeInSecs + " seconds (" + toHrsMinsSecs(timeInSecs) + ")");
		}
		return timeInSecs;
	}

	public static String toHrsMinsSecs(double totalLengthInSecs) {
		int secs = (int) Math.round(totalLengthInSecs % 60);
		int totalMins = (int) (totalLengthInSecs / 60);
		int mins = totalMins % 60;
		int hrs = totalMins / 60;
		return String.format("%02d", hrs) + ":" + String.format("%02d", mins) + ":" + String.format("%02d", secs);
	}

	public static void main(String[] args) {
		System.out.println(toHrsMinsSecs(919.845355276));
	}
}
