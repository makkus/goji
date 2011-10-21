package nz.org.nesi.goji.model.commands;

import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.exceptions.InitException;
import nz.org.nesi.goji.model.TaskInfo;

import org.globusonline.transfer.BaseTransferAPIClient;

public class Task extends AbstractCommand {

	private TaskInfo taskResult;

	public Task (BaseTransferAPIClient client) {
		super(client);
	}

	public Task(BaseTransferAPIClient client, String taskId)
			throws CommandException {
		super(client, PARAM.TASK_ID, taskId);
	}

	@Override
	protected PARAM[] getInputParameters() {
		return new PARAM[]{PARAM.TASK_ID};
	}

	@Override
	public Method getMethodType() {
		return Method.GET;
	}

	@Override
	protected PARAM[] getOptionalParameters() {
		return new PARAM[]{};
	}

	@Override
	protected PARAM[] getOutputParamets() {
		return new PARAM[]{};
	}

	@Override
	public String getPath() {
		return "/task/" + getConfig(PARAM.TASK_ID);
	}

	public TaskInfo getTaskInfo() {
		return taskResult;
	}

	@Override
	protected void initialize() throws InitException {
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

	public void setTaskId(String id) {
		try {
			setParameter(PARAM.TASK_ID, id);
		} catch (CommandException e) {
		}
	}

}
