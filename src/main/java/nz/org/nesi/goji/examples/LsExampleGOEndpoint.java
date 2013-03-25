package nz.org.nesi.goji.examples;

import grisu.jcommons.exceptions.CredentialException;
import grisu.jcommons.model.info.GFile;
import grith.jgrith.cred.Cred;
import grith.jgrith.cred.X509Cred;

import java.util.Set;

import nz.org.nesi.goji.control.GlobusOnlineSession;
import nz.org.nesi.goji.exceptions.CommandException;

public class LsExampleGOEndpoint {

	/**
	 * @param args
	 * @throws CredentialException
	 * @throws CommandException
	 */
	public static void main(String[] args) throws CredentialException,
	CommandException {

		// your globus online username
		String go_user = "nz";

		// creating the session
		Cred cred = X509Cred.create(args[0].toCharArray());
		GlobusOnlineSession session = new GlobusOnlineSession(go_user, cred);

		session.activateEndpoint("go#ep1", null);

		// now we can run commands against this endpoint, like listing its files
		Set<GFile> files = session.listDirectory("go#ep1",
				"/~/");
		for (GFile f : files) {
			System.out.println(f.getName());
		}

	}
}
