/*******************************************************************************
 * Copyright (c) 2008, 2009 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2m.atl.core.service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2m.atl.core.ATLCoreException;
import org.eclipse.m2m.atl.core.IExtractor;
import org.eclipse.m2m.atl.core.IInjector;
import org.eclipse.m2m.atl.core.IModel;
import org.eclipse.m2m.atl.core.IReferenceModel;
import org.eclipse.m2m.atl.core.ModelFactory;
import org.eclipse.m2m.atl.core.launch.ILauncher;

/**
 * The LauncherService utility class provides a generic way to launch a transformation using the ATL Core API.
 * 
 * @author <a href="mailto:william.piers@obeo.fr">William Piers</a>
 */
public final class LauncherService {

	/** The refining trace metamodel name, as defined by the compiler. */
	public static final String REFINING_TRACE_METAMODEL = "RefiningTrace"; //$NON-NLS-1$

	/** The refining trace model name, as defined by the compiler. */
	public static final String REFINING_TRACE_MODEL = "refiningTrace"; //$NON-NLS-1$

	private LauncherService() {
		super();
	}

	/**
	 * Launches a transformation using the given parameters. Process the injections, extractions.
	 * 
	 * @param mode
	 *            the launching mode
	 * @param monitor
	 *            the progress monitor
	 * @param launcher
	 *            the {@link ILauncher}
	 * @param inModels
	 *            a map defining the source models names: [model name, reference model name]
	 * @param inoutModels
	 *            a map defining the inout models names: [model name, reference model name]
	 * @param outModels
	 *            a map defining the target models names: [model name, reference model name]
	 * @param paths
	 *            a map defining the location of each model name: [model name, model location]
	 * @param options
	 *            a map of transformation options: [key, value]
	 * @param libraries
	 *            a map of libraries modules: [library name, module]
	 * @param modules
	 *            a list of modules to superimpose. The first module overrides the next ones.
	 * @return the transformation result
	 */
	@SuppressWarnings("unchecked")
	public static Object launch(String mode, IProgressMonitor monitor, ILauncher launcher,
			Map<String, String> inModels, Map<String, String> inoutModels, Map<String, String> outModels,
			Map<String, String> paths, Map<String, Object> options, Map<String, InputStream> libraries,
			InputStream... modules) throws ATLCoreException {
		launcher.initialize(options);
		Map<String, String> modelHandlers = (Map<String, String>)options.get("modelHandlers"); //$NON-NLS-1$
		boolean isRefiningTraceMode = "true".equals(options.get("isRefiningTraceMode")); //$NON-NLS-1$//$NON-NLS-2$
		ModelFactory defaultFactory = null;
		IExtractor extractor = null;
		IInjector injector = null;
		try {
			defaultFactory = CoreService.createModelFactory(launcher.getDefaultModelFactoryName());
			extractor = CoreService.getExtractor(defaultFactory.getDefaultExtractorName());
			injector = CoreService.getInjector(defaultFactory.getDefaultInjectorName());
		} catch (CoreException e) {
			throw new ATLCoreException(e.getMessage(), e);
		}

		// REFINING TRACE MODE SUPPORT {
		if (isRefiningTraceMode) {
			IReferenceModel refiningTraceMetamodel = defaultFactory
					.getBuiltInResource(REFINING_TRACE_METAMODEL + ".ecore"); //$NON-NLS-1$

			Map<String, Object> modelOptions = new HashMap<String, Object>();
			modelOptions.put("path", "temp"); //$NON-NLS-1$ //$NON-NLS-2$
			modelOptions.put("modelName", REFINING_TRACE_MODEL); //$NON-NLS-1$
			modelOptions.put("newModel", true); //$NON-NLS-1$
			IModel refiningTraceModel = defaultFactory.newModel(refiningTraceMetamodel, modelOptions);
			launcher.addOutModel(refiningTraceModel, REFINING_TRACE_MODEL, REFINING_TRACE_METAMODEL);
		}
		// }

		// INPUT MODELS INJECTION
		for (Iterator<String> i = inModels.keySet().iterator(); i.hasNext();) {
			String modelName = i.next();
			String referenceModelName = inModels.get(modelName);
			IModel model = getModel(modelName, referenceModelName, false, launcher, defaultFactory, paths,
					injector, modelHandlers);
			launcher.addInModel(model, modelName, referenceModelName);
		}

		// INOUT MODELS INJECTION
		for (Iterator<String> i = inoutModels.keySet().iterator(); i.hasNext();) {
			String modelName = i.next();
			String referenceModelName = inoutModels.get(modelName);
			IModel model = getModel(modelName, referenceModelName, false, launcher, defaultFactory, paths,
					injector, modelHandlers);
			launcher.addInOutModel(model, modelName, referenceModelName);
		}

		// OUTPUT MODELS CREATION
		for (Iterator<String> i = outModels.keySet().iterator(); i.hasNext();) {
			String modelName = i.next();
			String referenceModelName = outModels.get(modelName);
			IModel model = getModel(modelName, referenceModelName, true, launcher, defaultFactory, paths,
					injector, modelHandlers);
			launcher.addOutModel(model, modelName, referenceModelName);
		}

		// LIBRARIES
		for (Iterator<Entry<String, InputStream>> iterator = libraries.entrySet().iterator(); iterator
				.hasNext();) {
			Entry<String, InputStream> library = iterator.next();
			launcher.addLibrary(library.getKey(), library.getValue());
		}

		// LAUNCH
		Object transformationResult = launcher.launch(mode, monitor, options,(Object[])modules);

		// OUTPUT MODELS EXTRACTION
		for (Iterator<String> i = outModels.keySet().iterator(); i.hasNext();) {
			String modelName = i.next();
			extractor.extract(launcher.getModel(modelName), paths.get(modelName));
		}

		// INPUT/OUTPUT MODELS EXTRACTION
		for (Iterator<String> i = inoutModels.keySet().iterator(); i.hasNext();) {
			String modelName = i.next();
			String path = paths.get(getRefinedModelName(modelName));
			if (path != null) {
				extractor.extract(launcher.getModel(modelName), path);
			}
			// otherwise its considered as an input model
		}

		return transformationResult;
	}

