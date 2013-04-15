package nz.org.nesi.goji.control;

import grisu.jcommons.exceptions.CredentialException;
import grisu.jcommons.model.info.GFile;
import grisu.jcommons.utils.EndpointHelpers;
import grisu.model.info.dto.Directory;
import grisu.model.info.dto.FileSystem;
import grisu.model.info.dto.Group;
import grith.jgrith.cred.Cred;
import grith.jgrith.cred.ProxyCred;
import grith.jgrith.cred.X509Cred;
import grith.jgrith.plainProxy.LocalProxy;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import nz.org.nesi.goji.Goji;
import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.model.Endpoint;
import nz.org.nesi.goji.model.Transfer;
import nz.org.nesi.goji.model.commands.AbstractCommand;
import nz.org.nesi.goji.model.commands.Activate;
import nz.org.nesi.goji.model.commands.Deactivate;
import nz.org.nesi.goji.model.commands.EndpointAdd;
import nz.org.nesi.goji.model.commands.EndpointList;
import nz.org.nesi.goji.model.commands.EndpointRemove;
import nz.org.nesi.goji.model.commands.LsCommand;
import nz.org.nesi.goji.model.commands.PARAM;
import nz.org.nesi.goji.model.commands.TransferCommand;
import nz.org.nesi.goji.model.events.EndpointActivatedEvent;
import nz.org.nesi.goji.model.events.EndpointActivatingEvent;
import nz.org.nesi.goji.model.events.EndpointDeactivatedEvent;
import nz.org.nesi.goji.model.events.EndpointDeactivatingEvent;

import org.apache.commons.lang.StringUtils;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.python.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.eventbus.EventBus;

public class GlobusOnlineSession {

	private final Logger myLogger = LoggerFactory
			.getLogger(GlobusOnlineSession.class);

	private final BaseTransferAPIClient client;
	private final String go_username;
	private final String go_url;

	private Set<Endpoint> endpoints;

	private final EventBus eventBus = new EventBus();

	private Cred credential = null;

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

