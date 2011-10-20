package nz.org.nesi.goji;

import grith.jgrith.plainProxy.LocalProxy;

import org.apache.commons.lang.StringUtils;
import org.globusonline.transfer.BCTransferAPIClient;
import org.globusonline.transfer.BaseTransferAPIClient;

public class GlobusOnlineSession {

	private final BaseTransferAPIClient client;
	private final String go_username;
	private final String go_url;

	public GlobusOnlineSession(String go_username) {
		this(go_username, LocalProxy.PROXY_FILE, null);
	}

	public GlobusOnlineSession(String go_username, String pathToProxy,
			String go_url) {
		this.go_username = go_username;
		if (StringUtils.isBlank(go_url)) {
			this.go_url = Goji.DEFAULT_BASE_URL;
		} else {
			this.go_url = go_url;
		}
		try {
			client = new BCTransferAPIClient(go_username,
					BaseTransferAPIClient.FORMAT_JSON, pathToProxy,
					pathToProxy, Goji.DEFAULT_BASE_URL);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
