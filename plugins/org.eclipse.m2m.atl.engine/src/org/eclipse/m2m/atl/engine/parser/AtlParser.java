/*******************************************************************************
 * Copyright (c) 2004 INRIA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Frederic Jouault (INRIA) - initial API and implementation
 *	   William Piers (Obeo) - refactoring
 *     Dennis Wagelaar (Vrije Universiteit Brussel)
 *******************************************************************************/
package org.eclipse.m2m.atl.engine.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.gmt.tcs.injector.TCSInjector;
import org.eclipse.m2m.atl.ATLLogger;
import org.eclipse.m2m.atl.drivers.emf4atl.ASMEMFModel;
import org.eclipse.m2m.atl.drivers.emf4atl.ASMEMFModelElement;
import org.eclipse.m2m.atl.drivers.emf4atl.AtlEMFModelHandler;
import org.eclipse.m2m.atl.engine.vm.AtlModelHandler;
import org.eclipse.m2m.atl.engine.vm.ModelLoader;
import org.eclipse.m2m.atl.engine.vm.nativelib.ASMModel;

/**
 * The ATL parser.
 * 
 * @author <a href="mailto:frederic.jouault@univ-nantes.fr">Frederic Jouault</a>
 * @author <a href="mailto:william.piers@obeo.fr">William Piers</a>
 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
 */
public final class AtlParser {

	private static AtlParser defaultParser;

	private AtlModelHandler amh;

	private AtlParser() {
		amh = AtlModelHandler.getDefault(AtlEMFModelHandler.ID);
	}

	/**
	 * Returns the default ATL parser.
	 * 
	 * @return the default ATL parser
	 */
	public static AtlParser getDefault() {
		if (defaultParser == null) {
			defaultParser = new AtlParser();
		}
		return defaultParser;
	}

	/**
	 * Parses the given input stream.
	 * 
	 * @param in
	 *            an input stream
	 * @return the resulting EObject
	 */
	public EObject parse(InputStream in) {
		return parseWithProblems(in)[0];
	}

	/**
	 * Parses the given input stream.
	 * 
	 * @param in
	 *            an input stream
	 * @return the resulting ASMModel
	 */
	public ASMModel parseToModel(InputStream in) {
		return parseToModelWithProblems(in, true)[0];
	}

