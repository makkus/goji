/*
 * Copyright 2011 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.globusonline;

import java.io.BufferedReader;
import java.io.Console;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import org.json.JSONArray;


/**
 * Extension to the base client which supports reading PEM files using
 * Bouncy Castle, so the client cert/key don't have to be converted to
 * PKCS12. Uses JSON primarily, and allows console password retrieval.
 */
public class GojiTransferAPIClient extends BaseTransferAPIClient
{
	private static class ConsolePasswordFinder implements PasswordFinder
	{
		private ConsolePasswordFinder() { }

		public char[] getPassword()
		{
			Console c = System.console();
			return c.readPassword("Enter PEM Pass phrase: ");
		}
	}

	static KeyManager[] createKeyManagers(String certFile, String keyFile, boolean verbose)
			throws GeneralSecurityException, IOException
			{
		// Create a new empty key store.
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null);

		// Read the key. Ignore any non-key data in the file, to
		// support PEM files containing both certs and keys.
		FileReader fileReader = new FileReader(keyFile);
		PEMReader r = new PEMReader(fileReader, new ConsolePasswordFinder());
		KeyPair keyPair = null;
		try {
			Object o = null;
			while ((o = r.readObject()) != null) {
				if (o instanceof KeyPair) {
					keyPair = (KeyPair) o;
				}
			}
		} finally {
			r.close();
			fileReader.close();
		}

		// Read the cert(s). Ignore any non-cert data in the file, to
		// support PEM files containing both certs and keys.
		fileReader = new FileReader(certFile);
		r = new PEMReader(fileReader);
		X509Certificate cert = null;
		ArrayList<Certificate> chain = new ArrayList<Certificate>();
		try {
			Object o = null;
			int i = 0;
			while ((o = r.readObject()) != null) {
				if (!(o instanceof X509Certificate)) {
					continue;
				}
				cert = (X509Certificate) o;
				if (verbose)
				{
					System.out.println("client cert subject: "
							+ cert.getSubjectX500Principal());
					System.out.println("client cert issuer : "
							+ cert.getIssuerX500Principal());
				}
				chain.add(cert);
			}
		} finally {
			r.close();
			fileReader.close();
		}

		// The KeyStore requires a password for key entries.
		char[] password = { ' ' };

		// Since we never write out the key store, we don't bother protecting
		// the key.
		ks.setEntry("client-key",
				new KeyStore.PrivateKeyEntry(keyPair.getPrivate(),
						chain.toArray(new Certificate[0])),
						new KeyStore.PasswordProtection(password));

		// Shove the key store in a KeyManager.
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(
				KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, password);
		return kmf.getKeyManagers();
			}

	static TrustManager[] createTrustManagers(String trustedCAFile, boolean verbose)
			throws GeneralSecurityException, IOException {
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null);

		// Read the cert(s). The file must contain only certs, a cast
		// Exception will be thrown if it contains anything else.
		// TODO: wrap in friendly exception, it's a user error not a
		// programming error if the file contains a non-cert.
		FileReader fileReader = new FileReader(trustedCAFile);
		PEMReader r = new PEMReader(fileReader);
		X509Certificate cert = null;
		try {
			Object o = null;
			int i = 0;
			while ((o = r.readObject()) != null) {
				cert = (X509Certificate) o;

				if (verbose)
				{
					System.out.println("trusted cert subject: "
							+ cert.getSubjectX500Principal());
					System.out.println("trusted cert issuer : "
							+ cert.getIssuerX500Principal());
				}

				ks.setEntry("server-ca" + i,
						new KeyStore.TrustedCertificateEntry(cert), null);
				i++;
			}
		} finally {
			r.close();
			fileReader.close();
		}

		// Shove the key store in a TrustManager.
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(
				TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ks);
		return tmf.getTrustManagers();
	}

	public static JSONArray getResult(HttpsURLConnection c)
			throws IOException, GeneralSecurityException
			{
		JSONArray jArr = null;
		InputStream inputStream = c.getInputStream();
		InputStreamReader reader = new InputStreamReader(inputStream);
		BufferedReader in = new BufferedReader(reader);

		String inputLine = null;
		StringBuffer strbuf = new StringBuffer("[");

		while ((inputLine = in.readLine()) != null)
		{
			strbuf.append(inputLine);
		}
		strbuf.append("]");
		in.close();

		try
		{
			jArr = new JSONArray(strbuf.toString());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return jArr;
			}

	private final String path = null;
	private String go_username = null;
	private String cafile = null;
	private String certfile = null;
	private final String keyfile = null;
	private boolean verbose = false;

	/**
	 * Create a client for the user.
	 *
	 * @param username  the Globus Online user to sign in to the API with.
	 * @param alt  the content type to request from the server for responses.
	 *             Use one of the ALT_ constants.
	 * @param trustedCAFile path to a PEM file with a list of certificates
	 *                      to trust for verifying the server certificate.
	 *                      If null, just use the trust store configured by
	 *                      property files and properties passed on the
	 *                      command line.
	 * @param certFile  path to a PEM file containing a client certificate
	 *                  to use for authentication. If null, use the key
	 *                  store configured by property files and properties
	 *                  passed on the command line.
	 * @param keyFile  path to a PEM file containing a client key
	 *                 to use for authentication. If null, use the key
	 *                 store configured by property files and properties
	 *                 passed on the command line.
	 * @param baseUrl  alternate base URL of the service; can be used to
	 *                 connect to different versions of the API and instances
	 *                 running on alternate servers. If null, the URL of
	 *                 the latest version running on the production server
	 *                 is used.
	 */
	public GojiTransferAPIClient(String go_username, String baseurl,
			String certfile, String keyfile, String cafile, boolean verbose)
			throws KeyManagementException, NoSuchAlgorithmException, Exception
			{
		super(go_username, ALT_JSON, null, null, baseurl);

		this.go_username = go_username;
		this.cafile = cafile;
		this.certfile = certfile;
		this.verbose = verbose;

		Security.addProvider(new BouncyCastleProvider());

		this.trustManagers = this.createTrustManagers(
this.cafile, this.verbose);

		this.keyManagers = this.createKeyManagers(
this.certfile, this.keyfile,
				this.verbose);

		initSocketFactory(true);
			}


	public String getUsername()
	{
		return this.go_username;
	}

	public HttpsURLConnection request(String method, String path, String jsonData)
			throws IOException, MalformedURLException, GeneralSecurityException {
		if (! path.startsWith("/")) {
			path = "/" + path;
		}

		initSocketFactory(false);
		URL url = new URL(this.baseUrl + path);
		if (this.verbose)
		{
			System.out.println("[***] Request Path: " + this.baseUrl + path);
		}
		HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
		c.setConnectTimeout(this.timeout);
		c.setSSLSocketFactory(this.socketFactory);
		c.setRequestMethod(method);
		c.setFollowRedirects(false);
		c.setRequestProperty("X-Transfer-API-X509-User", this.username);
		c.setRequestProperty("Accept", this.alt);
		c.setUseCaches(false);
		c.setDoInput(true);
		c.setDoOutput(true);
		c.setRequestProperty("Content-Type", this.alt);
		c.setRequestProperty("Content-Length", "" + Integer.toString(jsonData.getBytes().length));
		c.setRequestProperty("Content-Language", "en-US");
		c.connect();

		DataOutputStream wr = new DataOutputStream(c.getOutputStream());
		wr.writeBytes(jsonData);
		wr.flush ();
		wr.close ();

		return c;
	}
}
