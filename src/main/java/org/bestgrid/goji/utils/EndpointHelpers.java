package org.bestgrid.goji.utils;

public class EndpointHelpers {

	public static String removeHash(String endpoint) {
		int pos = endpoint.indexOf("#");

		if (pos == -1) {
			return endpoint;
		} else {
			return endpoint.substring(pos + 1);
		}

	}

}
