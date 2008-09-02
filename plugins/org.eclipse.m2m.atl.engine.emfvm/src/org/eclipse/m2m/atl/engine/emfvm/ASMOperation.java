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
 *    Obeo - metamodel method support
 *    
 * $Id: ASMOperation.java,v 1.9 2008/09/02 15:29:17 wpiers Exp $
 *******************************************************************************/
package org.eclipse.m2m.atl.engine.emfvm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.m2m.atl.engine.emfvm.lib.Bag;
import org.eclipse.m2m.atl.engine.emfvm.lib.EnumLiteral;
import org.eclipse.m2m.atl.engine.emfvm.lib.ExecEnv;
import org.eclipse.m2m.atl.engine.emfvm.lib.HasFields;
import org.eclipse.m2m.atl.engine.emfvm.lib.OclParametrizedType;
import org.eclipse.m2m.atl.engine.emfvm.lib.OclSimpleType;
import org.eclipse.m2m.atl.engine.emfvm.lib.OclUndefined;
import org.eclipse.m2m.atl.engine.emfvm.lib.Operation;
import org.eclipse.m2m.atl.engine.emfvm.lib.TransientLink;
import org.eclipse.m2m.atl.engine.emfvm.lib.TransientLinkSet;
import org.eclipse.m2m.atl.engine.emfvm.lib.Tuple;
import org.eclipse.m2m.atl.engine.emfvm.lib.VMException;

