package org.bestgrid.goji.commands;

import org.bestgrid.goji.GO_PARAM;
import org.bestgrid.goji.exceptions.CommandConfigException;
import org.bestgrid.goji.exceptions.InitException;
import org.globusonline.GojiTransferAPIClient;

public class EndpointRemove extends AbstractCommand {


	public EndpointRemove(GojiTransferAPIClient client, String endpointName) {
		super(client, GO_PARAM.ENDPOINT_NAME, endpointName);
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
			return "/endpoint/" + getConfig(GO_PARAM.ENDPOINT_NAME);
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
			putOutput(GO_PARAM.TYPE, type);
			if (type.equals("result")) {
				putOutput(GO_PARAM.MESSAGE, extractFromResults("message"));
				putOutput(GO_PARAM.CODE, extractFromResults("code"));
				putOutput(GO_PARAM.RESOURCE, extractFromResults("resource"));
				putOutput(GO_PARAM.REQ_ID, extractFromResults("request_id"));
			} else {
				System.out.println("Got unknown result type: " + result);
			}
		}
	}

	@Override
	public String toString() {
		StringBuffer strbuf = new StringBuffer("\n");
		strbuf.append(getOutput(GO_PARAM.MESSAGE));
		strbuf.append("\n");
		return strbuf.toString();
	}

}
