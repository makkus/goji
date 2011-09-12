package org.bestgrid.goji;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bestgrid.goji.commands.Activate;

public class ActivateTest {

	/**
	 * @param args
	 * @throws UserInitException
	 * @throws CredentialException
	 */
	public static void main(String[] args) throws UserInitException,
	CredentialException {

		User user = null;

		if ((args.length > 0) && StringUtils.isNotBlank(args[0])) {
			user = new User("nz", args[0].toCharArray());
		} else {
			user = new User("nz");
		}

		// user.removeAllEndpoints();

		// user.addEndpoint("df.auckland.ac.nz", "/nz/nesi", "test1");

		System.out.println("Endpoints:");
		Map<String, Endpoint> eps = user.getEndpoints();
		for (String ep : eps.keySet()) {
			System.out.println(ep);
		}

		Credential cred = user.getCredential("/ARCS/BeSTGRID");

		String username = cred.getMyProxyUsername();
		char[] password = cred.getMyProxyPassword();

		cred.uploadMyProxy();

		System.out.println(username);
		System.out.println(new String(password));

		Activate a = new Activate(user.getClient(), "test1",
				"myproxy.arcs.org.au", username, password, 12);

	}

}
