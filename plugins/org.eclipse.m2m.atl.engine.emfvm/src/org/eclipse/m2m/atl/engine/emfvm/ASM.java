/*******************************************************************************
 * Copyright (c) 2007, 2008 INRIA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    INRIA - initial API and implementation
 *    Obeo - refactoring
 *    
 *******************************************************************************/
package org.eclipse.m2m.atl.engine.emfvm;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.m2m.atl.ATLLogger;
import org.eclipse.m2m.atl.core.IReferenceModel;
import org.eclipse.m2m.atl.engine.emfvm.AtlSuperimposeModule.AtlSuperimposeModuleException;
import org.eclipse.m2m.atl.engine.emfvm.adapter.EMFModelAdapter;
import org.eclipse.m2m.atl.engine.emfvm.adapter.IModelAdapter;
import org.eclipse.m2m.atl.engine.emfvm.adapter.UML2ModelAdapter;
import org.eclipse.m2m.atl.engine.emfvm.lib.ASMModule;
import org.eclipse.m2m.atl.engine.emfvm.lib.ExecEnv;
import org.eclipse.m2m.atl.engine.emfvm.lib.Extension;

/**
 * The ASM Class, which manages an ASM program.
 * 
 * @author <a href="mailto:frederic.jouault@univ-nantes.fr">Frederic Jouault</a>
 * @author <a href="mailto:mikael.barbero@univ-nantes.fr">Mikael Barbero</a>
 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
 * @author <a href="mailto:william.piers@obeo.fr">William Piers</a>
 */
public class ASM {

	private String name;

	private List operations = new ArrayList();

	private List fields = new ArrayList();

	private ASMOperation mainOperation;

