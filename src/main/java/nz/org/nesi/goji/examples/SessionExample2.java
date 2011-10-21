package nz.org.nesi.goji.examples;

import grisu.jcommons.model.info.GFile;
import grith.jgrith.voms.VO;

import java.util.Set;

import nz.org.nesi.goji.control.GlobusOnlineSession;
import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.exceptions.CredentialException;
import nz.org.nesi.goji.model.Credential;

public class SessionExample2 {

	/**
	 * @param args
	 * @throws CredentialException
	 * @throws CommandException
	 */
	public static void main(String[] args) throws CredentialException,
	CommandException {

		// your globus online username
		String go_user = "nz";

		// creating the session, this constructor assumes there's an existing
		// proxy
		// available at /tmp/x509u...
		GlobusOnlineSession session = new GlobusOnlineSession(go_user);

		// Creating a voms proxy out of the default proxy (need that for my
		// endpoints since I don't have
		// any filesystems I can access with a "plain", non-voms proxy.
		// You might not need it...
		VO nz = new VO("nz", "voms.bestgrid.org", 15000,
				"/C=NZ/O=BeSTGRID/OU=The University of Auckland/CN=voms.bestgrid.org");
		Credential nz_nesi = session.getCredential().createVomsCredential(nz,
				"/nz/nesi");
		// making sure that the proxy is accessible for GlobusOnline via MyProxy
		// internally, Goji creates a random username/password combination for
		// the proxy. Might change that later on...
		nz_nesi.uploadMyProxy();

		// activate the endpoint with the newly created voms proxy
		session.activateEndpoint("gram5_ceres_auckland_ac_nz--nz_nesi", nz_nesi);

		// now we can run commands against this endpoint, like listing it's
		// files
		Set<GFile> files = session.list("gram5_ceres_auckland_ac_nz--nz_nesi",
				"/~/");
		for (GFile f : files) {
			System.out.println(f.getName());
		}


	}
}
