package nz.org.nesi.goji.exceptions;

public class CommandException extends Exception {

	public CommandException() {
	}

	public CommandException(String arg0) {
		super(arg0);
	}

	public CommandException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public CommandException(Throwable arg0) {
		super(arg0);
	}

}