	/**
	 * ASM Constructor.
	 */
	public ASM() {
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Adds a field.
	 * 
	 * @param fieldName
	 *            the field name
	 * @param type
	 *            the field type
	 */
	public void addField(String fieldName, String type) {
		fields.add(fieldName);
	}

	/**
	 * Adds an operation.
	 * 
	 * @param operation
	 *            the operation to add
	 */
	public void addOperation(ASMOperation operation) {
		operations.add(operation);
		if (operation.getName().equals("main") && operation.getContext().equals("A")) { //$NON-NLS-1$ //$NON-NLS-2$
			mainOperation = operation;
		}
	}

	/**
	 * Returns All registered operations.
	 * 
	 * @return All registered operations
	 * @see #addOperation(ASMOperation)
	 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
	 */
	public Iterator getOperations() {
		return operations.iterator();
	}

	/**
	 * Returns "main" operation, if any.
	 * 
	 * @return "main" operation, if any
	 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
	 */
	public ASMOperation getMainOperation() {
		return mainOperation;
	}

	// TODO analyze:
	// - implements other options
	// - define options somewhere (currently, best definition is in regular VM)
	/**
	 * Launches the ASM.
	 * 
	 * @param models
	 *            the model map
	 * @param libraries
	 *            the library map
	 * @param superimpose
	 *            the superimpose list
	 * @param options
	 *            the option map
	 * @return the execution result
	 */
	public Object run(Map models, Map libraries, List superimpose, Map options) {
		Object ret = null;

		boolean printExecutionTime = "true".equals(options.get("printExecutionTime")); //$NON-NLS-1$ //$NON-NLS-2$

		long startTime = System.currentTimeMillis();

		ExecEnv execEnv = new ExecEnv(models);

		IModelAdapter modelAdapter;

		if ("true".equals(options.get("supportUML2Stereotypes"))) { //$NON-NLS-1$ //$NON-NLS-2$
			modelAdapter = new UML2ModelAdapter(execEnv);
		} else {
			modelAdapter = new EMFModelAdapter(execEnv);
		}

		// by default (if options.get("checkSameModel") == null) interModelReferences are not allowed
		modelAdapter.setAllowInterModelReferences("false".equals(options.get("checkSameModel"))); //$NON-NLS-1$ //$NON-NLS-2$
		execEnv.init(modelAdapter);

		if ("true".equals(options.get("step"))) { //$NON-NLS-1$ //$NON-NLS-2$
			execEnv.setStep(true);
		}

		String ext = (String)options.get("extensions"); //$NON-NLS-1$
		if (ext != null) {
			ClassLoader cl = getClass().getClassLoader();

			String extraClasspath = (String)options.get("extraClasspath"); //$NON-NLS-1$
			if (extraClasspath != null) {
				String[] paths = extraClasspath.split(","); //$NON-NLS-1$
				URL[] urls = new URL[paths.length];
				String userDir = (String)options.get("user.dir"); //$NON-NLS-1$
				if (userDir == null) {
					userDir = System.getProperty("user.dir"); //$NON-NLS-1$
				}
				for (int i = 0; i < paths.length; i++) {
					try {
						urls[i] = new File(userDir, paths[i]).toURI().toURL();
					} catch (MalformedURLException e) {
						throw new VMException(null,Messages.getString(
								"ASM.LOADINGERROR", new Object[] {paths[i]}), e); //$NON-NLS-1$
					}
				}
				cl = new URLClassLoader(urls, cl);
			}

			String[] extensions = ext.split(","); //$NON-NLS-1$
			for (int i = 0; i < extensions.length; i++) {
				try {
					Extension extension = (Extension)cl.loadClass(extensions[i]).newInstance();
					extension.apply(execEnv, options);
				} catch (ClassNotFoundException e) {
					throw new VMException(null,Messages.getString(
							"ASM.EXTLOADINGERROR", new Object[] {extensions[i]}), e); //$NON-NLS-1$
				} catch (InstantiationException e) {
					throw new VMException(null,Messages.getString(
							"ASM.EXTINSTANTIATEERROR", new Object[] {extensions[i]}), e); //$NON-NLS-1$
				} catch (IllegalAccessException e) {
					throw new VMException(null,Messages.getString(
							"ASM.EXTINSTANTIATEERROR", new Object[] {extensions[i]}), e); //$NON-NLS-1$
				}
			}
		}
		List extensionObjects = (List)options.get("extensionObjects"); //$NON-NLS-1$
		if (extensionObjects != null) {
			for (Iterator i = extensionObjects.iterator(); i.hasNext();) {
				((Extension)i.next()).apply(execEnv, options);
			}
		}

		ASMModule asmModule = new ASMModule();
		StackFrame frame = new StackFrame(execEnv, asmModule, mainOperation);

		for (Iterator i = libraries.values().iterator(); i.hasNext();) {
			ASM library = (ASM)i.next();
			registerOperations(execEnv, library.operations);
			if (library.mainOperation != null) {
				library.mainOperation.exec(new StackFrame(execEnv, asmModule, library.mainOperation));
			}
		}

		// register module operations after libraries to avoid overriding
		// "main" in execEnv (avoid superimposition problems)
		registerOperations(execEnv, operations);

		for (Iterator i = superimpose.iterator(); i.hasNext();) {
			ASM module = (ASM)i.next();
			AtlSuperimposeModule ami = new AtlSuperimposeModule(execEnv, module);
			try {
				ami.adaptModuleOperations();
			} catch (AtlSuperimposeModuleException e) {
				throw new VMException(frame, e.getLocalizedMessage(), e);
			}
			registerOperations(execEnv, module.operations);
		}

		ret = mainOperation.exec(frame);
		execEnv.terminated();
		long endTime = System.currentTimeMillis();
		if (printExecutionTime) {
			ATLLogger.info(Messages.getString(
					"ASM.EXECUTIONTIME", new Object[] {name, new Double((endTime - startTime) / 1000.)})); //$NON-NLS-1$
		}
		if ("true".equals(options.get("showSummary"))) { //$NON-NLS-1$ //$NON-NLS-2$
			ATLLogger.info(Messages.getString(
					"ASM.INSTRUCTIONSCOUNT", new Object[] {new Double(execEnv.getNbExecutedBytecodes())})); //$NON-NLS-1$
		}
		return ret;
	}

	/**
	 * Registers all ATL operations.
	 * 
	 * @param execEnv
	 *            the execution environment where to register operations
	 * @param operationsToRegister
	 *            the list of operations to register
	 */
	public void registerOperations(ExecEnv execEnv, List operationsToRegister) {
		for (Iterator i = operationsToRegister.iterator(); i.hasNext();) {
			ASMOperation op = (ASMOperation)i.next();
			String signature = op.getContext();
			if (signature.matches("^(Q|G|C|E|O|N).*$")) { //$NON-NLS-1$
				// Sequence, Bag, Collection, Set, OrderedSet, Native type
				ATLLogger
						.warning(Messages.getString("ASM.UNSUPPORTEDREGISTRATION", new Object[] {signature})); //$NON-NLS-1$
			} else {
				try {
					Object type = parseType(execEnv, new StringCharacterIterator(signature));
					execEnv.registerOperation(type, op, op.getName());
				} catch (SignatureParsingException spe) {
					throw new VMException(null, spe.getLocalizedMessage(), spe);
				}
			}
		}
	}

	// read until c, including c
	// returns everything read before c
	private String readUntil(CharacterIterator ci, char c) throws SignatureParsingException {
		StringBuffer ret = new StringBuffer();

		while (ci.current() != c) {
			ret.append(ci.current());
			ci.next();
		}
		read(ci, c);

		return ret.toString();
	}

	private void read(CharacterIterator ci, char c) throws SignatureParsingException {
		if (ci.current() != c) {
			throw new SignatureParsingException(
					Messages
							.getString(
									"ASM.PARSINGERROR", new Object[] {new Character(c), new Character(ci.current()), new Integer(ci.getIndex())})); //$NON-NLS-1$
		}
		ci.next();
	}

	// Type may be java.lang.Class, EClass, OclType
	private Object parseType(ExecEnv execEnv, CharacterIterator ci) throws SignatureParsingException {
		Object ret = parseTypeInternal(execEnv, ci);

		if (ci.next() != CharacterIterator.DONE) {
			throw new SignatureParsingException(Messages.getString(
					"ASM.SIGNATUREPARSINGERROR", new Object[] {new Integer(ci.getIndex())})); //$NON-NLS-1$
		}

		return ret;
	}

	private Object parseTypeInternal(ExecEnv execEnv, CharacterIterator ci) throws SignatureParsingException {
		Object ret = null;

		switch (ci.current()) {
			// case 'Q': case 'G': case 'C': // Sequence, Bag, Collection,
			// case 'E': case 'O': case 'N': // Set, OrderedSet, Native type
			// ci.next();
			// //ASMOclType elementType = parseTypeInternal(ci);
			// read(ci, ';');
			// break;
			// case 'T': // Tuple
			// ci.next();
			// Map attrs = new HashMap();
			// while(ci.current() != ';') {
			// ASMOclType attrType = parseTypeInternal(ci);
			// String attrName = readUntil(ci, ';');
			// //attrs.put(attrName, attrType); //TO DO: correct type
			// attrs.put(attrName, ASMOclAny.myType);
			// }
			// ret = new ASMTupleType(attrs);
			// break;
			case 'M': // Metamodel Class
				ci.next();
				String mname = readUntil(ci, '!');
				String modelName = readUntil(ci, ';');
				IReferenceModel model = (IReferenceModel)execEnv.getModel(mname);
				if (model != null) {
					Object ec = model.getMetaElementByName(modelName);
					if (ec == null) {
						throw new SignatureParsingException(Messages.getString(
								"ASM.MODELELEMENTNOTFOUND", new Object[] {modelName, mname})); //$NON-NLS-1$
					}
					ret = ec;
				} else {
					throw new VMException(null,Messages.getString("ASM.MODELNOTFOUND", new Object[] {mname})); //$NON-NLS-1$
				}
				break;

			// Primitive types, VM Types
			case 'A': // Module
				ret = ASMModule.class;
				ci.next();
				break;
			case 'J': // Object => OclAny ?
				ret = Object.class;
				ci.next();
				break;
			// case 'V': // Void
			// ret = ASMOclUndefined.myType;
			// ci.next();
			// break;
			case 'I': // Integer
				ret = Integer.class;
				ci.next();
				break;
			case 'B': // Boolean
				ret = Boolean.class;
				ci.next();
				break;
			case 'S': // String
				ret = String.class;
				ci.next();
				break;
			// case 'Z': // String
			// ret = ASMEnumLiteral.myType;
			// ci.next();
			// break;
			case 'D': // Real
				ret = Double.class;
				ci.next();
				break;
			// case 'L': // Model
			// ret = ASMModel.myType;
			// ci.next();
			// break;
			case CharacterIterator.DONE:
				throw new SignatureParsingException(Messages.getString(
						"ASM.SIGNATUREPARSINGERROR", new Object[] {new Integer(ci.getIndex())})); //$NON-NLS-1$
			default:
				throw new SignatureParsingException(Messages.getString(
						"ASM.UNKNOWTYPECODE", new Object[] {new Character(ci.current())})); //$NON-NLS-1$
		}

		return ret;
	}

	/**
	 * Exception dedicated to signature parsing issues.
	 */
	private class SignatureParsingException extends Exception {

		private static final long serialVersionUID = 7488097967558841786L;

		public SignatureParsingException(String msg) {
			super(msg);
		}
	}
}
