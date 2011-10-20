package nz.org.nesi.commands;

import nz.org.nesi.goji.GO_PARAM;
import nz.org.nesi.goji.exceptions.CommandConfigException;
import nz.org.nesi.goji.exceptions.InitException;

import org.apache.commons.lang.StringUtils;
import org.bestgrid.goji.utils.EndpointHelpers;
import org.globusonline.transfer.BaseTransferAPIClient;

public class ActivationRequirements extends AbstractCommand {

	public ActivationRequirements(BaseTransferAPIClient client) {
		super(client);
	}

	public ActivationRequirements(BaseTransferAPIClient client, String endpoint) {
		super(client, GO_PARAM.ENDPOINT_NAME, endpoint);
	}

	@Override
	public Method getMethodType() {
		return Method.GET;
	}

	@Override
	public String getPath() {
		try {
			return "/endpoint/"
					+ EndpointHelpers.encode(getConfig(GO_PARAM.ENDPOINT_NAME))
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

		String myProxyServer = extractFromResults("DATA", "value");
		if (StringUtils.isNotBlank(myProxyServer)) {
			putOutput(GO_PARAM.MYPROXY_HOST, myProxyServer);
		}

		// TODO extract more values if necessary

		// JSONObject obj = null;
		// try {
		// obj = result.getJSONObject(0);
		// } catch (JSONException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// Iterator i = obj.keys();
		//
		// while (i.hasNext()) {
		//
		// Object o = i.next();
		// System.out.println(o.toString());
		//
		// }
		//
		// System.out.println("MyProxy: " + myProxyServer);
	}

}
