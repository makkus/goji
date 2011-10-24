package nz.org.nesi.goji.examples;

import grisu.jcommons.exceptions.CredentialException;
import nz.org.nesi.goji.control.GlobusOnlineSession;
import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.model.Endpoint;

public class SessionExample {

	/**
	 * @param args
	 * @throws CredentialException
	 * @throws CommandException
	 */
	public static void main(String[] args) throws CredentialException,
	CommandException {

		String go_user = "nz";

		GlobusOnlineSession session = new GlobusOnlineSession(go_user);


		System.out.println("List of endpoints:");
		for (Endpoint e : session.getAllEndpoints()) {
			System.out.println(e.getName());
		}

		for (Endpoint e : session.getAllUserEndpoints()) {
			System.out.println("User endpoint: " + e.getName());
			System.out.println("Expires: " + e.getExpires());
		}

		System.out.println("Activating user endpoints...");
		session.activateAllUserEndpoints();
		System.out.println("User endpoints activated.");

		for (Endpoint e : session.getAllUserEndpoints()) {
			System.out.println("User endpoint: " + e.getName());
			System.out.println("Expires: " + e.getExpires());
			session.list(e.getName(), "/~/");

		}


	}
}
