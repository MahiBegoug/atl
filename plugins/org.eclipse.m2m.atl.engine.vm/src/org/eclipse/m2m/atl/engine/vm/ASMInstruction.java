/*******************************************************************************
 * Copyright (c) 2004 INRIA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * 	   Frederic Jouault (INRIA) - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2m.atl.engine.vm;

/**
 * @author <a href="mailto:frederic.jouault@univ-nantes.fr">Frederic Jouault</a>
 */
public class ASMInstruction {

	public ASMInstruction(String mnemonic) {
		this.mnemonic = mnemonic.intern();
	}

	public String getMnemonic() {
		return mnemonic;
	}

	public String toString() {
		return mnemonic;
	}

	protected String mnemonic;
}

