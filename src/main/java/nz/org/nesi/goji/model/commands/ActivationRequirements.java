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
