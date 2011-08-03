package org.bestgrid.goji.commands;

import org.bestgrid.goji.exceptions.CommandConfigException;
import org.bestgrid.goji.exceptions.InitException;
import org.globusonline.GojiTransferAPIClient;

public class EndpointRemove extends AbstractCommand {


	public EndpointRemove(GojiTransferAPIClient client, String endpointName) {
		super(client, Input.ENDPOINT_NAME, endpointName);
	}

	@Override
	public String getJsonData() {
		// not necessary
		return null;
	}

	@Override
	public Method getMethod() {
		return Method.DELETE;
	}

	@Override
	public String getPath() {
		try {
			return "/endpoint/" + getConfig(Input.ENDPOINT_NAME);
		} catch (CommandConfigException e) {
			throw new InitException(e);
		}

	}

	@Override
	protected void init() throws InitException {
	}

	@Override
	protected void processResult() {

		if (result != null) {
			String type = extractFromResults("DATA_TYPE");
			putOutput(Output.TYPE, type);
			if (type.equals("result")) {
				putOutput(Output.MESSAGE, extractFromResults("message"));
				putOutput(Output.CODE, extractFromResults("code"));
				putOutput(Output.RESOURCE, extractFromResults("resource"));
				putOutput(Output.REQ_ID, extractFromResults("request_id"));
			} else {
				System.out.println("Got unknown result type: " + result);
			}
		}
	}

	@Override
	public String toString() {
		StringBuffer strbuf = new StringBuffer("\n");
		strbuf.append(getOutput(Output.MESSAGE));
		strbuf.append("\n");
		return strbuf.toString();
	}

}
