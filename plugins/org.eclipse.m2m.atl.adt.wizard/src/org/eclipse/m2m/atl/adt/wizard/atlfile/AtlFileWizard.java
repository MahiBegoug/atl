/*******************************************************************************
 * Copyright (c) 2004 INRIA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Freddy Allilaire (INRIA) - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2m.atl.adt.wizard.atlfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.m2m.atl.engine.vm.ATLVMPlugin;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class AtlFileWizard extends Wizard implements INewWizard, IExecutableExtension {
	
	protected static Logger logger = Logger.getLogger(ATLVMPlugin.LOGGER);

	private IConfigurationElement configElement;
	
	private AtlFileScreen page;
	
	private ISelection selection;
	
	private IContainer modelProject;

	/**
	 * Constructor
	 */
	public AtlFileWizard() {
		super();
		setNeedsProgressMonitor(true);		
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new AtlFileScreen(selection);
		addPage(page);
	}
	
	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		newProjectBuilder();
		BasicNewProjectResourceWizard.updatePerspective(configElement);
		return true;
	}

	/**
	 * This method creates an ATL project in the workspace with :
	 * 		the ATL transformation file 
	 *		the toString file (if the project needs it)
	 *		the toString query file (if the project needs it)
	 */
	public void newProjectBuilder() {
		String fileName = page.getParameter(AtlFileScreen.NAME);
		String fileType = page.getParameter(AtlFileScreen.TYPE);
		modelProject = (IContainer)ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(page.getParameter(AtlFileScreen.CONTAINER)));
		String contentFile = "";//$NON-NLS-1$

		if (fileType.equals(AtlFileScreen.MODULE)) {
			contentFile = AtlFileScreen.MODULE + " " + fileName + "; -- Module Template\n"; //$NON-NLS-1$ //$NON-NLS-2$
			contentFile += "create " + page.getParameter(AtlFileScreen.OUT); //$NON-NLS-1$
			contentFile += " from " + page.getParameter(AtlFileScreen.IN) + ";\n"; //$NON-NLS-1$ //$NON-NLS-2$
			contentFile += page.getParameter(AtlFileScreen.LIB);
		}
		else if (fileType.equals(AtlFileScreen.QUERY)) {
			contentFile = AtlFileScreen.QUERY + " " + fileName + " = ; -- Query Template\n"; //$NON-NLS-1$ //$NON-NLS-2$
			contentFile += page.getParameter(AtlFileScreen.LIB);
		}
		else if (fileType.equals(AtlFileScreen.LIBRARY)) {
			contentFile = AtlFileScreen.LIBRARY + " " + fileName + "; -- Library Template\n"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		createFile(fileName + ".atl", contentFile); //$NON-NLS-1$
		
	}
	
	/**
	 * This method creates a file with its content in the project
	 * There is two cases :
	 * 		the project has external location
	 * 		the project has local location
	 * In the first case, a file is created in the file system and there is a link between this file
	 * and the ATL project.
	 * In the second case, a file is created in the project
	 * @param fileName name of the file to create
	 * @param content content of the file to create
	 */
	private void createFile(String fileName, String content) {
		IFile file = modelProject.getFile(new Path(fileName));
		try {
			InputStream stream = openContentStream(content);
			if (file.exists()) {
				file.setContents(stream, true, true, null);
			} else {
				file.create(stream, true, null);
			}
			stream.close();	
		}
		catch (IOException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
//			e.printStackTrace();
		}
		catch (CoreException e1) {
			logger.log(Level.SEVERE, e1.getLocalizedMessage(), e1);
//			e1.printStackTrace();
		}
	}

	/**
	 * This method transforms string into inputstream
	 * @param contents content of the file to cast in InputStream
	 * @return the InputStream content
	 */
	private InputStream openContentStream(String contents) {
		return new ByteArrayInputStream(contents.getBytes());
	}
	
	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	/**
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		this.configElement = config;
	}
}