/*******************************************************************************
 * Copyright (c) 2004 INRIA and C-S.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Frederic Jouault (INRIA) - initial API and implementation
 *    Freddy Allilaire (INRIA) - initial API and implementation
 *    Christophe Le Camus (C-S) - initial API and implementation
 *    Sebastien Gabel (C-S) - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2m.atl.drivers.uml24atl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.m2m.atl.engine.AtlEMFModelHandler;
import org.eclipse.m2m.atl.engine.vm.ModelLoader;
import org.eclipse.m2m.atl.engine.vm.nativelib.ASMModel;

/**
 * The UML2 model handler.
 * 
 * @author <a href="mailto:frederic.jouault@univ-nantes.fr">Frederic Jouault</a>
 * @author Freddy Allilaire (INRIA)
 * @author Christophe Le Camus (C-S)
 * @author Sebastien Gabel (C-S)
 */
public class AtlUML2ModelHandler extends AtlEMFModelHandler {

	private ASMUMLModel mofmm;
	
	private boolean useIDs = true;

	private boolean removeIDs;

	private String encoding = "ISO-8859-1";

	/**
	 * {@inheritDoc}
	 *
	 * @see org.eclipse.m2m.atl.engine.AtlEMFModelHandler#saveModel(org.eclipse.m2m.atl.engine.vm.nativelib.ASMModel, java.lang.String, org.eclipse.core.resources.IProject)
	 */
	public void saveModel(final ASMModel model, String fileName, IProject project) {
		String uri = project.getFullPath().toString() + "/" + fileName;
		saveModel(model, uri);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.eclipse.m2m.atl.engine.AtlEMFModelHandler#saveModel(org.eclipse.m2m.atl.engine.vm.nativelib.ASMModel, java.lang.String)
	 */
	public void saveModel(final ASMModel model, String uri) {
		saveModel(model, URI.createURI(uri), null);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.eclipse.m2m.atl.engine.AtlEMFModelHandler#saveModel(org.eclipse.m2m.atl.engine.vm.nativelib.ASMModel, java.io.OutputStream)
	 */
	public void saveModel(final ASMModel model, OutputStream out) {
		saveModel(model, null, out);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.eclipse.m2m.atl.engine.AtlEMFModelHandler#saveModel(org.eclipse.m2m.atl.engine.vm.nativelib.ASMModel, org.eclipse.emf.common.util.URI, java.io.OutputStream)
	 */
	protected void saveModel(final ASMModel model, URI uri, OutputStream out) {
		((ASMUMLModel)model).applyDelayedInvocations();
		XMIResource r = (XMIResource)((ASMUMLModel)model).getExtent();

		if (uri != null) {
			r.setURI(uri);
		}
		r.setXMIVersion("2.1");

		Map options = new HashMap();
		options.put(XMIResource.OPTION_ENCODING, encoding);
		options.put(XMIResource.OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.FALSE);
		options.put(XMIResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
		options.put(XMIResource.OPTION_SAVE_TYPE_INFORMATION, Boolean.TRUE);
		useIDs = true;
		if ((useIDs || removeIDs) && (r instanceof XMIResource)) {
			XMIResource xr = (XMIResource)r;
			int id = 1;
			Set alreadySet = new HashSet();
			for (Iterator i = r.getAllContents(); i.hasNext();) {
				EObject eo = (EObject)i.next();
				if (alreadySet.contains(eo)) {
					continue; // because sometimes a single element gets processed twice
				}
				xr.setID(eo, removeIDs ? null : ("a" + (id++)));
				alreadySet.add(eo);
			}
		}

		try {
			if (out != null) {
				r.save(out, options);
			} else {
				r.save(options);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.eclipse.m2m.atl.engine.AtlEMFModelHandler#getMof()
	 */
	public ASMModel getMof() {
		return mofmm;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.eclipse.m2m.atl.engine.AtlEMFModelHandler#loadModel(java.lang.String, org.eclipse.m2m.atl.engine.vm.nativelib.ASMModel, java.io.InputStream)
	 */
	public ASMModel loadModel(String name, ASMModel metamodel, InputStream in) {
		ASMModel ret = null;

		try {
			ret = ASMUMLModel.loadASMUMLModel(name, (ASMUMLModel)metamodel, in, null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.eclipse.m2m.atl.engine.AtlEMFModelHandler#newModel(java.lang.String, org.eclipse.m2m.atl.engine.vm.nativelib.ASMModel)
	 */
	public ASMModel newModel(String name, ASMModel metamodel) {
		ASMModel ret = null;

		try { // OUT
			ret = ASMUMLModel.newASMUMLModel(name, (ASMUMLModel)metamodel, null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	/**
	 * @see ASMUMLModel#newASMUMLModel(String, String, ASMUMLModel, ModelLoader)
	 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
	 */
	public ASMModel newModel(String name, String uri, ASMModel metamodel) {
		ASMModel ret = null;

		try {
			ret = ASMUMLModel.newASMUMLModel(name, uri, (ASMUMLModel)metamodel, null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	public AtlUML2ModelHandler() {
		mofmm = ASMUMLModel.createMOF(null);

	}

	public ASMModel getBuiltInMetaModel(String name) {
		ASMModel ret = null;

		return ret;
	}

	public boolean isHandling(ASMModel model) {
		return model instanceof ASMUMLModel;
	}

	public void disposeOfModel(ASMModel model) {
		((ASMUMLModel)model).dispose();
	}

	public ASMModel loadModel(String name, ASMModel metamodel, String uri) {
		ASMModel ret = null;
		// point entree
		try { // UML2
			ret = ASMUMLModel.loadASMUMLModel(name, (ASMUMLModel)metamodel, uri, null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	public ASMModel loadModel(String name, ASMModel metamodel, URI uri) {
		ASMModel ret = null;
		// PRO
		try {
			ret = ASMUMLModel.loadASMUMLModel(name, (ASMUMLModel)metamodel, uri, null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

}
