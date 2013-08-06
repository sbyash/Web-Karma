/*******************************************************************************
 * Copyright 2012 University of Southern California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This code was developed by the Information Integration Group as part 
 * of the Karma project at the Information Sciences Institute of the 
 * University of Southern California.  For more information, publications, 
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/

package edu.isi.karma.controller.command.alignment;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.karma.controller.command.Command;
import edu.isi.karma.controller.command.CommandException;
import edu.isi.karma.controller.update.FetchR2RMLUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.er.helper.TripleStoreUtil;
import edu.isi.karma.view.VWorkspace;

public class FetchR2RMLModelsCommand extends Command {
	private final String vWorksheetId;
	
	private String tripleStoreUrl;
	
	public String getTripleStoreUrl() {
		return tripleStoreUrl;
	}

	public void setTripleStoreUrl(String tripleStoreUrl) {
		this.tripleStoreUrl = tripleStoreUrl;
	}

	private static Logger logger = LoggerFactory.getLogger(FetchR2RMLModelsCommand.class);

	protected FetchR2RMLModelsCommand(String id, String vWorksheetId, String url) {
		super(id);
		this.vWorksheetId = vWorksheetId;
		if (url == null || url.isEmpty()) {
			url = TripleStoreUtil.defaultServerUrl + "/" + TripleStoreUtil.karma_model_repo;
		}
		this.tripleStoreUrl = url;
	}

	@Override
	public String getCommandName() {
		return FetchR2RMLModelsCommand.class.getName();
	}

	@Override
	public String getTitle() {
		return "Fetch R2RML from Triple Store";
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public CommandType getCommandType() {
		return CommandType.notUndoable;
	}
	
	@Override
	public UpdateContainer doIt(VWorkspace vWorkspace) throws CommandException {

		TripleStoreUtil utilObj = new TripleStoreUtil();
		HashMap<String, String> list = utilObj.fetchModelNames(this.tripleStoreUrl);
		return new UpdateContainer(new FetchR2RMLUpdate(list));
	}

	@Override
	public UpdateContainer undoIt(VWorkspace vWorkspace) {
		return null;
	}

}
