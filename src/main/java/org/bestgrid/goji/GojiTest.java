package org.bestgrid.goji;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bestgrid.goji.control.User;
import org.bestgrid.goji.exceptions.UserException;
import org.bestgrid.goji.model.Endpoint;


public class GojiTest {

	/**
	 * @param args
	 * @throws UserException
	 */
	public static void main(String[] args) throws Exception {

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
		user.createAllEndpoints();
		// //
		// System.out.println("Endpoints:");
		eps = user.getEndpoints();
		for (String ep : eps.keySet()) {
			System.out.println(ep);
		}
		//
		user.activateAllEndpoints();
		//
		// user.invalidateCredentials();

		// TransferCommand tc = new TransferCommand(
		// user.getClient(),
		// "nz#df_auckland_ac_nz--nz_nesi/~/test.file;nz#df_auckland_ac_nz--nz_nesi/~/testfile.txt",
		// "nz#gram5_ceres_auckland_ac_nz--nz_uoa/~/test.result3.file;nz#gram5_ceres_auckland_ac_nz--nz_uoa/~/testfile3.result.txt");
		//
		// String id = tc.getOutput(GO_PARAM.TASK_ID);
		//
		// System.out.println("Task_id: " + id);
		//
		// while (true) {
		// TaskInfoCommand ti = new TaskInfoCommand(user.getClient(), id);
		//
		// String status = ti.getTaskInfo().getStatus();
		// System.out.println(status);
		// if (!"ACTIVE".equals(status)) {
		// break;
		// }
		//
		// Thread.sleep(5000);
		// }

	}

}
