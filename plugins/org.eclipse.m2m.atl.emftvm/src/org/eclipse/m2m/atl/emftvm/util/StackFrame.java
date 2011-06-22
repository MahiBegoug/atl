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

import java.lang.reflect.Method;
import java.util.Collection;

import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.m2m.atl.emftvm.CodeBlock;
import org.eclipse.m2m.atl.emftvm.ExecEnv;
import org.eclipse.m2m.atl.emftvm.LineNumber;
import org.eclipse.m2m.atl.emftvm.LocalVariable;
import org.eclipse.m2m.atl.emftvm.Module;


/**
 * EMFTVM stack frame.
 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
 */
public final class StackFrame {

	private static final Object[] EMPTY = new Object[0];

	private final ExecEnv env;
	private final StackFrame parent;
	private final CodeBlock codeBlock;
	private final Method nativeMethod;
	private final Object[] locals;
	private final Object[] stack;
	private int sp = -1;
	private int pc = -1; // Only set when necessary

	/**
	 * Creates a new {@link StackFrame}.
	 * Use only for root frames!
	 */
	public StackFrame(final ExecEnv env, final CodeBlock codeBlock) {
		this.env = env;
		this.parent = null;
		this.codeBlock = codeBlock;
		this.nativeMethod = null;
		locals = new Object[codeBlock.getMaxLocals()];
		stack = new Object[codeBlock.getMaxStack()];
	}

	/**
	 * Creates a new {@link StackFrame}.
	 * @param parent the parent stack frame, if any
	 * @param codeBlock the codeBlock context for this stack frame
	 */
	public StackFrame(final StackFrame parent, final CodeBlock codeBlock) {
		this.env = parent.env;
		this.parent = parent;
		this.codeBlock = codeBlock;
		this.nativeMethod = null;
		locals = new Object[codeBlock.getMaxLocals()];
		stack = new Object[codeBlock.getMaxStack()];
	}

	/**
	 * Creates a new {@link StackFrame}.
	 * @param parent the parent stack frame, if any
	 * @param nativeMethod the native Java method context for this stack frame
	 */
	public StackFrame(final StackFrame parent, final Method nativeMethod) {
		this.env = parent.env;
		this.parent = parent;
		this.codeBlock = null;
		this.nativeMethod = nativeMethod;
		this.locals = EMPTY;
		this.stack = EMPTY;
	}

	/**
	 * Pushes value onto the stack.
	 * @param value
	 */
	public void push(final Object value) {
		stack[++sp] = value;
	}

	/**
	 * Pops an element off the stack.
	 * @return the top element of the stack.
	 */
	public Object pop() {
		return stack[sp--];
	}

	/**
	 * Pops an element off the stack without returning it.
	 */
	public void popv() {
		sp--;
	}

	/**
	 * @return the top element of the stack.
	 */
	public Object peek() {
		return stack[sp];
	}

	/**
	 * @return <code>true</code> iff the stack is empty.
	 */
	public boolean stackEmpty() {
		return sp == -1;
	}

	/**
	 * @param cb the code block
	 * @return the stack frame of cb, starting at the parent, if any, otherwise <code>null</code>
	 */
	private StackFrame getStackFrameFor(final CodeBlock cb) {
		StackFrame parent = this.parent;
		while (parent != null) {
			if (parent.codeBlock == cb) {
				return parent;
			}
			parent = parent.parent;
		}
		return null;
	}

	/**
	 * @param cbOffset
	 * @return the parent codeblock that lies cbOffset positions up, or <code>null</code>
	 */
	private CodeBlock getCodeBlock(final int cbOffset) {
		CodeBlock cb = this.codeBlock;
		for (int i = 0; i < cbOffset; i++) {
			cb = cb.getNestedFor();
		}
		return cb;
	}

