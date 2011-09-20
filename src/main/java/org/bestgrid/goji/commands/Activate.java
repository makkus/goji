package org.bestgrid.goji.commands;

import org.apache.commons.lang.StringUtils;
import org.bestgrid.goji.GO_PARAM;
import org.bestgrid.goji.exceptions.CommandConfigException;
import org.bestgrid.goji.exceptions.InitException;
import org.bestgrid.goji.exceptions.RequestException;
import org.bestgrid.goji.model.Credential;
import org.globusonline.GojiTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;

public class Activate extends AbstractCommand {

	public Activate(GojiTransferAPIClient client, String endpoint,
			Credential cred, Integer lifetime_in_hours) {

		this(client, endpoint, cred.getMyProxyServer(), cred
				.getMyProxyUsername(), cred.getMyProxyPassword(),
				lifetime_in_hours);

	}

	public Activate(GojiTransferAPIClient client, String endpoint,
			String myproxyServer, String myproxyUsername,
			char[] myproxyPassword, Integer lifetime_in_hours) {

		super(client, new ImmutableMap.Builder<GO_PARAM, String>()
				.put(GO_PARAM.ENDPOINT_NAME, endpoint)
				.put(GO_PARAM.MYPROXY_HOST, myproxyServer)
				.put(GO_PARAM.MYPROXY_USERNAME, myproxyUsername)
				.put(GO_PARAM.MYPROXY_PASSWORD, new String(myproxyPassword))
				.put(GO_PARAM.PROXY_LIFETIME_IN_HOURS,
						lifetime_in_hours.toString())
						.build());
	}

	private void activate(JSONArray arResult, String myProxyServer,
			String myProxyUser,
			String myProxyPassword) {

		Integer lifetimeInHours = -1;
		try {
			lifetimeInHours = Integer
					.parseInt(getConfig(GO_PARAM.PROXY_LIFETIME_IN_HOURS));
		} catch (Exception e) {
			throw new InitException("Can't get lifetime for proxy.", e);
		}

		// String endpoint = getConfig(Input.ENDPOINT_NAME);

		boolean ret = false;

		if (lifetimeInHours == -1) {
			lifetimeInHours = 12;
		}

		try {

			JSONObject jobj = arResult.getJSONObject(0);
			JSONArray dataArr = jobj.getJSONArray("DATA");

			JSONObject data = null;
			for (int i = 0; i < dataArr.length(); i++) {
				data = dataArr.getJSONObject(i);

				if ((data.get("name") != null)
						&& (data.get("name").equals("hostname"))) {
					data.put("value", myProxyServer);
				} else if ((data.get("name") != null)
						&& (data.get("name").equals("username"))) {
					data.put("value", myProxyUser);
				} else if ((data.get("name") != null)
						&& (data.get("name").equals("passphrase"))) {
					data.put("value", myProxyPassword);
				} else if ((data.get("name") != null)
						&& (data.get("name").equals("lifetime_in_hours"))) {
					data.put("value", lifetimeInHours);
				}
			}

		} catch (JSONException je) {
			throw new InitException(je);
		}
		String jsonData = arResult.toString();
		jsonData = jsonData.substring(1, jsonData.length() - 1);

		putJsonData(jsonData);


	}

	@Override
	public Method getMethodType() {
		return Method.POST;
	}

	@Override
	public String getPath() {

		return "/endpoint/" + getConfig(GO_PARAM.ENDPOINT_NAME) + "/activate";

	}

	@Override
	protected void init() throws InitException {

		// first, we need to get ActivationRequirements
		ActivationRequirements ar = null;
		try {
			ar = new ActivationRequirements(client,
					getConfig(GO_PARAM.ENDPOINT_NAME));
		} catch (CommandConfigException e) {
			throw new InitException(e);
		}

		JSONArray arResult = ar.getResult();
		if (arResult == null) {
			throw new InitException(
					"Could not determine ActivationRequirements");
		}

		String myproxyHost = null;
		try {
			myproxyHost = getConfig(GO_PARAM.MYPROXY_HOST);
		} catch (CommandConfigException e) {
			throw new InitException(e);
		}
		if (StringUtils.isBlank(myproxyHost)) {
			// use the default one
			myproxyHost = ar.getOutput(GO_PARAM.MYPROXY_HOST);
		}

		if (StringUtils.isBlank(myproxyHost)) {
			throw new InitException(
					"No myproxy provided and no default myproxy configured for endpoint");
		}

		String myProxyUsername = getConfig(GO_PARAM.MYPROXY_USERNAME);
		String myProxyPassString = getConfig(GO_PARAM.MYPROXY_PASSWORD);

		JSONArray dataArr = ar.getResult();

		activate(dataArr, myproxyHost, myProxyUsername, myProxyPassString);


	}

	@Override
	protected void processResult() {

		JSONObject jobj = null;
		try {
			jobj = result.getJSONObject(0);

			String activationMessage = jobj.getString("message");
			putOutput(GO_PARAM.MESSAGE, activationMessage);
			Boolean success = false;
			if (activationMessage.indexOf("activated successfully") != -1) {
				success = true;
			}
			putOutput(GO_PARAM.SUCCESS, success.toString());
			String subject = jobj.getString("subject");
			putOutput(GO_PARAM.SUBJECT, subject);
			String expire_time = jobj.getString("expire_time");
			putOutput(GO_PARAM.EXPIRE_TIME, expire_time);
		} catch (JSONException e) {
			throw new RequestException(e);
		}
	}

}
