package nz.org.nesi.goji.model;

import grisu.jcommons.exceptions.CredentialException;
import grith.jgrith.CredentialHelpers;
import grith.jgrith.myProxy.MyProxy_light;
import grith.jgrith.plainProxy.LocalProxy;
import grith.jgrith.plainProxy.PlainProxy;
import grith.jgrith.voms.VO;
import grith.jgrith.vomsProxy.VomsProxy;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.globus.common.CoGProperties;
import org.globus.myproxy.DestroyParams;
import org.globus.myproxy.InitParams;
import org.globus.myproxy.MyProxy;
import org.globus.myproxy.MyProxyException;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.Ostermiller.util.RandPass;

public class Credential {

	static final Logger myLogger = LoggerFactory.getLogger(Credential.class
			.getName());

	public final static String DEFAULT_MYPROXY_SERVER = "myproxy.arcs.org.au";
	public final static int DEFAULT_MYPROXY_PORT = 7512;

	public final static int DEFAULT_PROXY_LIFETIME_IN_HOURS = 12;

	public final static int MIN_REMAINING_LIFETIME = 600;

	private GSSCredential cred = null;
	private String myProxyUsername = null;
	private char[] myProxyPassword = null;

	private boolean myproxyCredential = false;
	private boolean uploaded = false;

	private final String myProxyHostOrig;
	private final int myProxyPortOrig;

	private String myProxyHostNew = null;
	private int myProxyPortNew = -1;

	private String localPath = null;

	private final String fqan;

	private final UUID uuid = UUID.randomUUID();

	/**
	 * Creates a Credential object from an x509 certificate and key pair that
	 * sits in the default globus location (usually $HOME/.globus/usercert.pem &
	 * userkey.pem) using the {@link #DEFAULT_PROXY_LIFETIME_IN_HOURS}.
	 * 
	 * @param passphrase
	 *            the certificate passphrase
	 * @throws CredentialException
	 *             if the proxy could not be created
	 */
	public Credential(char[] passphrase) throws CredentialException {
		this(CoGProperties.getDefault().getUserCertFile(), CoGProperties.getDefault().getUserKeyFile(), passphrase, DEFAULT_PROXY_LIFETIME_IN_HOURS);
	}

	/**
	 * Creates a Credential object from an x509 certificate and key pair that
	 * sits in the default globus location (usually $HOME/.globus/usercert.pem &
	 * userkey.pem).
	 * 
	 * @param passphrase
	 *            the certificate passphrase
	 * @param lifetime_in_hours
	 *            the lifetime of the proxy in hours
	 * @throws CredentialException
	 *             if the proxy could not be created
	 */
	public Credential(char[] passphrase, int lifetime_in_hours)
			throws CredentialException {
		this(CoGProperties.getDefault().getUserCertFile(), CoGProperties.getDefault().getUserKeyFile(), passphrase, lifetime_in_hours);
	}


	/**
	 * Creates a Credential object using the provided GSSCredential as base
	 * credential.
	 * 
	 * @param cred
	 *            a GSSCredential
	 * 
	 * @throws CredentialException
	 *             if the provided credential is not valid
	 */
	public Credential(GSSCredential cred) throws CredentialException {
		this.cred = cred;
		this.myproxyCredential = false;
		this.fqan = null;

		this.myProxyHostOrig = DEFAULT_MYPROXY_SERVER;
		this.myProxyPortOrig = DEFAULT_MYPROXY_PORT;

		this.myProxyHostNew = this.myProxyHostOrig;
		this.myProxyPortNew = this.myProxyPortOrig;

		getCredential();
	}

	/**
	 * Creates a new, VOMS-enabled credential out of the provided base
	 * credential.
	 * 
	 * @param cred
	 *            the base credential, this would usually have no voms attribute
	 *            certificate attached
	 * @param vo
	 *            the VO the new credential gets its attribute credential from
	 * @param fqan
	 *            the fqan (group) of the new credential
	 * @throws CredentialException
	 *             if the provided credential is not valid or the voms attribute
	 *             certificate could not be created
	 */
	public Credential(GSSCredential cred, VO vo, String fqan)
			throws CredentialException {

		this.fqan = fqan;
		try {
			VomsProxy vp = new VomsProxy(vo, fqan,
					CredentialHelpers.unwrapGlobusCredential(cred), new Long(
							cred.getRemainingLifetime()) * 1000);

			this.cred = CredentialHelpers.wrapGlobusCredential(vp
					.getVomsProxyCredential());
			this.myproxyCredential = false;
		} catch (Exception e) {
			throw new CredentialException("Can't create voms credential.", e);
		}

		this.myProxyHostOrig = DEFAULT_MYPROXY_SERVER;
		this.myProxyPortOrig = DEFAULT_MYPROXY_PORT;

		this.myProxyHostNew = this.myProxyHostOrig;
		this.myProxyPortNew = this.myProxyPortOrig;

	}

