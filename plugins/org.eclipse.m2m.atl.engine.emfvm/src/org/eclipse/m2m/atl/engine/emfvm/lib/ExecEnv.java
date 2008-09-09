/*******************************************************************************
 * Copyright (c) 2007, 2008 INRIA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    INRIA - initial API and implementation
 *    Obeo - bag implementation
 *    
 *
 * $Id: ExecEnv.java,v 1.16 2008/09/09 13:15:01 wpiers Exp $
 *******************************************************************************/
package org.eclipse.m2m.atl.engine.emfvm.lib;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2m.atl.engine.emfvm.EmfvmPlugin;

/**
 * Execution environment.
 * 
 * @author <a href="mailto:frederic.jouault@univ-nantes.fr">Frederic Jouault</a>
 * @author <a href="mailto:william.piers@obeo.fr">William Piers</a>
 * @author <a href="mailto:mikael.barbero@univ-nantes.fr">Mikael Barbero</a>
 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
 */
public class ExecEnv {

	/** The common ATL logger. */
	protected static Logger logger = Logger.getLogger(EmfvmPlugin.LOGGER);

	/** Supertypes correspondance map. */
	private Map supertypes = new HashMap();
	{
		// Integer extends Real
		supertypes.put(Integer.class, Arrays.asList(new Class[] {Double.class}));
		// Boolean extends OclAny
		supertypes.put(Boolean.class, Arrays.asList(new Class[] {Object.class}));
		// String extends OclAny
		supertypes.put(String.class, Arrays.asList(new Class[] {Object.class}));
		// Bag extends Collection
		supertypes.put(Bag.class, Arrays.asList(new Class[] {Collection.class}));
		// Sequence extends Collection
		supertypes.put(ArrayList.class, Arrays.asList(new Class[] {Collection.class}));
		// OrderedSet extends Collection
		supertypes.put(LinkedHashSet.class, Arrays.asList(new Class[] {Collection.class}));
		// Set extends Collection
		supertypes.put(HashSet.class, Arrays.asList(new Class[] {Collection.class}));
		// Collection extends OclAny
		supertypes.put(Collection.class, Arrays.asList(new Class[] {Object.class}));
		// Real extends OclAny
		supertypes.put(Double.class, Arrays.asList(new Class[] {Object.class}));
		// OclParametrizedType extends OclType
		supertypes.put(OclParametrizedType.class, Arrays.asList(new Class[] {OclType.class}));
		// OclSimpleType extends OclType
		supertypes.put(OclSimpleType.class, Arrays.asList(new Class[] {OclType.class}));
		// OclUndefined extends OclAny
		supertypes.put(OclUndefined.class, Arrays.asList(new Class[] {Object.class}));
		// TransientLink extends OclAny
		supertypes.put(TransientLink.class, Arrays.asList(new Class[] {Object.class}));
		// Map extends OclAny
		supertypes.put(HashMap.class, Arrays.asList(new Class[] {Object.class}));
		// ATLModule extends OclAny
		supertypes.put(ASMModule.class, Arrays.asList(new Class[] {Object.class}));
		// Tuple extends OclAny
		supertypes.put(Tuple.class, Arrays.asList(new Class[] {Object.class}));
		// EnumLiteral extends OclAny
		supertypes.put(EnumLiteral.class, Arrays.asList(new Class[] {Object.class}));
	}

