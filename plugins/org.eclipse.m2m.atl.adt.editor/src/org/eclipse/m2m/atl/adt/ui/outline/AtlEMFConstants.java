/*******************************************************************************
 * Copyright (c) 2004 INRIA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Tarik Idrissi (INRIA) - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2m.atl.adt.ui.outline;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

public abstract class AtlEMFConstants {

	public static EPackage pkAtl = null;

	public static EClass clUnit = null;

	public static EClass clModule = null;

	public static EClass clLibrary = null;

	public static EClass clQuery = null;

	public static EStructuralFeature sfModuleElements = null;

	public static EClass clRule = null;

	public static EClass clMatchedRule = null;

	public static EClass clLazyMatchedRule = null;

	public static EClass clCalledRule = null;

	public static EClass clHelper = null;

	public static EStructuralFeature sfHelperDefinition = null;

	public static EClass clLibraryRef = null;

	public static EClass clPatternElement = null;

	public static EClass clRuleVariableDeclaration = null;

	public static EClass clInPatternElement = null;

	public static EClass clOutPatternElement = null;

	public static EClass clSimpleInPatternElement = null;

	public static EClass clSimpleOutPatternElement = null;

	public static EClass clInPattern = null;

	public static EClass clOutPattern = null;

	public static EPackage pkExpressions = null;

	public static EClass clVariableDeclaration = null;

	public static EStructuralFeature sfVarName = null;

	public static EPackage pkOcl = null;

	public static EClass clOclFeatureDefinition = null;

	public static EClass clOclFeature = null;

	public static EClass clOclModel = null;

	public static EClass clParameter = null;

	public static EPackage pkCore = null;

	public static EClass clElement = null;

	public static EStructuralFeature sfLocation = null;

	private AtlEMFConstants() {
	}

}
