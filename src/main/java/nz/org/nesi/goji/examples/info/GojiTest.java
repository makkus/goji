package nz.org.nesi.goji.examples.info;

import grisu.jcommons.interfaces.GrinformationManagerDozer;
import grisu.jcommons.interfaces.InformationManager;
import grisu.jcommons.model.info.GFile;
import grisu.model.info.dto.Directory;
import grith.jgrith.Environment;
import grith.jgrith.cred.Cred;
import grith.jgrith.cred.MyProxyCred;

import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import nz.org.nesi.goji.control.GlobusOnlineUserSession;
import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.exceptions.UserException;
import nz.org.nesi.goji.model.Endpoint;

import org.apache.commons.lang3.StringUtils;

public class GojiTest {

	/**
	 * @param args
	 * @throws UserException
	 */
	public static void main(String[] args) throws Exception {

		Environment.initEnvironment();

		// Credential c = new Credential();

		// BaseTransferAPIClient client = new GssJSONTransferAPIClient(
		// go_username, trustedCAFile, c.getCredential(),
		// Goji.DEFAULT_BASE_URL);

		// InfoManager im = new YnfoManager(
		// "/home/markus/src/infosystems/ynfo/src/test/resources/default_config.groovy");

		InformationManager informationManager = new GrinformationManagerDozer(
				"/data/src/config/nesi-grid-info/nesi_info.groovy");

		// Credential c = new Credential("markus", "nixenixe25".toCharArray(),
		// "myproxy.arcs.org.au", 7512);

		Cred c = MyProxyCred.loadFromDefault();

		final GlobusOnlineUserSession session = new GlobusOnlineUserSession(
				"markus", "markus", c, informationManager);

		// if ((args.length == 1) && StringUtils.isNotBlank(args[0])) {
		// session = new GlobusOnlineUserSession("nz", args[0].toCharArray(),
		// im);
		// } else {
		// session = new GlobusOnlineUserSession("markus", im);
		// }

		System.out.println("All available VOs:");
		System.out.println(StringUtils.join(session.getFqans(), "\n"));

		session.removeAllEndpoints();
		System.exit(0);
		session.createAllEndpoints();

		for (Endpoint ep : session.getAllUserEndpoints()) {

			System.out.println("Endpoint " + ep.getName() + ": "
					+ ep.getHostname());
		}

		// long start = new Date().getTime();
		// session.activateAllEndpoints();
		// long end = new Date().getTime();
		// System.out.println("Time to activate endpoints: "+(end-start)/1000+" seconds");
		int threads = 20;
		// ExecutorService executor =
		// Executors.newFixedThreadPool(session.getDirectories().size());
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		for (final Directory d : session.getDirectories()) {
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						// session.getAllEndpoints(true);
						System.out.println("Started");
						session.activateEndpoint("nz", d, false);
						SortedSet<GFile> files = session.listDirectory(
								d.getAlias(), d.getPath());
						System.out.println("Finished");
						for (GFile f : files) {
							System.out.println("FILE: " + f.toString());
						}
						System.out.println("Listed " + d.getAlias());
					} catch (CommandException e) {

						System.err.println("ERROR: " + e.getLocalizedMessage());
					}
				}
			};
			executor.execute(t);
		}

		executor.shutdown();

		try {
			executor.awaitTermination(10, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.exit(0);

	}

}
