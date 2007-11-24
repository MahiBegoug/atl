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
package org.eclipse.m2m.atl.engine.emfvm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.m2m.atl.engine.emfvm.lib.EMFUtils;
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
		nativeClasses.put("OrderedSet", LinkedHashSet.class);
		nativeClasses.put("Tuple", Tuple.class);
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
				if(bytecode.value > maxLocals)
					maxLocals = bytecode.value;
			}
		}
		maxLocals++;		// because the highest encountered index is maxLocals - 1
		nbNestedIterates++;	// because the highest encountered nesting level is nbNestedIterates - 1
	}

	public String getName() {
		return name;
	}

	public Object exec(org.eclipse.m2m.atl.engine.emfvm.lib.StackFrame frame) {
		// Note: debug is not initialized from a constant, and therefore has a performance impact
		// TODO: measure this impact, and possibly remove the debug code
final boolean debug = frame.execEnv.step;
		Object localVars[] = frame.localVars;
		int pc = 0;
		int fp = 0;
		Object stack[] = new Object[MAX_STACK];
		Iterator nestedIterate[] = new Iterator[nbNestedIterates];
		Iterator it;
		Object s;
		try {
		while(pc < nbBytecodes) {
			Bytecode bytecode = bytecodes[pc++];
			frame.execEnv.nbExecutedBytecodes++;
if(debug) {
	frame.execEnv.out.println(name + ":" + (pc - 1) + "\t" + bytecode);
//	frame.execEnv.out.println("" + frame.execEnv.nbExecutedBytecodes);
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
	frame.execEnv.out.print("\tCalling ");
	frame.execEnv.prettyPrint(self);
	frame.execEnv.out.print("." + bytecode.operand + "(");
}
				Object type = ExecEnv.getType(self);
				Operation operation = frame.execEnv.getOperation(type, bytecode.operand);
				if(operation == null)
					throw new VMException(frame, "operation not found: " + frame.execEnv.toPrettyPrintedString(self) + "." + bytecode.operand);
				StackFrame calleeFrame = (StackFrame)frame.newFrame(operation);
				Object arguments[] = calleeFrame.localVars;
				int nbCalleeArgs = bytecode.value;
				
				boolean first = true;
				for(int i = nbCalleeArgs ; i >= 1 ; i--) {
					arguments[i] = stack[--fp];
if(debug) {
	if(!first)
		frame.execEnv.out.print(", ");
	first = false;
	frame.execEnv.prettyPrint(arguments[i]);
}
				}
if(debug) {
	frame.execEnv.out.println(")");
}
				--fp;	// pop self, that we already retrieved earlier to get the operation
				arguments[0] = self;

				s = operation.exec(calleeFrame);
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
				if(s instanceof EObject) {
					if(value instanceof OclUndefined)	// other values are *not* wrapped
						value = null;
					EMFUtils.set(frame, (EObject)s, (String)bytecode.operand, value);
				} else {
					((HasFields)s).set(frame, bytecode.operand, value);
				}
				break;
			case Bytecode.GET:
				s = stack[--fp];
				type = ExecEnv.getType(s);
				String propName = (String)bytecode.operand;
				Operation ai = frame.execEnv.getAttributeInitializer(type, propName);
				if(ai != null) {
					stack[fp++] = frame.execEnv.getHelperValue(frame, type, s, propName);
				} else if(s instanceof EObject) {
					stack[fp++] = EMFUtils.get(frame, (EObject)s, propName);
				} else {
					stack[fp++] = ((HasFields)s).get(frame, propName);
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
			case Bytecode.GETASM:
				stack[fp++] = frame.asmModule;
				break;
			case Bytecode.NEW:
				Object mname = stack[--fp];
				Object me = stack[--fp];
				if(mname.equals("#native")){
// TODO: makes sure the Map implementation is actually faster, then get rid of if-else-if implementation
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
					EClass ec = ExecEnv.findMetaElement(frame, mname, me);
					stack[fp++] = frame.execEnv.newElement(frame, ec);
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
					EClass ec = ExecEnv.findMetaElement(frame, mname, me);
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
	frame.execEnv.out.print("\tstack: ");
	for(int i = 0 ; i < fp ; i++) {
		if(i > 0)
			frame.execEnv.out.print(", ");
		frame.execEnv.prettyPrint(stack[i]);
	}
	frame.execEnv.out.println();
	
	frame.execEnv.out.print("\tlocals: ");
	boolean first = true;
	for(int i = 0 ; i < localVars.length ; i++) {
		String name = resolveVariableName(i, pc);
		if(name != null) {
			if(!first) {
				frame.execEnv.out.print(", ");
			}
			first = false;
			frame.execEnv.out.print(name + "=");
			frame.execEnv.prettyPrint(localVars[i]);
		}
	}
	frame.execEnv.out.println();
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
}
