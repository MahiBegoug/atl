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
package org.eclipse.m2m.atl.tests.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.m2m.atl.tests.AtlTestPlugin;

/**
 * Provide utilities to get file paths, or compare files.
 * 
 * @author William Piers <a href="mailto:william.piers@obeo.fr">william.piers@obeo.fr</a>
 */
public class FileUtils {

	private static final String CLASS_FILE = "org/eclipse/m2m/atl/tests/util/FileUtils.class";

	public static String getTestCommonDirectory() {
		try
		{
			return new File(FileLocator.toFileURL(AtlTestPlugin.getDefault().getBundle().getEntry("/")).getFile()).toString();
		}
		catch (Throwable t)
		{
			// Ignore
		}

		URL url = ClassLoader.getSystemResource(CLASS_FILE);
		if (url != null)
		{
			URI uri = URI.createURI(url.toString());
			if (uri.isArchive())
			{
				// Returns the inner URI
				String authority = uri.authority();

				// Removing the !
				authority = authority.substring(0, authority.length()-1);

				uri = URI.createURI(authority);
			}

			if (uri.isFile())
			{
				File parentDir = new File(uri.toFileString()).getParentFile();
				while (parentDir != null && parentDir.isDirectory())
				{
					String name = parentDir.getName();
					if (name.equals(AtlTestPlugin.PLUGIN_ID) || name.startsWith(AtlTestPlugin.PLUGIN_ID + "_"))
					{
						return parentDir.getAbsolutePath(); 
					}
					parentDir = parentDir.getParentFile();
				}
			}
		}
		throw new RuntimeException("Unable to compute the " + AtlTestPlugin.PLUGIN_ID + " directory");
	}

	/**
	 * With the path of a file, the URI of the file is returned
	 * @param filePath
	 * @return the URL corresponding to the file
	 * @throws IllegalArgumentException
	 */
	public static URI fileNameToURI(String filePath) throws IOException, IllegalArgumentException {
		if (filePath.startsWith("uri:")) {
			filePath = filePath.substring(4);
			return URI.createURI(filePath, false);
		} else {
			filePath=getTestCommonDirectory()+filePath;
			return URI.createFileURI(filePath);
		}
	}

	/**
	 * With the path of a file, the URL of the file is returned
	 * @param filePath
	 * @return the URL corresponding to the file
	 * @throws MalformedURLException
	 */
	public static URL fileNameToURL(String filePath) throws IOException, MalformedURLException {
		filePath="file:"+getTestCommonDirectory()+filePath;
		if (filePath.endsWith(".atl")) {
			filePath = filePath.substring(0,filePath.length() - 4)+".asm";
		}
		return new URL(filePath);
	}
	
	/**
	 * With the path of a file, the input stream of the file is returned
	 * @param filePath
	 * @return the input stream corresponding to the file
	 * @throws FileNotFoundException
	 */
	public static InputStream fileNameToInputStream(String filePath) throws FileNotFoundException, CoreException {
		filePath="file:"+getTestCommonDirectory()+filePath;
		File f = new File(filePath.substring(4));
		return new FileInputStream(f);
	}

	/**
	 * Compare bytes of two files.
	 * 
	 * @param left
	 * @param right
	 * @throws IOException
	 */
	public static void compareFiles(File left, File right, boolean delete) throws Exception {
		if (left.length() != right.length())
			throw new Exception("test failed : there are differences");

		FileInputStream lin = new FileInputStream(left);
		FileInputStream rin = new FileInputStream(right);
		try
		{
			byte[] lbuffer = new byte[4096];
			byte[] rbuffer = new byte[lbuffer.length];
			for (int lcount = 0; (lcount = lin.read(lbuffer)) > 0;)
			{
				int bytesRead = 0;
				for (int rcount = 0; (rcount = rin.read(rbuffer, bytesRead, lcount - bytesRead)) > 0;)
				{
					bytesRead += rcount;
				}
				for (int byteIndex = 0; byteIndex < lcount; byteIndex++)
				{
					if (lbuffer[byteIndex] != rbuffer[byteIndex])
						throw new Exception("test failed : there are differences");
				}
			}
		}
		finally
		{
			lin.close();
			rin.close();
		}
		if (delete) {
			left.delete();
		}
	}

}
