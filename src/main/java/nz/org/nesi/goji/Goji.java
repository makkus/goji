package nz.org.nesi.goji;

import grisu.info.ynfo.YnfoManager;
import grisu.jcommons.interfaces.InfoManager;
import grisu.jcommons.model.info.Directory;
import grith.jgrith.credential.Credential;
import grith.jgrith.credential.ProxyCredential;
import grith.jgrith.plainProxy.LocalProxy;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import nz.org.nesi.goji.model.Endpoint;
import nz.org.nesi.goji.model.commands.EndpointList;
import nz.org.nesi.goji.model.commands.EndpointRemove;

import org.globusonline.transfer.BCTransferAPIClient;
import org.globusonline.transfer.BaseTransferAPIClient;

public class Goji {

	public static final String VERSION = "v0.10";
	public static final String DEV_URL = "https://transfer.qa.api.globusonline.org/dev";

	public static final String DEFAULT_BASE_URL = "https://transfer.api.globusonline.org/"
			+ VERSION;

	// public static final String DEFAULT_BASE_URL = DEV_URL;

	public static void main(String[] args) throws KeyManagementException,
	NoSuchAlgorithmException, Exception {

		InfoManager im = new YnfoManager(
				"/home/markus/src/infosystems/ynfo/src/test/resources/default_config.groovy");

		String go_username = "nz";

		Credential c = new ProxyCredential();


		// BaseTransferAPIClient client = new GssJSONTransferAPIClient(
		// go_username, trustedCAFile, c.getCredential(),
		// Goji.DEFAULT_BASE_URL);

		BCTransferAPIClient client = new BCTransferAPIClient(go_username,
				BaseTransferAPIClient.FORMAT_JSON, LocalProxy.PROXY_FILE,
				LocalProxy.PROXY_FILE, Goji.DEFAULT_BASE_URL);
		// DEFAULT_BASE_URL, "/tmp/x509up_u1000",
		// "/tmp/x509up_u1000",
		// "/home/markus/.globus/certificates/gd_bundle.crt", false);

		// try {
		// EndpointRemove epR = new EndpointRemove(client, "autotest1");
		//
		// System.out.println(epR.toString());
		// } catch (Exception e) {
		// System.err.println(e.getLocalizedMessage());
		// }
		//
		// Thread.sleep(5000);

		// EndpointAdd epA = new EndpointAdd(client, "ng2.vpac.org",
		// "myproxy.arcs.org.au", null, false, true,
		// "canterbury_ac_nz--nz_nesi");
		//
		// System.out.println(epA.toString());

		EndpointList epL = new EndpointList(client);

		System.out.println("Endpoints: " + epL.getEndpoints().size());

		for (Endpoint e : epL.getEndpoints().values()) {

			if (e.getUsername().equals(go_username)) {
				try {
					System.out.println("Removing: " + e.getName());
					EndpointRemove epR = new EndpointRemove(client, e.getName());
					System.out.println(epR.toString());
				} catch (Exception ex) {
					System.err.println(ex.getLocalizedMessage());
				}
			}

		}

		String[] fqans = new String[] { "/nz/nesi", "/nz/test" };

		for (String fqan : fqans) {
			System.out.println(fqan + "\n=====\n");

			Set<Directory> dirs = im.getDirectoriesForVO(fqan);

			for (Directory d : dirs) {
				System.out.println(d.toString());
			}

		}

		// for (String fqan : fqans) {
		// Set<FileSystem> fss = im.getFileSystemsForVO(fqan);
		// for (FileSystem fs : fss) {
		// EndpointAdd ea = new EndpointAdd(client, fs.getHost(),
		// "myproxy.arcs.org.au", null, false, true,
		// EndpointHelpers.translateIntoEndpointName(fs.getHost(),
		// fqan));
		//
		// }
		//
		// }

	}

}
