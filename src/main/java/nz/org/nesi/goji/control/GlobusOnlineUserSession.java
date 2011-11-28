package nz.org.nesi.goji.control;

import grisu.jcommons.exceptions.CredentialException;
import grisu.jcommons.interfaces.InfoManager;
import grisu.jcommons.model.info.Directory;
import grisu.jcommons.model.info.FileSystem;
import grith.jgrith.Credential;

import java.util.Map;
import java.util.Set;

import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.exceptions.FileSystemException;
import nz.org.nesi.goji.model.Endpoint;
import nz.org.nesi.goji.model.Transfer;

import org.bestgrid.goji.utils.EndpointHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class GlobusOnlineUserSession extends GlobusOnlineSession {

	static final Logger myLogger = LoggerFactory
			.getLogger(GlobusOnlineUserSession.class.getName());

	private final InfoManager im;

	private final String endpoint_username;

	private final Set<Directory> directories = Sets.newTreeSet();

	private final BiMap<String, String> alias_map = HashBiMap.create();
	private final Map<FileSystem, Set<String>> filesystems = Maps.newTreeMap();

	public GlobusOnlineUserSession(String go_username, char[] certPassphrase,
			InfoManager im) throws CredentialException {
		super(go_username, certPassphrase);

		this.im = im;

		this.endpoint_username = go_username;

		init();
	}

	public GlobusOnlineUserSession(String go_username, Credential cred,
			InfoManager im) throws CredentialException {
		super(go_username, cred, null);

		this.im = im;

		this.endpoint_username = go_username;

		init();
	}

	public GlobusOnlineUserSession(String go_username, Credential cred,
			String go_url, InfoManager im) throws CredentialException {

		super(go_username, cred, go_url);

		this.im = im;

		this.endpoint_username = go_username;

		init();

	}

	public GlobusOnlineUserSession(String go_username, InfoManager im)
			throws CredentialException {
		super(go_username);

		this.im = im;

		this.endpoint_username = go_username;

		init();
	}

	public GlobusOnlineUserSession(String go_username, String pathToCredential,
			InfoManager im) throws CredentialException {
		super(go_username, pathToCredential);

		this.im = im;

		this.endpoint_username = go_username;

		init();
	}

	/**
	 * Activates all endpoints configured in the InfoManger instance for this
	 * session.
	 * 
	 * Note, the endpoints can only be activated with the default session
	 * credential or a voms-enabled credential that is derived from it. The
	 * voms-specific stuff is done automatically (i.e. the proper voms proxy
	 * created for every Endpoint).
	 * 
	 * @throws CommandException
	 *             if not all endpoints could be activated
	 */
	public void activateAllEndpoints() throws CommandException {

		for (Directory d : getDirectories()) {
			activateEndpoint(d, false);
		}

	}


	/**
	 * Makes sure that one endpoint exists for each of the {@link Directory}s
	 * that are configured for the set of groups of the user in the
	 * {@link InfoManager} instance for this session.
	 * 
	 * If an endpoint doesn't exist, it is created using either the alias for
	 * the directory, or, if the directory doesn't have one, an auto-created
	 * name ({@link EndpointHelpers#translateFromEndpointName(String, String)}.
	 * 
	 * @throws CommandException
	 *             if either the endpoint username for this object is not the
	 *             same as the globus username for the GO session or at least
	 *             one endpoint could not be created
	 */
	public void createAllEndpoints() throws CommandException {

		if (!endpoint_username.equals(getGlobusOnlineUsername())) {
			throw new CommandException(
					"Can't create endpoints because endpoint-username is different.");
		}

		Set<Endpoint> allEndpoints = getAllUserEndpoints(true);

		for (FileSystem fs : getFileSystems().keySet()) {

			for (String fqan : getFileSystems().get(fs)) {

				String alias = getEndpointName(fs, fqan);

				boolean alreadyExists = false;
				for (Endpoint ep : allEndpoints) {
					if (ep.getName().equals(alias)) {
						alreadyExists = true;
						break;
					}
				}

				if (!alreadyExists) {
					addEndpoint(fs.getHost(), alias);
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

		for (Directory d : getDirectories()) {

			if (endpointName_or_url.startsWith("gsiftp")) {
				String url = d.getUrl();
				if (endpointName_or_url.startsWith(url)) {
					return d;
				}
			} else {
				// String fqan = d.getFqan();
				// String host = d.getFilesystem().getHost();
				//
				// String name = EndpointHelpers.translateIntoEndpointName(host,
				// fqan);
				String name = d.getAlias();

				if (endpointName_or_url.startsWith(name)
						|| endpointName_or_url.startsWith(endpoint_username
								+ "#" + name)) {
					return d;
				}
			}
		}

		throw new FileSystemException(
				"Url can't be mapped to any of the existing filesystems: "
						+ endpointName_or_url);
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

	private void init() {
		// getting VOs of user
		getFqans();

		// calculate all the directories a user has access to
		directories.clear();
		getFileSystems().clear();
		for (String fqan : getFqans()) {
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

	@Override
	public Transfer transfer(String sourceUrl, String targetUrl)
			throws CommandException {
		try {
			sourceUrl = ensureGlobusUrl(sourceUrl);
			targetUrl = ensureGlobusUrl(targetUrl);
		} catch (FileSystemException e) {
			throw new CommandException(e);
		}
		return super.transfer(sourceUrl, targetUrl);
	}



}
