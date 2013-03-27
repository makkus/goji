package nz.org.nesi.goji.view;

import java.util.List;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;

public class GojiEndpointParameters {

	@Parameter(description = "Endpoints to activate (default: '"
			+ GojiCli.ALL_ENDPOINTS + "')")
	private final List<String> endpoints = Lists.newArrayList();

	public List<String> getEndpoints() {
		return endpoints;
	}

}
