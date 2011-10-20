package nz.org.nesi.goji.exceptions;

public class InitException extends RuntimeException {

	public InitException() {
	}

	public InitException(String message) {
		super(message);
	}

	public InitException(String message, Throwable cause) {
		super(message, cause);
	}

	public InitException(Throwable cause) {
		super(cause);
	}

}
