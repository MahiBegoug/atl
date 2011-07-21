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
package org.eclipse.m2m.atl.emftvm.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.InternalEList;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.m2m.atl.emftvm.Add;
import org.eclipse.m2m.atl.emftvm.And;
import org.eclipse.m2m.atl.emftvm.CodeBlock;
import org.eclipse.m2m.atl.emftvm.EmftvmPackage;
import org.eclipse.m2m.atl.emftvm.Enditerate;
import org.eclipse.m2m.atl.emftvm.ExecEnv;
import org.eclipse.m2m.atl.emftvm.Feature;
import org.eclipse.m2m.atl.emftvm.Field;
import org.eclipse.m2m.atl.emftvm.Findtype;
import org.eclipse.m2m.atl.emftvm.Get;
import org.eclipse.m2m.atl.emftvm.GetStatic;
import org.eclipse.m2m.atl.emftvm.GetTrans;
import org.eclipse.m2m.atl.emftvm.Getcb;
import org.eclipse.m2m.atl.emftvm.Goto;
import org.eclipse.m2m.atl.emftvm.If;
import org.eclipse.m2m.atl.emftvm.Ifn;
import org.eclipse.m2m.atl.emftvm.Ifte;
import org.eclipse.m2m.atl.emftvm.Implies;
import org.eclipse.m2m.atl.emftvm.InputRuleElement;
import org.eclipse.m2m.atl.emftvm.Insert;
import org.eclipse.m2m.atl.emftvm.Instruction;
import org.eclipse.m2m.atl.emftvm.Invoke;
import org.eclipse.m2m.atl.emftvm.InvokeAllCbs;
import org.eclipse.m2m.atl.emftvm.InvokeCb;
import org.eclipse.m2m.atl.emftvm.InvokeCbS;
import org.eclipse.m2m.atl.emftvm.InvokeStatic;
import org.eclipse.m2m.atl.emftvm.InvokeSuper;
import org.eclipse.m2m.atl.emftvm.Iterate;
import org.eclipse.m2m.atl.emftvm.LineNumber;
import org.eclipse.m2m.atl.emftvm.Load;
import org.eclipse.m2m.atl.emftvm.LocalVariable;
import org.eclipse.m2m.atl.emftvm.Match;
import org.eclipse.m2m.atl.emftvm.MatchS;
import org.eclipse.m2m.atl.emftvm.Model;
import org.eclipse.m2m.atl.emftvm.Module;
import org.eclipse.m2m.atl.emftvm.New;
import org.eclipse.m2m.atl.emftvm.Operation;
import org.eclipse.m2m.atl.emftvm.Or;
import org.eclipse.m2m.atl.emftvm.Push;
import org.eclipse.m2m.atl.emftvm.Remove;
import org.eclipse.m2m.atl.emftvm.Rule;
import org.eclipse.m2m.atl.emftvm.Set;
import org.eclipse.m2m.atl.emftvm.SetStatic;
import org.eclipse.m2m.atl.emftvm.Store;
import org.eclipse.m2m.atl.emftvm.util.DuplicateEntryException;
import org.eclipse.m2m.atl.emftvm.util.EMFTVMUtil;
import org.eclipse.m2m.atl.emftvm.util.EnumLiteral;
import org.eclipse.m2m.atl.emftvm.util.LazyBagOnCollection;
import org.eclipse.m2m.atl.emftvm.util.LazyList;
import org.eclipse.m2m.atl.emftvm.util.LazyListOnCollection;
import org.eclipse.m2m.atl.emftvm.util.LazyListOnList;
import org.eclipse.m2m.atl.emftvm.util.LazySetOnSet;
import org.eclipse.m2m.atl.emftvm.util.Matcher;
import org.eclipse.m2m.atl.emftvm.util.NativeTypes;
import org.eclipse.m2m.atl.emftvm.util.StackFrame;
import org.eclipse.m2m.atl.emftvm.util.VMException;
import org.eclipse.m2m.atl.emftvm.util.VMMonitor;


