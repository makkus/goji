package nz.org.nesi.goji.control;

import grisu.jcommons.exceptions.CredentialException;
import grisu.jcommons.model.info.GFile;
import grith.jgrith.plainProxy.LocalProxy;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import nz.org.nesi.goji.Goji;
import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.model.Credential;
import nz.org.nesi.goji.model.Endpoint;
import nz.org.nesi.goji.model.Transfer;
import nz.org.nesi.goji.model.commands.AbstractCommand;
import nz.org.nesi.goji.model.commands.Activate;
import nz.org.nesi.goji.model.commands.EndpointList;
import nz.org.nesi.goji.model.commands.LsCommand;
import nz.org.nesi.goji.model.commands.PARAM;
import nz.org.nesi.goji.model.commands.TransferCommand;

import org.apache.commons.lang.StringUtils;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.python.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class GlobusOnlineSession {

	private final Logger myLogger = LoggerFactory
			.getLogger(GlobusOnlineSession.class);

	private final BaseTransferAPIClient client;
	private final String go_username;
	private final String go_url;

	private Set<Endpoint> endpoints;

	private Credential credential = null;

	/**
	 * This constructor requires you to have a valid local proxy on the default
	 * globus location (e.g. /tmp/x509u... on linux).
	 * 
	 * Also uploads the proxy to MyProxy to make it accessible for GlobusOnline.
	 * 
	 * @param go_username
	 *            the GlobusOnline username
	 * @throws CredentialException
	 *             if no valid local proxy exists
	 */
	public GlobusOnlineSession(String go_username) throws CredentialException {
		this(go_username, LocalProxy.PROXY_FILE);
	}

	/**
	 * Creates a GlobusUserSession object.
	 * 
	 * Internally, this creates an instance of {@link JSONTransferAPIClient} and
	 * also ensures that the provided credential is uploaded to MyProxy.
	 * 
	 * @param go_username
	 *            the GlobusOnline username
	 * @param cred
	 *            the credential to use for this GO session
	 * @throws CredentialException
	 *             if MyProxy delegation of credential fails
	 */
	public GlobusOnlineSession(String go_username, Credential cred) throws CredentialException {
		this(go_username, cred, null);
	}

	/**
	 * Creates a GlobusUserSession object.
	 * 
	 * Internally, this creates an instance of {@link JSONTransferAPIClient}
	 * also ensures that the provided credential is uploaded to MyProxy.
	 * 
	 * @param go_username
	 *            the GlobusOnline username
	 * @param cred
	 *            the credential to use for this GO session
	 * @param go_url
	 *            the GO REST API url
	 * @throws CredentialException
	 *             if MyProxy delegation of credential fails
	 */
	public GlobusOnlineSession(String go_username, Credential cred,
			String go_url) throws CredentialException {
		this.go_username = go_username;
		if (StringUtils.isBlank(go_url)) {
			this.go_url = Goji.DEFAULT_BASE_URL;
		} else {
			this.go_url = go_url;
		}

		this.credential = cred;
		try {
			client = new JSONTransferAPIClient(go_username,
					System.getProperty("user.home") + File.separator
					+ ".globus" + File.separator + "certificates"
					+ File.separator + "gd_bundle.crt",
					cred.getLocalPath(), cred.getLocalPath(),
					Goji.DEFAULT_BASE_URL);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// retrieving endpoints in background
		loadEndpoints(false);

		cred.uploadMyProxy();

	}

	/**
	 * For this constructor you need to provide the path to a valid proxy.
	 * 
	 * Uploads the proxy to MyProxy to make it accessible for GlobusOnline.
	 * 
	 * @param go_username
	 *            the GlobusOnline username
	 * @param pathToCredential
	 *            the path to a local valid proxy certificate
	 * @throws CredentialException
	 */
	public GlobusOnlineSession(String go_username, String pathToCredential)
			throws CredentialException {
		this(go_username, new Credential(pathToCredential),
				Goji.DEFAULT_BASE_URL);
	}

	/**
	 * Activates all of the users endpoints, using the session credential.
	 * 
	 * If some endpoints need a different credential, they need to be activated
	 * manually. This method doesn't re-activate an endpoint that is already
	 * activated.
	 * 
	 * @throws CommandException
	 *             if not all endpoints could be activated.
	 */
	public void activateAllUserEndpoints() throws CommandException {

		for (Endpoint e : getAllUserEndpoints(false)) {

			myLogger.debug("Activating endpoint: " + e.getName());
			// we can use the session credential here, might not be what needs
			// to be done tough...
			activateEndpoint(e.getName(), getCredential());
		}

		// update internal endpoint list
		// we don't need to force since if an endpoint was activated the
		// endpoints variable would be null...
		loadEndpoints(false);
	}

	public void activateEndpoint(String ep, Credential cred)
			throws CommandException {
		activateEndpoint(ep, cred, false);
	}

	/**
	 * Activates the endpoint with the provided credential or tries to
	 * auto-activate if provided credential is null.
	 * 
	 * @param ep
	 *            the endpoint name
	 * @param cred
	 *            the credential
	 * @param forceReactivate
	 *            whether to re-activate the endpoint even if it is already
	 *            activated
	 * @throws CommandException
	 *             if the endpoint can't be activated for some reason
	 */
	public void activateEndpoint(String ep, Credential cred,
			boolean forceReactivate)
					throws CommandException {

		if (forceReactivate || !isActivated(ep)) {
			activateEndpoint(ep, cred);
			loadEndpoints(true);
			return;
		} else {
			return;
		}
	}

	/**
	 * Activates an endpoint.
	 * 
	 * Re-activates an endpoint even if it is already activated.
	 * 
	 * @param ep
	 *            the name of the endpoint
	 * @param cred
	 *            the credential to use
	 * @throws CommandException
	 *             if the Endpoint can't be activated
	 */
	public void activateOrReactivateEndpoint(String ep, Credential cred)
			throws CommandException {
		Activate a = newCommand(Activate.class);
		a.setEndpoint(ep);
		a.setCredential(cred);
		a.execute();
		// endpoint list needs to be refreshed now
		endpoints = null;
	}

	public AbstractCommand execute(Class commandClass, Map<PARAM, String> config)
			throws CommandException {
		AbstractCommand c = newCommand(commandClass, config);
		c.execute();
		return c;
	}

	public Set<Endpoint> getAllEndpoints() throws CommandException {
		return getAllEndpoints(false);
	}

	/**
	 * Getting all the users' endpoints.
	 * 
	 * @param forceRefresh
	 *            whether to force a refresh even if this was called before
	 * @return all endpoints
	 * @throws CommandException
	 *             if the endpoints can't be retrieved
	 */
	public synchronized Set<Endpoint> getAllEndpoints(boolean forceRefresh)
			throws CommandException {
		if ((endpoints == null) || forceRefresh) {
			EndpointList el = newCommand(EndpointList.class);
			el.execute();
			endpoints = Sets.newTreeSet(el.getEndpoints().values());
		}
		return endpoints;
	}

	/**
	 * Gets all of the endpoints that are owned by the user.
	 * 
	 * Doesn't refresh the endpoint list if it is already loaded.
	 * 
	 * @return the users' endpoints
	 * @throws CommandException
	 *             if the endpoints can't be retrieved
	 */
	public Set<Endpoint> getAllUserEndpoints() throws CommandException {
		return getAllUserEndpoints(false);
	}

	/**
	 * Gets all of the endpoints that are owned by the user.
	 * 
	 * @param forceRefresh
	 *            whether to force a refresh even if this was called before
	 *            (usually you don't need to do that since this class internally
	 *            tries to keep this kind of info up-to-date).
	 * @return the users' endpoints
	 * @throws CommandException
	 *             if the endpoints can't be retrieved
	 */
	public Set<Endpoint> getAllUserEndpoints(boolean forceRefresh)
			throws CommandException {

		Set<Endpoint> result = Sets.newTreeSet();

		for (Endpoint e : getAllEndpoints(forceRefresh)) {

			if (this.go_username.equals(e.getUsername())) {
				result.add(e);
			}
		}
		return result;
	}

	public Credential getCredential() {
		return this.credential;
	}

	public String getGlobusOnlineUsername() {
		return this.go_username;
	}

	public Transfer getTransfer(String taskId) {

		return new Transfer(client, taskId);

	}

	public boolean isActivated(String ep) throws CommandException {

		for (Endpoint e : getAllEndpoints(false) ) {

			if (e.equals(ep)) {
				return e.isActivated();
			}
		}
		throw new CommandException("No endpoint with name " + ep + " found.");
	}

	public SortedSet<GFile> listDirectory(String ep, String path)
			throws CommandException {

		LsCommand ls = newCommand(LsCommand.class);
		ls.setEndpoint(ep);
		ls.setPath(path);

		ls.execute();

		return ls.getFiles();

	}

	private void loadEndpoints(final boolean force) {
		new Thread() {
			@Override
			public void run() {
				try {
					getAllEndpoints(force);
				} catch (CommandException e) {
					myLogger.error("Can't update endpoints.", e);
				}
			}
		}.run();
	}

	public <T extends AbstractCommand> T newCommand(Class<T> commandClass) {

		try {
			Constructor c = commandClass
					.getConstructor(BaseTransferAPIClient.class);

			AbstractCommand com = (AbstractCommand) c.newInstance(this.client);

			return commandClass.cast(com);

		} catch (Exception e) {
			myLogger.error(
					"Can't create command " + commandClass.getSimpleName(), e);
			throw new RuntimeException(e);
		}

	}

	public <T extends AbstractCommand> T newCommand(Class<T> commandClass,
			Map<PARAM, String> config) throws CommandException {

		AbstractCommand c = newCommand(commandClass);
		c.init(config);

		return commandClass.cast(c);

	}

	public Transfer transfer(String sourceUrl, String targetUrl)
			throws CommandException {

		TransferCommand t = newCommand(TransferCommand.class);
		t.addTransfer(sourceUrl, targetUrl);

		t.execute();

		return new Transfer(client, t.getTaskId());

	}

}
