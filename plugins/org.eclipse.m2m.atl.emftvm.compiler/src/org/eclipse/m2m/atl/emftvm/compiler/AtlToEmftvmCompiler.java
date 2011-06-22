/*******************************************************************************
 * Copyright (c) 2011 Vrije Universiteit Brussel.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dennis Wagelaar, Vrije Universiteit Brussel - initial API and
 *         implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.m2m.atl.emftvm.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.m2m.atl.common.ATLLogger;
import org.eclipse.m2m.atl.core.ATLCoreException;
import org.eclipse.m2m.atl.core.IModel;
import org.eclipse.m2m.atl.core.emf.EMFModel;
import org.eclipse.m2m.atl.core.emf.EMFReferenceModel;
import org.eclipse.m2m.atl.emftvm.EmftvmFactory;
import org.eclipse.m2m.atl.emftvm.ExecEnv;
import org.eclipse.m2m.atl.emftvm.Metamodel;
import org.eclipse.m2m.atl.emftvm.Model;
import org.eclipse.m2m.atl.emftvm.util.DefaultModuleResolver;
import org.eclipse.m2m.atl.emftvm.util.ModuleResolver;
import org.eclipse.m2m.atl.emftvm.util.TimingData;
import org.eclipse.m2m.atl.emftvm.util.VMException;
import org.eclipse.m2m.atl.engine.ProblemConverter;
import org.eclipse.m2m.atl.engine.compiler.AtlStandaloneCompiler;
import org.eclipse.m2m.atl.engine.compiler.CompileTimeError;
import org.eclipse.m2m.atl.engine.parser.AtlParser;


/**
 * Invokes the ATL to EMFTVM compiler.
 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
 */
public class AtlToEmftvmCompiler implements AtlStandaloneCompiler {

	public static final String PLUGIN_ID = "org.eclipse.m2m.atl.emftvm.compiler";

	protected final ResourceSet rs = new ResourceSetImpl();
	protected final Metamodel atlmm = EmftvmFactory.eINSTANCE.createMetamodel();
	protected final Metamodel pbmm = EmftvmFactory.eINSTANCE.createMetamodel();
	protected final ModuleResolver mr = new DefaultModuleResolver("platform:/plugin/" + PLUGIN_ID + "/transformations/", rs);

