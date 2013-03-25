package nz.org.nesi.goji.examples;

import grith.jgrith.cred.Cred;
import grith.jgrith.cred.X509Cred;
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
		Cred cred = X509Cred.create(args[0].toCharArray());
		GlobusOnlineSession session = new GlobusOnlineSession(go_user, cred);

		Transfer t = session
				.getTransfer("2a19c5f4-fead-11e0-bc85-1231381a212f");

		t.waitForTransferToFinish();

		System.out.println(t.toString());
	}

}
