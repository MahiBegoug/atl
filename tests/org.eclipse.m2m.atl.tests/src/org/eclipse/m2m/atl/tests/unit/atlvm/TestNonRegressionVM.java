/*******************************************************************************
 * Copyright (c) 2007, 2008 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - ATL tester
 *******************************************************************************/
package org.eclipse.m2m.atl.tests.unit.atlvm;

import org.eclipse.m2m.atl.tests.unit.TestNonRegressionTransfo;

/**
 * Specifies TestNonRegressionTransfo for the vm.
 * 
 * @author William Piers <a href="mailto:william.piers@obeo.fr">william.piers@obeo.fr</a>
 */
public class TestNonRegressionVM extends TestNonRegressionTransfo {

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		setVmName("Regular VM (with debugger)");//$NON-NLS-1$
		setPropertiesPath("/data/vm.properties");//$NON-NLS-1$
	}
	
}

