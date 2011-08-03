package org.bestgrid.goji.commands;

import java.util.Map;
import java.util.TreeMap;

import org.bestgrid.goji.Endpoint;
import org.globusonline.GojiTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class EndpointList extends JGOCommand {

	private Map<String, Endpoint> endpoints;

	public EndpointList(GojiTransferAPIClient client) throws Exception {
		super(client, null);
	}

	public Map<String, Endpoint> getEndpoints() {
		return endpoints;
	}

	@Override
	public String getJsonData() {
		// not necessary
		return null;
	}

	@Override
	public Method getMethod() {
		return Method.GET;
	}

	@Override
	public String getPath() {
		return "/endpoint_list?limit=100";
	}

	@Override
	public void init() {
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
					endpoints.put(ep.name, ep);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
