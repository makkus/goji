package nz.org.nesi.goji.view;

import java.util.List;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;

public class GojiDeactivateParameters {

	@Parameter(description = "Endpoints to deactivate (default: '"
			+ GojiCli.ALL_ENDPOINTS + "')")
	private final List<String> endpoints = Lists.newArrayList();

	public List<String> getEndpoints() {
		return endpoints;
	}

}
