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
import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.exceptions.InitException;

import org.globusonline.transfer.BaseTransferAPIClient;

public class Deactivate extends AbstractCommand {

	public Deactivate(BaseTransferAPIClient client) {
		super(client);
	}

	public Deactivate(BaseTransferAPIClient client, String endpointName)
			throws CommandException {
		super(client, PARAM.ENDPOINT_NAME, endpointName);
	}

	@Override
	protected PARAM[] getInputParameters() {
		return new PARAM[] { PARAM.ENDPOINT_NAME };
	}

	@Override
	public String getJsonData() {
		// not necessary
		return null;
	}

	@Override
	public Method getMethodType() {
		return Method.POST;
	}

	@Override
	protected PARAM[] getOptionalParameters() {
		return new PARAM[] {};
	}

	@Override
	protected PARAM[] getOutputParamets() {
		return new PARAM[] { PARAM.MESSAGE, PARAM.CODE, PARAM.RESOURCE,
				PARAM.REQ_ID };
	}

	@Override
	public String getPath() {
		return "/endpoint/"
				+ EndpointHelpers.encode(getConfig(PARAM.ENDPOINT_NAME))
				+ "/deactivate";
	}

	@Override
	protected void initialize() throws InitException {
	}

	@Override
	protected void processResult() {

		if (result != null) {
			String type = extractFromResults("DATA_TYPE");
			putOutput(PARAM.TYPE, type);
			if (type.equals("result")) {
				putOutput(PARAM.MESSAGE, extractFromResults("message"));
				putOutput(PARAM.CODE, extractFromResults("code"));
				putOutput(PARAM.RESOURCE, extractFromResults("resource"));
				putOutput(PARAM.REQ_ID, extractFromResults("request_id"));
			} else {
				System.out.println("Got unknown result type: " + result);
			}
		}
	}

	public void setEndpoint(String epName) {
		try {
			setParameter(PARAM.ENDPOINT_NAME, epName);
		} catch (CommandException e) {
		}
	}

	@Override
	public String toString() {
		StringBuffer strbuf = new StringBuffer("\n");
		strbuf.append(getOutput(PARAM.MESSAGE));
		strbuf.append("\n");
		return strbuf.toString();
	}

}
