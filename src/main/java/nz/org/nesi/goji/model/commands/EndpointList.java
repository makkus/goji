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

import java.util.Map;
import java.util.TreeMap;

import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.model.Endpoint;

import org.globusonline.transfer.BaseTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class EndpointList extends AbstractCommand {

	private Map<String, Endpoint> endpoints;

	public EndpointList(BaseTransferAPIClient client) {
		super(client);
		try {
			init(null);
		} catch (CommandException e) {
			// should be fine
		}
	}

	public Map<String, Endpoint> getEndpoints() {
		return endpoints;
	}

	@Override
	protected PARAM[] getInputParameters() {
		return new PARAM[] {};
	}

	@Override
	public String getJsonData() {
		// not necessary
		return null;
	}

	@Override
	public Method getMethodType() {
		return Method.GET;
	}

	@Override
	protected PARAM[] getOptionalParameters() {
		return new PARAM[] {};
	}

	@Override
	protected PARAM[] getOutputParamets() {
		return new PARAM[] {};
	}

	@Override
	public String getPath() {
		return "/endpoint_list?limit=1000";
	}

	@Override
	public void initialize() {
		// not necessary
	}

	@Override
	protected void processResult() {

		endpoints = new TreeMap<String, Endpoint>();

		for (int i = 0; i < result.length(); i++) {
			try {
				JSONObject o = result.getJSONObject(i);
				JSONArray dataArr = o.getJSONArray("DATA"), dataArr2 = null;
				for (int j = 0; j < dataArr.length(); j++) {
					JSONObject data = dataArr.getJSONObject(j);
					Endpoint ep = new Endpoint(data, client.getUsername());
					endpoints.put(ep.getName(), ep);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
