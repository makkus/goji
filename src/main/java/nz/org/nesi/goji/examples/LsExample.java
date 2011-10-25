package nz.org.nesi.goji.examples;

import grisu.jcommons.exceptions.CredentialException;
import grisu.jcommons.model.info.GFile;

import java.util.Set;

import nz.org.nesi.goji.control.GlobusOnlineSession;
import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.model.Credential;

public class LsExample {

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
		Credential cred = new Credential(args[0].toCharArray());
		GlobusOnlineSession session = new GlobusOnlineSession(go_user, cred);

		// Creating a voms proxy out of the default proxy (need that for my
		// endpoints since I don't have any filesystems I can access with a
		// "plain", non-voms proxy.
		// You might not need to do it...
		Credential nz_nesi = session.getCredential().createVomsCredential(
				"/nz/nesi");
		// making sure that the proxy is accessible for GlobusOnline via MyProxy
		// internally, Goji creates a random username/password combination for
		// the proxy. Might change that later on...
		nz_nesi.uploadMyProxy();

		// activate the endpoint with the newly created voms proxy
		session.activateEndpoint("gram5_ceres_auckland_ac_nz--nz_nesi", nz_nesi);

		// now we can run commands against this endpoint, like listing its files
		Set<GFile> files = session.listDirectory("gram5_ceres_auckland_ac_nz--nz_nesi",
				"/~/");
		for (GFile f : files) {
			System.out.println(f.getName());
		}

	}
}
