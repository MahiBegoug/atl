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
package org.eclipse.m2m.atl.tests.unit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.m2m.atl.ATLLogger;
import org.eclipse.m2m.atl.engine.parser.AtlParser;
import org.eclipse.m2m.atl.tests.AtlTestsMessages;
import org.eclipse.m2m.atl.tests.util.FileUtils;
import org.eclipse.m2m.atl.tests.util.ModelUtils;

/**
 * Launches parsing on each atl file, compare results.
 * 
 * @author <a href="mailto:william.piers@obeo.fr">William Piers</a>
 */
public class TestNonRegressionParser extends TestNonRegression {

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.m2m.atl.tests.unit.TestNonRegression#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.m2m.atl.tests.unit.TestNonRegression#singleTest(java.io.File)
	 */
	protected void singleTest(File directory) {
		System.out.print(AtlTestsMessages.getString(
				"TestNonRegressionParser.SINGLETEST", new Object[] {directory.getName()})); //$NON-NLS-1$ 
		final File expectedDir = new File(directory.getPath().replaceFirst("inputs", "expected")); //$NON-NLS-1$//$NON-NLS-2$
		final String transfoPath = directory + File.separator + directory.getName() + ".atl"; //$NON-NLS-1$
		final String outputPath = directory + File.separator + directory.getName() + ".atl.xmi"; //$NON-NLS-1$
		final String expectedPath = expectedDir + File.separator + directory.getName() + ".atl.xmi"; //$NON-NLS-1$
		if (!new File(transfoPath).exists()) {
			fail(AtlTestsMessages.getString("TestNonRegressionParser.FILENOTFOUND")); //$NON-NLS-1$
		}
		try {
			EObject result = AtlParser.getDefault().parse(new FileInputStream(transfoPath));
			ModelUtils.save(result, "file:/" + transfoPath + ".xmi"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IOException e) {
			ATLLogger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			fail(AtlTestsMessages.getString("TestNonRegressionParser.FAIL")); //$NON-NLS-1$
		}

		try {
			FileUtils.compareFiles(new File(outputPath), new File(expectedPath), true);
		} catch (IOException e) {
			ATLLogger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			fail(AtlTestsMessages.getString(
					"TestNonRegressionParser.COMPARISONFAIL", new Object[] {directory.getName()})); //$NON-NLS-1$
		}

		System.out.println(AtlTestsMessages.getString("TestNonRegressionParser.OK")); //$NON-NLS-1$
	}

}