	/**
	 * Creates a Credential object out of an existing proxy credential.
	 * 
	 * This proxy would usually be on the default globus location (e.g.
	 * /tmp/x509u.... for Linux).
	 * 
	 * @param localPath
	 *            the path to the proxy credential
	 * @throws CredentialException
	 *             if the credential at the specified path is not valid
	 */
	public Credential(String localPath) throws CredentialException {

		this(CredentialHelpers.loadGssCredential(new File(localPath)));

	}

	/**
	 * Creates a Credential object from MyProxy login information.
	 * 
	 * @param myProxyUsername
	 *            the MyProxy username
	 * @param myProxyPassword
	 *            the MyProxy password
	 * @param myproxyHost
	 *            the MyProxy host
	 * @param myproxyPort
	 *            the MyProxy port
	 * @throws CredentialException
	 *             if no valid proxy could be retrieved from MyProxy
	 */
	public Credential(String myProxyUsername, char[] myProxyPassword,
			String myproxyHost, int myproxyPort)
					throws CredentialException {

		this.myProxyUsername = myProxyUsername;
		this.myProxyPassword = myProxyPassword;
		this.myproxyCredential = true;
		this.myProxyHostOrig = myproxyHost;
		this.myProxyPortOrig = myproxyPort;

		this.myProxyHostNew = this.myProxyHostOrig;
		this.myProxyPortNew = this.myProxyPortOrig;

		getCredential();
		// TODO: check cred for vo info
		this.fqan = null;
	}

	/**
	 * This one creates a Credential object by creating a proxy out of a local
	 * X509 certificate & key.
	 * 
	 * @param certFile
	 *            the path to the certificate
	 * @param keyFile
	 *            the path to the key
	 * @param certPassphrase
	 *            the passphrase for the certificate
	 * @param lifetime_in_hours
	 *            the lifetime of the proxy
	 * @throws IOException
	 * @throws GSSException
	 * @throws Exception
	 */
	public Credential(String certFile, String keyFile, char[] certPassphrase,
			int lifetime_in_hours) throws CredentialException {

		this.cred = PlainProxy.init(certFile, keyFile, certPassphrase,
				lifetime_in_hours);

		this.myproxyCredential = false;
		this.myProxyHostOrig = DEFAULT_MYPROXY_SERVER;
		this.myProxyPortOrig = DEFAULT_MYPROXY_PORT;

		this.myProxyHostNew = this.myProxyHostOrig;
		this.myProxyPortNew = this.myProxyPortOrig;

		this.fqan = null;

	}

