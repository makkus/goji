/*
 * Copyright 2010 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nz.org.nesi.goji.model.commands;

import grisu.jcommons.utils.EndpointHelpers;
import grith.jgrith.cred.Cred;
import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.exceptions.InitException;
import nz.org.nesi.goji.exceptions.RequestException;

import org.apache.commons.lang.StringUtils;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;

public class Activate extends AbstractCommand {

	public Activate(BaseTransferAPIClient client) {
		super(client);
	}

	public Activate(BaseTransferAPIClient client, String endpoint, Cred cred,
			Integer lifetime_in_hours) throws CommandException {

		this(client, endpoint, cred.getMyProxyHost(),
				cred.getMyProxyUsername(), cred.getMyProxyPassword(),
				lifetime_in_hours);

	}

	public Activate(BaseTransferAPIClient client, String endpoint,
			String myproxyServer, String myproxyUsername,
			char[] myproxyPassword, Integer lifetime_in_hours)
			throws CommandException {

		super(client, new ImmutableMap.Builder<PARAM, String>()
				.put(PARAM.ENDPOINT_NAME, endpoint)
				.put(PARAM.MYPROXY_HOST, myproxyServer)
				.put(PARAM.MYPROXY_USERNAME, myproxyUsername)
				.put(PARAM.MYPROXY_PASSWORD, new String(myproxyPassword))
				.put(PARAM.PROXY_LIFETIME_IN_HOURS,
						lifetime_in_hours.toString()).build());
	}

	private void activate(JSONArray arResult, String myProxyServer,
			String myProxyUser, String myProxyPassword) {

		if (StringUtils.isBlank(myProxyServer)) {
			putJsonData("");
		} else {

			Integer lifetimeInHours = -1;
			try {
				lifetimeInHours = Integer
						.parseInt(getConfig(PARAM.PROXY_LIFETIME_IN_HOURS));
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
						data.put("value", lifetimeInHours.toString());
					}
				}

			} catch (JSONException je) {
				throw new InitException(je);
			}
			String jsonData = arResult.toString();
			jsonData = jsonData.substring(1, jsonData.length() - 1);

			putJsonData(jsonData);
		}

	}

	@Override
	protected PARAM[] getInputParameters() {
		return new PARAM[] { PARAM.ENDPOINT_NAME };
	}

	@Override
	public Method getMethodType() {
		return Method.POST;
	}

	@Override
	protected PARAM[] getOptionalParameters() {
		return new PARAM[] { PARAM.PROXY_LIFETIME_IN_HOURS, PARAM.MYPROXY_HOST,
				PARAM.MYPROXY_USERNAME, PARAM.MYPROXY_PASSWORD };
	}

	@Override
	protected PARAM[] getOutputParamets() {
		return new PARAM[] { PARAM.MESSAGE, PARAM.SUCCESS, PARAM.SUBJECT,
				PARAM.EXPIRE_TIME };
	}

	@Override
	public String getPath() {

		if (StringUtils.isBlank(getConfig(PARAM.MYPROXY_HOST))) {

			return "/endpoint/"
					+ EndpointHelpers.encode(getConfig(PARAM.ENDPOINT_NAME))
					+ "/autoactivate";

		} else {

			return "/endpoint/"
					+ EndpointHelpers.encode(getConfig(PARAM.ENDPOINT_NAME))
					+ "/activate";
		}

	}

	@Override
	protected void initialize() throws InitException {

		// first, we need to get ActivationRequirements
		ActivationRequirements ar = null;
		try {
			ar = new ActivationRequirements(client,
					getConfig(PARAM.ENDPOINT_NAME));
		} catch (CommandException e) {
			throw new InitException(e);
		}

		JSONArray arResult = ar.getResult();
		if (arResult == null) {
			throw new InitException(
					"Could not determine ActivationRequirements");
		}

		String myproxyHost = null;

		myproxyHost = getConfig(PARAM.MYPROXY_HOST);

		// if (StringUtils.isBlank(myproxyHost)) {
		// // use the default one
		// myproxyHost = ar.getOutput(PARAM.MYPROXY_HOST);
		// }
		//
		// if (StringUtils.isBlank(myproxyHost)) {
		// throw new InitException(
		// "No myproxy provided and no default myproxy configured for endpoint");
		// }

		String myProxyUsername = getConfig(PARAM.MYPROXY_USERNAME);
		String myProxyPassString = getConfig(PARAM.MYPROXY_PASSWORD);

		JSONArray dataArr = ar.getResult();

		activate(dataArr, myproxyHost, myProxyUsername, myProxyPassString);

	}

	@Override
	protected void processResult() throws RequestException {

		JSONObject jobj = null;
		try {
			jobj = result.getJSONObject(0);

			String activationMessage = jobj.getString("message");
			putOutput(PARAM.MESSAGE, activationMessage);
			Boolean success = false;
			if (activationMessage.indexOf("activated successfully") != -1) {
				success = true;
			}
			putOutput(PARAM.SUCCESS, success.toString());
			String subject = jobj.getString("subject");
			putOutput(PARAM.SUBJECT, subject);
			String expire_time = jobj.getString("expire_time");
			putOutput(PARAM.EXPIRE_TIME, expire_time);
		} catch (JSONException e) {
			throw new RequestException(e);
		}
	}

	public void setCredential(Cred c) {
		setCredential(c, 12);
	}

	public void setCredential(Cred c, int lifetimeInHours) {

		if (c == null) {
			return;
		}

		setMyProxyHost(c.getMyProxyHost());
		setMyProxyUsername(c.getMyProxyUsername());
		setMyProxyPassword(c.getMyProxyPassword());
		setProxyLifetimeInHours(lifetimeInHours);

	}

	public void setEndpoint(String endpointName) {
		try {
			setParameter(PARAM.ENDPOINT_NAME, endpointName);
		} catch (CommandException e) {
		}
	}

	public void setMyProxyHost(String host) {
		try {
			setParameter(PARAM.MYPROXY_HOST, host);
		} catch (CommandException e) {
		}
	}

	public void setMyProxyPassword(char[] pw) {
		try {
			setParameter(PARAM.MYPROXY_PASSWORD, new String(pw));
		} catch (CommandException e) {
		}
	}

	public void setMyProxyUsername(String username) {
		try {
			setParameter(PARAM.MYPROXY_USERNAME, username);
		} catch (CommandException e) {
		}
	}

	public void setProxyLifetimeInHours(Integer hours) {
		try {
			setParameter(PARAM.PROXY_LIFETIME_IN_HOURS, hours.toString());
		} catch (CommandException e) {
			myLogger.error(
					"Can't set lifetime in Activate command: "
							+ e.getLocalizedMessage(), e);
		}
	}

}
