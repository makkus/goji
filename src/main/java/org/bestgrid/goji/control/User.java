package org.bestgrid.goji.control;

import grisu.info.ynfo.YnfoManager;
import grisu.jcommons.interfaces.InfoManager;
import grisu.jcommons.model.info.Directory;
import grisu.jcommons.model.info.FileSystem;
import grisu.jcommons.model.info.GFile;
import grisu.jcommons.utils.FileAndUrlHelpers;
import grith.jgrith.CredentialHelpers;
import grith.jgrith.myProxy.MyProxy_light;
import grith.jgrith.plainProxy.LocalProxy;
import grith.jgrith.plainProxy.PlainProxy;
import grith.jgrith.voms.VO;
import grith.jgrith.voms.VOManagement.VOManagement;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.bestgrid.goji.CredentialException;
import org.bestgrid.goji.GO_PARAM;
import org.bestgrid.goji.Goji;
import org.bestgrid.goji.commands.Activate;
import org.bestgrid.goji.commands.EndpointAdd;
import org.bestgrid.goji.commands.EndpointList;
import org.bestgrid.goji.commands.EndpointRemove;
import org.bestgrid.goji.commands.LsCommand;
import org.bestgrid.goji.commands.TransferCommand;
import org.bestgrid.goji.exceptions.FileSystemException;
import org.bestgrid.goji.exceptions.UserException;
import org.bestgrid.goji.model.Credential;
import org.bestgrid.goji.model.Endpoint;
import org.bestgrid.goji.utils.EndpointHelpers;
import org.globus.common.CoGProperties;
import org.globus.myproxy.MyProxyException;
import org.globusonline.GojiTransferAPIClient;
import org.ietf.jgss.GSSCredential;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class User {

	static final Logger myLogger = Logger.getLogger(User.class.getName());

	// private static InfoManager im = new InfoManagerImpl();
	private static InfoManager im = new YnfoManager(
			"/home/markus/src/infosystems/ynfo/src/test/resources/default_config.groovy");

	private GojiTransferAPIClient client = null;

	private final String go_username;
	private final String endpoint_username;

	private GSSCredential currentProxy = null;
	private final Map<String, Credential> proxies = Maps.newConcurrentMap();

	private Map<String, VO> fqans = null;

	private EndpointList endpointList = null;

	private final Set<Directory> directories = Sets.newTreeSet();

	private final Map<FileSystem, Set<String>> filesystems = Maps.newTreeMap();

	public User(String go_username) throws UserException {
		this(go_username, go_username);
	}

	/**
	 * Convenience constructor to create a user and init it using the password
	 * of a x509 certificate in the default location.
	 * 
	 * A proxy certificate will be created (and possibly an existing one will be
	 * overwritten).
	 * 
	 * @param go_username
	 *            the GlobusOnline username
	 * @param cred_password
	 *            the x509 password
	 */
	public User(String go_username, char[] cred_password) {
		this(go_username, go_username, cred_password);
	}

	/**
	 * Constructor to create and init a user in one go using provided proxy.
	 * 
	 * @param go_username
	 *            the GlobusOnline username
	 * @param proxy
	 *            a (valid) proxy
	 * @throws UserException
	 */
	public User(String go_username, GSSCredential proxy)
			throws UserException {
		this(go_username, go_username, proxy);
	}

	/**
	 * Default constructor for a User.
	 * 
	 * In order to use this constructor, a valid local proxy needs to exist,
	 * otherwise a UserInitException will be thrown.
	 * 
	 * @param go_username
	 *            the GlobusOnline username
	 * @throws UserException
	 */
	public User(String go_username, String endpoint_username)
			throws UserException {
		this.go_username = go_username;
		this.endpoint_username = endpoint_username;

		if (LocalProxy.validGridProxyExists()) {
			init(null);
		} else {
			throw new UserException(
					"No valid proxy credential found. Can't init user.");
		}
	}

	public User(String go_username, String endpoint_username, char[] cred_password) {
		this.go_username = go_username;
		this.endpoint_username = endpoint_username;
		init_x509(cred_password);
	}

	public User(String go_username, String myproxy_username,
			char[] myproxy_password, String myproxy_host, int myproxy_port)
					throws UserException {
		this(go_username, go_username, myproxy_username, myproxy_password,
				myproxy_host, myproxy_port);
	}

	public User(String go_username, String endpoint_username, GSSCredential proxy)
			throws UserException {
		this.go_username = go_username;
		this.endpoint_username = endpoint_username;
		init(proxy);
	}

	public User(String go_username, String endpoint_username, String myproxy_username,
			char[] myproxy_password, String myproxy_host, int myproxy_port)
					throws UserException {
		this.go_username = go_username;
		this.endpoint_username = endpoint_username;
		init_myproxy(myproxy_username, myproxy_password, myproxy_host,
				myproxy_port);
	}

	public boolean activateAllEndpoints() {

		Map<String, Endpoint> allEndpoints = getEndpoints(false);
		boolean success = true;
		for (Directory d : directories) {
			boolean s = activateEndpoint(d, false);
			if (!s) {
				success = false;
			}
		}

		return success;

	}

	/**
	 * Activates the endpoint that is responsible for the specified directory.
	 * 
	 * @param dir
	 *            the directory
	 * @param force
	 *            whether to force (re-)activation even if the endpoint in
	 *            question is already activated
	 * @return whether activation worked
	 */
	public boolean activateEndpoint(Directory dir, boolean force) {

		Endpoint ep = getEndpoint(getEndpointName(dir));

		if (ep.isActivated() && !force) {
			return true;
		}

		try {
			String epName = ep.getUsername() + "#" + ep.getName();


			myLogger.debug("Activating endpoint: " + epName);

			Credential cred = getCredential(dir.getFqan());
			cred.uploadMyProxy();

			Activate a = new Activate(client, epName, cred, 12);
			return true;
		} catch (Exception e) {
			myLogger.debug(e);
			return false;
		}

	}

	public boolean activateEndpoint(String epname_or_url, boolean force)
			throws FileSystemException {

		Directory d = getDirectory(epname_or_url);

		if (d != null) {
			boolean success = activateEndpoint(d, force);
			return success;
		} else {
			return false;
		}

	}

	public void addEndpoint(Directory d) throws UserException {
		addEndpoint(d.getFilesystem().getHost(), d.getFqan());
	}

	public void addEndpoint(String host, String fqan) throws UserException {
		String endpointAlias = EndpointHelpers.translateIntoEndpointName(host,
				fqan);

		addEndpoint(host, fqan, endpointAlias);
	}

	public void addEndpoint(String host, String fqan, String epName)
			throws UserException {

		if (!endpoint_username.equals(go_username)) {
			throw new UserException(
					"Can't create endpoints because endpoint-username is different.");
		}

		myLogger.debug("Adding endpoint for: " + epName);

		EndpointAdd ea = new EndpointAdd(client, host,
				Credential.DEFAULT_MYPROXY_SERVER,
				null, false, true, epName);

		endpointList = null;
	}

	public String cp(List<String> sources, String targetDir)
			throws CredentialException, FileSystemException {

		targetDir = FileAndUrlHelpers.ensureTrailingSlash(targetDir);
		targetDir = ensureGlobusUrl(targetDir);
		List<String> targets = new LinkedList<String>();

		List<String> sourcesNew = new LinkedList<String>();

		for (String s : sources) {

			s = ensureGlobusUrl(s);
			sourcesNew.add(s);

			Directory sourceDir = getDirectory(s);
			activateEndpoint(sourceDir, false);

			String filename = FileAndUrlHelpers.getFilename(s);
			targets.add(targetDir + filename);
		}

		Directory target = getDirectory(targetDir);
		activateEndpoint(target, false);

		TransferCommand tc = new TransferCommand(client, sourcesNew, targets);

		return tc.getOutput(GO_PARAM.TASK_ID);

	}

	public String cp(String source, String target) throws CredentialException,
	FileSystemException {

		Directory sourceDir = getDirectory(source);
		Directory targetDir = getDirectory(target);

		activateEndpoint(sourceDir, false);
		activateEndpoint(targetDir, false);

		TransferCommand tc = new TransferCommand(client,
				ensureGlobusUrl(source), ensureGlobusUrl(target));

		return tc.getOutput(GO_PARAM.TASK_ID);

	}

	public void createAllEndpoints() throws UserException {

		if (!endpoint_username.equals(go_username)) {
			throw new UserException(
					"Can't create endpoints because endpoint-username is different.");
		}

		Map<String, Endpoint> allEndpoints = getEndpoints(true);

		for (FileSystem fs : getFileSystems().keySet()) {

			for (String fqan : getFileSystems().get(fs)) {
				String endpointName = EndpointHelpers
						.translateIntoEndpointName(fs.getHost(), fqan);
				if (!allEndpoints.containsKey(endpointName)) {
					addEndpoint(fs.getHost(), fqan, endpointName);
				}


			}

		}
	}

	public String ensureGlobusUrl(String url) throws FileSystemException {

		Directory d = getDirectory(url);
		if (d == null) {
			throw new FileSystemException(
					"File can't be mapped to one of the existing filesystems: "
							+ url);
		}
		return endpoint_username + "#" + d.getAlias() + d.getPath()
				+ d.getRelativePath(url);
	}

	public String ensureGsiftpUrl(String url) throws FileSystemException {

		Directory d = getDirectory(url);
		return d.getUrl() + d.getRelativePath(url);
	}

	// public Directory findDirectory(String url) {
	//
	// for (Directory d : directories) {
	//
	// String rootUrl = d.getUrl();
	// if ( url.startsWith(rootUrl)) {
	// return d;
	// }
	// }
	// return null;
	// }

	public Map<String, Endpoint> getAllEndpoints() {
		return getAllEndpoints(false);
	}

	/**
	 * Returns a list of all public endpoints as well as the users' private
	 * endpoints.
	 * 
	 * @param force_refresh
	 *            whether to force refresh the list of endpoints
	 * @return all endpoints
	 */
	public Map<String, Endpoint> getAllEndpoints(boolean force_refresh) {

		if (force_refresh || (endpointList == null)) {
			endpointList = new EndpointList(client);
		}

		return endpointList.getEndpoints();
	}

	public GojiTransferAPIClient getClient() {
		return client;
	}

	public Credential getCredential(String fqan) throws CredentialException {

		Credential result = proxies.get(fqan);

		if (result == null) {

			result = new Credential(currentProxy, fqans.get(fqan), fqan);
			proxies.put(fqan, result);
		}

		return proxies.get(fqan);

	}

	/**
	 * Returns all the directories a user has access to.
	 * 
	 * @return the directories
	 */
	public Set<Directory> getDirectories() {
		return directories;
	}

	/**
	 * Returns the directory that can be accessed with the specified endpoint or
	 * url.
	 * 
	 * Note, this only works for endpoints that are auto-created by Goji, since
	 * otherwise we can't figure out which VO to use to access the filesystem.
	 * 
	 * @param endpointName_or_url
	 *            the endpoint or a url
	 * @return the directory or null if no directory can be found
	 * @throws FileSystemException
	 */
	public Directory getDirectory(String endpointName_or_url)
			throws FileSystemException {

		for (Directory d : getDirectories() ) {

			if ( endpointName_or_url.startsWith("gsiftp") ) {
				String url = d.getUrl();
				if (endpointName_or_url.startsWith(url)) {
					return d;
				}
			} else {
				String fqan = d.getFqan();
				String host = d.getFilesystem().getHost();

				String name = EndpointHelpers.translateIntoEndpointName(host, fqan);

				if (endpointName_or_url.startsWith(name)
						|| endpointName_or_url
						.startsWith(endpoint_username+"#"+name)) {
					return d;
				}
			}
		}

		throw new FileSystemException(
				"Url can't be mapped to any of the existing filesystems: "
						+ endpointName_or_url);
	}

	private Endpoint getEndpoint(String endpointName) {

		return getAllEndpoints(false).get(endpointName);

	}

	public String getEndpointName(Directory dir) {
		if (dir == null) {
			throw new IllegalArgumentException(
					"Directory parameter can not be null");
		}
		String epName = EndpointHelpers.translateIntoEndpointName(dir.getFilesystem().getHost(), dir.getFqan());
		return epName;
	}

	public Map<String, Endpoint> getEndpoints() {
		return getEndpoints(false);
	}

	/**
	 * Returns a map of all endpoints that the user has access to (calculated
	 * using info provider).
	 * 
	 * Key is endpoint name, value endpoint class.
	 * 
	 * @param force_refresh
	 *            whether to force refresh the list of endpoints and their
	 *            details
	 * @return a map of all of the users' endpoints
	 */
	public Map<String, Endpoint> getEndpoints(boolean force_refresh) {

		Map<String, Endpoint> result = Maps.newTreeMap();
		Map<String, Endpoint> allendpoints = getAllEndpoints(force_refresh);
		for ( String ep : allendpoints.keySet()) {
			if (allendpoints.get(ep).getUsername().equals(go_username)) {
				result.put(ep, allendpoints.get(ep));
			}
		}
		return result;
	}

	/**
	 * Returns all the filesystems a user has access to.
	 * 
	 * Keys are all filesystems, values are sets of fqans that can be used to
	 * access them. This is important because dependent on the fqan that is used
	 * to access the filesystem, a different user mapping will occur on the
	 * target site. So in GlobusOnline those filesystems need to be available as
	 * seperate endpoints and they need to be activated with a different proxy.
	 * 
	 * @return the filesystems
	 */
	public Map<FileSystem, Set<String>> getFileSystems() {
		return filesystems;
	}

	public Set<String> getFqans() {
		return this.fqans.keySet();
	}

	public String getGlobusOnlineUrl(String url) throws FileSystemException {

		Directory d = getDirectory(url);
		String root = d.getFilesystem().getUrl();
		String path = url.substring(root.length());

		return getEndpointName(d) + "/" + path;

	}

	public void getProxy(String fqan) {



	}

	/**
	 * Method to init user using the provided proxy credential.
	 * 
	 * @param proxy
	 *            a (valid) proxy
	 */
	public void init(GSSCredential cred) throws UserException {

		// save proxy file
		File proxyFile = new File(CoGProperties.getDefault().getProxyFile());

		if (cred == null) {
			if (!LocalProxy.validGridProxyExists()) {
				throw new UserException(
						"No credential provided and no local proxy exists.");
			} else {
				try {
					cred = CredentialHelpers.wrapGlobusCredential(CredentialHelpers
							.loadGlobusCredential(new File(LocalProxy.PROXY_FILE)));
				} catch (Exception e) {
					throw new UserException(
							"No credential provided and could not read existing local proxy.");
				}
			}
		}

		try {
			CredentialHelpers.writeToDisk(cred, proxyFile);
		} catch (Exception e1) {
			throw new UserException(e1);
		}

		// init GO-REST-client
		currentProxy = cred;

		try {
			client = new GojiTransferAPIClient(go_username,
					Goji.DEFAULT_BASE_URL, LocalProxy.PROXY_FILE,
					LocalProxy.PROXY_FILE,
					"/home/markus/.globus/certificates/gd_bundle.crt", false);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// getting VOs of user
		fqans = VOManagement.getAllFqans(cred, false);

		// calculate all the directories a user has access to
		directories.clear();
		getFileSystems().clear();
		for (String fqan : fqans.keySet()) {
			directories.addAll(im.getDirectoriesForVO(fqan));
		}
		for (Directory dir : directories) {
			Set<String> fqans = getFileSystems().get(dir.getFilesystem());
			if (fqans == null) {
				fqans = Sets.newTreeSet();
				getFileSystems().put(dir.getFilesystem(), fqans);
			}
			fqans.add(dir.getFqan());
		}

	}

	private void init_myproxy(String username, char[] password,
			String myproxyHost, int myproxyPort) throws UserException {

		try {
			GSSCredential cred = MyProxy_light.getDelegation(myproxyHost,
					myproxyPort, username, password,
					Credential.DEFAULT_PROXY_LIFETIME_IN_HOURS * 3600);
			init(cred);
		} catch (MyProxyException e) {
			throw new UserException(e);
		}

	}

	/**
	 * Convenience method to login using x509 certificate in default location.
	 * 
	 * @param cert_password
	 *            the password to unlock x509 certificate
	 */
	private void init_x509(char[] cert_password) {

		try {
			GSSCredential credential = PlainProxy.init(cert_password,
					Credential.DEFAULT_PROXY_LIFETIME_IN_HOURS);
			init(credential);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Arrays.fill(cert_password, 'x');
		}

	}

	public void invalidateCredentials() {

		for (Credential cred : proxies.values()) {

			try {
				cred.invalidate();
			} catch (CredentialException e) {
				e.printStackTrace();
			}

		}

	}

	public SortedSet<GFile> ls(String url) throws FileSystemException {

		url = ensureGlobusUrl(url);

		String epPart = EndpointHelpers.extractEndpointPart(url);
		String path = EndpointHelpers.extractPathPart(url);

		activateEndpoint(url, false);

		LsCommand lsC = new LsCommand(client, epPart, path);

		return lsC.getFiles();

	}

	public void removeAllEndpoints() throws UserException {

		if (!endpoint_username.equals(go_username)) {
			throw new UserException(
					"Can't create endpoints because endpoint-username is different.");
		}

		Map<String, Endpoint> endpoints = getEndpoints(true);
		for (String ep : endpoints.keySet()) {
			removeEndpoint(ep);
		}

	}

	public void removeEndpoint(Directory d) throws UserException {
		removeEndpoint(d.getFilesystem().getHost(), d.getFqan());
	}

	public void removeEndpoint(String alias) throws UserException {

		if (!endpoint_username.equals(go_username)) {
			throw new UserException(
					"Can't create endpoints because endpoint-username is different.");
		}

		myLogger.debug("Removing endpoint: " + alias);
		EndpointRemove er = new EndpointRemove(client, alias);

		endpointList = null;
	}

	public void removeEndpoint(String host, String fqan) throws UserException {
		String endpointAlias = EndpointHelpers.translateIntoEndpointName(host,
				fqan);
		removeEndpoint(endpointAlias);
	}

}
