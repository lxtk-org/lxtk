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
package org.lxtk.lx4e;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CallHierarchyOutgoingCall;
import org.eclipse.lsp4j.CallHierarchyOutgoingCallsParams;
import org.lxtk.CallHierarchyProvider;

/**
 * Requests the outgoing calls for the given call hierarchy item.
 */
public class CallHierarchyOutgoingCallsRequest
    extends LanguageFeatureRequestWithWorkDoneAndPartialResultProgress<CallHierarchyProvider,
        CallHierarchyOutgoingCallsParams, List<CallHierarchyOutgoingCall>>
{
    @Override
    protected CompletableFuture<List<CallHierarchyOutgoingCall>> send(
        CallHierarchyProvider provider, CallHierarchyOutgoingCallsParams params)
    {
        setTitle(MessageFormat.format(Messages.CallHierarchyOutgoingCallsRequest_title, params));
        return provider.getCallHierarchyOutgoingCalls(params);
    }
}
