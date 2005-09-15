package org.atl.engine.vm;

import org.atl.engine.vm.nativelib.ASMOclAny;

import java.util.List;

/**
 * @author Fr�d�ric Jouault
 */
public interface Operation {

	public String getName();

	public String getContext();

	public List getParameters();

	public String getSignature();

	public ASMOclAny exec(StackFrame frame);
}

