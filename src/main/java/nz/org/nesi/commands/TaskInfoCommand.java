package nz.org.nesi.commands;

import nz.org.goji.model.TaskInfo;
import nz.org.nesi.GO_PARAM;
import nz.org.nesi.goji.exceptions.InitException;

import org.globusonline.transfer.BaseTransferAPIClient;

public class TaskInfoCommand extends AbstractCommand {

	private TaskInfo taskResult;

	public TaskInfoCommand (BaseTransferAPIClient client) {
		super(client);
	}

	public TaskInfoCommand(BaseTransferAPIClient client, String taskId) {
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
