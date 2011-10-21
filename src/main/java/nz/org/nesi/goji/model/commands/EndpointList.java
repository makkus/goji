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
		return new PARAM[]{};
	}

	@Override
	protected PARAM[] getOutputParamets() {
		return new PARAM[]{};
	}

	@Override
	public String getPath() {
		return "/endpoint_list?limit=100";
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