/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Code Block</b></em>'.
 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getMaxLocals <em>Max Locals</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getMaxStack <em>Max Stack</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getCode <em>Code</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getLineNumbers <em>Line Numbers</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getLocalVariables <em>Local Variables</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getMatcherFor <em>Matcher For</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getApplierFor <em>Applier For</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getPostApplyFor <em>Post Apply For</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getBodyFor <em>Body For</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getInitialiserFor <em>Initialiser For</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getNested <em>Nested</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getNestedFor <em>Nested For</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getParentFrame <em>Parent Frame</em>}</li>
 *   <li>{@link org.eclipse.m2m.atl.emftvm.impl.CodeBlockImpl#getBindingFor <em>Binding For</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class CodeBlockImpl extends EObjectImpl implements CodeBlock {

	/**
	 * The default value of the '{@link #getMaxLocals() <em>Max Locals</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxLocals()
	 * @generated
	 * @ordered
	 */
	protected static final int MAX_LOCALS_EDEFAULT = -1;

	/**
	 * The default value of the '{@link #getMaxStack() <em>Max Stack</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxStack()
	 * @generated
	 * @ordered
	 */
	protected static final int MAX_STACK_EDEFAULT = -1;

	/**
	 * The cached value of the '{@link #getMaxLocals() <em>Max Locals</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxLocals()
	 * @generated NOT
	 * @ordered
	 */
	protected int maxLocals = MAX_LOCALS_EDEFAULT;

	/**
	 * The cached value of the '{@link #getMaxStack() <em>Max Stack</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxStack()
	 * @generated NOT
	 * @ordered
	 */
	protected int maxStack = MAX_STACK_EDEFAULT;

	/**
	 * The cached value of the '{@link #getCode() <em>Code</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCode()
	 * @generated
	 * @ordered
	 */
	protected EList<Instruction> code;

	/**
	 * The cached value of the '{@link #getLineNumbers() <em>Line Numbers</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLineNumbers()
	 * @generated
	 * @ordered
	 */
	protected EList<LineNumber> lineNumbers;

	/**
	 * The cached value of the '{@link #getLocalVariables() <em>Local Variables</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLocalVariables()
	 * @generated
	 * @ordered
	 */
	protected EList<LocalVariable> localVariables;

	/**
	 * The cached value of the '{@link #getNested() <em>Nested</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getNested()
	 * @generated
	 * @ordered
	 */
	protected EList<CodeBlock> nested;

	/**
	 * The default value of the '{@link #getParentFrame() <em>Parent Frame</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getParentFrame()
	 * @generated
	 * @ordered
	 */
	protected static final StackFrame PARENT_FRAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getParentFrame() <em>Parent Frame</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getParentFrame()
	 * @generated
	 * @ordered
	 */
	protected StackFrame parentFrame = PARENT_FRAME_EDEFAULT;

	private boolean ruleSet;
	private Rule rule;

	/**
	 * <!-- begin-user-doc -->
	 * Creates a new {@link CodeBlockImpl}.
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected CodeBlockImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * Returns the {@link EClass} that correspond to this metaclass.
	 * @return the {@link EClass} that correspond to this metaclass.
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return EmftvmPackage.Literals.CODE_BLOCK;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public int getMaxLocals() {
		if (maxLocals == MAX_LOCALS_EDEFAULT) {
			for (LocalVariable lv : getLocalVariables()) {
				maxLocals = Math.max(maxLocals, lv.getSlot());
			}
			maxLocals++; // highest index + 1
		}
		return maxLocals;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public void setMaxLocals(int newMaxLocals) {
		int oldMaxLocals = maxLocals;
		maxLocals = newMaxLocals;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, EmftvmPackage.CODE_BLOCK__MAX_LOCALS, oldMaxLocals, maxLocals));
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public int getMaxStack() {
		if (maxStack == MAX_STACK_EDEFAULT) {
			maxStack = 0;
			for (Instruction instr : getCode()) {
				maxStack = Math.max(maxStack, instr.getStackLevel());
			}
		}
		return maxStack;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public void setMaxStack(int newMaxStack) {
		int oldMaxStack = maxStack;
		maxStack = newMaxStack;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, EmftvmPackage.CODE_BLOCK__MAX_STACK, oldMaxStack, maxStack));
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Instruction> getCode() {
		if (code == null) {
			code = new EObjectContainmentWithInverseEList<Instruction>(Instruction.class, this, EmftvmPackage.CODE_BLOCK__CODE, EmftvmPackage.INSTRUCTION__OWNING_BLOCK);
		}
		return code;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<LineNumber> getLineNumbers() {
		if (lineNumbers == null) {
			lineNumbers = new EObjectContainmentWithInverseEList<LineNumber>(LineNumber.class, this, EmftvmPackage.CODE_BLOCK__LINE_NUMBERS, EmftvmPackage.LINE_NUMBER__OWNING_BLOCK);
		}
		return lineNumbers;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<LocalVariable> getLocalVariables() {
		if (localVariables == null) {
			localVariables = new EObjectContainmentWithInverseEList<LocalVariable>(LocalVariable.class, this, EmftvmPackage.CODE_BLOCK__LOCAL_VARIABLES, EmftvmPackage.LOCAL_VARIABLE__OWNING_BLOCK);
		}
		return localVariables;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Rule getMatcherFor() {
		if (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__MATCHER_FOR) return null;
		return (Rule)eContainer();
	}

	/**
	 * <!-- begin-user-doc. -->
	 * @see CodeBlockImpl#setMatcherFor(Rule)
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetMatcherFor(Rule newMatcherFor, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newMatcherFor, EmftvmPackage.CODE_BLOCK__MATCHER_FOR, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMatcherFor(Rule newMatcherFor) {
		if (newMatcherFor != eInternalContainer() || (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__MATCHER_FOR && newMatcherFor != null)) {
			if (EcoreUtil.isAncestor(this, newMatcherFor))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newMatcherFor != null)
				msgs = ((InternalEObject)newMatcherFor).eInverseAdd(this, EmftvmPackage.RULE__MATCHER, Rule.class, msgs);
			msgs = basicSetMatcherFor(newMatcherFor, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, EmftvmPackage.CODE_BLOCK__MATCHER_FOR, newMatcherFor, newMatcherFor));
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Rule getApplierFor() {
		if (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__APPLIER_FOR) return null;
		return (Rule)eContainer();
	}

	/**
	 * <!-- begin-user-doc. -->
	 * @see #setApplierFor(Rule)
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetApplierFor(Rule newApplierFor, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newApplierFor, EmftvmPackage.CODE_BLOCK__APPLIER_FOR, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setApplierFor(Rule newApplierFor) {
		if (newApplierFor != eInternalContainer() || (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__APPLIER_FOR && newApplierFor != null)) {
			if (EcoreUtil.isAncestor(this, newApplierFor))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newApplierFor != null)
				msgs = ((InternalEObject)newApplierFor).eInverseAdd(this, EmftvmPackage.RULE__APPLIER, Rule.class, msgs);
			msgs = basicSetApplierFor(newApplierFor, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, EmftvmPackage.CODE_BLOCK__APPLIER_FOR, newApplierFor, newApplierFor));
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Rule getPostApplyFor() {
		if (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__POST_APPLY_FOR) return null;
		return (Rule)eContainer();
	}

	/**
	 * <!-- begin-user-doc. -->
	 * @see #setPostApplyFor(Rule)
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetPostApplyFor(Rule newPostApplyFor, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newPostApplyFor, EmftvmPackage.CODE_BLOCK__POST_APPLY_FOR, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPostApplyFor(Rule newPostApplyFor) {
		if (newPostApplyFor != eInternalContainer() || (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__POST_APPLY_FOR && newPostApplyFor != null)) {
			if (EcoreUtil.isAncestor(this, newPostApplyFor))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newPostApplyFor != null)
				msgs = ((InternalEObject)newPostApplyFor).eInverseAdd(this, EmftvmPackage.RULE__POST_APPLY, Rule.class, msgs);
			msgs = basicSetPostApplyFor(newPostApplyFor, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, EmftvmPackage.CODE_BLOCK__POST_APPLY_FOR, newPostApplyFor, newPostApplyFor));
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Operation getBodyFor() {
		if (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__BODY_FOR) return null;
		return (Operation)eContainer();
	}

	/**
	 * <!-- begin-user-doc. -->
	 * @see #setBodyFor(Operation)
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetBodyFor(Operation newBodyFor, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newBodyFor, EmftvmPackage.CODE_BLOCK__BODY_FOR, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setBodyFor(Operation newBodyFor) {
		if (newBodyFor != eInternalContainer() || (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__BODY_FOR && newBodyFor != null)) {
			if (EcoreUtil.isAncestor(this, newBodyFor))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newBodyFor != null)
				msgs = ((InternalEObject)newBodyFor).eInverseAdd(this, EmftvmPackage.OPERATION__BODY, Operation.class, msgs);
			msgs = basicSetBodyFor(newBodyFor, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, EmftvmPackage.CODE_BLOCK__BODY_FOR, newBodyFor, newBodyFor));
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Field getInitialiserFor() {
		if (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__INITIALISER_FOR) return null;
		return (Field)eContainer();
	}

	/**
	 * <!-- begin-user-doc. -->
	 * @see #setInitialiserFor(Field)
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetInitialiserFor(Field newInitialiserFor, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newInitialiserFor, EmftvmPackage.CODE_BLOCK__INITIALISER_FOR, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setInitialiserFor(Field newInitialiserFor) {
		if (newInitialiserFor != eInternalContainer() || (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__INITIALISER_FOR && newInitialiserFor != null)) {
			if (EcoreUtil.isAncestor(this, newInitialiserFor))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newInitialiserFor != null)
				msgs = ((InternalEObject)newInitialiserFor).eInverseAdd(this, EmftvmPackage.FIELD__INITIALISER, Field.class, msgs);
			msgs = basicSetInitialiserFor(newInitialiserFor, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, EmftvmPackage.CODE_BLOCK__INITIALISER_FOR, newInitialiserFor, newInitialiserFor));
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<CodeBlock> getNested() {
		if (nested == null) {
			nested = new EObjectContainmentWithInverseEList<CodeBlock>(CodeBlock.class, this, EmftvmPackage.CODE_BLOCK__NESTED, EmftvmPackage.CODE_BLOCK__NESTED_FOR);
		}
		return nested;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public CodeBlock getNestedFor() {
		if (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__NESTED_FOR) return null;
		return (CodeBlock)eContainer();
	}

	/**
	 * <!-- begin-user-doc. -->
	 * @see #setNestedFor(CodeBlock)
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetNestedFor(CodeBlock newNestedFor, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newNestedFor, EmftvmPackage.CODE_BLOCK__NESTED_FOR, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setNestedFor(CodeBlock newNestedFor) {
		if (newNestedFor != eInternalContainer() || (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__NESTED_FOR && newNestedFor != null)) {
			if (EcoreUtil.isAncestor(this, newNestedFor))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newNestedFor != null)
				msgs = ((InternalEObject)newNestedFor).eInverseAdd(this, EmftvmPackage.CODE_BLOCK__NESTED, CodeBlock.class, msgs);
			msgs = basicSetNestedFor(newNestedFor, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, EmftvmPackage.CODE_BLOCK__NESTED_FOR, newNestedFor, newNestedFor));
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public StackFrame getParentFrame() {
		return parentFrame;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setParentFrame(StackFrame newParentFrame) {
		StackFrame oldParentFrame = parentFrame;
		parentFrame = newParentFrame;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, EmftvmPackage.CODE_BLOCK__PARENT_FRAME, oldParentFrame, parentFrame));
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InputRuleElement getBindingFor() {
		if (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__BINDING_FOR) return null;
		return (InputRuleElement)eContainer();
	}

	/**
	 * <!-- begin-user-doc. -->
	 * @see #setBindingFor(InputRuleElement)
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetBindingFor(InputRuleElement newBindingFor, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newBindingFor, EmftvmPackage.CODE_BLOCK__BINDING_FOR, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setBindingFor(InputRuleElement newBindingFor) {
		if (newBindingFor != eInternalContainer() || (eContainerFeatureID() != EmftvmPackage.CODE_BLOCK__BINDING_FOR && newBindingFor != null)) {
			if (EcoreUtil.isAncestor(this, newBindingFor))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newBindingFor != null)
				msgs = ((InternalEObject)newBindingFor).eInverseAdd(this, EmftvmPackage.INPUT_RULE_ELEMENT__BINDING, InputRuleElement.class, msgs);
			msgs = basicSetBindingFor(newBindingFor, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, EmftvmPackage.CODE_BLOCK__BINDING_FOR, newBindingFor, newBindingFor));
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}

	 * @see org.eclipse.m2m.atl.emftvm.CodeBlock#execute(StackFrame)
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public Object execute(StackFrame frame) {
		int pc = 0;
		final EList<Instruction> code = getCode();
		final int codeSize = code.size();
		final VMMonitor monitor = frame.getEnv().getMonitor();

		if (monitor != null) {
			monitor.enter(frame);
		}

		try {
			LOOP:
			while (pc < codeSize) {
				Instruction instr = code.get(pc++);
				if (monitor != null) {
					if (monitor.isTerminated()) {
						throw new VMException(frame, "Execution terminated.");
					} else {
						frame.setPc(pc);
						monitor.step(frame);
					}
				}
				switch (instr.getOpcode()) {
				case PUSH:
					frame.push(((Push)instr).getValue());
					break;
				case PUSHT:
					frame.push(true);
					break;
				case PUSHF:
					frame.push(false);
					break;
				case POP:
					frame.popv();
					break;
				case LOAD:
					frame.load(((Load)instr).getCbOffset(), ((Load)instr).getSlot());
					break;
				case STORE:
					frame.store(((Store)instr).getCbOffset(), ((Store)instr).getSlot());
					break;
				case SET:
					set(((Set)instr).getFieldname(), frame);
					break;
				case GET:
					frame.setPc(pc);
					frame.push(get(((Get)instr).getFieldname(), frame));
					break;
				case GET_TRANS:
					frame.setPc(pc);
					frame.push(getTrans(((GetTrans)instr).getFieldname(), frame));
					break;
				case SET_STATIC:
					setStatic(((SetStatic)instr).getFieldname(), frame);
					break;
				case GET_STATIC:
					frame.setPc(pc);
					frame.push(getStatic(((GetStatic)instr).getFieldname(), frame));
					break;
				case FINDTYPE: 
					frame.push(frame.getEnv().findType(
									((Findtype)instr).getModelname(), 
									((Findtype)instr).getTypename()));
					break;
				case FINDTYPE_S:
					frame.push(frame.getEnv().findType(
									(String)frame.pop(), 
									(String)frame.pop()));
					break;
				case NEW:
					frame.push(newInstr(((New)instr).getModelname(), 
									frame.pop(), 
									frame));
					break;
				case NEW_S:
					frame.push(newInstr((String)frame.pop(), 
									frame.pop(),
									frame));
					break;
				case DELETE:
					delete(frame);
					break;
				case DUP:
					frame.dup();
					break;
				case DUP_X1:
					frame.dupX1();
					break;
				case SWAP:
					frame.swap();
					break;
				case SWAP_X1:
					frame.swapX1();
					break;
				case IF:
					if ((Boolean)frame.pop()) {
						pc = ((If)instr).getOffset();
					}
					break;
				case IFN:
					if (!(Boolean)frame.pop()) {
						pc = ((Ifn)instr).getOffset();
					}
					break;
				case GOTO:
					pc = ((Goto)instr).getOffset();
					break;
				case ITERATE:
					Iterator<?> i = ((Collection<?>)frame.pop()).iterator();
					if (i.hasNext()) {
						frame.push(i);
						frame.push(i.next());
					} else {
						pc = ((Iterate)instr).getOffset(); // jump over ENDITERATE
					}
					break;
				case ENDITERATE:
					i = (Iterator<?>)frame.pop();
					if (i.hasNext()) {
						frame.push(i);
						frame.push(i.next());
						pc = ((Enditerate)instr).getOffset(); // jump to first loop instruction
					}
					break;
				case INVOKE:
					frame.setPc(pc);
					frame.push(invoke(((Invoke)instr).getOpname(), ((Invoke)instr).getArgcount(), frame));
					break;
				case INVOKE_STATIC: 
					frame.setPc(pc);
					frame.push(invokeStatic(((InvokeStatic)instr).getOpname(), ((InvokeStatic)instr).getArgcount(), frame));
					break;
				case INVOKE_SUPER: 
					frame.setPc(pc);
					frame.push(invokeSuper(getOperation(), ((InvokeSuper)instr).getOpname(), ((InvokeSuper)instr).getArgcount(), frame));
					break;
				case ALLINST:
					frame.push(EMFTVMUtil.findAllInstances(frame.getEnv(),
									(EClass)frame.pop()));
					break;
				case ALLINST_IN:
					frame.push(EMFTVMUtil.findAllInstIn(frame.getEnv(), 
									(EClass)frame.pop(), 
									frame.pop()));
					break;
				case ISNULL:
					frame.push(frame.pop() == null);
					break;
				case GETENVTYPE:
					frame.push(EmftvmPackage.eINSTANCE.getExecEnv());
					break;
				case NOT:
					frame.push(!(Boolean)frame.pop());
					break;
				case AND:
					CodeBlock cb = ((And)instr).getCodeBlock();
					frame.setPc(pc);
					frame.push((Boolean)frame.pop() && 
								(Boolean)cb.execute(frame.getSubFrame(cb, null)));
					break;
				case OR:
					cb = ((Or)instr).getCodeBlock();
					frame.setPc(pc);
					frame.push((Boolean)frame.pop() ||
								(Boolean)cb.execute(frame.getSubFrame(cb, null)));
					break;
				case XOR:
					frame.push((Boolean)frame.pop() ^ (Boolean)frame.pop());
					break;
				case IMPLIES:
					cb = ((Implies)instr).getCodeBlock();
					frame.setPc(pc);
					frame.push(!(Boolean)frame.pop() ||
								(Boolean)cb.execute(frame.getSubFrame(cb, null)));
					break;
				case IFTE:
					frame.setPc(pc);
					if ((Boolean)frame.pop()) {
						final CodeBlock thenCb = ((Ifte)instr).getThenCb();
						frame.push(thenCb.execute(frame.getSubFrame(thenCb, null)));
					} else {
						final CodeBlock elseCb = ((Ifte)instr).getElseCb();
						frame.push(elseCb.execute(frame.getSubFrame(elseCb, null)));
					}
					break;
				case RETURN:
					break LOOP;
				case GETCB:
					frame.push(((Getcb)instr).getCodeBlock());
					break;
				case INVOKE_ALL_CBS:
					Object[] args = getArguments(((InvokeAllCbs)instr).getArgcount(), frame);
					frame.setPc(pc);
					for (CodeBlock ncb : getNested()) {
						frame.push(ncb.execute(frame.getSubFrame(ncb, args)));
					}
					break;
				case INVOKE_CB:
					cb = ((InvokeCb)instr).getCodeBlock();
					frame.setPc(pc);
					frame.push(cb.execute(frame.getSubFrame(cb, getArguments(((InvokeCb)instr).getArgcount(), frame))));
					break;
				case INVOKE_CB_S: 
					cb = (CodeBlock)frame.pop();
					frame.setPc(pc);
					frame.push(cb.execute(frame.getSubFrame(cb, getArguments(((InvokeCbS)instr).getArgcount(), frame))));
					break;
				case MATCH:
					frame.setPc(pc);
					frame.push(matchOne(frame, findRule(frame.getEnv(), (Match)instr), ((Match)instr).getArgcount()));
					break;
				case MATCH_S: 
					frame.setPc(pc);
					frame.push(matchOne(frame, (Rule)frame.pop(), ((MatchS)instr).getArgcount()));
					break;
				case ADD:
					add(frame.pop(), frame.pop(), ((Add)instr).getFieldname(), 
							frame.getEnv(), -1);
					break;
				case REMOVE:
					remove(((Remove)instr).getFieldname(), frame);
					break;
				case INSERT:
					add(frame.pop(), frame.pop(), ((Insert)instr).getFieldname(), 
							frame.getEnv(), (Integer)frame.pop());
					break;
				default:
					throw new VMException(frame, String.format("Unsupported opcode: %s", instr.getOpcode()));
				} // switch
			} // while
		} catch (VMException e) {
			throw e;
		} catch (Exception e) {
			frame.setPc(pc);
			throw new VMException(frame, e);
		}

		if (monitor != null) {
			monitor.leave(frame);
		}

		if (frame.stackEmpty()) {
			return null;
		} else {
			return frame.pop();
		}
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public int getStackLevel() {
		final EList<Instruction> code = getCode();
		if (code.isEmpty()) {
			return 0;
		}
		return code.get(code.size() - 1).getStackLevel();
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public Module getModule() {
		final EObject container = eContainer();
		if (container != null) {
			switch (container.eClass().getClassifierID()) {
			case EmftvmPackage.FEATURE:
			case EmftvmPackage.FIELD:
			case EmftvmPackage.OPERATION:
				return ((Feature)container).getModule();
			case EmftvmPackage.RULE:
				return ((Rule)container).getModule();
			case EmftvmPackage.INPUT_RULE_ELEMENT:
				return ((InputRuleElement)container).getInputFor().getModule();
			case EmftvmPackage.CODE_BLOCK:
				return ((CodeBlock)container).getModule();
			default:
				break;
			}
		}
		return null;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public Operation getOperation() {
		final EObject container = eContainer();
		if (container != null) {
			switch (container.eClass().getClassifierID()) {
			case EmftvmPackage.OPERATION:
				return (Operation)container;
			case EmftvmPackage.CODE_BLOCK:
				return ((CodeBlock)container).getOperation();
			default:
				break;
			}
		}
		return null;
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case EmftvmPackage.CODE_BLOCK__CODE:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getCode()).basicAdd(otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__LINE_NUMBERS:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getLineNumbers()).basicAdd(otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__LOCAL_VARIABLES:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getLocalVariables()).basicAdd(otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__MATCHER_FOR:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetMatcherFor((Rule)otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__APPLIER_FOR:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetApplierFor((Rule)otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__POST_APPLY_FOR:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetPostApplyFor((Rule)otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__BODY_FOR:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetBodyFor((Operation)otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__INITIALISER_FOR:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetInitialiserFor((Field)otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__NESTED:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getNested()).basicAdd(otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__NESTED_FOR:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetNestedFor((CodeBlock)otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__BINDING_FOR:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetBindingFor((InputRuleElement)otherEnd, msgs);
		}
		return super.eInverseAdd(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case EmftvmPackage.CODE_BLOCK__CODE:
				return ((InternalEList<?>)getCode()).basicRemove(otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__LINE_NUMBERS:
				return ((InternalEList<?>)getLineNumbers()).basicRemove(otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__LOCAL_VARIABLES:
				return ((InternalEList<?>)getLocalVariables()).basicRemove(otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__MATCHER_FOR:
				return basicSetMatcherFor(null, msgs);
			case EmftvmPackage.CODE_BLOCK__APPLIER_FOR:
				return basicSetApplierFor(null, msgs);
			case EmftvmPackage.CODE_BLOCK__POST_APPLY_FOR:
				return basicSetPostApplyFor(null, msgs);
			case EmftvmPackage.CODE_BLOCK__BODY_FOR:
				return basicSetBodyFor(null, msgs);
			case EmftvmPackage.CODE_BLOCK__INITIALISER_FOR:
				return basicSetInitialiserFor(null, msgs);
			case EmftvmPackage.CODE_BLOCK__NESTED:
				return ((InternalEList<?>)getNested()).basicRemove(otherEnd, msgs);
			case EmftvmPackage.CODE_BLOCK__NESTED_FOR:
				return basicSetNestedFor(null, msgs);
			case EmftvmPackage.CODE_BLOCK__BINDING_FOR:
				return basicSetBindingFor(null, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eBasicRemoveFromContainerFeature(NotificationChain msgs) {
		switch (eContainerFeatureID()) {
			case EmftvmPackage.CODE_BLOCK__MATCHER_FOR:
				return eInternalContainer().eInverseRemove(this, EmftvmPackage.RULE__MATCHER, Rule.class, msgs);
			case EmftvmPackage.CODE_BLOCK__APPLIER_FOR:
				return eInternalContainer().eInverseRemove(this, EmftvmPackage.RULE__APPLIER, Rule.class, msgs);
			case EmftvmPackage.CODE_BLOCK__POST_APPLY_FOR:
				return eInternalContainer().eInverseRemove(this, EmftvmPackage.RULE__POST_APPLY, Rule.class, msgs);
			case EmftvmPackage.CODE_BLOCK__BODY_FOR:
				return eInternalContainer().eInverseRemove(this, EmftvmPackage.OPERATION__BODY, Operation.class, msgs);
			case EmftvmPackage.CODE_BLOCK__INITIALISER_FOR:
				return eInternalContainer().eInverseRemove(this, EmftvmPackage.FIELD__INITIALISER, Field.class, msgs);
			case EmftvmPackage.CODE_BLOCK__NESTED_FOR:
				return eInternalContainer().eInverseRemove(this, EmftvmPackage.CODE_BLOCK__NESTED, CodeBlock.class, msgs);
			case EmftvmPackage.CODE_BLOCK__BINDING_FOR:
				return eInternalContainer().eInverseRemove(this, EmftvmPackage.INPUT_RULE_ELEMENT__BINDING, InputRuleElement.class, msgs);
		}
		return super.eBasicRemoveFromContainerFeature(msgs);
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case EmftvmPackage.CODE_BLOCK__MAX_LOCALS:
				return getMaxLocals();
			case EmftvmPackage.CODE_BLOCK__MAX_STACK:
				return getMaxStack();
			case EmftvmPackage.CODE_BLOCK__CODE:
				return getCode();
			case EmftvmPackage.CODE_BLOCK__LINE_NUMBERS:
				return getLineNumbers();
			case EmftvmPackage.CODE_BLOCK__LOCAL_VARIABLES:
				return getLocalVariables();
			case EmftvmPackage.CODE_BLOCK__MATCHER_FOR:
				return getMatcherFor();
			case EmftvmPackage.CODE_BLOCK__APPLIER_FOR:
				return getApplierFor();
			case EmftvmPackage.CODE_BLOCK__POST_APPLY_FOR:
				return getPostApplyFor();
			case EmftvmPackage.CODE_BLOCK__BODY_FOR:
				return getBodyFor();
			case EmftvmPackage.CODE_BLOCK__INITIALISER_FOR:
				return getInitialiserFor();
			case EmftvmPackage.CODE_BLOCK__NESTED:
				return getNested();
			case EmftvmPackage.CODE_BLOCK__NESTED_FOR:
				return getNestedFor();
			case EmftvmPackage.CODE_BLOCK__PARENT_FRAME:
				return getParentFrame();
			case EmftvmPackage.CODE_BLOCK__BINDING_FOR:
				return getBindingFor();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case EmftvmPackage.CODE_BLOCK__MAX_LOCALS:
				setMaxLocals((Integer)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__MAX_STACK:
				setMaxStack((Integer)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__CODE:
				getCode().clear();
				getCode().addAll((Collection<? extends Instruction>)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__LINE_NUMBERS:
				getLineNumbers().clear();
				getLineNumbers().addAll((Collection<? extends LineNumber>)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__LOCAL_VARIABLES:
				getLocalVariables().clear();
				getLocalVariables().addAll((Collection<? extends LocalVariable>)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__MATCHER_FOR:
				setMatcherFor((Rule)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__APPLIER_FOR:
				setApplierFor((Rule)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__POST_APPLY_FOR:
				setPostApplyFor((Rule)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__BODY_FOR:
				setBodyFor((Operation)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__INITIALISER_FOR:
				setInitialiserFor((Field)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__NESTED:
				getNested().clear();
				getNested().addAll((Collection<? extends CodeBlock>)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__NESTED_FOR:
				setNestedFor((CodeBlock)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__PARENT_FRAME:
				setParentFrame((StackFrame)newValue);
				return;
			case EmftvmPackage.CODE_BLOCK__BINDING_FOR:
				setBindingFor((InputRuleElement)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case EmftvmPackage.CODE_BLOCK__MAX_LOCALS:
				setMaxLocals(MAX_LOCALS_EDEFAULT);
				return;
			case EmftvmPackage.CODE_BLOCK__MAX_STACK:
				setMaxStack(MAX_STACK_EDEFAULT);
				return;
			case EmftvmPackage.CODE_BLOCK__CODE:
				getCode().clear();
				return;
			case EmftvmPackage.CODE_BLOCK__LINE_NUMBERS:
				getLineNumbers().clear();
				return;
			case EmftvmPackage.CODE_BLOCK__LOCAL_VARIABLES:
				getLocalVariables().clear();
				return;
			case EmftvmPackage.CODE_BLOCK__MATCHER_FOR:
				setMatcherFor((Rule)null);
				return;
			case EmftvmPackage.CODE_BLOCK__APPLIER_FOR:
				setApplierFor((Rule)null);
				return;
			case EmftvmPackage.CODE_BLOCK__POST_APPLY_FOR:
				setPostApplyFor((Rule)null);
				return;
			case EmftvmPackage.CODE_BLOCK__BODY_FOR:
				setBodyFor((Operation)null);
				return;
			case EmftvmPackage.CODE_BLOCK__INITIALISER_FOR:
				setInitialiserFor((Field)null);
				return;
			case EmftvmPackage.CODE_BLOCK__NESTED:
				getNested().clear();
				return;
			case EmftvmPackage.CODE_BLOCK__NESTED_FOR:
				setNestedFor((CodeBlock)null);
				return;
			case EmftvmPackage.CODE_BLOCK__PARENT_FRAME:
				setParentFrame(PARENT_FRAME_EDEFAULT);
				return;
			case EmftvmPackage.CODE_BLOCK__BINDING_FOR:
				setBindingFor((InputRuleElement)null);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case EmftvmPackage.CODE_BLOCK__MAX_LOCALS:
				return getMaxLocals() != MAX_LOCALS_EDEFAULT;
			case EmftvmPackage.CODE_BLOCK__MAX_STACK:
				return getMaxStack() != MAX_STACK_EDEFAULT;
			case EmftvmPackage.CODE_BLOCK__CODE:
				return code != null && !code.isEmpty();
			case EmftvmPackage.CODE_BLOCK__LINE_NUMBERS:
				return lineNumbers != null && !lineNumbers.isEmpty();
			case EmftvmPackage.CODE_BLOCK__LOCAL_VARIABLES:
				return localVariables != null && !localVariables.isEmpty();
			case EmftvmPackage.CODE_BLOCK__MATCHER_FOR:
				return getMatcherFor() != null;
			case EmftvmPackage.CODE_BLOCK__APPLIER_FOR:
				return getApplierFor() != null;
			case EmftvmPackage.CODE_BLOCK__POST_APPLY_FOR:
				return getPostApplyFor() != null;
			case EmftvmPackage.CODE_BLOCK__BODY_FOR:
				return getBodyFor() != null;
			case EmftvmPackage.CODE_BLOCK__INITIALISER_FOR:
				return getInitialiserFor() != null;
			case EmftvmPackage.CODE_BLOCK__NESTED:
				return nested != null && !nested.isEmpty();
			case EmftvmPackage.CODE_BLOCK__NESTED_FOR:
				return getNestedFor() != null;
			case EmftvmPackage.CODE_BLOCK__PARENT_FRAME:
				return PARENT_FRAME_EDEFAULT == null ? parentFrame != null : !PARENT_FRAME_EDEFAULT.equals(parentFrame);
			case EmftvmPackage.CODE_BLOCK__BINDING_FOR:
				return getBindingFor() != null;
		}
		return super.eIsSet(featureID);
	}

	/**
	 * Finds the the {@link Rule} that contains this codeblock.
	 * @return the {@link Rule} that contains this codeblock, or <code>null</code>
	 * if not contained by a {@link Rule}.
	 */
	private Rule getRule() {
		if (!ruleSet) {
			CodeBlock cb = this;
			while (cb != null) {
				if (cb.eContainer() instanceof Rule) {
					rule = (Rule)cb.eContainer();
					break;
				} else {
					cb = cb.getNestedFor();
				}
			}
			ruleSet = true;
		}
		return rule;
	}

	/**
	 * @param env
	 * @param type
	 * @param name
	 * @return The {@link Field} with the given <code>type</code> and <code>name</code>, if any, otherwise <code>null</code>
	 */
	private Field findField(final ExecEnv env, Object type, String name) {
		final Rule rule = getRule();
		final Field field;
		if (rule != null) {
			field = rule.findField(type, name);
		} else {
			field = null;
		}
		if (field == null) {
			return env.findField(type, name);
		} else {
			return field;
		}
	}

	/**
	 * @param env
	 * @param type
	 * @param name
	 * @return The static {@link Field} with the given <code>type</code> and <code>name</code>, if any, otherwise <code>null</code>
	 */
	private Field findStaticField(final ExecEnv env, Object type, String name) {
		final Rule rule = getRule();
		final Field field;
		if (rule != null) {
			field = rule.findStaticField(type, name);
		} else {
			field = null;
		}
		if (field == null) {
			return env.findStaticField(type, name);
		} else {
			return field;
		}
	}

	/**
	 * Implements the SET instruction.
	 * @param propname
	 * @param env
	 * @param frame
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	private void set(final String propname, final StackFrame frame) 
	throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		final ExecEnv env = frame.getEnv();
		final Object o = frame.pop();
		final Object v = frame.pop();

		if (o instanceof EObject) {
			final EObject eo = (EObject)o;
			final EClass type = eo.eClass();
			final Field field = findField(env, type, propname);
			if (field != null) {
				field.setValue(o, v);
				return;
			}
			final EStructuralFeature sf = type.getEStructuralFeature(propname);
			if (sf != null) {
				EMFTVMUtil.set(env, eo, sf, v);
				return;
			}
			final Resource resource = eo.eResource();
			if (EMFTVMUtil.XMI_ID_FEATURE.equals(propname) && resource instanceof XMIResource) { //$NON-NLS-1$
				((XMIResource)resource).setID(eo, v.toString());
				return;
			}
			throw new NoSuchFieldException(String.format("Field %s::%s not found", 
					EMFTVMUtil.toPrettyString(type, env), propname));
		}

		// o is a regular Java object
		final Class<?> type = o == null ? Void.TYPE : o.getClass();
		final Field field = findField(env, type, propname);
		if (field != null) {
			field.setValue(o, v);
			return;
		}
		try {
			final java.lang.reflect.Field f = type.getField(propname);
			f.set(o, v);
		} catch (NoSuchFieldException e) {
			throw new NoSuchFieldException(String.format("Field %s::%s not found", 
					EMFTVMUtil.toPrettyString(type, env), propname));
		}
	}

	/**
	 * Adds <code>v</code> to <code>o.propname</code>.
	 * Implements the ADD and INSERT instructions.
	 * @param o
	 * @param v
	 * @param propname
	 * @param env
	 * @param index the insertion index (-1 for end)
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	private void add(final Object o, final Object v, final String propname, 
			final ExecEnv env, final int index) 
	throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		//TODO enable add on fields
		if (o instanceof EObject) {
			final EObject eo = (EObject)o;
			final EClass type = eo.eClass();
			final EStructuralFeature sf = type.getEStructuralFeature(propname);
			if (sf != null) {
				EMFTVMUtil.add(env, eo, sf, v, index);
				return;
			}
			final Resource resource = eo.eResource();
			if (EMFTVMUtil.XMI_ID_FEATURE.equals(propname) && resource instanceof XMIResource) { //$NON-NLS-1$
				if (((XMIResource)resource).getID(eo) != null) {
					throw new IllegalArgumentException(String.format(
							"Cannot add %s to field %s::%s: maximum multiplicity of 1 reached", 
							v, EMFTVMUtil.toPrettyString(eo, env), propname));
				}
				if (index > 0) {
					throw new IndexOutOfBoundsException(String.valueOf(index));
				}
				((XMIResource)resource).setID(eo, v.toString());
				return;
			}
			throw new NoSuchFieldException(String.format("Field %s::%s not found", 
					EMFTVMUtil.toPrettyString(type, env), propname));
		}
	}

	/**
	 * Implements the REMOVE instruction.
	 * @param propname
	 * @param env
	 * @param frame
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	private void remove(final String propname, final StackFrame frame) 
	throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		final ExecEnv env = frame.getEnv();
		final Object o = frame.pop();
		final Object v = frame.pop();

		//TODO enable remove on fields
		if (o instanceof EObject) {
			final EObject eo = (EObject)o;
			final EClass type = eo.eClass();
			final EStructuralFeature sf = type.getEStructuralFeature(propname);
			if (sf != null) {
				EMFTVMUtil.remove(env, eo, sf, v);
				return;
			}
			final Resource resource = eo.eResource();
			if (EMFTVMUtil.XMI_ID_FEATURE.equals(propname) && resource instanceof XMIResource) { //$NON-NLS-1$
				final XMIResource xmiRes = (XMIResource)resource;
				final Object xmiID = xmiRes.getID(eo);
				if (xmiID == null ? v == null : xmiID.equals(v)) {
					xmiRes.setID(eo, null);
				}
				return;
			}
			throw new NoSuchFieldException(String.format("Field %s::%s not found", 
					EMFTVMUtil.toPrettyString(type, env), propname));
		}
	}

	/**
	 * Implements the GET instruction.
	 * @param propname
	 * @param env
	 * @param frame
	 * @return the property value
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	@SuppressWarnings("unchecked")
	private Object get(final String propname, final StackFrame frame) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		final ExecEnv env = frame.getEnv();
		Object o = frame.pop();

		if (o instanceof EObject) {
			final EObject eo = (EObject)o;
			final EClass type = eo.eClass();
			final Field field = findField(env, type, propname);
			if (field != null) {
				return field.getValue(o, frame);
			}
			final EStructuralFeature sf = type.getEStructuralFeature(propname);
			if (sf != null) {
				return EMFTVMUtil.get(env, eo, sf);
			}
			final Resource resource = eo.eResource();
			if (EMFTVMUtil.XMI_ID_FEATURE.equals(propname) && resource instanceof XMIResource) { //$NON-NLS-1$
				return ((XMIResource)resource).getID(eo);
			}
			throw new NoSuchFieldException(String.format("Field %s::%s not found", 
					EMFTVMUtil.toPrettyString(type, env), propname));
		}

		// o is a regular Java object
		final Class<?> type = o == null ? Void.TYPE : o.getClass();
		final Field field = findField(env, type, propname);
		if (field != null) {
			return field.getValue(o, frame);
		}
		try {
			final java.lang.reflect.Field f = type.getField(propname);
			final Object result = f.get(o);
			if (result instanceof List<?>) {
				return new LazyListOnList<Object>((List<Object>)result);
			} else if (result instanceof java.util.Set<?>) {
				return new LazySetOnSet<Object>((java.util.Set<Object>)result);
			} else if (result instanceof Collection<?>) {
				return new LazyBagOnCollection<Object>((Collection<Object>)result);
			} else {
				return result;
			}
		} catch (NoSuchFieldException e) {
			throw new NoSuchFieldException(String.format("Field %s::%s not found", 
					EMFTVMUtil.toPrettyString(type, env), propname));
		}
	}

	/**
	 * Implements the GET_TRANS instruction.
	 * @param propname
	 * @param env
	 * @param frame
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	private Collection<Object> getTrans(final String propname, final StackFrame frame) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		final ExecEnv env = frame.getEnv();
		final Object o = frame.pop();

		if (o instanceof EObject) {
			final EObject eo = (EObject)o;
			final EClass type = eo.eClass();
			final Field field = findField(env, type, propname);
			if (field != null) {
				return getTrans(o, field, frame, new LazyList<Object>());
			} else {
				final EStructuralFeature sf = type.getEStructuralFeature(propname);
				if (sf == null) {
					throw new NoSuchFieldException(String.format("Field %s::%s not found", 
							EMFTVMUtil.toPrettyString(type, env), propname));
				}
				return getTrans(eo, sf, env, new LazyList<Object>());
			}
		} else {
			final Class<?> type = o.getClass();
			final Field field = findField(env, type, propname);
			if (field != null) {
				return getTrans(o, field, frame, new LazyList<Object>());
			} else {
				final java.lang.reflect.Field f = type.getField(propname);
				return getTrans(o, f, new LazyList<Object>());
			}
		}
	}

	/**
	 * Retrieves the transitive closure of <pre>field</pre> on <pre>object</pre>.
	 * @param object the object on which to retrieve <pre>field</pre>
	 * @param field the field for which to retrieve the value
	 * @param frame the current {@link StackFrame}
	 * @param result the intermediate list of values
	 * @return the updated result
	 */
	@SuppressWarnings("unchecked")
	private static LazyList<Object> getTrans(final Object object, final Field field, 
			final StackFrame frame, final LazyList<Object> result) {
		LazyList<Object> newResult = result;
		final Object value = field.getValue(object, frame);
		if (value instanceof List<?>) {
			final List<Object> cvalue = (List<Object>)value;
			newResult = newResult.union(new LazyListOnList<Object>(cvalue));
			for (Object v : cvalue) {
				newResult = getTrans(v, field, frame, newResult);
			}
		} else if (value instanceof Collection<?>) {
			final Collection<Object> cvalue = (Collection<Object>)value;
			newResult = newResult.union(new LazyListOnCollection<Object>(cvalue));
			for (Object v : cvalue) {
				newResult = getTrans(v, field, frame, newResult);
			}
		} else if (value != null) {
			newResult = newResult.append(value);
			newResult = getTrans(value, field, frame, newResult);
		}
		return newResult;
	}

	/**
	 * Retrieves the transitive closure of <pre>sf</pre> on <pre>object</pre>.
	 * @param object the object on which to retrieve <pre>sf</pre>
	 * @param sf the structural feature for which to retrieve the value
	 * @param env the current {@link ExecEnv}
	 * @param result the intermediate list of values
	 * @return the updated result
	 */
	@SuppressWarnings("unchecked")
	private static LazyList<Object> getTrans(final EObject object, 
			final EStructuralFeature sf, final ExecEnv env, 
			final LazyList<Object> result) {
		if (!sf.getEContainingClass().isSuperTypeOf(object.eClass())) {
			return result; // feature does not apply to object
		}
		LazyList<Object> newResult = result;
		final Object value = EMFTVMUtil.get(env, object, sf);
		if (value instanceof LazyList<?>) {
			final LazyList<Object> cvalue = (LazyList<Object>)value;
			newResult = newResult.union(cvalue);
			for (Object v : cvalue) {
				if (v instanceof EObject) {
					newResult = getTrans((EObject)v, sf, env, newResult);
				}
			}
		} else if (value != null) {
			assert !(value instanceof Collection<?>); // All collections should be LazyLists
			if (value instanceof Enumerator) {
				newResult = newResult.append(new EnumLiteral(value.toString()));
			} else {
				newResult = newResult.append(value);
				if (value instanceof EObject) {
					newResult = getTrans((EObject)value, sf, env, newResult);
				}
			}
		}
		return newResult;
	}

	/**
	 * Retrieves the transitive closure of <pre>field</pre> on <pre>object</pre>.
	 * @param object the object on which to retrieve <pre>field</pre>
	 * @param field the field for which to retrieve the value
	 * @param result the intermediate list of values
	 * @return the updated result
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	@SuppressWarnings("unchecked")
	private static LazyList<Object> getTrans(final Object object, 
			final java.lang.reflect.Field field, 
			final LazyList<Object> result) throws IllegalArgumentException, IllegalAccessException {
		if (!field.getDeclaringClass().isAssignableFrom(object.getClass())) {
			return result; // field does not apply to object
		}
		LazyList<Object> newResult = result;
		final Object value = field.get(object);
		if (value instanceof LazyList<?>) {
			final LazyList<Object> cvalue = (LazyList<Object>)value;
			newResult = newResult.union(cvalue);
			for (Object v : cvalue) {
				newResult = getTrans(v, field, newResult);
			}
		} else if (value instanceof List<?>) {
			final List<Object> cvalue = (List<Object>)value;
			newResult = newResult.union(new LazyListOnList<Object>(cvalue));
			for (Object v : cvalue) {
				newResult = getTrans(v, field, newResult);
			}
		} else if (value instanceof Collection<?>) {
			final Collection<Object> cvalue = (Collection<Object>)value;
			newResult = newResult.union(new LazyListOnCollection<Object>(cvalue));
			for (Object v : cvalue) {
				newResult = getTrans(v, field, newResult);
			}
		} else if (value != null) {
			newResult = newResult.append(value);
			newResult = getTrans(value, field, newResult);
		}
		return newResult;
	}

	/**
	 * Implements the SET_STATIC instruction.
	 * @param propname
	 * @param frame
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	private void setStatic(final String propname, final StackFrame frame) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		final ExecEnv env = frame.getEnv();
		final Object o = EMFTVMUtil.getRegistryType(frame.pop());
		final Object v = frame.pop();

		if (o instanceof EClass) {
			final EClass type = (EClass)o;
			final Field field = findStaticField(env, type, propname);
			if (field != null) {
				field.setStaticValue(v);
			} else {
				throw new NoSuchFieldException(String.format("Field %s::%s not found", 
						EMFTVMUtil.toPrettyString(type, env), propname));
			}
		} else if (o instanceof Class<?>) {
			final Class<?> type = (Class<?>)o;
			final Field field = findStaticField(env, type, propname);
			if (field != null) {
				field.setValue(o, v);	
			} else {
				final java.lang.reflect.Field f = type.getField(propname);
				if (Modifier.isStatic(f.getModifiers())) {
					f.set(null, v);
				} else {
					throw new NoSuchFieldException(String.format("Field %s::%s not found", 
							EMFTVMUtil.toPrettyString(type, env), propname));
				}
			}
		} else {
			throw new IllegalArgumentException(String.format("%s is not a type", 
					EMFTVMUtil.toPrettyString(o, env)));
		}
	}

	/**
	 * Implements the GET_STATIC instruction.
	 * @param propname
	 * @param frame
	 * @return the property value
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	private Object getStatic(final String propname, final StackFrame frame) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		final ExecEnv env = frame.getEnv();
		final Object o = EMFTVMUtil.getRegistryType(frame.pop());

		if (o instanceof EClass) {
			final EClass type = (EClass)o;
			final Field field = findStaticField(env, type, propname);
			if (field != null) {
				return field.getStaticValue(frame);
			} else {
				throw new NoSuchFieldException(String.format("Field %s::%s not found", 
						EMFTVMUtil.toPrettyString(type, env), propname));
			}
		} else if (o instanceof Class<?>) {
			final Class<?> type = (Class<?>)o;
			final Field field = findStaticField(env, type, propname);
			if (field != null) {
				return field.getStaticValue(frame);
			} else {
				final java.lang.reflect.Field f = type.getField(propname);
				if (Modifier.isStatic(f.getModifiers())) {
					return f.get(null);
				} else {
					throw new NoSuchFieldException(String.format("Field %s::%s not found", 
							EMFTVMUtil.toPrettyString(type, env), propname));
				}
			}
		} else {
			throw new IllegalArgumentException(String.format("%s is not a type", o));
		}
	}

	/**
	 * Implements the NEW and NEW_S instructions.
	 * @param modelname
	 * @param type
	 * @param fram
	 * @return the new object
	 */
	private static Object newInstr(final String modelname, final Object type, final StackFrame frame) {
		final ExecEnv env = frame.getEnv();
		if (type instanceof EClass) {
			final EClass eType = (EClass)type;
			Model model = env.getOutputModels().get(modelname);
			if (model == null) {
				model = env.getInoutModels().get(modelname);
			}
			if (model == null) {
				throw new IllegalArgumentException(String.format("Inout/output model %s not found", modelname));
			}
			return model.newElement(eType);
		} else {
			try {
				return NativeTypes.newInstance((Class<?>)type);
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	/**
	 * Implements the DELETE instruction.
	 * @param frame
	 */
	private static void delete(final StackFrame frame) {
		final ExecEnv env = frame.getEnv();
		final EObject element = (EObject)frame.pop();

		final Resource res = element.eResource();
		if (res == null) {
			throw new IllegalArgumentException(String.format(
					"Element %s is cannot be deleted; not contained in a model", 
					EMFTVMUtil.toPrettyString(element, env)));
		}
		final Model model = env.getInputModelOf(element);
		if (model != null) {
			throw new IllegalArgumentException(String.format(
					"Element %s is cannot be deleted; contained in input model %s", 
					EMFTVMUtil.toPrettyString(element, env), env.getModelID(model)));
		}
		env.getDeletionQueue().add(element);
	}

	/**
	 * Implements the INVOKE instruction.
	 * @param opname
	 * @param argcount
	 * @param frame
	 * @return the invocation result
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	private static Object invoke(final String opname, final int argcount, 
			final StackFrame frame) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		final ExecEnv env = frame.getEnv();
		final Object o = frame.pop();

		final Object type = EMFTVMUtil.getArgumentType(o);
		final Object[] args = getArguments(argcount, frame);
		final EList<Object> argTypes = EMFTVMUtil.getArgumentTypes(args);
		final Operation op = env.findOperation(type, opname, argTypes);
		if (op != null) {
			final CodeBlock body = op.getBody();
			return body.execute(frame.getSubFrame(body, o, args));
		}
		return EMFTVMUtil.invokeNative(frame, o, opname, args);
	}

	/**
	 * Implements the INVOKE_STATIC instruction.
	 * @param opname
	 * @param argcount
	 * @param frame
	 * @return the invocation result
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	private static Object invokeStatic(final String opname, final int argcount, 
			final StackFrame frame) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		final ExecEnv env = frame.getEnv();
		final Object type = frame.pop();

		if (type == null) {
			throw new IllegalArgumentException(String.format("Cannot invoke static operation %s on null type", opname));
		}

		if (type == env.eClass()) { // Lazy and called rule invocations are indistinguishable from static operations in ATL
			final Rule rule = env.findRule(opname);
			if (rule != null) {
				return matchOne(frame, rule, argcount);
			}
		}

		final Object[] args = getArguments(argcount, frame);
		final EList<Object> argTypes = EMFTVMUtil.getArgumentTypes(args);
		final Operation op = env.findStaticOperation(type, opname, argTypes);
		if (op != null) {
			final CodeBlock body = op.getBody();
			return body.execute(frame.getSubFrame(body, args));
		}
		if (type instanceof Class<?>) {
			return EMFTVMUtil.invokeNativeStatic(frame, (Class<?>)type, opname, args);
		}
		throw new UnsupportedOperationException(String.format("static %s::%s(%s)", 
				EMFTVMUtil.getTypeName(env, type), opname, EMFTVMUtil.getTypeNames(env, argTypes)));
	}

	/**
	 * Implements the INVOKE_SUPER instruction.
	 * @param context the current execution context type
	 * @param opname
	 * @param argcount
	 * @param frame
	 * @return the invocation result
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	private static Object invokeSuper(final Operation op, final String opname, final int argcount, 
			final StackFrame frame) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (op == null) {
			throw new IllegalArgumentException("INVOKE_SUPER can only be used in operations");
		}
		final EClassifier context = op.getEContext();
		if (context == null) {
			throw new IllegalArgumentException(String.format("Operation misses context type: %s", op));
		}

		final ExecEnv env = frame.getEnv();
		final Object o = frame.pop();

		final java.util.Set<Operation> ops = new LinkedHashSet<Operation>();
		final List<?> superTypes;
		if (context instanceof EClass) {
			superTypes = ((EClass)context).getESuperTypes();
		} else {
			final Class<?> ic = context.getInstanceClass();
			if (ic == null) {
				throw new IllegalArgumentException(String.format("Primitive EMF type without instance class %s", context));
			}
			superTypes = Collections.singletonList(ic.getSuperclass());
		}

		final Object[] args = getArguments(argcount, frame);
		final EList<Object> argTypes = EMFTVMUtil.getArgumentTypes(args);
		Operation superOp = null;
		for (Object superType : superTypes) {
			superOp = env.findOperation(superType, opname, argTypes);
			if (superOp != null) {
				ops.add(superOp);
			}
		}
		if (ops.size() > 1) {
			throw new DuplicateEntryException(String.format(
					"More than one super-operation found for context %s: %s",
					context, ops));
		}
		if (!ops.isEmpty()) {
			superOp = ops.iterator().next();
		}

		if (superOp != null) {
			final CodeBlock body = superOp.getBody();
			return body.execute(frame.getSubFrame(body, o, args));
		}

		final Class<?> ic = context.getInstanceClass();
		if (ic != null) {
			return EMFTVMUtil.invokeNativeSuper(frame, ic, o, opname, args);
		}

		throw new UnsupportedOperationException(String.format("super %s::%s(%s)", 
				EMFTVMUtil.getTypeName(env, context), opname, EMFTVMUtil.getTypeNames(env, argTypes)));
	}

	/**
	 * Gets argcount objects off the stack and returns them.
	 * @param argcount
	 * @param frame
	 * @return the arguments
	 */
	private static Object[] getArguments(final int argcount, final StackFrame frame) {
		final Object[] args = new Object[argcount];
		for (int i = 0; i < argcount; i++) {
			// Do not unwrap lazy values
			args[i] = frame.pop();
		}
		return args;
	}

	/**
	 * Finds the rule referred to by <pre>instr</pre>.
	 * @param env
	 * @param instr
	 * @return the rule mentioned by instr
	 * @throws IllegalArgumentException if rule not found
	 */
	private static Rule findRule(final ExecEnv env, final Match instr) {
		final String rulename = instr.getRulename();
		final Rule rule = env.findRule(rulename);
		if (rule == null) {
			throw new IllegalArgumentException(String.format("Rule %s not found", rulename));
		}
		return rule;
	}

	/**
	 * Executes rule with parameters derived from <code>rule</code>.
	 * @param frame
	 * @param rule
	 * @param argcount
	 */
	private static Object matchOne(final StackFrame frame, final Rule rule, final int argcount) {
		if (argcount != rule.getInputElements().size()) {
			throw new VMException(frame, String.format(
					"Rule %s has different amount of input elements than expected: %d instead of %d",
					rule.getName(), rule.getInputElements().size(), argcount));
		}
		final EObject[] elements = new EObject[argcount];
		for (int i = 0; i < argcount; i++) {
			elements[i] = (EObject)frame.pop();
		}
		return Matcher.matchOne(frame, rule, elements);
	}

	/**
	 * <!-- begin-user-doc. -->
	 * {@inheritDoc}
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer();
		final EObject container = eContainer();
		if (container != null) {
			result.append(container);
			if (container instanceof CodeBlock) {
				result.append('@');
				result.append(((CodeBlock)container).getNested().indexOf(this));
			} else if (container instanceof Field) {
				// nothing
			} else if (container instanceof Operation) {
				// nothing
			} else if (container instanceof InputRuleElement) {
				result.append('@');
				result.append(((InputRuleElement)container).getInputFor());
			} else if (container instanceof Rule) {
				final Rule r = (Rule)container;
				if (r.getMatcher() == this) {
					result.append("@matcher");
				} else if (r.getApplier() == this) {
					result.append("@applier");
				} else if (r.getPostApply() == this) {
					result.append("@postApply");
				} else {
					result.append("@unknown");
				}
			} else {
				result.append("@unknown");
			}
		} else {
			result.append("@uncontained");
		}
		return result.toString();
	}

	/**
	 * Returns the {@link Module} (for debugger).
	 * @return the {@link Module}
	 * @see CodeBlockImpl#getModule()
	 */
	public Module getASM() {
		return getModule();
	}

} //CodeBlockImpl
