package nz.org.nesi;

import grisu.info.ynfo.YnfoManager;
import grisu.jcommons.interfaces.InfoManager;

import java.util.Map;

import nz.org.goji.model.Endpoint;
import nz.org.nesi.goji.control.User;
import nz.org.nesi.goji.exceptions.UserException;

import org.apache.commons.lang.StringUtils;


public class GojiTest {

	/**
	 * @param args
	 * @throws UserException
	 */
	public static void main(String[] args) throws Exception {

		User user = null;

		InfoManager im = new YnfoManager(
				"/home/markus/src/infosystems/ynfo/src/test/resources/default_config.groovy");

		if (StringUtils.isNotBlank(args[0])) {
			user = new User("nz", args[0].toCharArray(), im);
		} else {
			user = new User("nz", im);
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