	/**
	 * Creates a new {@link AtlToEmftvmCompiler}.
	 */
	public AtlToEmftvmCompiler() {
		super();
		atlmm.setResource(((EMFReferenceModel)AtlParser.getDefault().getAtlMetamodel()).getResource());
		pbmm.setResource(((EMFReferenceModel)AtlParser.getDefault().getProblemMetamodel()).getResource());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.m2m.atl.engine.compiler.AtlStandaloneCompiler#compile(java.io.InputStream, java.lang.String)
	 */
	public CompileTimeError[] compile(InputStream in, String outputFileName) {
		EObject[] eObjects = compileWithProblemModel(in, outputFileName);

		// convert the EObjects into an easily readable form (instances of CompileTimeError).
		CompileTimeError[] result = new CompileTimeError[eObjects.length];
		for (int i = 0; i < eObjects.length; i++) {
			result[i] = ProblemConverter.convertProblem(eObjects[i]);
		}

		// return them to caller
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.m2m.atl.engine.compiler.AtlStandaloneCompiler#compileWithProblemModel(java.io.InputStream, java.lang.String)
	 */
	@SuppressWarnings("deprecation")
	public EObject[] compileWithProblemModel(InputStream in,
			String outputFileName) {
		EObject[] result = new EObject[0];
		try {
			File asm = new File(outputFileName);
			if (asm.exists()) {
				asm.delete();
			}
			asm.createNewFile();
			//TODO Refactor ATL's compiler framework to support multiple file extensions
			outputFileName = outputFileName.substring(0, outputFileName.lastIndexOf('.')) + ".emftvm";
			result = compileWithProblemModel(in, new FileOutputStream(outputFileName));
			final IFile[] outputFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(
					java.net.URI.create("file:/" + outputFileName));
			for (IFile file : outputFiles) {
				file.getParent().refreshLocal(IResource.DEPTH_ONE, null);
				if (file.exists()) {
					file.setDerived(true);
				}
			}
		} catch (IOException e) {
			ATLLogger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			EmftvmCompilerPlugin.log(e);
		} catch (CoreException e) {
			ATLLogger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			EmftvmCompilerPlugin.log(e.getStatus());
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.m2m.atl.engine.compiler.AtlStandaloneCompiler#compileWithProblemModel(java.io.InputStream, java.io.OutputStream)
	 */
	public EObject[] compileWithProblemModel(InputStream in,
			OutputStream outputStream) {
		final List<EObject> pbs = new ArrayList<EObject>();
		try {
			final IModel[] parsed = AtlParser.getDefault().parseToModelWithProblems(in, true);
			final IModel atlmodel = parsed[0];
			final IModel problems = parsed[1];
			
			if (getProblems(problems, pbs) == 0) {
				final EObject[] cpbs = compileWithProblemModel(atlmodel, outputStream);
				for (EObject cpb : cpbs) {
					pbs.add(cpb);
				}
			}
		} catch (ATLCoreException e) {
			ATLLogger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			EmftvmCompilerPlugin.log(e);
		}
		return pbs.toArray(new EObject[pbs.size()]);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.m2m.atl.engine.compiler.AtlStandaloneCompiler#compileWithProblemModel(org.eclipse.m2m.atl.core.IModel, java.io.OutputStream)
	 */
	public EObject[] compileWithProblemModel(IModel atlModel,
			OutputStream outputStream) {
		final List<EObject> pbs = new ArrayList<EObject>();
		
		final Model atlm = EmftvmFactory.eINSTANCE.createModel();
		atlm.setResource(((EMFModel)atlModel).getResource());

		final Resource pr = rs.createResource(URI.createFileURI("problems.xmi"));
		final Model pbm = EmftvmFactory.eINSTANCE.createModel();
		pbm.setResource(pr);
		
		final Resource r = rs.createResource(URI.createFileURI("out.emftvm"), "org.eclipse.m2m.atl.emftvm");
		final Model emftvmm = EmftvmFactory.eINSTANCE.createModel();
		emftvmm.setResource(r);
		
		final Resource ri = rs.createResource(URI.createFileURI("inlined.emftvm"), "org.eclipse.m2m.atl.emftvm");
		final Model emftvmmi = EmftvmFactory.eINSTANCE.createModel();
		emftvmmi.setResource(ri);
		
		try {
			ExecEnv env = EmftvmFactory.eINSTANCE.createExecEnv();
			env.getMetaModels().put("ATL", atlmm);
			env.getMetaModels().put("Problem", pbmm);
			env.getInputModels().put("IN", atlm);
			env.getOutputModels().put("OUT", pbm);
			env.loadModule(mr, "ATLWFR");
			env.run(new TimingData());
			
			if (getProblems(pbm, pbs) == 0) {
				env = EmftvmFactory.eINSTANCE.createExecEnv();
				env.getMetaModels().put("ATL", atlmm);
				env.getMetaModels().put("Problem", pbmm);
				env.getInputModels().put("IN", atlm);
				env.getOutputModels().put("OUT", emftvmm);
				env.getOutputModels().put("PBS", pbm);
				env.loadModule(mr, "ATLtoEMFTVM");
				env.run(new TimingData());
				
				if (getProblems(pbm, pbs) == 0) {
					env = EmftvmFactory.eINSTANCE.createExecEnv();
					env.getInputModels().put("IN", emftvmm);
					env.getOutputModels().put("OUT", emftvmmi);
					env.loadModule(mr, "InlineCodeblocks");
					env.run(new TimingData());
					
					ri.save(outputStream, Collections.emptyMap());
				}
			}
		} catch (VMException e) {
			ATLLogger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			EmftvmCompilerPlugin.log(e);
		} catch (IOException e) {
			ATLLogger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			EmftvmCompilerPlugin.log(e);
		} finally {
			rs.getResources().remove(pr); // unload
			rs.getResources().remove(r); // unload
			rs.getResources().remove(ri); // unload
		}

		return pbs.toArray(new EObject[pbs.size()]);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.m2m.atl.engine.compiler.AtlStandaloneCompiler#compileWithProblemModel(org.eclipse.m2m.atl.core.IModel, java.lang.String)
	 */
	public EObject[] compileWithProblemModel(IModel atlModel,
			String outputFileName) {
		try {
			File asm = new File(outputFileName);
			if (asm.exists()) {
				asm.delete();
			}
			asm.createNewFile();
			outputFileName = outputFileName.substring(0, outputFileName.lastIndexOf('.')) + ".emftvm";
			return compileWithProblemModel(atlModel, new FileOutputStream(outputFileName));
		} catch (IOException e) {
			ATLLogger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			EmftvmCompilerPlugin.log(e);
		}
		return new EObject[0];
	}

	/**
	 * Retrieves problem elements from <code>problems</code>.
	 * @param problems the problems model
	 * @param pbElements the collection of problem elements to augment
	 * @return the number of error problems
	 */
	@SuppressWarnings("unchecked")
	protected int getProblems(IModel problems, Collection<EObject> pbElements) {
		final Collection<EObject> pbs = (Collection<EObject>) problems.getElementsByType(
				problems.getReferenceModel().getMetaElementByName("Problem")); //$NON-NLS-1$

		int nbErrors = 0;
		if (pbs != null) {
			for (EObject pb : pbs) {
				EStructuralFeature severityFeature = pb.eClass().getEStructuralFeature("severity"); //$NON-NLS-1$
				if (severityFeature != null && "error".equals(((EEnumLiteral)pb.eGet(severityFeature)).getName())) { //$NON-NLS-1$
					nbErrors++;
				}
			}
			pbElements.addAll(pbs);
		}

		return nbErrors;
	}

	/**
	 * Retrieves problem elements from <code>problems</code>.
	 * @param problems the problems model
	 * @param pbElements the collection of problem elements to augment
	 * @return the number of error problems
	 */
	protected int getProblems(Model problems, Collection<EObject> pbElements) {
		final Collection<EObject> pbs = (Collection<EObject>) problems.allInstancesOf(
				(EClass) pbmm.findType("Problem")); //$NON-NLS-1$

		int nbErrors = 0;
		if (pbs != null) {
			for (EObject pb : pbs) {
				EStructuralFeature severityFeature = pb.eClass().getEStructuralFeature("severity"); //$NON-NLS-1$
				if (severityFeature != null && "error".equals(((EEnumLiteral)pb.eGet(severityFeature)).getName())) { //$NON-NLS-1$
					nbErrors++;
				}
			}
			pbElements.addAll(pbs);
		}

		return nbErrors;
	}

}
