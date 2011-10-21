package nz.org.nesi.goji.model.commands;

import nz.org.nesi.goji.exceptions.InitException;

import org.globusonline.transfer.BaseTransferAPIClient;

public class SubmissionId extends AbstractCommand {

	public SubmissionId(BaseTransferAPIClient client) {
		super(client);
	}

	@Override
	protected PARAM[] getInputParameters() {
		return new PARAM[]{};
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
		return new PARAM[] { PARAM.SUBMISSION_ID };
	}

	@Override
	public String getPath() {
		return "/transfer/submission_id";
	}

	@Override
	protected void initialize() throws InitException {
		// not necessary
	}

	@Override
	protected void processResult() {
		String transferID = extractFromResults("value");
		putOutput(PARAM.SUBMISSION_ID, transferID);
	}

}
