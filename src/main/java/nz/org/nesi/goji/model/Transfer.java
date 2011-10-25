package nz.org.nesi.goji.model;

import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.model.commands.Task;

import org.globusonline.transfer.BaseTransferAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a TaskInfo object for easier to use workflow methods.
 * 
 * @author Markus Binsteiner
 * 
 */
public class Transfer {

	private static final int DEFAULT_WAIT_INBETWEEN_STATUS_CHECKS_TIME_IN_SECONDS = 10;

	Logger myLogger = LoggerFactory.getLogger(Transfer.class);

	private final String taskId;
	private final BaseTransferAPIClient client;

	private TaskInfo info;

	public Transfer(BaseTransferAPIClient client, String taskId) {
		this.client = client;
		this.taskId = taskId;

		// loading Transfer in background
		new Thread() {
			@Override
			public void run() {
				refresh();
			}
		}.start();
	}

	public String getCompletionTimeString(boolean refresh) {

		return getInfo(refresh).getCompletionTime();
	}

	private TaskInfo getInfo(boolean refresh) {
		if (refresh || (info == null)) {
			refresh();
		}
		return info;
	}

	public String getStatus() {
		return getStatus(false);
	}

	public String getStatus(boolean refresh) {

		if (refresh || (info == null)) {
			refresh();
		}
		return info.getStatus();
	}

	public boolean isFinished() {
		return isFinished(false);
	}

	public boolean isFinished(boolean refresh) {

		String completionTimeString = getCompletionTimeString(refresh);
		if (completionTimeString != null) {
			return true;
		} else {
			return false;
		}

	}

	public synchronized void refresh() {

		if ((info != null) && isFinished(false)) {
			return;
		}
		try {
			Task task = new Task(client, taskId);
			info = task.getTaskInfo();
		} catch (CommandException e) {
			myLogger.error("Can't update task info.", e);
		}
	}

	public void waitForTransferToFinish() {

		waitForTransferToFinish(DEFAULT_WAIT_INBETWEEN_STATUS_CHECKS_TIME_IN_SECONDS);

	}

	public void waitForTransferToFinish(int secondsInbetweenStatusUpdates) {

		while (!isFinished(true)) {
			System.out.println(info.toString());
			try {
				Thread.sleep(secondsInbetweenStatusUpdates * 1000);
			} catch (InterruptedException e) {
				myLogger.error("Wait for transfer to finish interrupted.", e);
				return;
			}
		}

	}

}
