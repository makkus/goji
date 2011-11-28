package nz.org.nesi.goji.control;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.ietf.jgss.GSSCredential;

public class GssJSONTransferAPIClient extends JSONTransferAPIClient {

	private static KeyManager[] generateKeyManagers(GSSCredential cred)
			throws GeneralSecurityException, IOException {

		GlobusGSSCredentialImpl gCred = (GlobusGSSCredentialImpl) cred;
		X509Certificate[] certChain = gCred.getCertificateChain();
		int i = 1;
		for (X509Certificate c : certChain) {
			System.out.println("Certificate: " + i);
			System.out.println(c.toString());
			i++;
		}
		PrivateKey priKey = gCred.getPrivateKey();


		// The KeyStore requires a password for key entries.
		char[] password = { ' ' };

		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null);

		ArrayList<Certificate> chain = new ArrayList<Certificate>();
		for ( X509Certificate c : certChain) {
			chain.add(c);
		}
		ks.setEntry("client-key", new KeyStore.PrivateKeyEntry(priKey,
				chain
				.toArray(new Certificate[] {})),
				new KeyStore.PasswordProtection(password));

		// ks.setKeyEntry("client-key", priKey, password, certChain);

		// Shove the key store in a KeyManager.
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
				.getDefaultAlgorithm());
		kmf.init(ks, password);
		return kmf.getKeyManagers();
	}

	public GssJSONTransferAPIClient(String username,
			String trustedCAFile,
			GSSCredential cred, String baseUrl)
					throws GeneralSecurityException, IOException {

		super(username, trustedCAFile, generateKeyManagers(cred), baseUrl);
	}
}
