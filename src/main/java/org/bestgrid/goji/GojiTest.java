package org.bestgrid.goji;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bestgrid.goji.commands.TransferCommand;


public class GojiTest {

	/**
	 * @param args
	 * @throws UserInitException
	 */
	public static void main(String[] args) throws UserInitException {

		User user = null;

		if (StringUtils.isNotBlank(args[0])) {
			user = new User("nz", args[0].toCharArray());
		} else {
			user = new User("nz");
		}

		// System.out.println("Groups:\n"
		// + StringUtils.join(user.getFqans(), "\n"));
		// System.out.println("Directories:\n"
		// + StringUtils.join(user.getDirectories(), "\n"));
		// System.out.println("FileSystems:\n"
		// + StringUtils.join(user.getFileSystems().keySet(), "\n"));

		// System.out.println("Endpoints:");
		Map<String, Endpoint> eps = null;
		// eps = user.getEndpoints();
		// for (String ep : eps.keySet()) {
		// System.out.println(ep);
		// }
		//
		// user.removeAllEndpoints();
		//
		// System.out.println("Endpoints:");
		// eps = user.getEndpoints();
		// for (String ep : eps.keySet()) {
		// System.out.println(ep);
		// }
		//
		// SubmissionIdCommand sid = new SubmissionIdCommand(user.getClient());
		//
		// System.out.println("SID: " + sid.getOutput(GO_PARAM.TRANSFER_ID));
		//
		// user.createAllEndpoints();
		//
		// System.out.println("Endpoints:");
		// eps = user.getEndpoints();
		// for (String ep : eps.keySet()) {
		// System.out.println(ep);
		// }

		// user.activateAllEndpoints();

		TransferCommand tc = new TransferCommand(user.getClient(),
				"nz#df_auckland_ac_nz--nz_nesi/~/test.file",
				"nz#gram5_ceres_auckland_ac_nz--nz_uoa/~/test.result.file");

		System.out.println("Task_id: " + tc.getOutput(GO_PARAM.TASK_ID));

	}

}
