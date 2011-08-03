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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Basic client for interacting with the Globus Online Transfer API as a single
 * user, using x509 authentication.
 *
 * Does not make any assumptions about how the application will parse data
 * or handle key and trust stores.
 */
public class BaseTransferAPIClient {
    protected String username;
    protected String baseUrl;
    protected String alt;

    protected int timeout = 30 * 1000; // 30 seconds, in milliseconds.

    protected KeyManager[] keyManagers;
    protected TrustManager[] trustManagers;
    protected SSLSocketFactory socketFactory;

    static final String VERSION = "v0.10";
    static final String DEFAULT_BASE_URL =
                    "https://transfer.api.globusonline.org/" + VERSION;

    static final String ALT_JSON = "application/json";
    static final String ALT_XML = "application/xml";
    static final String ALT_HTML = "application/xhtml+xml";
    static final String ALT_DEFAULT = ALT_JSON;

    public static void main(String[] args) {
        BaseTransferAPIClient c = new BaseTransferAPIClient(args[0],
                                        BaseTransferAPIClient.ALT_JSON);
        try {
            HttpsURLConnection r = c.request("GET", "/tasksummary");
            BaseTransferAPIClient.printResult(r);
            r.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printResult(HttpsURLConnection c)
                    throws IOException, GeneralSecurityException, APIError {
        int code = c.getResponseCode();
        String message = c.getResponseMessage();
        System.out.println(code + " " + message);
        Map<String, List<String>> headers = c.getHeaderFields();
        Iterator headerIt = headers.entrySet().iterator();
        while (headerIt.hasNext()) {
            Map.Entry pair = (Map.Entry)headerIt.next();
            String key = (String)pair.getKey();
            if (key == null) {
				continue;
			}
            List<String> valueList = (List<String>) pair.getValue();
            Iterator valuesIt = valueList.iterator();
            while (valuesIt.hasNext()) {
                System.out.println(pair.getKey() + ": " + valuesIt.next());
            }
        }

        InputStream inputStream = null;
        if (code < 400) {
			inputStream = c.getInputStream();
		} else {
			inputStream = c.getErrorStream();
		}
        InputStreamReader reader = new InputStreamReader(inputStream);
        BufferedReader in = new BufferedReader(reader);

        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            System.out.println(inputLine);
        }

        in.close();
    }

    public BaseTransferAPIClient(String username, String alt) {
        this(username, alt, null, null, null);
    }

    public BaseTransferAPIClient(String username, String alt, String baseUrl) {
        this(username, alt, null, null, baseUrl);
    }

    /**
     * Create a client for the specified user.
     *
     * @param username  the Globus Online user to sign in to the API with.
     * @param alt  the content type to request from the server for responses.
     *             Use one of the ALT_ constants.
     * @param trustManagers trust managers to use for the HTTPS connection
     *                      for validating the server certificate,
     *                      or null to use the default configured trust
     *                      managers.
     * @param keyManagers key managers to use for the HTTPS connection
     *                    for providing the client key and cert needed for
     *                    authentication, or null to use the default
     *                    configured key managers.
     * @param baseUrl  alternate base URL of the service; can be used to
     *                 connect to different versions of the API and instances
     *                 running on alternate servers. If null, the URL of
     *                 the latest version running on the production server
     *                 is used.
     */
    public BaseTransferAPIClient(String username, String alt,
                                 TrustManager[] trustManagers,
                                 KeyManager[] keyManagers, String baseUrl) {
        this.username = username;
        this.alt = alt;
        if (baseUrl == null) {
            this.baseUrl = BaseTransferAPIClient.DEFAULT_BASE_URL;
        } else {
            this.baseUrl = baseUrl;
        }

        this.trustManagers = trustManagers;
        this.keyManagers = keyManagers;

        this.socketFactory = null;
    }

    /**
     * Parse an error response and return an APIError instance containing
     * the error data.
     *
     * Subclasses should override this with a method that parses the
     * response body according to the alt used and fills in all the fields
     * of APIError. See XMLDomTransferAPIClient for an example.
     */
    protected APIError constructAPIError(int statusCode, String statusMessage,
                                         String errorCode, InputStream input) {
        return new APIError(statusCode, statusMessage, errorCode);
    }

    protected void initSocketFactory(boolean force)
                    throws KeyManagementException, NoSuchAlgorithmException {
        if ((this.socketFactory == null) || force) {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(this.keyManagers, this.trustManagers, null);
            this.socketFactory = context.getSocketFactory();
        }
    }

    public HttpsURLConnection request(String method, String path)
          throws IOException, MalformedURLException, GeneralSecurityException,
                 APIError {
        if (! path.startsWith("/")) {
            path = "/" + path;
        }
        initSocketFactory(false);
        URL url = new URL(this.baseUrl + path);
        //System.out.println("[***] Request Path: " + this.baseUrl + path);
        HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
        c.setConnectTimeout(this.timeout);
        c.setSSLSocketFactory(this.socketFactory);
        c.setRequestMethod(method);
        c.setFollowRedirects(false);
        c.setRequestProperty("X-Transfer-API-X509-User", this.username);
        c.setRequestProperty("Accept", this.alt);
        c.connect();

        int statusCode = c.getResponseCode();
        if (statusCode >= 400) {
            String statusMessage = c.getResponseMessage();
            String errorHeader = null;
            Map<String, List<String>> headers = c.getHeaderFields();
            if (headers.containsKey("X-Transfer-API-Error")) {
                errorHeader = headers.get("X-Transfer-API-Error").get(0);
            }
            throw constructAPIError(statusCode, statusMessage, errorHeader,
                                    c.getErrorStream());
        }
        return c;
    }

    public void setConnectTimeout(int milliseconds) {
        this.timeout = milliseconds;
    }
}