	/**
	 * Launches a transformation using the given parameters. Process the injections, extractions.
	 * 
	 * @param mode
	 *            the launching mode
	 * @param monitor
	 *            the progress monitor
	 * @param launcher
	 *            the {@link ILauncher}
	 * @param modelFactories
	 *            a map defining which {@link ModelFactory} to use, for each model name
	 * @param extractors
	 *            a map defining which {@link IExtractor} to use, for each model name
	 * @param injectors
	 *            a map defining which {@link IInjector} to use, for each model name
	 * @param inModels
	 *            a map defining the source models names: [model name, reference model name]
	 * @param inoutModels
	 *            a map defining the inout models names: [model name, reference model name]
	 * @param outModels
	 *            a map defining the target models names: [model name, reference model name]
	 * @param paths
	 *            a map defining the location of each model name: [model name, model location]
	 * @param options
	 *            a map of transformation options: [key, value]
	 * @param libraries
	 *            a map of libraries modules: [library name, module]
	 * @param modules
	 *            a list of modules to superimpose. The first module overrides the next ones.
	 * @return the transformation result
	 */
	@SuppressWarnings("unchecked")
	public static Object launch(String mode, IProgressMonitor monitor, ILauncher launcher,
			Map<String, ModelFactory> modelFactories, Map<String, IExtractor> extractors,
			Map<String, IInjector> injectors, Map<String, String> inModels, Map<String, String> inoutModels,
			Map<String, String> outModels, Map<String, String> paths, Map<String, Object> options,
			Map<String, InputStream> libraries, InputStream... modules) throws ATLCoreException {

		launcher.initialize(options);
		Map<String, String> modelHandlers = (Map<String, String>)options.get("modelHandlers"); //$NON-NLS-1$
		boolean isRefiningTraceMode = "true".equals(options.get("isRefiningTraceMode")); //$NON-NLS-1$//$NON-NLS-2$

		// REFINING TRACE MODE SUPPORT {
		if (isRefiningTraceMode) {
			ModelFactory factory = modelFactories.get(REFINING_TRACE_METAMODEL);
			IReferenceModel refiningTraceMetamodel = factory.getBuiltInResource(REFINING_TRACE_METAMODEL
					+ ".ecore"); //$NON-NLS-1$

			Map<String, Object> modelOptions = new HashMap<String, Object>();
			modelOptions.put("path", "temp"); //$NON-NLS-1$ //$NON-NLS-2$
			modelOptions.put("modelName", REFINING_TRACE_MODEL); //$NON-NLS-1$
			modelOptions.put("newModel", true); //$NON-NLS-1$
			IModel refiningTraceModel = factory.newModel(refiningTraceMetamodel, modelOptions);
			launcher.addOutModel(refiningTraceModel, REFINING_TRACE_MODEL, REFINING_TRACE_METAMODEL);
		}
		// }

		// INPUT MODELS INJECTION
		for (Iterator<String> i = inModels.keySet().iterator(); i.hasNext();) {
			String modelName = i.next();
			String referenceModelName = inModels.get(modelName);
			IModel model = getModel(modelName, referenceModelName, false, launcher, modelFactories
					.get(referenceModelName), paths, injectors.get(modelName), modelHandlers);
			launcher.addInModel(model, modelName, referenceModelName);
		}

		// INOUT MODELS INJECTION
		for (Iterator<String> i = inoutModels.keySet().iterator(); i.hasNext();) {
			String modelName = i.next();
			String referenceModelName = inoutModels.get(modelName);
			IModel model = getModel(modelName, referenceModelName, false, launcher, modelFactories
					.get(referenceModelName), paths, injectors.get(modelName), modelHandlers);
			launcher.addInOutModel(model, modelName, referenceModelName);
		}

		// OUTPUT MODELS CREATION
		for (Iterator<String> i = outModels.keySet().iterator(); i.hasNext();) {
			String modelName = i.next();
			String referenceModelName = outModels.get(modelName);
			IModel model = getModel(modelName, referenceModelName, true, launcher, modelFactories
					.get(referenceModelName), paths, injectors.get(modelName), modelHandlers);
			launcher.addOutModel(model, modelName, referenceModelName);
		}

		// LIBRARIES
		for (Iterator<Entry<String, InputStream>> iterator = libraries.entrySet().iterator(); iterator
				.hasNext();) {
			Entry<String, InputStream> library = iterator.next();
			launcher.addLibrary(library.getKey(), library.getValue());
		}

		// LAUNCH
		Object transformationResult = launcher.launch(mode, monitor, options,(Object[])modules);

		// OUTPUT MODELS EXTRACTION
		for (Iterator<String> i = outModels.keySet().iterator(); i.hasNext();) {
			String modelName = i.next();
			extractors.get(modelName).extract(launcher.getModel(modelName), paths.get(modelName));
		}

		// INPUT/OUTPUT MODELS EXTRACTION
		for (Iterator<String> i = inoutModels.keySet().iterator(); i.hasNext();) {
			String modelName = i.next();
			extractors.get(modelName).extract(launcher.getModel(modelName),
					paths.get(getRefinedModelName(modelName)));
		}

		return transformationResult;
	}

