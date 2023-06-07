package ai.bhashini.tts.utils;

public class WavFileException extends Exception {
	private static final long serialVersionUID = -4689948440660122503L;

	public WavFileException() {
		super();
	}

	public WavFileException(String message) {
		super(message);
	}

	public WavFileException(String message, Throwable cause) {
		super(message, cause);
	}

	public WavFileException(Throwable cause) {
		super(cause);
	}
}
