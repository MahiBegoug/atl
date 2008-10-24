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
package org.eclipse.m2m.atl.engine.parser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.m2m.atl.ATLLogger;

/**
 * ATL source inspector, used to catch main file informations. Also allows to update them.
 * 
 * @author <a href="mailto:william.piers@obeo.fr">William Piers</a>
 */
public final class AtlSourceManager {

	// ATL File Type:
	/** 0 : undefined. */
	public static final int ATL_FILE_TYPE_UNDEFINED = 0;

	/** 0 : module. */
	public static final int ATL_FILE_TYPE_MODULE = 1;

	/** 0 : query. */
	public static final int ATL_FILE_TYPE_QUERY = 2;

	/** 0 : library. */
	public static final int ATL_FILE_TYPE_LIBRARY = 3;

	// Metamodel filter types:
	/** 0 : input + output metamodels. */
	public static final int ALL_METAMODELS = 0;

	/** 1 : input metamodels. */
	public static final int INPUT_METAMODELS = 1;

	/** 2 : OUTPUT metamodels. */
	public static final int OUTPUT_METAMODELS = 2;

	/** URI tag value. */
	public static final String URI_TAG = "nsURI"; //$NON-NLS-1$

	/** PATH tag value. */
	public static final String PATH_TAG = "path"; //$NON-NLS-1$

	private static final ResourceSet RESOURCE_SET = new ResourceSetImpl();

	/** The detected metamodels Map[id,List[EPackage]]. */
	private Map metamodelsPackages;

	/** Input models / metamodels names Map. */
	private Map inputModels;

	/** Output models / metamodels names Map. */
	private Map outputModels;

	private List librariesImports;

	private int atlFileType;

	private boolean initialized;

	private EObject model;

	private Map metamodelLocations;

	/**
	 * Creates an atl source manager.
	 */
	public AtlSourceManager() {
		super();
	}

	/**
	 * Returns the ATL file type.
	 * 
	 * @return the ATL file type
	 */
	public int getATLFileType() {
		return atlFileType;
	}

	public Map getInputModels() {
		return inputModels;
	}

	public Map getOutputModels() {
		return outputModels;
	}

	public List getLibrariesImports() {
		return librariesImports;
	}

	/**
	 * Update method : parsing and metamodel detection.
	 * 
	 * @param content
	 *            the content of the atl file
	 */
	public void updateDataSource(String content) {
		try {
			parseMetamodels(content);
		} catch (IOException e) {
			// TODO apply marker on the file
			// Exceptions are detected by the compiler
			// AtlUIPlugin.log(e);
		}
	}

