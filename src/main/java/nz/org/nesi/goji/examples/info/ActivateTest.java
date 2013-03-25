package nz.org.nesi.goji.examples.info;

import grisu.grin.YnfoManager;
import grisu.jcommons.exceptions.CredentialException;
import grith.jgrith.cred.Cred;

import java.util.Map;

import nz.org.nesi.goji.control.UserEnvironment;
import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.exceptions.UserException;
import nz.org.nesi.goji.model.Endpoint;
import nz.org.nesi.goji.model.commands.Activate;

import org.apache.commons.lang.StringUtils;

public class ActivateTest {

	/**
	 * @param args
	 * @throws UserException
	 * @throws CredentialException
	 * @throws CommandException
	 */
	public static void main(String[] args) throws UserException,
	CredentialException, CommandException {

		UserEnvironment user = null;

		YnfoManager im = new YnfoManager("nesi");
		

		if ((args.length > 0) && StringUtils.isNotBlank(args[0])) {
			user = new UserEnvironment("nz", args[0].toCharArray(), im.getGrid());
		} else {
			user = new UserEnvironment("nz", im.getGrid());
		}

		// user.removeAllEndpoints();

		// user.addEndpoint("df.auckland.ac.nz", "/nz/nesi", "test1");

		System.out.println("Endpoints:");
		Map<String, Endpoint> eps = user.getEndpoints();
		for (String ep : eps.keySet()) {
			System.out.println(ep);
		}

		Cred cred = user.getCredential("/ARCS/BeSTGRID");

		String username = cred.getMyProxyUsername();
		char[] password = cred.getMyProxyPassword();

		cred.uploadMyProxy();

		System.out.println(username);
		System.out.println(new String(password));

		Activate a = new Activate(user.getClient(), "test1",
				"myproxy.arcs.org.au", username, password, 12);

	}

}
