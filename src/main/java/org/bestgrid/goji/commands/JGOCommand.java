package org.bestgrid.goji.commands;

import java.util.Map;
import java.util.TreeMap;

import org.globusonline.GojiTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONObject;


public abstract class JGOCommand {

	public enum Method {
		GET,
		POST,
		DELETE
	}

	public static final String NO_VALUE = "no_value";

	protected final GojiTransferAPIClient client;

	protected JSONArray result = null;

	private final Map<String, String> config;
	private final Map<String, String> output = new TreeMap<String, String>();

	public JGOCommand(GojiTransferAPIClient client) throws Exception {
		this(client, null);
	}

	public JGOCommand(GojiTransferAPIClient client, Map<String, String> config)
			throws Exception {
		this.client = client;
		this.config = config;
		init();
		client.request(this);
	}

	public String extractFromResults(String parameter)
	{
		String value = null;
		if (result != null)
		{
			try
			{
				JSONObject jobj = result.getJSONObject(0);
				if (jobj.get(parameter) != null)
				{
					value = jobj.get(parameter).toString();
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return value;
	}

	public String getConfig(String key) {
		if (config != null) {
			return config.get(key);
		} else {
			throw new CommandConfigException("Key " + key + " not in config");
		}
	}

	abstract public String getJsonData();

	abstract public Method getMethod();
	public String getOutput(String key) {
		return output.get(key);
	}

	abstract public String getPath();
	protected abstract void init() throws Exception;

	protected abstract void processResult();

	public void putOutput(String key, String value) {
		this.output.put(key, value);
	}

	public void setResult(JSONArray result) {
		this.result = result;
		processResult();
	}

}
