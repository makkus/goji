package nz.org.nesi.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

import nz.org.nesi.goji.GO_PARAM;
import nz.org.nesi.goji.exceptions.CommandConfigException;
import nz.org.nesi.goji.exceptions.InitException;
import nz.org.nesi.goji.exceptions.RequestException;
import nz.org.nesi.goji.model.Endpoint;

import org.apache.commons.lang.StringUtils;
import org.globusonline.transfer.APIError;
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

	public static final String name = AbstractCommand.class.getSimpleName();

	protected final BaseTransferAPIClient client;

	protected JSONArray result = null;

	private Map<GO_PARAM, String> config;
	private Map<GO_PARAM, String> output = null;
	private String jsonData = null;

	private int responseCode = NOT_CALLED;

	static final Logger myLogger = LoggerFactory
			.getLogger(AbstractCommand.class);


	/**
	 * Constructor that doesn't also executes the command.
	 * 
	 * @param client
	 *            the client
	 */
	public AbstractCommand(BaseTransferAPIClient client) {

		myLogger.debug("Creating GO command: " + name);
		this.client = client;

	}

	public AbstractCommand(BaseTransferAPIClient client, GO_PARAM configInput,
			String configValue) {
		this(client,
				new ImmutableMap.Builder<GO_PARAM, String>().put(configInput,
						configValue).build());
	}

	public AbstractCommand(BaseTransferAPIClient client,
			GO_PARAM inputKey1, String input1, GO_PARAM inputKey2, String input2) {
		this(client,
				new ImmutableMap.Builder<GO_PARAM, String>().put(inputKey1,
						input1).put(inputKey2, input2).build());
	}

	public AbstractCommand(BaseTransferAPIClient client,
			Map<GO_PARAM, String> config) {

		myLogger.debug("Creating GO command: " + name);
		this.client = client;

		setConfig(config);
		execute();
	}

	public void execute() {

		myLogger.debug("Initializing GO command: " + name);
		init();
		myLogger.debug("Executing GO command: " + name + " using path: "
				+ getPath());
		try {
			HttpsURLConnection c = client.request(this.getMethodType()
					.toString(), this.getPath(), getJsonData(), null);
			myLogger.debug("Executed GO command: " + name);
			setResult(c);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (APIError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (RequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	 * Throws {@link CommandConfigException} if no such config parameter exists.
	 * 
	 * @param key
	 *            the parameter
	 * @return the value of the config parameter
	 * @throws CommandConfigException
	 *             if no such config parameter exists
	 */
	public String getConfig(GO_PARAM key) {
		if (config != null) {
			String result = config.get(key);
			if (StringUtils.isBlank(result) || NO_VALUE.equals(result)) {
				return null;
			} else {
				return result;
			}

		} else {
			throw new CommandConfigException("Config not valid");
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
	abstract public Method getMethodType();

	/**
	 * Returns all processed output values.
	 * 
	 * Those values are computed from the GO result JSON data in the
	 * {@link #processResult()} method of the implementing class.
	 * 
	 * @return all processed output values
	 */
	public Map<GO_PARAM, String> getOutput() {
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
	public String getOutput(GO_PARAM key) {
		return output.get(key);
	}

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
	protected abstract void init() throws InitException;

	protected abstract void processResult() throws RequestException;

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
	protected void putOutput(GO_PARAM key, String value) {
		if (output == null) {
			throw new IllegalStateException("Result not set yet.");
		}
		this.output.put(key, value);
	}

	public void setConfig(Map<GO_PARAM, String> config) {
		output = null;
		this.result = null;
		this.responseCode = NOT_CALLED;
		this.config = config;
	}


	public void setParameter(GO_PARAM param, String value) {
		if ( config == null ) {
			config = Maps.newHashMap();
		}
		config.put(param, value);
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
			output = new TreeMap<GO_PARAM, String>();
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
