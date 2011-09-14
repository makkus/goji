/*
 * Copyright 2011 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bestgrid.goji;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bestgrid.goji.exceptions.RequestException;
import org.json.JSONArray;
import org.json.JSONObject;

public class TaskInfo
{
	private double MBitsPerSec;
	private long bytesTransferred;
	private String requestTime, completionTime;
	private String taskID, type, status, deadline, command;
	private int files, directories;
	private int subtasks_succeeded, subtasks_canceled, subtasks_failed,
	subtasks_pending, subtasks_retrying;

	public TaskInfo(JSONObject jobj)
	{
		try {
			createFromJSON(jobj);
		} catch (Exception e) {
			throw new RequestException(
					"Can't create TaskResult from JSONObject.", e);
		}
	}

	private void createFromJSON(JSONObject jobj) throws Exception {
		JSONArray dataArr = null, linkArr = null;
		JSONObject data = null, link = null;

		this.MBitsPerSec = 0;
		this.requestTime = jobj.getString("request_time");
		this.completionTime = jobj.getString("completion_time");
		this.bytesTransferred = jobj.getLong("bytes_transferred");

		if ((this.requestTime != null) && (!this.requestTime.equals("null"))
				&& (this.completionTime != null)
				&& (!this.completionTime.equals("null"))) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			Date d1 = df.parse(this.requestTime);
			Date d2 = df.parse(this.completionTime);

			long timeDiffSecs = ((d2.getTime() - d1.getTime()) / 1000);
			double MBitsTransferred = ((this.bytesTransferred / (1024L * 1024L)) * 8);
			this.MBitsPerSec = ((timeDiffSecs > 0) ? (MBitsTransferred / timeDiffSecs)
					: 0);
		}

		this.taskID = jobj.getString("task_id");
		this.type = jobj.getString("type");
		this.status = jobj.getString("status");
		this.deadline = jobj.getString("deadline");
		this.subtasks_succeeded = jobj.getInt("subtasks_succeeded");
		this.subtasks_canceled = jobj.getInt("subtasks_canceled");
		this.subtasks_failed = jobj.getInt("subtasks_failed");
		this.subtasks_pending = jobj.getInt("subtasks_pending");
		this.subtasks_retrying = jobj.getInt("subtasks_retrying");
		this.command = jobj.getString("command");
		this.files = jobj.getInt("files");
		this.directories = jobj.getInt("directories");

		// if (verbose)
		// {
		// System.out.println("subtasks_expired " +
		// jobj.get("subtasks_expired"));
		// System.out.println("username            = " + jobj.get("username"));
		// System.out.println("DATA_TYPE           = " + jobj.get("DATA_TYPE"));

		// linkArr = jobj.getJSONArray("LINKS");
		// for(int i = 0; i < linkArr.length(); i++)
		// {
		// link = linkArr.getJSONObject(i);
		// System.out.println("LINK[" + i + "] = (resource=" +
		// link.get("resource") +
		// ", rel=" + link.get("rel") + "href=" + link.get("href") + ")");
		// }
		// }
	}

	public long getBytesTransferred() {
		return bytesTransferred;
	}

	public String getCommand() {
		return command;
	}

	public String getCompletionTime() {
		return completionTime;
	}

	public String getDeadline() {
		return deadline;
	}

	public int getDirectories() {
		return directories;
	}

	public int getFiles() {
		return files;
	}

	public double getMBitsPerSec() {
		return MBitsPerSec;
	}

	public String getRequestTime() {
		return requestTime;
	}

	public String getStatus() {
		return status;
	}

	public int getSubtasks_canceled() {
		return subtasks_canceled;
	}

	public int getSubtasks_failed() {
		return subtasks_failed;
	}

	public int getSubtasks_pending() {
		return subtasks_pending;
	}

	public int getSubtasks_retrying() {
		return subtasks_retrying;
	}

	public int getSubtasks_succeeded() {
		return subtasks_succeeded;
	}

	public String getTaskID() {
		return taskID;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString()
	{
		StringBuffer strbuf = new StringBuffer("\n=== Task Details ===");
		strbuf.append("\nTask ID          : " + this.taskID);
		strbuf.append("\nTask Type        : " + this.type);
		strbuf.append("\nParent Task ID   : n/a"); // FIXME: Support?
		strbuf.append("\nStatus           : " + this.status);
		strbuf.append("\nRequest Time     : " + this.requestTime);
		strbuf.append("\nDeadline         : " + this.deadline);
		strbuf.append("\nCompletion Time  : " + this.completionTime);
		strbuf.append("\nTasks Successful : " + this.subtasks_succeeded);
		strbuf.append("\nTasks Canceled   : " + this.subtasks_canceled);
		strbuf.append("\nTasks Failed     : " + this.subtasks_failed);
		strbuf.append("\nTasks Pending    : " + this.subtasks_pending);
		strbuf.append("\nTasks Retrying   : " + this.subtasks_retrying);
		strbuf.append("\nCommand          : " + this.command);
		strbuf.append("\nFiles            : " + this.files);
		strbuf.append("\nDirectories      : " + this.directories);
		strbuf.append("\nBytes Transferred: " + this.bytesTransferred);
		strbuf.append("\nMBits/sec        : " + this.MBitsPerSec);
		strbuf.append("\n");
		return strbuf.toString();
	}
}