/*******************************************************************************
 * Copyright (c) 2007, 2008 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - ATL compatibility converter
 *******************************************************************************/
package org.eclipse.m2m.atl.adt.perspective.compatibility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.m2m.atl.adt.perspective.AtlPerspectiveMessages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An utility class for old ATL projects conversion.
 * 
 * @author William Piers <a href="mailto:william.piers@obeo.fr">william.piers@obeo.fr</a>
 * @author Freddy Allilaire
 */
public class CompatibilityUtils {

	public static final String oldNatureId = "org.atl.eclipse.adt.builder.atlNature"; //$NON-NLS-1$
	public static final String newNatureId = "org.eclipse.m2m.atl.adt.builder.atlNature"; //$NON-NLS-1$

	public static final String oldBuilderId = "org.atl.eclipse.adt.builder.atlBuilder"; //$NON-NLS-1$
	public static final String newBuilderId = "org.eclipse.m2m.atl.adt.builder.atlBuilder"; //$NON-NLS-1$

	public static final String oldConfigId = "org.atl.eclipse.adt.launching.atlTransformation"; //$NON-NLS-1$
	public static final String newConfigId = "org.eclipse.m2m.atl.adt.launching.atlTransformation"; //$NON-NLS-1$

	/**
	 * @return the list of the old projects
	 * @throws CoreException
	 */
	public static IProject[] getProjects() throws CoreException {
		List res = new ArrayList();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (project.isOpen()) {
				String[] ids = project.getDescription().getNatureIds();
				if (arrayContains(ids, oldNatureId) && ! arrayContains(ids, newNatureId)) {
					res.add(project);	
				}
			}
		}
		return (IProject[]) res.toArray(new IProject[res.size()]);		
	}

	private static boolean arrayContains(Object[] array,Object o){
		for (int i = 0; i < array.length; i++) {
			if (o.equals(array[i]))	return true;
		}
		return false;
	}
	
	/**
	 * @return the list of the old configurations
	 * @throws CoreException
	 */
	public static ILaunchConfiguration[] getConfigurations() throws CoreException {
		List res = new ArrayList();
		ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();
		for (int i = 0; i < configs.length; i++) {
			ILaunchConfiguration config = configs[i];
			if (config.getType().getIdentifier().equals(oldConfigId)) {
				res.add(config);
			}
		}
		return (ILaunchConfiguration[]) res.toArray(new ILaunchConfiguration[res.size()]);		
	}
	
	/**
	 * @param projects the projects to convert
	 * @throws CoreException
	 */
	public static void convertProjects(Object[] projects) throws Exception {
		//TODO The NullProgressMonitor does nothing, it could be interesting to catch a ProgressMonitor or to add one.
		NullProgressMonitor monitor = new NullProgressMonitor();
		monitor.beginTask(AtlPerspectiveMessages.getString("CompatibilityUtils.0"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
		for (int i = 0; i < projects.length; i++) {
			IProject project = (IProject) projects[i];
			monitor.subTask(AtlPerspectiveMessages.getString("CompatibilityUtils.1") + project.getName()); //$NON-NLS-1$
			
			IProjectDescription desc = project.getDescription();
			ICommand[] commands = desc.getBuildSpec();
			String[] natures = desc.getNatureIds();
			
			ICommand[] newCommands = new ICommand[commands.length];
			for (int j = 0; j < commands.length; j++) {
				ICommand command = commands[j];
				if (command.getBuilderName().equals(oldBuilderId)) {
					command.setBuilderName(newBuilderId);
					monitor.subTask(AtlPerspectiveMessages.getString("CompatibilityUtils.2")); //$NON-NLS-1$
				}
				newCommands[j] = command;
			}		
			desc.setBuildSpec(newCommands);
			
			String[] newNatures = new String[natures.length];
			for (int j = 0; j < natures.length; j++) {
				String nature = natures[j];
				if (nature.equals(oldNatureId)) {
					nature = newNatureId;
					monitor.subTask(AtlPerspectiveMessages.getString("CompatibilityUtils.3")); //$NON-NLS-1$
				}
				newNatures[j] = nature;
			}
			desc.setNatureIds(newNatures);
			
			project.setDescription(desc, IProject.AVOID_NATURE_CONFIG, null);
			project.refreshLocal(IProject.DEPTH_INFINITE,null);
			monitor.done();
		}
	}

	/**
	 * @param configurations the configurations to convert
	 * @return true if Eclipse needs to be restarted
	 * @throws Exception
	 */
	public static boolean convertConfigurations(Object[] configurations) throws Exception {
		//TODO The NullProgressMonitor does nothing, it could be interesting to catch a ProgressMonitor or to add one.
		NullProgressMonitor monitor = new NullProgressMonitor();
		monitor.beginTask(AtlPerspectiveMessages.getString("CompatibilityUtils.4"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
		boolean needToRestart = false;
		for (int i = 0; i < configurations.length; i++) {
			ILaunchConfiguration conf = (ILaunchConfiguration) configurations[i];
			monitor.subTask(AtlPerspectiveMessages.getString("CompatibilityUtils.5") + conf.getName()); //$NON-NLS-1$
			if (conf.getType().getIdentifier().equals(oldConfigId)) {
				IFile ifile = conf.getFile();
				File file = conf.getLocation().toFile();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder;

				try {
					docBuilder = factory.newDocumentBuilder();
					Document document = docBuilder.parse(file);

					Element root = (Element) document.getFirstChild();
					if (root.getAttribute("type").equals(CompatibilityUtils.oldConfigId)) { //$NON-NLS-1$
						root.setAttribute("type", CompatibilityUtils.newConfigId); //$NON-NLS-1$
						monitor.subTask(AtlPerspectiveMessages.getString("CompatibilityUtils.8") + conf.getName()); //$NON-NLS-1$
					}

					Transformer transformer = TransformerFactory.newInstance().newTransformer();
					StreamResult result = new StreamResult(file);
					DOMSource source = new DOMSource(document);
					transformer.transform(source, result);
					
				} catch (ParserConfigurationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (Throwable e) {
					// nothing, if not valid, the config is not converted
				}
				
				if (ifile != null) {
					ifile.refreshLocal(IFile.DEPTH_INFINITE, monitor);					
				} else {
					needToRestart = true;
				}
			}	
			monitor.done();
		}
		return needToRestart;
	}
}
