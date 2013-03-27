package nz.org.nesi.goji.view;

import grisu.jcommons.constants.GridEnvironment;
import grith.jgrith.cred.GridCliParameters;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;

public class GojiMainParameters extends GridCliParameters {
	
	@Parameter(names = { "-i", "--info-file" }, description = "file describing grid resources")
	private String infoFile = GridEnvironment.getGridInfoConfigFile().getAbsolutePath();

	public String getInfoFile() {
		return infoFile;
	}
	
	

}
