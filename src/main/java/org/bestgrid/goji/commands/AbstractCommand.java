package org.bestgrid.goji.commands;

import java.util.Map;
import java.util.TreeMap;

import org.bestgrid.goji.exceptions.CommandConfigException;
import org.bestgrid.goji.exceptions.InitException;
import org.globusonline.GojiTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONObject;


public abstract class AbstractCommand {

	public enum Method {
		GET,
		POST,
		DELETE,
		PUT
	}

	public static final String NO_VALUE = "no_value";

	protected final GojiTransferAPIClient client;

	protected String path = null;

	protected JSONArray result = null;

	private final Map<String, String> config;
	private Map<String, String> output = null;

	public AbstractCommand(GojiTransferAPIClient client) {
		this(client, null);
	}

	public AbstractCommand(GojiTransferAPIClient client,
			Map<String, String> config) {
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

	public String getConfig(String key) throws CommandConfigException {
		if (config != null) {
			return config.get(key);
		} else {
			throw new CommandConfigException("Key " + key + " not in config");
		}
	}

	/**
	 * For more complex commands you need to create the JsonData that gets sent
	 * as argument to the rest api.
	 * 
	 * Might be not necessary though. Should be, if possible, calculated in the
	 * {@link #init()} method.
	 * 
	 * @return the json data
	 */
	abstract public String getJsonData();

	/**
	 * Whether you want to call your method via GET, POST, DELETE, PUT....
	 * 
	 * @return the method
	 */
	abstract public Method getMethod();

	/**
	 * Returns a string representation of the output value for the specified
	 * key.
	 * 
	 * Needs to be populated/implemented in the {@link #processResult()} method.
	 * 
	 * @param key
	 *            the key for the output value you want to know
	 * @return the value of the output value
	 */
	public String getOutput(String key) {
		return output.get(key);
	}

	public String getPath() {
		return path;
	}

	/**
	 * Optional init things you might need to do. Maybe check whether config is
	 * valid or create json data for {@link #getJsonData()}.
	 * 
	 * @throws InitException
	 *             if initialization can't be done
	 */
	protected abstract void init() throws InitException;

	protected abstract void processResult();

	protected void putOutput(String key, String value) {
		if (output == null) {
			throw new IllegalStateException("Result not set yet.");
		}
		this.output.put(key, value);
	}

	public void setResult(JSONArray result) {
		output = new TreeMap<String, String>();
		this.result = result;
		processResult();
	}

}
