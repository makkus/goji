package nz.org.nesi.goji.model.commands;

import java.util.Map;

import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.exceptions.InitException;

import org.globusonline.transfer.BaseTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;


public class EndpointAdd extends AbstractCommand {

	public EndpointAdd(BaseTransferAPIClient client) {
		super(client);
	}


	public EndpointAdd(BaseTransferAPIClient client,
			Map<PARAM, String> config)
					throws CommandException {

		super(client, config);

	}

	public EndpointAdd(BaseTransferAPIClient client, String gridFTPServer,
			String myProxyServer, String serverDN, boolean isGlobusConnect,
			boolean isPublic, String endpointName)
					throws CommandException {
		super(client, new ImmutableMap.Builder<PARAM, String>()
				.put(PARAM.GRIDFTP_SERVER, gridFTPServer)
				.put(PARAM.MYPROXY_HOST, myProxyServer)
				.put(PARAM.SERVER_DN, (serverDN == null) ? NO_VALUE : serverDN)
				.put(PARAM.IS_GLOBUS_CONNECT,
						new Boolean(isGlobusConnect).toString())
						.put(PARAM.IS_PUBLIC, new Boolean(isPublic).toString())
						.put(PARAM.ENDPOINT_NAME, endpointName).build());
	}

	@Override
	protected PARAM[] getInputParameters() {
		return new PARAM[] { PARAM.GRIDFTP_SERVER, PARAM.MYPROXY_HOST,
				PARAM.ENDPOINT_NAME };
	}

	@Override
	public Method getMethodType() {
		return Method.POST;
	}

	@Override
	protected PARAM[] getOptionalParameters() {
		return new PARAM[] { PARAM.SERVER_DN, PARAM.IS_GLOBUS_CONNECT,
				PARAM.IS_PUBLIC };
	}

	@Override
	protected PARAM[] getOutputParamets() {
		return new PARAM[] { PARAM.MESSAGE, PARAM.GC_KEY, PARAM.REQ_ID,
				PARAM.CANONICAL_NAME };
	}

	@Override
	public String getPath() {
		return "/endpoint";
	}

	@Override
	protected void initialize() {

		JSONObject jobj = new JSONObject();

		try {
			jobj.put("username", client.getUsername());

			jobj.put("DATA_TYPE", "endpoint");
			jobj.put("activated", (Object) null);
			jobj.put("is_globus_connect",
					Boolean.parseBoolean(getConfig(PARAM.IS_GLOBUS_CONNECT)));

			// v0.9
			// jobj.put("name", username + "#" + endpointName);
			// jobj.put("canonical_name", username + "#" + endpointName);

			// v0.10
			String endpointName = getConfig(PARAM.ENDPOINT_NAME);
			int pos = endpointName.indexOf("#");
			if (pos != -1) {
				endpointName = endpointName.substring(pos + 1);
				jobj.put("name", endpointName);
				jobj.put("canonical_name", endpointName);
			} else {
				jobj.put("name", endpointName);
				jobj.put("canonical_name", endpointName);
			}
			jobj.put("myproxy_server", getConfig(PARAM.MYPROXY_HOST));

			JSONArray dataArr = new JSONArray();
			JSONObject dataObj = new JSONObject();
			String host = "";
			int port = 2811;
			String[] pieces = getConfig(PARAM.GRIDFTP_SERVER).split(":");
			if (pieces != null) {
				host = pieces[0];
				if (pieces.length > 1) {
					try {
						port = Integer.parseInt(pieces[1]);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}

			dataObj.put("DATA_TYPE", "server");
			dataObj.put("hostname", host);
			dataObj.put("port", port);
			dataObj.put("uri", "gsiftp://" + host + ":" + port);
			dataObj.put("scheme", "gsiftp");
			if (getConfig(PARAM.SERVER_DN) != null) {
				dataObj.put("subject", getConfig(PARAM.SERVER_DN));
			}
			dataArr.put(dataObj);
			jobj.put("DATA", dataArr);

			jobj.put("public", Boolean.parseBoolean(getConfig(PARAM.IS_PUBLIC)));

			String jsonData = jobj.toString();
			myLogger.debug("SENDING POST: " + jsonData);
			putJsonData(jsonData);
		} catch (JSONException e) {
			throw new InitException(e);
		}
	}

	@Override
	public void processResult() {

		if (this.result != null) {
			String type = extractFromResults("DATA_TYPE");
			if (type.equals("endpoint_create_result")) {
				putOutput(PARAM.MESSAGE, extractFromResults("message"));
				putOutput(PARAM.GC_KEY,
						extractFromResults("globus_connect_setup_key"));
				putOutput(PARAM.REQ_ID, extractFromResults("request_id"));
				putOutput(PARAM.CANONICAL_NAME,
						extractFromResults("canonical_name"));
			} else {
				System.out.println("Got unknown result type: " + result);
			}
		}

	}

	public void setEndpointName(String name) {
		try {
			setParameter(PARAM.ENDPOINT_NAME, name);
		} catch (CommandException e) {
		}
	}

	public void setGlobusConnect(Boolean isGC) {
		try {
			setParameter(PARAM.IS_GLOBUS_CONNECT, isGC.toString());
		} catch (CommandException e) {
		}
	}


	public void setGridFtpServer(String server) {
		try {
			setParameter(PARAM.GRIDFTP_SERVER, server);
		} catch (CommandException e) {
		}
	}

	public void setIsPublic(Boolean isPublic) {
		try {
			setParameter(PARAM.IS_PUBLIC, isPublic.toString());
		} catch (CommandException e) {
		}
	}

	public void setMyProxyHost(String host) {
		try {
			setParameter(PARAM.MYPROXY_HOST, host);
		} catch (CommandException e) {
		}
	}

	public void setServerDn(String dn) {
		try {
			setParameter(PARAM.SERVER_DN, dn);
		} catch (CommandException e) {
		}
	}

	@Override
	public String toString() {
		StringBuffer strbuf = new StringBuffer("\n");
		if ((getOutput(PARAM.GC_KEY) != null)
				&& (!getOutput(PARAM.GC_KEY).equals("null"))) {
			strbuf.append("Created the Globus Connect endpoint '");
			strbuf.append(getOutput(PARAM.CANONICAL_NAME));
			strbuf.append("'.");
			strbuf.append("\n");
			strbuf.append("Use this setup key when installing Globus Connect:");
			strbuf.append("\n\t");
			strbuf.append(getOutput(PARAM.CANONICAL_NAME));
			strbuf.append("\n");
		} else {
			strbuf.append(getOutput(PARAM.MESSAGE));
			strbuf.append("\n");
		}
		return strbuf.toString();
	}


}
