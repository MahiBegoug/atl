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
package org.eclipse.m2m.atl.emftvm.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.m2m.atl.emftvm.Field;


/**
 * Reusable class for storage and lookup of {@link Field}s.
 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
 *
 */
public final class FieldContainer {

	/**
	 * Lookup table for fields: (name -> (context -> field)).
	 */
	protected final Map<String, TypeMap<Object, Field>> fields = 
		new HashMap<String, TypeMap<Object,Field>>();

	/**
	 * Lookup table for static fields: (name -> (context -> field)).
	 */
	protected final Map<String, TypeMap<Object, Field>> staticFields = 
		new HashMap<String, TypeMap<Object,Field>>();

	/**
	 * Registers f in the corresponding lookup table.
	 * @param f the field to register
	 */
	public void registerField(final Field f) {
		f.clear();
		if (f.isStatic()) {
			registerFieldIn(f, staticFields);
		} else {
			registerFieldIn(f, fields);
		}
	}

	/**
	 * Registers f in the corresponding lookup table.
	 * @param f the field to register
	 */
	private static void registerFieldIn(final Field f, final Map<String, TypeMap<Object, Field>> reg) {
		final String name = f.getName();
		TypeMap<Object, Field> fieldreg;
		fieldreg = reg.get(name);
		if (fieldreg == null) {
			fieldreg = new TypeHashMap<Object, Field>();
			reg.put(name, fieldreg);
		}
		final EClassifier ectx = f.getEContext();
		assert ectx != null;
		final Object ctx = EMFTVMUtil.getRegistryType(ectx);
		fieldreg.put(ctx, f);
	}

	/**
	 * Finds the corresponding field for <pre>name</pre> and <pre>context</pre>.
	 * @param context the context type of the field
	 * @param name the name of the field
	 * @return the corresponding field, or <code>null</code> if not found
	 * @throws DuplicateEntryException if there is a field conflict
	 */
	public Field findField(final Object context, final String name) throws DuplicateEntryException {
		TypeMap<Object, Field> fieldreg = fields.get(name);
		if (fieldreg != null) {
			// First try to find with direct type
			final Field f = fieldreg.get(name);
			// Fall back to full resolving algorithm
			if (f == null) {
				Object ctxKey = fieldreg.findKey(context);
				if (ctxKey != null) {
					return fieldreg.get(ctxKey);
				}
			}
			return f;
		} else {
			return null;
		}
	}

	/**
	 * Finds the corresponding static field for <pre>name</pre> and <pre>context</pre>.
	 * @param context the context type of the field
	 * @param name the name of the field
	 * @return the corresponding field, or <code>null</code> if not found
	 * @throws DuplicateEntryException if there is a field conflict
	 */
	public Field findStaticField(Object context, String name) throws DuplicateEntryException {
		final TypeMap<Object, Field> fieldreg = staticFields.get(name);
		if (fieldreg != null) {
			// Static fields must be defined in exact context type
			return fieldreg.get(context);
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "FieldContainer [fields=" + fields + ", staticFields="
				+ staticFields + "]";
	}

}
