package nz.org.nesi.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import nz.org.nesi.goji.GO_PARAM;
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
			List<String> sourcePaths, List<String> targetPaths) {
		super(client, new ImmutableMap.Builder<GO_PARAM, String>()
				.put(GO_PARAM.SOURCE_PATH, StringUtils.join(sourcePaths, ";"))
				.put(GO_PARAM.TARGET_PATH, StringUtils.join(targetPaths, ";"))
				.build());
	}

	public TransferCommand(BaseTransferAPIClient client,
			Map<GO_PARAM, String> config) {
		super(client, config);
	}

	public TransferCommand(BaseTransferAPIClient client, String sourcePath,
			String targetPath) {
		super(client, new ImmutableMap.Builder<GO_PARAM, String>()
				.put(GO_PARAM.SOURCE_PATH, sourcePath)
				.put(GO_PARAM.TARGET_PATH, targetPath).build());
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
	public Method getMethodType() {
		return Method.POST;
	}

	@Override
	public String getPath() {
		return "/transfer";
	}

	@Override
	protected void init() throws InitException {

		String[] sourcePaths = StringUtils.split(
				getConfig(GO_PARAM.SOURCE_PATH), ";");
		String[] targetPaths = StringUtils.split(
				getConfig(GO_PARAM.TARGET_PATH), ";");

		if (sourcePaths.length != targetPaths.length) {
			throw new InitException(
					"Different amount of source and target paths");
		}

		try {
			SubmissionIdCommand sidc = new SubmissionIdCommand(client);
			submissionId = sidc.getOutput(GO_PARAM.SUBMISSION_ID);
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
			putOutput(GO_PARAM.MESSAGE, message);
			putOutput(GO_PARAM.TASK_ID, taskId);
		} else {
			message += "Transfer FAILED (HTTP Error code " + getResponseCode()
					+ ").  This Transfer cannot be started.";
		}

	}

}
