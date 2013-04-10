package nz.org.nesi.goji.view;

import grisu.jcommons.constants.GridEnvironment;
import grith.jgrith.cred.GridCliParameters;

import java.io.File;

import org.apache.commons.lang.StringUtils;

import com.beust.jcommander.Parameter;

public class GojiMainParameters extends GridCliParameters {

	@Parameter(names = { "-g", "--go-username" }, description = "globus online username")
	private final String goUsername = null;

	@Parameter(names = { "-e", "--endpoint-username" }, description = "endpoint globus online username")
	private final String endpointUsername = "nz";

	@Parameter(names = { "-i", "--info-file" }, description = "file describing grid resources")
	private String infoFile = GridEnvironment.getGridInfoConfigFile()
			.getAbsolutePath();

	public String getInfoFile() {

		if (StringUtils.isBlank(infoFile)) {
			infoFile = GridEnvironment.getGridInfoConfigFile()
					.getAbsolutePath();
			if (!new File(infoFile).exists() || !new File(infoFile).isFile()) {
				infoFile = "https://raw.github.com/nesi/nesi-grid-info/develop/nesi_info.groovy";
			}
		}
		return infoFile;
	}

	public String getGoUsername() {
		return goUsername;
	}

	public String getEndpointUsername() {
		return endpointUsername;
	}

}
