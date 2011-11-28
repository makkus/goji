package nz.org.nesi.goji.examples.info;

import grisu.control.info.SqlInfoManager;
import grisu.jcommons.interfaces.InfoManager;
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

		InfoManager im = new SqlInfoManager();

		if ((args.length == 1) && StringUtils.isNotBlank(args[0])) {
			session = new GlobusOnlineUserSession("nz", args[0].toCharArray(),
					im);
		} else {
			session = new GlobusOnlineUserSession("markus", im);
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
				"markus#df_auckland_ac_nz--nz_nesi/~/halt-pvm.sh",
				"markus#df_auckland_ac_nz--nz_nesi/~/testfolder/halt-pvm.sh");

		t.waitForTransferToFinish();
		System.out.println("Finished.");


	}

}
