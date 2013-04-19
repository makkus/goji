package nz.org.nesi.goji.view.cli;

import java.util.List;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;

public class GojiLsParameters {

	@Parameter(description = "urls/paths to list")
	private List<String> urls = Lists.newArrayList();;

	public List<String> getUrls() {
		return urls;
	}

}
