package org.bestgrid.goji.commands;

import java.util.Map;
import java.util.TreeMap;

import org.bestgrid.goji.Endpoint;
import org.bestgrid.goji.exceptions.CommandConfigException;
import org.bestgrid.goji.exceptions.InitException;
import org.globusonline.GojiTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;

/**
 * This is the parent class for all classes that model a call to the GO REST
 * API.
 * 
 * It does some general housekeeping and controls the basic workflow: set
 * config, call GO, parse results.
 * 
 * GO is already called in the constructor. If the call fails it can be due to a
 * config error or a runtime error. You need to explicitly catch those when
 * creating a Command object, since in both cases runtime exceptions are thrown.
 * 
 * After a successful GO call, the object holds a set of meaningful (for this
 * method) output key/value pairs and also, on a per implementation basis,
 * created java objects like {@link Endpoint}s and such.
 * 
 * @author Markus Binsteiner
 * 
 */
public abstract class AbstractCommand {

	enum Input {

		ENDPOINT_NAME,
		GRIDFTP_SERVER,
		MYPROXY_SERVER,
		SERVER_DN,
		IS_GLOBUS_CONNECT,
		IS_PUBLIC;
	}

	public enum Method {
		GET,
		POST,
		DELETE,
		PUT
	}

	enum Output {
		TYPE,
		MESSAGE,
		CODE,
		RESOURCE,
		REQ_ID,
		GC_KEY,
		CANONICAL_NAME;

	}

	public static final String NO_VALUE = "no_value";

	protected final GojiTransferAPIClient client;

	protected JSONArray result = null;

	private final Map<Input, String> config;
	private Map<Output, String> output = null;
	private String jsonData = null;

	public AbstractCommand(GojiTransferAPIClient client) {
		this(client, null);
	}

	public AbstractCommand(GojiTransferAPIClient client, Input configInput,
			String configValue) {
		this(client,
				new ImmutableMap.Builder<Input, String>().put(configInput,
						configValue).build());
	}

	public AbstractCommand(GojiTransferAPIClient client,
			Map<Input, String> config) {
		this.client = client;
		this.config = config;
		init();
		client.request(this);
	}

	/**
	 * Helper method to extract results from the json object that GO returns.
	 * 
	 * @param parameter
	 *            the parameter to extract
	 * @return the value
	 */
	protected String extractFromResults(String parameter)
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

	/**
	 * Helper method to extract results from within a child jsonarray of the
	 * json object that GO returns.
	 * 
	 * @param parameter
	 *            the parameter to extract
	 * @return the value
	 */
	protected String extractFromResults(String arrayName, String parameter)
	{
		String value = null;
		if (result != null)
		{
			try
			{
				JSONObject jsonO = result.getJSONObject(0);
				if (jsonO != null) {
					JSONArray dataArr = jsonO.getJSONArray(arrayName);
					if ( dataArr != null ) {

						JSONObject jobj = dataArr.getJSONObject(0);
						if (jobj.get(parameter) != null)
						{
							value = jobj.get(parameter).toString();
						}
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return value;
	}

	/**
	 * Returns the value of the given config parameter.
	 * 
	 * Throws {@link CommandConfigException} if no such config parameter exists.
	 * 
	 * @param key
	 *            the parameter
	 * @return the value of the config parameter
	 * @throws CommandConfigException
	 *             if no such config parameter exists
	 */
	public String getConfig(Input key) throws CommandConfigException {
		if (config != null) {
			return config.get(key);
		} else {
			throw new CommandConfigException("Key " + key + " not in config");
		}
	}

	/**
	 * Returns the json data that was prepared by the implementing class (in the
	 * {@link #init()} method} and that is needed for executing the query.
	 * 
	 * @return the json data
	 */
	public String getJsonData() {
		return jsonData;
	}

	/**
	 * Whether you want to call your method via GET, POST, DELETE, PUT....
	 * 
	 * @return the method
	 */
	abstract public Method getMethod();

	/**
	 * Returns all processed output values.
	 * 
	 * Those values are computed from the GO result JSON data in the
	 * {@link #processResult()} method of the implementing class.
	 * 
	 * @return all processed output values
	 */
	public Map<Output,String> getOutput() {
		return output;
	}

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
	public String getOutput(Output key) {
		return output.get(key);
	}

	/**
	 * Returns the path of the REST query.
	 * 
	 * @return the path
	 */
	abstract public String getPath();

	/**
	 * Init things you might need to do.
	 * 
	 * Maybe also check whether config is valid or create json data for
	 * {@link #getJsonData()}.
	 * 
	 * If you need to populated jsondata, you need to do it via
	 * {@link #putJsonData(String)}.
	 * 
	 * @throws InitException
	 *             if initialization can't be done
	 */
	protected abstract void init() throws InitException;

	protected abstract void processResult();

	/**
	 * For more complex commands you need to create the JsonData that gets sent
	 * as argument to the rest api.
	 * 
	 * Might be not necessary though. Should be, if possible, calculated in the
	 * {@link #init()} method.
	 * 
	 * @param the json data
	 */
	protected void putJsonData(String jsonData) {
		this.jsonData = jsonData;
	}

	/**
	 * Use this method to add processed output values to the output value set of
	 * this command.
	 * 
	 * Those output values need to be strings and can be used to do audits or
	 * somesuch. If the call creates other values (like a list of
	 * {@link Endpoint}s, you should create an extra field and getter for it in
	 * the implementing class.
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 */
	protected void putOutput(Output key, String value) {
		if (output == null) {
			throw new IllegalStateException("Result not set yet.");
		}
		this.output.put(key, value);
	}


	/**
	 * Called by the {@link GojiTransferAPIClient} after the call succeeded.
	 * 
	 * Don't call this manually.
	 * 
	 * @param result
	 *            the query result
	 */
	public void setResult(JSONArray result) {
		output = new TreeMap<Output, String>();
		this.result = result;
		processResult();
	}

}
