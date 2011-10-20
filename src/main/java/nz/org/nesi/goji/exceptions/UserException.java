package nz.org.nesi.goji.exceptions;

public class UserException extends Exception {

	public UserException() {
	}

	public UserException(String arg0) {
		super(arg0);
	}

	public UserException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public UserException(Throwable arg0) {
		super(arg0);
	}

}
