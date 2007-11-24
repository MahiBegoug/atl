/*******************************************************************************
 * Copyright (c) 2007 INRIA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Fr�d�ric Jouault - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2m.atl.engine.emfvm.lib;

public class OclParametrizedType extends OclType {

	private Object elementType;
	
	public String toString() {
		return super.toString() + "(" + elementType + ")";
	}

	public void setElementType(Object elementType) {
		this.elementType = elementType;
	}
}
