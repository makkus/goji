package nz.org.nesi.goji.examples.info;

import grisu.jcommons.interfaces.GrinformationManagerDozer;
import grisu.jcommons.interfaces.InformationManager;
import nz.org.nesi.goji.control.GlobusOnlineUserSession;
import nz.org.nesi.goji.exceptions.UserException;
import nz.org.nesi.goji.model.Transfer;

import org.apache.commons.lang.StringUtils;


public class GojiTransfer {

	/**
	 * @param args
	 * @throws UserException
	 */
	public static void main(String[] args) throws Exception {

		GlobusOnlineUserSession session = null;

		// InfoManager im = new YnfoManager(
		// "/home/markus/src/infosystems/ynfo/src/test/resources/default_config.groovy");

//		YnfoManager im = new YnfoManager("nesi");
		InformationManager informationManager = new GrinformationManagerDozer("/data/src/config/nesi-grid-info/nesi_info.groovy");

		if ((args.length == 1) && StringUtils.isNotBlank(args[0])) {
			session = new GlobusOnlineUserSession("markus", args[0].toCharArray(),
					informationManager);
		} else {
			session = new GlobusOnlineUserSession("markus", informationManager);
		}


		session.createAllEndpoints();
		session.activateAllEndpoints();

		// session.transfer(
		// "markus#df_auckland_ac_nz--nz_nesi/~/simpleTestFile.txt",
		// "markus#gram5_ceres_auckland_ac_nz--nz_nesi/~/tttttt");

		// Transfer t = session.transfer(
		// "gsiftp://df.auckland.ac.nz/~/simpleTestFile.txt",
		// "gsiftp://gram5.ceres.auckland.ac.nz/~/tttttt222");
		Transfer t = session.transfer(
				"markus#pan/~/grid-client.jar",
				"markus#canterbury_p7/~/testfolder/grid-client.jar");

		t.waitForTransferToFinish();
		System.out.println("Finished.");

		System.exit(0);
	}

}
