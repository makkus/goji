package org.globusonline.commands;

import org.globusonline.GojiTransferAPIClient;

import com.google.common.collect.ImmutableMap;

public class EndpointRemove extends JGOCommand {

	public static final String ENDPOINT_NAME = "endpoint_name";

	public static final String TYPE = "type";
	public static final String MESSAGE = "msg";
	public static final String CODE = "code";
	public static final String RESOURCE = "resource";
	public static final String REQ_ID = "req_id";

	public EndpointRemove(GojiTransferAPIClient client, String endpointName)
			throws Exception {
		super(client, new ImmutableMap.Builder<String, String>().put(
				ENDPOINT_NAME, endpointName).build());
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
		return "/endpoint/" + getConfig(ENDPOINT_NAME);
	}

	@Override
	protected void init() throws Exception {
		// not necessary
	}

	@Override
	protected void processResult() {

		if (result != null) {
			String type = extractFromResults("DATA_TYPE");
			putOutput(TYPE, type);
			if (type.equals("result")) {
				putOutput(MESSAGE, extractFromResults("message"));
				putOutput(CODE, extractFromResults("code"));
				putOutput(RESOURCE, extractFromResults("resource"));
				putOutput(REQ_ID, extractFromResults("request_id"));
			} else {
				System.out.println("Got unknown result type: " + result);
			}
		}
	}

	@Override
	public String toString() {
		StringBuffer strbuf = new StringBuffer("\n");
		strbuf.append(getOutput(MESSAGE));
		strbuf.append("\n");
		return strbuf.toString();
	}

}
