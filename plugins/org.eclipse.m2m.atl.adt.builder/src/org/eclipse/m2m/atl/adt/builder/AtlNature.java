/*******************************************************************************
 * Copyright (c) 2004 INRIA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    INRIA - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2m.atl.adt.builder;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class AtlNature implements IProjectNature {
	
	/** The Atl nature id. Each Atl project is added this nature */
	public static final String ATL_NATURE_ID = "org.eclipse.m2m.atl.adt.builder.atlNature";
	
	private IProject project;

	/**
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	public void configure() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		boolean found = false;
		
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(AtlBuilder.ATL_BUILDER_ID)) {
				found = true;
				break;
			}
		}
		if (!found) { 
			//add builder to project
			ICommand command = desc.newCommand();
			command.setBuilderName(AtlBuilder.ATL_BUILDER_ID);
			ICommand[] newCommands = new ICommand[commands.length + 1];
			
			// Add it before other builders.
			System.arraycopy(commands, 0, newCommands, 1, commands.length);
			newCommands[0] = command;
			desc.setBuildSpec(newCommands);
			project.setDescription(desc, null);
		}
	}
	
	/**
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
	    IProjectDescription desc = project.getDescription();
	    ICommand[] commands = desc.getBuildSpec();
	    boolean found = false;
	    int i = 0;
	    for (; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(AtlBuilder.ATL_BUILDER_ID)) {
			    found = true;
				break;
			}
		}
	    if (found) {
	        ICommand[] newCommands = new ICommand[commands.length - 1];
	        System.arraycopy(commands, 0, newCommands, 0, i);
	        if (i+1 < commands.length)
	            System.arraycopy(commands, i+1, newCommands, i+1, commands.length - (i + 1));
	    }
	}
	
	/**
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		return project;
	}
	
	/**
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	public void setProject(IProject project) {
		this.project = project;
	}
}
