package nz.org.nesi.commands;

import nz.org.nesi.goji.GO_PARAM;
import nz.org.nesi.goji.exceptions.InitException;

import org.globusonline.transfer.BaseTransferAPIClient;

public class SubmissionIdCommand extends AbstractCommand {

	public SubmissionIdCommand(BaseTransferAPIClient client) {
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
