package org.bestgrid.goji.commands;

import grisu.jcommons.model.info.GFile;

import java.util.Map;
import java.util.SortedSet;

import org.bestgrid.goji.GO_PARAM;
import org.bestgrid.goji.exceptions.InitException;
import org.bestgrid.goji.utils.EndpointHelpers;
import org.globusonline.transfer.BCTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.Sets;

public class LsCommand extends AbstractCommand {

	private SortedSet<GFile> files;

	public LsCommand(BCTransferAPIClient client, Map<GO_PARAM, String> params) {
		super(client, params);
	}

	public LsCommand(BCTransferAPIClient client, String endpoint, String path) {
		super(client, GO_PARAM.ENDPOINT_NAME, endpoint, GO_PARAM.PATH, path);
	}

	public SortedSet<GFile> getFiles() {
		return files;
	}

	@Override
	public Method getMethodType() {
		return Method.GET;
	}

	@Override
	public String getPath() {
		return "/endpoint/"
				+ EndpointHelpers.encode(getConfig(GO_PARAM.ENDPOINT_NAME))
				+ "/ls?path="
				+ getConfig(GO_PARAM.PATH);
	}

	@Override
	protected void init() throws InitException {
	}

	@Override
	protected void processResult() {

		try {

			String path = extractFromResults("path");
			putOutput(GO_PARAM.PATH, path);
			String endpoint = extractFromResults("endpoint");
			putOutput(GO_PARAM.ENDPOINT_NAME, endpoint);
			String group = extractFromResults("DATA", "group");

			files = Sets.newTreeSet();

			JSONObject jsonO = result.getJSONObject(0);
			if (jsonO != null) {
				JSONArray dataArr = jsonO.getJSONArray("DATA");

				for (int i = 0; i < dataArr.length(); i++) {
					JSONObject o = dataArr.getJSONObject(i);
					GFile f = new GFile(o);
					files.add(f);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
