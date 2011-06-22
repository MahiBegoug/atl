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
package org.eclipse.m2m.atl.emftvm.launcher;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * EMFTVM Launcher plug-in class.
 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
 */
public class EmftvmLauncherPlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.eclipse.m2m.atl.emftvm.launcher";

	private static EmftvmLauncherPlugin instance;

	/**
	 * @return the instance
	 */
	public static EmftvmLauncherPlugin getInstance() {
		return instance;
	}

	/**
	 * Logs <code>e</code> to the Eclipse log.
	 * @param e
	 */
	public static void log(final Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, 0, e.getLocalizedMessage(), e));
	}

	/**
	 * Logs <code>status</code> to the Eclipse log.
	 * @param status
	 */
	public static void log(final IStatus status) {
		getInstance().getLog().log(status);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		instance = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		instance = null;
		super.stop(context);
	}

}
