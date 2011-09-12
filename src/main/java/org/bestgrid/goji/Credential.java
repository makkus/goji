package org.bestgrid.goji;

import grith.jgrith.CredentialHelpers;
import grith.jgrith.myProxy.MyProxy_light;
import grith.jgrith.voms.VO;
import grith.jgrith.vomsProxy.VomsProxy;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.globus.gsi.GlobusCredentialException;
import org.globus.myproxy.InitParams;
import org.globus.myproxy.MyProxy;
import org.globus.myproxy.MyProxyException;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import com.Ostermiller.util.RandPass;

public class Credential {

	static final Logger myLogger = Logger.getLogger(Credential.class.getName());

	public final static String DEFAULT_MYPROXY_SERVER = "myproxy.arcs.org.au";
	public final static int DEFAULT_MYPROXY_PORT = 7512;

	public final static int DEFAULT_PROXY_LIFETIME_IN_HOURS = 12;

	public final static int MIN_REMAINING_LIFETIME = 600;

	private GSSCredential cred = null;
	private String myProxyUsername = null;
	private char[] myProxyPassword = null;

	private boolean myproxyCredential = false;
	private boolean uploaded = false;

	private final String myProxyHostOrig = DEFAULT_MYPROXY_SERVER;
	private final int myProxyPortOrig = DEFAULT_MYPROXY_PORT;

	private String myProxyHostNew = myProxyHostOrig;
	private int myProxyPortNew = myProxyPortOrig;

	private String localPath = null;

	private final UUID uuid = UUID.randomUUID();

	public Credential(GSSCredential cred) throws CredentialException {
		this.cred = cred;
		this.myproxyCredential = false;

		getCredential();
	}

	public Credential(GSSCredential cred, VO vo, String fqan)
			throws CredentialException {

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

	}

	public Credential(String localPath) throws CredentialException {
		try {
			this.cred = CredentialHelpers
					.loadGssCredential(new File(localPath));
		} catch (GlobusCredentialException e) {
			throw new CredentialException("Can't read local proxy.", e);
		}
		this.localPath = localPath;
		this.myproxyCredential = false;
	}

	public Credential(String myProxyUsername, char[] myProxyPassword)
			throws CredentialException {

		this.myProxyUsername = myProxyUsername;
		this.myProxyPassword = myProxyPassword;
		this.myproxyCredential = true;
		getCredential();
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

	public boolean isMyProxyCredential() {
		return myproxyCredential;
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