	public GlobusOnlineSession(String go_username, char[] certPassphrase)
			throws CredentialException {
		this(go_username, X509Cred.create(certPassphrase),
				Goji.DEFAULT_BASE_URL);
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
	public GlobusOnlineSession(String go_username, Cred cred)
			throws CredentialException {
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
	 * @param endpoint_username
	 *            the username of the user maintaining the endpoints
	 * @param cred
	 *            the credential to use for this GO session
	 * @param go_url
	 *            the GO REST API url
	 * @throws CredentialException
	 *             if MyProxy delegation of credential fails
	 */
	public GlobusOnlineSession(String go_username, Cred cred, String go_url)
			throws CredentialException {

		this.go_username = go_username;
		if (StringUtils.isBlank(go_url)) {
			this.go_url = Goji.DEFAULT_BASE_URL;
		} else {
			this.go_url = go_url;
		}

		this.credential = cred;
		// to make sure we can create JSONTransferAPIClient
		// // TODO use client that can take credential directly
		this.credential.saveProxy();

		try {
			client = new JSONTransferAPIClient(go_username,
					System.getProperty("user.home") + File.separator
							+ ".globus" + File.separator + "certificates"
							+ File.separator + "gd_bundle.crt",
					cred.getProxyPath(), cred.getProxyPath(),
					Goji.DEFAULT_BASE_URL);
			client.setUseMultiThreaded(true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		try {
			// client = new GssJSONTransferAPIClient(go_username,
			// System.getProperty("user.home") + File.separator
			// + ".globus" + File.separator + "certificates"
			// + File.separator + "gd_bundle.crt",
			// cred.getCredential(),
			// Goji.DEFAULT_BASE_URL);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		cred.uploadMyProxy();
		// retrieving endpoints in background
		loadEndpoints(false);

	}

	public void registerForEvents(Object subscriber) {
		eventBus.register(subscriber);
	}

	public void deregisterForEvents(Object subscriber) {
		eventBus.unregister(subscriber);
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
		this(go_username, new ProxyCred(pathToCredential),
				Goji.DEFAULT_BASE_URL);
	}

	// /**
	// * Activates all of the users endpoints, using the session credential.
	// *
	// * If some endpoints need a different credential, they need to be
	// activated
	// * manually. This method doesn't re-activate an endpoint that is already
	// * activated.
	// *
	// * @throws CommandException
	// * if not all endpoints could be activated.
	// */
	// public void activateAllUserEndpoints() throws CommandException {
	//
	// for (Endpoint e : getAllUserEndpoints(false)) {
	//
	// myLogger.debug("Activating endpoint: " + e.getName());
	// // we can use the session credential here, might not be what needs
	// // to be done tough...
	// activateEndpoint(e.getName(), getCredential());
	// }
	//
	// // update internal endpoint list
	// // we don't need to force since if an endpoint was activated the
	// // endpoints variable would be null...
	// loadEndpoints(false);
	// }

	public void activateEndpoints(final String endpoint_username,
			final Collection<Directory> eps, final boolean forceReactivate,
			boolean waitToFinish, boolean reloadEndpoints)
			throws CommandException {

		if (eps.size() == 0) {
			return;
		}

		ExecutorService executor = Executors.newFixedThreadPool(eps.size());

		for (final Directory ep : eps) {

			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						activateEndpoint(endpoint_username, ep,
								forceReactivate, false);
					} catch (CommandException e) {
						e.printStackTrace();
					}
				}
			};
			executor.execute(t);
		}

		executor.shutdown();

		if (waitToFinish) {
			try {
				executor.awaitTermination(10, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (reloadEndpoints) {
				loadEndpoints(true);
			}
		}
	}

	public void deactivateEndpoints(final Collection<String> eps,
			boolean waitToFinish) {

		if (eps.size() == 0) {
			return;
		}

		ExecutorService executor = Executors.newFixedThreadPool(eps.size());

		for (final String ep : eps) {

			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						deactivateEndpoint(ep);
					} catch (CommandException e) {
						e.printStackTrace();
					}
				}
			};
			executor.execute(t);
		}

		executor.shutdown();

		if (waitToFinish) {
			try {
				executor.awaitTermination(10, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Activates the endpoint that is responsible for the provided Directory.
	 * 
	 * For this to work, the endpoint had to be created with the
	 * {@link #addEndpoint(Directory, String)} method.
	 * 
	 * @param d
	 *            the directory
	 * @param forceReactivate
	 *            whether to activate the endpoint even if it is already
	 *            activated
	 * @throws CommandException
	 *             if the endpoint could not be activated
	 */
	public void activateEndpoint(final String endpoint_username,
			final Directory d, final boolean forceReactivate,
			boolean reloadEndpoints) throws CommandException {

		Collection<Group> groups = d.getGroups();
		final Set<String> avail_groups = getCredential().getAvailableFqans()
				.keySet();

		Collection<Group> intersection = Collections2.filter(groups,
				new Predicate<Group>() {

					public boolean apply(Group input) {
						String fqan = input.getFqan();
						if (avail_groups.contains(fqan)) {
							return true;
						} else {
							return false;
						}
					}
				});

		if (intersection.size() == 0) {
			throw new CommandException(
					"No group available to activate endpoint:" + d.getAlias());
		}

		activateEndpoint(endpoint_username + "#" + d.getAlias(), intersection
				.iterator().next().toString(), forceReactivate, reloadEndpoints);

	}

	/**
	 * Dectivates the endpoint that is responsible for the provided Directory.
	 * 
	 * 
	 * @param d
	 *            the directory
	 * @throws CommandException
	 *             if the endpoint could not be activated
	 */
	public void deactivateEndpoint(final String endpoint)
			throws CommandException {

		EndpointDeactivatingEvent ev1 = new EndpointDeactivatingEvent(endpoint);
		eventBus.post(ev1);

		Deactivate d = newCommand(Deactivate.class);
		d.setEndpoint(endpoint);

		d.execute();

		// endpoint list needs to be refreshed now
		invalidateEndpoints();

		EndpointDeactivatedEvent ev2 = new EndpointDeactivatedEvent(endpoint,
				ev1);
		eventBus.post(ev2);

	}

	/**
	 * Activates the specified endpoint with the provided credential.
	 * 
	 * @param ep
	 *            the endpoint name
	 * @param cred
	 *            the credential
	 * @throws CommandException
	 *             if the endpoint can't be activated
	 */
	public void activateEndpoint(String ep, Cred cred) throws CommandException {
		activateEndpoint(ep, cred, false, true);
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
	 * @params reloadEndpoints whether to reload all endpoints when finished
	 * @throws CommandException
	 *             if the endpoint can't be activated for some reason
	 */
	public void activateEndpoint(String ep, Cred cred, boolean forceReactivate,
			boolean reloadEndpoints) throws CommandException {

		synchronized (ep) {

			if (forceReactivate || !isActivated(ep)) {
				activateOrReactivateEndpoint(ep, cred);
				loadEndpoints(true);
				return;
			} else {
				return;
			}
		}
	}

	/**
	 * Activates an endpoint using a voms-credential that is created out of the
	 * session credential.
	 * 
	 * If the credential was already used before no new Credential is created.
	 * 
	 * @param ep
	 *            the name of the endpoint
	 * @param fqan
	 *            the group
	 * @param forceReactivate
	 *            whether to re-activate the endpoint even if it is already
	 * @params reloadEndpoints whether to reload the state of all endpoints once
	 *         finished
	 * @throws CommandException
	 *             if the endpoint can't be found or activated
	 */
	public void activateEndpoint(String ep, String fqan,
			boolean forceReactivate, boolean reloadEndpoints)
			throws CommandException {

		Cred tmp = getCredential().getGroupCredential(fqan);
		tmp.uploadMyProxy();
		activateEndpoint(ep, tmp, forceReactivate, reloadEndpoints);

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
	public void activateOrReactivateEndpoint(String ep, Cred cred)
			throws CommandException {

		EndpointActivatingEvent ev1 = new EndpointActivatingEvent(ep);
		eventBus.post(ev1);

		Activate a = newCommand(Activate.class);
		a.setEndpoint(ep);
		a.setCredential(cred);

		if (cred != null) {
			// make sure the proxy is in MyProxy
			cred.uploadMyProxy();
		}
		a.execute();

		// endpoint list needs to be refreshed now
		invalidateEndpoints();

		EndpointActivatedEvent ev2 = new EndpointActivatedEvent(ep, ev1);
		eventBus.post(ev2);
	}

	/**
	 * Activates an endpoint using a voms-credential that is created out of the
	 * session credential.
	 * 
	 * If the credential was already used before no new Credential is created.
	 * 
	 * @param ep
	 *            the name of the endpoint
	 * @param fqan
	 *            the fqan
	 * @throws CommandException
	 *             if the endpoint can't get activated
	 */
	public void activateOrReactivateEndpoint(String ep, String fqan)
			throws CommandException {

		Cred tmp = getCredential().getGroupCredential(fqan);
		activateOrReactivateEndpoint(ep, tmp);

	}

	/**
	 * Adds an endpoint for the specified directory and uses the directories
	 * alias as the endpoint name.
	 * 
	 * @param d
	 *            the directory
	 * @param myProxyHost
	 *            the default proxy host for this endpoint
	 * @throws CommandException
	 *             if the endpoint could not be created
	 */
	public void addEndpoint(Directory d) throws CommandException {
		addEndpoint(d.getFilesystem(), d.getAlias());
	}

	/**
	 * Adds an endpoint to the list of users endpoints and creates a unique name
	 * for it out of group and hostname.
	 * 
	 * @param fs
	 *            the gridftp filesystem for the endpoint
	 * @param fqan
	 *            the group that is used to access this endpoint
	 * @param myProxyHost
	 *            the MyProxy host to be used with this endpoint
	 * @throws CommandException
	 *             if the endpoint can't be created or the group is not
	 *             available for the user
	 */
	public void addEndpoint(FileSystem fs, String alias)
			throws CommandException {

		addEndpoint(fs.getHost(), alias);

	}

	/**
	 * Adds an endpoint without a default MyProxy server.
	 * 
	 * @param host
	 *            the host
	 * @param epName
	 *            the endpoint name
	 * @throws CommandException
	 *             if the endpoint can't be created
	 */
	public void addEndpoint(String host, String epName) throws CommandException {
		addEndpoint(host, epName, null);
	}

	/**
	 * Adds an endpoint.
	 * 
	 * @param host
	 *            the MyProxy hostname for this endpoint
	 * @param epName
	 *            the name of the endpoint
	 * @throws CommandException
	 *             if the endpoint can't be created
	 */
	public void addEndpoint(String host, String epName, String myProxyHost)
			throws CommandException {

		myLogger.debug("Adding endpoint for: " + epName);

		EndpointAdd ea = newCommand(EndpointAdd.class);
		ea.setGridFtpServer(host);
		ea.setIsPublic(true);
		ea.setMyProxyHost(myProxyHost);
		ea.setGlobusConnect(false);
		ea.setEndpointName(epName);

		ea.execute();

		invalidateEndpoints();
	}

	/**
	 * Creates a Command, configures and executes it.
	 * 
	 * @param commandClass
	 *            the command class
	 * @param config
	 *            the config for the command
	 * @return the Command object
	 * @throws CommandException
	 */
	protected AbstractCommand execute(Class commandClass,
			Map<PARAM, String> config) throws CommandException {
		AbstractCommand c = newCommand(commandClass, config);
		c.execute();
		return c;
	}

	public Set<Endpoint> getAllEndpoints() throws CommandException {
		return getAllEndpoints(false);
	}

	/**
	 * Getting all the endpoints (user endpoints and public ones).
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
	 * @param user
	 *            the username
	 * @return the users' endpoints
	 * @throws CommandException
	 *             if the endpoints can't be retrieved
	 */
	public Set<Endpoint> getAllUserEndpoints(String user)
			throws CommandException {
		return getAllUserEndpoints(user, false);
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
	public Set<Endpoint> getAllUserEndpoints(String user, boolean forceRefresh)
			throws CommandException {

		Set<Endpoint> result = Sets.newTreeSet();

		for (Endpoint e : getAllEndpoints(forceRefresh)) {

			if (user.equals(e.getUsername())) {
				result.add(e);
			}
		}
		return result;
	}

	/**
	 * The credential that was used to connect to GlobusOnline.
	 * 
	 * @return the credential
	 */
	public Cred getCredential() {
		return this.credential;
	}

	// protected String getEndpointName(FileSystem fs, String fqan) {
	// return EndpointHelpers.translateIntoEndpointName(fs.getAlias(), fqan);
	// }

	public Endpoint getEndpoint(String name) throws CommandException {
		return getEndpoint(name, false);
	}

	public Endpoint getEndpoint(String name, boolean refresh)
			throws CommandException {

		String username = EndpointHelpers.extractUsername(name);
		String epName = EndpointHelpers.extractEndpointName(name);

		for (Endpoint ep : getAllEndpoints(refresh)) {
			if (ep.getUsername().equals(username)
					&& ep.getName().equals(epName)) {
				return ep;
			}
		}
		return null;
	}

	/**
	 * Get all fqans (groups) that are available to the user when using the
	 * default session credential and the system-configured VOs (vomses files in
	 * ~/.glite/vomses)
	 * 
	 * @return
	 */
	public Set<String> getFqans() {

		return credential.getAvailableFqans().keySet();
	}

	// /**
	// * The GlobusOnline username for this session.
	// *
	// * @return the username
	// */
	// public String getGlobusOnlineUsername() {
	// return this.go_username;
	// }

	/**
	 * Retrieves a transfer info from GlobusOnline and creates a Transfer object
	 * from it.
	 * 
	 * @param taskId
	 *            the GlobusOnline taskId
	 * @return the (populated) Transfer object
	 */
	public Transfer getTransfer(String taskId) {

		return new Transfer(client, taskId);

	}

	protected void invalidateEndpoints() {
		endpoints = null;
	}

	/**
	 * Returns the state of an endpoint.
	 * 
	 * @param ep
	 *            the name of the endpoint
	 * @return whether the endpoint is currently activated or not.
	 * @throws CommandException
	 *             if no endpoint with the specified name could be found
	 */
	public boolean isActivated(String ep) throws CommandException {

		for (Endpoint e : getAllEndpoints(false)) {

			if (e.equals(ep)) {
				return e.isActivated();
			}
		}
		throw new CommandException("No endpoint with name " + ep + " found.");
	}

	/**
	 * Lists a directory.
	 * 
	 * @param ep
	 *            the name of the endpoint
	 * @param path
	 *            the (absolute) path
	 * @return the file listing
	 * @throws CommandException
	 *             if the file listing command could not be created or exeuted
	 */
	public SortedSet<GFile> listDirectory(String ep, String path)
			throws CommandException {

		LsCommand ls = newCommand(LsCommand.class);
		ls.setEndpoint(ep);
		ls.setPath(path);

		ls.execute();

		return ls.getFiles();

	}

	private void loadEndpoints(final boolean force) {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					getAllEndpoints(force);
				} catch (CommandException e) {
					myLogger.error("Can't update endpoints.", e);
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Creates a new Command without configuring it.
	 * 
	 * @param commandClass
	 *            the command clas
	 * @return the command
	 */
	protected <T extends AbstractCommand> T newCommand(Class<T> commandClass) {

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

	/**
	 * Creates a new Command and applies config to it.
	 * 
	 * @param commandClass
	 *            the command class
	 * @param config
	 *            config for the command
	 * @return the command object
	 * @throws CommandException
	 *             if the command could not be created
	 */
	protected <T extends AbstractCommand> T newCommand(Class<T> commandClass,
			Map<PARAM, String> config) throws CommandException {

		AbstractCommand c = newCommand(commandClass);
		c.init(config);

		return commandClass.cast(c);

	}

	public void removeAllEndpoints(String user) throws CommandException {

		Set<Endpoint> endpoints = getAllUserEndpoints(user, true);
		for (Endpoint ep : endpoints) {
			try {
				removeEndpoint(user + "#" + ep.getName());
			} catch (CommandException e) {
				myLogger.debug("Can't remove endpoint '" + ep.getFullName()
						+ "': " + e.getLocalizedMessage());
			}
		}

	}

	public void removeEndpoint(Directory d) throws CommandException {
		removeEndpoint(d.getAlias());
	}

	public void removeEndpoint(String endpointname) throws CommandException {

		myLogger.debug("Removing endpoint: " + endpointname);
		EndpointRemove er = newCommand(EndpointRemove.class);
		er.setEndpoint(endpointname);
		er.execute();

		invalidateEndpoints();
	}

	/**
	 * Creates a file transfer via GlobusOnline.
	 * 
	 * @param sourceUrl
	 *            the sourceUrl
	 * @param targetUrl
	 *            the targetUrl
	 * @return the {@link Transfer} object
	 * @throws CommandException
	 *             if the transfer couldn't be created
	 */
	public Transfer transfer(String sourceUrl, String targetUrl)
			throws CommandException {

		TransferCommand t = newCommand(TransferCommand.class);
		t.addTransfer(sourceUrl, targetUrl);

		t.execute();

		return new Transfer(client, t.getTaskId());

	}

}
