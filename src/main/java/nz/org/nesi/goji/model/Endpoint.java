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
package nz.org.nesi.goji.model;

import grisu.jcommons.utils.EndpointHelpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class Endpoint implements Comparable {

	private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	{
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
	}

	private int port;

	private String canonical_name, username, description, myproxy_server,
	activated, name;

	private String expire_time, is_public, ls_link, scheme, subject, hostname,
	uri;

	private String hosts, subjects, status;

	private boolean username_matches, is_globus_connect;

	private Date expires;

	public Endpoint(JSONObject jobj, String username) throws Exception {
		createFromJSON(jobj, username);
	}

	public int compareTo(Object arg0) {
		if ( arg0 instanceof Endpoint ) {
			Endpoint o = (Endpoint) arg0;
			int i = getUsername().compareTo(o.getUsername());
			if (i == 0) {
				return getName().compareTo(o.getName());
			} else {
				return i;
			}
		} else {
			return -1;
		}
	}



	public void createFromJSON(JSONObject jobj, String username)
			throws Exception {
		JSONArray jobjArr = null;

		this.is_globus_connect = false;

		this.canonical_name = jobj.getString("canonical_name");
		this.username = jobj.getString("username");
		this.description = jobj.getString("description");
		this.myproxy_server = jobj.getString("myproxy_server");
		this.activated = jobj.getString("activated");
		this.name = jobj.getString("name");
		this.expire_time = jobj.getString("expire_time");
		if ("null".equals(this.expire_time)) {
			this.expire_time = null;
		}
		this.is_public = jobj.getString("public");
		this.ls_link = jobj.getString("ls_link");

		String gc = jobj.getString("is_globus_connect");
		if (gc != null) {
			this.is_globus_connect = (gc.equals("true") ? true : false);
		}

		try {
			jobjArr = jobj.getJSONArray("DATA");
			if ((jobjArr == null) || (jobjArr.length() < 1)) {
				return;
			}

			for (int i = 0; i < jobjArr.length(); i++) {
				JSONObject jobj2 = jobjArr.getJSONObject(i);

				if (i > 0) {
					this.hosts += ", " + jobj2.getString("uri");
				} else {
					this.hosts = jobj2.getString("uri");
				}

				if (i > 0) {
					this.subjects += ", " + jobj2.getString("subject");
				} else {
					this.subjects = jobj2.getString("subject");
				}

				this.port = jobj2.getInt("port");
				this.scheme = jobj2.getString("scheme");
				this.subject = jobj2.getString("subject");
				this.hostname = jobj2.getString("hostname");
				this.uri = jobj2.getString("uri");
			}
		} catch (Exception e) {
		}

		String myproxy_server = "n/a";
		if (jobj.get("myproxy_server") != null) {
			this.myproxy_server = jobj.getString("myproxy_server");
			if ((myproxy_server == null) || (myproxy_server.equals("null"))) {
				myproxy_server = "n/a";
			}
		}

		if ((this.subjects == null) || (this.subjects.indexOf("null") != -1)) {
			this.subjects = "";
		}

		this.username_matches = (this.username.equals(username) ? true : false);

		this.status = "n/a";

		if (jobj.get("activated") != null) {
			if (jobj.getString("activated").equals("true")) {
				this.status = "ACTIVE";
			}
		}
		if (StringUtils.isNotBlank(this.expire_time)
				&& !"null".equals(this.expire_time)) {
			this.expires = DATE_FORMAT.parse(this.expire_time);
		}
	}

	@Override
	public boolean equals(Object o) {

		if ( o instanceof Endpoint ) {
			Endpoint other = (Endpoint)o;
			if ( getUsername().equals(((Endpoint) o).getUsername()) && getName().equals(other.getName()) ) {
				return true;
			}
		} else if (o instanceof String) {
			String other = (String) o;

			if (other.contains("#")) {
				String u = EndpointHelpers.extractUsername(other);

				if (!u.equals(getUsername())) {
					return false;
				}

			}
			String epName = EndpointHelpers.extractEndpointName(other);
			if (!epName.equals(getName())) {
				return false;
			} else {
				return true;
			}
		}

		return false;
	}

	public Date getExpires() {
		return expires;
	}

	public String getHostname() {
		return hostname;
	}

	public String getName() {
		return name;
	}

	public String getUsername() {
		return username;
	}

	@Override
	public int hashCode() {
		return (43 * getName().hashCode()) + (124 * getUsername().hashCode());
	}

	public boolean isActivated() {
		return Boolean.parseBoolean(activated);
	}

	@Override
	public String toString() {
		StringBuffer strbuf = new StringBuffer("");
		// if ((this.username_matches == true) || (this.is_public == true))
		// {
		strbuf.append("\nName              : " + this.canonical_name);
		strbuf.append("\nHost(s)           : " + this.hosts);
		strbuf.append("\nSubject(s)        : " + this.subjects);
		strbuf.append("\nMyProxy Server    : " + this.myproxy_server);
		// if (this.list_verbose == true)
		// {
		// strbuf.append("\nCredential Status : " + this.status);
		// strbuf.append("\nCredential Expires: " + this.expires);
		// }
		strbuf.append("\n");
		// }
		return strbuf.toString();
	}
}