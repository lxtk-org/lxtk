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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.ui.callhierarchy.CallHierarchyKind;
import org.eclipse.handly.ui.callhierarchy.CallHierarchyNode;
import org.eclipse.handly.ui.callhierarchy.CallLocation;
import org.eclipse.handly.ui.callhierarchy.CallTextInfo;
import org.eclipse.handly.ui.callhierarchy.ICallHierarchyNode;
import org.eclipse.lsp4j.CallHierarchyOutgoingCall;
import org.lxtk.DocumentUri;

/**
 * Represents an outgoing node of LXTK-based call hierarchy.
 */
public final class OutgoingCallHierarchyNode
    extends CallHierarchyNode
{
    private static final OutgoingCallHierarchyNode[] EMPTY_ARRAY = new OutgoingCallHierarchyNode[0];

    /**
     * Returns a new {@link OutgoingCallHierarchyNode} for the given call hierarchy element;
     * the node will have no parent.
     *
     * @param element not <code>null</code>
     * @return the created root node (never <code>null</code>)
     */
    public static OutgoingCallHierarchyNode newRootNode(CallHierarchyElement element)
    {
        return new OutgoingCallHierarchyNode(null, element);
    }

    private OutgoingCallHierarchyNode(OutgoingCallHierarchyNode parent,
        CallHierarchyElement element)
    {
        super(parent, element);
    }

    @Override
    public CallHierarchyKind getKind()
    {
        return CallHierarchyKind.CALLEE;
    }

    @Override
    protected ICallHierarchyNode[] computeChildren(IProgressMonitor monitor)
    {
        CallHierarchyElement caller = (CallHierarchyElement)getElement();
        CallHierarchyUtility utility = caller.getCallHierarchyUtility();

        List<CallHierarchyOutgoingCall> calls =
            utility.getOutgoingCalls(caller.getCallHierarchyItem(), monitor);
        if (calls == null || calls.isEmpty())
            return EMPTY_ARRAY;

        URI uri = DocumentUri.convert(caller.getCallHierarchyItem().getUri());
        List<OutgoingCallHierarchyNode> result = new ArrayList<>(calls.size());
        for (CallHierarchyOutgoingCall call : calls)
        {
            CallHierarchyElement callee = new CallHierarchyElement(call.getTo(), utility);

            OutgoingCallHierarchyNode node = new OutgoingCallHierarchyNode(this, callee);

            List<CallTextInfo> callTextInfos = utility.getCallTextInfo(uri, call.getFromRanges());
            for (CallTextInfo callTextInfo : callTextInfos)
            {
                node.addCallLocation(new CallLocation(caller, callee, callTextInfo));
            }

            result.add(node);
        }
        return result.toArray(EMPTY_ARRAY);
    }
}
