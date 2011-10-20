package nz.org.nesi.goji.exceptions;

public class RequestException extends Exception {

	public RequestException() {
	}

	public RequestException(String message) {
		super(message);
	}

	public RequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public RequestException(Throwable cause) {
		super(cause);
	}

}