	private static IModel getModel(String modelName, String referenceModelName, boolean newModel,
			ILauncher launcher, ModelFactory modelFactory, Map<String, String> paths, IInjector injector,
			Map<String, String> modelHandlers) throws ATLCoreException {
		IReferenceModel referenceModel = (IReferenceModel)launcher.getModel(referenceModelName);

		if (referenceModel == null) {
			Map<String, Object> referenceModelOptions = new HashMap<String, Object>();
			if (modelHandlers != null) {
				referenceModelOptions.put("modelHandlerName", modelHandlers.get(referenceModelName)); //$NON-NLS-1$
			}
			referenceModelOptions.put("modelName", referenceModelName); //$NON-NLS-1$
			referenceModelOptions.put("path", paths.get(referenceModelName)); //$NON-NLS-1$
			referenceModel = modelFactory.newReferenceModel(referenceModelOptions);
			injector.inject(referenceModel, paths.get(referenceModelName));
		}

		Map<String, Object> modelOptions = new HashMap<String, Object>();
		modelOptions.put("modelName", modelName); //$NON-NLS-1$
		modelOptions.put("path", paths.get(modelName)); //$NON-NLS-1$
		modelOptions.put("newModel", newModel); //$NON-NLS-1$

		IModel model = modelFactory.newModel(referenceModel, modelOptions);
		if (!newModel) {
			injector.inject(model, paths.get(modelName));
		}
		return model;
	}

	/**
	 * Returns the refined model name.
	 * 
	 * @param modelName
	 *            the inital model name
	 * @return the refined model name
	 */
	public static String getRefinedModelName(String modelName) {
		return "REFINED#" + modelName; //$NON-NLS-1$
	}

}
