package org.bestgrid.goji.commands;

import java.util.Map;

import org.bestgrid.goji.exceptions.CommandConfigException;
import org.bestgrid.goji.exceptions.InitException;
import org.globusonline.GojiTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;

public class EndpointAdd extends AbstractCommand {


	public EndpointAdd(GojiTransferAPIClient client, Map<Input, String> config) {

		super(client, config);

	}

	public EndpointAdd(GojiTransferAPIClient client, String gridFTPServer,
			String myProxyServer, String serverDN, boolean isGlobusConnect,
			boolean isPublic, String endpointName) {
		super(client, new ImmutableMap.Builder<Input, String>()
				.put(Input.GRIDFTP_SERVER, gridFTPServer)
				.put(Input.MYPROXY_SERVER, myProxyServer)
				.put(Input.SERVER_DN, (serverDN == null) ? NO_VALUE : serverDN)
				.put(Input.IS_GLOBUS_CONNECT,
						new Boolean(isGlobusConnect).toString())
						.put(Input.IS_PUBLIC, new Boolean(isPublic).toString())
						.put(Input.ENDPOINT_NAME, endpointName).build());
	}


	@Override
	public Method getMethod() {
		return Method.POST;
	}

	@Override
	public String getPath() {
		return "/endpoint";
	}

	@Override
	protected void init() {

		JSONObject jobj = new JSONObject();

		try {
			jobj.put("username", client.getUsername());

			jobj.put("DATA_TYPE", "endpoint");
			jobj.put("activated", (Object) null);
			jobj.put("is_globus_connect",
					Boolean.parseBoolean(getConfig(Input.IS_GLOBUS_CONNECT)));

			// v0.9
			// jobj.put("name", username + "#" + endpointName);
			// jobj.put("canonical_name", username + "#" + endpointName);

			// v0.10
			String endpointName = getConfig(Input.ENDPOINT_NAME);
			int pos = endpointName.indexOf("#");
			if (pos != -1) {
				endpointName = endpointName.substring(pos + 1);
				jobj.put("name", endpointName);
				jobj.put("canonical_name", endpointName);
			} else {
				jobj.put("name", endpointName);
				jobj.put("canonical_name", endpointName);
			}
			jobj.put("myproxy_server", getConfig(Input.MYPROXY_SERVER));

			JSONArray dataArr = new JSONArray();
			JSONObject dataObj = new JSONObject();
			String host = "";
			String port = "2811";
			String[] pieces = getConfig(Input.GRIDFTP_SERVER).split(":");
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
			if (getConfig(Input.SERVER_DN) != null) {
				dataObj.put("subject", getConfig(Input.SERVER_DN));
			}
			dataArr.put(dataObj);
			jobj.put("DATA", dataArr);

			jobj.put("public", Boolean.parseBoolean(getConfig(Input.IS_PUBLIC)));

			String jsonData = jobj.toString();
			System.out.println("SENDING POST: " + jsonData);
			putJsonData(jsonData);
		} catch (JSONException e) {
			throw new InitException(e);
		} catch (CommandConfigException e) {
			throw new InitException(e);
		}
	}

	@Override
	public void processResult() {

		if (this.result != null) {
			String type = extractFromResults("DATA_TYPE");
			if (type.equals("endpoint_create_result")) {
				putOutput(Output.MESSAGE, extractFromResults("message"));
				putOutput(Output.GC_KEY,
						extractFromResults("globus_connect_setup_key"));
				putOutput(Output.REQ_ID, extractFromResults("request_id"));
				putOutput(Output.CANONICAL_NAME,
						extractFromResults("canonical_name"));
			} else {
				System.out.println("Got unknown result type: " + result);
			}
		}

	}

	@Override
	public String toString() {
		StringBuffer strbuf = new StringBuffer("\n");
		if ((getOutput(Output.GC_KEY) != null)
				&& (!getOutput(Output.GC_KEY).equals("null"))) {
			strbuf.append("Created the Globus Connect endpoint '");
			strbuf.append(getOutput(Output.CANONICAL_NAME));
			strbuf.append("'.");
			strbuf.append("\n");
			strbuf.append("Use this setup key when installing Globus Connect:");
			strbuf.append("\n\t");
			strbuf.append(getOutput(Output.CANONICAL_NAME));
			strbuf.append("\n");
		} else {
			strbuf.append(getOutput(Output.MESSAGE));
			strbuf.append("\n");
		}
		return strbuf.toString();
	}

}
