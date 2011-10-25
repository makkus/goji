package nz.org.nesi.goji.examples;

import nz.org.nesi.goji.control.GlobusOnlineSession;
import nz.org.nesi.goji.model.Credential;
import nz.org.nesi.goji.model.Transfer;

public class TransferExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

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
		// activate the ep1 endpoing
		session.activateEndpoint("go#ep1", null);

		Transfer t = session.transfer(
				"gram5_ceres_auckland_ac_nz--nz_nesi/~/testfile.result.txt",
				"go#ep1/~/testfile.result.txt");

		t.waitForTransferToFinish(2);

		System.out.println(t.getCompletionTimeString(false));



	}

}
