/*
 * Copyright 2010 University of Chicago
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
package nz.org.nesi.goji.model.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.exceptions.InitException;

import org.apache.commons.lang.StringUtils;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;

public class TransferCommand extends AbstractCommand {

	public static final int JGO_TRANSFER_SUCCESS = 202;

	private String submissionId = null;
	private JSONArray dataPairArr = null;

	public TransferCommand(BaseTransferAPIClient client) {
		super(client);
	}

	public TransferCommand(BaseTransferAPIClient client,
			List<String> sourcePaths, List<String> targetPaths)
			throws CommandException {
		super(client, new ImmutableMap.Builder<PARAM, String>()
				.put(PARAM.SOURCE_PATH, StringUtils.join(sourcePaths, ";"))
				.put(PARAM.TARGET_PATH, StringUtils.join(targetPaths, ";"))
				.build());
	}

	public TransferCommand(BaseTransferAPIClient client,
			Map<PARAM, String> config) throws CommandException {
		super(client, config);
	}

	public TransferCommand(BaseTransferAPIClient client, String sourcePath,
			String targetPath) throws CommandException {
		super(client, new ImmutableMap.Builder<PARAM, String>()
				.put(PARAM.SOURCE_PATH, sourcePath)
				.put(PARAM.TARGET_PATH, targetPath).build());
	}

	private void addSourceDestPathPair(String sourcePath, String destPath)
			throws Exception {

		int pos = sourcePath.indexOf("/");
		if (pos == -1) {
			throw new Exception("Invalid Source Endpoint format: " + sourcePath);
		}
		String sourceEndpoint = sourcePath.substring(0, pos);
		sourcePath = sourcePath.substring(pos);

		pos = destPath.indexOf("/");
		if (pos == -1) {
			throw new Exception("Invalid Destination Endpoint format: "
					+ destPath);
		}
		String destEndpoint = destPath.substring(0, pos);
		destPath = destPath.substring(pos);

		JSONObject data = new JSONObject();
		data.put("recursive", false);
		data.put("source_path", sourcePath);
		data.put("source_endpoint", sourceEndpoint);
		data.put("destination_path", destPath);
		data.put("destination_endpoint", destEndpoint);
		data.put("DATA_TYPE", "transfer_item");

		this.dataPairArr.put(data);
	}

	public void addTransfer(String source, String target) {
		String currentSource = getConfig(PARAM.SOURCE_PATH);
		String currentTarget = getConfig(PARAM.TARGET_PATH);

		if (StringUtils.isBlank(currentSource)) {
			try {
				setParameter(PARAM.SOURCE_PATH, source);
			} catch (CommandException e) {
			}
		} else {
			try {
				setParameter(PARAM.SOURCE_PATH, currentSource + ";" + source);
			} catch (CommandException e) {
			}
		}

		if (StringUtils.isBlank(currentTarget)) {
			try {
				setParameter(PARAM.TARGET_PATH, target);
			} catch (CommandException e) {
			}
		} else {
			try {
				setParameter(PARAM.TARGET_PATH, currentTarget + ";" + target);
			} catch (CommandException e) {
			}
		}
	}

	private JSONArray assemblePostArgument() {

		// FIXME: RESPECT PASSED IN TIMEOUT if not null
		// System.out.println("Timeout                   : " + timeout);
		// set deadline to 1 day and 6 hours (i.e. 30 hours) from current time
		long dateMS = System.currentTimeMillis() + (30 * 60 * 60 * 1000);
		Date deadline = new Date(dateMS);

		String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("America/Chicago"));

		try {
			JSONObject jobj = new JSONObject();
			jobj.put("DATA", this.dataPairArr);
			jobj.put("length", this.dataPairArr.length());
			jobj.put("deadline", sdf.format(deadline));
			// v0.9
			// jobj.put("transfer_id", this.transferID);

			// v0.10
			jobj.put("submission_id", this.submissionId);
			jobj.put("DATA_TYPE", "transfer");

			JSONArray dataArr = new JSONArray();
			dataArr.put(jobj);

			return dataArr;
		} catch (JSONException e) {
			throw new InitException("Can't assemble transfer post parameter.",
					e);
		}

	}

	@Override
	protected PARAM[] getInputParameters() {
		return new PARAM[] { PARAM.SOURCE_PATH, PARAM.TARGET_PATH };
	}

	@Override
	public Method getMethodType() {
		return Method.POST;
	}

	@Override
	protected PARAM[] getOptionalParameters() {
		return new PARAM[] {};
	}

	@Override
	protected PARAM[] getOutputParamets() {
		return new PARAM[] { PARAM.MESSAGE, PARAM.TASK_ID };
	}

	@Override
	public String getPath() {
		return "/transfer";
	}

	public String getTaskId() {
		return getOutput(PARAM.TASK_ID);
	}

	@Override
	protected void initialize() throws InitException {

		String[] sourcePaths = StringUtils.split(getConfig(PARAM.SOURCE_PATH),
				";");
		String[] targetPaths = StringUtils.split(getConfig(PARAM.TARGET_PATH),
				";");

		if (sourcePaths.length != targetPaths.length) {
			throw new InitException(
					"Different amount of source and target paths");
		}

		try {
			SubmissionId sidc = new SubmissionId(client);
			sidc.execute();
			submissionId = sidc.getOutput(PARAM.SUBMISSION_ID);
		} catch (Exception e) {
			throw new InitException("Can't get submission id.", e);
		}

		dataPairArr = new JSONArray();

		for (int i = 0; i < sourcePaths.length; i++) {

			try {
				addSourceDestPathPair(sourcePaths[i], targetPaths[i]);
			} catch (Exception e) {
				throw new InitException("Can't add transfer: " + sourcePaths[i]
						+ "/" + targetPaths[i]);
			}
		}

		JSONArray dataArr = assemblePostArgument();

		String jsonData = dataArr.toString();
		jsonData = jsonData.substring(1, jsonData.length() - 1);

		putJsonData(jsonData);

	}

	@Override
	protected void processResult() {

		String message = "Initiating Globus.org Transfer\n";
		String taskId = null;

		if (getResponseCode() == JGO_TRANSFER_SUCCESS) {

			message += extractFromResults("message");
			taskId = extractFromResults("task_id");
			putOutput(PARAM.MESSAGE, message);
			putOutput(PARAM.TASK_ID, taskId);
		} else {
			message += "Transfer FAILED (HTTP Error code " + getResponseCode()
					+ ").  This Transfer cannot be started.";
			taskId = extractFromResults("task_id");
			putOutput(PARAM.MESSAGE, message);
			putOutput(PARAM.TASK_ID, taskId);

		}

	}

	@Override
	public Map<String, String> getQueryParams() {
		return null;
	}

}
