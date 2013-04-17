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

import grisu.jcommons.model.info.GFile;
import grisu.jcommons.utils.EndpointHelpers;

import java.util.Map;
import java.util.SortedSet;

import nz.org.nesi.goji.exceptions.CommandException;
import nz.org.nesi.goji.exceptions.InitException;

import org.globusonline.transfer.BCTransferAPIClient;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class LsCommand extends AbstractCommand {

	private SortedSet<GFile> files;

	public LsCommand(BaseTransferAPIClient client) {
		super(client);
	}

	public LsCommand(BaseTransferAPIClient client, Map<PARAM, String> params)
			throws CommandException {
		super(client, params);
	}

	public LsCommand(BCTransferAPIClient client, String endpoint, String path)
			throws CommandException {
		super(client, PARAM.ENDPOINT_NAME, endpoint, PARAM.PATH, path);
	}

	public SortedSet<GFile> getFiles() {
		return files;
	}

	@Override
	protected PARAM[] getInputParameters() {
		return new PARAM[] { PARAM.ENDPOINT_NAME, PARAM.PATH };
	}

	@Override
	public Method getMethodType() {
		return Method.GET;
	}

	@Override
	protected PARAM[] getOptionalParameters() {
		return new PARAM[] {};
	}

	@Override
	protected PARAM[] getOutputParamets() {
		return new PARAM[] { PARAM.ENDPOINT_NAME, PARAM.PATH };
	}

	@Override
	public String getPath() {
		return "/endpoint/"
				+ EndpointHelpers.encode(getConfig(PARAM.ENDPOINT_NAME))
				+ "/ls";
	}

	@Override
	protected void initialize() throws InitException {
	}

	@Override
	protected void processResult() {

		try {

			String path = extractFromResults("path");
			putOutput(PARAM.PATH, path);
			String endpoint = extractFromResults("endpoint");
			putOutput(PARAM.ENDPOINT_NAME, endpoint);
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

	public void setEndpoint(String ep) {
		try {
			setParameter(PARAM.ENDPOINT_NAME, ep);
		} catch (CommandException e) {
		}
	}

	public void setPath(String path) {
		try {
			setParameter(PARAM.PATH, path);
		} catch (CommandException e) {
		}
	}

	@Override
	public Map<String, String> getQueryParams() {
		return ImmutableMap.of("path", getConfig(PARAM.PATH));
	}
}
