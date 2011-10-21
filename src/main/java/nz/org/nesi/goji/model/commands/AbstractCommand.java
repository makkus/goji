package nz.org.nesi.goji.model.commands;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.exceptions.InitException;
import nz.org.nesi.goji.exceptions.RequestException;
import nz.org.nesi.goji.model.Endpoint;

import org.apache.commons.lang.StringUtils;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.python.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	// enum Input {
	//
	// ENDPOINT_NAME,
	// GRIDFTP_SERVER,
	// MYPROXY_HOST,
	// SERVER_DN,
	// IS_GLOBUS_CONNECT,
	// IS_PUBLIC,
	// MYPROXY_USERNAME,
	// MYPROXY_PASSWORD,
	// PROXY_LIFETIME_IN_HOURS;
	// }

	public enum Method {
		GET,
		POST,
		DELETE,
		PUT
	}

	// enum Output {
	// TYPE,
	// MESSAGE,
	// CODE,
	// RESOURCE,
	// REQ_ID,
	// GC_KEY,
	// CANONICAL_NAME,
	// MYPROXY_HOST,
	// SUCCESS,
	// SUBJECT,
	// EXPIRE_TIME;
	// }

	public static final String NO_VALUE = "no_value";
	public static final int NOT_CALLED = Integer.MIN_VALUE;

	public final String name;

	protected final BaseTransferAPIClient client;

	protected JSONArray result = null;

	private Map<PARAM, String> config;
	private Map<PARAM, String> output = null;
	private String jsonData = null;

	private int responseCode = NOT_CALLED;
	private boolean failed = true;
	private Exception exception = null;

	static final Logger myLogger = LoggerFactory
			.getLogger(AbstractCommand.class);


	/**
	 * Constructor that doesn't also executes the command.
	 * 
	 * @param client
	 *            the client
	 */
	public AbstractCommand(BaseTransferAPIClient client) {

		this.name = this.getClass().getSimpleName();
		myLogger.debug("Creating GO command: " + name);
		this.client = client;
	}

	public AbstractCommand(BaseTransferAPIClient client,
			Map<PARAM, String> config) throws CommandException {

		this.name = this.getClass().getSimpleName();
		myLogger.debug("Creating GO command: " + name);
		this.client = client;


		init(config);
		execute();
	}

	public AbstractCommand(BaseTransferAPIClient client, PARAM configInput,
			String configValue) throws CommandException {
		this(client,
				new ImmutableMap.Builder<PARAM, String>().put(configInput,
						configValue).build());
	}

	public AbstractCommand(BaseTransferAPIClient client,
			PARAM inputKey1,
			String input1, PARAM inputKey2, String input2)
					throws CommandException {
		this(client,
				new ImmutableMap.Builder<PARAM, String>().put(inputKey1,
						input1).put(inputKey2, input2).build());
	}

	public void execute() throws CommandException {

		for (PARAM p : getInputParameters()) {
			String value = config.get(p);
			if (StringUtils.isBlank(value)) {
				throw new CommandException("Parameter " + p.toString()
						+ " not set.");
			}
		}

		myLogger.debug("Initializing GO command: " + name);
		try {
			initialize();
		} catch (InitException e) {
			throw new CommandException("Could not initialize command: "
					+ e.getLocalizedMessage(), e);
		}
		myLogger.debug("Executing GO command: " + name + " using path: "
				+ getPath());
		try {
			HttpsURLConnection c = client.request(this.getMethodType()
					.toString(), this.getPath(), getJsonData(), null);
			myLogger.debug("Executed GO command: " + name);
			setResult(c);
			failed = false;
		} catch (Exception e) {
			failed = true;
			exception = e;
			myLogger.debug(
					"Can't execute GO command " + name + ": "
							+ e.getLocalizedMessage(), e);
			throw new CommandException("Could not execute command: "
					+ e.getLocalizedMessage(), e);

		}

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
	 * Throws {@link CommandException} if no such config parameter exists.
	 * 
	 * @param key
	 *            the parameter
	 * @return the value of the config parameter
	 * @throws CommandException
	 *             if no such config parameter exists
	 */
	public String getConfig(PARAM key) {
		if (config != null) {
			String result = config.get(key);
			if (StringUtils.isBlank(result) || NO_VALUE.equals(result)) {
				return null;
			} else {
				return result;
			}

		} else {
			return null;
		}
	}

	/**
	 * All required input parameters for this command.
	 * 
	 * @return input parameters
	 */
	protected abstract PARAM[] getInputParameters();

	/**
	 * Returns the json data that was prepared by the implementing class (in the
	 * {@link #initialize()} method} and that is needed for executing the query.
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
	abstract public Method getMethodType();

	/**
	 * The optional input parameters for this command.
	 * @return optional input parameters
	 */
	protected abstract PARAM[] getOptionalParameters();

	/**
	 * Returns all processed output values.
	 * 
	 * Those values are computed from the GO result JSON data in the
	 * {@link #processResult()} method of the implementing class.
	 * 
	 * @return all processed output values
	 */
	public Map<PARAM, String> getOutput() {
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
	public String getOutput(PARAM key) {
		return output.get(key);
	}

	protected abstract PARAM[] getOutputParamets();

	/**
	 * Returns the path of the REST query.
	 * 
	 * @return the path
	 */
	abstract public String getPath();

	public int getResponseCode() {
		return responseCode;
	}

	public JSONArray getResult() {
		return this.result;
	}

	/**
	 * This (re-)initializes the command.
	 * 
	 * Needs to be called before the command can be executed.
	 * 
	 * @param config
	 *            the config for the command
	 * @throws CommandException
	 */
	public void init(Map<PARAM, String> config) throws CommandException {

		if (config != null) {
			for (PARAM p : config.keySet()) {

				if ((Arrays.binarySearch(getInputParameters(), p) < 0)
						&& (Arrays.binarySearch(getOptionalParameters(), p) < 0)) {
					throw new CommandException("Parameter " + p.toString()
							+ " not a valid input parameter for " + name
							+ " command.");
				}

			}
		}
		this.output = null;
		this.failed = true;
		this.exception = null;

		this.result = null;
		this.responseCode = NOT_CALLED;
		if (config != null) {
			this.config = config;
		} else {
			this.config = Maps.newHashMap();
		}
	}

	/**
	 * Init things you might need to do.
	 * 
	 * Maybe also check whether config is valid {@link #getJsonData()}.
	 * 
	 * If you need to populate jsondata, you need to do it via
	 * {@link #putJsonData(String)}.
	 * 
	 * @throws InitException
	 *             if initialization can't be done
	 */
	protected abstract void initialize() throws InitException;

	/**
	 * Process the results.
	 * 
	 * Results are stored in the {@link #result} variable. Parse the results in this methods and populate all {@link PARAM} that are specified in the {@link #getOutputParamets()} result
	 * in the {@link #output} map (using the {@link #putOutput(PARAM, String) method).
	 * 
	 * @throws RequestException
	 */
	protected abstract void processResult() throws RequestException;

	/**
	 * For more complex commands you need to create the JsonData that gets sent
	 * as argument to the rest api.
	 * 
	 * Might be not necessary though. Should be, if possible, calculated in the
	 * {@link #initialize()} method.
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
	protected void putOutput(PARAM key, String value) {
		if (output == null) {
			throw new IllegalStateException("Result not set yet.");
		}
		this.output.put(key, value);
	}


	/**
	 * Sets or changes a single input parameter.
	 * 
	 * @param param
	 *            the parameter type
	 * @param value
	 *            the value
	 * @throws CommandException
	 */
	public void setParameter(PARAM p, String value) throws CommandException {
		if ( config == null ) {
			init(null);
		}
		if ((Arrays.binarySearch(getInputParameters(), p) < 0)
				&& (Arrays.binarySearch(getOptionalParameters(), p) < 0)) {
			throw new CommandException("Parameter " + p.toString()
					+ " not a valid input parameter for " + name
					+ " command.");
		}
		config.put(p, value);
	}

	/**
	 * Called after the call.
	 * 
	 * Don't call this manually.
	 * 
	 * @param result
	 *            the query result
	 * @param responseCode
	 *            the response code of the call
	 * @throws RequestException
	 */
	private void setResult(HttpsURLConnection c) throws RequestException {
		try {
			output = new TreeMap<PARAM, String>();
			result = null;
			this.responseCode = c.getResponseCode();
			myLogger.debug("Response code for GO command " + responseCode);

			InputStream inputStream = c.getInputStream();
			InputStreamReader reader = new InputStreamReader(inputStream);
			BufferedReader in = new BufferedReader(reader);

			String inputLine = null;
			StringBuffer strbuf = new StringBuffer("[");

			while ((inputLine = in.readLine()) != null) {
				strbuf.append(inputLine);
			}
			strbuf.append("]");
			in.close();


			result = new JSONArray(strbuf.toString());
			processResult();
		} catch (Exception e) {
			throw new RequestException(e);
		}

	}

}
