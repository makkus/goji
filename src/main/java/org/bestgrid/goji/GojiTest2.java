package org.bestgrid.goji;

import grisu.jcommons.model.info.Directory;

import org.apache.commons.lang.StringUtils;
import org.bestgrid.goji.control.User;
import org.bestgrid.goji.exceptions.UserException;

public class GojiTest2 {

	/**
	 * @param args
	 * @throws UserException
	 */
	public static void main(String[] args) throws Exception {

		User user = null;

		if ((args.length > 0) && StringUtils.isNotBlank(args[0])) {
			user = new User("nz", args[0].toCharArray());
		} else {
			user = new User("nz");
		}

		// for (String ep : user.getAllEndpoints().keySet()) {
		// System.out.println(ep);
		// }

		for (Directory d : user.getDirectories()) {
			System.out.println(d.getUrl());
		}

		System.out
		.println(user
				.ensureGlobusUrl("gsiftp://df.auckland.ac.nz/~/testfile"));

		System.out
		.println(user
				.ensureGsiftpUrl("nz#gram5_ceres_auckland_ac_nz--nz_uoa/~/testfile"));

		// Set<GFile> files = user.ls("nz#df_auckland_ac_nz--nz_nesi/~/");
		//
		// for (GFile f : files) {
		// System.out.println(f.getName());
		// }

		// List<String> sources = Lists.newArrayList(
		// "gsiftp://df.auckland.ac.nz/~/ted_talk_brene_brown.mp4",
		// "gsiftp://df.auckland.ac.nz/~/test-qstat",
		// "nz#df_auckland_ac_nz--nz_nesi/~/testfile.txt");
		//
		// user.cp(sources, "gsiftp://df.auckland.ac.nz/~/testfolder");
		user.ls("gsiftp://df.auckland.ac.nz/~/");

		// user.cp(source, target)

		// String id = null;// tc.getOutput(GO_PARAM.TASK_ID);
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
