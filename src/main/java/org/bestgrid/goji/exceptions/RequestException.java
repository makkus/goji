package org.bestgrid.goji.exceptions;

public class RequestException extends RuntimeException {

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
