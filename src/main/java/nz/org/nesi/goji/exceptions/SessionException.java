package nz.org.nesi.goji.exceptions;

public class SessionException extends RuntimeException {

	public SessionException(String msg) {
		super(msg);
	}

	public SessionException(String msg, Exception e) {
		super(msg, e);
	}

}
