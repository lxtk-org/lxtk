/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.lxtk.lx4e.ui.callhierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.ui.callhierarchy.CallHierarchyKind;
import org.eclipse.handly.ui.callhierarchy.CallHierarchyNode;
import org.eclipse.handly.ui.callhierarchy.CallLocation;
import org.eclipse.handly.ui.callhierarchy.CallTextInfo;
import org.eclipse.handly.ui.callhierarchy.ICallHierarchyNode;
import org.eclipse.lsp4j.CallHierarchyIncomingCall;
import org.lxtk.DocumentUri;

/**
 * Represents an incoming node of LXTK-based call hierarchy.
 */
public final class IncomingCallHierarchyNode
    extends CallHierarchyNode
{
    private static final IncomingCallHierarchyNode[] EMPTY_ARRAY = new IncomingCallHierarchyNode[0];

    /**
     * Returns a new {@link IncomingCallHierarchyNode} for the given call hierarchy element;
     * the node will have no parent.
     *
     * @param element not <code>null</code>
     * @return the created root node (never <code>null</code>)
     */
    public static IncomingCallHierarchyNode newRootNode(CallHierarchyElement element)
    {
        return new IncomingCallHierarchyNode(null, element);
    }

    private IncomingCallHierarchyNode(IncomingCallHierarchyNode parent,
        CallHierarchyElement element)
    {
        super(parent, element);
    }

    @Override
    public CallHierarchyKind getKind()
    {
        return CallHierarchyKind.CALLER;
    }

    @Override
    protected ICallHierarchyNode[] computeChildren(IProgressMonitor monitor)
    {
        CallHierarchyElement callee = (CallHierarchyElement)getElement();
        CallHierarchyUtility utility = callee.getCallHierarchyUtility();

        List<CallHierarchyIncomingCall> calls =
            utility.getIncomingCalls(callee.getCallHierarchyItem(), monitor);
        if (calls == null || calls.isEmpty())
            return EMPTY_ARRAY;

        List<IncomingCallHierarchyNode> result = new ArrayList<>(calls.size());
        for (CallHierarchyIncomingCall call : calls)
        {
            CallHierarchyElement caller = new CallHierarchyElement(call.getFrom(), utility);

            IncomingCallHierarchyNode node = new IncomingCallHierarchyNode(this, caller);

            List<CallTextInfo> callTextInfos = utility.getCallTextInfo(
                DocumentUri.convert(call.getFrom().getUri()), call.getFromRanges());
            for (CallTextInfo callTextInfo : callTextInfos)
            {
                node.addCallLocation(new CallLocation(caller, callee, callTextInfo));
            }

            result.add(node);
        }
        return result.toArray(EMPTY_ARRAY);
    }
}
