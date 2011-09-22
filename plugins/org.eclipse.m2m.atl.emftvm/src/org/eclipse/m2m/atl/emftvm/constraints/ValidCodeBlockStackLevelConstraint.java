/*******************************************************************************
 * Copyright (c) 2011 Dennis Wagelaar, Vrije Universiteit Brussel.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dennis Wagelaar, Vrije Universiteit Brussel
 *******************************************************************************/
package org.eclipse.m2m.atl.emftvm.constraints;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.validation.AbstractModelConstraint;
import org.eclipse.emf.validation.IValidationContext;
import org.eclipse.m2m.atl.emftvm.CodeBlock;

/**
 * Validates the final stack level of code blocks, which should be 0 or 1.
 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
 */
public class ValidCodeBlockStackLevelConstraint extends AbstractModelConstraint {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IStatus validate(IValidationContext ctx) {
		final EObject tgt = ctx.getTarget();
		if (tgt instanceof CodeBlock) {
			final CodeBlock cb = (CodeBlock)tgt;
			if (cb.getStackLevel() < 0 || cb.getStackLevel() > 1) {
				return ctx.createFailureStatus(cb, cb.getStackLevel());
			}
		}
		return ctx.createSuccessStatus();
	}

}
