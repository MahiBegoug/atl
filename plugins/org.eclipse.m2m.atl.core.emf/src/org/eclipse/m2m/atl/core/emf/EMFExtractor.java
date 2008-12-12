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
package org.eclipse.m2m.atl.core.emf;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.m2m.atl.ATLLogger;
import org.eclipse.m2m.atl.core.IExtractor;
import org.eclipse.m2m.atl.core.IModel;

/**
 * The EMF implementation of the {@link IExtractor} interface.
 * 
 * @author <a href="mailto:william.piers@obeo.fr">William Piers</a>
 */
public class EMFExtractor implements IExtractor {

	/**
	 * {@inheritDoc}
	 *
	 * @see org.eclipse.m2m.atl.core.IExtractor#extract(org.eclipse.m2m.atl.core.IModel, java.lang.Object, java.util.Map)
	 */
	public void extract(IModel targetModel, Object target, Map<String, Object> options) {
		Map<?, ?> pathMap = null;
		ResourceSet resourceSet = ((EMFModelFactory)targetModel.getModelFactory()).getResourceSet();
		
		Object contentType = options.get(EMFModelFactory.OPTION_CONTENT_TYPE);
		Object object = target.toString();
		if (object instanceof Map<?, ?>) {
			pathMap = (Map<?, ?>)object;
			for (Resource resource : ((EMFModel)target).getResources()) {
				String path = (String)pathMap.get(resource);
				if (path.startsWith("ext:")) { //$NON-NLS-1$
					path = path.substring(4);
				}
				extract(resourceSet, resource, path, contentType, options);
			}
		} else if (object instanceof String) {
			String path = (String)object;
			if (path.startsWith("ext:")) { //$NON-NLS-1$
				path = path.substring(4);
			}
			if (!((EMFModel)targetModel).getResources().isEmpty()) {
				extract(resourceSet, ((EMFModel)targetModel).getResources().get(0), path, contentType, options);
			} else {
				ATLLogger.severe(Messages.getString("EMFExtractor.NO_RESOURCE", new Object[] {path})); //$NON-NLS-1$
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.m2m.atl.core.IExtractor#extract(org.eclipse.m2m.atl.core.IModel, java.lang.Object)
	 */
	public void extract(IModel targetModel, Object target) {
		extract(targetModel, target, Collections.<String, Object> emptyMap());
	}

	private void extract(ResourceSet resourceSet, Resource resource, String path, Object contentType, Map<String, Object> options) {
		//TODO do not systematically recreate the resource
		Resource newResource = null;
		if (contentType == null) {
			newResource = resourceSet.createResource(URI.createFileURI(path));
		} else {
			// TODO compatibility
			// newResource = EMFModelFactory.getResourceSet().createResource(URI.createFileURI(path),
			// (String)contentType);
			newResource = resourceSet.createResource(URI.createFileURI(path));
		}
		newResource.getContents().addAll(resource.getContents());

		// default options, may be replaced
		Map<String, Object> extractOptions = new HashMap<String, Object>();
		extractOptions.put(XMLResource.OPTION_ENCODING, "ISO-8859-1"); //$NON-NLS-1$
		extractOptions.put(XMLResource.OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.FALSE);
		extractOptions.putAll(options);

		try {
			newResource.save(extractOptions);
		} catch (IOException e) {
			ATLLogger.log(Level.SEVERE, Messages.getString("EMFExtractor.ERROR_EXTRACTING", path), e); //$NON-NLS-1$
		}
		resourceSet.getResources().remove(newResource);
	}

}
