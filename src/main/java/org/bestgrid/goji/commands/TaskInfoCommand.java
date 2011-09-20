package org.bestgrid.goji.commands;

import org.bestgrid.goji.GO_PARAM;
import org.bestgrid.goji.exceptions.InitException;
import org.bestgrid.goji.model.TaskInfo;
import org.globusonline.GojiTransferAPIClient;

public class TaskInfoCommand extends AbstractCommand {

	private TaskInfo taskResult;

	public TaskInfoCommand(GojiTransferAPIClient client, String taskId) {
		super(client, GO_PARAM.TASK_ID, taskId);
	}

	@Override
	public Method getMethodType() {
		return Method.GET;
	}

	@Override
	public String getPath() {
		return "/task/" + getConfig(GO_PARAM.TASK_ID);
	}

	public TaskInfo getTaskInfo() {
		return taskResult;
	}

	@Override
	protected void init() throws InitException {
		// nothing to do here
	}

	@Override
	protected void processResult() {


		try {
			taskResult = new TaskInfo(result.getJSONObject(0));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
