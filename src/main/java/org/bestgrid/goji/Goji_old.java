package org.bestgrid.goji;

import grisu.X;
import grisu.control.info.CachedMdsInformationManager;
import grisu.jcommons.interfaces.InformationManager;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class Goji_old {

	static final String VERSION = "v0.10";
	static final String DEFAULT_BASE_URL = "https://transfer.api.globusonline.org/"
			+ VERSION;

	public static void main(String[] args) throws KeyManagementException,
	NoSuchAlgorithmException, Exception {

		String dburl = "jdbc:mysql://localhost:3306/mds_test?autoReconnect=true";
		String username = "grisu_read";
		String password = "password";

		// String myProxyServer = "myproxy.arcs.org.au";
		// String serverDN = null;
		// boolean isGlobusConnect = false;
		// boolean isPublic = true;

		String go_username = "nz";

		// ImmutableMap<String, String> config = ImmutableMap.of("databaseUrl",
		// dburl, "user", username, "password", password);
		//
		// InformationManager im = new SqlMDSInformationManager(config);

		// SQLQueryClient qc = new SQLQueryClient(dburl, username, password);

		InformationManager im = new CachedMdsInformationManager((String) null);

		Map<String, String[]> dls = im.getDataLocationsForVO("/nz/nesi");

		for (String se : dls.keySet()) {
			X.p("Storage element: " + se);
		}

		// X.p("Servers: " + qc.getGridFTPServersOnGrid().length);

		// for (String s : qc.getGridFTPServersOnGrid()) {
		// String host = FileSystemHelpers.getHost(s);
		// int port = FileSystemHelpers.getPort(s);
		// X.p(s);
		// X.p("\t" + host);
		// X.p("\t" + port);
		//
		// // EndpointAdd epa = new EndpointAdd(client, host, myProxyServer,
		// // serverDN, isGlobusConnect, isPublic, endpointName,
		// // gc_username);
		//
		// }

		// GojiTransferAPIClient client = new GojiTransferAPIClient(go_username,
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
		//
		// EndpointList epL = new EndpointList(client);
		//
		// System.out.println("Results: " + epL.getEndpoints().size());
		//
		// for (Endpoint e : epL.getEndpoints().values()) {
		// System.out.println(e.name + "\t" + e.hostname);
		// try {
		// ActivationRequirements ar = new ActivationRequirements(client,
		// e.username + "%23" + e.name);
		//
		// System.out.println("\t" + ar.getOutput(GO_PARAM.MYPROXY_HOST));
		// } catch (Exception edd) {
		//
		// edd.printStackTrace();
		// }
		//
		// }
		//
		// ActivationRequirements ar = new ActivationRequirements(client,
		// "nz%23auto_test_1");
		//
		// Activate a = new Activate(client, "nz%23auto_test_1",
		// "myproxy.arcs.org.au", "markus_x509", args[0].toCharArray(), 10);
		//
		// System.out.println("Activate: " + a.getOutput(GO_PARAM.SUCCESS)
		// + a.getOutput(GO_PARAM.SUBJECT));

		// EndpointRemove epR = new EndpointRemove(client, "auto_test_1");

		// System.out.println(epR.toString());

	}

}