/**
 * ASM commands sheduler.
 *
 * @author Fr�d�ric Jouault <a href="mailto:frederic.jouault@univ-nantes.fr">frederic.jouault@univ-nantes.fr</a>
 * @author William Piers <a href="mailto:william.piers@obeo.fr">william.piers@obeo.fr</a>
 * @author Mikael Barbero <a href="mailto:mikael.barbero@univ-nantes.fr">mikael.barbero@univ-nantes.fr</a>
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ASMOperation extends Operation {

	public static final int MAX_STACK = 100;
	private String name;
	private String context;
	private List parameters = new ArrayList();
	private Bytecode bytecodes[];
	private int nbBytecodes;
	private int nbNestedIterates = 0;

	private List lineNumberTable = new ArrayList();
	private List localVariableTable = new ArrayList();

	public int getMaxLocals() {
		return maxLocals;
	}

	private static Map nativeClasses = new HashMap();
	static {
		nativeClasses.put("Sequence", ArrayList.class);
		nativeClasses.put("Set", HashSet.class);
		nativeClasses.put("Bag", Bag.class);
		nativeClasses.put("OrderedSet", LinkedHashSet.class);
		nativeClasses.put("Tuple", Tuple.class);
		nativeClasses.put("EnumLiteral", EnumLiteral.class);
		nativeClasses.put("OclSimpleType", OclSimpleType.class);
		nativeClasses.put("OclParametrizedType", OclParametrizedType.class);
		nativeClasses.put("TransientLinkSet", TransientLinkSet.class);
		nativeClasses.put("TransientLink", TransientLink.class);
		nativeClasses.put("Map", HashMap.class);

		// should not use "new" on the following types
		nativeClasses.put("String", String.class);
		nativeClasses.put("Integer", Integer.class);
		nativeClasses.put("OclAny", Object.class);
		nativeClasses.put("Boolean", Boolean.class);
		nativeClasses.put("Real", Double.class);
	}

	public ASMOperation(String name) {
		super(1);	// maxLocals will be computed later in setBytecodes()
		this.name = name;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getContext() {
		return context;
	}

	public void addParameter(String name, String type) {
		parameters.add(name);
	}

	public void addLineNumberEntry(String id, int begin, int end) {
		lineNumberTable.add(new LineNumberEntry(id, begin, end));
	}

	public List getLineNumberTable() {
		return lineNumberTable;
	}

	public String resolveLineNumber(int l) {
		String ret = null;

		for(Iterator i = lineNumberTable.iterator() ; i.hasNext() && (ret == null) ; ) {
			LineNumberEntry lne = (LineNumberEntry)i.next();
			if((l >= lne.begin) && (l <= lne.end)) {
				ret = lne.id;
			}
		}

		return ret;
	}

	public class LineNumberEntry {

		public LineNumberEntry(String id, int begin, int end) {
			this.id = id;
			this.begin = begin;
			this.end = end;
		}

		public String id;	/* startLine:startColumn-endLine:endColumn */
		public int begin;
		public int end;
	}

	public void addLocalVariableEntry(int slot, String name, int begin, int end) {
		localVariableTable.add(new LocalVariableEntry(slot, name, begin, end));
	}

	public List getLocalVariableTable() {
		return localVariableTable;
	}

	public String resolveVariableName(int slot, int l) {
		String ret = null;

		for(Iterator i = localVariableTable.iterator() ; i.hasNext() & (ret == null) ; ) {
			LocalVariableEntry lve = (LocalVariableEntry)i.next();

			if((slot == lve.slot) && (l >= lve.begin) && (l <= lve.end)) {
				ret = lve.name;
			}
		}

		return ret;
	}

	public class LocalVariableEntry {

		public LocalVariableEntry(int slot, String name, int begin, int end) {
			this.slot = slot;
			this.name = name;
			this.begin = begin;
			this.end = end;
		}

		public int slot;
		public String name;
		public int begin;
		public int end;
	}

	public void setBytecodes(Bytecode bytecodes[]) {
		this.bytecodes = bytecodes;
		this.nbBytecodes = bytecodes.length;

		// There are at least as many local variables (excluding self) as parameters.
		// This statement is necessary because the last parameters may be unused.
		maxLocals = parameters.size();

		// pre-computes:
		//	- target and nesting levels for iterate and enditerate
		//	- maxLocals
		Stack stack = new Stack();
		for(int i = 0 ; i < nbBytecodes ; i++) {
			Bytecode bytecode = bytecodes[i]; 
			if(bytecode.opcode == Bytecode.ITERATE) {
				bytecode.value2 = stack.size();
				stack.push(new Integer(i));
				if(bytecode.value2 > nbNestedIterates)
					nbNestedIterates = bytecode.value2;
			} else if(bytecode.opcode == Bytecode.ENDITERATE) {
				int iterateIndex = ((Integer)stack.pop()).intValue();
				bytecode.value = iterateIndex + 1;
				bytecode.value2 = stack.size();
				bytecodes[iterateIndex].value = i + 1;
			} else if((bytecode.opcode == Bytecode.LOAD) || (bytecode.opcode == Bytecode.STORE)) {
				// With the new model-based ASM the variables are explicit even without debug information.
				// Therefore, we could use that information instead of analyzing loads and stores.
				if(bytecode.value > maxLocals)
					maxLocals = bytecode.value;
			}
		}
		maxLocals++;		// because the highest encountered index is maxLocals - 1
		nbNestedIterates++;	// because the highest encountered nesting level is nbNestedIterates - 1
	}

	/**
	 * @return The bytecodes, if any.
	 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
	 */
	public Bytecode[] getBytecodes() {
		return bytecodes;
	}

	public String getName() {
		return name;
	}

	public Object exec(org.eclipse.m2m.atl.engine.emfvm.lib.StackFrame frame) {
		final ExecEnv execEnv = frame.execEnv;
		
		// Note: debug is not initialized from a constant, and therefore has a performance impact
		// TODO: measure this impact, and possibly remove the debug code
		final boolean debug = execEnv.step;
		
		final Object localVars[] = frame.localVars;
		int pc = 0;
		int fp = 0;
		final Object stack[] = new Object[MAX_STACK];
		final Iterator nestedIterate[] = new Iterator[nbNestedIterates];
		Iterator it;
		Object s;
		StringBuffer log = null;
		try {
			while(pc < nbBytecodes) {
				Bytecode bytecode = bytecodes[pc++];
				execEnv.nbExecutedBytecodes++;
				if(debug) {
					execEnv.logger.info(name + ":" + (pc - 1) + "\t" + bytecode);
//					logger.info("" + frame.execEnv.nbExecutedBytecodes);
				}
				switch(bytecode.opcode) {
				case Bytecode.PUSHD:
				case Bytecode.PUSHI:
				case Bytecode.PUSH:
					stack[fp++] = bytecode.operand;
					break;
				case Bytecode.PUSHT:
					stack[fp++] = Boolean.TRUE;
					break;
				case Bytecode.PUSHF:
					stack[fp++] = Boolean.FALSE;
					break;
				case Bytecode.CALL:
					Object self = stack[fp - bytecode.value - 1];
					if(debug) {
						log = new StringBuffer();
						log.append("\tCalling ");
						log.append(frame.execEnv.toPrettyPrintedString(self));
						log.append(".");
						log.append(bytecode.operand);
						log.append("(");
					}
					Object type = execEnv.modelAdapter.getType(self);
					int nbCalleeArgs = bytecode.value;
					Operation operation = execEnv.getOperation(type, bytecode.operand);

					if (operation != null) {
						StackFrame calleeFrame = (StackFrame)frame.newFrame(operation);
						Object arguments[] = calleeFrame.localVars;

						boolean first = true;
						for(int i = nbCalleeArgs ; i >= 1 ; i--) {
							arguments[i] = stack[--fp];
							if(debug) {
								if(!first)
									log.append(", ");
								first = false;
								log.append(execEnv.toPrettyPrintedString(arguments[i]));
							}
						}
						if(debug) {
							log.append(")");
							execEnv.logger.info(log.toString());
						}
						--fp;	// pop self, that we already retrieved earlier to get the operation
						arguments[0] = self;

						s = operation.exec(calleeFrame);
					} else {
						Assert.isTrue(bytecode.operand instanceof String);
						// find native method
						Object arguments[] = new Object[nbCalleeArgs];

						boolean first = true;
						for(int i = nbCalleeArgs-1 ; i >= 0 ; i--) {
							arguments[i] = stack[--fp];
							if(debug) {
								if(!first)
									log.append(", ");
								first = false;
								log.append(execEnv.toPrettyPrintedString(arguments[i]));
							}
						}
						if(debug) {
							log.append(")");
							execEnv.logger.info(log.toString());
						}
						--fp;	// pop self, that we already retrieved earlier to get the operation
						
						Method m = findMethod(self.getClass(), (String)bytecode.operand, getTypesOf(arguments));
						if (m == null)
							throw new VMException(frame, "operation not found: " + execEnv.toPrettyPrintedString(self) + "." + bytecode.operand);
						
						s = m.invoke(self, arguments);
					}
					
					if(s != null) stack[fp++] = s;
					break;
				case Bytecode.LOAD:
					stack[fp++] = localVars[bytecode.value];
					break;
				case Bytecode.STORE:
					localVars[bytecode.value] = stack[--fp];
					break;
				case Bytecode.SET:
					Object value = stack[--fp];
					s = stack[--fp];
					if(s instanceof HasFields) {
						((HasFields)s).set(frame, bytecode.operand, value);						
					} else {
						if(value instanceof Collection) {
							Collection c = (Collection)value;
							//TODO collections of collections have to be managed
							while(c.remove(OclUndefined.SINGLETON));
						} else if(value instanceof OclUndefined) {	// other values are *not* wrapped
							value = null;
						}
						execEnv.modelAdapter.set(frame, s, (String)bytecode.operand, value);
					}
					break;
				case Bytecode.GET:
					s = stack[--fp];
					type = execEnv.modelAdapter.getType(s);
					String propName = (String)bytecode.operand;
					Operation ai = execEnv.getAttributeInitializer(type, propName);
					if(ai != null) {
						stack[fp++] = execEnv.getHelperValue(frame, type, s, propName);
					} else if(s instanceof HasFields) {
						stack[fp++] = ((HasFields)s).get(frame, propName);
					} else {
						stack[fp++] = execEnv.modelAdapter.get(frame, s, propName);						
					}
					break;
				case Bytecode.DUP:
					s = stack[fp - 1];
					stack[fp++] = s;
					break;
				case Bytecode.DUP_X1:	// ..., value2, value1 => ..., value1, value2, value1
					s = stack[fp - 1];
					stack[fp++] = s;
					stack[fp - 2] = stack[fp - 3];
					stack[fp - 3] = stack[fp - 1];
					break;
				case Bytecode.DELETE:
					s = stack[--fp];
					execEnv.modelAdapter.delete(frame, s);
					break;
				case Bytecode.GETASM:
					stack[fp++] = frame.asmModule;
					break;
				case Bytecode.NEW:
					Object mname = stack[--fp];
					Object me = stack[--fp];
					if(mname.equals("#native")){
//						TODO: makes sure the Map implementation is actually faster, then get rid of if-else-if implementation
						/*
					if(me.equals("Sequence")) {
						stack[fp++] = new ArrayList();
					} else if(me.equals("Set")) {
						stack[fp++] = new HashSet();
					} else if(me.equals("OrderedSet")) {
						stack[fp++] = new LinkedHashSet();
					} else if(me.equals("Tuple")) {
						stack[fp++] = new Tuple();
					} else if(me.equals("OclSimpleType")) {
						stack[fp++] = new OclSimpleType();
					} else if(me.equals("OclParametrizedType")) {
						stack[fp++] = new OclParametrizedType();
					} else if(me.equals("TransientLinkSet")) {
						stack[fp++] = new TransientLinkSet();
					} else if(me.equals("TransientLink")) {
						stack[fp++] = new TransientLink();
					} else if(me.equals("Map")) {
						stack[fp++] = new HashMap();
					} else {
						throw new VMException(frame, "cannot create " + mname + "!" + me);
					}
/*/
						Class c = (Class)nativeClasses.get(me);
						if(c != null) {
							stack[fp++] = c.newInstance();
						} else {
							throw new VMException(frame, "cannot create " + mname + "!" + me);
						}
						/**/
					} else {
						Object ec = ExecEnv.findMetaElement(frame, mname, me);
						stack[fp++] = execEnv.newElement(frame, ec);
					}
					break;
				case Bytecode.FINDME:
					mname = stack[--fp];
					me = stack[--fp];
					if(mname.equals("#native")){					
						Class c = (Class)nativeClasses.get(me);
						if(c != null) {
							stack[fp++] = c;
						} else {
							throw new VMException(frame, "cannot find " + mname + "!" + me);
						}
					} else {
						Object ec = ExecEnv.findMetaElement(frame, mname, me);
						stack[fp++] = ec;
					}
					break;
				case Bytecode.ITERATE:
					Collection c = (Collection)stack[--fp];
					it = c.iterator();
					if(it.hasNext()) {
						nestedIterate[bytecode.value2] = it;
						stack[fp++] = it.next();
					} else {
						pc = bytecode.value;
					}

					break;
				case Bytecode.ENDITERATE:
					it = nestedIterate[bytecode.value2];
					if(it.hasNext()) {
						stack[fp++] = it.next();
						pc = bytecode.value;
					}
					break;
				case Bytecode.POP:
					fp--;
					break;
				case Bytecode.SWAP:
					s = stack[fp - 1];
					stack[fp - 1] = stack[fp - 2];
					stack[fp - 2] = s;
					break;
				case Bytecode.IF:
					if(Boolean.TRUE.equals(stack[--fp])) {
						pc = bytecode.value;
					}
					break;
				case Bytecode.GOTO:
					pc = bytecode.value;
					break;
				default:
					throw new VMException(frame, "Unimplemented bytecode " + bytecode.opcode);
				}

				if(debug) {
					log = new StringBuffer();
					log.append("\tstack: ");
					for(int i = 0 ; i < fp ; i++) {
						if(i > 0)
							log.append(", ");
						log.append(frame.execEnv.toPrettyPrintedString(stack[i]));
					}
					execEnv.logger.info(log.toString());
					
					log = new StringBuffer();
					log.append("\tlocals: ");
					boolean first = true;
					for(int i = 0 ; i < localVars.length ; i++) {
						String name = resolveVariableName(i, pc);
						if(name != null) {
							if(!first) {
								log.append(", ");
							}
							first = false;
							log.append(name + "=");
							log.append(frame.execEnv.toPrettyPrintedString(localVars[i]));
						}
					}
					execEnv.logger.info(log.toString());
				}
			}
		} catch(Exception e) {
			((StackFrame)frame).pc = pc - 1;
			if(e instanceof VMException)
				throw (VMException)e;	// do not rewrap
			else
				throw new VMException(frame, e);
		}

		return (fp > 0 ? stack[--fp] : null);
	}
	
	private static Class[] getTypesOf(Object[] arguments) {
		final Class argumentTypes[] = new Class[arguments.length];
		for(int i = 0; i < arguments.length; i++) {
			argumentTypes[i] = arguments[i].getClass();
		}
		return argumentTypes;
	}

	/**
	 * Looks for a method into cache and metamodel.
	 * @param caller The class of the method
	 * @param name The method name
	 * @param argumentTypes The types of all arguments
	 * @return the method if found, null otherwise
	 */
	protected static Method findMethod(Class caller, String name, Class argumentTypes[]) {
		final String sig = getMethodSignature(name, argumentTypes);
		Method ret = findCachedMethod(caller, sig);
		if (ret != null) {
			return ret;
		}

		Method methods[] = caller.getDeclaredMethods(); 
		for(int i = 0 ; i < (methods.length) && (ret == null) ; i++) {
			Method method = methods[i];
			if(method.getName().equals(name)) {
				Class pts[] = method.getParameterTypes();
				if(pts.length == argumentTypes.length) {
					boolean ok = true;
					for(int j = 0 ; (j < pts.length) && ok ; j++) {
						if(!pts[j].isAssignableFrom(argumentTypes[j])) {
							if (!(pts[j] == boolean.class && argumentTypes[j] == Boolean.class
									|| pts[j] == int.class && argumentTypes[j] == Integer.class
									|| pts[j] == char.class && argumentTypes[j] == Character.class
									|| pts[j] == long.class && argumentTypes[j] == Long.class
									|| pts[j] == float.class && argumentTypes[j] == Float.class
									|| pts[j] == double.class && argumentTypes[j] == Double.class)) {
								ok = false;
							}
						}
					}
					if(ok)
						ret = method;
				}
			}
		}

		if((ret == null) && (caller.getSuperclass() != null)) {
			ret = findMethod(caller.getSuperclass(), name, argumentTypes);
		}

		cacheMethod(caller, sig, ret);

		return ret;
	}

	/**
	 * Cache used to store methods
	 */
	private static WeakHashMap methodCache = new WeakHashMap();

	/**
	 * Find a method in the cache
	 * @param caller The class of the method
	 * @param signature The method signature
	 * @return the method
	 */
	private static Method findCachedMethod(Class caller, String signature) {
		Method ret = null;
		Map sigMap = (Map) methodCache.get(caller);
		if (sigMap != null) {
			ret = (Method) sigMap.get(signature);
		}
		return ret;
	}

	/**
	 * Stores a method in a cache.
	 * @param caller The class of the method
	 * @param signature The method signature
	 * @param method The method to store
	 */
	private static void cacheMethod(Class caller, String signature, Method method) {
		synchronized (methodCache) {
			Map sigMap = (Map) methodCache.get(caller);
			if (sigMap == null) {
				sigMap = new HashMap();
				methodCache.put(caller, sigMap);
			}
			sigMap.put(signature, method);
		}
	}

	/**
	 * Generates a String signature to store methods
	 * @param name
	 * @param argumentTypes
	 * @return The method signature
	 */
	private static String getMethodSignature(String name, Class argumentTypes[]) {
		StringBuffer sig = new StringBuffer();
		sig.append(name);
		sig.append('(');
		for (int i = 0; i < argumentTypes.length; i++) {
			if (i > 0) {
				sig.append(',');
			}
			sig.append(argumentTypes[i].getName());
		}
		sig.append(')');
		return sig.toString();
	}

	public String toString() {
		return this.context + "." + this.name;
	}
}