	/**
	 * Parses the given input stream.
	 * 
	 * @param in
	 *            an input stream
	 * @param hideErrors
	 *            disable standard output in order to hide errors
	 * @return the parser resulting ASMModel[model,problemModel]
	 */
	public ASMModel[] parseToModelWithProblems(InputStream in, boolean hideErrors) {
		final ASMModel[] ret = new ASMModel[2];
		final ModelLoader ml = amh.createModelLoader();
		final ASMModel atlmm = ml.getATL();
		final ASMModel pbmm = ml.getBuiltInMetaModel("Problem"); //$NON-NLS-1$

		try {
			ret[0] = ml.newModel("temp", "temp", atlmm); //$NON-NLS-1$ //$NON-NLS-2$
			ret[1] = ml.newModel("pb", "pb", pbmm); //$NON-NLS-1$ //$NON-NLS-2$

			final TCSInjector ebnfi = new TCSInjector();
			final Map params = new HashMap();
			params.put("name", "ATL"); //$NON-NLS-1$ //$NON-NLS-2$
			params.put("problems", ret[1]); //$NON-NLS-1$

			if (hideErrors) {
				//TODO Find another way to hide parsing errors.
				//
				// Dennis Wagelaar: ATL does not "own" the System object in Eclipse, 
				// so don't fight with the other threads over who gets to set System.out.
				// Even though System.out is only temporarily redirected here,
				// it may cause unexpected results with other threads that "inexplicably"
				// lose some System.out.println() output.
				//
				// N.B. This was also the reason to switch from System.out logging to
				// a logging framework, as ATL previously hijacked System.out for its own
				// console widget. MOFscript was then fighting with ATL over who gets
				// to hijack System.out, resulting in MOFscript output to show up in the
				// ATL console.

				// desactivate standard output
//				OutputStream stream = new ByteArrayOutputStream();
//				PrintStream out = new PrintStream(stream);
//				PrintStream origOut = System.out;
//				System.setOut(out);

				// launch parsing
				ebnfi.inject(ret[0], in, params);

				// reactivate standard output
//				System.setOut(origOut);
//				stream.close();
//				out.close();
			} else {
				// launch parsing
				ebnfi.inject(ret[0], in, params);
			}

		} catch (IOException e) {
			ATLLogger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (Exception e) {
			ATLLogger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}

		return ret;
	}

	/**
	 * Returns An array of EObject, the first one being an ATL!Unit and the following ones Problem!Problem.
	 * 
	 * @param in
	 *            InputStream to parse ATL code from.
	 * @return An array of EObject, the first one being an ATL!Unit and the following ones Problem!Problem.
	 */
	public EObject[] parseWithProblems(InputStream in) {
		return convertToEmf(parseToModelWithProblems(in, true), "Unit"); //$NON-NLS-1$
	}

	/**
	 * ATL injector launcher.
	 * 
	 * @param expression
	 *            an ATL expression
	 * @param expressionType
	 *            the ATL expression type the Syntax Element parsed
	 * @param hideErrors
	 *            disable standard output in order to hide errors
	 * @return outputs models
	 */
	public EObject[] parseExpression(String expression, String expressionType, boolean hideErrors) {
		if (expressionType == null) {
			return null;
		}
		final ASMModel[] ret = new ASMModel[2];
		final ModelLoader ml = amh.createModelLoader();
		final ASMModel atlmm = ml.getATL();
		final ASMModel pbmm = ml.getBuiltInMetaModel("Problem"); //$NON-NLS-1$

		try {
			ret[0] = ml.newModel("temp", "temp", atlmm); //$NON-NLS-1$ //$NON-NLS-2$
			ret[1] = ml.newModel("pb", "pb", pbmm); //$NON-NLS-1$ //$NON-NLS-2$
			TCSInjector ebnfi = new TCSInjector();
			Map params = new HashMap();
			params.put("name", "ATL-" + expressionType); //$NON-NLS-1$ //$NON-NLS-2$
			params.put("problems", ret[1]); //$NON-NLS-1$

			if (hideErrors) {
				// desactivate standard output
				OutputStream stream = new ByteArrayOutputStream();
				PrintStream out = new PrintStream(stream);
				PrintStream origOut = System.out;
				System.setOut(out);

				// launch parsing
				ebnfi.inject(ret[0], new ByteArrayInputStream(expression.getBytes()), params);

				// reactivate standard output
				System.setOut(origOut);
				stream.close();
				out.close();
			} else {
				// launch parsing
				ebnfi.inject(ret[0], new ByteArrayInputStream(expression.getBytes()), params);
			}

		} catch (Exception e) {
			// nothing : silent incorrect expressions parsing
		}
		String rootTypeName = Character.toUpperCase(expressionType.charAt(0)) + expressionType.substring(1);
		return convertToEmf(ret, rootTypeName);
	}

	private static EObject[] convertToEmf(ASMModel[] parsed, String rootTypeName) {
		EObject[] ret = null;
		EObject retUnit = null;
		Collection pbs = null;

		ASMModel atlmodel = parsed[0];
		ASMModel problems = parsed[1];
		if (atlmodel instanceof ASMEMFModel) {
			Collection modules = atlmodel.getElementsByType(rootTypeName);
			if (modules.size() > 0) {
				retUnit = ((ASMEMFModelElement)modules.iterator().next()).getObject();
			}
			pbs = problems.getElementsByType("Problem"); //$NON-NLS-1$
		}

		if (pbs != null) {
			ret = new EObject[1 + pbs.size()];
			int k = 1;
			for (Iterator i = pbs.iterator(); i.hasNext();) {
				ret[k++] = ((ASMEMFModelElement)i.next()).getObject();
			}
		} else {
			ret = new EObject[1];
		}
		ret[0] = retUnit;

		return ret;
	}

}
