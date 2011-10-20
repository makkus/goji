package org.bestgrid.goji;

import grisu.info.ynfo.YnfoManager;
import grisu.jcommons.interfaces.InfoManager;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bestgrid.goji.commands.Activate;
import org.bestgrid.goji.control.User;
import org.bestgrid.goji.exceptions.UserException;
import org.bestgrid.goji.model.Credential;
import org.bestgrid.goji.model.Endpoint;

public class ActivateTest {

	/**
	 * @param args
	 * @throws UserException
	 * @throws CredentialException
	 */
	public static void main(String[] args) throws UserException,
	CredentialException {

		User user = null;

		InfoManager im = new YnfoManager(
				"/home/markus/src/infosystems/ynfo/src/test/resources/default_config.groovy");

		if ((args.length > 0) && StringUtils.isNotBlank(args[0])) {
			user = new User("nz", args[0].toCharArray(), im);
		} else {
			user = new User("nz", im);
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
