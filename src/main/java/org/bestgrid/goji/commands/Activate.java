package org.bestgrid.goji.commands;

import org.bestgrid.goji.exceptions.CommandConfigException;
import org.bestgrid.goji.exceptions.InitException;
import org.bestgrid.goji.utils.EndpointHelpers;
import org.globusonline.GojiTransferAPIClient;

public class Activate extends AbstractCommand {

	public Activate(GojiTransferAPIClient client, String endpoint) {
		super(client, Input.ENDPOINT_NAME, EndpointHelpers.removeHash(endpoint));
	}

	@Override
	public Method getMethod() {
		return Method.POST;
	}

	@Override
	public String getPath() {

		try {
			return "/endpoint/" + getConfig(Input.ENDPOINT_NAME) + "/activate";
		} catch (CommandConfigException e) {
			// should not happen
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void init() throws InitException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void processResult() {
		// TODO Auto-generated method stub

	}

}
