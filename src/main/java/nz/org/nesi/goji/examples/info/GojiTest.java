package nz.org.nesi.goji.examples.info;

import grisu.control.info.SqlInfoManager;
import grisu.jcommons.interfaces.InfoManager;
import grith.jgrith.credential.Credential;
import grith.jgrith.credential.CredentialFactory;
import nz.org.nesi.goji.control.GlobusOnlineUserSession;
import nz.org.nesi.goji.exceptions.UserException;
import nz.org.nesi.goji.model.Endpoint;

import org.apache.commons.lang.StringUtils;


public class GojiTest {

	/**
	 * @param args
	 * @throws UserException
	 */
	public static void main(String[] args) throws Exception {

		GlobusOnlineUserSession session = null;

		// Credential c = new Credential();

		// BaseTransferAPIClient client = new GssJSONTransferAPIClient(
		// go_username, trustedCAFile, c.getCredential(),
		// Goji.DEFAULT_BASE_URL);

		//		 InfoManager im = new YnfoManager(
		//		 "/home/markus/src/infosystems/ynfo/src/test/resources/default_config.groovy");

		InfoManager im = new SqlInfoManager();

		// Credential c = new Credential("markus", "nixenixe25".toCharArray(),
		// "myproxy.arcs.org.au", 7512);

		Credential c = CredentialFactory.createFromCommandline();
		c.saveCredential();
		// Credential c = new Credential("0istbesserals00".toCharArray());
		// c.saveCredential();

		session = new GlobusOnlineUserSession("markus", c, im);

		// if ((args.length == 1) && StringUtils.isNotBlank(args[0])) {
		// session = new GlobusOnlineUserSession("nz", args[0].toCharArray(),
		// im);
		// } else {
		// session = new GlobusOnlineUserSession("markus", im);
		// }

		System.out.println("All available VOs:");
		System.out.println(StringUtils.join(session.getFqans(), "\n"));

		// session.removeAllEndpoints();
		session.createAllEndpoints();

		for (Endpoint ep : session.getAllUserEndpoints()) {

			System.out.println("Endpoint " + ep.getName() + ": "
					+ ep.getHostname());
		}

		session.activateAllEndpoints();


	}

}
