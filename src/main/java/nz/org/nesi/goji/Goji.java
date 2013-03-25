package nz.org.nesi.goji;

import grisu.grin.YnfoManager;
import grisu.grin.model.Grid;
import grisu.jcommons.model.info.Directory;
import grisu.jcommons.model.info.Group;
import grith.jgrith.cred.Cred;
import grith.jgrith.cred.ProxyCred;
import grith.jgrith.plainProxy.LocalProxy;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Set;

import nz.org.nesi.goji.model.Endpoint;
import nz.org.nesi.goji.model.commands.EndpointList;
import nz.org.nesi.goji.model.commands.EndpointRemove;

import org.globusonline.transfer.BCTransferAPIClient;
import org.globusonline.transfer.BaseTransferAPIClient;

import com.google.common.collect.Sets;

public class Goji {

	public static final String VERSION = "v0.10";
	public static final String DEV_URL = "https://transfer.qa.api.globusonline.org/dev";

	public static final String DEFAULT_BASE_URL = "https://transfer.api.globusonline.org/"
			+ VERSION;

	// public static final String DEFAULT_BASE_URL = DEV_URL;

	public static void main(String[] args) throws KeyManagementException,
	NoSuchAlgorithmException, Exception {

		YnfoManager im = new YnfoManager(
				"/home/markus/src/infosystems/ynfo/src/test/resources/default_config.groovy");
		
		Grid grid = im.getGrid();

		String go_username = "nz";

		Cred c = new ProxyCred();


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

		Set<Group> groups = Sets.newTreeSet();
		
		groups.add(grid.getGroup("/nz/nesi"));
		//groups.add(grid.getGroup("/nz/nesi"));
		

		for (Group group : groups) {
			System.out.println(group.toString() + "\n=====\n");


			Collection<Directory> dirs = grid.getResources(Directory.class, group);

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
