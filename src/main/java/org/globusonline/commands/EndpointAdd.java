package org.globusonline.commands;

import org.globusonline.GojiTransferAPIClient;
import org.globusonline.JGOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EndpointAdd extends JGOCommand {

	private final GojiTransferAPIClient client;

	private static final String OPERATION = "endpoint-add";

	// input
	private final String gridFTPServer;
	private final String myProxyServer;
	private final String serverDN;
	private final boolean isGlobusConnect;
	private final boolean isPublic;
	private String endpointName;
	private final String username;

	private String jsonData;

	// output
	private String msg = null;
	private String gc_key = null;
	private String req_id = null;

	private final String resource = null;
	private String type = null;
	private String canonical_name = null;

	public EndpointAdd(GojiTransferAPIClient client, String gridFTPServer,
			String myProxyServer, String serverDN, boolean isGlobusConnect,
			boolean isPublic, String endpointName, String username)
					throws JSONException {

		this.client = client;
		this.gridFTPServer = gridFTPServer;
		this.myProxyServer = myProxyServer;
		this.serverDN = serverDN;
		this.isGlobusConnect = isGlobusConnect;
		this.isPublic = isPublic;
		this.endpointName = endpointName;
		this.username = username;
		init();
	}

	@Override
	public String getJsonData() {

		return jsonData;
	}

	@Override
	public String getMethod() {
		return "POST";
	}

	@Override
	public String getPath() {
		return "/endpoint";
	}

	private void init() throws JSONException {
		JSONObject jobj = new JSONObject();

		jobj.put("username", username);
		jobj.put("DATA_TYPE", "endpoint");
		jobj.put("activated", (Object) null);
		jobj.put("is_globus_connect", isGlobusConnect);

		// v0.9
		// jobj.put("name", username + "#" + endpointName);
		// jobj.put("canonical_name", username + "#" + endpointName);

		// v0.10
		int pos = endpointName.indexOf("#");
		if (pos != -1) {
			endpointName = endpointName.substring(pos + 1);
			jobj.put("name", endpointName);
			jobj.put("canonical_name", endpointName);
		} else {
			jobj.put("name", endpointName);
			jobj.put("canonical_name", endpointName);
		}
		jobj.put("myproxy_server", myProxyServer);

		JSONArray dataArr = new JSONArray();
		JSONObject dataObj = new JSONObject();
		String host = "";
		String port = "2811";
		String[] pieces = gridFTPServer.split(":");
		if (pieces != null) {
			host = pieces[0];
			if (pieces.length > 1) {
				port = pieces[1];
			}
		}

		dataObj.put("DATA_TYPE", "server");
		dataObj.put("hostname", host);
		dataObj.put("port", port);
		dataObj.put("uri", "gsiftp://" + host + ":" + port);
		dataObj.put("scheme", "gsiftp");
		if (serverDN != null) {
			dataObj.put("subject", serverDN);
		}
		dataArr.put(dataObj);
		jobj.put("DATA", dataArr);

		jobj.put("public", isPublic);

		jsonData = jobj.toString();
		System.out.println("SENDING POST: " + jsonData);
	}

	@Override
	public void processResult() {

		if (this.result != null) {
			this.type = JGOUtils.extractFromResults(result, "DATA_TYPE");
			if (this.type.equals("endpoint_create_result")) {
				this.msg = JGOUtils.extractFromResults(result, "message");
				this.gc_key = JGOUtils.extractFromResults(result,
						"globus_connect_setup_key");
				this.req_id = JGOUtils.extractFromResults(result, "request_id");
				this.canonical_name = JGOUtils.extractFromResults(result,
						"canonical_name");
			} else {
				System.out.println("Got unknown result type: " + result);
			}
		}

	}

	@Override
	public String toString() {
		StringBuffer strbuf = new StringBuffer("\n");
		if ((this.gc_key != null) && (!this.gc_key.equals("null"))) {
			strbuf.append("Created the Globus Connect endpoint '");
			strbuf.append(this.canonical_name);
			strbuf.append("'.");
			strbuf.append("\n");
			strbuf.append("Use this setup key when installing Globus Connect:");
			strbuf.append("\n\t");
			strbuf.append(this.gc_key);
			strbuf.append("\n");
		} else {
			strbuf.append(this.msg);
			strbuf.append("\n");
		}
		return strbuf.toString();
	}

}
