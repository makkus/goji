package org.bestgrid.goji.control;

import grisu.jcommons.interfaces.InfoManager;
import grisu.jcommons.model.info.Directory;
import grisu.jcommons.model.info.FileSystem;
import grith.jgrith.CredentialHelpers;
import grith.jgrith.myProxy.MyProxy_light;
import grith.jgrith.plainProxy.LocalProxy;
import grith.jgrith.plainProxy.PlainProxy;
import grith.jgrith.voms.VO;
import grith.jgrith.voms.VOManagement.VOManagement;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bestgrid.goji.CredentialException;
import org.bestgrid.goji.Goji;
import org.bestgrid.goji.commands.Activate;
import org.bestgrid.goji.commands.EndpointAdd;
import org.bestgrid.goji.commands.EndpointList;
import org.bestgrid.goji.commands.EndpointRemove;
import org.bestgrid.goji.exceptions.UserInitException;
import org.bestgrid.goji.model.Credential;
import org.bestgrid.goji.model.Endpoint;
import org.bestgrid.goji.utils.EndpointHelpers;
import org.globus.common.CoGProperties;
import org.globus.myproxy.MyProxyException;
import org.globusonline.GojiTransferAPIClient;
import org.ietf.jgss.GSSCredential;
import org.vpac.grisu.control.info.SqlInfoManager;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class User {

	static final Logger myLogger = Logger.getLogger(User.class.getName());

	// private static InfoManager im = new InfoManagerImpl();
	private static InfoManager im = new SqlInfoManager();

	private GojiTransferAPIClient client = null;

	private final String go_username;

	private GSSCredential currentProxy = null;
	private final Map<String, Credential> proxies = Maps.newConcurrentMap();

	private Map<String, VO> fqans = null;

	private EndpointList endpointList = null;

	private final Set<Directory> directories = Sets.newTreeSet();
	private final Map<FileSystem, Set<String>> filesystems = Maps.newTreeMap();
	/**
	 * Default constructor for a User.
	 * 
	 * In order to use this constructor, a valid local proxy needs to exist,
	 * otherwise a UserInitException will be thrown.
	 * 
	 * @param go_username
	 *            the GlobusOnline username
	 * @throws UserInitException
	 */
	public User(String go_username) throws UserInitException {
		this.go_username = go_username;

		if (LocalProxy.validGridProxyExists()) {
			init(null);
		} else {
			throw new UserInitException(
					"No valid proxy credential found. Can't init user.");
		}
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
		this.go_username = go_username;
		init_x509(cred_password);
	}

	/**
	 * Constructor to create and init a user in one go using provided proxy.
	 * 
	 * @param go_username
	 *            the GlobusOnline username
	 * @param proxy
	 *            a (valid) proxy
	 * @throws UserInitException
	 */
	public User(String go_username, GSSCredential proxy)
			throws UserInitException {
		this.go_username = go_username;
		init(proxy);
	}

	public User(String go_username, String myproxy_username,
			char[] myproxy_password, String myproxy_host, int myproxy_port)
					throws UserInitException {
		this.go_username = go_username;
		init_myproxy(myproxy_username, myproxy_password, myproxy_host,
				myproxy_port);
	}

	public void activateAllEndpoints() {

		Map<String, Endpoint> allEndpoints = getEndpoints();

		for (Directory d : directories) {
			try {
				activateEndpoint(d);
			} catch (CredentialException e) {
				e.printStackTrace();
			}
		}

	}

	public void activateEndpoint(Directory dir) throws CredentialException {

		String epName = EndpointHelpers.translateIntoEndpointName(dir.getFilesystem().getHost(), dir.getFqan());

		myLogger.debug("Activating endpoint: " + epName);

		Credential cred = getCredential(dir.getFqan());
		cred.uploadMyProxy();

		Activate a = new Activate(client, epName, cred, 12);

	}

	public void addEndpoint(Directory d) {
		addEndpoint(d.getFilesystem().getHost(), d.getFqan());
	}

	public void addEndpoint(String host, String fqan) {
		String endpointAlias = EndpointHelpers.translateIntoEndpointName(host,
				fqan);

		addEndpoint(host, fqan, endpointAlias);
	}

	public void addEndpoint(String host, String fqan, String epName) {

		myLogger.debug("Adding endpoint for: " + epName);

		EndpointAdd ea = new EndpointAdd(client, host,
				Credential.DEFAULT_MYPROXY_SERVER,
				null, false, true, epName);

		endpointList = null;
	}

	public void createAllEndpoints() {

		Map<String, Endpoint> allEndpoints = getEndpoints();

		for (FileSystem fs : filesystems.keySet()) {

			for (String fqan : filesystems.get(fs)) {
				String endpointName = EndpointHelpers
						.translateIntoEndpointName(fs.getHost(), fqan);
				if (!allEndpoints.containsKey(endpointName)) {
					addEndpoint(fs.getHost(), fqan, endpointName);
				}


			}

		}
	}

	/**
	 * Returns a list of all public endpoints as well as the users' private
	 * endpoints.
	 * 
	 * @return all endpoints
	 */
	public Map<String, Endpoint> getAllEndpoints() {

		if (endpointList == null) {
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
	 * Returns a map of all endpoints that the user has access to (calculated
	 * using info provider).
	 * 
	 * Key is endpoint name, value endpoint class.
	 * 
	 * @return a map of all of the users' endpoints
	 */
	public Map<String, Endpoint> getEndpoints() {

		Map<String, Endpoint> result = Maps.newTreeMap();
		Map<String, Endpoint> allendpoints = getAllEndpoints();
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

	public void getProxy(String fqan) {



	}

	/**
	 * Method to init user using the provided proxy credential.
	 * 
	 * @param proxy
	 *            a (valid) proxy
	 */
	public void init(GSSCredential cred) throws UserInitException {

		// save proxy file
		File proxyFile = new File(CoGProperties.getDefault().getProxyFile());

		if (cred == null) {
			if (!LocalProxy.validGridProxyExists()) {
				throw new UserInitException(
						"No credential provided and no local proxy exists.");
			} else {
				try {
					cred = CredentialHelpers.wrapGlobusCredential(CredentialHelpers
							.loadGlobusCredential(new File(LocalProxy.PROXY_FILE)));
				} catch (Exception e) {
					throw new UserInitException(
							"No credential provided and could not read existing local proxy.");
				}
			}
		}

		try {
			CredentialHelpers.writeToDisk(cred, proxyFile);
		} catch (Exception e1) {
			throw new UserInitException(e1);
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
		filesystems.clear();
		for (String fqan : fqans.keySet()) {
			directories.addAll(im.getDirectoriesForVO(fqan));
		}
		for (Directory dir : directories) {
			Set<String> fqans = filesystems.get(dir.getFilesystem());
			if (fqans == null) {
				fqans = Sets.newTreeSet();
				filesystems.put(dir.getFilesystem(), fqans);
			}
			fqans.add(dir.getFqan());
		}

	}

	private void init_myproxy(String username, char[] password,
			String myproxyHost, int myproxyPort) throws UserInitException {

		try {
			GSSCredential cred = MyProxy_light.getDelegation(myproxyHost,
					myproxyPort, username, password,
					Credential.DEFAULT_PROXY_LIFETIME_IN_HOURS * 3600);
			init(cred);
		} catch (MyProxyException e) {
			throw new UserInitException(e);
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

	public void removeAllEndpoints() {

		Map<String, Endpoint> endpoints = getEndpoints();
		for (String ep : endpoints.keySet()) {
			removeEndpoint(ep);
		}

	}

	public void removeEndpoint(Directory d) {
		removeEndpoint(d.getFilesystem().getHost(), d.getFqan());
	}

	public void removeEndpoint(String alias) {

		myLogger.debug("Removing endpoint: " + alias);
		EndpointRemove er = new EndpointRemove(client, alias);

		endpointList = null;
	}

	public void removeEndpoint(String host, String fqan) {
		String endpointAlias = EndpointHelpers.translateIntoEndpointName(host,
				fqan);
		removeEndpoint(endpointAlias);
	}

}