	// TODO:
	// - make static to avoid recomputing the map of nativelib operations for each launch
	// However, it is much more convenient when not static for development:
	// it is possible to add operations without needing to restart Eclipse
	// - modularize the nativelib (e.g., by splitting into diffent classes, or methods)
	private Map vmTypeOperations = new HashMap();
	{
		Map operationsByName;

		// Real
		operationsByName = new HashMap();
		vmTypeOperations.put(Double.class, operationsByName);
		operationsByName.put("/", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(((Number)localVars[0]).doubleValue() / ((Number)localVars[1]).doubleValue());
			}
		});
		operationsByName.put("*", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(((Number)localVars[0]).doubleValue() * ((Number)localVars[1]).doubleValue());
			}
		});
		operationsByName.put("-", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(((Number)localVars[0]).doubleValue() - ((Number)localVars[1]).doubleValue());
			}
		});
		operationsByName.put("+", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(((Number)localVars[0]).doubleValue() + ((Number)localVars[1]).doubleValue());
			}
		});
		operationsByName.put("<", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Number)localVars[0]).doubleValue() < ((Number)localVars[1])
						.doubleValue());
			}
		});
		operationsByName.put("<=", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Number)localVars[0]).doubleValue() <= ((Number)localVars[1])
						.doubleValue());
			}
		});
		operationsByName.put(">", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Number)localVars[0]).doubleValue() > ((Number)localVars[1])
						.doubleValue());
			}
		});
		operationsByName.put(">=", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Number)localVars[0]).doubleValue() >= ((Number)localVars[1])
						.doubleValue());
			}
		});
		operationsByName.put("=", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Number)localVars[0]).doubleValue() == ((Number)localVars[1])
						.doubleValue());
			}
		});
		operationsByName.put("toString", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return localVars[0].toString();
			}
		});
		operationsByName.put("abs", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.abs(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("round", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Integer((int)Math.round(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("floor", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Integer((int)Math.floor(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("max", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.max(((Number)localVars[0]).doubleValue(), ((Number)localVars[1])
						.doubleValue()));
			}
		});
		operationsByName.put("min", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.min(((Number)localVars[0]).doubleValue(), ((Number)localVars[1])
						.doubleValue()));
			}
		});
		operationsByName.put("acos", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.acos(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("asin", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.asin(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("atan", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.atan(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("cos", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.cos(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("sin", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.sin(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("tan", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.tan(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("toDegrees", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.toDegrees(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("toRadians", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.toRadians(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("exp", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.exp(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("log", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.log(((Number)localVars[0]).doubleValue()));
			}
		});
		operationsByName.put("sqrt", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(Math.sqrt(((Number)localVars[0]).doubleValue()));
			}
		});

		// Integer
		operationsByName = new HashMap();
		vmTypeOperations.put(Integer.class, operationsByName);
		operationsByName.put("*", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				if (localVars[1] instanceof Integer) {
					return new Integer(((Integer)localVars[0]).intValue()
							* ((Integer)localVars[1]).intValue());
				} else {
					return new Double(((Number)localVars[0]).doubleValue()
							* ((Number)localVars[1]).doubleValue());
				}
			}
		});
		operationsByName.put("-", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				if (localVars[1] instanceof Integer) {
					return new Integer(((Integer)localVars[0]).intValue()
							- ((Integer)localVars[1]).intValue());
				} else {
					return new Double(((Number)localVars[0]).doubleValue()
							- ((Number)localVars[1]).doubleValue());
				}
			}
		});
		operationsByName.put("+", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				if (localVars[1] instanceof Integer) {
					return new Integer(((Integer)localVars[0]).intValue()
							+ ((Integer)localVars[1]).intValue());
				} else {
					return new Double(((Number)localVars[0]).doubleValue()
							+ ((Number)localVars[1]).doubleValue());
				}
			}
		});
		operationsByName.put("div", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Integer(((Integer)localVars[0]).intValue() / ((Integer)localVars[1]).intValue());
			}
		});
		operationsByName.put("mod", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Integer(((Integer)localVars[0]).intValue() % ((Integer)localVars[1]).intValue());
			}
		});
		operationsByName.put("/", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Double(((Integer)localVars[0]).intValue() / ((Number)localVars[1]).doubleValue());
			}
		});
		operationsByName.put("<", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				if (localVars[1] instanceof Integer) {
					return new Boolean(((Integer)localVars[0]).intValue() < ((Integer)localVars[1])
							.intValue());
				} else {
					return new Boolean(((Number)localVars[0]).doubleValue() < ((Number)localVars[1])
							.doubleValue());
				}
			}
		});
		operationsByName.put("<=", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				if (localVars[1] instanceof Integer) {
					return new Boolean(((Integer)localVars[0]).intValue() <= ((Integer)localVars[1])
							.intValue());
				} else {
					return new Boolean(((Number)localVars[0]).doubleValue() <= ((Number)localVars[1])
							.doubleValue());
				}
			}
		});
		operationsByName.put(">", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				if (localVars[1] instanceof Integer) {
					return new Boolean(((Integer)localVars[0]).intValue() > ((Integer)localVars[1])
							.intValue());
				} else {
					return new Boolean(((Number)localVars[0]).doubleValue() > ((Number)localVars[1])
							.doubleValue());
				}
			}
		});
		operationsByName.put(">=", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				if (localVars[1] instanceof Integer) {
					return new Boolean(((Integer)localVars[0]).intValue() >= ((Integer)localVars[1])
							.intValue());
				} else {
					return new Boolean(((Number)localVars[0]).doubleValue() >= ((Number)localVars[1])
							.doubleValue());
				}
			}
		});
		operationsByName.put("=", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				if (localVars[1] instanceof Integer) {
					return new Boolean(((Integer)localVars[0]).intValue() == ((Integer)localVars[1])
							.intValue());
				} else {
					return new Boolean(((Number)localVars[0]).doubleValue() == ((Number)localVars[1])
							.doubleValue());
				}
			}
		});
		operationsByName.put("toString", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return localVars[0].toString();
			}
		});
		operationsByName.put("toHexString", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return Integer.toHexString(((Integer)localVars[0]).intValue());
			}
		});
		operationsByName.put("toBinaryString", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return Integer.toBinaryString(((Integer)localVars[0]).intValue());
			}
		});

		// Boolean
		operationsByName = new HashMap();
		vmTypeOperations.put(Boolean.class, operationsByName);
		operationsByName.put("not", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(!((Boolean)localVars[0]).booleanValue());
			}
		});
		operationsByName.put("and", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Boolean)localVars[0]).booleanValue()
						&& ((Boolean)localVars[1]).booleanValue());
			}
		});
		operationsByName.put("or", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Boolean)localVars[0]).booleanValue()
						|| ((Boolean)localVars[1]).booleanValue());
			}
		});
		operationsByName.put("xor", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Boolean)localVars[0]).booleanValue()
						^ ((Boolean)localVars[1]).booleanValue());
			}
		});
		operationsByName.put("implies", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Boolean)localVars[0]).booleanValue() ? ((Boolean)localVars[1])
						.booleanValue() : true);
			}
		});

		// Sequence
		operationsByName = new HashMap();
		vmTypeOperations.put(ArrayList.class, operationsByName);
		operationsByName.put("insertAt", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;

				int index = ((Integer)localVars[1]).intValue();
				List ret = new ArrayList((Collection)localVars[0]);
				ret.add(index - 1, localVars[2]);

				return ret;
			}
		});
		operationsByName.put("at", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;

				int index = ((Integer)localVars[1]).intValue();

				return ((List)localVars[0]).get(index - 1);
			}
		});
		operationsByName.put("subSequence", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;

				int start = ((Integer)localVars[1]).intValue();
				int end = ((Integer)localVars[2]).intValue();
				if (end >= start) {
					return new ArrayList(((List)localVars[0]).subList(start - 1, end));
				} else {
					return emptySequence;
				}
			}
		});
		operationsByName.put("indexOf", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Integer(((List)localVars[0]).indexOf(localVars[1]) + 1);
			}
		});
		operationsByName.put("prepend", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;

				List ret = new ArrayList((Collection)localVars[0]);
				ret.add(0, localVars[1]);

				return ret;
			}
		});
		operationsByName.put("including", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				List ret = new ArrayList((Collection)localVars[0]);
				ret.add(localVars[1]);
				return ret;
			}
		});
		operationsByName.put("excluding", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				List ret = new ArrayList((Collection)localVars[0]);
				ret.removeAll(Arrays.asList(new Object[] {localVars[1]}));
				return ret;
			}
		});
		operationsByName.put("append", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				List ret = new ArrayList((Collection)localVars[0]);
				ret.add(localVars[1]);
				return ret;
			}
		});
		operationsByName.put("union", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				List ret = new ArrayList((Collection)localVars[0]);
				ret.addAll((Collection)localVars[1]);
				return ret;
			}
		});
		operationsByName.put("asSequence", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return localVars[0];
			}
		});
		operationsByName.put("first", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				List l = (List)localVars[0];
				if (l.isEmpty()) {
					return OclUndefined.SINGLETON;
				} else {
					return l.get(0);
				}
			}
		});
		operationsByName.put("last", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				List l = (List)localVars[0];
				if (l.isEmpty()) {
					return OclUndefined.SINGLETON;
				} else {
					return l.get(l.size() - 1);
				}
			}
		});
		operationsByName.put("flatten", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				List base = null;
				List ret = new ArrayList((Collection)localVars[0]);
				boolean containsCollection;
				do {
					base = ret;
					ret = new ArrayList();
					containsCollection = false;
					for (Iterator iterator = base.iterator(); iterator.hasNext();) {
						Object object = iterator.next();
						if (object instanceof Collection) {
							Collection subCollection = (Collection)object;
							ret.addAll(subCollection);
							Iterator iterator2 = subCollection.iterator();
							while (containsCollection && iterator2.hasNext()) {
								Object subCollectionObject = iterator2.next();
								if (subCollectionObject instanceof Collection) {
									containsCollection = true;
								}
							}
						} else {
							ret.add(object);
						}
					}
				} while (containsCollection);
				return ret;
			}
		});

		// Bag
		operationsByName = new HashMap();
		vmTypeOperations.put(Bag.class, operationsByName);
		operationsByName.put("including", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Bag ret = new Bag((Collection)localVars[0]);
				ret.add(localVars[1]);
				return ret;
			}
		});
		operationsByName.put("excluding", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Bag ret = new Bag((Collection)localVars[0]);
				ret.removeAll(Arrays.asList(new Object[] {localVars[1]}));
				return ret;
			}
		});
		operationsByName.put("asBag", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return localVars[0];
			}
		});
		operationsByName.put("flatten", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Bag base = null;
				Bag ret = new Bag((Collection)localVars[0]);
				boolean containsCollection;
				do {
					base = ret;
					ret = new Bag();
					containsCollection = false;
					for (Iterator iterator = base.iterator(); iterator.hasNext();) {
						Object object = iterator.next();
						if (object instanceof Collection) {
							Collection subCollection = (Collection)object;
							ret.addAll(subCollection);
							Iterator iterator2 = subCollection.iterator();
							while (!containsCollection && iterator2.hasNext()) {
								Object subCollectionObject = iterator2.next();
								if (subCollectionObject instanceof Collection) {
									containsCollection = true;
								}
							}
						} else {
							ret.add(object);
						}
					}
				} while (containsCollection);
				return ret;
			}
		});

		// OrderedSet
		operationsByName = new HashMap();
		vmTypeOperations.put(LinkedHashSet.class, operationsByName);
		operationsByName.put("insertAt", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;

				int idx = ((Integer)localVars[1]).intValue() - 1;
				LinkedHashSet ret = new LinkedHashSet();
				LinkedHashSet s = (LinkedHashSet)localVars[0];

				int k = 0;
				for (Iterator i = s.iterator(); i.hasNext();) {
					if (k++ == idx) {
						ret.add(localVars[2]);
					}
					ret.add(i.next());
				}
				return ret;
			}
		});
		operationsByName.put("prepend", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;

				int idx = 0;
				LinkedHashSet ret = new LinkedHashSet();
				LinkedHashSet s = (LinkedHashSet)localVars[0];

				int k = 0;
				for (Iterator i = s.iterator(); i.hasNext();) {
					if (k++ == idx) {
						ret.add(localVars[1]);
					}
					ret.add(i.next());
				}
				return ret;
			}
		});
		operationsByName.put("including", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				LinkedHashSet ret = new LinkedHashSet((Collection)localVars[0]);
				ret.add(localVars[1]);
				return ret;
			}
		});
		operationsByName.put("excluding", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				LinkedHashSet ret = new LinkedHashSet((Collection)localVars[0]);
				ret.removeAll(Arrays.asList(new Object[] {localVars[1]}));
				return ret;
			}
		});
		operationsByName.put("append", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				LinkedHashSet ret = new LinkedHashSet((Collection)localVars[0]);
				ret.add(localVars[1]);
				return ret;
			}
		});
		operationsByName.put("asOrderedSet", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return localVars[0];
			}
		});
		operationsByName.put("first", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return ((LinkedHashSet)localVars[0]).iterator().next();
			}
		});
		// optimized version
		operationsByName.put("count", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Object o = localVars[1];
				return ((HashSet)localVars[0]).contains(o) ? new Integer(1) : new Integer(0);
			}
		});
		operationsByName.put("union", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Set ret = new LinkedHashSet((Collection)localVars[0]);
				ret.addAll((Collection)localVars[1]);
				return ret;
			}
		});
		operationsByName.put("flatten", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Set base = null;
				Set ret = new LinkedHashSet((Collection)localVars[0]);
				boolean containsCollection;
				do {
					base = ret;
					ret = new LinkedHashSet();
					containsCollection = false;
					for (Iterator iterator = base.iterator(); iterator.hasNext();) {
						Object object = iterator.next();
						if (object instanceof Collection) {
							Collection subCollection = (Collection)object;
							ret.addAll(subCollection);
							Iterator iterator2 = subCollection.iterator();
							while (containsCollection && iterator2.hasNext()) {
								Object subCollectionObject = iterator2.next();
								if (subCollectionObject instanceof Collection) {
									containsCollection = true;
								}
							}
						} else {
							ret.add(object);
						}
					}
				} while (containsCollection);
				return ret;
			}
		});

		// Set
		operationsByName = new HashMap();
		vmTypeOperations.put(HashSet.class, operationsByName);
		operationsByName.put("including", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Set ret = new HashSet((Collection)localVars[0]);
				ret.add(localVars[1]);
				return ret;
			}
		});
		operationsByName.put("excluding", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Set ret = new HashSet((Collection)localVars[0]);
				ret.removeAll(Arrays.asList(new Object[] {localVars[1]}));
				return ret;
			}
		});
		operationsByName.put("intersection", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Set ret = new HashSet((Collection)localVars[0]);
				ret.retainAll((Collection)localVars[1]);
				return ret;
			}
		});
		operationsByName.put("-", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Set ret = new HashSet((Collection)localVars[0]);
				ret.removeAll((Collection)localVars[1]);
				return ret;
			}
		});
		operationsByName.put("symetricDifference", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Set ret = new HashSet((Collection)localVars[0]);

				Set t = new HashSet((Collection)localVars[1]);
				t.removeAll(ret);

				ret.removeAll((Collection)localVars[1]);
				ret.addAll(t);

				return ret;
			}
		});
		operationsByName.put("asSet", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return localVars[0];
			}
		});
		operationsByName.put("flatten", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Set base = null;
				Set ret = new HashSet((Collection)localVars[0]);
				boolean containsCollection;
				do {
					base = ret;
					ret = new HashSet();
					containsCollection = false;
					for (Iterator iterator = base.iterator(); iterator.hasNext();) {
						Object object = iterator.next();
						if (object instanceof Collection) {
							Collection subCollection = (Collection)object;
							ret.addAll(subCollection);
							Iterator iterator2 = subCollection.iterator();
							while (containsCollection && iterator2.hasNext()) {
								Object subCollectionObject = iterator2.next();
								if (subCollectionObject instanceof Collection) {
									containsCollection = true;
								}
							}
						} else {
							ret.add(object);
						}
					}
				} while (containsCollection);
				return ret;
			}
		});
		// optimized version
		operationsByName.put("count", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Object o = localVars[1];
				return ((HashSet)localVars[0]).contains(o) ? new Integer(1) : new Integer(0);
			}
		});
		operationsByName.put("union", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Set ret = new HashSet((Collection)localVars[0]);
				ret.addAll((Collection)localVars[1]);
				return ret;
			}
		});

		// Collection
		operationsByName = new HashMap();
		vmTypeOperations.put(Collection.class, operationsByName);
		operationsByName.put("size", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Integer(((Collection)localVars[0]).size());
			}
		});
		operationsByName.put("sum", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Collection c = (Collection)localVars[0];
				if (c.isEmpty()) {
					return OclUndefined.SINGLETON;
				} else {
					Iterator i = c.iterator();
					Object ret = i.next();
					Operation operation = getOperation(modelAdapter.getType(ret), "+");
					while (i.hasNext()) {
						StackFrame callee = frame.newFrame(operation);
						callee.localVars[0] = ret;
						callee.localVars[1] = i.next();
						ret = operation.exec(callee);
					}
					return ret;
				}
			}
		});
		operationsByName.put("includes", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Collection)localVars[0]).contains(localVars[1]));
			}
		});
		operationsByName.put("excludes", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(!((Collection)localVars[0]).contains(localVars[1]));
			}
		});
		operationsByName.put("count", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				int ret = 0;
				Object o = localVars[1];
				for (Iterator i = ((Collection)localVars[0]).iterator(); i.hasNext();) {
					if (i.next().equals(o)) {
						ret++;
					}
				}

				return new Integer(ret);
			}
		});
		operationsByName.put("includesAll", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(!((Collection)localVars[0]).containsAll((Collection)localVars[1]));
			}
		});
		operationsByName.put("excludesAll", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				boolean ret = true;

				Collection s = (Collection)localVars[0];
				for (Iterator i = ((Collection)localVars[1]).iterator(); i.hasNext();) {
					ret = ret && !s.contains(i.next());
				}

				return new Boolean(ret);
			}
		});
		operationsByName.put("isEmpty", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Collection)localVars[0]).isEmpty());
			}
		});
		operationsByName.put("notEmpty", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(!((Collection)localVars[0]).isEmpty());
			}
		});
		operationsByName.put("asSequence", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new ArrayList((Collection)localVars[0]);
			}
		});
		operationsByName.put("asSet", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Set ret = new HashSet((Collection)localVars[0]);
				return ret;
			}
		});
		operationsByName.put("asBag", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Bag((Collection)localVars[0]);
			}
		});
		operationsByName.put("asOrderedSet", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new LinkedHashSet((Collection)localVars[0]);
			}
		});

		// String
		operationsByName = new HashMap();
		vmTypeOperations.put(String.class, operationsByName);
		operationsByName.put("size", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Integer(((String)localVars[0]).length());
			}
		});
		operationsByName.put("regexReplaceAll", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return ((String)localVars[0]).replaceAll((String)localVars[1], (String)localVars[2]);
			}
		});
		operationsByName.put("replaceAll", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return ((String)localVars[0]).replace(((String)localVars[1]).charAt(0),
						((String)localVars[2]).charAt(0));
			}
		});
		operationsByName.put("split", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return Arrays.asList(((String)localVars[0]).split((String)localVars[1]));
			}
		});
		operationsByName.put("toInteger", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return Integer.valueOf((String)localVars[0]);
			}
		});
		operationsByName.put("toReal", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return Double.valueOf((String)localVars[0]);
			}
		});
		operationsByName.put("toUpper", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return ((String)localVars[0]).toUpperCase();
			}
		});
		operationsByName.put("toLower", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return ((String)localVars[0]).toLowerCase();
			}
		});
		operationsByName.put("toSequence", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				String tmp = (String)localVars[0];
				ArrayList ret = new ArrayList();
				for (int i = 0; i < tmp.length(); i++) {
					ret.add("" + tmp.charAt(i));
				}
				return ret;
			}
		});
		operationsByName.put("startsWith", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((String)localVars[0]).startsWith((String)localVars[1]));
			}
		});
		operationsByName.put("substring", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return ((String)localVars[0]).substring(((Number)localVars[1]).intValue() - 1,
						((Number)localVars[2]).intValue());
			}
		});
		operationsByName.put("indexOf", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Integer(((String)localVars[0]).indexOf((String)localVars[1]));
			}
		});
		operationsByName.put("lastIndexOf", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Integer(((String)localVars[0]).lastIndexOf((String)localVars[1]));
			}
		});
		operationsByName.put("endsWith", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((String)localVars[0]).endsWith((String)localVars[1]));
			}
		});
		operationsByName.put("+", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return (String)localVars[0] + localVars[1];
			}
		});
		operationsByName.put("concat", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return (String)localVars[0] + localVars[1];
			}
		});
		operationsByName.put("<", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((String)localVars[0]).compareTo((String)localVars[1]) < 0);
			}
		});
		operationsByName.put("<=", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((String)localVars[0]).compareTo((String)localVars[1]) <= 0);
			}
		});
		operationsByName.put(">", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((String)localVars[0]).compareTo((String)localVars[1]) > 0);
			}
		});
		operationsByName.put(">=", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((String)localVars[0]).compareTo((String)localVars[1]) >= 0);
			}
		});
		operationsByName.put("=", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((String)localVars[0]).equals(localVars[1]));
			}
		});
		operationsByName.put("writeTo", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(
						writeToWithCharset(frame, (String)localVars[0], (String)localVars[1], null));
			}
		});
		operationsByName.put("writeToWithCharset", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(writeToWithCharset(frame, (String)localVars[0], (String)localVars[1],
						(String)localVars[2]));
			}
		});

		// Tuple
		operationsByName = new HashMap();
		vmTypeOperations.put(Tuple.class, operationsByName);
		operationsByName.put("=", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Tuple)localVars[0]).equals(localVars[1]));
			}
		});

		// OclAny
		operationsByName = new HashMap();
		vmTypeOperations.put(Object.class, operationsByName);
		operationsByName.put("toString", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return toPrettyPrintedString(localVars[0]);
			}
		});
		operationsByName.put("=", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(localVars[0].equals(localVars[1]));
			}
		});
		operationsByName.put("<>", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(!localVars[0].equals(localVars[1]));
			}
		});
		operationsByName.put("oclIsUndefined", new Operation(1) {
			public Object exec(StackFrame frame) {
				return Boolean.FALSE;
			}
		});
		operationsByName.put("debug", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				logger.info(localVars[1] + ": " + toPrettyPrintedString(localVars[0]));
				return localVars[0];
			}
		});

		// OclUndefined
		operationsByName = new HashMap();
		vmTypeOperations.put(OclUndefined.class, operationsByName);
		operationsByName.put("oclIsUndefined", new Operation(1) {
			public Object exec(StackFrame frame) {
				return Boolean.TRUE;
			}
		});

		// OclType
		operationsByName = new HashMap();
		vmTypeOperations.put(OclType.class, operationsByName);
		operationsByName.put("setName", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				((OclType)localVars[0]).setName((String)localVars[1]);
				return null;
			}
		});
		operationsByName.put("conformsTo", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((OclType)localVars[0]).conformsTo((OclType)localVars[1]));
			}
		});

		// OclParametrizedType
		operationsByName = new HashMap();
		vmTypeOperations.put(OclParametrizedType.class, operationsByName);
		operationsByName.put("setElementType", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				((OclParametrizedType)localVars[0]).setElementType(localVars[1]);
				return null;
			}
		});

		// Class
		operationsByName = new HashMap();
		vmTypeOperations.put(Class.class, operationsByName);
		operationsByName.put("registerHelperAttribute", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				String name = (String)localVars[1];
				String initOperationName = (String)localVars[2];
				frame.execEnv.registerAttributeHelper(localVars[0], name, initOperationName);
				return null;
			}
		});
		operationsByName.put("conformsTo", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return new Boolean(((Class)localVars[1]).isAssignableFrom((Class)localVars[0]));
			}
		});

		// TransientLink
		operationsByName = new HashMap();
		vmTypeOperations.put(TransientLink.class, operationsByName);
		operationsByName.put("setRule", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				((TransientLink)localVars[0]).setRule((String)localVars[1]);
				return null;
			}
		});
		operationsByName.put("addSourceElement", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				((TransientLink)localVars[0]).getSourceElements().put(localVars[1], localVars[2]);
				return null;
			}
		});
		operationsByName.put("addTargetElement", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				TransientLink tl = (TransientLink)localVars[0];
				tl.getTargetElements().put(localVars[1], localVars[2]);
				tl.getTargetElementsList().add(localVars[2]);
				return null;
			}
		});
		operationsByName.put("getSourceElement", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return ((TransientLink)localVars[0]).getSourceElements().get(localVars[1]);
			}
		});
		operationsByName.put("getTargetElement", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Object ret = ((TransientLink)localVars[0]).getTargetElements().get(localVars[1]);
				if (ret == null) {
					ret = OclUndefined.SINGLETON;
				}
				return ret;
			}
		});
		operationsByName.put("getTargetFromSource", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Object ret = ((TransientLink)localVars[0]).getTargetElementsList().iterator().next();
				if (ret == null) {
					ret = OclUndefined.SINGLETON;
				}
				return ret;
			}
		});
		operationsByName.put("getNamedTargetFromSource", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Object ret = ((TransientLink)localVars[0]).getTargetElements().get(localVars[2]);
				if (ret == null) {
					ret = OclUndefined.SINGLETON;
				}
				return ret;
			}
		});
		operationsByName.put("addVariable", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				((TransientLink)localVars[0]).getVariables().put(localVars[1], localVars[2]);
				return null;
			}
		});
		operationsByName.put("getVariable", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return ((TransientLink)localVars[0]).getVariables().get(localVars[1]);
			}
		});

		// TransientLinkSet
		operationsByName = new HashMap();
		vmTypeOperations.put(TransientLinkSet.class, operationsByName);
		operationsByName.put("addLink", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				TransientLink tl = (TransientLink)localVars[1];
				TransientLinkSet tls = (TransientLinkSet)localVars[0];
				tls.addLink(tl);
				return null;
			}
		});
		operationsByName.put("addLink2", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				TransientLink tl = (TransientLink)localVars[1];
				TransientLinkSet tls = (TransientLinkSet)localVars[0];
				boolean isDefault = ((Boolean)localVars[2]).booleanValue();
				tls.addLink2(tl, isDefault);
				return null;
			}
		});
		operationsByName.put("getLinksByRule", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return ((TransientLinkSet)localVars[0]).getLinksByRule(localVars[1]);
			}
		});
		operationsByName.put("getLinkBySourceElement", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				TransientLink ret = ((TransientLinkSet)localVars[0]).getLinkBySourceElement(localVars[1]);
				if (ret == null) {
					return OclUndefined.SINGLETON;
				} else {
					return ret;
				}
			}
		});
		operationsByName.put("getLinkByRuleAndSourceElement", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				TransientLink ret = ((TransientLinkSet)localVars[0]).getLinkByRuleAndSourceElement(
						localVars[1], localVars[2]);
				if (ret == null) {
					return OclUndefined.SINGLETON;
				} else {
					return ret;
				}
			}
		});

		// Map
		operationsByName = new HashMap();
		vmTypeOperations.put(HashMap.class, operationsByName);
		operationsByName.put("get", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Object ret = ((Map)localVars[0]).get(localVars[1]);
				if (ret == null) {
					ret = OclUndefined.SINGLETON;
				}
				return ret;
			}
		});
		operationsByName.put("including", new Operation(3) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Map ret = new HashMap((Map)localVars[0]);
				ret.put(localVars[1], localVars[2]);
				return ret;
			}
		});
		operationsByName.put("getKeys", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return ((Map)localVars[0]).keySet();
			}
		});
		operationsByName.put("getValues", new Operation(1) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				return ((Map)localVars[0]).values();
			}
		});
		operationsByName.put("union", new Operation(2) {
			public Object exec(StackFrame frame) {
				Object[] localVars = frame.localVars;
				Map ret = new HashMap((Map)localVars[0]);
				ret.putAll((Map)localVars[1]);
				return ret;
			}
		});
	}

	private Map operationsByType = new HashMap();

	/** Stores the number of executed bytecodes. */
	private long nbExecutedBytecodes;

	/** The common model adapter. */
	private ModelAdapter modelAdapter;

	// TODO: map this to corresponding option
	private boolean cacheAttributeHelperResults = true;

	private Map helperValuesByElement = new HashMap();

	private Map attributeInitializers = new HashMap();

	private Map nameByModel;

	private ArrayList emptySequence = new ArrayList();

	private Operation noInitializer = new Operation(0) {
		public Object exec(StackFrame frame) {
			return null;
		}
	};

	/** Map of the model sorted by names. */
	private Map modelsByName;

	/** Debug mode. */
	private boolean step;

	/**
	 * Creates a new execenv parametrized by models.
	 * 
	 * @param models
	 *            the models map
	 */
	public ExecEnv(Map models) {
		this.modelsByName = models;
		nameByModel = new HashMap();
		for (Iterator i = modelsByName.keySet().iterator(); i.hasNext();) {
			String name = (String)i.next();
			Model model = (Model)modelsByName.get(name);
			nameByModel.put(model, name);
		}
	}

	/**
	 * Initializes the execenv.
	 * 
	 * @param modelAdapterParam
	 *            the model adapter
	 */
	public void init(ModelAdapter modelAdapterParam) {
		this.modelAdapter = modelAdapterParam;
		this.modelAdapter.registerVMSupertypes(this.supertypes);
		this.modelAdapter.registerVMTypeOperations(this.vmTypeOperations);
	}

	/**
	 * Returns the model containing the element.
	 * 
	 * @param element
	 *            a model element
	 * @return the model containing the element
	 */
	public String getModelNameOf(Object element) {
		return (String)nameByModel.get(modelAdapter.getModelOf(element));
	}

	/**
	 * Returns the model name.
	 * 
	 * @param model
	 *            a model
	 * @return the model name
	 */
	public String getNameOf(Model model) {
		return (String)nameByModel.get(model);
	}

	/**
	 * Returns the model by name.
	 * 
	 * @param name
	 *            the model name
	 * @return the model by name
	 */
	public Model getModel(Object name) {
		return (Model)modelsByName.get(name);
	}

	public Iterator getModels() {
		return modelsByName.values().iterator();
	}

	/**
	 * Find an operation by its context type and name.
	 * 
	 * @param type
	 *            operation context type
	 * @param name
	 *            operation name
	 * @return the operation
	 */
	public Operation getOperation(Object type, Object name) {
		// note: debug is final, therefore there is no runtime penalty if it is false
		final boolean debug = false;
		Operation ret = null;
		Map map = getOperations(type, false);
		if (map != null) {
			ret = (Operation)map.get(name);
		}
		if (debug) {
			logger.info(this + "@" + this.hashCode() + ".getOperation(" + type + ", " + name + ")");
		}
		if (ret == null) {
			if (debug) {
				logger.info("looking in super of this for operation " + name);
			}
			for (Iterator i = modelAdapter.getSupertypes(type).iterator(); i.hasNext() && (ret == null);) {
				Object st = i.next();
				ret = getOperation(st, name);
			}
			// let us remember this operation (remark: we could also precompute this for all types)
			if (map != null) {
				map.put(name, ret);
			}
		}

		return ret;
	}

	/**
	 * Stores an attribute helper.
	 * 
	 * @param type
	 *            the attribute type
	 * @param name
	 *            the attribute name
	 * @param initOperationName
	 *            the init operation name
	 */
	public void registerAttributeHelper(Object type, String name, String initOperationName) {
		Operation op = getOperation(type, initOperationName);
		getAttributeInitializers(type, true).put(name, op);
	}

	/**
	 * Returns the attribute initializer.
	 * 
	 * @param type
	 *            the attribute type
	 * @param name
	 *            the attribute name
	 * @return the attribute initializer
	 */
	public Operation getAttributeInitializer(Object type, String name) {
		Operation ret = null;
		Map map = getAttributeInitializers(type, true); // was false, but we need to remember search results
		if (map != null) {
			ret = (Operation)map.get(name);
		}
		if (ret == null) {
			for (Iterator i = modelAdapter.getSupertypes(type).iterator(); i.hasNext() && (ret == null);) {
				Object st = i.next();
				ret = getAttributeInitializer(st, name);
			}
			// let us remember (remark: we could precompute this)
			if (map != null) {
				if (ret == null) {
					ret = noInitializer;
				}
				map.put(name, ret);
			}
		}

		if (ret == noInitializer) {
			ret = null;
		}
		return ret;
	}

	private Map getAttributeInitializers(Object type, boolean createIfMissing) {
		Map ret = (Map)attributeInitializers.get(type);

		if (createIfMissing && (ret == null)) {
			ret = new HashMap();
			attributeInitializers.put(type, ret);
		}

		return ret;
	}

	private Map getHelperValues(Object element) {
		Map ret = (Map)helperValuesByElement.get(element);

		if (ret == null) {
			ret = new HashMap();
			helperValuesByElement.put(element, ret);
		}

		return ret;
	}

	/**
	 * Gets the value of an helper.
	 * 
	 * @param frame
	 *            the frame context
	 * @param type
	 *            the helper type
	 * @param element
	 *            the element
	 * @param name
	 *            the helper value name
	 * @return the value
	 */
	public Object getHelperValue(StackFrame frame, Object type, Object element, String name) {
		Object ret = null;
		Map helperValues = getHelperValues(element);
		SoftReference sr = (SoftReference)helperValues.get(name);

		if (sr != null) {
			ret = sr.get();
		}
		if (ret == null) {
			Operation o = getAttributeInitializer(type, name);

			StackFrame calleeFrame = frame.newFrame(o);
			Object[] arguments = calleeFrame.localVars;
			arguments[0] = element;

			ret = o.exec(calleeFrame);
			if (cacheAttributeHelperResults) {
				helperValues.put(name, new SoftReference(ret));
			}
		}

		return ret;
	}

	/**
	 * Registers operation for a given type.
	 * 
	 * @param type
	 *            the type
	 * @param oper
	 *            the operation
	 * @param name
	 *            the operation name
	 */
	public void registerOperation(Object type, Operation oper, String name) {
		getOperations(type, true).put(name, oper);
	}

	private Map getOperations(Object type, boolean createIfMissing) {
		Map ret = (Map)operationsByType.get(type);

		if (ret == null) {
			Map vmops = getVMOperations(type);
			if (createIfMissing || ((vmops != null) && !vmops.isEmpty())) {
				ret = new HashMap();
				operationsByType.put(type, ret);
				if (vmops != null) {
					ret.putAll(vmops);
				}
			}
		}

		return ret;
	}

	private Map getVMOperations(Object type) {
		return (Map)vmTypeOperations.get(type);
	}

	/**
	 * Converts a value to a displayable string.
	 * 
	 * @param value
	 *            the value to convert
	 * @return the displayable string
	 */
	public String toPrettyPrintedString(Object value) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		prettyPrint(new PrintStream(out), value);

		return out.toString();
	}

	/**
	 * Displays a value.
	 * 
	 * @param value
	 *            the value to display
	 */
	public void prettyPrint(Object value) {
		// prettyPrint(out, value);
		logger.info(toPrettyPrintedString(value));
	}

	/**
	 * Displays a value.
	 * 
	 * @param out
	 *            the stream
	 * @param value
	 *            the value to display
	 */
	public void prettyPrint(PrintStream out, Object value) {
		if (value == null) {
			out.print("<null>"); // print(null) does not work
		} else if (value instanceof String) {
			out.print('\'');
			out.print(value); // TODO: escape
			out.print('\'');
		} else if (value instanceof EnumLiteral) {
			out.print('#');
			out.print(value); // TODO: escape
		} else if (value instanceof LinkedHashSet) {
			out.print("OrderedSet {");
			prettyPrintCollection(out, (Collection)value);
		} else if (value instanceof HashSet) {
			out.print("Set {");
			prettyPrintCollection(out, (Collection)value);
		} else if (value instanceof ArrayList) {
			out.print("Sequence {");
			prettyPrintCollection(out, (Collection)value);
		} else if (value instanceof Bag) {
			out.print("Bag {");
			prettyPrintCollection(out, (Collection)value);
		} else if (value instanceof Tuple) {
			out.print("Tuple {");
			boolean first = true;
			for (Iterator i = ((Tuple)value).getMap().entrySet().iterator(); i.hasNext();) {
				Map.Entry entry = (Map.Entry)i.next();

				if (first) {
					first = false;
				} else {
					out.print(", ");
				}
				out.print(entry.getKey());
				out.print(" = ");
				prettyPrint(out, entry.getValue());
			}
			out.print('}');
		} else if (value instanceof HashMap) {
			out.print("Map {");
			boolean first = true;
			for (Iterator i = ((Map)value).entrySet().iterator(); i.hasNext();) {
				Map.Entry entry = (Map.Entry)i.next();

				if (first) {
					first = false;
				} else {
					out.print(", ");
				}
				out.print('(');
				prettyPrint(out, entry.getKey());
				out.print(", ");
				prettyPrint(out, entry.getValue());
				out.print(')');
			}
			out.print('}');
		} else if (value instanceof OclUndefined) {
			out.print("OclUndefined");
		} else {
			if (!modelAdapter.prettyPrint(out, value)) {
				out.print(value);
			}
		}
	}

	/**
	 * Display a collection.
	 * 
	 * @param out
	 *            the out stream
	 * @param col
	 *            the collection
	 */
	public void prettyPrintCollection(PrintStream out, Collection col) {
		boolean first = true;
		for (Iterator i = col.iterator(); i.hasNext();) {
			if (!first) {
				out.print(", ");
			}
			prettyPrint(out, i.next());
			first = false;
		}
		out.print('}');
	}

	/**
	 * Finds a meta element by its name.
	 * 
	 * @param frame
	 *            the frame context
	 * @param mname
	 *            the metamodel name
	 * @param me
	 *            the model element
	 * @return the meta element
	 */
	public static Object findMetaElement(org.eclipse.m2m.atl.engine.emfvm.lib.StackFrame frame, Object mname,
			Object me) {
		Object ret = null;

		ReferenceModel referenceModel = (ReferenceModel)frame.execEnv.getModel(mname);
		if (referenceModel != null) {
			ret = referenceModel.getMetaElementByName((String)me);
			if (ret == null) {
				throw new RuntimeException("cannot find class " + me + " in reference model " + mname);
			}
		} else {
			throw new RuntimeException("cannot find reference model " + mname);
		}

		return ret;
	}

	/**
	 * Creates a new element in the given frame.
	 * 
	 * @param frame
	 *            the frame context
	 * @param ec
	 *            the element type
	 * @return the new element
	 */
	public Object newElement(StackFrame frame, Object ec) {
		Object s = null;
		for (Iterator i = getModels(); i.hasNext();) {
			AbstractModel model = (AbstractModel)i.next();
			if (!model.isTarget()) {
				continue;
			}
			if (model.getReferenceModel().isModelOf(ec)) {
				s = model.newElement(ec);
				break;
			}
		}
		if (s == null) {
			throw new VMException(frame, "cannot create " + toPrettyPrintedString(ec));
		}
		return s;
	}

	/**
	 * Writes self to fileName with given character set.
	 * 
	 * @param frame
	 *            VM stack frame
	 * @param self
	 *            the string to write
	 * @param fileName
	 *            the file to write to
	 * @param charset
	 *            the character set to use, or use default when null
	 * @return true on success
	 * @throws VMException
	 *             if an {@link IOException} occurs
	 */
	private static boolean writeToWithCharset(StackFrame frame, String self, String fileName, String charset)
			throws VMException {
		boolean ret = false;
		try {
			File file = getFile(fileName);
			if (file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			}
			PrintStream out = null;
			if (charset == null) {
				out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)), true);
			} else {
				out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)), true, charset);
			}
			out.print(self);
			out.close();
			ret = true;
		} catch (IOException ioe) {
			throw new VMException(frame, ioe);
		}
		return ret;
	}

	/**
	 * Returns the file in the workspace, or the file in the filesystem if the workspace is not available.
	 * 
	 * @param path
	 *            the absolute or relative path to a file.
	 * @return the file in the workspace, or the file in the filesystem if the workspace is not available.
	 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
	 */
	private static File getFile(String path) {
		String newPath = null;
		try {
			Class[] emptyClassArray = new Class[] {};
			Object[] emptyObjectArray = new Object[] {};
			Class rp = Class.forName("org.eclipse.core.resources.ResourcesPlugin");
			Object ws = rp.getMethod("getWorkspace", emptyClassArray).invoke(null, emptyObjectArray);
			Object root = ws.getClass().getMethod("getRoot", emptyClassArray).invoke(ws, emptyObjectArray);
			Path wspath = new Path(path);
			Object wsfile = root.getClass().getMethod("getFile", new Class[] {IPath.class}).invoke(root,
					new Object[] {wspath});
			newPath = wsfile.getClass().getMethod("getLocation", emptyClassArray).invoke(wsfile,
					emptyObjectArray).toString();
		} catch (Throwable e) {
			// fall back to native java.io.File path resolution
		}
		return new File(newPath);
	}

	public Map getModelsByName() {
		return modelsByName;
	}

	public boolean isStep() {
		return step;
	}

	public void setStep(boolean step) {
		this.step = step;
	}

	public ModelAdapter getModelAdapter() {
		return modelAdapter;
	}

	public long getNbExecutedBytecodes() {
		return nbExecutedBytecodes;
	}

	/**
	 * Increments the nbExecutedBytecodes.
	 */
	public void incNbExecutedBytecodes() {
		this.nbExecutedBytecodes++;
	}

	public Map getSupertypes() {
		return supertypes;
	}
}
