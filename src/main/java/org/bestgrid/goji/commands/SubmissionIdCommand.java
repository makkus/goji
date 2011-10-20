package org.bestgrid.goji.commands;

import org.bestgrid.goji.GO_PARAM;
import org.bestgrid.goji.exceptions.InitException;
import org.globusonline.transfer.BCTransferAPIClient;

public class SubmissionIdCommand extends AbstractCommand {

	public SubmissionIdCommand(BCTransferAPIClient client) {
		super(client);
	}

	@Override
	public Method getMethodType() {
		return Method.GET;
	}

	@Override
	public String getPath() {
		return "/transfer/submission_id";
	}

	@Override
	protected void init() throws InitException {
		// not necessary
	}

	@Override
	protected void processResult() {
		String transferID = extractFromResults("value");
		putOutput(GO_PARAM.SUBMISSION_ID, transferID);
	}

}
