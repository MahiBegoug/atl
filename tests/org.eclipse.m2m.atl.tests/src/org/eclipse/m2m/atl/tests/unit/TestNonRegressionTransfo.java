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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.m2m.atl.engine.AtlCompiler;
import org.eclipse.m2m.atl.engine.CompilerNotFoundException;
import org.eclipse.m2m.atl.tests.AtlTestPlugin;
import org.eclipse.m2m.atl.tests.AtlTestsMessages;
import org.eclipse.m2m.atl.tests.util.FileUtils;
import org.eclipse.m2m.atl.tests.util.ModelUtils;
import org.eclipse.m2m.atl.tests.util.TransfoLauncher;

/**
 * Test if the results models are still the same as expected.
 * 
 * @author William Piers <a href="mailto:william.piers@obeo.fr">william.piers@obeo.fr</a>
 */
public abstract class TestNonRegressionTransfo extends TestNonRegression {

	private final static boolean RECOMPILE_BEFORE_LAUNCH = true;
	private final static boolean APPLY_COMPILATION = false;

	private double totalTime = 0;
	private String vmName = null;
	private TransfoLauncher launcher = new TransfoLauncher();

	/* (non-Javadoc)
	 * @see org.eclipse.m2m.atl.tests.unit.TestNonRegression#singleTest(java.io.File)
	 */
	protected void singleTest(File directory) {
		if (vmName == null) {
			fail(AtlTestsMessages.getString("TestNonRegressionTransfo.MISSINGVMNAME")); //$NON-NLS-1$
		}
		System.out.print(AtlTestsMessages.getString("TestNonRegressionTransfo.SINGLETEST",new Object[]{directory.getName()})); //$NON-NLS-1$
		final String buildURI = directory+ File.separator + directory.getName() + ".launch";	 //$NON-NLS-1$

		if (!new File(buildURI).exists()) fail(AtlTestsMessages.getString("TestNonRegressionTransfo.3")); //$NON-NLS-1$
		if (launcher == null) fail(AtlTestsMessages.getString("TestNonRegressionTransfo.4")); //$NON-NLS-1$

		try {
			launcher.parseConfiguration(buildURI);
		} catch (Exception e) {
			e.printStackTrace();
			fail(AtlTestsMessages.getString("TestNonRegressionTransfo.5")); //$NON-NLS-1$
		} 

		if (RECOMPILE_BEFORE_LAUNCH) {
			/*
			 * COMPILER LAUNCH 
			 * 
			 */			
			URL atlUrl = launcher.getAtlURL();
			String atlFilePath = atlUrl.getFile();
			String outName = "";
			InputStream is = null;
			
			try {
				if (APPLY_COMPILATION) {
					outName = atlFilePath.substring(0, atlFilePath.lastIndexOf('.')) + ".asm";//$NON-NLS-1$
				} else {
					try {
						outName = atlFilePath.substring(0, atlFilePath.lastIndexOf('.')) + ".temp.asm";//$NON-NLS-1$
						launcher.setAsmUrl(new URL("file:"+outName));						
					} catch (MalformedURLException e) {
						e.printStackTrace();
						fail("URL problem : "+atlUrl); //$NON-NLS-1$
					}
				}
				
				is = atlUrl.openStream();
			} catch(IOException e) {
				e.printStackTrace();
				fail("File not found : "+atlUrl); //$NON-NLS-1$
			}
			try {
				AtlCompiler.getDefault().compile(is, outName);
			} catch(CompilerNotFoundException cnfee) {
				cnfee.printStackTrace();
				fail("Compiler not found"); //$NON-NLS-1$
			}
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
				fail(atlUrl+" compilation failed"); //$NON-NLS-1$
			}
		}

		/*
		 * TRANSFORMATION LAUNCH 
		 * 
		 */
		double executionTime = 0;
		try {
			executionTime = launcher.run(vmName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(AtlTestsMessages.getString("TestNonRegressionTransfo.6", new Object[]{e})); //$NON-NLS-1$
		}
		System.out.println(executionTime+"s."); //$NON-NLS-1$
		AtlTestPlugin.getResourceSet().getResources().clear();

		/*
		 * RESULTS COMPARISON 
		 * 
		 */

		Map output = launcher.getOutput();
		//metamodels registration for emf comparison
		for (Iterator iter = output.values().iterator(); iter.hasNext();) {
			String metaid = (String) iter.next();
			String metapath = (String) launcher.getPath().get(metaid);
			try {
				ModelUtils.registerMetamodel(FileUtils.fileNameToURI(metapath), AtlTestPlugin.getResourceSet());				
			} catch (IOException e) {
				e.printStackTrace();
				fail(AtlTestsMessages.getString("TestNonRegressionTransfo.7")); //$NON-NLS-1$
			}		
		}

		for (Iterator iter = output.keySet().iterator(); iter.hasNext();) {
			String outputid = (String) iter.next();
			String outputPath = (String) launcher.getPath().get(outputid);
			String expectedPath = FileUtils.getTestCommonDirectory()+outputPath.replaceFirst("inputs","expected"); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				outputPath = FileUtils.getTestCommonDirectory()+outputPath;
				ModelUtils.compareModels(new File(outputPath), new File(expectedPath), true, true);
			} catch (Exception e) {
				e.printStackTrace();
				fail(AtlTestsMessages.getString("TestNonRegressionTransfo.8")); //$NON-NLS-1$
			}
		}
		totalTime+=executionTime;
		AtlTestPlugin.getResourceSet().getResources().clear();
	}

	protected void setVmName(String vmName) {
		this.vmName = vmName;
	}

	public String getVmName() {
		return vmName;
	}

	protected void tearDown() throws Exception {
		System.out.println("total time : "+ totalTime +"s.");
		super.tearDown();
	}
}

