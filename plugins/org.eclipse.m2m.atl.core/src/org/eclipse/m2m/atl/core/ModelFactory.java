/*******************************************************************************
 * Copyright (c) 2008 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2m.atl.core;

import java.util.Map;

/**
 * The ModelFactory abstract class allows to create {@link IModel} and {@link IReferenceModel}.
 * 
 * @author <a href="mailto:william.piers@obeo.fr">William Piers</a>
 */
public abstract class ModelFactory {

	/**
	 * Returns a default {@link IInjector} associated with the current factory.
	 * 
	 * @return the default {@link IInjector}
	 */
	public abstract IInjector getDefaultInjector();

	/**
	 * Returns a default {@link IExtractor} associated with the current factory.
	 * 
	 * @return the default {@link IExtractor}
	 */
	public abstract IExtractor getDefaultExtractor();

	/**
	 * Creates a new {@link IReferenceModel} using default options.
	 * 
	 * @return a new {@link IReferenceModel}
	 */
	public abstract IReferenceModel newReferenceModel();
	
	/**
	 * Creates a new {@link IReferenceModel} using specified options.
	 * 
	 * @param options
	 *            the creation options
	 * @return a new {@link IReferenceModel}
	 */
	public abstract IReferenceModel newReferenceModel(Map<String, Object> options);

	/**
	 * Creates a new {@link IModel} using default options and conforming to the given
	 * {@link IReferenceModel}.
	 * 
	 * @param referenceModel
	 *            the {@link IReferenceModel}
	 * @return a new {@link IModel}
	 */
	public abstract IModel newModel(IReferenceModel referenceModel);
	
	/**
	 * Creates a new {@link IModel} using specified options and conforming to the given
	 * {@link IReferenceModel}.
	 * 
	 * @param referenceModel
	 *            the {@link IReferenceModel}
	 * @param options
	 *            the creation options
	 * @return a new {@link IModel}
	 */
	public abstract IModel newModel(IReferenceModel referenceModel, Map<String, Object> options);

	/**
	 * Returns the built-in resource matching the given name.
	 * 
	 * @param name
	 *            the resource name
	 * @return the built-in resource matching the given name
	 */
	public abstract IReferenceModel getBuiltInResource(String name);
}