	/**
	 * Update method : parsing and metamodel detection.
	 * 
	 * @param inputStream
	 *            the atl file input stream
	 */
	public void updateDataSource(InputStream inputStream) {
		String content = null;
		try {
			byte[] bytes = new byte[inputStream.available()];
			inputStream.read(bytes);
			content = new String(bytes);
		} catch (IOException e) {
			ATLLogger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		updateDataSource(content);
	}

	public EObject getModel() {
		return model;
	}

	/**
	 * Metamodels access method.
	 * 
	 * @param filter
	 *            the metamodel filter
	 * @return the map of searched metamodels
	 */
	public Map getMetamodelPackages(int filter) {
		if (inputModels == null && inputModels == null) {
			return metamodelsPackages;
		}
		switch (filter) {
			case INPUT_METAMODELS:
				Map inputres = new HashMap();
				for (Iterator iterator = inputModels.values().iterator(); iterator.hasNext();) {
					String id = (String)iterator.next();
					inputres.put(id, metamodelsPackages.get(id));
				}
				return inputres;
			case OUTPUT_METAMODELS:
				Map outputres = new HashMap();
				for (Iterator iterator = outputModels.values().iterator(); iterator.hasNext();) {
					String id = (String)iterator.next();
					outputres.put(id, inputModels.get(id));
				}
				return outputres;
			default:
				return metamodelsPackages;
		}
	}

	/**
	 * Access on a specific metamodel.
	 * 
	 * @param metamodelId
	 *            the metamodel id
	 * @return the metamodels list
	 */
	public List getMetamodelPackages(String metamodelId) {
		return (List)metamodelsPackages.get(metamodelId);
	}

	/**
	 * Parsing method : detects uris and stores metamodels.
	 * 
	 * @param text
	 *            the atl file.
	 * @throws IOException
	 */
	private void parseMetamodels(String text) throws IOException {
		metamodelsPackages = new HashMap();
		metamodelLocations = new HashMap();
		inputModels = new HashMap();
		outputModels = new HashMap();
		librariesImports = new ArrayList();
	
		byte[] buffer = text.getBytes();
		int length = buffer.length;
		BufferedReader brin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer, 0,
				length)));
	
		List uris = getTaggedInformations(brin, URI_TAG);
		for (Iterator iterator = uris.iterator(); iterator.hasNext();) {
			String line = (String)iterator.next();
			if (line.split("=").length == 2) { //$NON-NLS-1$
				String name = line.split("=")[0].trim(); //$NON-NLS-1$
				String uri = line.split("=")[1].trim(); //$NON-NLS-1$
				if (uri != null && uri.length() > 0) {
					uri = uri.trim();
	
					// EPackage registration
					EPackage regValue = EPackage.Registry.INSTANCE.getEPackage(uri);
					if (regValue != null) {
						ArrayList list = new ArrayList();
						list.add(regValue);
						metamodelsPackages.put(name, list);
						metamodelLocations.put(name, "uri:" + uri); //$NON-NLS-1$
					}
				}
			}
		}
	
		List paths = getTaggedInformations(brin, PATH_TAG);
		for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
			String line = (String)iterator.next();
			if (line.split("=").length == 2) { //$NON-NLS-1$
				String name = line.split("=")[0].trim(); //$NON-NLS-1$
				String path = line.split("=")[1].trim(); //$NON-NLS-1$
				if (path != null && path.length() > 0) {
					path = path.trim();
					Resource resource = load(URI.createPlatformResourceURI(path, true),
							RESOURCE_SET);
					if (resource != null) {
						ArrayList list = new ArrayList();
						for (Iterator it = resource.getContents().iterator(); it.hasNext();) {
							Object object = it.next();
							if (object instanceof EPackage) {
								list.add(object);
							}
						}
						metamodelsPackages.put(name, list);
						metamodelLocations.put(name, path);
					}
				}
			}
		}
	
		model = AtlParser.getDefault().parse(new ByteArrayInputStream(text.getBytes()));
	
		if (model == null) {
			inputModels = null;
			outputModels = null;
			return;
		}
	
		if (model.eClass().getName().equals("Module")) { //$NON-NLS-1$
			atlFileType = ATL_FILE_TYPE_MODULE;
			// input models computation
			EList inModelsList = (EList)eGet(model, "inModels"); //$NON-NLS-1$
			if (inModelsList != null) {
				for (Iterator iterator = inModelsList.iterator(); iterator.hasNext();) {
					EObject me = (EObject)iterator.next();
					EObject mm = (EObject)eGet(me, "metamodel"); //$NON-NLS-1$
					inputModels.put(eGet(me, "name").toString(), eGet(mm, "name").toString()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
	
			// output models computation
			EList outModelsList = (EList)eGet(model, "outModels"); //$NON-NLS-1$
			if (outModelsList != null) {
				for (Iterator iterator = outModelsList.iterator(); iterator.hasNext();) {
					EObject me = (EObject)iterator.next();
					EObject mm = (EObject)eGet(me, "metamodel"); //$NON-NLS-1$
					outputModels.put(eGet(me, "name").toString(), eGet(mm, "name").toString()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
	
		} else if (model.eClass().getName().equals("Query")) { //$NON-NLS-1$
			atlFileType = ATL_FILE_TYPE_QUERY;
			outputModels = null;
			for (Iterator iterator = model.eResource().getAllContents(); iterator.hasNext();) {
				EObject eo = (EObject)iterator.next();
				if (eo.eClass().getName().equals("OclModel")) { //$NON-NLS-1$
					String metamodelName = (String)eGet(eo, "name"); //$NON-NLS-1$
					inputModels.put("IN", metamodelName); //$NON-NLS-1$
					break;
				}
			}
		} else if (model.eClass().getName().equals("Library")) { //$NON-NLS-1$
			atlFileType = ATL_FILE_TYPE_LIBRARY;
		}
	
		// libraries computation
		EList librariesList = (EList)eGet(model, "libraries"); //$NON-NLS-1$
		if (librariesList != null) {
			for (Iterator iterator = librariesList.iterator(); iterator.hasNext();) {
				EObject lib = (EObject)iterator.next();
				librariesImports.add(eGet(lib, "name")); //$NON-NLS-1$
			}
		}
	
		initialized = true;
	}

	public Map getMetamodelLocations() {
		return metamodelLocations;
	}
	
	/**
	 * Status method.
	 * 
	 * @return <code>True</code> if the some metamodels have ever been detected , <code>False</code> if not.
	 */
	public boolean initialized() {
		return initialized;
	}

	/**
	 * Returns the list of tagged informations (header).
	 * 
	 * @param reader
	 *            the input
	 * @param tag
	 *            the tag to search
	 * @return the tagged information
	 * @throws IOException
	 */
	private static List getTaggedInformations(BufferedReader reader, String tag) throws IOException {
		reader.mark(1000);
		List res = new ArrayList();
		while (reader.ready()) {
			String line = reader.readLine();
			// code begins, uris checking stops.
			if (line == null || line.startsWith("library") //$NON-NLS-1$
					|| line.startsWith("module") || line.startsWith("query")) { //$NON-NLS-1$ //$NON-NLS-2$
				break;
			} else {
				if (line.trim().startsWith("-- @" + tag)) { //$NON-NLS-1$
					line = line.replaceFirst("^\\p{Space}*--\\p{Space}*@" //$NON-NLS-1$
							+ tag + "\\p{Space}+([^\\p{Space}]*)\\p{Space}*$", "$1"); //$NON-NLS-1$ //$NON-NLS-2$
					res.add(line);
				}
			}
		}
		reader.reset();
		return res;
	}

	/**
	 * Loads a model from an {@link org.eclipse.emf.common.util.URI URI} in a given {@link ResourceSet}.
	 * 
	 * @param modelURI
	 *            {@link org.eclipse.emf.common.util.URI URI} where the model is stored.
	 * @param resourceSet
	 *            The {@link ResourceSet} to load the model in.
	 * @return The packages of the model loaded from the URI.
	 * @throws IOException
	 *             If the given file does not exist.
	 */
	private static Resource load(URI modelURI, ResourceSet resourceSet) throws IOException {
		String fileExtension = modelURI.fileExtension();
		if (fileExtension == null || fileExtension.length() == 0) {
			fileExtension = Resource.Factory.Registry.DEFAULT_EXTENSION;
		}

		final Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		final Object resourceFactory = reg.getExtensionToFactoryMap().get(fileExtension);
		if (resourceFactory != null) {
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(fileExtension,
					resourceFactory);
		} else {
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(fileExtension,
					new XMIResourceFactoryImpl());
		}

		final Resource modelResource = resourceSet.createResource(modelURI);
		final Map options = new HashMap();
		options.put(XMLResource.OPTION_ENCODING, System.getProperty("file.encoding")); //$NON-NLS-1$
		modelResource.load(options);
		return modelResource;
	}

	/**
	 * Returns the value of a feature on an EObject.
	 * 
	 * @param self
	 *            the EObject
	 * @param featureName
	 *            the feature name
	 * @return the feature value
	 */
	private static Object eGet(EObject self, String featureName) {
		EStructuralFeature feature = self.eClass().getEStructuralFeature(featureName);
		if (feature != null) {
			return self.eGet(feature);
		}
		return null;
	}

}