	/**
	 * Sets local variable with given slot to value.
	 * @param slot
	 * @param value
	 */
	public void setLocal(final int cbOffset, final int slot, final Object value) {
		if (cbOffset > 0) {
			try {
				final CodeBlock cb = getCodeBlock(cbOffset);
				final StackFrame parent = getStackFrameFor(cb);
				parent.locals[slot] = value;
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format(
						"Cannot address super-block local variable %d from %s",
						slot, this), e);
			}
		}
		locals[slot] = value;
	}

	/**
	 * Sets the first local variables to the given values.
	 * @param self
	 * @param values
	 */
	public void setLocals(final Object self, final Object[] values) {
		locals[0] = self;
		System.arraycopy(values, 0, locals, 1, values.length);
	}

	/**
	 * Sets the first local variables to the given values.
	 * @param values
	 */
	public void setLocals(final Object[] values) {
		System.arraycopy(values, 0, locals, 0, values.length);
	}

	/**
	 * @param cbOffset parent code block offset
	 * @param slot
	 * @return the local variable value with the given slot.
	 */
	public Object getLocal(final int cbOffset, final int slot) {
		if (cbOffset > 0) {
			try {
				final CodeBlock cb = getCodeBlock(cbOffset);
				final StackFrame parent = getStackFrameFor(cb);
				return parent.locals[slot];
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format(
						"Cannot address super-block local variable %d from %s",
						slot, this), e);
			}
		}
		return locals[slot];
	}

	/**
	 * Loads local variable with given cbOffset and slot onto the stack.
	 * @param cbOffset code block offset
	 * @param slot
	 */
	public void load(final int cbOffset, final int slot) {
		stack[++sp] = getLocal(cbOffset, slot);
	}

	/**
	 * Pops top stack value into local variable with given cbOffset and slot.
	 * @param cbOffset code block offset
	 * @param slot
	 */
	public void store(final int cbOffset, final int slot) {
		setLocal(cbOffset, slot, stack[sp--]);
	}

	/**
	 * Duplicates top stack value onto stack.
	 */
	public void dup() {
		sp++;
		stack[sp] = stack[sp-1];
	}

	/**
	 * Pops top two values from stack, pushes top value, then pushes original two values back.
	 */
	public void dup_x1() {
		sp++;						// .ab...
		final int sp1 = sp - 1;
		final int sp2 = sp1 - 1;
		stack[sp] = stack[sp1];		// aab...
		stack[sp1] = stack[sp2];	// abb...
		stack[sp2] = stack[sp];		// aba...
	}

	/**
	 * Swaps top two values on the stack.
	 */
	public void swap() {
		final Object top = stack[sp];	// ab...
		final int sp1 = sp - 1;
		stack[sp] = stack[sp1];			// bb...
		stack[sp1] = top;				// ba...
	}

	/**
	 * Swaps third value over top two values on the stack.
	 */
	public void swap_x1() {
		final Object top = stack[sp];	// abc...
		final int sp1 = sp - 1;
		final int sp2 = sp - 2;
		stack[sp] = stack[sp2];			// cbc...
		stack[sp2] = stack[sp1];		// cbb...
		stack[sp1] = top;				// cab...
	}

	/**
	 * @return the parent stack frame
	 */
	public StackFrame getParent() {
		return parent;
	}

	/**
	 * @return the codeBlock
	 */
	public CodeBlock getCodeBlock() {
		return codeBlock;
	}

	/**
	 * @return the env
	 */
	public ExecEnv getEnv() {
		return env;
	}

	/**
	 * @return the nativeMethod
	 */
	public Method getNativeMethod() {
		return nativeMethod;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final CodeBlock cb = getCodeBlock();
		final Method m = getNativeMethod();
		final int pc = getPc() - 1; // Program counter is 1-based
		final StringBuffer sb = new StringBuffer("at ");
		if (cb != null) {
			sb.append(cb);
		} else if (m != null) {
			sb.append(m);
		} else {
			sb.append(super.toString());
		}
		if (pc > -1) {
			sb.append('#');
			sb.append(pc);
			if (cb != null) {
				sb.append('(');
				final Module module = cb.getModule();
				if (module != null) {
					sb.append(module);
				} else {
					sb.append("<unknown>");
				}
				final LineNumber ln = cb.getCode().get(pc).getLineNumber();
				if (ln != null) {
					sb.append("#[");
					sb.append(ln.toString());
					sb.append(']');
				}
				sb.append(')');
			}
		} else if (cb != null) {
			sb.append('(');
			final Module module = cb.getModule();
			if (module != null) {
				sb.append(module);
			} else {
				sb.append("<unknown>");
			}
			sb.append(')');
		}
		sb.append("\n\tLocal variables: ");
		if (cb != null && pc > -1) {
			sb.append('[');
			boolean first = true;
			for (int slot = 0; slot < locals.length; slot++) {
				if (!first) {
					sb.append(", ");
				}
				first = false;
				for (LocalVariable lv : cb.getLocalVariables()) {
					if (lv.getSlot() == slot && lv.getStartInstructionIndex() <= pc && lv.getEndInstructionIndex() >= pc) {
						sb.append(lv.toString());
						sb.append(" = ");
						sb.append(EMFTVMUtil.toPrettyString(locals[slot], getEnv()));
						break;
					}
				}
			}
			sb.append(']');
		} else {
			sb.append(EMFTVMUtil.toPrettyString(locals, getEnv()));
		}
		final StackFrame parent = getParent();
		if (parent != null) {
			sb.append('\n');
			sb.append(parent.toString());
		}
		return sb.toString();
	}

	/**
	 * @param pc the pc to set
	 */
	public void setPc(int pc) {
		this.pc = pc;
	}

	/**
	 * @return the pc
	 */
	public int getPc() {
		return pc;
	}

	/**
	 * Retrieves a new stack frame that is a sub-frame of <code>this</code>.
	 * @param cb the code block of the sub-frame
	 * @param args the arguments to pass into the sub-frame
	 * @return a new stack frame
	 */
	public StackFrame getSubFrame(final CodeBlock cb, final Object[] args) {
		final StackFrame frame = new StackFrame(this, cb);
		if (args != null) {
			frame.setLocals(args);
		}
		return frame;
	}

	/**
	 * Retrieves a new stack frame that is a sub-frame of <code>this</code>.
	 * @param cb the code block of the sub-frame
	 * @param context the <code>self</code> argument to pass into the sub-frame
	 * @param args the other arguments to pass into the sub-frame
	 * @return a new stack frame
	 */
	public StackFrame getSubFrame(final CodeBlock cb, final Object context, final Object[] args) {
		final StackFrame frame = new StackFrame(this, cb);
		if (args != null) {
			frame.setLocals(context, args);
		} else {
			frame.setLocal(0, 0, context);
		}
		return frame;
	}

	/**
	 * Retrieves a new stack frame that is a sub-frame of <code>this</code>.
	 * @param method the native method of the sub-frame
	 * @param args the arguments to pass into the sub-frame
	 * @return a new stack frame
	 */
	public StackFrame getSubFrame(final Method method, final Object[] args) {
		final StackFrame frame = new StackFrame(this, method);
		if (args != null) {
			prepareNativeArgs(method, args, frame);
		}
		return frame;
	}

	/**
	 * Retrieves a new stack frame that is a sub-frame of <code>this</code>.
	 * @param method the native method of the sub-frame
	 * @param context the <code>self</code> argument to pass into the sub-frame
	 * @param args the other arguments to pass into the sub-frame
	 * @return a new stack frame
	 */
	public StackFrame getSubFrame(final Method method, final Object context, final Object[] args) {
		final StackFrame frame = new StackFrame(this, method);
		prepareCodeBlockArg(context, frame);
		if (args != null) {
			prepareNativeArgs(method, args, frame);
		}
		return frame;
	}

	/**
	 * Prepares arg instances of {@link CodeBlock} by setting their parent frame (for native re-entry).
	 * @param arg
	 * @param subFrame
	 */
	private static void prepareCodeBlockArg(final Object arg, final StackFrame subFrame) {
		if (arg instanceof CodeBlock) {
			((CodeBlock) arg).setParentFrame(subFrame);
		}
	}

	/**
	 * Prepares args instances of {@link CodeBlock} by setting their parent frame (for native re-entry).
	 * @param method
	 * @param args
	 * @param subFrame
	 */
	private static void prepareNativeArgs(final Method method, final Object[] args, final StackFrame subFrame) {
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			prepareCodeBlockArg(arg, subFrame);
			if (arg instanceof EnumLiteral) {
				args[i] = convertEnumLiteral((EnumLiteral) arg, method.getParameterTypes()[i]);
			}
		}
	}

	/**
	 * Tries to convert literal to an instance of type
	 * @param literal
	 * @param type
	 * @return an instance of type, or literal if conversion failed
	 */
	private static Object convertEnumLiteral(final EnumLiteral literal, final Class<?> type) {
		if (Enumerator.class.isAssignableFrom(type)) {
			try {
				final String litName = literal.getName();
				final java.lang.reflect.Field valuesField = type.getDeclaredField("VALUES");
				final Object values = valuesField.get(null);
				if (values instanceof Collection<?>) {
					for (Object value : (Collection<?>) values) {
						if (value instanceof Enumerator) {
							if (litName.equals(((Enumerator) value).getName()) ||
								litName.equals(value.toString())) {
								return value;
							}
						}
					}
				}
			// Ignore exceptions; just don't convert here
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			}
		}
		return literal;
	}

}
