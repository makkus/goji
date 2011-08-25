package org.bestgrid.goji.commands;

import org.bestgrid.goji.exceptions.CommandConfigException;
import org.bestgrid.goji.exceptions.InitException;
import org.globusonline.GojiTransferAPIClient;
import org.json.JSONObject;

public class ActivationRequirements extends AbstractCommand {

	public ActivationRequirements(GojiTransferAPIClient client, String endpoint) {
		super(client, Input.ENDPOINT_NAME, endpoint);
	}

	@Override
	public Method getMethod() {
		return Method.GET;
	}

	@Override
	public String getPath() {
		try {
			return "/endpoint/" + getConfig(Input.ENDPOINT_NAME)
					+ "/activation_requirements";
		} catch (CommandConfigException e) {
			// should not happen
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void init() throws InitException {
		// not necessary
	}

	@Override
	protected void processResult() {

		String myProxyServer = null;
		try {
			JSONObject jobj = result.getJSONObject(0);
			if (jobj.get("DATA") != null) {
				myProxyServer = extractFromResults("DATA", "value");
			}
		} catch (Exception e) {
		}

		System.out.println("MyProxy: " + myProxyServer);
	}

}
