/**
 * Copyright (c) 2008 INRIA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     INRIA - initial API and implementation
 *
 * $Id: ReferenceLocation.java,v 1.1 2009/03/04 16:06:00 wpiers Exp $
 */
package org.eclipse.gmt.tcs.injector;

/**
 * @author <a href="mailto:frederic.jouault@univ-nantes.fr">Frederic Jouault</a>
 *
 */
public class ReferenceLocation {

	private Object sourceLocation;
	private Object target;

	/**
	 * @param location
	 * @param object
	 */
	public ReferenceLocation(Object sourceLocation, Object target) {
		this.sourceLocation = sourceLocation;
		this.target = target;
	}

	public Object getSourceLocation() {
		return sourceLocation;
	}

	public Object getTarget() {
		return target;
	}
}
