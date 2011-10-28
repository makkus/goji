package nz.org.nesi.goji.examples.info;

import grisu.info.ynfo.YnfoManager;
import grisu.jcommons.interfaces.InfoManager;
import nz.org.nesi.goji.control.GlobusOnlineUserSession;
import nz.org.nesi.goji.exceptions.UserException;

import org.apache.commons.lang.StringUtils;


public class GojiTest {

	/**
	 * @param args
	 * @throws UserException
	 */
	public static void main(String[] args) throws Exception {

		GlobusOnlineUserSession session = null;

		InfoManager im = new YnfoManager(
				"/home/markus/src/infosystems/ynfo/src/test/resources/default_config.groovy");

		if (StringUtils.isNotBlank(args[0])) {
			session = new GlobusOnlineUserSession("nz", args[0].toCharArray(),
					im);
		} else {
			session = new GlobusOnlineUserSession("nz", im);
		}

		System.out.println("All available VOs:");
		System.out.println(StringUtils.join(session.getFqans(), "\n"));

		// session.removeAllEndpoints();
		// session.createAllEndpoints();


		session.activateAllEndpoints();


	}

}
