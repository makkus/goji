package nz.org.nesi.goji.examples;

import grith.jgrith.credential.Credential;
import grith.jgrith.credential.X509Credential;
import nz.org.nesi.goji.control.GlobusOnlineSession;
import nz.org.nesi.goji.model.Transfer;

public class WaitForTransferExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// your globus online username
		String go_user = "nz";

		// creating the session
		Credential cred = new X509Credential(args[0].toCharArray());
		GlobusOnlineSession session = new GlobusOnlineSession(go_user, cred);

		Transfer t = session
				.getTransfer("2a19c5f4-fead-11e0-bc85-1231381a212f");

		t.waitForTransferToFinish();

		System.out.println(t.toString());
	}

}
