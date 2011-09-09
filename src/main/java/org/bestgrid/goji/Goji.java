package org.bestgrid.goji;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import org.bestgrid.goji.commands.EndpointList;
import org.bestgrid.simplinfo.model.Directory;
import org.bestgrid.simplinfo.model.InfoManager;
import org.bestgrid.simplinfo.model.InfoManagerImpl;
import org.globusonline.GojiTransferAPIClient;

public class Goji {

	static final String VERSION = "v0.10";
	static final String DEFAULT_BASE_URL = "https://transfer.api.globusonline.org/"
			+ VERSION;

	public static void main(String[] args) throws KeyManagementException,
	NoSuchAlgorithmException, Exception {

		InfoManager im = new InfoManagerImpl();

		String go_username = "nz";


		GojiTransferAPIClient client = new GojiTransferAPIClient(go_username,
				DEFAULT_BASE_URL, "/tmp/x509up_u1000",
				"/tmp/x509up_u1000",
				"/home/markus/.globus/certificates/gd_bundle.crt", false);

		// try {
		// EndpointRemove epR = new EndpointRemove(client, "autotest1");
		//
		// System.out.println(epR.toString());
		// } catch (Exception e) {
		// System.err.println(e.getLocalizedMessage());
		// }
		//
		// Thread.sleep(5000);
		//
		// EndpointAdd epA = new EndpointAdd(client, "ng2.vpac.org",
		// "myproxy.arcs.org.au", null, false, true,
		// "canterbury_ac_nz--nz_nesi");
		//
		// System.out.println(epA.toString());

		EndpointList epL = new EndpointList(client);

		System.out.println("Endpoints: " + epL.getEndpoints().size());

		// for (Endpoint e : epL.getEndpoints().values()) {
		//
		// if (e.username.equals(go_username)) {
		// try {
		// System.out.println("Removing: " + e.name);
		// EndpointRemove epR = new EndpointRemove(client, e.name);
		// System.out.println(epR.toString());
		// } catch (Exception ex) {
		// System.err.println(ex.getLocalizedMessage());
		// }
		// }
		//
		// }

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
