package nz.org.nesi.goji.model.commands;

import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.exceptions.InitException;

import org.apache.commons.lang.StringUtils;
import org.bestgrid.goji.utils.EndpointHelpers;
import org.globusonline.transfer.BaseTransferAPIClient;

public class ActivationRequirements extends AbstractCommand {

	public ActivationRequirements(BaseTransferAPIClient client) {
		super(client);
	}

	public ActivationRequirements(BaseTransferAPIClient client, String endpoint)
			throws CommandException {
		super(client, PARAM.ENDPOINT_NAME, endpoint);
	}

	@Override
	protected PARAM[] getInputParameters() {
		return new PARAM[] { PARAM.ENDPOINT_NAME };
	}

	@Override
	public Method getMethodType() {
		return Method.GET;
	}

	@Override
	protected PARAM[] getOptionalParameters() {
		return new PARAM[]{};
	}

	@Override
	protected PARAM[] getOutputParamets() {
		return new PARAM[]{PARAM.MYPROXY_HOST};
	}

	@Override
	public String getPath() {
		return "/endpoint/"
				+ EndpointHelpers.encode(getConfig(PARAM.ENDPOINT_NAME))
				+ "/activation_requirements";

	}

	@Override
	protected void initialize() throws InitException {
		// not necessary
	}

	@Override
	protected void processResult() {

		String myProxyServer = extractFromResults("DATA", "value");
		if (StringUtils.isNotBlank(myProxyServer)) {
			putOutput(PARAM.MYPROXY_HOST, myProxyServer);
		}

		// TODO extract more values if necessary
	}

	public void setEndpoint(String epName) {
		try {
			setParameter(PARAM.ENDPOINT_NAME, epName);
		} catch (CommandException e) {
		}
	}


}