	public Credential createVomsCredential(VO vo, String fqan)
			throws CredentialException {
		return new Credential(getCredential(), vo, fqan);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Credential) {
			Credential other = (Credential)o;
			if ( myproxyCredential ) {
				if ( myProxyUsername.equals(other.getMyProxyUsername())
						&& Arrays.equals(myProxyPassword, other.getMyProxyPassword()) ) {
					return true;
				} else {
					return false;
				}
			} else {
				try {
					return getCredential().equals(((Credential) o).getCredential());
				} catch (CredentialException e) {
					return false;
				}
			}
		} else {
			return false;
		}
	}

	public GSSCredential getCredential() throws CredentialException {

		if ( this.cred == null ) {
			// means, get it from myproxy
			try {
				cred = MyProxy_light.getDelegation(myProxyHostOrig, myProxyPortOrig,
						myProxyUsername, myProxyPassword,
						DEFAULT_PROXY_LIFETIME_IN_HOURS);
			} catch (MyProxyException e) {
				throw new CredentialException(
						"Can't retrieve credential from MyProxy", e);
			}

		} else {
			try {
				if (this.cred.getRemainingLifetime() < MIN_REMAINING_LIFETIME) {
					if (!myproxyCredential) {
						throw new CredentialException(
								"Min lifetime shorter than threshold.");
					}
				}
			} catch (GSSException e) {
				throw new CredentialException("Can't get remaining lifetime from credential", e);
			}
		}
		return cred;
	}

	public String getFqan() {
		return this.fqan;
	}

	public String getLocalPath() {

		if ( localPath == null ) {
			try {
				CredentialHelpers.writeToDisk(getCredential(), new File(
						LocalProxy.PROXY_FILE));
				localPath = LocalProxy.PROXY_FILE;
			} catch (Exception e) {
				myLogger.error(
						"Could  not write credential: "
								+ e.getLocalizedMessage(), e);
				throw new RuntimeException(e);
			}
		}
		return localPath;
	}

	public char[] getMyProxyPassword() {

		if (myproxyCredential) {
			return myProxyPassword;
		} else {
			if (myProxyPassword != null) {
				return myProxyPassword;
			} else {
				myProxyPassword = new RandPass().getPassChars(10);
				return myProxyPassword;
			}
		}
	}

	public String getMyProxyServer() {
		return myProxyHostNew;
	}

	public String getMyProxyUsername() {

		if (myproxyCredential) {
			return myProxyUsername;
		} else {
			if (StringUtils.isNotBlank(myProxyUsername)) {
				return myProxyUsername;
			} else {
				// try {
				// myProxyUsername = cred.getName().toString()
				// + UUID.randomUUID().toString();
				// } catch (Exception e) {
				myProxyUsername = UUID.randomUUID().toString();
				// }
				return myProxyUsername;
			}

		}
	}

	@Override
	public int hashCode() {
		if ( myproxyCredential ) {
			return (myProxyPassword.hashCode() + myProxyPassword.hashCode()) * 32;
		} else {
			try {
				return getCredential().hashCode() * 432;
			} catch (CredentialException e) {
				return uuid.hashCode();
			}
		}
	}

	public void invalidate() throws CredentialException {

		myLogger.debug("Invalidating credential for " + fqan);

		DestroyParams request = new DestroyParams();
		request.setUserName(myProxyUsername);
		request.setPassphrase(new String(myProxyPassword));

		MyProxy mp = new MyProxy(myProxyHostNew, myProxyPortNew);
		try {
			mp.destroy(getCredential(), request);
		} catch (Exception e) {
			throw new CredentialException(
					"Could not destroy myproxy credential.", e);
		}

	}

	public boolean isMyProxyCredential() {
		return myproxyCredential;
	}

	public void saveCredential(String localPath) throws CredentialException {

		CredentialHelpers.writeToDisk(getCredential(), new File(localPath));

		this.localPath = localPath;
	}

	public void uploadMyProxy() throws CredentialException {
		uploadMyProxy(null, -1);
	}

	private void uploadMyProxy(String myProxyHostUp, int myProxyPortUp)
			throws CredentialException {

		// TODO: check whether new upload is required?
		if (uploaded == true) {
			return;
		}

		if (StringUtils.isNotBlank(myProxyHostUp)) {
			this.myProxyHostNew = myProxyHostUp;
		}

		if (myProxyPortUp > 0) {
			this.myProxyPortNew = myProxyPortUp;
		}

		if (myproxyCredential
				&& (this.myProxyHostOrig.equals(this.myProxyHostNew) && (this.myProxyPortOrig == this.myProxyPortNew))) {
			// doesn't make sense in that case
			return;
		}

		myLogger.debug("Uploading credential to: " + myProxyHostNew);

		MyProxy mp = MyProxy_light.getMyProxy(myProxyHostNew, myProxyPortNew);

		InitParams params = null;
		try {
			params = MyProxy_light.prepareProxyParameters(getMyProxyUsername(),
					null, null, null, null,
					DEFAULT_PROXY_LIFETIME_IN_HOURS * 3600);
		} catch (MyProxyException e) {
			throw new CredentialException("Can't prepare myproxy parameters", e);
		}

		try {
			MyProxy_light.init(mp, getCredential(), params,
					getMyProxyPassword());
			uploaded = true;
		} catch (Exception e) {
			throw new CredentialException("Can't upload MyProxy", e);
		}

	}

}
