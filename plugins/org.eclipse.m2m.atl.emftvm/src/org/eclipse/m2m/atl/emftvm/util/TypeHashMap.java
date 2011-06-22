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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;

/**
 * {@link HashMap} implementation of {@link TypeMap}.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 *
 * @param <K>
 * @param <V>
 */
public class TypeHashMap<K, V> extends HashMap<K, V> implements TypeMap<K, V> {

	private static final long serialVersionUID = -4866974440115920626L;

	/* (non-Javadoc)
	 * @see org.eclipse.m2m.atl.emftvm.util.TypeMap#findKey(java.lang.Object)
	 */
	public Object findKey(final Object key) {
		if (key instanceof EClass) {
			final EClass eCls = (EClass) key;
			Object result = findEClassKey((EClass) key);
			if (result == null) {
				final Class<?> ic = eCls.getInstanceClass();
				if (ic != null) {
					result = findClassKey(ic);
				} else {
					result = findClassKey(Object.class); // everything is an object
				}
			}
			return result;
		}
		if (key instanceof Class<?>) {
			return findClassKey((Class<?>) key);
		}
		if (containsKey(key)) {
			return key;
		}
		return null;
	}

	private EClass findEClassKey(final EClass key) {
		final Set<EClass> keys = new HashSet<EClass>();
		findEClassKeys(key, keys);
		EClass mostSpecificKey = null;
		for (EClass superKey : keys) {
			if (mostSpecificKey == null || mostSpecificKey.isSuperTypeOf(superKey)) {
				mostSpecificKey = superKey;
			} else if (!superKey.isSuperTypeOf(mostSpecificKey)) { // sibling types
				throw new DuplicateEntryException(String.format("Superkeys %s and %s both have an entry in %s", mostSpecificKey, superKey, this));
			}
		}
		return mostSpecificKey;
	}

	private void findEClassKeys(final EClass key, final Set<EClass> result) {
		if (containsKey(key)) {
			result.add(key);
		} else {
			for (EClass eSuperType : key.getESuperTypes()) {
				findEClassKeys(eSuperType, result);
			}
		}
	}

	private Class<?> findClassKey(final Class<?> key) {
		if (containsKey(key)) {
			return key;
		}
		Class<?> mostSpecificKey = null;
		final Class<?> superType = key.getSuperclass();
		if (superType != null) {
			mostSpecificKey = findClassKey(superType);
		} else if (key != Object.class) {
			mostSpecificKey = findClassKey(Object.class); // everything is an object
		}
		for (Class<?> iface : key.getInterfaces()) {
			Class<?> ifaceKey = findClassKey(iface);
			if (ifaceKey != null && (mostSpecificKey == null || mostSpecificKey.isAssignableFrom(ifaceKey))) {
				mostSpecificKey = ifaceKey;
			}
		}
		return mostSpecificKey;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.m2m.atl.emftvm.util.TypeMap#findAllKeys(java.lang.Object, java.util.Set)
	 */
	public void findAllKeys(final Object key, final Set<Object> keys) {
		if (key instanceof EClass) {
			final EClass eCls = (EClass) key;
			final Class<?> ic = eCls.getInstanceClass();
			findAllEClassKeys(eCls, keys);
			if (ic != null) {
				findAllClassKeys(ic, keys);
			} else {
				findAllClassKeys(Object.class, keys); // everything is an object
			}
		} else if (key instanceof Class<?>) {
			findAllClassKeys((Class<?>) key, keys);
		} else if (containsKey(key)) {
			keys.add(key);
		}
	}

	private void findAllEClassKeys(final EClass key, final Set<Object> result) {
		if (containsKey(key)) {
			result.add(key);
		}
		for (EClass eSuperType : key.getESuperTypes()) {
			findAllEClassKeys(eSuperType, result);
		}
	}

	private void findAllClassKeys(final Class<?> key, final Set<Object> result) {
		if (containsKey(key)) {
			result.add(key);
		}
		final Class<?> superType = key.getSuperclass();
		if (superType != null) {
			findAllClassKeys(superType, result);
		} else if (key != Object.class) {
			findAllClassKeys(Object.class, result); // everything is an object
		}
		for (Class<?> iface : key.getInterfaces()) {
			findAllClassKeys(iface, result);
		}
	}

}
